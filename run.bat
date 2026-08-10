@echo off
title P2P Services v1 - Email RFQ Application
cd /d "%~dp0"

echo ===================================================
echo Starting P2P Services v1 (with Email RFQ Module)
echo ===================================================

call "C:\Users\Procucev\Docs\java version rfq project\apache-maven-3.9.16\bin\mvn.cmd" spring-boot:run

echo.
echo ===================================================
echo Application finished or encountered an error above.
echo ===================================================
pause
