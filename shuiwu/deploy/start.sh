#!/usr/bin/env bash
# =====================================================================
# 水务监测分析系统 - 一键启动 (Linux / Docker)
# 用法：bash start.sh
# 停止：bash stop.sh
# =====================================================================
set -e
cd "$(dirname "$0")"

echo "=========================================="
echo "  水务监测分析系统 - 启动 (Docker Compose)"
echo "=========================================="

# 检查 docker 是否可用
if ! command -v docker >/dev/null 2>&1; then
  echo "[错误] 未检测到 docker，请先安装 Docker 与 Docker Compose 插件。"
  exit 1
fi
if ! docker compose version >/dev/null 2>&1; then
  echo "[错误] 未检测到 docker compose 插件，请安装 docker-compose-plugin。"
  exit 1
fi

echo "[1/3] 构建并启动所有服务 (首次构建需拉取依赖，较耗时)..."
docker compose up -d --build

echo "[2/3] 等待后端与前端就绪..."
for i in $(seq 1 30); do
  if curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/auth/login 2>/dev/null | grep -q 400 ; then
    break
  fi
  if curl -s -o /dev/null http://127.0.0.1/ 2>/dev/null; then
    break
  fi
  sleep 2
done

echo "[3/3] 服务状态如下："
docker compose ps

echo ""
echo "=========================================="
echo "  启动完成！访问地址 (请用服务器 IP 替换 HOST)："
echo "    管理后台      http://HOST/      默认账号 admin / 123456"
echo "    可视化大屏    http://HOST/bigscreen"
echo "    后端接口      http://HOST:8080/api"
echo "    查看日志      docker compose -f $(pwd)/docker-compose.yml logs -f"
echo "    停止服务      运行 bash $(pwd)/stop.sh"
echo "=========================================="