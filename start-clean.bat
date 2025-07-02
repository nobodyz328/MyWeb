@echo off
echo 正在清理并启动Spring Boot博客应用...
echo.

echo 清理项目...
mvn clean

echo.
echo 下载依赖...
mvn dependency:resolve

echo.
echo 编译项目...
mvn compile

echo.
echo 启动应用...
mvn spring-boot:run

pause 