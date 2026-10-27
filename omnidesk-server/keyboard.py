import logging
import os
import platform
import subprocess
from typing import List, Union

logger = logging.getLogger("LinuxRemoteKeyboard")

try:
    from pynput.keyboard import Key, Controller as KeyboardController
    pynput_kbd = KeyboardController()
    HAS_PYNPUT = True
except Exception:
    HAS_PYNPUT = False
    pynput_kbd = None

try:
    import pyautogui
    HAS_PYAUTOGUI = True
except Exception:
    HAS_PYAUTOGUI = False

def get_desktop_env():
    env = dict(os.environ)
    if "DISPLAY" not in env or not env["DISPLAY"]:
        env["DISPLAY"] = ":0"
    if "XAUTHORITY" not in env or not env["XAUTHORITY"]:
        default_xauth = os.path.expanduser("~/.Xauthority")
        if os.path.exists(default_xauth):
            env["XAUTHORITY"] = default_xauth
    return env

# Mapping of special key strings to pynput / pyautogui keys
SPECIAL_KEYS_MAP = {
    "ESC": ("esc", Key.esc if HAS_PYNPUT else None),
    "ESCAPE": ("esc", Key.esc if HAS_PYNPUT else None),
    "TAB": ("tab", Key.tab if HAS_PYNPUT else None),
    "ENTER": ("enter", Key.enter if HAS_PYNPUT else None),
    "RETURN": ("enter", Key.enter if HAS_PYNPUT else None),
    "BACKSPACE": ("backspace", Key.backspace if HAS_PYNPUT else None),
    "BKSP": ("backspace", Key.backspace if HAS_PYNPUT else None),
    "BS": ("backspace", Key.backspace if HAS_PYNPUT else None),
    "DELETE": ("delete", Key.delete if HAS_PYNPUT else None),
    "DEL": ("delete", Key.delete if HAS_PYNPUT else None),
    "SPACE": ("space", Key.space if HAS_PYNPUT else None),
    "SHIFT": ("shift", Key.shift if HAS_PYNPUT else None),
    "CTRL": ("ctrl", Key.ctrl if HAS_PYNPUT else None),
    "CONTROL": ("ctrl", Key.ctrl if HAS_PYNPUT else None),
    "ALT": ("alt", Key.alt if HAS_PYNPUT else None),
    "OPTION": ("alt", Key.alt if HAS_PYNPUT else None),
    "SUPER": ("win", Key.cmd if HAS_PYNPUT else None),
    "WIN": ("win", Key.cmd if HAS_PYNPUT else None),
    "CMD": ("command", Key.cmd if HAS_PYNPUT else None),
    "COMMAND": ("command", Key.cmd if HAS_PYNPUT else None),
    "UP": ("up", Key.up if HAS_PYNPUT else None),
    "DOWN": ("down", Key.down if HAS_PYNPUT else None),
    "LEFT": ("left", Key.left if HAS_PYNPUT else None),
    "RIGHT": ("right", Key.right if HAS_PYNPUT else None),
    "PAGE_UP": ("pageup", Key.page_up if HAS_PYNPUT else None),
    "PAGE_DOWN": ("pagedown", Key.page_down if HAS_PYNPUT else None),
    "HOME": ("home", Key.home if HAS_PYNPUT else None),
    "END": ("end", Key.end if HAS_PYNPUT else None),
    "F1": ("f1", Key.f1 if HAS_PYNPUT and hasattr(Key, "f1") else None),
    "F2": ("f2", Key.f2 if HAS_PYNPUT and hasattr(Key, "f2") else None),
    "F3": ("f3", Key.f3 if HAS_PYNPUT and hasattr(Key, "f3") else None),
    "F4": ("f4", Key.f4 if HAS_PYNPUT and hasattr(Key, "f4") else None),
    "F5": ("f5", Key.f5 if HAS_PYNPUT and hasattr(Key, "f5") else None),
    "F6": ("f6", Key.f6 if HAS_PYNPUT and hasattr(Key, "f6") else None),
    "F7": ("f7", Key.f7 if HAS_PYNPUT and hasattr(Key, "f7") else None),
    "F8": ("f8", Key.f8 if HAS_PYNPUT and hasattr(Key, "f8") else None),
    "F9": ("f9", Key.f9 if HAS_PYNPUT and hasattr(Key, "f9") else None),
    "F10": ("f10", Key.f10 if HAS_PYNPUT and hasattr(Key, "f10") else None),
    "F11": ("f11", Key.f11 if HAS_PYNPUT and hasattr(Key, "f11") else None),
    "F12": ("f12", Key.f12 if HAS_PYNPUT and hasattr(Key, "f12") else None)
}

# Accurate case-sensitive X11 keysyms for xdotool
XDOTOOL_KEYSYMS = {
    "BACKSPACE": "BackSpace",
    "BKSP": "BackSpace",
    "BS": "BackSpace",
    "DELETE": "Delete",
    "DEL": "Delete",
    "ENTER": "Return",
    "RETURN": "Return",
    "ESC": "Escape",
    "ESCAPE": "Escape",
    "TAB": "Tab",
    "SPACE": "space",
    "UP": "Up",
    "DOWN": "Down",
    "LEFT": "Left",
    "RIGHT": "Right",
    "PAGE_UP": "Page_Up",
    "PAGEUP": "Page_Up",
    "PGUP": "Page_Up",
    "PAGE_DOWN": "Page_Down",
    "PAGEDOWN": "Page_Down",
    "PGDN": "Page_Down",
    "HOME": "Home",
    "END": "End",
    "SHIFT": "Shift_L",
    "CTRL": "Control_L",
    "CONTROL": "Control_L",
    "ALT": "Alt_L",
    "SUPER": "Super_L",
    "WIN": "Super_L",
    "F1": "F1",
    "F2": "F2",
    "F3": "F3",
    "F4": "F4",
    "F5": "F5",
    "F6": "F6",
    "F7": "F7",
    "F8": "F8",
    "F9": "F9",
    "F10": "F10",
    "F11": "F11",
    "F12": "F12"
}

class KeyboardManager:
    def type_text(self, text: str):
        if not text:
            return
        env = get_desktop_env()
        try:
            if HAS_PYNPUT and pynput_kbd:
                try:
                    pynput_kbd.type(text)
                    return
                except Exception:
                    pass
            if HAS_PYAUTOGUI:
                try:
                    pyautogui.write(text)
                    return
                except Exception:
                    pass
            subprocess.run(["xdotool", "type", "--delay", "2", "--", text], env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        except Exception as e:
            logger.error(f"Type text error: {e}")

    def press_key(self, key_name: str):
        if not key_name:
            return
        norm = key_name.upper().strip()
        env = get_desktop_env()

        # Check sticky modifier press/release
        is_down = norm.endswith("_DOWN")
        is_up = norm.endswith("_UP")
        if is_down or is_up:
            base_key = norm.rsplit("_", 1)[0]
            if base_key in SPECIAL_KEYS_MAP:
                py_key, pyn_key = SPECIAL_KEYS_MAP[base_key]
                if HAS_PYNPUT and pynput_kbd and pyn_key:
                    try:
                        if is_down:
                            pynput_kbd.press(pyn_key)
                        else:
                            pynput_kbd.release(pyn_key)
                        return
                    except Exception:
                        pass
                if HAS_PYAUTOGUI and py_key:
                    try:
                        if is_down:
                            pyautogui.keyDown(py_key)
                        else:
                            pyautogui.keyUp(py_key)
                        return
                    except Exception:
                        pass
                xdo_sym = XDOTOOL_KEYSYMS.get(base_key, base_key)
                action = "keydown" if is_down else "keyup"
                subprocess.run(["xdotool", action, xdo_sym], env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                return

        # 1. Check special keys mapping for pynput
        if norm in SPECIAL_KEYS_MAP:
            py_key, pyn_key = SPECIAL_KEYS_MAP[norm]
            if HAS_PYNPUT and pynput_kbd and pyn_key:
                try:
                    pynput_kbd.press(pyn_key)
                    pynput_kbd.release(pyn_key)
                    logger.debug(f"Dispatched via pynput: {norm}")
                    return
                except Exception as ex:
                    logger.debug(f"pynput key dispatch failed: {ex}")

            if HAS_PYAUTOGUI and py_key:
                try:
                    pyautogui.press(py_key)
                    logger.debug(f"Dispatched via pyautogui: {norm}")
                    return
                except Exception as ex:
                    logger.debug(f"pyautogui key dispatch failed: {ex}")

        # 2. X11 / xdotool execution (Standard on Linux Mint)
        xdo_sym = XDOTOOL_KEYSYMS.get(norm)
        if not xdo_sym:
            if len(key_name) == 1:
                xdo_sym = key_name
            else:
                xdo_sym = norm.capitalize()

        try:
            res = subprocess.run(["xdotool", "key", xdo_sym], env=env, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True)
            if res.returncode != 0:
                logger.warning(f"xdotool returned error for {xdo_sym}: {res.stderr.strip()}")
            else:
                logger.info(f"Successfully sent key to X11: {xdo_sym}")
        except Exception as e:
            logger.error(f"Press key error ({key_name} -> {xdo_sym}): {e}")

    def trigger_shortcut(self, combination: Union[str, List[str]]):
        env = get_desktop_env()
        try:
            if isinstance(combination, str):
                # e.g. "Ctrl+C" or "Alt+Tab"
                keys = [k.strip() for k in combination.replace("+", " ").split()]
            else:
                keys = list(combination)

            clean_keys = []
            xdotool_keys = []
            for k in keys:
                u = k.upper().strip()
                if u in ("CTRL", "CONTROL"):
                    clean_keys.append("ctrl")
                    xdotool_keys.append("Control_L")
                elif u in ("ALT", "OPTION"):
                    clean_keys.append("option" if platform.system() == "Darwin" else "alt")
                    xdotool_keys.append("Alt_L")
                elif u == "SHIFT":
                    clean_keys.append("shift")
                    xdotool_keys.append("Shift_L")
                elif u in ("SUPER", "WIN"):
                    clean_keys.append("command" if platform.system() == "Darwin" else "win")
                    xdotool_keys.append("Super_L")
                elif u in ("CMD", "COMMAND"):
                    clean_keys.append("command" if platform.system() == "Darwin" else "ctrl")
                    xdotool_keys.append("Super_L")
                elif u in XDOTOOL_KEYSYMS:
                    py_k = SPECIAL_KEYS_MAP.get(u, (k.lower(), None))[0]
                    clean_keys.append(py_k)
                    xdotool_keys.append(XDOTOOL_KEYSYMS[u])
                else:
                    clean_keys.append(k.lower())
                    xdotool_keys.append(k)

            if HAS_PYAUTOGUI:
                try:
                    pyautogui.hotkey(*clean_keys)
                    return
                except Exception:
                    pass

            xdo_str = "+".join(xdotool_keys)
            subprocess.run(["xdotool", "key", xdo_str], env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            logger.info(f"Triggered shortcut: {xdo_str}")
        except Exception as e:
            logger.error(f"Shortcut trigger error ({combination}): {e}")

