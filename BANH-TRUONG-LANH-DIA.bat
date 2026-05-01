@echo off
chcp 65001 > nul
title OnlineAuctionSystem

echo ==========================================
echo    1. DON DEP HE THONG
echo ==========================================
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8888 ^| findstr LISTENING') do taskkill /f /pid %%a >nul 2>&1
echo [OK] He thong da sach se!
echo.

set BASE_DIR=%~dp0
cd /d "%BASE_DIR%"

echo ==========================================
echo    2. CAP NHAT VA BUILD CODE MOI
echo ==========================================
cd auction-server
call mvn clean install -DskipTests
cd ..

echo ==========================================
echo    3. KHOI DONG SERVER MOI
echo ==========================================
start "AuctionSERVER" /D "%BASE_DIR%auction-server" cmd /k "chcp 65001 > nul && mvn clean compile exec:java"

echo Dang doi Server san sang (10 giay)...
timeout /t 10

echo ==========================================
echo    4. KHOI DONG CLIENT
echo ==========================================
cd auction-client
mvn clean javafx:run

pause
