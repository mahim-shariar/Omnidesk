# OmniDesk - Universal PC Remote Companion Server

A lightweight, secure, cross-platform companion receiver for **OmniDesk** that lets you control your **Windows, macOS, and Linux** desktop from your Android phone over local Wi-Fi with zero cloud dependencies.

---

## ⚡ Super Easy Guided Installation (Beginner-Friendly)

OmniDesk includes an **interactive step-by-step setup wizard** that walks you through prompts one by one, detects your operating system, installs dependencies, handles firewall & permissions, and launches the server with your pairing PIN and QR code!

### 🪟 Windows (10 & 11)
Open Command Prompt or PowerShell in the `omnidesk-server` directory and run:
```cmd
install.bat
```
*(Or simply double-click `install.bat` in Windows File Explorer!)*

The wizard will prompt you:
1. ✅ **Python check**: Confirms Python 3.9+ is ready.
2. 📦 **Dependencies**: Automatically installs `websockets`, `psutil`, `pyautogui`, `pynput`, `zeroconf`.
3. 🛡️ **Firewall**: Automatically configures Windows Defender Firewall rules for port 8765 (TCP) and 8766 (UDP).
4. 🚀 **Auto-Start**: Optionally adds OmniDesk to your Windows Startup folder.
5. 🖥️ **Desktop Shortcut**: Optionally creates a clickable desktop launcher icon.
6. 🎯 **Instant Launch**: Starts the server and displays the 6-digit pairing PIN and QR code!

---

### 🍎 macOS (Sonoma, Ventura, Monterey, Sequoia+)
Open Terminal in the `omnidesk-server` directory and run:
```bash
./install-mac.sh
```
*(Or: `python3 setup.py`)*

The wizard will prompt you:
1. ✅ **Python & Tools check**: Detects Python 3 / Xcode tools.
2. 📦 **Virtual Environment**: Sets up an isolated `.venv` to keep your system clean.
3. 🔐 **macOS Permissions**: Prompts and automatically opens macOS **System Settings > Privacy & Security > Accessibility** so you can click to allow mouse and keyboard control.
4. 🚀 **Auto-Start LaunchAgent**: Optionally configures `com.omnidesk.server.plist` to start automatically on Mac login.
5. 🎯 **Instant Launch**: Starts the receiver showing the pairing QR code and PIN!

---

### 🐧 Linux (Ubuntu, Debian, Linux Mint, Fedora, Arch Linux)
Open a terminal in the `omnidesk-server` directory and run:
```bash
./install.sh
```
*(Or: `python3 setup.py`)*

The wizard will prompt you:
1. ✅ **Distro Detection**: Detects your package manager (`apt`, `dnf`, `pacman`, `zypper`).
2. 📦 **Desktop Tools**: Automatically installs `xdotool`, `playerctl`, `xclip`, and `pulseaudio-utils`.
3. 🛡️ **UFW Firewall**: Prompts to open ports 8765/tcp, 8766/udp, and 5353/udp if UFW is active.
4. 🎮 **Uinput Permissions**: Configures `/dev/uinput` group membership for virtual gamepad without root.
5. ⚙️ **Systemd User Service**: Enables and starts `omnidesk.service` in the background so it runs on boot.
6. 🎯 **Instant Launch**: Displays status and live pairing code!

---

### 🌐 Universal Python Command (Works on ANY OS)
If you already have Python 3 installed, simply run:
```bash
python3 setup.py
```
*(or on Windows: `python setup.py`)*

This runs the guided interactive wizard (`install_wizard.py`) with colored, step-by-step numbered prompts.

---

## 📱 Connecting from your Android Phone

1. Ensure your Android phone and PC are on the same Wi-Fi network (or connected via USB cable).
2. Launch the **OmniDesk** app.
3. Your computer will be detected automatically! Tap **Connect**.
4. Enter the 6-digit pairing code shown in your PC terminal (or scan the terminal QR code with OmniDesk's built-in scanner).
5. Done! You have full remote control of your computer!

---

## 🔌 USB Cable Mode (Zero-Latency / No Wi-Fi)

For competitive gaming, crowded Wi-Fi, or offline use without a router:
1. Connect your phone via USB cable and enable **USB Debugging**.
2. Run on your PC:
   ```bash
   adb reverse tcp:8765 tcp:8765
   ```
3. In OmniDesk, tap **Connect via USB Cable (127.0.0.1)**.
4. Experience instant, ultra-fast 125Hz wired control!
