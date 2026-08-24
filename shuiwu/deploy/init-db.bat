@echo off
chcp 65001 >nul
title 水务监测分析系统 - 数据库初始化
echo ==========================================
echo   初始化数据库 shuiwu
echo   注意：会先删除已有 shuiwu 库，请确认无重要数据
echo ==========================================
set /p MYSQL_PWD=请输入 MySQL root 密码:
echo 正在执行 /sql/shuiwu.sql ...
mysql -uroot -p%MYSQL_PWD% < "%~dp0..\sql\shuiwu.sql"
if %errorlevel%==0 (
    echo 数据库初始化成功！
    echo 默认账号: admin / operator / viewer  密码均为 123456
) else (
    echo 初始化失败，请检查 MySQL 是否运行、密码是否正确。
)
pause