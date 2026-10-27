import asyncio
import json
import logging
import os
import re
import shutil
import socket
import subprocess
import threading
from typing import Dict, Optional
from config import DISCOVERY_PORT, WS_PORT, MDNS_SERVICE_TYPE

logger = logging.getLogger("OmniDeskDiscovery")

try:
    from zeroconf import IPVersion, ServiceInfo, Zeroconf
    try:
        from zeroconf.asyncio import AsyncZeroconf, AsyncServiceInfo
        HAS_ASYNC_ZEROCONF = True
    except Exception:
        HAS_ASYNC_ZEROCONF = False
        AsyncZeroconf = None
        AsyncServiceInfo = None
    HAS_ZEROCONF = True
except Exception as e:
    HAS_ZEROCONF = False
    HAS_ASYNC_ZEROCONF = False
    AsyncZeroconf = None
    AsyncServiceInfo = None
    logger.info(f"zeroconf Python module not available ({e}); fallback to avahi-publish/UDP")


class DiscoveryServer:
    def __init__(self, status_manager):
        self.status_manager = status_manager
        self.running = False
        self.udp_sock: Optional[socket.socket] = None
        self.aio_zeroconf: Optional[object] = None
        self.sync_zeroconf: Optional[object] = None
        self.zeroconf_info: Optional[object] = None
        self.avahi_proc: Optional[subprocess.Popen] = None
        self._listen_task: Optional[asyncio.Task] = None
        self._broadcast_task: Optional[asyncio.Task] = None

    def get_discovery_payload(self) -> Dict:
        status = self.status_manager.get_system_status()
        return {
            "service": "omnidesk",
            "hostname": status.get("hostname", "OmniDesk-PC"),
            "os": status.get("distro", "Universal"),
            "version": status.get("version", "1.0"),
            "ip": status.get("ip", "127.0.0.1"),
            "mac": status.get("mac", ""),
            "port": WS_PORT,
            "is_laptop": status.get("is_laptop", False),
            "battery_percent": status.get("battery_percent"),
            "os_type": status.get("os_type", "unknown")
        }

    async def start(self):
        self.running = True
        payload = self.get_discovery_payload()

        # 1. Register mDNS / DNS-SD via AsyncZeroconf (non-blocking asyncio)
        await self._start_mdns_zeroconf(payload)

        # 2. Fallback to Avahi CLI on Linux if Zeroconf is not active
        if not self.aio_zeroconf and not self.sync_zeroconf:
            self._start_mdns_avahi(payload)

        # 3. Create UDP discovery responder socket (port 8766)
        try:
            self.udp_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self.udp_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            if hasattr(socket, "SO_REUSEPORT"):
                try:
                    self.udp_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEPORT, 1)
                except Exception:
                    pass
            self.udp_sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
            self.udp_sock.bind(("", DISCOVERY_PORT))
            self.udp_sock.setblocking(False)
            logger.info(f"UDP Discovery responder listening on port {DISCOVERY_PORT}")

            loop = asyncio.get_running_loop()
            self._listen_task = loop.create_task(self._listen_loop())
            self._broadcast_task = loop.create_task(self._periodic_broadcast_loop())
        except Exception as e:
            logger.error(f"Failed to bind UDP discovery port {DISCOVERY_PORT}: {e}")

    async def _start_mdns_zeroconf(self, payload: Dict):
        if not HAS_ZEROCONF:
            return

        try:
            hostname = payload.get("hostname", "OmniDesk-PC")
            ip = payload.get("ip", "127.0.0.1")
            if not ip or ip == "127.0.0.1":
                try:
                    ip = self.status_manager._get_local_ip()
                except Exception:
                    ip = "127.0.0.1"

            # Parse IP into bytes safely
            try:
                ip_bytes = socket.inet_aton(ip)
            except Exception:
                ip = "127.0.0.1"
                ip_bytes = socket.inet_aton("127.0.0.1")

            # Clean host name according to RFC 1035 / RFC 2181 (letters, digits, hyphens only, no spaces)
            clean_host = re.sub(r'[^a-zA-Z0-9\-]', '-', hostname).strip('-')
            clean_host = re.sub(r'-+', '-', clean_host) or "omnidesk-pc"
            server_name = f"{clean_host}.local."

            # Clean service instance name for display (max 63 chars, no control chars)
            clean_instance = re.sub(r'[\x00-\x1f\x7f]', '', hostname).strip()[:50] or "OmniDesk"
            service_type = MDNS_SERVICE_TYPE if MDNS_SERVICE_TYPE.endswith(".") else f"{MDNS_SERVICE_TYPE}."
            service_name = f"{clean_instance}.{service_type}"

            properties = {
                "hostname": str(hostname),
                "os": str(payload.get("os", "Universal")),
                "version": str(payload.get("version", "1.0")),
                "ip": str(ip),
                "mac": str(payload.get("mac", "")),
                "service": "omnidesk",
                "port": str(WS_PORT),
                "os_type": str(payload.get("os_type", "unknown"))
            }

            # Select AsyncServiceInfo if available, otherwise standard ServiceInfo
            InfoClass = AsyncServiceInfo if (HAS_ASYNC_ZEROCONF and AsyncServiceInfo is not None) else ServiceInfo
            self.zeroconf_info = InfoClass(
                type_=service_type,
                name=service_name,
                addresses=[ip_bytes],
                port=WS_PORT,
                properties=properties,
                server=server_name
            )

            # Modern AsyncZeroconf with asyncio (avoids blocking I/O and unregister_all_services warnings)
            if HAS_ASYNC_ZEROCONF and AsyncZeroconf is not None:
                self.aio_zeroconf = AsyncZeroconf(ip_version=IPVersion.V4Only)
                # allow_name_change=True avoids NonUniqueNameException on port/reconnect collision
                await self.aio_zeroconf.async_register_service(self.zeroconf_info, allow_name_change=True)
                logger.info(f"AsyncZeroconf mDNS registered: {self.zeroconf_info.name} on {ip}:{WS_PORT}")
            elif HAS_ZEROCONF:
                loop = asyncio.get_running_loop()
                def _sync_init():
                    zc = Zeroconf(ip_version=IPVersion.V4Only)
                    zc.register_service(self.zeroconf_info, allow_name_change=True)
                    return zc
                self.sync_zeroconf = await loop.run_in_executor(None, _sync_init)
                logger.info(f"Zeroconf mDNS registered (sync fallback): {self.zeroconf_info.name} on {ip}:{WS_PORT}")

        except Exception as e:
            err_type = type(e).__name__
            err_msg = str(e) or "Name collision or network interface error"
            logger.warning(f"Could not register Zeroconf mDNS service: {err_type} ({err_msg})")
            await self._cleanup_zeroconf()

    async def _cleanup_zeroconf(self):
        """Asynchronously and cleanly closes Zeroconf without blocking the event loop."""
        if self.aio_zeroconf:
            try:
                if self.zeroconf_info:
                    try:
                        await self.aio_zeroconf.async_unregister_service(self.zeroconf_info)
                    except Exception as ex:
                        logger.debug(f"AsyncZeroconf unregister warning: {ex}")
                await self.aio_zeroconf.async_close()
                logger.info("AsyncZeroconf mDNS service closed cleanly")
            except Exception as e:
                logger.debug(f"Error during AsyncZeroconf close: {e}")
            finally:
                self.aio_zeroconf = None
                self.zeroconf_info = None

        if self.sync_zeroconf:
            try:
                loop = asyncio.get_running_loop()
                await loop.run_in_executor(None, self.sync_zeroconf.close)
            except Exception:
                pass
            finally:
                self.sync_zeroconf = None
                self.zeroconf_info = None

    def _start_mdns_avahi(self, payload: Dict):
        publish_cmd = shutil.which("avahi-publish-service") or shutil.which("avahi-publish")
        if not publish_cmd:
            return
        try:
            hostname = payload["hostname"]
            cmd = [
                publish_cmd,
                "-s",
                hostname,
                "_linuxremote._tcp",
                str(WS_PORT),
                f"hostname={hostname}",
                f"os={payload['os']}",
                f"version={payload['version']}",
                f"ip={payload['ip']}",
                f"mac={payload.get('mac', '')}",
                "service=linux_remote",
                f"port={WS_PORT}",
                f"os_type={payload.get('os_type', 'linux')}"
            ]
            self.avahi_proc = subprocess.Popen(
                cmd,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL
            )
            logger.info(f"mDNS Service published via avahi-publish for {hostname}")
        except Exception as e:
            logger.warning(f"Could not publish via avahi: {e}")

    async def _listen_loop(self):
        loop = asyncio.get_running_loop()
        while self.running:
            try:
                data, addr = await loop.sock_recvfrom(self.udp_sock, 1024)
                message = data.decode("utf-8", errors="ignore").strip()
                if "LINUX_REMOTE_DISCOVER" in message or "DISCOVER" in message:
                    reply = json.dumps(self.get_discovery_payload()).encode("utf-8")
                    self.udp_sock.sendto(reply, addr)
                    logger.debug(f"Replied to discovery probe from {addr}")
            except Exception:
                await asyncio.sleep(0.5)

    async def _periodic_broadcast_loop(self):
        while self.running:
            try:
                payload = json.dumps(self.get_discovery_payload()).encode("utf-8")
                # 1. Global broadcast
                try:
                    self.udp_sock.sendto(payload, ("255.255.255.255", DISCOVERY_PORT))
                except Exception:
                    pass

                # 2. Targeted subnet broadcasts on each local network interface
                try:
                    import psutil
                    for iface_name, addrs in psutil.net_if_addrs().items():
                        if iface_name.startswith('lo') or 'docker' in iface_name or 'veth' in iface_name:
                            continue
                        for addr in addrs:
                            if addr.family == socket.AF_INET and getattr(addr, 'broadcast', None):
                                try:
                                    self.udp_sock.sendto(payload, (addr.broadcast, DISCOVERY_PORT))
                                except Exception:
                                    pass
                except Exception:
                    pass
            except Exception:
                pass
            await asyncio.sleep(3.0)

    async def stop_async(self):
        """Asynchronous shutdown of all discovery services."""
        self.running = False
        for t in (self._listen_task, self._broadcast_task):
            if t and not t.done():
                t.cancel()
                try:
                    await t
                except (asyncio.CancelledError, Exception):
                    pass
        self._listen_task = None
        self._broadcast_task = None
        await self._cleanup_zeroconf()

        if self.avahi_proc:
            try:
                self.avahi_proc.terminate()
                self.avahi_proc.wait(timeout=1)
            except Exception:
                pass
            self.avahi_proc = None

        if self.udp_sock:
            try:
                self.udp_sock.close()
            except Exception:
                pass
            self.udp_sock = None

    def stop(self):
        """Synchronous wrapper for stop that safely dispatches async cleanup."""
        self.running = False
        try:
            loop = asyncio.get_running_loop()
            if loop.is_running():
                loop.create_task(self._cleanup_zeroconf())
            else:
                loop.run_until_complete(self._cleanup_zeroconf())
        except RuntimeError:
            if self.aio_zeroconf:
                try:
                    new_loop = asyncio.new_event_loop()
                    new_loop.run_until_complete(self.aio_zeroconf.async_close())
                    new_loop.close()
                except Exception:
                    pass
                self.aio_zeroconf = None
                self.zeroconf_info = None

            if self.sync_zeroconf:
                try:
                    t = threading.Thread(target=self.sync_zeroconf.close, daemon=True)
                    t.start()
                    t.join(timeout=1.5)
                except Exception:
                    pass
                self.sync_zeroconf = None
                self.zeroconf_info = None

        if self.avahi_proc:
            try:
                self.avahi_proc.terminate()
                self.avahi_proc.wait(timeout=1)
            except Exception:
                pass
            self.avahi_proc = None

        if self.udp_sock:
            try:
                self.udp_sock.close()
            except Exception:
                pass
            self.udp_sock = None
