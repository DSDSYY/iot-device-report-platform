@echo off
chcp 65001 >nul
cd /d %~dp0\..
echo 启动 IoT 后端 (port 8090)...
echo 前置依赖：JDK 17+ / Maven 3.9+ / MySQL 8 / Redis / RabbitMQ
call mvn spring-boot:run
pause