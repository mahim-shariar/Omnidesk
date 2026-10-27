# OmniDesk Companion Server - PowerShell Guided Installer
# Run: powershell -ExecutionPolicy Bypass -File .\install.ps1

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "     ⚡ OmniDesk Companion Server - Windows PowerShell Wizard ⚡" -ForegroundColor Green
Write-Host "     Prompt-by-prompt guided setup with administrator firewall setup" -ForegroundColor Gray
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host ""

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# 1. Check Python
$pythonCmd = $null
if (Get-Command python -ErrorAction SilentlyContinue) {
    $pythonCmd = "python"
} elseif (Get-Command py -ErrorAction SilentlyContinue) {
    $pythonCmd = "py"
}

if (-not $pythonCmd) {
    Write-Host "[!] Python 3 was not found on your PC." -ForegroundColor Yellow
    $installWinget = Read-Host "Install Python 3.11 automatically via winget? [Y/n]"
    if ($installWinget -ne "n" -and $installWinget -ne "N") {
        Write-Host "Installing Python 3.11..." -ForegroundColor Cyan
        winget install Python.Python.3.11
        Write-Host "Python installed! Please restart PowerShell and run install.ps1 again." -ForegroundColor Green
        exit 0
    } else {
        Write-Host "Please install Python from https://www.python.org/downloads/ and check 'Add to PATH'." -ForegroundColor Yellow
        exit 1
    }
}

Write-Host "[OK] Using Python:" -ForegroundColor Green
& $pythonCmd --version

# 2. Check if install_wizard.py exists and execute it
$wizardPath = Join-Path $ScriptDir "install_wizard.py"
if (Test-Path $wizardPath) {
    Write-Host ""
    Write-Host "Launching OmniDesk Guided Installation Wizard..." -ForegroundColor Cyan
    & $pythonCmd $wizardPath
    exit $LASTEXITCODE
}

# Fallback PowerShell wizard
Write-Host ""
Write-Host "----------------------------------------------------------------------" -ForegroundColor Blue
Write-Host "[Step 1/3] Install Python Dependencies" -ForegroundColor Cyan
Write-Host "----------------------------------------------------------------------" -ForegroundColor Blue
$doDeps = Read-Host "Install / update Python requirements now? [Y/n]"
if ($doDeps -ne "n" -and $doDeps -ne "N") {
    & $pythonCmd -m pip install --upgrade pip
    & $pythonCmd -m pip install -r (Join-Path $ScriptDir "requirements.txt")
}

Write-Host ""
Write-Host "----------------------------------------------------------------------" -ForegroundColor Blue
Write-Host "[Step 2/3] Windows Firewall Configuration" -ForegroundColor Cyan
Write-Host "----------------------------------------------------------------------" -ForegroundColor Blue
$doFw = Read-Host "Configure Windows Defender Firewall rules (8765 TCP, 8766 UDP)? [Y/n]"
if ($doFw -ne "n" -and $doFw -ne "N") {
    $isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
    if ($isAdmin) {
        netsh advfirewall firewall add rule name="OmniDesk WebSocket" dir=in action=allow protocol=TCP localport=8765 | Out-Null
        netsh advfirewall firewall add rule name="OmniDesk Discovery" dir=in action=allow protocol=UDP localport=8766 | Out-Null
        Write-Host "[OK] Firewall rules configured!" -ForegroundColor Green
    } else {
        Write-Host "Elevating with administrator privileges..." -ForegroundColor Yellow
        Start-Process powershell -Verb RunAs -ArgumentList "-Command netsh advfirewall firewall add rule name='OmniDesk WebSocket' dir=in action=allow protocol=TCP localport=8765; netsh advfirewall firewall add rule name='OmniDesk Discovery' dir=in action=allow protocol=UDP localport=8766"
    }
}

Write-Host ""
Write-Host "----------------------------------------------------------------------" -ForegroundColor Blue
Write-Host "[Step 3/3] Start OmniDesk Server" -ForegroundColor Cyan
Write-Host "----------------------------------------------------------------------" -ForegroundColor Blue
$startNow = Read-Host "Launch OmniDesk Server right now? [Y/n]"
if ($startNow -ne "n" -and $startNow -ne "N") {
    Set-Location $ScriptDir
    & $pythonCmd (Join-Path $ScriptDir "server.py")
}
