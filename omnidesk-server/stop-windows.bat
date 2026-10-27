@echo off
setlocal EnableDelayedExpansion
title Stop OmniDesk Server
cd /d "%~dp0"

echo Stopping OmniDesk Server...

REM 1. Find process listening on port 8765
set FOUND_PID=
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8765 ^| findstr LISTENING') do (
    set FOUND_PID=%%a
)

if defined FOUND_PID (
    echo Terminating server process PID: !FOUND_PID!
    taskkill /F /PID !FOUND_PID! >nul 2>&1
    echo [OK] Port 8765 process terminated.
) else (
    echo [INFO] No process was actively listening on port 8765.
)

REM 2. Clean up any lingering python/pythonw server.py processes
for /f "tokens=2" %%i in ('wmic process where "commandline like '%%server.py%%'" get processid ^| findstr /r "[0-9]"') do (
    taskkill /F /PID %%i >nul 2>&1
)

REM 3. Clean PID file
if exist "%USERPROFILE%\.config\omnidesk\server.pid" (
    del "%USERPROFILE%\.config\omnidesk\server.pid" >nul 2>&1
)

echo.
echo [OK] OmniDesk Server is completely stopped.
timeout /t 2 >nul
