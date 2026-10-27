#!/usr/bin/env bash
# ==============================================================================
# OmniDesk Companion Server - Run in Background (macOS)
# Starts OmniDesk detached from the terminal so you can safely CLOSE the terminal
# window and the server will CONTINUE running silently in the background!
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

LOGS_DIR="$HOME/Library/Logs"
mkdir -p "$LOGS_DIR"
mkdir -p "$HOME/.config/omnidesk"
LOG_FILE="$LOGS_DIR/OmniDesk.log"
PIN_FILE="$HOME/.config/omnidesk/current_pin.txt"
PID_FILE="$HOME/.config/omnidesk/server.pid"

GREEN="\033[0;32m"
BLUE="\033[0;34m"
CYAN="\033[0;36m"
YELLOW="\033[1;33m"
BOLD="\033[1m"
DIM="\033[2m"
NC="\033[0m"

# Check if already running on port 8765
EXISTING_PID=$(lsof -ti:8765 2>/dev/null || true)
if [ -n "$EXISTING_PID" ]; then
    echo -e "${YELLOW}OmniDesk Server is already running in background! (PID: $EXISTING_PID)${NC}"
    if [ -f "$PIN_FILE" ]; then
        PIN=$(head -n 1 "$PIN_FILE" | tr -d '\r\n')
        echo -e "Current Pairing PIN: ${BOLD}${GREEN}[ $PIN ]${NC}"
    fi
    echo -e "${CYAN}To stop it: ./stop-mac.sh${NC}"
    echo -e "${CYAN}To view status: ./status.sh${NC}"
    exit 0
fi

# Select Python binary
if [ -f "$SCRIPT_DIR/.venv/bin/python3" ]; then
    PYTHON_BIN="$SCRIPT_DIR/.venv/bin/python3"
elif [ -f "$SCRIPT_DIR/venv/bin/python3" ]; then
    PYTHON_BIN="$SCRIPT_DIR/venv/bin/python3"
elif [ -f "$HOME/.local/share/omnidesk/venv/bin/python3" ]; then
    PYTHON_BIN="$HOME/.local/share/omnidesk/venv/bin/python3"
else
    PYTHON_BIN="python3"
fi

# Ensure dependencies exist before backgrounding
if ! "$PYTHON_BIN" -c "import websockets" &>/dev/null; then
    echo -e "${YELLOW}OmniDesk packages are not yet installed in $PYTHON_BIN.${NC}"
    if [ -f "$SCRIPT_DIR/install-mac.sh" ]; then
        echo -e "${CYAN}Launching guided macOS installer...${NC}\n"
        exec "$SCRIPT_DIR/install-mac.sh"
    fi
fi

echo -e "${CYAN}Starting OmniDesk Server in the background...${NC}"

# Launch detached with nohup and disown
nohup "$PYTHON_BIN" "$SCRIPT_DIR/server.py" >> "$LOG_FILE" 2>&1 &
SERVER_PID=$!
disown $SERVER_PID 2>/dev/null || true
echo "$SERVER_PID" > "$PID_FILE"

# Wait a brief moment for socket to bind and initial PIN to be generated
sleep 2

LOCAL_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || true)
if [ -z "$LOCAL_IP" ]; then
    LOCAL_IP=$("$PYTHON_BIN" -c "import socket; s=socket.socket(socket.AF_INET, socket.SOCK_DGRAM); s.connect(('8.8.8.8', 80)); print(s.getsockname()[0]); s.close()" 2>/dev/null || echo "127.0.0.1")
fi

PIN=""
if [ -f "$PIN_FILE" ]; then
    PIN=$(head -n 1 "$PIN_FILE" | tr -d '\r\n')
fi

echo ""
echo -e "${BLUE}────────────────────────────────────────────────${NC}"
echo -e "  🍎  ${BOLD}OmniDesk Server (macOS Background)${NC}  🍎"
echo -e "${BLUE}────────────────────────────────────────────────${NC}"
echo -e "  • Status:     ${GREEN}Running & Detached ✨${NC}"
echo -e "  • PID:        ${YELLOW}${SERVER_PID}${NC}"
echo -e "  • Address:    ${CYAN}ws://${LOCAL_IP}:8765${NC}"
if [ -n "$PIN" ]; then
    echo -e "  • Pair PIN:   ${BOLD}${GREEN}[ ${PIN} ]${NC}"
fi
echo -e "  • Log File:   ${DIM}${LOG_FILE}${NC}"
echo -e "${BLUE}────────────────────────────────────────────────${NC}"
echo -e "${BOLD}${GREEN}✔ SUCCESS: You can now SAFELY CLOSE THIS TERMINAL WINDOW!${NC}"
echo -e "${DIM}  The OmniDesk receiver will keep running in the background.${NC}"
echo -e "${DIM}  To stop the server at any time, run: ./stop-mac.sh${NC}"
echo ""
