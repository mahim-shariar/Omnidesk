@echo off
setlocal EnableDelayedExpansion
title OmniDesk Status (Windows)
cd /d "%~dp0"

echo =====================================================
echo           OmniDesk Windows Server Status
echo =====================================================

REM 1. Port 8765 check
set SERVER_PID=
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8765 ^| findstr LISTENING') do (
    set SERVER_PID=%%a
)

if defined SERVER_PID (
    echo  Server State:    [RUNNING] Active in Background (PID: !SERVER_PID!)
    echo  Port 8765:       OPEN and listening for phone connections
) else (
    echo  Server State:    [STOPPED] Port 8765 is not listening
)

REM 2. Read Pairing PIN
if exist "%USERPROFILE%\.config\omnidesk\current_pin.txt" (
    set /p PIN=<"%USERPROFILE%\.config\omnidesk\current_pin.txt"
    echo  Pairing PIN:     [ !PIN! ]
)

REM 3. Show local IP address
for /f "tokens=4" %%a in ('route print ^| findstr 0.0.0.0 ^| findstr /v "0.0.0.0.*0.0.0.0"') do (
    set LOCAL_IP=%%a
)
if defined LOCAL_IP (
    echo  Host Local IP:   !LOCAL_IP!
    echo  WebSocket URL:   ws://!LOCAL_IP!:8765
)

echo.
echo -----------------------------------------------------
echo Helpful Commands:
echo   start-background.bat  - Run server in background
echo   stop-windows.bat      - Stop background server
echo   autostart-windows.bat - Configure startup on login
echo =====================================================
pause
