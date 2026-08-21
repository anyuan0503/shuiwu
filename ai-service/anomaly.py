# -*- coding: utf-8 -*-
"""
管网异常分析服务。
输入 series：每条含 deviceId / deviceName / field / data，其中 data 为升序的
[[timestamp, value], ...]。

算法：z-score + 滑动均值做离群检测，将连续离群点聚合成异常段：
  {deviceId, field, start, end, score, desc}
start/end 取段内首尾时间；score 为段内最大偏离度（0~1）。
无有效数据时降级返回空数组。
"""
import math


def _as_float(v):
    try:
        return float(v)
    except (TypeError, ValueError):
        return None


def _median(xs):
    s = sorted(xs)
    m = len(s) // 2
    return s[m] if len(s) % 2 == 1 else (s[m - 1] + s[m]) / 2.0


def _robust_z_score(values):
    """返回每个点的稳健 z 值（Robust z-score）。

    用中位数 + MAD(绝对中位差) 替代均值 + 标准差，避免单个离群值抬高方差、
    使 z 值停留在阈值边界而漏报（经典 z-score 的缺陷）。
    """
    med = _median(values)
    mad = _median([abs(v - med) for v in values])
    if mad <= 1e-9:
        # MAD 退化（大量重复值）时回退到标准差法
        mean = sum(values) / len(values)
        std = math.sqrt(sum((v - mean) ** 2 for v in values) / len(values))
        if std <= 1e-9:
            return [0.0] * len(values)
        scale = std
    else:
        scale = mad / 0.6745  # 与正态总标准差对齐的缩放因子
    return [(v - med) / scale for v in values]


def detect_anomalies(series):
    """series: list of {deviceId, deviceName, field, data}"""
    anomalies = []

    for item in series or []:
        device_id = item.get("deviceId") or item.get("device_id")
        device_name = item.get("deviceName") or item.get("device_name", str(device_id))
        field = item.get("field", "pressure")
        data = item.get("data") or []

        # 解析并按时间升序
        pts = []
        for pair in data:
            if not isinstance(pair, (list, tuple)) or len(pair) < 2:
                continue
            ts, raw = pair[0], pair[1]
            v = _as_float(raw)
            if v is None:
                continue
            pts.append((ts, v))
        pts.sort(key=lambda x: x[0])
        if len(pts) < 3:
            continue

        values = [v for _t, v in pts]
        zs = _robust_z_score(values)

        # 稳健 z 绝对值大于阈值视为离群；MAD 方法对单个离群点非常敏感，
        # 不存在"方差被抬高导致漏报"的问题。
        K = 3.0
        outlier_idx = {i for i, z in enumerate(zs) if abs(z) >= K}

        if not outlier_idx:
            continue

        # 聚合成段：连续离群点合并
        idxs = sorted(outlier_idx)
        segments = [[idxs[0]]]
        for i in range(1, len(idxs)):
            if idxs[i] == idxs[i - 1] + 1:
                segments[-1].append(idxs[i])
            else:
                segments.append([idxs[i]])

        for seg in segments:
            seg_vals = [values[i] for i in seg]
            # 段内最大偏离度（稳健量纲）归一化到 0~1
            max_z = max(abs(zs[i]) for i in seg)
            score = min(1.0, max(0.0, math.tanh(max_z / 4.0)))
            desc = "%s 的 %s 在 %s 至 %s 出现离群，偏离度约 %.0f%%" % (
                device_name, field, pts[seg[0]][0], pts[seg[-1]][0], score * 100)
            anomalies.append({
                "deviceId": device_id,
                "field": field,
                "start": pts[seg[0]][0],
                "end": pts[seg[-1]][0],
                "score": round(score, 4),
                "desc": desc,
            })

    return anomalies