@echo off
chcp 65001 > nul
title Auction Client

set BASE_DIR=%~dp0
cd /d "%BASE_DIR%auction-client"

echo Dang khoi dong them mot Client moi...
call mvn javafx:run
