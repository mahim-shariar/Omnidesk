#!/usr/bin/env bash
# ==============================================================================
# OmniDesk Companion Server - Universal Uninstaller (macOS & Linux)
# ==============================================================================

set -e

RED="\033[0;31m"
GREEN="\033[0;32m"
YELLOW="\033[1;33m"
NC="\033[0m"

echo -e "${RED}Uninstalling OmniDesk Companion Server...${NC}"

# macOS LaunchAgent cleanup
if [ "$(uname -s)" == "Darwin" ]; then
    PLIST="$HOME/Library/LaunchAgents/com.omnidesk.server.plist"
    if [ -f "$PLIST" ]; then
        launchctl stop com.omnidesk.server 2>/dev/null || true
        launchctl unload -w "$PLIST" 2>/dev/null || true
        rm -f "$PLIST"
        echo "Removed macOS LaunchAgent."
    fi
fi

# Linux systemd cleanup
if command -v systemctl &>/dev/null; then
    systemctl --user stop omnidesk.service 2>/dev/null || true
    systemctl --user disable omnidesk.service 2>/dev/null || true
    systemctl --user stop linux-remote.service 2>/dev/null || true
    systemctl --user disable linux-remote.service 2>/dev/null || true
    rm -f "$HOME/.config/systemd/user/omnidesk.service" 2>/dev/null || true
    rm -f "$HOME/.config/systemd/user/linux-remote.service" 2>/dev/null || true
    systemctl --user daemon-reload 2>/dev/null || true
fi

# Stop any running process on port 8765
if command -v lsof &>/dev/null; then
    kill -9 $(lsof -ti:8765 2>/dev/null) 2>/dev/null || true
elif command -v fuser &>/dev/null; then
    fuser -k 8765/tcp 2>/dev/null || true
fi

# Remove installed files
rm -rf "$HOME/.local/share/omnidesk"
rm -rf "$HOME/.local/share/linux-remote"
rm -f "$HOME/Desktop/Start OmniDesk.command" "$HOME/Desktop/omnidesk.desktop"
rm -f "$HOME/Library/Logs/OmniDesk.log" "$HOME/Library/Logs/OmniDesk-error.log"

# Optionally ask or clean config
read -p "Do you want to delete paired devices and configurations? (y/N) " confirm
if [[ "$confirm" =~ ^[Yy]$ ]]; then
    rm -rf "$HOME/.config/omnidesk"
    rm -rf "$HOME/.config/linux-remote"
    echo "Removed configuration and paired devices."
fi

echo -e "${GREEN}OmniDesk Companion Server uninstalled successfully.${NC}"
