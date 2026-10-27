#!/usr/bin/env python3
import asyncio
import json
import logging
import signal
import sys
import subprocess
import warnings
warnings.filterwarnings("ignore", category=DeprecationWarning, module="websockets")

import os
import sys
import subprocess
from pathlib import Path

# Self-Bootstrapping / Dependency Auto-Resolution
# If launched with a Python interpreter that does not have websockets,
# automatically search for nearby virtual environments (.venv/venv),
# or auto-create an isolated .venv and install requirements!
def _ensure_dependencies():
    try:
        import websockets  # noqa: F401
        return
    except (ImportError, ModuleNotFoundError):
        pass

    base_dir = Path(__file__).resolve().parent
    req_file = base_dir / "requirements.txt"

    # 1. Search for existing virtual environments with websockets
    candidate_venvs = [
        base_dir / ".venv",
        base_dir / "venv",
        Path.home() / ".local" / "share" / "omnidesk" / "venv",
        Path.home() / ".local" / "share" / "linux-remote" / "venv",
    ]
    for venv_path in candidate_venvs:
        if sys.platform == "win32":
            py_bin = venv_path / "Scripts" / "python.exe"
        else:
            py_bin = venv_path / "bin" / "python3"
            if not py_bin.exists():
                py_bin = venv_path / "bin" / "python"
        if py_bin.exists() and py_bin.resolve() != Path(sys.executable).resolve():
            try:
                subprocess.run(
                    [str(py_bin), "-c", "import websockets"],
                    check=True,
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL
                )
                # Found working venv! Re-exec with this interpreter
                os.execv(str(py_bin), [str(py_bin)] + sys.argv)
            except Exception:
                pass

    # 2. Attempt automatic venv creation and dependency installation
    print("\n\033[1;36m=====================================================\033[0m")
    print("\033[1;33m⚡ OmniDesk Server: Missing required packages ('websockets')\033[0m")
    print("\033[0;32m📦 Automatically creating isolated environment & installing...\033[0m")
    print("\033[1;36m=====================================================\033[0m\n")

    target_venv = base_dir / ".venv"
    try:
        if not target_venv.exists():
            subprocess.run([sys.executable, "-m", "venv", str(target_venv)], check=True)

        if sys.platform == "win32":
            venv_py = target_venv / "Scripts" / "python.exe"
            venv_pip = target_venv / "Scripts" / "pip.exe"
        else:
            venv_py = target_venv / "bin" / "python3"
            venv_pip = target_venv / "bin" / "pip"

        if venv_py.exists():
            print("Installing packages from PyPI (websockets, psutil, zeroconf, pyautogui, pynput)...")
            pip_cmd = (
                [str(venv_pip), "install", "-r", str(req_file)]
                if req_file.exists()
                else [str(venv_pip), "install", "websockets>=12.0", "psutil>=5.9.0", "zeroconf>=0.131.0", "pyautogui>=0.9.54", "pynput>=1.7.6"]
            )
            subprocess.run(pip_cmd, check=True)
            print("\n\033[1;32m✔ Installation successful! Starting OmniDesk Server...\033[0m\n")
            os.execv(str(venv_py), [str(venv_py)] + sys.argv)
    except Exception as err:
        # Fallback: Try installing directly with current python -m pip
        try:
            pip_cmd = [sys.executable, "-m", "pip", "install", "-r", str(req_file)]
            res = subprocess.run(pip_cmd + ["--break-system-packages"], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            if res.returncode == 0:
                print("\n\033[1;32m✔ Installed via pip! Starting OmniDesk Server...\033[0m\n")
                os.execv(sys.executable, [sys.executable] + sys.argv)
        except Exception:
            pass

        print(f"\n\033[1;31m❌ Automatic dependency setup could not complete: {err}\033[0m")
        print("\033[1;37mPlease run the guided installer for your operating system:\033[0m")
        if sys.platform == "darwin":
            print(f"  \033[1;32mcd {base_dir} && ./install-mac.sh\033[0m")
            print("  \033[0;36mOr manually in terminal:\033[0m")
            print("  python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt")
        elif sys.platform == "win32":
            print(f"  \033[1;32mcd {base_dir} && install.bat\033[0m")
        else:
            print(f"  \033[1;32mcd {base_dir} && ./install.sh\033[0m")
        print("")
        sys.exit(1)

_ensure_dependencies()

import websockets
try:
    from websockets.legacy.server import WebSocketServerProtocol
except ImportError:
    try:
        from websockets.server import WebSocketServerProtocol
    except ImportError:
        WebSocketServerProtocol = object

# Filter out unhandled HTTP probe noise from websockets loggers
class HandshakeWarningFilter(logging.Filter):
    def filter(self, record):
        msg = record.getMessage()
        if any(term in msg for term in ("invalid Connection header", "426 Upgrade Required", "opening handshake failed")):
            return False
        if record.exc_info and len(record.exc_info) > 1 and record.exc_info[1] is not None:
            exc_str = str(record.exc_info[1])
            exc_type = type(record.exc_info[1]).__name__
            if "invalid Connection header" in exc_str or "InvalidUpgrade" in exc_type:
                return False
        return True

_filter = HandshakeWarningFilter()
logging.getLogger("websockets.server").addFilter(_filter)
logging.getLogger("websockets").addFilter(_filter)

from config import WS_HOST, WS_PORT, TELEMETRY_INTERVAL, APP_NAME, VERSION, PID_FILE
from protocol import RemoteMessage, ProtocolError
from authentication import AuthManager
from mouse import MouseManager
from keyboard import KeyboardManager
from media import MediaManager
from system_control import SystemControlManager
from status import StatusManager
from discovery import DiscoveryServer

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)]
)
for handler in logging.getLogger().handlers:
    handler.addFilter(_filter)

logger = logging.getLogger("OmniDeskServer")

class OmniDeskServer:
    def __init__(self):
        self.auth_manager = AuthManager()
        self.mouse_manager = MouseManager()
        self.keyboard_manager = KeyboardManager()
        self.media_manager = MediaManager()
        self.system_manager = SystemControlManager()
        self.status_manager = StatusManager()
        self.discovery_server = DiscoveryServer(self.status_manager)

        # Active authenticated sockets: ws -> device_id
        self.authenticated_clients: Dict[WebSocketServerProtocol, str] = {}
        self.all_clients: Set[WebSocketServerProtocol] = set()
        self.running = True
        self._stop_event: Optional[asyncio.Event] = None
        self.telemetry_task: Optional[asyncio.Task] = None

    @property
    def stop_event(self) -> asyncio.Event:
        if self._stop_event is None:
            self._stop_event = asyncio.Event()
        return self._stop_event

    def notify_desktop(self, title: str, body: str):
        try:
            import platform
            sys_name = platform.system()
            if sys_name == "Darwin":
                # Escape quotes
                clean_body = body.replace('"', '\\"')
                clean_title = title.replace('"', '\\"')
                subprocess.run(["osascript", "-e", f'display notification "{clean_body}" with title "{clean_title}"'],
                               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            elif sys_name == "Windows":
                pass
            else:
                subprocess.run(["notify-send", "-a", "OmniDesk", title, body],
                               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        except Exception:
            pass

    async def handle_connection(self, websocket: WebSocketServerProtocol):
        client_ip = websocket.remote_address[0]
        logger.info(f"Incoming connection from {client_ip}")
        self.all_clients.add(websocket)

        try:
            async for raw_message in websocket:
                try:
                    msg = RemoteMessage.parse(raw_message)
                    msg_type = msg.get("type", "")
                    req_id = msg.get("request_id")

                    # Handle unauthenticated actions: discover, ping, pair_request, pair_verify, auth
                    if msg_type in ("discover", "ping"):
                        payload = self.discovery_server.get_discovery_payload()
                        reply = RemoteMessage.success(msg_type, req_id, **payload)
                        await websocket.send(reply)
                        continue

                    if msg_type == "pair_request":
                        device_name = msg.get("device_name", "Android Phone")
                        force_new = bool(msg.get("force_new", False))
                        pin = self.auth_manager.generate_pairing_pin(force_new=force_new)
                        self.notify_desktop("Pairing Request", f"Device '{device_name}' wants to pair.\nCode: {pin}")
                        reply = RemoteMessage.success("pair_request", req_id, message=f"Pairing code active on PC", pin_required=True)
                        await websocket.send(reply)
                        continue

                    elif msg_type == "pair_verify":
                        pin = msg.get("pin", "")
                        dev_id = msg.get("device_id", "unknown")
                        dev_name = msg.get("device_name", "Android Phone")
                        token = self.auth_manager.verify_pairing_pin(pin, dev_id, dev_name)
                        if token:
                            self.authenticated_clients[websocket] = dev_id
                            self.notify_desktop("Device Paired", f"{dev_name} is now connected.")
                            reply = RemoteMessage.success("pair_verify", req_id, token=token, device_name=dev_name)
                        else:
                            reply = RemoteMessage.error("pair_verify", "Invalid or expired pairing code", req_id, code=401)
                        await websocket.send(reply)
                        continue

                    elif msg_type == "auth":
                        token = msg.get("token", "")
                        dev_id = msg.get("device_id", "unknown")
                        dev_name = msg.get("device_name", "Android Phone")
                        if self.auth_manager.is_token_valid(token):
                            self.authenticated_clients[websocket] = dev_id
                            logger.info(f"Client authenticated: {dev_name} ({client_ip})")
                            pc_stat = self.status_manager.get_system_status()
                            reply = RemoteMessage.success("auth", req_id, message="Authenticated", pc_info=pc_stat)
                        else:
                            reply = RemoteMessage.error("auth", "Invalid or revoked token", req_id, code=401)
                        await websocket.send(reply)
                        continue

                    # All other commands require authentication
                    if websocket not in self.authenticated_clients:
                        reply = RemoteMessage.error(msg_type, "Authentication required", req_id, code=403)
                        await websocket.send(reply)
                        continue

                    # Execute command
                    await self.process_authenticated_command(websocket, msg_type, msg, req_id)

                except ProtocolError as pe:
                    logger.warning(f"Protocol error from {client_ip}: {pe}")
                    await websocket.send(RemoteMessage.error("error", str(pe)))
                except Exception as ex:
                    logger.error(f"Error handling message: {ex}", exc_info=True)
                    await websocket.send(RemoteMessage.error("error", f"Internal error: {ex}"))

        except websockets.exceptions.ConnectionClosed:
            logger.info(f"Connection closed for {client_ip}")
        finally:
            self.authenticated_clients.pop(websocket, None)
            self.all_clients.discard(websocket)

    async def process_authenticated_command(self, ws: WebSocketServerProtocol, cmd: str, data: dict, req_id: str):
        # 1. Mouse commands
        if cmd == "mouse_move":
            dx = float(data.get("dx", 0))
            dy = float(data.get("dy", 0))
            self.mouse_manager.move(dx, dy)

        elif cmd == "mouse_click":
            btn = data.get("button", "left")
            dbl = bool(data.get("double", False))
            self.mouse_manager.click(btn, dbl)
            if req_id:
                await ws.send(RemoteMessage.success(cmd, req_id))

        elif cmd == "mouse_down":
            btn = data.get("button", "left")
            self.mouse_manager.mouse_down(btn)

        elif cmd == "mouse_up":
            btn = data.get("button", "left")
            self.mouse_manager.mouse_up(btn)

        elif cmd == "mouse_scroll":
            dx = float(data.get("dx", 0))
            dy = float(data.get("dy", 0))
            self.mouse_manager.scroll(dx, dy)

        # 2. Keyboard commands
        elif cmd == "text_type":
            txt = data.get("text", "")
            self.keyboard_manager.type_text(txt)
            if req_id:
                await ws.send(RemoteMessage.success(cmd, req_id))

        elif cmd == "key_press":
            k = data.get("key", "")
            self.keyboard_manager.press_key(k)
            if req_id:
                await ws.send(RemoteMessage.success(cmd, req_id))

        elif cmd == "shortcut":
            sc = data.get("shortcut") or data.get("keys", "")
            self.keyboard_manager.trigger_shortcut(sc)
            if req_id:
                await ws.send(RemoteMessage.success(cmd, req_id))

        # 3. Media & Volume commands
        elif cmd == "volume_set":
            val = int(data.get("value", 50))
            self.media_manager.set_volume(val)
            stat = self.media_manager.get_audio_status()
            await ws.send(RemoteMessage.success(cmd, req_id, **stat))

        elif cmd == "volume_delta":
            delta = int(data.get("delta", 5))
            self.media_manager.change_volume(delta)
            stat = self.media_manager.get_audio_status()
            await ws.send(RemoteMessage.success(cmd, req_id, **stat))

        elif cmd == "media_toggle":
            self.media_manager.play_pause()
            if req_id:
                await ws.send(RemoteMessage.success(cmd, req_id))

        elif cmd == "media_play":
            self.media_manager.play()
            if req_id:
                await ws.send(RemoteMessage.success(cmd, req_id))

        elif cmd == "media_pause":
            self.media_manager.pause()
            if req_id:
                await ws.send(RemoteMessage.success(cmd, req_id))

        elif cmd == "media_next":
            self.media_manager.next_track()
            if req_id:
                await ws.send(RemoteMessage.success(cmd, req_id))

        elif cmd == "media_previous":
            self.media_manager.previous_track()
            if req_id:
                await ws.send(RemoteMessage.success(cmd, req_id))

        elif cmd == "media_mute":
            self.media_manager.toggle_mute()
            stat = self.media_manager.get_audio_status()
            await ws.send(RemoteMessage.success(cmd, req_id, **stat))

        # 4. PC system actions
        elif cmd == "system_action":
            act = data.get("action", "")
            ok = self.system_manager.execute_action(act)
            if ok:
                await ws.send(RemoteMessage.success(cmd, req_id, action=act))
            else:
                await ws.send(RemoteMessage.error(cmd, f"Failed to execute action: {act}", req_id))

        # 4.1 Clipboard sync
        elif cmd == "clipboard_set":
            txt = data.get("text", "")
            ok = self.system_manager.set_clipboard(txt)
            await ws.send(RemoteMessage.success(cmd, req_id, set=ok))

        elif cmd == "clipboard_get":
            txt = self.system_manager.get_clipboard()
            await ws.send(RemoteMessage.success(cmd, req_id, text=txt))

        # 4.2 Power Timer (Sleep / Shutdown Timer)
        elif cmd == "schedule_power":
            mins = int(data.get("minutes", 30))
            ok = self.system_manager.schedule_shutdown(mins)
            await ws.send(RemoteMessage.success(cmd, req_id, scheduled=ok, minutes=mins))

        elif cmd == "cancel_power":
            ok = self.system_manager.cancel_scheduled_shutdown()
            await ws.send(RemoteMessage.success(cmd, req_id, canceled=ok))

        # 5. Application Launcher
        elif cmd == "get_applications":
            apps = self.system_manager.list_applications()
            await ws.send(RemoteMessage.success(cmd, req_id, applications=apps))

        elif cmd == "launch_application":
            app_id = data.get("app_id") or data.get("name", "")
            ok = self.system_manager.launch_app(app_id)
            if ok:
                await ws.send(RemoteMessage.success(cmd, req_id, app_id=app_id))
            else:
                await ws.send(RemoteMessage.error(cmd, f"Could not launch {app_id}", req_id))

        # 6. Status & Telemetry
        elif cmd == "get_status":
            stat = self.status_manager.get_system_status()
            audio = self.media_manager.get_audio_status()
            await ws.send(RemoteMessage.success(cmd, req_id, status=stat, audio=audio))

        # 7. Gamepad input
        elif cmd == "gamepad_input":
            btn = data.get("button")
            axis = data.get("axis")
            val = data.get("value")
            # Map standard gamepad buttons to virtual keys/actions
            if btn:
                gamepad_key_map = {
                    "A": "ENTER",
                    "B": "ESC",
                    "X": "SPACE",
                    "Y": "TAB",
                    "UP": "UP",
                    "DOWN": "DOWN",
                    "LEFT": "LEFT",
                    "RIGHT": "RIGHT",
                    "START": "ENTER",
                    "SELECT": "BACKSPACE"
                }
                k = gamepad_key_map.get(str(btn).upper())
                if k:
                    self.keyboard_manager.press_key(k)
            elif axis:
                # Analog stick mapped to relative mouse movement
                ax_x = float(data.get("dx", 0))
                ax_y = float(data.get("dy", 0))
                self.mouse_manager.move(ax_x * 8, ax_y * 8)

        # 8. Unpair / Revoke
        elif cmd == "unpair":
            tok = data.get("token")
            self.auth_manager.revoke_device(tok)
            await ws.send(RemoteMessage.success(cmd, req_id, message="Device revoked"))

        else:
            await ws.send(RemoteMessage.error(cmd, f"Unknown command: {cmd}", req_id))

    async def telemetry_broadcast_loop(self):
        while self.running:
            try:
                if self.authenticated_clients:
                    status = self.status_manager.get_system_status()
                    audio = self.media_manager.get_audio_status()
                    msg = RemoteMessage.notification("telemetry", status=status, audio=audio)
                    # Broadcast to active authenticated clients
                    targets = list(self.authenticated_clients.keys())
                    for ws in targets:
                        try:
                            await ws.send(msg)
                        except Exception:
                            pass
            except Exception as e:
                logger.debug(f"Telemetry broadcast error: {e}")
            await asyncio.sleep(TELEMETRY_INTERVAL)

    def process_http_request(self, *args, **kwargs):
        try:
            path = ""
            conn = None
            req = None
            req_headers = None

            if len(args) == 1:
                arg0 = args[0]
                if isinstance(arg0, str):
                    path = arg0
                elif hasattr(arg0, "path"):
                    req = arg0
                    path = getattr(arg0, "path", "")
                    req_headers = getattr(arg0, "headers", None)
            elif len(args) >= 2:
                conn = args[0]
                req = args[1]
                if isinstance(req, str):
                    path = req
                else:
                    path = getattr(req, "path", "")
                    req_headers = getattr(req, "headers", None)
                if req_headers is None and hasattr(args[1], "get"):
                    req_headers = args[1]

            # CRITICAL: If client sends Upgrade: websocket, do NOT intercept!
            # Return None to allow websockets to complete the standard 101 Switching Protocols handshake.
            if req_headers:
                upgrade = ""
                if hasattr(req_headers, "get"):
                    upgrade = req_headers.get("Upgrade", "") or req_headers.get("upgrade", "")
                if "websocket" in str(upgrade).lower():
                    return None

            clean_path = path.split("?")[0].rstrip("/") if path else ""
            if not clean_path:
                # Root path "/" without dedicated REST endpoint should proceed with websocket handshake
                return None

            # Only intercept specific REST discovery and health-check endpoints
            if clean_path in ("/discover", "/status", "/api/status", "/ping"):
                payload = self.discovery_server.get_discovery_payload()
                json_str = json.dumps(payload)
                body_bytes = json_str.encode("utf-8")
                import http

                # 1. Modern websockets (>= 13.0) ServerConnection.respond
                if conn and hasattr(conn, "respond"):
                    try:
                        return conn.respond(http.HTTPStatus.OK, json_str)
                    except Exception as e:
                        logger.debug(f"conn.respond failed: {e}")

                # 2. Modern websockets http11 Response object directly
                try:
                    from websockets.http11 import Response
                    from websockets.datastructures import Headers
                    headers = Headers([
                        ("content-type", "application/json"),
                        ("content-length", str(len(body_bytes))),
                        ("access-control-allow-origin", "*"),
                        ("connection", "close"),
                    ])
                    return Response(200, "OK", headers, body_bytes)
                except Exception as e:
                    logger.debug(f"http11 Response creation failed: {e}")

                # 3. Legacy websockets tuple format: (status, headers, body_bytes)
                headers = [
                    ("Content-Type", "application/json"),
                    ("Content-Length", str(len(body_bytes))),
                    ("Access-Control-Allow-Origin", "*"),
                    ("Connection", "close"),
                ]
                return (http.HTTPStatus.OK, headers, body_bytes)
        except Exception as e:
            logger.debug(f"HTTP handler error: {e}")
        return None

    async def start(self):
        # Generate initial PIN on launch so user can see it right away
        init_pin = self.auth_manager.generate_pairing_pin()

        # Start UDP discovery responder
        try:
            await self.discovery_server.start()
        except Exception as e:
            logger.warning(f"Could not start discovery server: {e}")

        # Start WebSocket server (with robust version fallback & port conflict recovery)
        server = None
        for attempt in range(2):
            try:
                try:
                    server = await websockets.serve(
                        self.handle_connection,
                        WS_HOST,
                        WS_PORT,
                        process_request=self.process_http_request,
                        ping_interval=10,
                        ping_timeout=15,
                    )
                except (TypeError, Exception) as ex:
                    logger.debug(f"websockets.serve fallback ({ex})")
                    server = await websockets.serve(
                        self.handle_connection,
                        WS_HOST,
                        WS_PORT,
                        process_request=self.process_http_request,
                    )
                break
            except OSError as e:
                if ("Address already in use" in str(e) or getattr(e, "errno", None) == 98) and attempt == 0:
                    logger.warning(f"Port {WS_PORT} busy. Attempting to release and retry...")
                    try:
                        subprocess.run(["fuser", "-k", f"{WS_PORT}/tcp"],
                                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                    except Exception:
                        pass
                    await asyncio.sleep(1.5)
                else:
                    logger.error(f"Cannot bind WebSocket port {WS_PORT}: {e}")
                    raise

        import platform
        sys_type = platform.system()
        if sys_type == "Darwin":
            os_banner = "🍎  OmniDesk Server (macOS)  🍎"
            os_label = f"macOS {platform.mac_ver()[0]}"
        elif sys_type == "Windows":
            os_banner = "🪟  OmniDesk Server (Windows)  🪟"
            os_label = f"Windows {platform.release()}"
        else:
            os_banner = "⚡  OmniDesk Server (Linux)  ⚡"
            os_label = self.status_manager.os_info.get("distro", "Linux")

        print("\n" + "─" * 48)
        print(f"  {os_banner}")
        print("─" * 48)
        print(f"  • System:     {os_label} ({self.status_manager.hostname})")
        print(f"  • Status:     Running & Ready ✨")
        print(f"  • Address:    ws://{WS_HOST}:{WS_PORT}")
        print(f"  • Pair PIN:   [ {init_pin} ]")
        print("─" * 48 + "\n")
        logger.info(f"=== {APP_NAME} v{VERSION} ({sys_type}) ===")
        logger.info(f"WebSocket server running on ws://{WS_HOST}:{WS_PORT}")
        logger.info(f"Current initial pairing PIN: {init_pin}")
        logger.info("Ready for Android phone connections!")

        # Record PID for process managers and status scripts
        try:
            import os
            PID_FILE.write_text(str(os.getpid()))
        except Exception:
            pass

        # Launch background telemetry loop
        self.telemetry_task = asyncio.create_task(self.telemetry_broadcast_loop())

        # Wait until stop event is triggered
        await self.stop_event.wait()
        logger.info("Server shutting down cleanly...")
        await self.stop()

    async def stop(self):
        self.running = False
        try:
            if PID_FILE.exists():
                PID_FILE.unlink(missing_ok=True)
        except Exception:
            pass

        if self._stop_event and not self._stop_event.is_set():
            self._stop_event.set()

        if self.telemetry_task and not self.telemetry_task.done():
            self.telemetry_task.cancel()
            try:
                await self.telemetry_task
            except (asyncio.CancelledError, Exception):
                pass

        try:
            await self.discovery_server.stop_async()
        except Exception as e:
            logger.debug(f"Error stopping discovery server: {e}")
            self.discovery_server.stop()

        # Gracefully close all connected WebSocket clients
        close_futs = []
        for ws in list(self.all_clients):
            try:
                close_futs.append(ws.close(code=1001, reason="Server shutting down"))
            except Exception:
                pass
        if close_futs:
            await asyncio.gather(*close_futs, return_exceptions=True)

# Backward compatibility alias
LinuxRemoteServer = OmniDeskServer

def main():
    import traceback
    # Initialize the event loop FIRST so all asyncio primitives attach to this loop
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    server = OmniDeskServer()

    def shutdown():
        logger.info("Signal received, stopping OmniDesk Server...")
        server.running = False
        if not server.stop_event.is_set():
            loop.call_soon_threadsafe(server.stop_event.set)

    for sig in (signal.SIGINT, signal.SIGTERM):
        try:
            loop.add_signal_handler(sig, shutdown)
        except (NotImplementedError, AttributeError):
            try:
                signal.signal(sig, lambda s, f: shutdown())
            except Exception:
                pass

    try:
        loop.run_until_complete(server.start())
    except (KeyboardInterrupt, SystemExit):
        pass
    except Exception as e:
        logger.critical(f"FATAL SERVER CRASH: {e}")
        traceback.print_exc()
        sys.exit(1)
    finally:
        try:
            loop.run_until_complete(server.stop())
        except Exception:
            pass
        try:
            # Cleanly cancel and wait for any remaining pending tasks on this loop
            pending = [t for t in asyncio.all_tasks(loop) if not t.done()]
            if pending:
                for task in pending:
                    task.cancel()
                loop.run_until_complete(asyncio.gather(*pending, return_exceptions=True))
        except Exception:
            pass
        try:
            loop.run_until_complete(loop.shutdown_asyncgens())
        except Exception:
            pass
        loop.close()
        logger.info("Server stopped cleanly.")

if __name__ == "__main__":
    main()
