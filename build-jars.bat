@echo off
chcp 65001 > nul
title Build Fat JARs

set BASE_DIR=%~dp0
cd /d "%BASE_DIR%"

echo ==========================================
echo    BUILD SERVER FAT JAR
echo ==========================================
cd auction-server
call mvn clean install -DskipTests
if errorlevel 1 (
    echo [LOI] Build server that bai!
    if "%~1"=="" pause
    exit /b 1
)
cd ..

echo.
echo ==========================================
echo    BUILD CLIENT FAT JAR
echo ==========================================
cd auction-client
call mvn clean package -DskipTests
if errorlevel 1 (
    echo [LOI] Build client that bai!
    if "%~1"=="" pause
    exit /b 1
)
cd ..

echo.
echo ==========================================
echo    COPY VAO release/
echo ==========================================
if not exist "release" mkdir release
copy /Y "auction-server\target\server.jar" "release\server.jar" > nul
copy /Y "auction-client\target\client.jar" "release\client.jar" > nul
echo [OK] release\server.jar
echo [OK] release\client.jar

echo.
echo Chay server: java -jar release\server.jar
echo Chay client: java -jar release\client.jar
if "%~1"=="" pause
