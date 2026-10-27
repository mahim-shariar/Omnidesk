#!/usr/bin/env bash
# ==============================================================================
# OmniDesk Companion Server - Stop Server (macOS)
# Stops both launchctl background service and any running standalone server instances
# ==============================================================================

PLIST_LABEL="com.omnidesk.server"
PLIST_PATH="$HOME/Library/LaunchAgents/${PLIST_LABEL}.plist"
PID_FILE="$HOME/.config/omnidesk/server.pid"

GREEN="\033[0;32m"
YELLOW="\033[1;33m"
NC="\033[0m"

echo -e "${YELLOW}Stopping OmniDesk Server on macOS...${NC}"

# 1. Stop LaunchAgent if running
if [ -f "$PLIST_PATH" ]; then
    launchctl stop "$PLIST_LABEL" 2>/dev/null || true
fi

# 2. Stop PID from PID_FILE
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE" 2>/dev/null || true)
    if [ -n "$PID" ]; then
        kill -15 "$PID" 2>/dev/null || true
        sleep 0.5
        kill -9 "$PID" 2>/dev/null || true
    fi
    rm -f "$PID_FILE"
fi

# 3. Kill any remaining process listening on port 8765
PORT_PID=$(lsof -ti:8765 2>/dev/null || true)
if [ -n "$PORT_PID" ]; then
    kill -9 $PORT_PID 2>/dev/null || true
    echo -e "${GREEN}✔ Killed process on port 8765 (PID: $PORT_PID)${NC}"
fi

# 4. Clean any hanging python server.py instances
pkill -f "python.*server.py" 2>/dev/null || true

echo -e "${GREEN}✔ OmniDesk Server stopped successfully.${NC}"
