#!/bin/bash
# ==============================================================================
# Linux Remote - USB Cable Mode Helper
# Enables instant connection between your Android phone and Linux PC over a USB cable
# ==============================================================================

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}=== Linux Remote USB Cable Setup ===${NC}"
echo "Checking ADB (Android Debug Bridge)..."

if ! command -v adb &> /dev/null; then
    echo -e "${YELLOW}ADB is not installed. Installing adb...${NC}"
    sudo apt update -qq && sudo apt install -y adb
fi

echo "Checking connected USB Android devices..."
DEVICES=$(adb devices | grep -w "device" | awk '{print $1}')

if [ -z "$DEVICES" ]; then
    echo -e "${YELLOW}No authorized Android device detected over USB.${NC}"
    echo "1. Connect your phone to your PC with a USB cable."
    echo "2. On your phone: enable Developer Options -> USB Debugging."
    echo "3. When prompted on your phone screen, tap 'Allow USB debugging'."
    echo ""
    echo "Retrying detection in 5 seconds..."
    sleep 5
    DEVICES=$(adb devices | grep -w "device" | awk '{print $1}')
fi

if [ -n "$DEVICES" ]; then
    echo -e "${GREEN}✓ Found connected Android device: $DEVICES${NC}"
    echo "Forwarding remote port 8765 to phone over USB..."
    adb reverse tcp:8765 tcp:8765
    echo -e "${GREEN}✓ ADB reverse port forwarding established successfully!${NC}"
    echo ""
    echo -e "${GREEN}Your Android phone can now connect to Linux Remote with 0ms latency over USB!${NC}"
    echo "In the app, tap 'Use USB Cable (127.0.0.1)' or tap the USB button."
else
    echo -e "${RED}Could not establish ADB USB connection.${NC}"
    echo "Alternative: Turn on 'USB Tethering' in your phone's Settings -> Network & Internet -> Hotspot & tethering."
fi
