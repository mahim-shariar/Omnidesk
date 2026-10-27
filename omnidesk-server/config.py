import os
from pathlib import Path

# OmniDesk Universal PC Remote Server Configuration
APP_NAME = "OmniDesk"
VERSION = "1.0.0"

# Network settings
WS_HOST = "0.0.0.0"
WS_PORT = 8765
DISCOVERY_PORT = 8766
MDNS_SERVICE_TYPE = "_linuxremote._tcp.local."

# Configuration paths (OmniDesk with fallback to legacy path)
LEGACY_DIR = Path(os.path.expanduser("~/.config/linux-remote"))
CONFIG_DIR = Path(os.path.expanduser("~/.config/omnidesk"))
if LEGACY_DIR.exists() and not CONFIG_DIR.exists():
    CONFIG_DIR = LEGACY_DIR
else:
    CONFIG_DIR.mkdir(parents=True, exist_ok=True)

PAIRED_DEVICES_FILE = CONFIG_DIR / "paired_devices.json"
SERVER_CONFIG_FILE = CONFIG_DIR / "config.json"
PAIRING_PIN_FILE = CONFIG_DIR / "current_pin.txt"
PID_FILE = CONFIG_DIR / "server.pid"

# Heartbeat & Telemetry interval (seconds)
TELEMETRY_INTERVAL = 2.0

# Rate limits (events per second)
MAX_MOUSE_EVENTS_PER_SEC = 240
MAX_KEY_EVENTS_PER_SEC = 100
MAX_SYSTEM_ACTIONS_PER_MIN = 20

# Whitelisted system commands for safe execution
SYSTEM_COMMANDS_WHITELIST = {
    "lock",
    "suspend",
    "restart",
    "shutdown",
    "show_desktop",
    "screenshot",
    "open_terminal",
    "display_off",
    "display_on",
    "system_monitor",
    "cinnamon_restart",
    "explorer_restart",
    "finder_restart"
}
