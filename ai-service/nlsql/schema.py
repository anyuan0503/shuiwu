# -*- coding: utf-8 -*-
"""
NL2SQL 表结构元数据。
水务监测数据采用"按月分表"策略，物理表命名 monitor_data_YYYYMM。
此处同时提供可用的月份分表清单，供规则引擎在跨月/取主表时选择最大的月份表。
"""

# 当月(用于生成最近的分表名)；可被外部覆盖
DEFAULT_YEAR_MONTH = ("2026", "08")

# 监测数据月分表清单（模拟可用表；实际运行时由 credentials 决定真实库）
MONITOR_DATA_TABLES = [
    "monitor_data_202606",
    "monitor_data_202607",
    "monitor_data_202608",
]


def current_main_table():
    """取最大的 monitor_data_YYYYMM 表作为默认主表（safer 策略）。"""
    return MONITOR_DATA_TABLES[-1]


def table_by_month(y, m):
    """按年月拼分表名。"""
    return "monitor_data_%s%s" % (y, m)


MONITOR_FIELDS = {
    "device_id": {"label": "设备ID", "type": "bigint", "desc": "设备编号"},
    "device_id_monitor": {"label": "设备编号", "type": "bigint", "desc": "监测设备ID"},
    "data_time": {"label": "时间", "type": "datetime", "desc": "采集时间"},
    "pressure": {"label": "压力", "type": "decimal", "unit": "MPa", "desc": "管网压力"},
    "flow": {"label": "流量", "type": "decimal", "unit": "m3/h", "desc": "管线流量"},
    "ph": {"label": "pH", "type": "decimal", "unit": "", "desc": "酸碱度"},
    "turbidity": {"label": "浊度", "type": "decimal", "unit": "NTU", "desc": "浑浊程度"},
    "residual_cl": {"label": "余氯", "type": "decimal", "unit": "mg/L", "desc": "余氯浓度"},
    "temperature": {"label": "温度", "type": "decimal", "unit": "°C", "desc": "水温"},
    "level": {"label": "液位", "type": "decimal", "unit": "m", "desc": "蓄水池液位"},
    "is_clean": {"label": "清洗标记", "type": "tinyint", "desc": "0脏 1正常 2已修复"},
}

# 指标名别名 -> 物理字段
METRIC_ALIASES = {
    "压力": "pressure",
    "压强": "pressure",
    "流量": "flow",
    "ph": "ph",
    "ph值": "ph",
    "酸碱度": "ph",
    "浊度": "turbidity",
    "浑浊度": "turbidity",
    "余氯": "residual_cl",
    "余氯量": "residual_cl",
    "温度": "temperature",
    "水温": "temperature",
    "液位": "level",
    "水位": "level",
}

# 设备别名关键词（中文片段 -> 用 device 相关条件）
DEVICE_HINTS = ["泵站", "泵房", "压力计", "压力表", "片区", "水厂", "水站", "测点", "监测点", "小区"]

# 假设备(用于模拟数据，保证演示)
SIM_DEVICES = [
    {"id": 1, "name": "一号泵站压力计", "area": "东城片区"},
    {"id": 2, "name": "文化路压力计", "area": "老城区"},
    {"id": 3, "name": "南郊水厂压力计", "area": "南郊片区"},
]


def build_default_tables():
    """契约要求调用方传 tables；若无则给出内置默认，保证服务单独演示可用。"""
    main = current_main_table()
    fields = list(MONITOR_FIELDS.keys())
    return [{
        "name": main,
        "desc": "管网监测数据月分表(主表)",
        "fields": fields,
    }]