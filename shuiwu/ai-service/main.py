# -*- coding: utf-8 -*-
"""
水务监测分析系统 - AI 微服务 (FastAPI)
内部接口（后端 http://127.0.0.1:8000 调用，对外不开放）：
  POST /ai/nlsql   NL2SQL（规则引擎，离线可用）
  POST /ai/clean   数据清洗
  POST /ai/anomaly 管网异常分析

契约参见 /workspace/docs/APICONTRACT.md 第 9 节。
"""
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
from typing import Any, Optional, List

from nlsql.rule_engine import USED_ENGINE, run_rule_engine, RuleEngine
import cleaning as cleaning_mod
import anomaly as anomaly_mod

app = FastAPI(title="水务 AI 微服务", version="1.0.0")


# ---------- Pydantic 请求模型（与契约一致） ----------
class NlSqlRequest(BaseModel):
    question: str
    tables: Optional[List[Any]] = None
    credentials: Optional[Any] = None


class CleanRequest(BaseModel):
    rows: List[Any] = Field(default_factory=list)
    field: Optional[str] = "pressure"


class AnomalyItem(BaseModel):
    deviceId: Any = None
    deviceName: Optional[str] = None
    field: Optional[str] = "pressure"
    data: Optional[List[Any]] = None


class AnomalyRequest(BaseModel):
    series: List[AnomalyItem] = Field(default_factory=list)


# ---------- 工具：尝试真实库查询（吞异常降级模拟） ----------
def _try_real_query(question, tables, credentials):
    """若 credentials+pymysql 可用则真实执行 rawSql；任何异常返回 None。"""
    if not credentials or not isinstance(credentials, dict):
        return None
    host = credentials.get("host")
    port = credentials.get("port", 3306)
    user = credentials.get("user")
    password = credentials.get("password")
    database = credentials.get("database")
    if not (host and user):
        return None
    try:
        import pymysql
    except Exception:
        return None
    try:
        engine = RuleEngine(question, tables, credentials)
        raw_sql, _meta = engine._build_raw_sql()
        conn = pymysql.connect(
            host=host, port=int(port), user=user, password=password,
            database=database, charset="utf8mb4", connect_timeout=2,
            cursorclass=pymysql.cursors.DictCursor,
        )
        try:
            with conn.cursor() as cur:
                cur.execute(raw_sql)
                rows = cur.fetchall()
        finally:
            conn.close()
        # DateTime 序列化
        for r in rows:
            for k, v in r.items():
                if hasattr(v, "strftime"):
                    r[k] = v.strftime("%Y-%m-%d %H:%M:%S")
        return rows
    except Exception:
        # 吞异常，返回 None 让哪吒走模拟数据
        return None


@app.get("/")
def index():
    return {"service": "water-ai", "engine": USED_ENGINE, "status": "ok"}


@app.get("/ai/health")
def health():
    return {
        "status": "UP",
        "engine": USED_ENGINE,
        "endpoints": ["/ai/nlsql", "/ai/clean", "/ai/anomaly"],
    }


@app.post("/ai/nlsql")
async def nlsql(req: NlSqlRequest):
    """自然语言查数 NL2SQL。"""
    try:
        res = run_rule_engine(req.question, list(req.tables or []), req.credentials)
        # 尝试真实库查询，成功则覆盖 tableData 并重算 answer/chart（失败保持模拟）
        real = None
        if req.credentials:
            real = _try_real_query(req.question, req.tables, req.credentials)
        if real:
            res["tableData"] = real
            res["answer"] = res.get("answer") + "（已连接 %s 真实查询）" % req.credentials.get("database", "")
        return JSONResponse(status_code=200, content=res)
    except Exception as e:
        return JSONResponse(status_code=200, content={
            "success": False,
            "usedEngine": USED_ENGINE,
            "rawSql": "",
            "answer": "NL2SQL 服务异常：%s" % str(e),
            "chartConfig": {"chartType": "table", "title": "查询结果", "x": "", "y": "", "series": []},
            "tableData": [],
            "error": str(e),
        })


@app.post("/ai/clean")
async def clean(req: CleanRequest):
    """数据清洗。"""
    try:
        result = cleaning_mod.clean_rows(req.rows, field=req.field or "pressure")
        return JSONResponse(status_code=200, content=result)
    except Exception as e:
        return JSONResponse(status_code=200, content={
            "cleaned": 0, "repaired": 0, "removed": 0,
            "detail": [], "error": str(e),
        })


@app.post("/ai/anomaly")
async def anomaly(req: AnomalyRequest):
    """管网异常分析。"""
    try:
        items = [
            {
                "deviceId": s.deviceId,
                "deviceName": s.deviceName,
                "field": s.field or "pressure",
                "data": s.data or [],
            }
            for s in req.series
        ]
        result = anomaly_mod.detect_anomalies(items)
        return JSONResponse(status_code=200, content={"anomalies": result})
    except Exception as e:
        return JSONResponse(status_code=200, content={"anomalies": [], "error": str(e)})