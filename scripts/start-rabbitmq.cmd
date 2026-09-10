@echo off
chcp 65001 >nul
REM ===== 按本机实际安装路径修改以下环境变量 =====
if "%RABBITMQ_HOME%"=="" set RABBITMQ_HOME=C:\rabbitmq\rabbitmq_server-3.13.7
if "%ERLANG_HOME%"==""   set ERLANG_HOME=C:\Program Files\Erlang OTP
if "%RABBITMQ_BASE%"=="" set RABBITMQ_BASE=C:\rabbitmq-data
if "%RABBITMQ_ERLANG_COOKIE%"=="" set RABBITMQ_ERLANG_COOKIE=change-me-demo-cookie
set RABBITMQ_NODENAME=rabbit@localhost
echo 启动 RabbitMQ (127.0.0.1:5672)...
call "%RABBITMQ_HOME%\sbin\rabbitmq-server.bat"
pause