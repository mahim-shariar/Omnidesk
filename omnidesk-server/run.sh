#!/usr/bin/env bash
# Universal launcher for macOS and Linux
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

if [ -f "$DIR/.venv/bin/python3" ]; then
    PY="$DIR/.venv/bin/python3"
elif [ -f "$HOME/.local/share/omnidesk/venv/bin/python3" ]; then
    PY="$HOME/.local/share/omnidesk/venv/bin/python3"
elif [ -f "$HOME/.local/share/linux-remote/venv/bin/python3" ]; then
    PY="$HOME/.local/share/linux-remote/venv/bin/python3"
elif [ -d "venv" ]; then
    PY="venv/bin/python3"
else
    PY="python3"
fi

if ! "$PY" -c "import websockets" &>/dev/null; then
    if [ -f "$DIR/install.sh" ]; then
        exec "$DIR/install.sh"
    fi
fi

exec "$PY" server.py
