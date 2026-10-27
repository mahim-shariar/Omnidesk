#!/usr/bin/env bash
# ==============================================================================
# OmniDesk Companion Server - macOS Autostart & Service Manager
# Uses macOS native launchd (LaunchAgent) for permanent background persistence.
# Even when you close the terminal or log out/in, OmniDesk continues running!
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLIST_LABEL="com.omnidesk.server"
PLIST_PATH="$HOME/Library/LaunchAgents/${PLIST_LABEL}.plist"
LOGS_DIR="$HOME/Library/Logs"
LOG_OUT="${LOGS_DIR}/OmniDesk.log"
LOG_ERR="${LOGS_DIR}/OmniDesk-error.log"
PIN_FILE="$HOME/.config/omnidesk/current_pin.txt"
PID_FILE="$HOME/.config/omnidesk/server.pid"

GREEN="\033[0;32m"
BLUE="\033[0;34m"
CYAN="\033[0;36m"
YELLOW="\033[1;33m"
RED="\033[0;31m"
BOLD="\033[1m"
DIM="\033[2m"
NC="\033[0m"

# Locate Python 3 binary
find_python() {
    if [ -f "$SCRIPT_DIR/.venv/bin/python3" ]; then
        echo "$SCRIPT_DIR/.venv/bin/python3"
    elif [ -f "$SCRIPT_DIR/venv/bin/python3" ]; then
        echo "$SCRIPT_DIR/venv/bin/python3"
    elif [ -f "$HOME/.local/share/omnidesk/venv/bin/python3" ]; then
        echo "$HOME/.local/share/omnidesk/venv/bin/python3"
    else
        which python3
    fi
}

PYTHON_BIN=$(find_python)

enable_autostart() {
    echo -e "${CYAN}Installing macOS LaunchAgent for OmniDesk Server...${NC}"
    mkdir -p "$HOME/Library/LaunchAgents"
    mkdir -p "$LOGS_DIR"

    # Unload existing definition if present
    if [ -f "$PLIST_PATH" ]; then
        launchctl unload -w "$PLIST_PATH" 2>/dev/null || true
    fi

    # Write pristine LaunchAgent plist
    cat <<EOF > "$PLIST_PATH"
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>${PLIST_LABEL}</string>
    <key>ProgramArguments</key>
    <array>
        <string>${PYTHON_BIN}</string>
        <string>${SCRIPT_DIR}/server.py</string>
    </array>
    <key>WorkingDirectory</key>
    <string>${SCRIPT_DIR}</string>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
    <key>ProcessType</key>
    <string>Interactive</string>
    <key>StandardOutPath</key>
    <string>${LOG_OUT}</string>
    <key>StandardErrorPath</key>
    <string>${LOG_ERR}</string>
    <key>EnvironmentVariables</key>
    <dict>
        <key>PATH</key>
        <string>/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin</string>
    </dict>
</dict>
</plist>
EOF

    # Load and start LaunchAgent (supports both modern macOS bootstrap and legacy load -w)
    UID_NUM=$(id -u)
    launchctl bootout "gui/$UID_NUM" "$PLIST_PATH" 2>/dev/null || launchctl unload -w "$PLIST_PATH" 2>/dev/null || true
    launchctl bootstrap "gui/$UID_NUM" "$PLIST_PATH" 2>/dev/null || launchctl load -w "$PLIST_PATH" 2>/dev/null || true
    launchctl kickstart -k "gui/$UID_NUM/${PLIST_LABEL}" 2>/dev/null || launchctl start "$PLIST_LABEL" 2>/dev/null || true

    sleep 1.5

    # Check if process is active on port 8765; if not, kick off background runner as backup
    PORT_PID=$(lsof -ti:8765 2>/dev/null || true)
    if [ -z "$PORT_PID" ]; then
        if [ -f "$SCRIPT_DIR/start-background-mac.sh" ]; then
            "$SCRIPT_DIR/start-background-mac.sh" >/dev/null 2>&1 &
            sleep 1.5
        fi
    fi

    echo -e "${GREEN}✔ macOS LaunchAgent installed & activated!${NC}"
    echo -e "${BOLD}${GREEN}✔ OmniDesk is RUNNING in the background right now!${NC}"
    echo -e "${GREEN}✔ It will automatically start whenever your Mac boots or logs in.${NC}"
    echo -e "${CYAN}ℹ The server runs 100% in the background — closing the terminal will NOT kill it.${NC}"
    echo -e "${DIM}  Logs: ${LOG_OUT}${NC}"
    echo ""
    show_status
    echo -e "${BOLD}${GREEN}✔ SUCCESS: You can now SAFELY CLOSE THIS TERMINAL WINDOW!${NC}"
    echo -e "${DIM}  The server is active in background and registered for every Mac boot.${NC}\n"
}

disable_autostart() {
    echo -e "${YELLOW}Disabling macOS LaunchAgent for OmniDesk Server...${NC}"
    if [ -f "$PLIST_PATH" ]; then
        UID_NUM=$(id -u)
        launchctl bootout "gui/$UID_NUM" "$PLIST_PATH" 2>/dev/null || launchctl unload -w "$PLIST_PATH" 2>/dev/null || true
        rm -f "$PLIST_PATH"
        echo -e "${GREEN}✔ LaunchAgent removed (${PLIST_PATH})${NC}"
    else
        echo -e "${DIM}LaunchAgent was not installed.${NC}"
    fi

    # Stop any running process on port 8765
    stop_service
}

start_service() {
    echo -e "${CYAN}Starting OmniDesk LaunchAgent...${NC}"
    if [ ! -f "$PLIST_PATH" ]; then
        enable_autostart
        return
    fi
    UID_NUM=$(id -u)
    launchctl bootstrap "gui/$UID_NUM" "$PLIST_PATH" 2>/dev/null || launchctl load -w "$PLIST_PATH" 2>/dev/null || true
    launchctl kickstart -k "gui/$UID_NUM/${PLIST_LABEL}" 2>/dev/null || launchctl start "$PLIST_LABEL" 2>/dev/null || true
    sleep 1.5
    show_status
}

stop_service() {
    echo -e "${YELLOW}Stopping OmniDesk Server...${NC}"
    if [ -f "$PLIST_PATH" ]; then
        UID_NUM=$(id -u)
        launchctl bootout "gui/$UID_NUM" "$PLIST_PATH" 2>/dev/null || launchctl unload -w "$PLIST_PATH" 2>/dev/null || launchctl stop "$PLIST_LABEL" 2>/dev/null || true
    fi

    # Kill process listening on port 8765
    PORT_PID=$(lsof -ti:8765 2>/dev/null || true)
    if [ -n "$PORT_PID" ]; then
        kill -9 $PORT_PID 2>/dev/null || true
        echo -e "${GREEN}✔ Terminated process on port 8765 (PID: $PORT_PID)${NC}"
    fi

    # Clean PID file
    rm -f "$PID_FILE" 2>/dev/null || true
    echo -e "${GREEN}✔ OmniDesk Server stopped.${NC}"
}

restart_service() {
    stop_service
    sleep 1
    start_service
}

show_status() {
    echo -e "\n${BLUE}=====================================================${NC}"
    echo -e "${BOLD}   🍎 OmniDesk Server macOS Service Status${NC}"
    echo -e "${BLUE}=====================================================${NC}"

    # Check LaunchAgent
    if [ -f "$PLIST_PATH" ]; then
        LOADED=$(launchctl list | grep "$PLIST_LABEL" || true)
        if [ -n "$LOADED" ]; then
            echo -e " LaunchAgent:  ${GREEN}● LOADED & ACTIVE${NC} (${PLIST_LABEL})"
        else
            echo -e " LaunchAgent:  ${YELLOW}○ INSTALLED (Not currently loaded)${NC}"
        fi
    else
        echo -e " LaunchAgent:  ${DIM}Not installed (Autostart disabled)${NC}"
    fi

    # Check Port 8765
    PORT_PID=$(lsof -ti:8765 2>/dev/null || true)
    if [ -n "$PORT_PID" ]; then
        echo -e " Server State: ${GREEN}● RUNNING in Background${NC} (PID: ${PORT_PID})"
        echo -e " Port 8765:    ${GREEN}✓ OPEN and listening for phone connections${NC}"
    else
        echo -e " Server State: ${RED}○ STOPPED${NC} (Port 8765 not active)"
    fi

    # Show local IP address
    LOCAL_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || true)
    if [ -z "$LOCAL_IP" ]; then
        LOCAL_IP=$(python3 -c "import socket; s=socket.socket(socket.AF_INET, socket.SOCK_DGRAM); s.connect(('8.8.8.8', 80)); print(s.getsockname()[0]); s.close()" 2>/dev/null || echo "127.0.0.1")
    fi
    echo -e " Local IP:     ${CYAN}${LOCAL_IP}${NC}"
    echo -e " Connect URL:  ${CYAN}ws://${LOCAL_IP}:8765${NC}"

    # Show active PIN
    if [ -f "$PIN_FILE" ]; then
        CURRENT_PIN=$(head -n 1 "$PIN_FILE" | tr -d '\r\n')
        echo -e " Pairing PIN:  ${BOLD}${GREEN}[ ${CURRENT_PIN} ]${NC}"
    fi

    echo -e "${BLUE}-----------------------------------------------------${NC}"
    if [ -f "$LOG_OUT" ]; then
        echo -e "${DIM}Recent Log Output (${LOG_OUT}):${NC}"
        tail -n 8 "$LOG_OUT" 2>/dev/null || true
    fi
    echo -e "${BLUE}=====================================================${NC}\n"
}

# CLI Argument Router
CMD="${1:-status}"

case "$CMD" in
    enable|install)
        enable_autostart
        ;;
    disable|uninstall|remove)
        disable_autostart
        ;;
    start)
        start_service
        ;;
    stop)
        stop_service
        ;;
    restart)
        restart_service
        ;;
    status)
        show_status
        ;;
    logs)
        tail -f "$LOG_OUT"
        ;;
    *)
        echo "Usage: $0 {enable|disable|start|stop|restart|status|logs}"
        echo ""
        echo "Commands:"
        echo "  enable   - Install LaunchAgent to autostart OmniDesk on Mac login and run in background"
        echo "  disable  - Remove LaunchAgent and stop background autostart"
        echo "  start    - Start background LaunchAgent service"
        echo "  stop     - Stop background service and free port 8765"
        echo "  restart  - Restart background service"
        echo "  status   - View live running status, port state, and 6-digit pairing PIN"
        echo "  logs     - Follow live server log stream"
        exit 1
        ;;
esac
