#!/usr/bin/env bash
# ==============================================================================
# OmniDesk Companion Server - Run in Background (Linux / Universal)
# Starts OmniDesk detached from terminal so closing the terminal does NOT kill it
# ==============================================================================

OS_NAME="$(uname -s)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ "$OS_NAME" == "Darwin" ]; then
    exec "$SCRIPT_DIR/start-background-mac.sh"
fi

cd "$SCRIPT_DIR"
mkdir -p "$HOME/.config/omnidesk"
LOG_FILE="$HOME/.config/omnidesk/server.log"
PIN_FILE="$HOME/.config/omnidesk/current_pin.txt"
PID_FILE="$HOME/.config/omnidesk/server.pid"

GREEN="\033[0;32m"
BLUE="\033[0;34m"
CYAN="\033[0;36m"
YELLOW="\033[1;33m"
BOLD="\033[1m"
DIM="\033[2m"
NC="\033[0m"

# If systemd service is active, show status
if command -v systemctl &>/dev/null && systemctl --user is-active --quiet omnidesk.service 2>/dev/null; then
    echo -e "${GREEN}OmniDesk is already running as an active systemd user service!${NC}"
    exec "$SCRIPT_DIR/status.sh"
fi

# Check if port 8765 is active
if command -v ss &>/dev/null && ss -tulpn 2>/dev/null | grep -q ":8765"; then
    echo -e "${YELLOW}OmniDesk is already running on port 8765!${NC}"
    exec "$SCRIPT_DIR/status.sh"
fi

# Find python
if [ -f "$SCRIPT_DIR/.venv/bin/python3" ]; then
    PYTHON_BIN="$SCRIPT_DIR/.venv/bin/python3"
elif [ -f "$SCRIPT_DIR/venv/bin/python3" ]; then
    PYTHON_BIN="$SCRIPT_DIR/venv/bin/python3"
else
    PYTHON_BIN="$(which python3)"
fi

echo -e "${CYAN}Starting OmniDesk in the background...${NC}"
nohup "$PYTHON_BIN" "$SCRIPT_DIR/server.py" >> "$LOG_FILE" 2>&1 &
SERVER_PID=$!
disown $SERVER_PID 2>/dev/null || true
echo "$SERVER_PID" > "$PID_FILE"

sleep 2

LOCAL_IP=$(hostname -I 2>/dev/null | awk '{print $1}')
PIN=""
if [ -f "$PIN_FILE" ]; then
    PIN=$(head -n 1 "$PIN_FILE")
fi

echo ""
echo -e "${BLUE}────────────────────────────────────────────────${NC}"
echo -e "  ⚡  ${BOLD}OmniDesk Server (Linux Background)${NC}  ⚡"
echo -e "${BLUE}────────────────────────────────────────────────${NC}"
echo -e "  • Status:     ${GREEN}Running & Detached ✨${NC}"
echo -e "  • PID:        ${YELLOW}${SERVER_PID}${NC}"
echo -e "  • Address:    ${CYAN}ws://${LOCAL_IP:-127.0.0.1}:8765${NC}"
if [ -n "$PIN" ]; then
    echo -e "  • Pair PIN:   ${BOLD}${GREEN}[ ${PIN} ]${NC}"
fi
echo -e "  • Log File:   ${DIM}${LOG_FILE}${NC}"
echo -e "${BLUE}────────────────────────────────────────────────${NC}"
echo -e "${BOLD}${GREEN}✔ SUCCESS: You can now SAFELY CLOSE THIS TERMINAL!${NC}"
echo -e "${DIM}  The server will continue running in the background.${NC}"
echo -e "${DIM}  To stop: ./stop.sh${NC}"
echo ""
