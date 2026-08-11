@echo off
title P2P Services v1 - Email RFQ Application
cd /d "%~dp0"

echo ===================================================
echo Starting P2P Services v1 (with Email RFQ Module)
echo ===================================================

call mvnw.cmd spring-boot:run

echo.
echo ===================================================
echo Application finished or encountered an error above.
echo ===================================================
pause
