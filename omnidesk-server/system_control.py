import glob
import logging
import os
import platform
import subprocess
import time
from typing import Dict, List
from config import SYSTEM_COMMANDS_WHITELIST

logger = logging.getLogger("OmniDeskSystem")
SYS_NAME = platform.system()  # "Linux", "Darwin" (macOS), "Windows"

class SystemControlManager:
    def execute_action(self, action: str) -> bool:
        act = action.lower().strip()
        if act not in SYSTEM_COMMANDS_WHITELIST:
            logger.warning(f"Rejected non-whitelisted system action: {action}")
            return False

        try:
            # ---------------------------------------------------------
            # 1. LOCK SYSTEM
            # ---------------------------------------------------------
            if act == "lock":
                if SYS_NAME == "Darwin":
                    # macOS lock: sleep display or lock screen via loginwindow
                    if subprocess.run(["pmset", "displaysleepnow"]).returncode == 0:
                        return True
                    return subprocess.run(["osascript", "-e", 'tell application "loginwindow" to «event aevtrlgo»']).returncode == 0
                elif SYS_NAME == "Windows":
                    # Windows lock workstation
                    return subprocess.run(["rundll32.exe", "user32.dll,LockWorkStation"]).returncode == 0
                else:
                    # Linux lock
                    if subprocess.run(["cinnamon-screensaver-command", "-l"]).returncode == 0:
                        return True
                    if subprocess.run(["xdg-screensaver", "lock"]).returncode == 0:
                        return True
                    return subprocess.run(["loginctl", "lock-session"]).returncode == 0

            # ---------------------------------------------------------
            # 2. SUSPEND / SLEEP
            # ---------------------------------------------------------
            elif act == "suspend":
                if SYS_NAME == "Darwin":
                    subprocess.Popen(["pmset", "sleepnow"])
                    return True
                elif SYS_NAME == "Windows":
                    subprocess.Popen(["rundll32.exe", "powrprof.dll,SetSuspendState", "0,1,0"])
                    return True
                else:
                    subprocess.Popen(["systemctl", "suspend"])
                    return True

            # ---------------------------------------------------------
            # 3. RESTART
            # ---------------------------------------------------------
            elif act == "restart":
                if SYS_NAME == "Darwin":
                    subprocess.Popen(["osascript", "-e", 'tell application "System Events" to restart'])
                    return True
                elif SYS_NAME == "Windows":
                    subprocess.Popen(["shutdown", "/r", "/t", "0"])
                    return True
                else:
                    subprocess.Popen(["systemctl", "reboot"])
                    return True

            # ---------------------------------------------------------
            # 4. SHUTDOWN
            # ---------------------------------------------------------
            elif act == "shutdown":
                if SYS_NAME == "Darwin":
                    subprocess.Popen(["osascript", "-e", 'tell application "System Events" to shut down'])
                    return True
                elif SYS_NAME == "Windows":
                    subprocess.Popen(["shutdown", "/s", "/t", "0"])
                    return True
                else:
                    subprocess.Popen(["systemctl", "poweroff"])
                    return True

            # ---------------------------------------------------------
            # 5. SHOW DESKTOP
            # ---------------------------------------------------------
            elif act == "show_desktop":
                if SYS_NAME == "Darwin":
                    # Mission Control / Show Desktop keycode (F11 is keycode 103)
                    res = subprocess.run(["osascript", "-e", 'tell application "System Events" to key code 103'])
                    if res.returncode == 0:
                        return True
                    try:
                        import pyautogui
                        pyautogui.hotkey("command", "f3")
                        return True
                    except Exception:
                        return False
                elif SYS_NAME == "Windows":
                    try:
                        subprocess.run(["powershell", "-NoProfile", "-c", "(New-Object -ComObject Shell.Application).ToggleDesktop()"], timeout=2)
                        return True
                    except Exception:
                        try:
                            import pyautogui
                            pyautogui.hotkey("win", "d")
                            return True
                        except Exception:
                            return False
                else:
                    # Linux Cinnamon Super+d toggles desktop
                    return subprocess.run(["xdotool", "key", "super+d"]).returncode == 0

            # ---------------------------------------------------------
            # 6. SCREENSHOT
            # ---------------------------------------------------------
            elif act == "screenshot":
                if SYS_NAME == "Darwin":
                    # Capture screen to clipboard
                    subprocess.Popen(["screencapture", "-c"])
                    return True
                elif SYS_NAME == "Windows":
                    # Windows Screen Snipping Tool
                    subprocess.Popen(["cmd.exe", "/c", "start", "ms-screenclip:"])
                    return True
                else:
                    subprocess.Popen(["gnome-screenshot"])
                    return True

            # ---------------------------------------------------------
            # 7. OPEN TERMINAL
            # ---------------------------------------------------------
            elif act == "open_terminal":
                if SYS_NAME == "Darwin":
                    subprocess.Popen(["open", "-a", "Terminal"])
                    return True
                elif SYS_NAME == "Windows":
                    try:
                        subprocess.Popen(["wt.exe"])
                    except Exception:
                        subprocess.Popen(["cmd.exe", "/c", "start", "powershell.exe"])
                    return True
                else:
                    subprocess.Popen(["x-terminal-emulator"])
                    return True

            # ---------------------------------------------------------
            # 8. DISPLAY OFF (SCREEN SLEEP)
            # ---------------------------------------------------------
            elif act == "display_off":
                if SYS_NAME == "Darwin":
                    subprocess.run(["pmset", "displaysleepnow"])
                    return True
                elif SYS_NAME == "Windows":
                    subprocess.run(["powershell", "-NoProfile", "-c",
                                    "(Add-Type '[DllImport(\"user32.dll\")]public static extern int SendMessage(int hWnd, int hMsg, int wParam, int lParam);' -Name a -Pas)::SendMessage(-1,0x0112,0xF170,2)"],
                                   timeout=3)
                    return True
                else:
                    env = os.environ.copy()
                    if "DISPLAY" not in env or not env["DISPLAY"]:
                        env["DISPLAY"] = ":0"
                    subprocess.run(["xset", "dpms", "force", "off"], env=env)
                    return True

            # ---------------------------------------------------------
            # 9. DISPLAY ON
            # ---------------------------------------------------------
            elif act == "display_on":
                if SYS_NAME == "Darwin":
                    subprocess.run(["caffeinate", "-u", "-t", "2"])
                    return True
                elif SYS_NAME == "Windows":
                    try:
                        import pyautogui
                        pyautogui.moveRel(1, 1)
                        pyautogui.moveRel(-1, -1)
                        return True
                    except Exception:
                        return True
                else:
                    env = os.environ.copy()
                    if "DISPLAY" not in env or not env["DISPLAY"]:
                        env["DISPLAY"] = ":0"
                    subprocess.run(["xset", "dpms", "force", "on"], env=env)
                    return True

            # ---------------------------------------------------------
            # 10. SYSTEM MONITOR / TASK MANAGER
            # ---------------------------------------------------------
            elif act == "system_monitor":
                if SYS_NAME == "Darwin":
                    subprocess.Popen(["open", "-a", "Activity Monitor"])
                    return True
                elif SYS_NAME == "Windows":
                    subprocess.Popen(["taskmgr.exe"])
                    return True
                else:
                    try:
                        subprocess.Popen(["gnome-system-monitor"])
                        return True
                    except Exception:
                        subprocess.Popen(["x-terminal-emulator", "-e", "htop"])
                        return True

            # ---------------------------------------------------------
            # 11. DESKTOP SHELL RESTART
            # ---------------------------------------------------------
            elif act in ("cinnamon_restart", "explorer_restart", "finder_restart"):
                if SYS_NAME == "Darwin":
                    # Restart macOS Finder / Dock
                    subprocess.Popen(["killall", "Finder"])
                    return True
                elif SYS_NAME == "Windows":
                    # Restart Windows Explorer
                    subprocess.Popen(["cmd.exe", "/c", "taskkill /f /im explorer.exe && start explorer.exe"])
                    return True
                else:
                    # Restart Cinnamon desktop shell without closing running apps
                    subprocess.Popen(["pkill", "-HUP", "-f", "cinnamon"])
                    return True

        except Exception as e:
            logger.error(f"Error executing action {action}: {e}")
            return False
        return False

    def set_clipboard(self, text: str) -> bool:
        try:
            if SYS_NAME == "Darwin":
                p = subprocess.Popen(["pbcopy"], stdin=subprocess.PIPE)
                p.communicate(input=text.encode("utf-8"))
                return p.returncode == 0
            elif SYS_NAME == "Windows":
                # PowerShell Set-Clipboard
                p = subprocess.Popen(["powershell", "-NoProfile", "-c", "Set-Clipboard -Value $input"], stdin=subprocess.PIPE)
                p.communicate(input=text.encode("utf-8"))
                return p.returncode == 0
            else:
                p = subprocess.Popen(["xclip", "-selection", "clipboard"], stdin=subprocess.PIPE)
                p.communicate(input=text.encode("utf-8"))
                if p.returncode == 0:
                    return True
                p = subprocess.Popen(["xsel", "--clipboard", "--input"], stdin=subprocess.PIPE)
                p.communicate(input=text.encode("utf-8"))
                return p.returncode == 0
        except Exception as e:
            logger.debug(f"set_clipboard error: {e}")
        return False

    def get_clipboard(self) -> str:
        try:
            if SYS_NAME == "Darwin":
                res = subprocess.run(["pbpaste"], stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True, timeout=2)
                if res.returncode == 0:
                    return res.stdout
            elif SYS_NAME == "Windows":
                res = subprocess.run(["powershell", "-NoProfile", "-c", "Get-Clipboard"], stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True, timeout=2)
                if res.returncode == 0:
                    return res.stdout.strip()
            else:
                res = subprocess.run(["xclip", "-selection", "clipboard", "-o"],
                                     stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True, timeout=2)
                if res.returncode == 0:
                    return res.stdout
                res = subprocess.run(["xsel", "--clipboard", "--output"],
                                     stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True, timeout=2)
                if res.returncode == 0:
                    return res.stdout
        except Exception:
            pass
        return ""

    def schedule_shutdown(self, minutes: int) -> bool:
        try:
            mins = max(1, min(1440, int(minutes)))
            if SYS_NAME == "Windows":
                secs = mins * 60
                subprocess.Popen(["shutdown", "/s", "/t", str(secs)])
                return True
            elif SYS_NAME == "Darwin":
                subprocess.Popen(["sudo", "shutdown", "-s", f"+{mins}"])
                return True
            else:
                subprocess.Popen(["shutdown", f"+{mins}"])
                return True
        except Exception as e:
            logger.error(f"Error scheduling shutdown: {e}")
            return False

    def cancel_scheduled_shutdown(self) -> bool:
        try:
            if SYS_NAME == "Windows":
                subprocess.run(["shutdown", "/a"])
                return True
            elif SYS_NAME == "Darwin":
                subprocess.run(["sudo", "killall", "shutdown"])
                return True
            else:
                subprocess.run(["shutdown", "-c"])
                return True
        except Exception as e:
            logger.error(f"Error canceling shutdown: {e}")
            return False

    def list_applications(self) -> List[Dict[str, str]]:
        apps = []
        seen_names = set()

        if SYS_NAME == "Darwin":
            # macOS default applications
            defaults = [
                {"id": "safari", "name": "Safari", "icon": "web-browser", "exec": "Safari"},
                {"id": "chrome", "name": "Google Chrome", "icon": "google-chrome", "exec": "Google Chrome"},
                {"id": "terminal", "name": "Terminal", "icon": "utilities-terminal", "exec": "Terminal"},
                {"id": "finder", "name": "Finder", "icon": "system-file-manager", "exec": "Finder"},
                {"id": "vscode", "name": "Visual Studio Code", "icon": "code", "exec": "Visual Studio Code"},
                {"id": "notes", "name": "Notes", "icon": "accessories-text-editor", "exec": "Notes"},
                {"id": "calculator", "name": "Calculator", "icon": "accessories-calculator", "exec": "Calculator"},
                {"id": "settings", "name": "System Settings", "icon": "preferences-system", "exec": "System Settings"},
                {"id": "music", "name": "Music", "icon": "multimedia-audio-player", "exec": "Music"},
                {"id": "activity_monitor", "name": "Activity Monitor", "icon": "utilities-system-monitor", "exec": "Activity Monitor"}
            ]
            for item in defaults:
                apps.append(item)
                seen_names.add(item["name"].lower())

            # Scan /Applications and ~/Applications
            mac_dirs = ["/Applications", "/System/Applications", os.path.expanduser("~/Applications")]
            for m_dir in mac_dirs:
                if not os.path.exists(m_dir):
                    continue
                try:
                    for entry in os.listdir(m_dir):
                        if entry.endswith(".app"):
                            app_name = entry[:-4]
                            if app_name.lower() not in seen_names:
                                seen_names.add(app_name.lower())
                                apps.append({
                                    "id": app_name.lower().replace(" ", "-"),
                                    "name": app_name,
                                    "icon": "application-x-executable",
                                    "exec": app_name
                                })
                except Exception:
                    pass

            return apps[:40]

        elif SYS_NAME == "Windows":
            # Windows default applications
            defaults = [
                {"id": "chrome", "name": "Google Chrome", "icon": "google-chrome", "exec": "chrome"},
                {"id": "edge", "name": "Microsoft Edge", "icon": "web-browser", "exec": "msedge"},
                {"id": "notepad", "name": "Notepad", "icon": "accessories-text-editor", "exec": "notepad"},
                {"id": "calculator", "name": "Calculator", "icon": "accessories-calculator", "exec": "calc"},
                {"id": "explorer", "name": "File Explorer", "icon": "system-file-manager", "exec": "explorer"},
                {"id": "terminal", "name": "PowerShell / Terminal", "icon": "utilities-terminal", "exec": "powershell"},
                {"id": "taskmgr", "name": "Task Manager", "icon": "utilities-system-monitor", "exec": "taskmgr"},
                {"id": "settings", "name": "Settings", "icon": "preferences-system", "exec": "start ms-settings:"},
                {"id": "code", "name": "VS Code", "icon": "code", "exec": "code"},
                {"id": "spotify", "name": "Spotify", "icon": "multimedia-audio-player", "exec": "spotify"}
            ]
            for item in defaults:
                apps.append(item)
                seen_names.add(item["name"].lower())

            # Scan Windows Start Menu
            win_dirs = [
                os.path.expandvars(r"%ProgramData%\Microsoft\Windows\Start Menu\Programs"),
                os.path.expandvars(r"%APPDATA%\Microsoft\Windows\Start Menu\Programs")
            ]
            for w_dir in win_dirs:
                if not os.path.exists(w_dir):
                    continue
                try:
                    for root, _, files in os.walk(w_dir):
                        for f in files:
                            if f.endswith(".lnk"):
                                lnk_name = f[:-4]
                                if lnk_name.lower() not in seen_names and not lnk_name.lower().startswith("uninstall"):
                                    seen_names.add(lnk_name.lower())
                                    apps.append({
                                        "id": lnk_name.lower().replace(" ", "-"),
                                        "name": lnk_name,
                                        "icon": "application-x-executable",
                                        "exec": os.path.join(root, f)
                                    })
                except Exception:
                    pass

            return apps[:40]

        else:
            # Linux application scanning
            search_dirs = [
                "/usr/share/applications",
                os.path.expanduser("~/.local/share/applications")
            ]

            defaults = [
                {"id": "firefox", "name": "Firefox", "icon": "firefox", "exec": "firefox"},
                {"id": "chrome", "name": "Google Chrome", "icon": "google-chrome", "exec": "google-chrome"},
                {"id": "terminal", "name": "Terminal", "icon": "utilities-terminal", "exec": "x-terminal-emulator"},
                {"id": "files", "name": "Nemo Files", "icon": "system-file-manager", "exec": "nemo"},
                {"id": "code", "name": "VS Code", "icon": "code", "exec": "code"},
                {"id": "vlc", "name": "VLC Media Player", "icon": "vlc", "exec": "vlc"},
                {"id": "text_editor", "name": "Text Editor", "icon": "accessories-text-editor", "exec": "xed"},
                {"id": "calculator", "name": "Calculator", "icon": "accessories-calculator", "exec": "gnome-calculator"},
                {"id": "settings", "name": "System Settings", "icon": "preferences-system", "exec": "cinnamon-settings"}
            ]

            for item in defaults:
                apps.append(item)
                seen_names.add(item["name"].lower())

            # Dynamically scan .desktop files
            try:
                for s_dir in search_dirs:
                    if not os.path.exists(s_dir):
                        continue
                    for fpath in glob.glob(os.path.join(s_dir, "*.desktop")):
                        try:
                            name, exec_cmd, icon, nodisplay = None, None, "application-x-executable", False
                            with open(fpath, "r", encoding="utf-8", errors="ignore") as f:
                                for line in f:
                                    line = line.strip()
                                    if line.startswith("Name=") and not name:
                                        name = line.split("=", 1)[1]
                                    elif line.startswith("Exec=") and not exec_cmd:
                                        exec_cmd = line.split("=", 1)[1].split("%")[0].strip()
                                    elif line.startswith("Icon=") and not icon:
                                        icon = line.split("=", 1)[1].strip()
                                    elif line.startswith("NoDisplay=true"):
                                        nodisplay = True
                            if name and exec_cmd and not nodisplay:
                                if name.lower() not in seen_names:
                                    seen_names.add(name.lower())
                                    apps.append({
                                        "id": os.path.basename(fpath).replace(".desktop", ""),
                                        "name": name,
                                        "icon": icon,
                                        "exec": exec_cmd
                                    })
                        except Exception:
                            continue
            except Exception as e:
                logger.debug(f"Error scanning .desktop files: {e}")

            return apps[:40]

    def launch_app(self, app_identifier: str) -> bool:
        if not app_identifier:
            return False

        if SYS_NAME == "Darwin":
            try:
                subprocess.Popen(["open", "-a", app_identifier])
                logger.info(f"Launched macOS application: {app_identifier}")
                return True
            except Exception:
                try:
                    subprocess.Popen(["open", app_identifier])
                    return True
                except Exception as e:
                    logger.error(f"Failed to launch macOS app {app_identifier}: {e}")
                    return False

        elif SYS_NAME == "Windows":
            try:
                if hasattr(os, "startfile") and os.path.exists(app_identifier):
                    os.startfile(app_identifier)
                    return True
                subprocess.Popen(["cmd.exe", "/c", "start", "", app_identifier], shell=True)
                logger.info(f"Launched Windows application: {app_identifier}")
                return True
            except Exception as e:
                logger.error(f"Failed to launch Windows app {app_identifier}: {e}")
                return False

        # Linux launching
        all_apps = self.list_applications()
        for app in all_apps:
            if app["id"] == app_identifier or app["name"].lower() == app_identifier.lower() or app["exec"] == app_identifier:
                cmd = app["exec"].split()
                try:
                    subprocess.Popen(cmd)
                    logger.info(f"Launched Linux application: {app['name']}")
                    return True
                except Exception as e:
                    logger.error(f"Failed to launch {app['name']}: {e}")
                    return False

        # Fallback to PATH binary
        safe_bin = os.path.basename(app_identifier.strip())
        import shutil
        if shutil.which(safe_bin):
            try:
                subprocess.Popen([safe_bin])
                return True
            except Exception as e:
                logger.error(f"Failed to launch bin {safe_bin}: {e}")
                return False

        return False
