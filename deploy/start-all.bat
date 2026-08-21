# =====================================================================
#  水务监测分析系统 - 一键启动脚本 (Windows)
#  停止请运行 stop-all.bat
#  依赖：已安装并已启动 MySQL 5.7 与 Redis；JDK17、Python3、Node已装。
#  首次使用请先执行 init-db.bat 初始化数据库。
# =====================================================================
@echo off
chcp 65001 >nul
setlocal
title 水务监测分析系统 - 启动器

rem ---------- 路径设置(按需修改) ----------
set ROOT=%~dp0..
set BACKEND=%ROOT%\backend
set AI_DIR=%ROOT%\ai-service
set FRONTEND=%ROOT%\frontend
set JAR=%BACKEND%\target\water-monitor-1.0.0.jar

echo ==========================================
echo   [1/4] 启动 Redis ... (请确认已安装并已启动)
echo ==========================================
where redis-server >nul 2>nul && (start "Redis" redis-server) || echo 提示：未找到 redis-server，请手动启动 Redis 后继续。

echo ==========================================
echo   [2/4] 启动 Java 后端 (端口8080)
echo ==========================================
if not exist "%JAR%" (
    echo   未找到后端jar包，正在编译请等待...
    cd /d "%BACKEND%"
    mvn -q -DskipTests package
)
start "水务-后端" cmd /k "java -jar \"%JAR%\""
echo   backend PID window started.

echo ==========================================
echo   [3/4] 启动 Python AI 微服务 (端口8000)
echo ==========================================
start "水务-AI微服务" cmd /k "cd /d \"%AI_DIR%\" && python -m uvicorn main:app --host 0.0.0.0 --port 8000"

echo ==========================================
echo   [4/4] 启动前端 (Nginx 托管 dist / 端口80)
echo   若未构建前端，自动执行 npm run build
echo ==========================================
if not exist "%FRONTEND%\dist\index.html" (
    echo   构建前端中...
    cd /d "%FRONTEND%"
    npm install
    npm run build
)
where nginx >nul 2>nul && start "水务-前端Nginx" cmd /k "nginx -c \"%ROOT%\deploy\nginx.conf\" -p \"%ROOT%\" ^&^& pause" || (
    echo   提示：未找到 nginx，改用 Vite 开发服务器(端口5173)。
    start "水务-前端Vite" cmd /k "cd /d \"%FRONTEND%\" && npm run dev"
)

echo ==========================================
echo   启动流程完成！
echo   访问地址：
echo     管理后台   http://localhost/
echo     Vite dev  http://localhost:5173/
echo     可视化大屏 后台内进入 或 http://localhost/bigscreen
echo   默认账号: admin / 123456
echo ==========================================
endlocal
pause