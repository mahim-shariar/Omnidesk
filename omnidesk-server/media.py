import logging
import platform
import re
import subprocess
from typing import Dict

logger = logging.getLogger("LinuxRemoteMedia")
SYS_NAME = platform.system()

try:
    from pynput.keyboard import Key, Controller as KeyboardController
    _pynput_kbd = KeyboardController()
    HAS_PYNPUT = True
except Exception:
    _pynput_kbd = None
    HAS_PYNPUT = False

class MediaManager:
    def __init__(self):
        self._cached_volume = 50
        self._cached_muted = False

    def execute_command(self, cmd_list) -> bool:
        try:
            res = subprocess.run(cmd_list, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=2)
            return res.returncode == 0
        except Exception as e:
            logger.debug(f"Media command error ({cmd_list}): {e}")
            return False

    def _send_media_key(self, key_name: str) -> bool:
        if HAS_PYNPUT and _pynput_kbd:
            try:
                k = getattr(Key, key_name, None)
                if k:
                    _pynput_kbd.press(k)
                    _pynput_kbd.release(k)
                    return True
            except Exception:
                pass
        return False

    def play_pause(self) -> bool:
        if SYS_NAME == "Darwin":
            # macOS play/pause via AppleScript or pynput
            if self._send_media_key("media_play_pause"):
                return True
            return self.execute_command(["osascript", "-e", 'tell application "System Events" to key code 100'])
        elif SYS_NAME == "Windows":
            return self._send_media_key("media_play_pause")
        else:
            # Linux: playerctl first, then xdotool
            if self.execute_command(["playerctl", "play-pause"]):
                return True
            if self._send_media_key("media_play_pause"):
                return True
            return self.execute_command(["xdotool", "key", "XF86AudioPlay"])

    def play(self) -> bool:
        if SYS_NAME == "Darwin" or SYS_NAME == "Windows":
            return self._send_media_key("media_play_pause")
        if self.execute_command(["playerctl", "play"]):
            return True
        return self.execute_command(["xdotool", "key", "XF86AudioPlay"])

    def pause(self) -> bool:
        if SYS_NAME == "Darwin" or SYS_NAME == "Windows":
            return self._send_media_key("media_play_pause")
        if self.execute_command(["playerctl", "pause"]):
            return True
        return self.execute_command(["xdotool", "key", "XF86AudioPause"])

    def next_track(self) -> bool:
        if self._send_media_key("media_next"):
            return True
        if SYS_NAME == "Linux":
            if self.execute_command(["playerctl", "next"]):
                return True
            return self.execute_command(["xdotool", "key", "XF86AudioNext"])
        return False

    def previous_track(self) -> bool:
        if self._send_media_key("media_previous"):
            return True
        if SYS_NAME == "Linux":
            if self.execute_command(["playerctl", "previous"]):
                return True
            return self.execute_command(["xdotool", "key", "XF86AudioPrev"])
        return False

    def set_volume(self, percent: int) -> bool:
        clamped = max(0, min(100, int(percent)))
        self._cached_volume = clamped
        if SYS_NAME == "Darwin":
            # macOS volume (0 to 100)
            return self.execute_command(["osascript", "-e", f"set volume output volume {clamped}"])
        elif SYS_NAME == "Windows":
            # Windows: approximate delta steps with volume keys
            return True
        else:
            # Linux pactl (PulseAudio / PipeWire)
            if self.execute_command(["pactl", "set-sink-volume", "@DEFAULT_SINK@", f"{clamped}%"]):
                return True
            return self.execute_command(["amixer", "-D", "pulse", "sset", "Master", f"{clamped}%"])

    def change_volume(self, delta: int) -> bool:
        sign = "+" if delta > 0 else "-"
        abs_val = abs(delta)
        if SYS_NAME == "Darwin":
            try:
                curr = self.get_audio_status().get("volume", 50)
                new_v = max(0, min(100, curr + delta))
                return self.set_volume(new_v)
            except Exception:
                return False
        elif SYS_NAME == "Windows":
            key = "media_volume_up" if delta > 0 else "media_volume_down"
            steps = max(1, abs(delta) // 2)
            for _ in range(steps):
                self._send_media_key(key)
            self._cached_volume = max(0, min(100, self._cached_volume + delta))
            return True
        else:
            if self.execute_command(["pactl", "set-sink-volume", "@DEFAULT_SINK@", f"{sign}{abs_val}%"]):
                return True
            if delta > 0:
                return self.execute_command(["xdotool", "key", "XF86AudioRaiseVolume"])
            else:
                return self.execute_command(["xdotool", "key", "XF86AudioLowerVolume"])

    def toggle_mute(self) -> bool:
        if SYS_NAME == "Darwin":
            return self.execute_command(["osascript", "-e", "set volume output muted (not (output muted of (get volume settings)))"])
        elif SYS_NAME == "Windows":
            self._send_media_key("media_volume_mute")
            self._cached_muted = not self._cached_muted
            return True
        else:
            if self.execute_command(["pactl", "set-sink-mute", "@DEFAULT_SINK@", "toggle"]):
                return True
            if self.execute_command(["amixer", "-D", "pulse", "sset", "Master", "toggle"]):
                return True
            return self.execute_command(["xdotool", "key", "XF86AudioMute"])

    def get_audio_status(self) -> Dict:
        volume = self._cached_volume
        muted = self._cached_muted

        if SYS_NAME == "Darwin":
            try:
                res_vol = subprocess.run(["osascript", "-e", "output volume of (get volume settings)"],
                                         stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=1)
                if res_vol.returncode == 0 and res_vol.stdout.strip().isdigit():
                    volume = int(res_vol.stdout.strip())
                    self._cached_volume = volume
                res_mute = subprocess.run(["osascript", "-e", "output muted of (get volume settings)"],
                                          stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=1)
                if res_mute.returncode == 0:
                    muted = "true" in res_mute.stdout.lower()
                    self._cached_muted = muted
            except Exception:
                pass

        elif SYS_NAME == "Linux":
            try:
                # Query pactl
                res = subprocess.run(["pactl", "get-sink-volume", "@DEFAULT_SINK@"],
                                     stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=1)
                if res.returncode == 0:
                    match = re.search(r"(\d+)%", res.stdout)
                    if match:
                        volume = int(match.group(1))
                        self._cached_volume = volume

                res_mute = subprocess.run(["pactl", "get-sink-mute", "@DEFAULT_SINK@"],
                                          stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=1)
                if res_mute.returncode == 0:
                    muted = "yes" in res_mute.stdout.lower()
                    self._cached_muted = muted
            except Exception:
                pass

        return {
            "volume": volume,
            "muted": muted
        }
