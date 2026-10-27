@echo off
setlocal EnableDelayedExpansion
title OmniDesk Server - Windows Autostart Manager
cd /d "%~dp0"

set STARTUP_DIR=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup
set VBS_TARGET=%STARTUP_DIR%\OmniDesk_Silent.vbs

if "%1"=="enable" goto do_enable
if "%1"=="disable" goto do_disable
if "%1"=="status" goto do_status

echo =====================================================
echo    OmniDesk Server - Windows Autostart Manager
echo =====================================================
echo.
echo What would you like to do?
echo   [1] Enable Autostart on Windows Logon (Silent Background)
echo   [2] Disable Autostart
echo   [3] Check Current Autostart Status
echo.
set /p CHOICE="Choose an option [1/2/3]: "
if "%CHOICE%"=="1" goto do_enable
if "%CHOICE%"=="2" goto do_disable
if "%CHOICE%"=="3" goto do_status
goto :eof

:do_enable
echo.
echo Configuring silent background autostart...

REM 1. Create silent launcher in Windows Startup folder
(
    echo Set WshShell = CreateObject^("WScript.Shell"^)
    echo WshShell.CurrentDirectory = "%~dp0"
    echo WshShell.Run "wscript.exe """ ^& "%~dp0run-invisible.vbs" ^& """", 0, False
) > "%VBS_TARGET%"

echo [OK] Created Startup shortcut:
echo      %VBS_TARGET%

REM 2. Also register Windows Scheduled Task for logon resilience
schtasks /create /tn "OmniDeskServer" /tr "wscript.exe \"%~dp0run-invisible.vbs\"" /sc onlogon /rl highest /f >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Registered Scheduled Task 'OmniDeskServer' (Runs on Logon with High Privileges)
)

echo.
echo =====================================================
echo [SUCCESS] OmniDesk will now start automatically in the
echo background whenever you log into Windows!
echo No terminal window will ever be shown.
echo =====================================================
echo.
set /p START_NOW="Launch the background server right now? [Y/n]: "
if /i not "%START_NOW%"=="n" (
    call "%~dp0start-background.bat"
)
goto :eof

:do_disable
echo.
echo Removing OmniDesk autostart...
if exist "%VBS_TARGET%" (
    del "%VBS_TARGET%"
    echo [OK] Removed Startup folder entry.
)
schtasks /delete /tn "OmniDeskServer" /f >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Removed Scheduled Task 'OmniDeskServer'.
)
echo [OK] Autostart disabled!
pause
goto :eof

:do_status
echo.
echo =====================================================
echo           OmniDesk Windows Autostart Status
echo =====================================================
if exist "%VBS_TARGET%" (
    echo Startup Folder:    [ENABLED] %VBS_TARGET%
) else (
    echo Startup Folder:    [DISABLED] Not present
)

schtasks /query /tn "OmniDeskServer" >nul 2>&1
if %errorlevel% equ 0 (
    echo Scheduled Task:    [ENABLED] Task 'OmniDeskServer' active
) else (
    echo Scheduled Task:    [DISABLED] Not present
)

echo.
REM Check listening port 8765
netstat -ano | findstr :8765 | findstr LISTENING >nul 2>&1
if %errorlevel% equ 0 (
    echo Server State:      [RUNNING] Listening on port 8765
    if exist "%USERPROFILE%\.config\omnidesk\current_pin.txt" (
        set /p PIN=<"%USERPROFILE%\.config\omnidesk\current_pin.txt"
        echo Current PIN:       [ !PIN! ]
    )
) else (
    echo Server State:      [STOPPED] Port 8765 not active
)
echo =====================================================
pause
goto :eof
