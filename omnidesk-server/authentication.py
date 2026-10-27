import json
import logging
import random
import secrets
import time
from typing import Dict, Optional
from config import PAIRED_DEVICES_FILE, PAIRING_PIN_FILE

logger = logging.getLogger("LinuxRemoteAuth")

class AuthManager:
    def __init__(self):
        self.paired_devices: Dict[str, Dict] = {} # token -> {device_id, device_name, paired_at}
        self.active_pin: Optional[str] = None
        self.pin_expires_at: float = 0
        # pin -> expires_at timestamp (keeps recent valid PINs for 15 mins to prevent race conditions)
        self.valid_pins: Dict[str, float] = {}
        self.load_paired_devices()
        self._load_existing_pin_file()

    def _load_existing_pin_file(self):
        if PAIRING_PIN_FILE.exists():
            try:
                content = PAIRING_PIN_FILE.read_text().strip().splitlines()
                if content:
                    pin = content[0].strip()
                    if len(pin) >= 4:
                        self.active_pin = pin
                        self.pin_expires_at = time.time() + 900 # 15 minutes
                        self.valid_pins[pin] = self.pin_expires_at
            except Exception as e:
                logger.debug(f"Could not load initial pin file: {e}")

    def load_paired_devices(self):
        if PAIRED_DEVICES_FILE.exists():
            try:
                with open(PAIRED_DEVICES_FILE, "r") as f:
                    self.paired_devices = json.load(f)
                logger.info(f"Loaded {len(self.paired_devices)} paired device(s)")
            except Exception as e:
                logger.error(f"Error loading paired devices: {e}")
                self.paired_devices = {}
        else:
            self.paired_devices = {}

    def save_paired_devices(self):
        try:
            with open(PAIRED_DEVICES_FILE, "w") as f:
                json.dump(self.paired_devices, f, indent=2)
        except Exception as e:
            logger.error(f"Error saving paired devices: {e}")

    def generate_pairing_pin(self, force_new: bool = False) -> str:
        now = time.time()
        # If an active PIN already exists and is still valid (more than 60s remaining),
        # keep it so users reading the terminal/screen aren't surprised by an invalidation.
        if not force_new and self.active_pin and now < (self.pin_expires_at - 60):
            logger.info(f"Re-using active pairing PIN: [ {self.active_pin} ] (expires in {int(self.pin_expires_at - now)}s)")
            return self.active_pin

        pin = f"{random.randint(100000, 999999)}"
        self.active_pin = pin
        self.pin_expires_at = now + 900 # 15 minutes validity
        self.valid_pins[pin] = self.pin_expires_at

        # Save to file for easy inspection, QR generator, or desktop notification
        try:
            with open(PAIRING_PIN_FILE, "w") as f:
                f.write(f"{pin}\nExpires in 15 minutes\n")
        except Exception as e:
            logger.warning(f"Could not write PIN file: {e}")

        logger.info("\n" + "="*45)
        logger.info("   OMNIDESK - ACTIVE PAIRING CODE")
        logger.info(f"             [ {pin} ]")
        logger.info("  Enter this 6-digit code on your phone")
        logger.info("  (Valid for 15 minutes)")
        logger.info("="*45 + "\n")
        return pin

    def verify_pairing_pin(self, entered_pin: str, device_id: str, device_name: str) -> Optional[str]:
        now = time.time()
        import re
        entered = re.sub(r'[^a-zA-Z0-9]', '', str(entered_pin)).strip()
        if not entered:
            logger.warning("Empty PIN submitted")
            return None

        # Clean expired PINs
        self.valid_pins = {p: exp for p, exp in self.valid_pins.items() if exp > now}

        # Check in memory active PINs
        is_valid = False
        if entered in self.valid_pins:
            is_valid = True
        elif self.active_pin and entered == self.active_pin and now <= self.pin_expires_at:
            is_valid = True
        else:
            # Check PAIRING_PIN_FILE as fallback
            if PAIRING_PIN_FILE.exists():
                try:
                    file_pin = PAIRING_PIN_FILE.read_text().strip().splitlines()[0].strip()
                    if entered == file_pin:
                        is_valid = True
                except Exception:
                    pass

        if not is_valid:
            logger.warning(f"Incorrect or expired PIN attempt: '{entered}' (Active PIN: {self.active_pin})")
            return None

        # Pairing successful, generate long-lived token
        token = secrets.token_hex(32)
        self.paired_devices[token] = {
            "device_id": device_id,
            "device_name": device_name,
            "paired_at": now
        }
        self.save_paired_devices()
        logger.info(f"Successfully paired device: {device_name} ({device_id}) with token suffix ...{token[-6:]}")

        return token

    def is_token_valid(self, token: str) -> bool:
        if not token:
            return False
        return token in self.paired_devices

    def revoke_device(self, token: str) -> bool:
        if token in self.paired_devices:
            dev = self.paired_devices.pop(token)
            self.save_paired_devices()
            logger.info(f"Revoked paired device: {dev.get('device_name')}")
            return True
        return False

    def get_paired_devices_list(self):
        devices = []
        for token, info in self.paired_devices.items():
            devices.append({
                "device_id": info.get("device_id"),
                "device_name": info.get("device_name"),
                "paired_at": info.get("paired_at"),
                "token_suffix": token[-6:]
            })
        return devices
