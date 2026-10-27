@echo off
setlocal EnableDelayedExpansion
title OmniDesk Server - Background Launcher
cd /d "%~dp0"

echo =====================================================
echo    OmniDesk Server (Windows Background Mode)
echo =====================================================
echo.

REM Check if already running on port 8765
netstat -ano | findstr :8765 | findstr LISTENING >nul 2>&1
if %errorlevel% equ 0 (
    echo [NOTE] OmniDesk Server is already running in background!
    echo.
    if exist "%USERPROFILE%\.config\omnidesk\current_pin.txt" (
        set /p PIN=<"%USERPROFILE%\.config\omnidesk\current_pin.txt"
        echo Current Pairing PIN: [ !PIN! ]
    )
    echo.
    echo To stop:  stop-windows.bat
    echo To check: status-windows.bat
    echo.
    pause
    exit /b 0
)

echo Starting OmniDesk Server silently in the background...
wscript.exe "%~dp0run-invisible.vbs"

REM Wait 2 seconds for server to initialize and bind
timeout /t 2 /nobreak >nul 2>&1

echo.
echo -----------------------------------------------------
echo   OmniDesk Server is now ACTIVE in the background!
echo -----------------------------------------------------

REM Read pairing PIN if generated
if exist "%USERPROFILE%\.config\omnidesk\current_pin.txt" (
    set /p PIN=<"%USERPROFILE%\.config\omnidesk\current_pin.txt"
    echo   • Pairing PIN:   [ !PIN! ]
)
echo   • Port:          8765 (WebSocket)
echo   • Status:        Running Silently
echo.
echo [SUCCESS] You can now SAFELY CLOSE THIS WINDOW!
echo The receiver will continue running in the background.
echo.
echo Helpful commands:
echo   stop-windows.bat    - Stops the background receiver
echo   status-windows.bat  - View active PIN and connection status
echo.
pause
