@echo off
setlocal EnableDelayedExpansion
title OmniDesk Server Launcher
cd /d "%~dp0"

if "%1"=="--background" goto do_background
if "%1"=="-b" goto do_background
if "%1"=="--foreground" goto do_foreground
if "%1"=="-f" goto do_foreground
if "%1"=="--stop" goto do_stop
if "%1"=="--status" goto do_status

echo =====================================================
echo    🪟  OmniDesk Server for Windows  🪟
echo =====================================================
echo.
echo How would you like to run the server?
echo   [1] Background Mode (Recommended)
echo       Runs silently in background. Safe to close this window!
echo   [2] Foreground Mode
echo       Shows live streaming logs directly in this window.
echo   [3] Autostart Settings
echo       Enable or disable starting automatically on login.
echo.
set /p CHOICE="Choose an option [1/2/3] (Default: 1): "
if "%CHOICE%"=="2" goto do_foreground
if "%CHOICE%"=="3" goto do_autostart
goto do_background

:do_background
call "%~dp0start-background.bat"
goto :eof

:do_foreground
echo Starting OmniDesk Server in foreground... (Press Ctrl+C to stop)
if exist "%~dp0.venv\Scripts\python.exe" (
    "%~dp0.venv\Scripts\python.exe" "%~dp0server.py"
) else (
    python "%~dp0server.py"
)
pause
goto :eof

:do_autostart
call "%~dp0autostart-windows.bat"
goto :eof

:do_stop
call "%~dp0stop-windows.bat"
goto :eof

:do_status
call "%~dp0status-windows.bat"
goto :eof
