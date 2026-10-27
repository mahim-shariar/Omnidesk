#!/usr/bin/env bash
# ==============================================================================
# OmniDesk Companion Server - Linux Autostart Manager (systemd user service)
# ==============================================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_NAME="omnidesk.service"
SYSTEMD_USER_DIR="$HOME/.config/systemd/user"
SERVICE_FILE="$SYSTEMD_USER_DIR/$SERVICE_NAME"

GREEN="\033[0;32m"
BLUE="\033[0;34m"
CYAN="\033[0;36m"
YELLOW="\033[1;33m"
RED="\033[0;31m"
BOLD="\033[1m"
NC="\033[0m"

# Find python
if [ -f "$SCRIPT_DIR/.venv/bin/python3" ]; then
    PYTHON_BIN="$SCRIPT_DIR/.venv/bin/python3"
elif [ -f "$SCRIPT_DIR/venv/bin/python3" ]; then
    PYTHON_BIN="$SCRIPT_DIR/venv/bin/python3"
else
    PYTHON_BIN="$(which python3)"
fi

enable_service() {
    echo -e "${CYAN}Configuring systemd user service for OmniDesk...${NC}"
    mkdir -p "$SYSTEMD_USER_DIR"

    cat <<EOF > "$SERVICE_FILE"
[Unit]
Description=OmniDesk Companion Server
After=network.target

[Service]
Type=simple
Environment="DISPLAY=:0"
WorkingDirectory=${SCRIPT_DIR}
ExecStart=${PYTHON_BIN} ${SCRIPT_DIR}/server.py
Restart=on-failure
RestartSec=3

[Install]
WantedBy=default.target
EOF

    systemctl --user daemon-reload
    systemctl --user enable "$SERVICE_NAME"
    systemctl --user restart "$SERVICE_NAME"

    echo -e "${GREEN}✔ Systemd user service enabled and started!${NC}"
    echo -e "${GREEN}✔ OmniDesk will now start automatically whenever your PC boots or logs in.${NC}"
    echo -e "${CYAN}ℹ Runs 100% in the background independent of any terminal.${NC}"
    echo ""
    "$SCRIPT_DIR/status.sh"
}

disable_service() {
    echo -e "${YELLOW}Disabling systemd user service for OmniDesk...${NC}"
    systemctl --user stop "$SERVICE_NAME" 2>/dev/null || true
    systemctl --user disable "$SERVICE_NAME" 2>/dev/null || true
    rm -f "$SERVICE_FILE"
    systemctl --user daemon-reload
    echo -e "${GREEN}✔ Autostart service disabled!${NC}"
}

CMD="${1:-status}"
case "$CMD" in
    enable|install)
        enable_service
        ;;
    disable|uninstall|remove)
        disable_service
        ;;
    start)
        systemctl --user start "$SERVICE_NAME"
        "$SCRIPT_DIR/status.sh"
        ;;
    stop)
        systemctl --user stop "$SERVICE_NAME"
        echo -e "${GREEN}✔ Service stopped.${NC}"
        ;;
    restart)
        systemctl --user restart "$SERVICE_NAME"
        "$SCRIPT_DIR/status.sh"
        ;;
    status)
        "$SCRIPT_DIR/status.sh"
        ;;
    *)
        echo "Usage: $0 {enable|disable|start|stop|restart|status}"
        exit 1
        ;;
esac
