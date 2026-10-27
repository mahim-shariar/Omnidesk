#!/usr/bin/env python3
"""
OmniDesk - Desktop QR Code Pairing Generator
Generates a QR code for instant 1-scan mobile pairing with OmniDesk Companion Server.
Compatible with Windows, macOS, and Linux.
"""

import json
import os
import platform
import socket
import sys
import webbrowser
from pathlib import Path

# Paths
CONFIG_DIR = Path(os.path.expanduser("~/.config/omnidesk"))
if not CONFIG_DIR.exists():
    legacy_dir = Path(os.path.expanduser("~/.config/linux-remote"))
    if legacy_dir.exists():
        CONFIG_DIR = legacy_dir
    else:
        CONFIG_DIR.mkdir(parents=True, exist_ok=True)

PIN_FILE = CONFIG_DIR / "current_pin.txt"

def get_local_ip() -> str:
    """Finds active non-loopback local network IP."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        try:
            return socket.gethostbyname(socket.gethostname())
        except Exception:
            return "127.0.0.1"

def get_or_create_pin() -> str:
    """Reads existing PIN or generates a fresh 6-digit PIN."""
    if PIN_FILE.exists():
        try:
            pin = PIN_FILE.read_text().strip().splitlines()[0]
            if len(pin) >= 4:
                return pin
        except Exception:
            pass

    import random
    new_pin = f"{random.randint(100000, 999999)}"
    try:
        PIN_FILE.write_text(new_pin)
    except Exception:
        pass
    return new_pin

def main():
    ip = get_local_ip()
    port = 8765
    hostname = socket.gethostname()
    os_name = platform.system()
    pin = get_or_create_pin()

    payload_dict = {
        "protocol": "omnidesk",
        "ip": ip,
        "port": port,
        "pin": pin,
        "hostname": hostname,
        "os": os_name
    }
    payload_json = json.dumps(payload_dict)
    payload_uri = f"omnidesk://{ip}:{port}?pin={pin}&name={hostname}&os={os_name}"

    print("\n" + "=" * 60)
    print("      OMNIDESK - DESKTOP PAIRING QR CODE GENERATOR      ")
    print("=" * 60)
    print(f"Computer Hostname : {hostname}")
    print(f"Operating System  : {os_name}")
    print(f"Local IP Address  : {ip}")
    print(f"WebSocket Port    : {port}")
    print(f"Pairing PIN Code  : {pin}")
    print("-" * 60)

    # Attempt terminal ASCII QR generation if qrcode library is present
    has_terminal_qr = False
    try:
        import qrcode
        qr = qrcode.QRCode(border=1)
        qr.add_data(payload_json)
        qr.make(fit=True)
        print("\nScan this QR code with the OmniDesk mobile app:\n")
        qr.print_ascii(invert=True)
        has_terminal_qr = True

        # Also save PNG
        try:
            img = qr.make_image(fill_color="black", back_color="white")
            png_path = Path(__file__).parent / "pairing_qr.png"
            img.save(png_path)
            print(f"Saved QR code image to: {png_path}")
        except Exception:
            pass
    except ImportError:
        pass

    # Generate a sleek HTML visual QR page and open in desktop browser
    import urllib.parse
    html_file = Path(__file__).parent / "pairing_qr.html"
    encoded_payload = urllib.parse.quote(payload_json)
    
    # Use reliable QR code image rendering via qrserver API
    qr_img_url = f"https://api.qrserver.com/v1/create-qr-code/?size=300x300&data={encoded_payload}"

    html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>OmniDesk - Pair with {hostname}</title>
    <style>
        body {{
            background: #0B0F14;
            color: #F3F4F6;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            margin: 0;
            padding: 20px;
            box-sizing: border-box;
        }}
        .card {{
            background: #131A22;
            border: 1px solid #2A3647;
            border-radius: 20px;
            padding: 32px;
            max-width: 440px;
            width: 100%;
            text-align: center;
            box-shadow: 0 20px 40px rgba(0,0,0,0.6);
        }}
        .badge {{
            display: inline-block;
            background: rgba(16, 185, 129, 0.15);
            color: #10B981;
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 600;
            margin-bottom: 12px;
        }}
        h1 {{
            margin: 0 0 6px 0;
            font-size: 24px;
            font-weight: 700;
        }}
        p.subtitle {{
            color: #94A3B8;
            font-size: 14px;
            margin: 0 0 24px 0;
        }}
        .qr-box {{
            background: #FFFFFF;
            padding: 16px;
            border-radius: 16px;
            display: inline-block;
            margin-bottom: 20px;
            box-shadow: 0 4px 16px rgba(0,0,0,0.3);
        }}
        .qr-box img {{
            display: block;
            width: 240px;
            height: 240px;
        }}
        .pin-pill {{
            background: #1E2836;
            border: 1px solid #334155;
            padding: 12px;
            border-radius: 12px;
            font-family: monospace;
            font-size: 24px;
            letter-spacing: 6px;
            font-weight: bold;
            color: #34D399;
            margin-bottom: 16px;
        }}
        .details {{
            font-size: 13px;
            color: #94A3B8;
            line-height: 1.6;
            border-top: 1px solid #1E293B;
            padding-top: 16px;
        }}
    </style>
</head>
<body>
    <div class="card">
        <span class="badge">OmniDesk Universal Companion</span>
        <h1>Pair with {hostname}</h1>
        <p class="subtitle">Open OmniDesk on your phone and scan this code</p>
        
        <div class="qr-box">
            <img src="{qr_img_url}" alt="Pairing QR Code">
        </div>

        <div style="font-size: 12px; color: #94A3B8; margin-bottom: 6px;">Manual 6-Digit PIN:</div>
        <div class="pin-pill">{pin}</div>

        <div class="details">
            Host IP: <strong>{ip}:{port}</strong><br>
            OS: <strong>{os_name}</strong> • Pure Local Wi-Fi
        </div>
    </div>
</body>
</html>"""

    html_file.write_text(html_content, encoding="utf-8")
    print(f"\nCreated pairing web page: {html_file}")
    
    # Automatically open in desktop web browser
    try:
        webbrowser.open(html_file.as_uri())
        print("Opened QR pairing screen in your default browser.")
    except Exception:
        pass

    print("\nPairing Payload (JSON):")
    print(payload_json)
    print("\nPairing Payload (URI):")
    print(payload_uri)
    print("\nReady! Scan the QR code or enter the 6-digit PIN on your phone to complete pairing.\n")

if __name__ == "__main__":
    main()
