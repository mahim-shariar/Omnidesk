#!/usr/bin/env bash
# ==============================================================================
# OmniDesk Companion Server - Launcher for macOS
# ==============================================================================
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

GREEN="\033[0;32m"
BLUE="\033[0;34m"
CYAN="\033[0;36m"
YELLOW="\033[1;33m"
BOLD="\033[1m"
DIM="\033[2m"
NC="\033[0m"

# Handle CLI Flags
if [ "$1" == "--background" ] || [ "$1" == "-b" ] || [ "$1" == "-d" ] || [ "$1" == "--autostart" ] || [ "$1" == "-a" ] || [ "$1" == "--service" ]; then
    exec "$DIR/autostart-mac.sh" enable
elif [ "$1" == "--run-once" ]; then
    exec "$DIR/start-background-mac.sh"
elif [ "$1" == "--foreground" ] || [ "$1" == "-f" ]; then
    RUN_MODE="foreground"
elif [ "$1" == "--stop" ]; then
    exec "$DIR/stop-mac.sh"
elif [ "$1" == "--status" ]; then
    exec "$DIR/status.sh"
fi

# Locate Python
if [ -f "$DIR/.venv/bin/python3" ]; then
    PYTHON_BIN="$DIR/.venv/bin/python3"
elif [ -f "$DIR/venv/bin/python3" ]; then
    PYTHON_BIN="$DIR/venv/bin/python3"
elif [ -f "$HOME/.local/share/omnidesk/venv/bin/python3" ]; then
    PYTHON_BIN="$HOME/.local/share/omnidesk/venv/bin/python3"
else
    PYTHON_BIN="python3"
fi

# Verify websockets dependency before proceeding; auto-launch guided installer if missing
if ! "$PYTHON_BIN" -c "import websockets" &>/dev/null; then
    echo -e "${YELLOW}OmniDesk packages ('websockets') are not yet installed in $PYTHON_BIN.${NC}"
    if [ -f "$DIR/install-mac.sh" ]; then
        echo -e "${CYAN}Launching macOS installer now to set up your environment...${NC}\n"
        exec "$DIR/install-mac.sh"
    fi
fi

# Check if port 8765 is already running
PORT_PID=$(lsof -ti:8765 2>/dev/null || true)
if [ -n "$PORT_PID" ]; then
    echo -e "${YELLOW}OmniDesk Server is already running on port 8765 (PID: $PORT_PID)!${NC}"
    PIN_FILE="$HOME/.config/omnidesk/current_pin.txt"
    if [ -f "$PIN_FILE" ]; then
        PIN=$(head -n 1 "$PIN_FILE" | tr -d '\r\n')
        echo -e "Current Pairing PIN: ${BOLD}${GREEN}[ $PIN ]${NC}"
    fi
    echo ""
    echo "Options:"
    echo "  1. View Status:     ./status.sh"
    echo "  2. Stop Server:     ./stop-mac.sh"
    echo "  3. Restart Server:  ./stop-mac.sh && ./run-mac.sh"
    exit 0
fi

if [ -z "$RUN_MODE" ]; then
    echo -e "${CYAN}=====================================================${NC}"
    echo -e "${BOLD}${GREEN}   🍎  OmniDesk Server for macOS  🍎${NC}"
    echo -e "${CYAN}=====================================================${NC}"
    echo ""
    echo -e "How would you like to run the server?"
    echo -e "  ${BOLD}[1] Autostart on Boot & Background Mode${NC} ${GREEN}(Recommended)${NC}"
    echo -e "      ${DIM}Runs silently in background RIGHT NOW and automatically on every Mac boot / login.${NC}"
    echo -e "      ${DIM}Safe to close this terminal!${NC}"
    echo -e "  ${BOLD}[2] Background Mode Only (Run Once)${NC}"
    echo -e "      ${DIM}Runs silently in background right now, without installing auto-start on boot.${NC}"
    echo -e "  ${BOLD}[3] Foreground Mode${NC}"
    echo -e "      ${DIM}Shows live streaming logs directly in this terminal window (Ctrl+C to stop).${NC}"
    echo ""
    read -p "Select option [1/2/3] (Default: 1): " CHOICE
    CHOICE=${CHOICE:-1}

    if [ "$CHOICE" == "1" ]; then
        exec "$DIR/autostart-mac.sh" enable
    elif [ "$CHOICE" == "2" ]; then
        exec "$DIR/start-background-mac.sh"
    elif [ "$CHOICE" == "3" ]; then
        RUN_MODE="foreground"
    fi
fi

# Foreground mode:
echo -e "${GREEN}Starting OmniDesk Server in foreground... (Press Ctrl+C to stop)${NC}"
exec "$PYTHON_BIN" "$DIR/server.py"
