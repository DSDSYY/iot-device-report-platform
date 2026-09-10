@echo off
chcp 65001 >nul
if "%REDIS_HOME%"=="" set REDIS_HOME=C:\redis
echo 启动 Redis (127.0.0.1:6379)...
start "IoT-Redis" /min "%REDIS_HOME%\redis-server.exe" --port 6379
echo Redis 已启动（若窗口秒退说明端口被占用/已在运行）
pause