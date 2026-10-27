# OmniDesk

> **Transform your Android device into a low-latency, ultra-responsive remote control trackpad, keyboard, presenter, media deck, and system monitor for Linux, macOS, and Windows.**

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Linux%20%7C%20macOS%20%7C%20Windows-green.svg)](#)
[![No Cloud](https://img.shields.io/badge/Privacy-100%25%20Local%20LAN%20%26%20FOSS-success.svg)](#)
[![F-Droid Compatible](https://img.shields.io/badge/F--Droid-Ready-brightgreen.svg)](FDROID_PUBLISHING_GUIDE.md)

---

## 📑 Table of Contents
1. [Overview & Features](#overview--features)
2. [How It Works (Architecture)](#how-it-works-architecture)
3. [Android App Installation](#android-app-installation)
4. [Companion Server Setup](#companion-server-setup)
   - [🐧 Linux Setup (Ubuntu, Debian, Fedora, Arch, openSUSE)](#-linux-setup)
   - [🍏 macOS Setup (Apple Silicon & Intel)](#-macos-setup)
   - [🪟 Windows Setup (Windows 10, 11)](#-windows-setup)
5. [Pairing & Connecting](#pairing--connecting)
   - [Auto Wi-Fi Discovery](#1-auto-wi-fi-discovery)
   - [QR Code Pairing](#2-qr-code-pairing)
   - [6-Digit PIN Pairing](#3-6-digit-pin-pairing)
   - [Zero-Latency USB Cable Mode](#4-zero-latency-usb-cable-mode-adb--reverse-tethering)
6. [Background Daemon & Autostart on Boot](#background-daemon--autostart-on-boot)
7. [Troubleshooting & FAQ](#troubleshooting--faq)
8. [License & Privacy Guarantee](#license--privacy-guarantee)

---

## 🚀 Overview & Features

OmniDesk is a 100% Free and Open Source (FOSS) client-server system designed for privacy-conscious users. Unlike commercial remote tools, **all communication stays strictly on your local Wi-Fi or USB tether**. No accounts, no subscriptions, and zero third-party cloud relays.

- 🖱️ **Precision Trackpad**: Smooth inertial scrolling, two-finger tap (right click), three-finger tap (middle click), click-and-drag, sensitivity and acceleration tuning.
- ⌨️ **Full Keyboard & Functional Hotkeys**: Full typing support, arrow keys, Ctrl/Alt/Super/Cmd modifier toggles, and common OS shortcuts (`Alt+Tab`, `Cmd+Space`, `Ctrl+C`, `Ctrl+V`).
- 🎵 **Media & Volume Deck**: Play/Pause, Next, Previous, Volume slider, Mute toggle, and media player integration (`playerctl` on Linux, native media keys on Windows & macOS).
- 📊 **Real-time System Telemetry**: Live CPU usage %, RAM %, Battery status, and uptime streamed straight to your phone.
- ⚡ **Power & Session Management**: Lock Screen, Sleep, Restart, Shutdown, and Wake-on-LAN (WOL).
- 🧑‍🏫 **Presentation Remote**: Next/Previous slide triggers, fullscreen presentation mode, slide timer, and virtual laser pointer simulation.
- 🎮 **Virtual Gamepad**: Low-latency touch buttons, D-pad, and analog thumbstick emulation.
- 📋 **Shared Clipboard**: One-tap copy/paste between phone and desktop.
- 🔌 **USB Cable Mode**: Play or work with 0ms wireless latency through ADB USB reverse port forwarding.

---

## ⚙️ How It Works (Architecture)

```
┌───────────────────────────────────────┐         Direct Local Network (LAN)
│      OmniDesk Android Client          │         ==========================
│   (Jetpack Compose + Material 3)      │
└──────────────────┬────────────────────┘
                   │
                   │  1. UDP Beacon Broadcast & Ping (Port 8766 UDP)
                   │  2. mDNS / DNS-SD Zeroconf discovery (_linuxremote._tcp)
                   │  3. WebSocket Protocol with TLS/PIN Auth (Port 8765 TCP)
                   ▼
┌───────────────────────────────────────┐
│     OmniDesk Companion Server         │
│  (Python 3.8+ asyncio & websockets)   │
├───────────────────────────────────────┤
│  • Linux:   python-uinput / xdotool   │
│  • macOS:   PyAutoGUI / Quartz CGEvent│
│  • Windows: pywin32 / SendInput / ctypes│
└───────────────────────────────────────┘
```

### Network Ports Used:
| Port | Protocol | Purpose |
|------|----------|---------|
| **`8765`** | **TCP** | Main WebSocket connection (mouse, keyboard, media, telemetry) |
| **`8766`** | **UDP** | LAN Discovery broadcast beacon & direct ping responder |
| **`5353`** | **UDP** | mDNS / Zeroconf local discovery (`_linuxremote._tcp.local`) |

---

## 📱 Android App Installation

### Option 1: Install APK via GitHub Releases / IzzyOnDroid / Obtainium
1. Download the latest `OmniDesk.apk` from the [Releases](../../releases) tab.
2. Tap the downloaded APK to install.
3. If prompted, allow "Install from Unknown Sources".

### Option 2: Build From Source (Android Studio / Gradle)
```bash
git clone https://github.com/your-username/omnidesk.git
cd omnidesk
# Build debug APK
gradle :app:assembleDebug
# APK generated at: app/build/outputs/apk/debug/app-debug.apk
```

---

## 💻 Companion Server Setup

The companion server is located in the `omnidesk-server/` directory. It requires **Python 3.8 or newer**.

---

### 🐧 Linux Setup

Works on **Ubuntu, Debian, Linux Mint, Pop!_OS, Fedora, Arch Linux, Manjaro, openSUSE**, and other distributions.

#### 1. Automatic Install
Open a terminal inside the project directory:
```bash
cd omnidesk-server
chmod +x *.sh
./install.sh
```
*The installer automatically detects your package manager (`apt`, `dnf`, `pacman`, or `zypper`), installs necessary dependencies (`python3-venv`, `xdotool`, `playerctl`), creates a dedicated Python virtual environment (`.venv`), and installs required packages.*

#### 2. Manual / Minimal Setup (if preferred)
```bash
cd omnidesk-server
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

#### 3. Linux Firewall Configuration
If you have an active firewall, allow OmniDesk's discovery and communication ports:
- **UFW (Ubuntu / Debian):**
  ```bash
  sudo ufw allow 8765/tcp comment "OmniDesk WebSocket"
  sudo ufw allow 8766/udp comment "OmniDesk Discovery"
  ```
- **Firewalld (Fedora / RHEL):**
  ```bash
  sudo firewall-cmd --add-port=8765/tcp --permanent
  sudo firewall-cmd --add-port=8766/udp --permanent
  sudo firewall-cmd --reload
  ```

#### 4. Gamepad & Virtual Input Permissions (Optional)
To use the virtual gamepad on Linux without needing `sudo`:
```bash
sudo usermod -aG input $USER
echo 'KERNEL=="uinput", GROUP="input", MODE="0660"' | sudo tee /etc/udev/rules.d/99-uinput.rules
sudo udevadm control --reload-rules && sudo udevadm trigger
```

#### 5. Running the Server on Linux
- **Interactive Foreground (Terminal output with QR code):**
  ```bash
  ./run.sh
  ```
- **Run in Background (Detached):**
  ```bash
  ./start-background.sh
  ```
- **Check Status / Stop:**
  ```bash
  ./status.sh
  ./stop.sh
  ```

---

### 🍏 macOS Setup

Supports **macOS Sonoma (14.x), Sequoia (15.x), Ventura (13.x), and Monterey (12.x)** on both **Apple Silicon (M1/M2/M3/M4)** and **Intel**.

#### 1. Automatic Install
Open **Terminal.app** and run:
```bash
cd omnidesk-server
chmod +x *.sh
./install-mac.sh
```
*The installer creates the isolated Python virtual environment, installs PyAutoGUI, websockets, and Mac-native system integrations, and creates a desktop double-click launcher.*

#### 2. ⚠️ Grant Accessibility Permissions (CRITICAL for Mac)
macOS security requires permission to simulate mouse movements and keystrokes:
1. Open **System Settings** (or **System Preferences**).
2. Go to **Privacy & Security** ➔ **Accessibility**.
3. Click the **`+`** icon and add:
   - **Terminal** (if running from Terminal)
   - **Python** (e.g., `/usr/local/bin/python3` or the `.venv/bin/python` binary)
4. Go to **Privacy & Security** ➔ **Input Monitoring** and ensure the same applications are enabled.
5. *If cursor movement does not respond, restart Terminal after toggling the checkmark.*

#### 3. Running the Server on macOS
- **Interactive Mode (Shows QR Code & Live Logs):**
  ```bash
  ./run-mac.sh
  ```
- **Background Mode (No open terminal window):**
  ```bash
  ./start-background-mac.sh
  ```
- **Check Status / Stop:**
  ```bash
  ./status.sh
  ./stop-mac.sh
  ```

---

### 🪟 Windows Setup

Supports **Windows 10 and Windows 11 (64-bit)**.

#### 1. Prerequisites
- Download and install **Python 3.8 or newer** from [python.org](https://www.python.org/downloads/).
- ⚠️ **IMPORTANT**: On the first installer screen, make sure to check:
  `☑ Add python.exe to PATH`.

#### 2. Automatic Install
1. Open File Explorer and navigate to the `omnidesk-server` folder.
2. Double-click **`install.bat`** (or right-click `install.ps1` and select *Run with PowerShell*).
3. The script will automatically create `.venv`, install all dependencies, and prepare desktop shortcuts.

#### 3. Windows Firewall Prompt
When running for the first time, Windows Defender Firewall will show a security prompt:
- Check **"Private networks, such as my home or work network"**.
- Click **"Allow access"**.
- *(If you missed the prompt, open Windows Defender Firewall > "Allow an app through firewall" > add Python or allow TCP port 8765 and UDP port 8766).*

#### 4. Running the Server on Windows
- **Interactive Mode (Console window with QR code & pairing PIN):**
  - Double-click **`run.bat`**.
- **Silent / Background Mode (Completely invisible, no taskbar window):**
  - Double-click **`start-background.bat`**.
  - *(Uses `run-invisible.vbs` to execute via `pythonw.exe` without opening a command prompt).*
- **Check Status / Stop:**
  - Double-click **`status-windows.bat`** to check if the server is active.
  - Double-click **`stop-windows.bat`** to terminate the background process.

---

## 🔄 Background Daemon & Autostart on Boot

To have OmniDesk run silently in the background whenever your computer turns on:

### Linux (systemd User Service)
Run the autostart configuration script:
```bash
cd omnidesk-server
./autostart-linux.sh
```
Or manually configure systemd:
```bash
mkdir -p ~/.config/systemd/user
cp remote.service ~/.config/systemd/user/omnidesk.service
# Edit path to point to your omnidesk-server location
systemctl --user daemon-reload
systemctl --user enable --now omnidesk.service
```

### macOS (LaunchAgent)
Run the macOS autostart script:
```bash
cd omnidesk-server
./autostart-mac.sh
```
*This installs a LaunchAgent at `~/Library/LaunchAgents/com.omnidesk.server.plist` which launches the server automatically whenever you log into macOS.*

### Windows (Startup Folder / Task Scheduler)
Run the Windows autostart script:
- Double-click **`autostart-windows.bat`**.
*This places a silent startup shortcut in your Windows `shell:startup` folder that runs `start-background.bat` on user login.*

---

## 🔗 Pairing & Connecting

Make sure your Android device and computer are on the **same Wi-Fi network**.

### 1. Auto Wi-Fi Discovery
1. Launch the **OmniDesk** app on Android.
2. The app automatically scans your local subnet via UDP and mDNS.
3. Your PC will appear in the **Discovered Devices** list. Tap it to connect!

### 2. QR Code Pairing
1. Run `./run.sh`, `./run-mac.sh`, or `run.bat` in a terminal.
2. A QR code is rendered directly in your terminal.
3. In the Android app, tap the **QR Code icon** in the top bar and point your camera at the screen.

### 3. 6-Digit PIN Pairing
1. The server prints a secure 6-digit PIN upon startup.
2. In the Android app, enter the PIN when prompted to establish a cryptographically verified session.
3. Once paired, your device is saved to the **Trusted Devices** list.

### 4. Zero-Latency USB Cable Mode (ADB / Reverse Tethering)
If Wi-Fi is unavailable, congested, or you need absolute minimum latency:
1. Connect your Android phone to your PC via a USB cable.
2. Enable **USB Debugging** in Android Developer Options.
3. Run the reverse tethering command:
   ```bash
   adb reverse tcp:8765 tcp:8765
   ```
   *(Or on Linux/Mac, run `./usb-cable-enable.sh`).*
4. In the Android app, enter `127.0.0.1` as the host IP and tap **Connect**.

---

## ❓ Troubleshooting & FAQ

#### Q: "Connection rejected (200 OK)" in the server logs?
**A:** This occurred in older server revisions when an HTTP health-check intercepted WebSocket upgrade requests. Update to the latest `server.py` where the WebSocket handshake header `Upgrade: websocket` is strictly preserved.

#### Q: The server shows "Incoming connection" then "Connection closed" every few seconds?
**A:** That is normal background discovery traffic! The Android app periodically probes port 8765 on your subnet to show whether your computer is currently online and measure latency. It does not disrupt active paired sessions.

#### Q: Mac cursor does not move even though it says "Connected"?
**A:** macOS requires Accessibility permissions. Go to **System Settings > Privacy & Security > Accessibility** and ensure **Terminal** (or the Python binary) is checked. You may need to toggle it off and on again after updating macOS.

#### Q: The app cannot discover my PC on Wi-Fi?
**A:**
1. Verify both devices are connected to the exact same Wi-Fi router (not one on Guest Wi-Fi and one on Main Wi-Fi).
2. Check your router settings for **AP Isolation / Client Isolation** (disable it so LAN devices can communicate).
3. Ensure port **8765 TCP** and **8766 UDP** are open in your computer's firewall.
4. Try connecting directly by entering your computer's local IP address (e.g. `192.168.1.150`).

#### Q: `ModuleNotFoundError: No module named 'websockets'`
**A:** You ran `python3 server.py` directly without activating the virtual environment. Always start the server via the provided runner script (`./run.sh`, `./run-mac.sh`, or `run.bat`), which activates `.venv` automatically, or run `source .venv/bin/activate` first.

---

## 🔒 License & Privacy Guarantee

OmniDesk is distributed under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for complete details.

- **Zero Tracking**: No Google Analytics, no Firebase Analytics, no Mixpanel, and no Sentry.
- **Zero Cloud Storage**: No remote databases; your server runs solely on your own hardware.
- **100% Free as in Freedom**: F-Droid compliant, open source, and free to audit, modify, and redistribute.
