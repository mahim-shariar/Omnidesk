#!/usr/bin/env bash
# OmniDesk Companion Server - macOS Prompt-by-Prompt Guided Setup
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
echo -e "${BOLD}${GREEN}     ⚡ OmniDesk Companion Server - macOS Setup Wizard ⚡${NC}"
echo -e "${DIM}     Prompt-by-prompt guided installer with Accessibility setup${NC}"
echo -e "${CYAN}======================================================================${NC}"
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Check Python 3
if ! command -v python3 &> /dev/null; then
    echo -e "${YELLOW}Python 3 is required on your Mac.${NC}"
    read -p "Install Xcode Command Line Tools (includes Python 3)? [Y/n]: " INSTALL_XCODE
    INSTALL_XCODE=${INSTALL_XCODE:-Y}
    if [[ "$INSTALL_XCODE" =~ ^[Yy] ]]; then
        xcode-select --install || true
        echo -e "${YELLOW}Please complete the installation dialog, then run ./install-mac.sh again.${NC}"
        exit 0
    else
        echo -e "${RED}Please install Python 3 from https://www.python.org/downloads/ or brew install python${NC}"
        exit 1
    fi
fi

# Launch rich Python wizard if available
if command -v python3 &> /dev/null && [ -f "$SCRIPT_DIR/install_wizard.py" ]; then
    exec python3 "$SCRIPT_DIR/install_wizard.py"
fi

# Fallback bash wizard
echo -e "${GREEN}✔ Detected Python 3: $(python3 --version)${NC}"

# Step 1: Virtual environment
echo ""
echo -e "${BOLD}${CYAN}[Step 1/4] Setting up Python Virtual Environment...${NC}"
read -p "Create virtual environment (.venv) to keep packages isolated? [Y/n]: " USE_VENV
USE_VENV=${USE_VENV:-Y}

if [[ "$USE_VENV" =~ ^[Yy] ]]; then
    python3 -m venv "$SCRIPT_DIR/.venv"
    PYTHON_BIN="$SCRIPT_DIR/.venv/bin/python3"
    PIP_BIN="$SCRIPT_DIR/.venv/bin/pip"
else
    PYTHON_BIN="python3"
    PIP_BIN="pip3"
fi

# Step 2: Dependencies
echo ""
echo -e "${BOLD}${CYAN}[Step 2/4] Installing Python Module Dependencies...${NC}"
"$PIP_BIN" install --upgrade pip --quiet || true
"$PIP_BIN" install -r "$SCRIPT_DIR/requirements.txt"
echo -e "${GREEN}✔ Dependencies installed successfully!${NC}"

# Step 3: macOS Permissions
echo ""
echo -e "${BOLD}${CYAN}[Step 3/4] macOS System Permissions (CRITICAL)...${NC}"
echo -e "${YELLOW}macOS requires Accessibility & Input Monitoring permissions for mouse & keyboard control.${NC}"
read -p "Open macOS Privacy & Security Settings directly to Accessibility? [Y/n]: " OPEN_PERMS
OPEN_PERMS=${OPEN_PERMS:-Y}
if [[ "$OPEN_PERMS" =~ ^[Yy] ]]; then
    open "x-apple.systempreferences:com.apple.preference.security?Privacy_Accessibility"
    echo -e "${GREEN}✔ System Settings opened! Toggle the switch for Terminal or Python.${NC}"
fi

read -p "Also open Input Monitoring settings? [Y/n]: " OPEN_INPUT
OPEN_INPUT=${OPEN_INPUT:-Y}
if [[ "$OPEN_INPUT" =~ ^[Yy] ]]; then
    open "x-apple.systempreferences:com.apple.preference.security?Privacy_ListenEvent"
fi

# Step 4: Auto-start LaunchAgent
echo ""
echo -e "${BOLD}${CYAN}[Step 4/4] Auto-Start on macOS Login & Background Mode...${NC}"
read -p "Start OmniDesk Server automatically on login and keep it running in background? [Y/n]: " AUTO_START
AUTO_START=${AUTO_START:-Y}
if [[ "$AUTO_START" =~ ^[Yy] ]]; then
    "$SCRIPT_DIR/autostart-mac.sh" enable
    echo ""
    echo -e "${CYAN}======================================================================${NC}"
    echo -e "${BOLD}${GREEN}   🎉 macOS Setup Complete! 🎉${NC}"
    echo -e "${CYAN}======================================================================${NC}"
    echo -e "${BOLD}${GREEN}✔ OmniDesk Server is already RUNNING in the background!${NC}"
    echo -e "${DIM}  You can now SAFELY CLOSE THIS TERMINAL WINDOW.${NC}"
    echo -e "${DIM}  To check status later: ./status.sh${NC}"
    echo -e "${DIM}  To stop the server:    ./stop-mac.sh${NC}"
    echo ""
    exit 0
fi

echo ""
echo -e "${CYAN}======================================================================${NC}"
echo -e "${BOLD}${GREEN}   🎉 macOS Setup Complete! 🎉${NC}"
echo -e "${CYAN}======================================================================${NC}"
read -p "Launch OmniDesk Server right now in the background? [Y/n]: " START_NOW
START_NOW=${START_NOW:-Y}
if [[ "$START_NOW" =~ ^[Yy] ]]; then
    exec "$SCRIPT_DIR/start-background-mac.sh"
fi
