@echo off
title P2P Services v1 - Test Runner
cd /d "%~dp0"

echo ===================================================
echo Running P2P Services v1 Tests
echo ===================================================

call "C:\Users\Procucev\Docs\java version rfq project\apache-maven-3.9.16\bin\mvn.cmd" test

pause
