# -*- coding: utf-8 -*-
"""
数据清洗服务。
输入 rows（每行是监测数值字符串/数值）。规则：
  1) 非数值 -> 置空/剔除标记
  2) 超出物理合理范围 -> 脏数据
     pressure 0-2.5 MPa, ph 0-14, turbidity 0-20 NTU, residual_cl 0-5,
     temperature -10~60, level 0-20, flow >= 0
  3) 相邻阶跃跳变/绝对差过大 -> 异常点
返回 {cleaned, repaired, removed, detail:[{index, reason}]}
"""
import math

# 字段物理合理范围
FIELD_RANGES = {
    "pressure": (0.0, 2.5),
    "ph": (0.0, 14.0),
    "turbidity": (0.0, 20.0),
    "residual_cl": (0.0, 5.0),
    "temperature": (-10.0, 60.0),
    "level": (0.0, 20.0),
    "flow": (0.0, None),  # 仅下界
}

# 阶跃跳变阈值：超过上限比例或相对差分过大判为异常
JUMP_RATIO = 5.0       # 相邻差分超过前值 5 倍视为跳变
ABSOLUTE_JUMP = {       # 各字段允许的绝对值突变上限
    "pressure": 0.5, "flow": 300, "ph": 3.0, "turbidity": 8.0,
    "residual_cl": 1.5, "temperature": 15.0, "level": 5.0,
}


def _to_float(v):
    """容忍字符串数值。"""
    if isinstance(v, bool) or v is None:
        return None
    if isinstance(v, (int, float)):
        return float(v)
    s = str(v).strip()
    if s in ("", "-", "--", "null", "None", "NULL", "nan"):
        return None
    try:
        return float(s)
    except (ValueError, TypeError):
        return None


def _in_range(field, val):
    if field not in FIELD_RANGES:
        return True
    lo, hi = FIELD_RANGES[field]
    if hi is None:
        return val >= lo
    return lo <= val <= hi


def clean_rows(rows, field="pressure"):
    """
    rows: list，每行可为数值/字符串，或含指标 key 的 dict。
    field: 本次清洗的指标字段。
    """
    cleaned = 0      # 总共处理数
    repaired = 0     # 修正数
    removed = 0      # 剔除数
    detail = []

    # 归一化成数值序列 + 原始引用
    parsed = []  # (raw_value, numeric_or_None)
    for r in rows:
        if isinstance(r, dict):
            parsed.append((r, _to_float(r.get(field))))
        else:
            parsed.append((r, _to_float(r)))

    # 第一遍：非数值/越界识别
    status = []  # (is_valid: bool, reason_or_None, numeric_or_None)
    for i, (_raw, num) in enumerate(parsed):
        if num is None:
            status.append((False, "index %d 非数值，置空剔除" % i, None))
            continue
        if not _in_range(field, num):
            status.append((False, "index %d %s 超出合理物理范围" % (i, field), num))
            continue
        status.append((True, None, num))

    # 第二遍：阶跃跳变检测（仅对范围内有效值比较相邻）
    effective = [(i, s[2]) for i, s in enumerate(status) if s[0] and s[2] is not None]
    jump_idx = set()
    for k in range(1, len(effective)):
        i_prev, v_prev = effective[k - 1]
        i_cur, v_cur = effective[k]
        if v_prev == 0:
            continue
        ratio = abs(v_cur - v_prev) / abs(v_prev)
        abs_diff = abs(v_cur - v_prev)
        allowance = ABSOLUTE_JUMP.get(field, 5.0)
        if ratio >= JUMP_RATIO and abs_diff >= allowance:
            jump_idx.add(i_cur)
            # 修正：将异常点修正为前值，作为 repaired
            status[i_cur] = (True, None, v_prev)
            repaired += 1
            detail.append({"index": i_cur, "reason": "index %d 相对前一值跳变，已修正为前值" % i_cur})

    # 汇总
    removed_idx = set()
    for i, (valid, reason, num) in enumerate(status):
        cleaned += 1
        if not valid:
            removed += 1
            removed_idx.add(i)
            if reason and "跳变" not in reason:
                detail.append({"index": i, "reason": reason})

    # 去重：被标记为 removed 的 index 不再算 repaired
    repair_count = sum(1 for d in detail if "修正" in d.get("reason", "") and d["index"] not in removed_idx)
    repaired = repair_count

    # 计算 cleaned 处理数为输入行总数；repaired 为修正数（跳变修正）；removed 为非数值+越界
    return {
        "cleaned": cleaned,
        "repaired": repaired,
        "removed": removed,
        "detail": detail,
    }