#!/usr/bin/env bash
# ==============================================================================
# OmniDesk - Universal Status Checker (macOS & Linux)
# Shows clean, concise status without opening a scrollable pager or flooding terminal
# ==============================================================================

GREEN="\033[0;32m"
BLUE="\033[0;34m"
CYAN="\033[0;36m"
YELLOW="\033[1;33m"
RED="\033[0;31m"
BOLD="\033[1m"
DIM="\033[2m"
NC="\033[0m"

OS_NAME="$(uname -s)"

if [ "$OS_NAME" == "Darwin" ]; then
    DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    exec "$DIR/autostart-mac.sh" status
fi

# Linux status
echo -e "${BLUE}=== OmniDesk Server Status (Linux) ===${NC}"

SVC_NAME="omnidesk.service"
if ! systemctl --user is-active --quiet "$SVC_NAME" 2>/dev/null; then
    if systemctl --user is-active --quiet linux-remote.service 2>/dev/null; then
        SVC_NAME="linux-remote.service"
    fi
fi

if command -v systemctl &>/dev/null && systemctl --user is-active --quiet "$SVC_NAME" 2>/dev/null; then
    echo -e "Service State:   ${GREEN}● ACTIVE ($SVC_NAME Running)${NC}"
else
    # Check if running as detached background process
    PID_FILE="$HOME/.config/omnidesk/server.pid"
    if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE" 2>/dev/null)" 2>/dev/null; then
        echo -e "Service State:   ${GREEN}● RUNNING in Background${NC} (PID: $(cat "$PID_FILE"))"
    else
        echo -e "Service State:   ${RED}● INACTIVE or STOPPED${NC}"
    fi
fi

# Check listening WebSocket port 8765
if command -v ss &>/dev/null; then
    PORT_CHECK=$(ss -tulpn 2>/dev/null | grep ":8765" || true)
elif command -v netstat &>/dev/null; then
    PORT_CHECK=$(netstat -tuln 2>/dev/null | grep ":8765" || true)
else
    PORT_CHECK=""
fi

if [ -n "$PORT_CHECK" ]; then
    echo -e "WebSocket Port:  ${GREEN}✓ Port 8765 is OPEN and listening${NC}"
else
    echo -e "WebSocket Port:  ${YELLOW}! Port 8765 not detected listening yet${NC}"
fi

# Show local IP address
LOCAL_IP=$(hostname -I 2>/dev/null | awk '{print $1}')
echo -e "Host Local IP:   ${YELLOW}${LOCAL_IP:-127.0.0.1}${NC}"
echo -e "WebSocket URL:   ${CYAN}ws://${LOCAL_IP:-127.0.0.1}:8765${NC}"

# Check active PIN
PIN_FILE="$HOME/.config/omnidesk/current_pin.txt"
if [ ! -f "$PIN_FILE" ]; then
    PIN_FILE="$HOME/.config/linux-remote/current_pin.txt"
fi
if [ -f "$PIN_FILE" ]; then
    PIN=$(head -n 1 "$PIN_FILE")
    echo -e "Pairing PIN:     ${BOLD}${GREEN}[ $PIN ]${NC}"
fi

echo ""
if command -v journalctl &>/dev/null; then
    echo -e "${BLUE}=== Recent Service Logs (No Pager) ===${NC}"
    journalctl --user -u "$SVC_NAME" -n 10 --no-pager 2>/dev/null || true
fi

echo ""
echo "Helpful Commands:"
echo "  Run Background:     ./start-background.sh"
echo "  Live Terminal Test: python3 server.py"
echo "  Stop Server:        ./stop.sh"
echo "  Autostart Manager:  ./autostart-linux.sh"
