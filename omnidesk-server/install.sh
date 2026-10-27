#!/usr/bin/env bash
# OmniDesk Companion Server - Linux Prompt-by-Prompt Guided Setup
set -e

GREEN="\033[0;32m"
BLUE="\033[0;34m"
CYAN="\033[0;36m"
YELLOW="\033[1;33m"
RED="\033[0;31m"
BOLD="\033[1m"
DIM="\033[2m"
NC="\033[0m"

echo -e "${CYAN}======================================================================${NC}"
echo -e "${BOLD}${GREEN}     ⚡ OmniDesk Companion Server - Linux Setup Wizard ⚡${NC}"
echo -e "${DIM}     Prompt-by-prompt guided installer with smart suggestions${NC}"
echo -e "${CYAN}======================================================================${NC}"
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Check if Python 3 and venv are installed
if ! command -v python3 &> /dev/null; then
    echo -e "${YELLOW}Python 3 is required but not installed.${NC}"
    read -p "Install Python 3 now using system package manager? [Y/n]: " INSTALL_PY
    INSTALL_PY=${INSTALL_PY:-Y}
    if [[ "$INSTALL_PY" =~ ^[Yy] ]]; then
        if command -v apt &> /dev/null; then
            sudo apt update && sudo apt install -y python3 python3-pip python3-venv
        elif command -v dnf &> /dev/null; then
            sudo dnf install -y python3 python3-pip
        elif command -v pacman &> /dev/null; then
            sudo pacman -S --noconfirm python python-pip
        elif command -v zypper &> /dev/null; then
            sudo zypper install -y python3 python3-pip
        fi
    else
        echo -e "${RED}Python 3 is required. Please install Python 3 and re-run.${NC}"
        exit 1
    fi
fi

# Launch the cross-platform interactive Python wizard if available
if command -v python3 &> /dev/null && [ -f "$SCRIPT_DIR/install_wizard.py" ]; then
    exec python3 "$SCRIPT_DIR/install_wizard.py"
fi

# Fallback bash wizard if python script is somehow unavailable
echo -e "${BOLD}${CYAN}[Step 1/5] Checking Python 3...${NC}"
echo -e "${GREEN}✔ Python 3 is ready! ($(python3 --version))${NC}"

# Step 2: System desktop packages
echo ""
echo -e "${BOLD}${CYAN}[Step 2/5] Desktop Control Tools...${NC}"
echo -e "${DIM}Tools: xdotool, playerctl, xclip, pulseaudio-utils, avahi-utils${NC}"
read -p "Install desktop tools using your system package manager? [Y/n]: " INSTALL_TOOLS
INSTALL_TOOLS=${INSTALL_TOOLS:-Y}
if [[ "$INSTALL_TOOLS" =~ ^[Yy] ]]; then
    if command -v apt &> /dev/null; then
        sudo apt update -qq || true
        sudo apt install -y python3-venv python3-pip xdotool playerctl libnotify-bin pulseaudio-utils xclip avahi-utils avahi-daemon || true
    elif command -v dnf &> /dev/null; then
        sudo dnf install -y xdotool playerctl libnotify pulseaudio-utils xclip avahi-tools avahi || true
    elif command -v pacman &> /dev/null; then
        sudo pacman -S --noconfirm xdotool playerctl libnotify pulseaudio xclip avahi || true
    fi
    echo -e "${GREEN}✔ Desktop tools installed!${NC}"
fi

# Step 3: Python Virtual Environment & Dependencies
echo ""
echo -e "${BOLD}${CYAN}[Step 3/5] Python Module Dependencies...${NC}"
read -p "Create an isolated virtual environment (.venv)? [Y/n]: " USE_VENV
USE_VENV=${USE_VENV:-Y}
if [[ "$USE_VENV" =~ ^[Yy] ]]; then
    python3 -m venv "$SCRIPT_DIR/.venv"
    PYTHON_BIN="$SCRIPT_DIR/.venv/bin/python3"
    PIP_BIN="$SCRIPT_DIR/.venv/bin/pip"
else
    PYTHON_BIN="python3"
    PIP_BIN="pip3"
fi

"$PIP_BIN" install --upgrade pip --quiet || true
"$PIP_BIN" install -r "$SCRIPT_DIR/requirements.txt" || "$PIP_BIN" install --break-system-packages -r "$SCRIPT_DIR/requirements.txt"
echo -e "${GREEN}✔ Dependencies installed!${NC}"

# Step 4: Firewall & Uinput Permissions
echo ""
echo -e "${BOLD}${CYAN}[Step 4/5] Permissions & Firewall...${NC}"
if command -v ufw &> /dev/null; then
    if sudo ufw status 2>/dev/null | grep -q "Status: active"; then
        read -p "Allow OmniDesk in UFW firewall (8765/tcp, 8766/udp, 5353/udp)? [Y/n]: " ALLOW_UFW
        ALLOW_UFW=${ALLOW_UFW:-Y}
        if [[ "$ALLOW_UFW" =~ ^[Yy] ]]; then
            sudo ufw allow 8765/tcp comment "OmniDesk WebSocket" 2>/dev/null || true
            sudo ufw allow 8766/udp comment "OmniDesk Discovery" 2>/dev/null || true
            sudo ufw allow 5353/udp comment "mDNS Zeroconf" 2>/dev/null || true
            echo -e "${GREEN}✔ UFW firewall ports opened!${NC}"
        fi
    fi
fi

if [ -e "/dev/uinput" ]; then
    read -p "Configure /dev/uinput permissions for virtual gamepad without sudo? [Y/n]: " ALLOW_UINPUT
    ALLOW_UINPUT=${ALLOW_UINPUT:-Y}
    if [[ "$ALLOW_UINPUT" =~ ^[Yy] ]]; then
        USER_NAME="${USER:-$LOGNAME}"
        sudo usermod -aG input "$USER_NAME" 2>/dev/null || true
        echo 'KERNEL=="uinput", MODE="0660", GROUP="input", OPTIONS+="static_node=uinput"' | sudo tee /etc/udev/rules.d/99-omnidesk-uinput.rules > /dev/null || true
        sudo udevadm control --reload-rules 2>/dev/null || true
        sudo udevadm trigger 2>/dev/null || true
        echo -e "${GREEN}✔ Uinput permissions configured!${NC}"
    fi
fi

# Step 5: Systemd Background Service
echo ""
echo -e "${BOLD}${CYAN}[Step 5/5] Background Service & Auto-Start...${NC}"
read -p "Enable OmniDesk background service so it starts automatically? [Y/n]: " ENABLE_SERVICE
ENABLE_SERVICE=${ENABLE_SERVICE:-Y}
if [[ "$ENABLE_SERVICE" =~ ^[Yy] ]]; then
    SYSTEMD_DIR="$HOME/.config/systemd/user"
    mkdir -p "$SYSTEMD_DIR"
    cat <<EOF > "$SYSTEMD_DIR/omnidesk.service"
[Unit]
Description=OmniDesk Companion Server
After=network.target

[Service]
Type=simple
Environment="DISPLAY=:0"
WorkingDirectory=$SCRIPT_DIR
ExecStart=$PYTHON_BIN $SCRIPT_DIR/server.py
Restart=on-failure
RestartSec=3

[Install]
WantedBy=default.target
EOF
    systemctl --user daemon-reload 2>/dev/null || true
    systemctl --user enable omnidesk.service 2>/dev/null || true
    systemctl --user restart omnidesk.service 2>/dev/null || true
    echo -e "${GREEN}✔ OmniDesk systemd user service enabled and started!${NC}"
    echo -e "Check status anytime: ${BOLD}systemctl --user status omnidesk.service${NC}"
fi

echo ""
echo -e "${CYAN}======================================================================${NC}"
echo -e "${BOLD}${GREEN}   🎉 Linux Setup Complete! 🎉${NC}"
echo -e "${CYAN}======================================================================${NC}"
read -p "Launch OmniDesk Server in this terminal right now? [Y/n]: " START_NOW
START_NOW=${START_NOW:-Y}
if [[ "$START_NOW" =~ ^[Yy] ]]; then
    echo -e "${GREEN}Starting OmniDesk Server... (Press Ctrl+C to stop)${NC}"
    "$PYTHON_BIN" "$SCRIPT_DIR/server.py"
fi
