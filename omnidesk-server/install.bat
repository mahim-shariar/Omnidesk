@echo off
setlocal EnableDelayedExpansion
title OmniDesk Companion Server - Windows Guided Setup

echo ======================================================================
echo       ⚡ OmniDesk Companion Server - Windows Setup Wizard ⚡
echo       Beginner-friendly prompt-by-prompt guided installation
echo ======================================================================
echo.

REM 1. Check Python installation
python --version >nul 2>&1
if %errorlevel% neq 0 (
    py --version >nul 2>&1
    if !errorlevel! equ 0 (
        set PYCMD=py
    ) else (
        echo [ERROR] Python 3 was not detected on this system.
        echo.
        echo Would you like to automatically install Python using Windows Package Manager (winget)?
        set /p INSTALL_WINGET="Install Python 3.11 via winget now? [Y/n]: "
        if /i not "!INSTALL_WINGET!"=="n" (
            echo Running: winget install Python.Python.3.11 --silent
            winget install Python.Python.3.11
            echo.
            echo [NOTE] Python has been installed. Please close this window and run install.bat again!
            pause
            exit /b 0
        )
        echo.
        echo Or manually download Python:
        echo 1. Go to: https://www.python.org/downloads/
        echo 2. IMPORTANT: Check "Add Python to PATH" during installation!
        echo 3. Re-run install.bat
        echo.
        pause
        exit /b 1
    )
) else (
    set PYCMD=python
)

echo [OK] Python detected:
%PYCMD% --version
echo.

REM 2. If install_wizard.py exists, launch the rich interactive Python wizard
if exist "%~dp0install_wizard.py" (
    echo Launching OmniDesk Interactive Setup Wizard...
    echo.
    %PYCMD% "%~dp0install_wizard.py"
    if %errorlevel% equ 0 (
        exit /b 0
    )
)

REM 3. Fallback Batch Interactive Wizard
echo.
echo ----------------------------------------------------------------------
echo Step 1/3: Python Package Dependencies
echo ----------------------------------------------------------------------
set /p INSTALL_DEPS="Install / update required Python packages now? [Y/n]: "
if /i not "!INSTALL_DEPS!"=="n" (
    echo Upgrading pip and installing requirements...
    %PYCMD% -m pip install --upgrade pip
    %PYCMD% -m pip install -r "%~dp0requirements.txt"
    echo [OK] Dependencies installed!
)

echo.
echo ----------------------------------------------------------------------
echo Step 2/3: Windows Defender Firewall Configuration
echo ----------------------------------------------------------------------
echo OmniDesk requires inbound connections on Port 8765 (TCP) and 8766 (UDP).
set /p ALLOW_FW="Configure Windows Defender Firewall rules now? [Y/n]: "
if /i not "!ALLOW_FW!"=="n" (
    echo Adding firewall rules (requires Administrator privileges)...
    netsh advfirewall firewall add rule name="OmniDesk WebSocket" dir=in action=allow protocol=TCP localport=8765 >nul 2>&1
    netsh advfirewall firewall add rule name="OmniDesk Discovery" dir=in action=allow protocol=UDP localport=8766 >nul 2>&1
    echo [OK] Firewall rules configured!
)

echo.
echo ----------------------------------------------------------------------
echo Step 3/4: Desktop Shortcut
echo ----------------------------------------------------------------------
set /p MAKE_SHORTCUT="Create 'Start OmniDesk.bat' on your Desktop? [Y/n]: "
if /i not "!MAKE_SHORTCUT!"=="n" (
    if exist "%USERPROFILE%\Desktop" (
        (
            echo @echo off
            echo title OmniDesk Server
            echo cd /d "%~dp0"
            echo call start-background.bat
        ) > "%USERPROFILE%\Desktop\Start OmniDesk.bat"
        echo [OK] Created 'Start OmniDesk.bat' on your Desktop!
    )
)

echo.
echo ----------------------------------------------------------------------
echo Step 4/4: Auto-Start on Windows Logon (Silent Background)
echo ----------------------------------------------------------------------
set /p DO_AUTOSTART="Start OmniDesk automatically in background when Windows starts? [Y/n]: "
if /i not "!DO_AUTOSTART!"=="n" (
    call "%~dp0autostart-windows.bat" enable
)

echo.
echo ======================================================================
echo   🎉 Installation Complete!
echo   • Run in Background: start-background.bat (Safe to close window)
echo   • Stop Server:       stop-windows.bat
echo   • Check Status:      status-windows.bat
echo ======================================================================
echo.
set /p START_NOW="Launch OmniDesk Server in the background right now? [Y/n]: "
if /i not "!START_NOW!"=="n" (
    call "%~dp0start-background.bat"
)

pause
