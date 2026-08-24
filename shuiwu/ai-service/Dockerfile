# =====================================================================
# 水务监测分析系统 - AI 微服务镜像 (FastAPI)
# =====================================================================
FROM python:3.11-slim

WORKDIR /app

# 先拷贝依赖声明以利用层缓存
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 拷贝源码
COPY . .

ENV TZ=Asia/Shanghai \
    PYTHONUNBUFFERED=1

EXPOSE 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]