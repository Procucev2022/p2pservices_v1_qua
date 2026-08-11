@echo off
title P2P Services v1 - Test Runner
cd /d "%~dp0"

echo ===================================================
echo Running P2P Services v1 Tests
echo ===================================================

call mvnw.cmd test

pause
