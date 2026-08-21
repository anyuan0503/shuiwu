@echo off
chcp 65001 >nul
title 水务监测分析系统 - 停止脚本
echo 正在关闭 水务监测分析系统 相关进程...
echo (Java 后端 / Python AI / Nginx / Node)
taskkill /F /FI "IMAGENAME eq java.exe" >nul 2>nul
taskkill /F /FI "IMAGENAME eq javaw.exe" >nul 2>nul
taskkill /F /FI "WINDOWTITLE eq 水务-AI微服务*" >nul 2>nul
taskkill /F /FI "WINDOWTITLE eq 水务-前端Nginx*" >nul 2>nul
taskkill /F /FI "IMAGENAME eq nginx.exe" >nul 2>nul
taskkill /F /FI "IMAGENAME eq node.exe" >nul 2>nul
echo 完成。Redis/MySQL 若作为服务安装请自行决定是否停止。
pause