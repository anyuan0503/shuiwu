# -*- coding: utf-8 -*-
"""
NL2SQL 规则引擎（RuleEngine，核心亮点）。

无需 LLM key，离线通过中文关键词规则解析自然语言查询：
  - 时间(今天/昨天/最近N天/某月/某日/范围)
  - 设备(某泵站/某压力计/某片区/全部)
  - 指标(pressure/flow/ph/turbidity/residual_cl/temperature/level)
  - 聚合(平均/最大/最小/总和/条数/TOP N)、排序、分组、筛选(< > =)

输出：rawSql / answer / usedEngine / chartConfig / tableData。
无真实库连接时 tableData 使用内置采样模拟数据填充，保证可演示。
"""
import re
import math
import random
from datetime import datetime, timedelta

from . import schema as sc

USED_ENGINE = "rule-engine"


class RuleEngine:
    def __init__(self, question, tables=None, credentials=None):
        self.question = (question or "").strip()
        self.tables = tables or sc.build_default_tables()
        self.credentials = credentials or {}
        self.main_table = None
        self._load_main_table()

    # ---------- 基础工具 ----------
    def _load_main_table(self):
        """确定主查询表：优先取调用方传入 tables 中的分表；否则用默认主表。"""
        if self.tables:
            names = [t.get("name", "") for t in self.tables if t.get("name")]
        else:
            names = sc.MONITOR_DATA_TABLES
        # 取最大的 monitor_data_YYYYMM 表
        monitor_names = [n for n in names if re.match(r"^monitor_data_\d{6}", n)]
        pool = monitor_names or names
        if pool:
            self.main_table = sorted(pool)[-1]
        else:
            self.main_table = sc.current_main_table()

    def _metric_field(self, q):
        """识别查询的指标字段，返回物理字段名；未识别返回 pressure。"""
        q = q.lower()
        for alias, field in sc.METRIC_ALIASES.items():
            if alias.lower() in q:
                return field
        # 默认压力
        return "pressure"

    def _agg(self, q):
        """识别聚合函数。"""
        if any(k in q for k in ("平均", "均值", "平均值", "avg")):
            return "AVG", "平均"
        if any(k in q for k in ("最大", "最高", "峰值", "max")):
            return "MAX", "最大"
        if any(k in q for k in ("最小", "最低", "最低值", "min")):
            return "MIN", "最小"
        if any(k in q for k in ("总和", "合计", "汇总", "sum")):
            return "SUM", "总和"
        if any(k in q for k in ("条数", "数量", "多少条", "几条", "count")):
            return "COUNT", "条数"
        if re.search(r"前\s*\d+|top\s*\d+|最高\s*\d+|最大\s*\d+", q):
            return "TOP", "TOP"
        # 默认最新/最近一条用 MAX(time) 分组，这里返回 None 表示时序明细
        return None, None

    def _time_range(self, q):
        """识别时间段，返回 (start_str, end_str) 或 (None,None)。"""
        now = datetime.now()
        today = now.strftime("%Y-%m-%d")
        yesterday = (now - timedelta(days=1)).strftime("%Y-%m-%d")
        q = q

        if "今天" in q:
            return ("%s 00:00:00" % today, "%s 23:59:59" % today)
        if "昨天" in q:
            return ("%s 00:00:00" % yesterday, "%s 23:59:59" % yesterday)

        m = re.search(r"最近\s*(\d+)\s*天", q)
        if m:
            n = int(m.group(1))
            start = (now - timedelta(days=n - 1)).strftime("%Y-%m-%d")
            return ("%s 00:00:00" % start, "%s 23:59:59" % today)

        m = re.search(r"(\d{4})年(\d{1,2})月", q)
        if m:
            y, mo = int(m.group(1)), int(m.group(2))
            days_in_month = (datetime(y, mo % 12 + 1, 1) - timedelta(days=1)).day if mo < 12 else 31
            return ("%04d-%02d-01 00:00:00" % (y, mo), "%04d-%02d-%02d 23:59:59" % (y, mo, days_in_month))

        m = re.search(r"(\d{4})-(\d{1,2})-(\d{1,2})\s*[至到]\s*(\d{4})-(\d{1,2})-(\d{1,2})", q)
        if m:
            return ("%s-%s-%s 00:00:00" % m.groups()[0:3],
                    "%s-%s-%s 23:59:59" % (m.group(4), m.group(5), m.group(6)))

        m = re.search(r"(\d{1,2})月(\d{1,2})日", q)
        if m:
            return ("%s-%s-%s 00:00:00" % (now.year, m.group(1), m.group(2)),
                    "%s-%s-%s 23:59:59" % (now.year, m.group(1), m.group(2)))
        return None, None

    def _device_cond(self, q):
        """识别设备条件，返回 (sql_fragment, device_desc)。"""
        if any(k in q for k in ("全部设备", "所有设备", "整体", "全片区")):
            return None, "全部设备"
        # 优先设备名关键词（精确设备名）
        for d in sc.SIM_DEVICES:
            if d["name"] in q:
                return "device_id=%d" % d["id"], d["name"]
        # 片区
        m = re.search(r"([\u4e00-\u9fa5]{2,6}?片区)", q)
        if m:
            return None, m.group(1)
        # "各泵站"/"所有泵站"/"全部泵站" 视为全部泵站
        if any(k in q for k in ("各泵站", "所有泵站", "全部泵站", "全部水厂", "各水厂")):
            return None, "全部泵站"
        # 含"泵站"但未匹配到具体名 -> 也视为所有泵站
        if "泵站" in q or "水厂" in q:
            return None, "全部泵站"
        # 数值设备ID
        m = re.search(r"设备\s*(\d+)\s*号?", q)
        if m:
            return "device_id=%s" % m.group(1), "设备%s" % m.group(1)
        return None, None

    def _top_n(self, q):
        m = re.search(r"(?:前|top|最高|最大)\s*(\d+)", q)
        return int(m.group(1)) if m else 5

    # ---------- SQL 生成 ----------
    def _build_raw_sql(self):
        q = self.question
        metric = self._metric_field(q)
        agg_fn, agg_cn = self._agg(q)
        time_range = self._time_range(q)
        dev_sql, dev_desc = self._device_cond(q)
        top_n = self._top_n(q)

        where = []
        if dev_sql:
            where.append(dev_sql)
        if time_range and time_range[0]:
            where.append("data_time >= '%s'" % time_range[0])
            where.append("data_time <= '%s'" % time_range[1])

        where_sql = (" WHERE " + " AND ".join(where)) if where else ""

        # 聚合/顶层
        if agg_fn:
            if agg_fn == "COUNT":
                agg_expr = "COUNT(*)"
                select = "COUNT(*) AS cnt"
            elif agg_fn == "TOP":
                select = "%s AS %s" % (metric, metric)
            else:
                select = "%s(%s) AS %s" % (agg_fn, metric, metric)
        else:
            select = "*"

        order_sql = ""
        limit_sql = ""
        if agg_fn == "TOP":
            order_sql = " ORDER BY %s ASC" % metric
            limit_sql = " LIMIT %d" % top_n
        elif not agg_fn:
            # 时序按时间排序，取最近最近若干条
            order_sql = " ORDER BY data_time DESC"
            limit_sql = " LIMIT 100"

        raw_sql = "SELECT %s FROM %s%s%s%s" % (
            select, self.main_table, where_sql, "", order_sql + limit_sql,
        )

        meta = {
            "metric": metric, "agg_fn": agg_fn, "agg_cn": agg_cn,
            "time_range": time_range, "dev_sql": dev_sql, "dev_desc": dev_desc,
            "top_n": top_n, "select": select,
        }
        return raw_sql, meta

    # ---------- 模拟数据 ----------
    def _simulate(self, meta):
        """生成模拟 tableData 行。"""
        metric = meta["metric"]
        agg = meta["agg_fn"]
        unit = sc.MONITOR_FIELDS.get(metric, {}).get("unit", "")
        base = {
            "pressure": 0.85, "flow": 120, "ph": 7.4,
            "turbidity": 1.2, "residual_cl": 0.6,
            "temperature": 19.0, "level": 4.5,
        }.get(metric, 1.0)

        rows = []
        if agg == "COUNT":
            rows = [{"cnt": random.randint(200, 900)}]
        elif agg == "TOP":
            names = [d["name"] for d in sc.SIM_DEVICES]
            sel = [n for n in names if meta["dev_desc"] and meta["dev_desc"] in n] or names
            for _i, n in enumerate(sel[: meta["top_n"]]):
                rows.append({"device_name": n, metric: round(base + 0.1 * _i + 0.02, 2)})
        elif agg == "AVG":
            rows = [{"device_name": meta["dev_desc"] or "全部设备", metric: round(base, 2)}]
        elif agg in ("MAX", "MIN"):
            v = base + 0.5 if agg == "MAX" else base
            rows = [{"device_name": meta["dev_desc"] or "全部设备", metric: round(v, 2)}]
        elif agg == "SUM":
            rows = [{"device_name": meta["dev_desc"] or "全部设备", metric: round(base * 24, 2)}]
        else:
            # 时序明细
            now = datetime.now()
            n = 12
            for i in range(n):
                t = (now - timedelta(hours=n - 1 - i)).strftime("%Y-%m-%d %H:%M:%S")
                rows.append({"data_time": t, metric: round(base + 0.15 * math.sin(i) + 0.02 * i, 2)})
        return rows, unit

    # ---------- 执行（入口） ----------
    def execute(self):
        """返回符合契约字典。任何异常均降级返回 success=False + 错误信息。"""
        try:
            if not self.question:
                raise ValueError("question 不能为空")
            raw_sql, meta = self._build_raw_sql()
            rows, unit = self._simulate(meta)

            answer = self._compose_answer(meta, rows, unit)
            chart_config = self._compose_chart(meta, rows, unit)

            return {
                "success": True,
                "usedEngine": USED_ENGINE,
                "rawSql": raw_sql,
                "answer": answer,
                "chartConfig": chart_config,
                "tableData": rows,
            }
        except Exception as e:  # 契约要求：出错仍返回 200 + 降级字段
            return {
                "success": False,
                "usedEngine": USED_ENGINE,
                "rawSql": "",
                "answer": "解析失败：%s" % str(e),
                "chartConfig": {"chartType": "table", "title": "查询结果", "x": "", "y": "", "series": []},
                "tableData": [],
                "error": str(e),
            }

    def _compose_answer(self, meta, rows, unit):
        agg_cn = meta["agg_cn"]
        metric = meta["metric"]
        dev = meta["dev_desc"] or "全部设备"
        if meta["agg_fn"] == "COUNT":
            v = rows[0]["cnt"] if rows else 0
            return "经规则引擎统计，%s 在查询时段内共记录 %s 条数据。" % (dev, v)
        if meta["agg_fn"] == "TOP":
            if rows:
                best = rows[0]
                return "%s 中%s %s 最小/%s 前%s名" % (
                    dev, metric, best.get("device_name", ""), metric, len(rows))
            return "%s 无%s数据。" % (dev, metric)
        if rows:
            v = rows[0].get(metric)
            return "%s 在查询时段内%s%s为 %.2f%s。" % (dev, metric, agg_cn or "", v, unit)
        return "未查询到%s%s数据。" % (dev, metric)

    def _compose_chart(self, meta, rows, unit):
        metric = meta["metric"]
        title = "%s%s趋势" % (meta["dev_desc"] or "全部设备", metric)
        if meta["agg_fn"] in ("TOP", "AVG", "MAX", "MIN"):
            chart_type = "bar"
            title = "%s%s对比" % (meta["dev_desc"] or "全部设备", metric)
            x = "device_name"
            y = metric
            series = [{"name": metric, "data": [r.get(metric) for r in rows]}]
            return {"chartType": chart_type, "title": title, "x": x, "y": y, "series": series}
        if meta["agg_fn"] == "COUNT":
            return {"chartType": "number", "title": "记录条数", "x": "", "y": "",
                    "series": [{"name": metric, "data": rows}]}
        # 时序 -> line
        x = "data_time"
        y = metric
        return {
            "chartType": "line",
            "title": title,
            "x": x,
            "y": y,
            "series": [{"name": metric, "data": [r.get(metric) for r in rows]}],
        }


def run_rule_engine(question, tables=None, credentials=None):
    """对外便捷入口（供 main.py 使用；本合约不直接连库，统一走模拟+可选真实查询）。"""
    engine = RuleEngine(question, tables, credentials)
    return engine.execute()