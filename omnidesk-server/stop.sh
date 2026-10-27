#!/usr/bin/env bash
# ==============================================================================
# OmniDesk Companion Server - Universal Stop Script (Linux & macOS)
# ==============================================================================

OS_NAME="$(uname -s)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ "$OS_NAME" == "Darwin" ]; then
    exec "$SCRIPT_DIR/stop-mac.sh"
fi

GREEN="\033[0;32m"
YELLOW="\033[1;33m"
NC="\033[0m"

echo -e "${YELLOW}Stopping OmniDesk Server on Linux...${NC}"

# 1. Stop systemd user service if running
if command -v systemctl &>/dev/null; then
    systemctl --user stop omnidesk.service 2>/dev/null || true
    systemctl --user stop linux-remote.service 2>/dev/null || true
fi

# 2. Stop PID from PID_FILE
PID_FILE="$HOME/.config/omnidesk/server.pid"
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE" 2>/dev/null || true)
    if [ -n "$PID" ]; then
        kill -15 "$PID" 2>/dev/null || true
        sleep 0.5
        kill -9 "$PID" 2>/dev/null || true
    fi
    rm -f "$PID_FILE"
fi

# 3. Kill process on port 8765 if still open
if command -v fuser &>/dev/null; then
    fuser -k 8765/tcp 2>/dev/null || true
fi

# 4. Clean any lingering python server.py processes
pkill -f "python.*server.py" 2>/dev/null || true

echo -e "${GREEN}✔ OmniDesk Server stopped successfully.${NC}"
