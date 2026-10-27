#!/usr/bin/env python3
"""
OmniDesk Companion Server - Interactive Cross-Platform Installation Wizard
Prompt-by-prompt guided setup with smart suggestions for Windows, macOS, and Linux.
Designed for complete beginners with automatic permission configuration.
"""

import os
import sys
import platform
import shutil
import subprocess
import socket
import time
from pathlib import Path

# Terminal Colors & Styling
class Style:
    RESET = "\033[0m"
    BOLD = "\033[1m"
    DIM = "\033[2m"
    GREEN = "\033[92m"
    BLUE = "\033[94m"
    CYAN = "\033[96m"
    YELLOW = "\033[93m"
    RED = "\033[91m"
    MAGENTA = "\033[95m"

# Windows VT mode initialization
if platform.system() == "Windows":
    try:
        import ctypes
        kernel32 = ctypes.windll.kernel32
        kernel32.SetConsoleMode(kernel32.GetStdHandle(-11), 7)
    except Exception:
        pass

def get_base_dir() -> Path:
    return Path(__file__).resolve().parent

def get_local_ips() -> list:
    """Discovers all local non-loopback IPv4 addresses."""
    ips = []
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        primary = s.getsockname()[0]
        s.close()
        if primary and primary != "127.0.0.1":
            ips.append(primary)
    except Exception:
        pass

    try:
        import psutil
        for iface, addrs in psutil.net_if_addrs().items():
            for addr in addrs:
                if addr.family == socket.AF_INET and not addr.address.startswith("127."):
                    if addr.address not in ips:
                        ips.append(addr.address)
    except Exception:
        pass

    return ips or ["127.0.0.1"]

def print_banner():
    os_name = platform.system()
    os_dist = ""
    if os_name == "Linux":
        try:
            with open("/etc/os-release") as f:
                for line in f:
                    if line.startswith("PRETTY_NAME="):
                        os_dist = line.split("=")[1].strip().strip('"')
                        break
        except Exception:
            os_dist = "Linux"
    elif os_name == "Darwin":
        os_dist = f"macOS {platform.mac_ver()[0]}"
    elif os_name == "Windows":
        os_dist = f"Windows {platform.release()}"

    ips = get_local_ips()

    print(f"\n{Style.CYAN}{'='*68}{Style.RESET}")
    print(f"{Style.BOLD}{Style.GREEN}   ⚡ OmniDesk Universal PC Remote - Guided Installation Wizard ⚡{Style.RESET}")
    print(f"{Style.DIM}   Beginner-friendly step-by-step setup with smart suggested defaults{Style.RESET}")
    print(f"{Style.CYAN}{'='*68}{Style.RESET}")
    print(f" {Style.BOLD}Detected System :{Style.RESET} {Style.YELLOW}{os_dist or os_name}{Style.RESET} ({platform.machine()})")
    print(f" {Style.BOLD}Python Version  :{Style.RESET} {platform.python_version()} ({sys.executable})")
    print(f" {Style.BOLD}PC Hostname     :{Style.RESET} {socket.gethostname()}")
    print(f" {Style.BOLD}Local Network IP:{Style.RESET} {Style.CYAN}{', '.join(ips)}{Style.RESET}")
    print(f"{Style.CYAN}{'-'*68}{Style.RESET}")
    print(f"{Style.DIM}💡 Tip: Press ENTER to accept the highlighted [Y/n] suggested default at each step!{Style.RESET}\n")

def ask_prompt(question: str, default: bool = True, hint: str = "") -> bool:
    """Prompts user with a clear [Y/n] choice where ENTER accepts the suggested default."""
    default_str = "Y/n" if default else "y/N"
    hint_str = f" {Style.DIM}({hint}){Style.RESET}" if hint else ""
    prompt_tag = f"[{Style.GREEN if default else Style.DIM}{default_str}{Style.RESET}]"
    while True:
        try:
            response = input(f" {Style.BOLD}👉 {question}{Style.RESET} {prompt_tag}{hint_str}: ").strip().lower()
            if not response:
                return default
            if response in ("y", "yes"):
                return True
            if response in ("n", "no"):
                return False
            print(f"   {Style.YELLOW}Please type 'y' for Yes or 'n' for No, then press Enter.{Style.RESET}")
        except (KeyboardInterrupt, EOFError):
            print(f"\n\n{Style.YELLOW}Installation cancelled by user. You can re-run this setup anytime!{Style.RESET}")
            sys.exit(0)

def step_header(num: int, total: int, title: str, subtitle: str = ""):
    print(f"\n{Style.BLUE}{'━'*68}{Style.RESET}")
    print(f"{Style.BOLD}{Style.CYAN}[Step {num}/{total}]{Style.RESET} {Style.BOLD}{title}{Style.RESET}")
    if subtitle:
        print(f"{Style.DIM}       {subtitle}{Style.RESET}")
    print(f"{Style.BLUE}{'━'*68}{Style.RESET}")

def suggest_next(next_title: str):
    print(f"\n{Style.DIM}👉 Next suggested step: {Style.BOLD}{next_title}{Style.RESET}")

def run_cmd(cmd, check=True, capture=False):
    """Executes a command safely with proper error printing."""
    shell = isinstance(cmd, str)
    try:
        if capture:
            res = subprocess.run(cmd, shell=shell, check=check, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            return res.stdout.strip()
        else:
            return subprocess.run(cmd, shell=shell, check=check)
    except subprocess.CalledProcessError as e:
        if check:
            raise e
        return None

def is_admin() -> bool:
    """Checks if running with administrator or root privileges."""
    if platform.system() == "Windows":
        try:
            import ctypes
            return ctypes.windll.shell32.IsUserAnAdmin() != 0
        except Exception:
            return False
    else:
        return os.geteuid() == 0

# -------------------------------------------------------------
# OS-Specific Permission & Configuration Handlers
# -------------------------------------------------------------

def setup_windows_permissions():
    """Windows Defender Firewall and permission checks."""
    print(f"\n{Style.DIM}Windows Firewall must allow incoming connections on:{Style.RESET}")
    print(f" • {Style.BOLD}Port 8765 TCP{Style.RESET} (WebSocket Remote Control)")
    print(f" • {Style.BOLD}Port 8766 UDP{Style.RESET} (Automatic Wi-Fi Device Discovery)")

    if is_admin():
        if ask_prompt("Configure Windows Firewall rules automatically now?", default=True, hint="Recommended for Wi-Fi discovery"):
            print(f"   {Style.YELLOW}Applying Windows Defender Firewall rules...{Style.RESET}")
            cmd_tcp = 'netsh advfirewall firewall add rule name="OmniDesk WebSocket" dir=in action=allow protocol=TCP localport=8765'
            cmd_udp = 'netsh advfirewall firewall add rule name="OmniDesk Discovery" dir=in action=allow protocol=UDP localport=8766'
            try:
                subprocess.run(cmd_tcp, shell=True, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                subprocess.run(cmd_udp, shell=True, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                print(f"   {Style.GREEN}✔ Windows Firewall rules created successfully!{Style.RESET}")
            except Exception as e:
                print(f"   {Style.YELLOW}⚠ Could not add rules automatically: {e}{Style.RESET}")
    else:
        print(f"\n   {Style.YELLOW}ℹ Notice: Administrator privileges are needed to add firewall rules.{Style.RESET}")
        if ask_prompt("Request Administrator permission to add firewall rules?", default=True, hint="Will trigger Windows UAC prompt"):
            ps_script = (
                'netsh advfirewall firewall add rule name=\\"OmniDesk WebSocket\\" dir=in action=allow protocol=TCP localport=8765; '
                'netsh advfirewall firewall add rule name=\\"OmniDesk Discovery\\" dir=in action=allow protocol=UDP localport=8766'
            )
            elevate_cmd = f'powershell -Command "Start-Process powershell -ArgumentList \'-Command {ps_script}\' -Verb RunAs"'
            try:
                subprocess.run(elevate_cmd, shell=True)
                print(f"   {Style.GREEN}✔ Firewall configuration prompt completed!{Style.RESET}")
            except Exception:
                print(f"   {Style.YELLOW}⚠ You can manually run this command in an Admin PowerShell later:{Style.RESET}")
                print(f"     netsh advfirewall firewall add rule name=\"OmniDesk\" dir=in action=allow protocol=TCP localport=8765")
        else:
            print(f"   {Style.DIM}Skipped automatic firewall configuration. If the phone cannot connect, allow ports 8765 and 8766 in Windows Firewall.{Style.RESET}")

def setup_macos_permissions():
    """macOS Accessibility and Input Monitoring permissions."""
    print(f"\n{Style.DIM}macOS requires system permissions to control mouse & keyboard from your phone:{Style.RESET}")
    print(f" 1. {Style.BOLD}Accessibility{Style.RESET}       : Allows moving the mouse pointer and clicking")
    print(f" 2. {Style.BOLD}Input Monitoring{Style.RESET}    : Allows keyboard typing & hotkey shortcuts")

    print(f"\n{Style.YELLOW}📌 Instructions for macOS:{Style.RESET}")
    print(f" When System Settings opens, toggle the switch next to {Style.BOLD}Terminal{Style.RESET}, {Style.BOLD}iTerm{Style.RESET}, or {Style.BOLD}Python{Style.RESET}.")

    if ask_prompt("Open macOS System Settings to Accessibility right now?", default=True, hint="Opens Privacy & Security"):
        print(f"   {Style.YELLOW}Opening System Settings > Privacy & Security > Accessibility...{Style.RESET}")
        try:
            subprocess.run(["open", "x-apple.systempreferences:com.apple.preference.security?Privacy_Accessibility"])
            print(f"   {Style.GREEN}✔ Settings opened! Please enable the toggle for Terminal / Python.{Style.RESET}")
        except Exception:
            print(f"   {Style.YELLOW}Please open manually: System Settings -> Privacy & Security -> Accessibility{Style.RESET}")

    if ask_prompt("Also open Input Monitoring settings?", default=True, hint="Needed for full keyboard control"):
        try:
            subprocess.run(["open", "x-apple.systempreferences:com.apple.preference.security?Privacy_ListenEvent"])
            print(f"   {Style.GREEN}✔ Input Monitoring settings opened!{Style.RESET}")
        except Exception:
            pass

    # Quick permission check test
    if ask_prompt("Test mouse control now to confirm permission is active?", default=True, hint="Will gently nudge mouse cursor by 2 pixels"):
        try:
            import pyautogui
            cur_x, cur_y = pyautogui.position()
            pyautogui.moveTo(cur_x + 2, cur_y + 2, duration=0.1)
            pyautogui.moveTo(cur_x, cur_y, duration=0.1)
            print(f"   {Style.GREEN}✔ Mouse simulation test passed! Permissions are functioning.{Style.RESET}")
        except Exception as e:
            print(f"   {Style.YELLOW}⚠ Test result: {e}{Style.RESET}")
            print(f"   {Style.DIM}If the mouse didn't move, make sure to enable Terminal in Accessibility, then restart your terminal.{Style.RESET}")

def setup_linux_permissions():
    """Linux desktop tools, X11/Wayland detection, firewall, and uinput rules."""
    # Detect Display Server
    session_type = os.environ.get("XDG_SESSION_TYPE", "").lower()
    print(f" Display Server detected: {Style.YELLOW}{session_type or 'unknown'}{Style.RESET}")
    if "wayland" in session_type:
        print(f" {Style.YELLOW}ℹ Notice: Wayland is active on this session.{Style.RESET}")
        print(f"   OmniDesk includes Wayland support, but for 100% full mouse & game injection,")
        print(f"   an {Style.BOLD}X11 / Xorg session{Style.RESET} (selectable at the Linux login screen gear icon) is recommended.")

    # Package manager detection
    pkg_mgr = None
    if shutil.which("apt"):
        pkg_mgr = "apt"
    elif shutil.which("dnf"):
        pkg_mgr = "dnf"
    elif shutil.which("pacman"):
        pkg_mgr = "pacman"
    elif shutil.which("zypper"):
        pkg_mgr = "zypper"

    print(f"\n{Style.DIM}Linux desktop companion tools:{Style.RESET}")
    print(f" • {Style.BOLD}xdotool{Style.RESET}          : Smooth mouse pointer movement & clicks")
    print(f" • {Style.BOLD}playerctl{Style.RESET}        : Spotify, YouTube, VLC media control")
    print(f" • {Style.BOLD}xclip{Style.RESET}            : Seamless 2-way clipboard sync with Android")
    print(f" • {Style.BOLD}pulseaudio-utils{Style.RESET} : Volume control & mute")
    print(f" • {Style.BOLD}avahi-daemon{Style.RESET}     : Zero-configuration automatic Wi-Fi discovery")

    if pkg_mgr:
        if ask_prompt(f"Install Linux desktop tools using '{pkg_mgr}' package manager?", default=True, hint="Requires sudo"):
            try:
                if pkg_mgr == "apt":
                    cmd = "sudo apt update && sudo apt install -y xdotool playerctl libnotify-bin pulseaudio-utils xclip avahi-utils avahi-daemon"
                elif pkg_mgr == "dnf":
                    cmd = "sudo dnf install -y xdotool playerctl libnotify pulseaudio-utils xclip avahi-tools avahi"
                elif pkg_mgr == "pacman":
                    cmd = "sudo pacman -S --noconfirm xdotool playerctl libnotify pulseaudio xclip avahi"
                elif pkg_mgr == "zypper":
                    cmd = "sudo zypper install -y xdotool playerctl libnotify-tools pulseaudio-utils xclip avahi"
                subprocess.run(cmd, shell=True)
                print(f"   {Style.GREEN}✔ Linux desktop tools installed!{Style.RESET}")
            except Exception as e:
                print(f"   {Style.YELLOW}⚠ Could not install system packages automatically: {e}{Style.RESET}")

    # UFW Firewall
    if shutil.which("ufw"):
        ufw_status = run_cmd("sudo ufw status 2>/dev/null || true", capture=True)
        if "Status: active" in ufw_status:
            print(f"\n {Style.DIM}UFW Firewall is currently active.{Style.RESET}")
            if ask_prompt("Allow OmniDesk ports in UFW firewall (8765/tcp, 8766/udp, 5353/udp)?", default=True):
                subprocess.run("sudo ufw allow 8765/tcp comment 'OmniDesk WebSocket' 2>/dev/null || true", shell=True)
                subprocess.run("sudo ufw allow 8766/udp comment 'OmniDesk Discovery' 2>/dev/null || true", shell=True)
                subprocess.run("sudo ufw allow 5353/udp comment 'mDNS Zeroconf' 2>/dev/null || true", shell=True)
                print(f"   {Style.GREEN}✔ UFW firewall rules added!{Style.RESET}")

    # Gamepad /dev/uinput permissions
    if os.path.exists("/dev/uinput"):
        print(f"\n {Style.DIM}Virtual Gamepad uses Linux /dev/uinput kernel module.{Style.RESET}")
        if ask_prompt("Configure /dev/uinput permissions for virtual gamepad without root?", default=True, hint="Adds user to 'input' group"):
            try:
                user = os.environ.get("USER", os.environ.get("LOGNAME", ""))
                rule = 'KERNEL=="uinput", MODE="0660", GROUP="input", OPTIONS+="static_node=uinput"'
                cmd = f"sudo usermod -aG input {user} 2>/dev/null && echo '{rule}' | sudo tee /etc/udev/rules.d/99-omnidesk-uinput.rules > /dev/null && sudo udevadm control --reload-rules && sudo udevadm trigger"
                subprocess.run(cmd, shell=True)
                print(f"   {Style.GREEN}✔ Virtual gamepad uinput rules configured for user '{user}'!{Style.RESET}")
            except Exception:
                print(f"   {Style.YELLOW}⚠ Optional uinput configuration skipped.{Style.RESET}")

    # Avahi mDNS Daemon
    if shutil.which("systemctl"):
        try:
            res = subprocess.run(["systemctl", "is-active", "--quiet", "avahi-daemon"])
            if res.returncode != 0:
                if ask_prompt("Enable and start Avahi mDNS daemon for automatic Wi-Fi discovery?", default=True):
                    subprocess.run("sudo systemctl enable --now avahi-daemon 2>/dev/null || true", shell=True)
                    print(f"   {Style.GREEN}✔ Avahi discovery daemon active!{Style.RESET}")
        except Exception:
            pass

# -------------------------------------------------------------
# Auto-Start & Launcher Handlers
# -------------------------------------------------------------

def setup_autostart(base_dir: Path, python_exe: str):
    """Configures automatic start on system boot or user login."""
    os_name = platform.system()
    server_py = base_dir / "server.py"

    if os_name == "Windows":
        startup_dir = Path(os.environ.get("APPDATA", "")) / "Microsoft" / "Windows" / "Start Menu" / "Programs" / "Startup"
        if startup_dir.exists():
            vbs_file = startup_dir / "OmniDesk_Silent.vbs"
            vbs_content = f'''Set WshShell = CreateObject("WScript.Shell")
WshShell.CurrentDirectory = "{base_dir}"
WshShell.Run "wscript.exe """ & "{base_dir}\\run-invisible.vbs""" & """", 0, False
'''
            try:
                vbs_file.write_text(vbs_content)
                print(f"   {Style.GREEN}✔ Silent background startup entry created in Windows Startup folder!{Style.RESET}")
                print(f"     {Style.DIM}({vbs_file}){Style.RESET}")
            except Exception as e:
                print(f"   {Style.YELLOW}⚠ Could not create startup entry: {e}{Style.RESET}")

        # Also register Scheduled Task for logon resilience
        try:
            cmd = f'schtasks /create /tn "OmniDeskServer" /tr "wscript.exe \\"{base_dir}\\run-invisible.vbs\\"" /sc onlogon /rl highest /f'
            subprocess.run(cmd, shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            print(f"   {Style.GREEN}✔ Registered Windows Scheduled Task 'OmniDeskServer' (Logon Autostart){Style.RESET}")
        except Exception:
            pass

    elif os_name == "Darwin":
        launch_agents = Path.home() / "Library" / "LaunchAgents"
        launch_agents.mkdir(parents=True, exist_ok=True)
        logs_dir = Path.home() / "Library" / "Logs"
        logs_dir.mkdir(parents=True, exist_ok=True)
        plist_path = launch_agents / "com.omnidesk.server.plist"
        plist_content = f"""<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.omnidesk.server</string>
    <key>ProgramArguments</key>
    <array>
        <string>{python_exe}</string>
        <string>{server_py}</string>
    </array>
    <key>WorkingDirectory</key>
    <string>{base_dir}</string>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
    <key>ProcessType</key>
    <string>Interactive</string>
    <key>StandardOutPath</key>
    <string>{logs_dir}/OmniDesk.log</string>
    <key>StandardErrorPath</key>
    <string>{logs_dir}/OmniDesk-error.log</string>
    <key>EnvironmentVariables</key>
    <dict>
        <key>PATH</key>
        <string>/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin</string>
    </dict>
</dict>
</plist>
"""
        try:
            plist_path.write_text(plist_content)
            # Unload old plist if loaded, then bootstrap/load and start
            run_cmd(f"launchctl bootout gui/$(id -u) '{plist_path}' 2>/dev/null || launchctl unload -w '{plist_path}' 2>/dev/null || true")
            run_cmd(f"launchctl bootstrap gui/$(id -u) '{plist_path}' 2>/dev/null || launchctl load -w '{plist_path}' 2>/dev/null || true")
            run_cmd(f"launchctl kickstart -k gui/$(id -u)/com.omnidesk.server 2>/dev/null || launchctl start com.omnidesk.server 2>/dev/null || true")
            print(f"   {Style.GREEN}✔ macOS LaunchAgent installed & activated ({plist_path})!{Style.RESET}")
            print(f"   {Style.CYAN}ℹ OmniDesk is now running silently in the background.{Style.RESET}")
            print(f"     Closing the terminal will NOT stop the server.{Style.RESET}")
            print(f"     Logs: {logs_dir}/OmniDesk.log")
        except Exception as e:
            print(f"   {Style.YELLOW}⚠ Could not create LaunchAgent: {e}{Style.RESET}")

    elif os_name == "Linux":
        systemd_dir = Path.home() / ".config" / "systemd" / "user"
        systemd_dir.mkdir(parents=True, exist_ok=True)
        service_file = systemd_dir / "omnidesk.service"
        service_content = f"""[Unit]
Description=OmniDesk Companion Server
After=network.target

[Service]
Type=simple
Environment="DISPLAY=:0"
WorkingDirectory={base_dir}
ExecStart={python_exe} {server_py}
Restart=on-failure
RestartSec=3

[Install]
WantedBy=default.target
"""
        try:
            service_file.write_text(service_content)
            run_cmd("systemctl --user daemon-reload 2>/dev/null || true")
            run_cmd("systemctl --user enable omnidesk.service 2>/dev/null || true")
            run_cmd("systemctl --user restart omnidesk.service 2>/dev/null || true")
            print(f"   {Style.GREEN}✔ Systemd user service enabled and started!{Style.RESET}")
            print(f"     Check status anytime with: {Style.BOLD}systemctl --user status omnidesk.service{Style.RESET}")
        except Exception as e:
            print(f"   {Style.YELLOW}⚠ Could not enable systemd service: {e}{Style.RESET}")

def create_desktop_shortcut(base_dir: Path, python_exe: str):
    """Creates a convenient double-clickable launcher on the user's desktop."""
    os_name = platform.system()
    server_py = base_dir / "server.py"

    if os_name == "Windows":
        desktop = Path(os.environ.get("USERPROFILE", "")) / "Desktop"
        if desktop.exists():
            bat = desktop / "Start OmniDesk.bat"
            bat.write_text(f'@echo off\ntitle OmniDesk PC Server\ncd /d "{base_dir}"\n"{python_exe}" "{server_py}"\npause\n')
            print(f"   {Style.GREEN}✔ Shortcut 'Start OmniDesk.bat' created on your Desktop!{Style.RESET}")
    elif os_name == "Linux":
        desktop = Path.home() / "Desktop"
        if desktop.exists():
            shortcut = desktop / "omnidesk.desktop"
            content = f"""[Desktop Entry]
Type=Application
Name=OmniDesk Server
Comment=Control your PC from Android
Exec={python_exe} {server_py}
Path={base_dir}
Terminal=true
Categories=Utility;Network;
"""
            shortcut.write_text(content)
            try:
                shortcut.chmod(0o755)
            except Exception:
                pass
            print(f"   {Style.GREEN}✔ Desktop launcher 'omnidesk.desktop' created on your Desktop!{Style.RESET}")
    elif os_name == "Darwin":
        desktop = Path.home() / "Desktop"
        if desktop.exists():
            command_file = desktop / "Start OmniDesk.command"
            command_file.write_text(f"""#!/bin/bash
cd "{base_dir}"
"{python_exe}" "{server_py}"
""")
            try:
                command_file.chmod(0o755)
            except Exception:
                pass
            print(f"   {Style.GREEN}✔ Clickable launcher 'Start OmniDesk.command' created on your Desktop!{Style.RESET}")

# -------------------------------------------------------------
# Main Guided Installation Flow
# -------------------------------------------------------------

def main():
    print_banner()
    base_dir = get_base_dir()
    total_steps = 6

    # =========================================================
    # STEP 1: Python Environment
    # =========================================================
    step_header(1, total_steps, "Python Virtual Environment Setup", "Keeps PC Python packages clean and isolated")
    print(f" Installation folder: {Style.YELLOW}{base_dir}{Style.RESET}")
    
    use_venv = ask_prompt(
        "Create an isolated Virtual Environment (.venv)?",
        default=True,
        hint="Recommended — prevents package conflicts"
    )
    
    python_exe = sys.executable
    venv_dir = base_dir / ".venv"

    if use_venv:
        if not venv_dir.exists():
            print(f"   {Style.BLUE}Creating virtual environment at {venv_dir}...{Style.RESET}")
            try:
                subprocess.run([sys.executable, "-m", "venv", str(venv_dir)], check=True)
                print(f"   {Style.GREEN}✔ Virtual environment created successfully!{Style.RESET}")
            except Exception as e:
                print(f"   {Style.YELLOW}⚠ Could not create venv ({e}); using system Python.{Style.RESET}")
        
        if platform.system() == "Windows":
            venv_python = venv_dir / "Scripts" / "python.exe"
        else:
            venv_python = venv_dir / "bin" / "python3"
            if not venv_python.exists():
                venv_python = venv_dir / "bin" / "python"
        
        if venv_python.exists():
            python_exe = str(venv_python)
            print(f"   Using interpreter: {Style.GREEN}{python_exe}{Style.RESET}")

    suggest_next("Install Python module dependencies (websockets, pyautogui, pynput, zeroconf, qrcode)")

    # =========================================================
    # STEP 2: Python Dependencies
    # =========================================================
    step_header(2, total_steps, "Install Required Python Packages", "Installs remote control, discovery, and networking libraries")
    req_file = base_dir / "requirements.txt"
    print(f" Packages to install: {Style.DIM}websockets, psutil, pyautogui, pynput, zeroconf, qrcode{Style.RESET}")
    
    if ask_prompt("Install or update required packages now?", default=True, hint="Downloads from PyPI"):
        print(f"   {Style.YELLOW}Installing packages using pip...{Style.RESET}")
        try:
            subprocess.run([python_exe, "-m", "pip", "install", "--upgrade", "pip"], check=False, stdout=subprocess.DEVNULL)
        except Exception:
            pass

        install_cmd = [python_exe, "-m", "pip", "install", "-r", str(req_file)]
        try:
            subprocess.run(install_cmd, check=True)
            print(f"   {Style.GREEN}✔ All Python dependencies installed successfully!{Style.RESET}")
        except subprocess.CalledProcessError:
            print(f"   {Style.YELLOW}Retrying with --break-system-packages (for Debian 12 / Ubuntu 24.04+)...{Style.RESET}")
            subprocess.run([python_exe, "-m", "pip", "install", "--break-system-packages", "-r", str(req_file)], check=False)

    suggest_next("Configure operating system permissions & firewall rules")

    # =========================================================
    # STEP 3: OS Permissions & Firewall
    # =========================================================
    os_name = platform.system()
    step_header(3, total_steps, f"{os_name} Permissions & Firewall", "Ensures phone can connect and PC accepts remote inputs")
    if os_name == "Windows":
        setup_windows_permissions()
    elif os_name == "Darwin":
        setup_macos_permissions()
    elif os_name == "Linux":
        setup_linux_permissions()

    suggest_next("Configure Auto-Start so the server starts automatically when PC boots")

    # =========================================================
    # STEP 4: Auto-Start on Boot / Login
    # =========================================================
    step_header(4, total_steps, "Auto-Start Configuration", "Allows OmniDesk to run in the background whenever PC starts")
    enable_auto = ask_prompt("Enable Auto-Start when this PC boots or logs in?", default=True, hint="No need to run terminal manually")
    if enable_auto:
        setup_autostart(base_dir, python_exe)
    else:
        print(f"   {Style.DIM}Auto-start skipped. You can start the server manually whenever needed.{Style.RESET}")

    suggest_next("Create a convenient Desktop shortcut")

    # =========================================================
    # STEP 5: Desktop Shortcut
    # =========================================================
    step_header(5, total_steps, "Desktop Launcher Shortcut", "Puts a double-clickable launcher on your Desktop")
    make_shortcut = ask_prompt("Create a Desktop launcher shortcut?", default=True, hint="Double-click to start anytime")
    if make_shortcut:
        create_desktop_shortcut(base_dir, python_exe)
    else:
        print(f"   {Style.DIM}Desktop shortcut skipped.{Style.RESET}")

    suggest_next("Launch OmniDesk server now and connect with your Android phone")

    # =========================================================
    # STEP 6: Completion & Server Launch
    # =========================================================
    step_header(6, total_steps, "Launch OmniDesk Companion Server", "Displays QR Code and 6-digit PIN on this screen")
    
    ips = get_local_ips()
    print(f"\n{Style.CYAN}{'='*68}{Style.RESET}")
    print(f"{Style.BOLD}{Style.GREEN}   🎉 OmniDesk Companion Server Setup Complete! 🎉{Style.RESET}")
    print(f"{Style.CYAN}{'='*68}{Style.RESET}")
    print(f" {Style.BOLD}How to Connect from Your Android Phone:{Style.RESET}")
    print(f" 1. Ensure your phone and this PC are on the {Style.BOLD}same Wi-Fi network{Style.RESET} (or connected via USB cable)")
    print(f" 2. Open the {Style.BOLD}OmniDesk app{Style.RESET} on your phone")
    print(f" 3. Tap {Style.GREEN}Scan Wi-Fi{Style.RESET} (it will auto-detect this PC), or tap {Style.GREEN}Scan QR Code{Style.RESET}")
    print(f" 4. Enter the 6-digit PIN shown on this screen to pair securely!\n")
    print(f" {Style.DIM}Server Address : {Style.BOLD}ws://{ips[0]}:8765{Style.RESET}")
    print(f" {Style.DIM}Manual Run CMD : {Style.BOLD}{python_exe} server.py{Style.RESET}\n")

    # Check if server is already running in background
    is_running_background = False
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(0.5)
        res = s.connect_ex(("127.0.0.1", 8765))
        s.close()
        if res == 0:
            is_running_background = True
    except Exception:
        pass

    if is_running_background:
        print(f" {Style.GREEN}● OmniDesk Server is already RUNNING in the background!{Style.RESET}")
        pin_file = Path.home() / ".config" / "omnidesk" / "current_pin.txt"
        if pin_file.exists():
            try:
                pin = pin_file.read_text().splitlines()[0].strip()
                print(f" {Style.BOLD}Current Pairing PIN: {Style.GREEN}[ {pin} ]{Style.RESET}")
            except Exception:
                pass
        print(f"\n {Style.BOLD}{Style.GREEN}✔ Setup complete! You can safely close this terminal window.{Style.RESET}")
        print(f" {Style.DIM}The server will continue running in the background.{Style.RESET}\n")
        return

    start_now = ask_prompt("🚀 Launch OmniDesk Companion Server right now?", default=True, hint="Starts live server with pairing PIN & QR code")
    if start_now:
        print(f"\n{Style.GREEN}Starting OmniDesk Server... (Press Ctrl+C to stop){Style.RESET}\n")
        server_py = base_dir / "server.py"
        try:
            subprocess.run([python_exe, str(server_py)])
        except KeyboardInterrupt:
            print(f"\n{Style.YELLOW}OmniDesk Server stopped.{Style.RESET}")

if __name__ == "__main__":
    main()
