import logging
import os
import subprocess
import time

logger = logging.getLogger("LinuxRemoteMouse")

try:
    from pynput.mouse import Button, Controller as MouseController
    pynput_mouse = MouseController()
    HAS_PYNPUT = True
except Exception:
    HAS_PYNPUT = False
    pynput_mouse = None

try:
    import pyautogui
    pyautogui.FAILSAFE = False
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

class MouseManager:
    def __init__(self):
        self.is_dragging = False
        self.move_accum_x = 0.0
        self.move_accum_y = 0.0
        self.scroll_accum_x = 0.0
        self.scroll_accum_y = 0.0
        self.last_scroll_time = 0.0

    def move(self, dx: float, dy: float):
        try:
            # Bound deltas to prevent runaway jumps
            dx = max(-200.0, min(200.0, float(dx)))
            dy = max(-200.0, min(200.0, float(dy)))

            # Subpixel accumulation for silky smooth precision
            self.move_accum_x += dx
            self.move_accum_y += dy
            step_x = int(self.move_accum_x)
            step_y = int(self.move_accum_y)

            if step_x == 0 and step_y == 0:
                return

            self.move_accum_x -= step_x
            self.move_accum_y -= step_y

            if HAS_PYNPUT and pynput_mouse:
                pynput_mouse.move(step_x, step_y)
                return
            elif HAS_PYAUTOGUI:
                pyautogui.moveRel(step_x, step_y)
                return

            # xdotool fallback
            env = get_desktop_env()
            subprocess.run(
                ["xdotool", "mousemove_relative", "--", str(step_x), str(step_y)],
                env=env,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL
            )
        except Exception as e:
            logger.debug(f"Mouse move error: {e}")

    def click(self, button: str = "left", double: bool = False):
        try:
            btn_clean = button.lower().strip()
            btn_map = {
                "left": (Button.left if HAS_PYNPUT else "left", 1),
                "right": (Button.right if HAS_PYNPUT else "right", 3),
                "middle": (Button.middle if HAS_PYNPUT else "middle", 2)
            }
            mapped_btn, xdo_btn = btn_map.get(btn_clean, (Button.left if HAS_PYNPUT else "left", 1))

            success = False
            if HAS_PYNPUT and pynput_mouse:
                try:
                    pynput_mouse.click(mapped_btn, 2 if double else 1)
                    success = True
                except Exception as pynput_err:
                    logger.debug(f"pynput click error: {pynput_err}")

            if not success and HAS_PYAUTOGUI:
                try:
                    if double:
                        pyautogui.doubleClick(button=btn_clean)
                    else:
                        pyautogui.click(button=btn_clean)
                    success = True
                except Exception as pyag_err:
                    logger.debug(f"pyautogui click error: {pyag_err}")

            if not success:
                env = get_desktop_env()
                cmd = ["xdotool", "click", "--repeat", "2" if double else "1", str(xdo_btn)]
                subprocess.run(cmd, env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        except Exception as e:
            logger.error(f"Mouse click error: {e}")

    def mouse_down(self, button: str = "left"):
        try:
            self.is_dragging = True
            btn_map = {
                "left": Button.left if HAS_PYNPUT else "left",
                "right": Button.right if HAS_PYNPUT else "right",
                "middle": Button.middle if HAS_PYNPUT else "middle"
            }
            mapped_btn = btn_map.get(button.lower(), Button.left if HAS_PYNPUT else "left")
            if HAS_PYNPUT and pynput_mouse:
                pynput_mouse.press(mapped_btn)
            elif HAS_PYAUTOGUI:
                pyautogui.mouseDown(button=button.lower())
            else:
                xbtn = 2 if button.lower() == "middle" else (3 if button.lower() == "right" else 1)
                env = get_desktop_env()
                subprocess.run(["xdotool", "mousedown", str(xbtn)], env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        except Exception as e:
            logger.error(f"Mouse down error: {e}")

    def mouse_up(self, button: str = "left"):
        try:
            self.is_dragging = False
            btn_map = {
                "left": Button.left if HAS_PYNPUT else "left",
                "right": Button.right if HAS_PYNPUT else "right",
                "middle": Button.middle if HAS_PYNPUT else "middle"
            }
            mapped_btn = btn_map.get(button.lower(), Button.left if HAS_PYNPUT else "left")
            if HAS_PYNPUT and pynput_mouse:
                pynput_mouse.release(mapped_btn)
            elif HAS_PYAUTOGUI:
                pyautogui.mouseUp(button=button.lower())
            else:
                xbtn = 2 if button.lower() == "middle" else (3 if button.lower() == "right" else 1)
                env = get_desktop_env()
                subprocess.run(["xdotool", "mouseup", str(xbtn)], env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        except Exception as e:
            logger.error(f"Mouse up error: {e}")

    def scroll(self, dx: float, dy: float):
        try:
            now = time.time()
            if now - self.last_scroll_time > 0.3:
                self.scroll_accum_x = 0.0
                self.scroll_accum_y = 0.0
            self.last_scroll_time = now

            dx = max(-3.0, min(3.0, float(dx)))
            dy = max(-3.0, min(3.0, float(dy)))

            if HAS_PYNPUT and pynput_mouse:
                ticks_y = int(round(dy))
                ticks_x = int(round(dx))
                if ticks_y != 0 or ticks_x != 0:
                    pynput_mouse.scroll(ticks_x, ticks_y)
                return
            elif HAS_PYAUTOGUI:
                pyautogui.scroll(int(round(dy)))
                return

            self.scroll_accum_y += dy
            self.scroll_accum_x += dx

            env = get_desktop_env()

            ticks_y = int(self.scroll_accum_y)
            if ticks_y != 0:
                self.scroll_accum_y -= ticks_y
                btn = "4" if ticks_y > 0 else "5"
                subprocess.run(["xdotool", "click", btn], env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

            ticks_x = int(self.scroll_accum_x)
            if ticks_x != 0:
                self.scroll_accum_x -= ticks_x
                btn = "7" if ticks_x > 0 else "6"
                subprocess.run(["xdotool", "click", btn], env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        except Exception as e:
            logger.debug(f"Scroll error: {e}")
