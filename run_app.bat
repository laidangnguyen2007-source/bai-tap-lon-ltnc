@echo off
echo =========================================
echo KHOI DONG HE THONG AUCTION (SERVER + CLIENT)
echo =========================================

echo 1. Dang khoi dong Server...
:: Mở một cửa sổ CMD mới để chạy Server
start cmd /k "cd /d C:\Study\Java\BaiTapLon\bai-tap-lon-ltnc\auction-server && mvn exec:java"

echo 2. Dang doi 8 giay de Server kip khoi dong...
:: Lệnh timeout giúp hệ thống chờ vài giây trước khi chạy bước tiếp theo
timeout /t 8 /nobreak >nul

echo 3. Dang khoi dong Client...
:: Mở một cửa sổ CMD thứ 2 để chạy Client
start cmd /k "cd /d C:\Study\Java\BaiTapLon\bai-tap-lon-ltnc\auction-client && mvn javafx:run"

echo Hoan tat! Ban co the tat cua so nay.
exit