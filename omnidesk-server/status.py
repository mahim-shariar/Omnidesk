import glob
import logging
import os
import platform
import socket
import time
from typing import Dict

logger = logging.getLogger("OmniDeskStatus")

try:
    import psutil
    HAS_PSUTIL = True
except Exception:
    HAS_PSUTIL = False

class StatusManager:
    def __init__(self):
        self.hostname = socket.gethostname()
        self.os_info = self._get_os_info()

    def _get_os_info(self) -> Dict[str, str]:
        sys_name = platform.system()
        if sys_name == "Windows":
            rel = platform.release()
            return {
                "distro": "Windows",
                "version": f"Windows {rel}",
                "os_type": "windows"
            }
        elif sys_name == "Darwin":
            mac_ver = platform.mac_ver()[0]
            return {
                "distro": "macOS",
                "version": f"macOS {mac_ver}" if mac_ver else "macOS",
                "os_type": "macos"
            }
        else:
            distro = "Linux"
            version = ""
            try:
                if os.path.exists("/etc/os-release"):
                    with open("/etc/os-release", "r") as f:
                        for line in f:
                            if line.startswith("PRETTY_NAME="):
                                distro = line.split("=", 1)[1].strip().strip('"')
                            elif line.startswith("VERSION="):
                                version = line.split("=", 1)[1].strip().strip('"')
                else:
                    distro = platform.system() + " " + platform.release()
            except Exception:
                pass
            return {"distro": distro, "version": version, "os_type": "linux"}

    def _get_local_ip(self) -> str:
        # 1. Try connecting UDP socket to common external endpoints to find outbound route
        for target in ('8.8.8.8', '1.1.1.1', '10.255.255.255'):
            try:
                s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                s.connect((target, 1))
                ip = s.getsockname()[0]
                s.close()
                if ip and not ip.startswith('127.'):
                    return ip
            except Exception:
                pass

        # 2. Try psutil net_if_addrs
        if HAS_PSUTIL:
            try:
                for iface_name, addrs in psutil.net_if_addrs().items():
                    if iface_name.startswith('lo') or 'docker' in iface_name or 'veth' in iface_name:
                        continue
                    for addr in addrs:
                        if addr.family == socket.AF_INET and not addr.address.startswith('127.'):
                            return addr.address
            except Exception:
                pass

        # 3. Fallback to hostname resolution
        try:
            ip = socket.gethostbyname(socket.gethostname())
            if ip and not ip.startswith('127.'):
                return ip
        except Exception:
            pass

        return '127.0.0.1'

    def _get_cpu_temperature(self) -> float:
        if HAS_PSUTIL and hasattr(psutil, "sensors_temperatures"):
            try:
                temps = psutil.sensors_temperatures()
                if temps:
                    for name, entries in temps.items():
                        for entry in entries:
                            if entry.current > 0:
                                return round(entry.current, 1)
            except Exception:
                pass

        # Fallback to /sys/class/thermal
        try:
            for zone in glob.glob("/sys/class/thermal/thermal_zone*/temp"):
                with open(zone, "r") as f:
                    val = float(f.read().strip())
                    return round(val / 1000.0, 1)
        except Exception:
            pass

        return 0.0

    def _get_mac_address(self) -> str:
        try:
            for iface in sorted(glob.glob("/sys/class/net/*")):
                iface_name = os.path.basename(iface)
                if iface_name != "lo" and not iface_name.startswith("docker") and not iface_name.startswith("veth"):
                    addr_file = os.path.join(iface, "address")
                    if os.path.exists(addr_file):
                        with open(addr_file, "r") as f:
                            mac = f.read().strip()
                            if mac and mac != "00:00:00:00:00:00":
                                return mac.upper()
        except Exception:
            pass

        try:
            import uuid
            node = uuid.getnode()
            mac = ':'.join(f"{(node >> i) & 0xff:02X}" for i in range(0, 48, 8)[::-1])
            return mac
        except Exception:
            return ""

    def _get_battery_info(self) -> Dict:
        # 1. psutil sensors_battery
        if HAS_PSUTIL and hasattr(psutil, "sensors_battery"):
            try:
                b = psutil.sensors_battery()
                if b is not None:
                    return {
                        "has_battery": True,
                        "battery_percent": round(float(b.percent), 1),
                        "is_charging": bool(b.power_plugged),
                        "is_laptop": True
                    }
            except Exception:
                pass

        # 2. Linux /sys/class/power_supply fallback
        try:
            bat_dirs = sorted(glob.glob("/sys/class/power_supply/BAT*"))
            if bat_dirs:
                bat_dir = bat_dirs[0]
                cap_file = os.path.join(bat_dir, "capacity")
                status_file = os.path.join(bat_dir, "status")
                pct = 0.0
                if os.path.exists(cap_file):
                    with open(cap_file, "r") as f:
                        pct = float(f.read().strip())
                is_charging = False
                if os.path.exists(status_file):
                    with open(status_file, "r") as f:
                        st = f.read().strip().lower()
                        is_charging = (st in ("charging", "full"))
                return {
                    "has_battery": True,
                    "battery_percent": round(pct, 1),
                    "is_charging": is_charging,
                    "is_laptop": True
                }
        except Exception:
            pass

        return {
            "has_battery": False,
            "battery_percent": None,
            "is_charging": False,
            "is_laptop": False
        }

    def get_system_status(self) -> Dict:
        cpu_pct = 0.0
        ram_pct = 0.0
        ram_used_gb = 0.0
        ram_total_gb = 0.0
        disk_pct = 0.0
        disk_used_gb = 0.0
        disk_total_gb = 0.0
        uptime_sec = 0

        if HAS_PSUTIL:
            try:
                cpu_pct = psutil.cpu_percent(interval=None)
                vmem = psutil.virtual_memory()
                ram_pct = vmem.percent
                ram_used_gb = round(vmem.used / (1024**3), 1)
                ram_total_gb = round(vmem.total / (1024**3), 1)

                disk_path = os.getenv("SystemDrive", "C:") + "\\" if platform.system() == "Windows" else "/"
                disk = psutil.disk_usage(disk_path)
                disk_pct = disk.percent
                disk_used_gb = round(disk.used / (1024**3), 1)
                disk_total_gb = round(disk.total / (1024**3), 1)

                uptime_sec = int(time.time() - psutil.boot_time())
            except Exception as e:
                logger.debug(f"psutil status error: {e}")
        else:
            # Fallback reading /proc/loadavg and /proc/meminfo
            try:
                with open("/proc/loadavg", "r") as f:
                    cpu_pct = round(float(f.read().split()[0]) * 10, 1)
                with open("/proc/uptime", "r") as f:
                    uptime_sec = int(float(f.read().split()[0]))
            except Exception:
                pass

        temp_c = self._get_cpu_temperature()
        ip_addr = self._get_local_ip()
        mac_addr = self._get_mac_address()
        bat_info = self._get_battery_info()

        return {
            "hostname": self.hostname,
            "distro": self.os_info["distro"],
            "version": self.os_info["version"],
            "ip": ip_addr,
            "mac": mac_addr,
            "cpu_percent": round(cpu_pct, 1),
            "ram_percent": round(ram_pct, 1),
            "ram_used_gb": ram_used_gb,
            "ram_total_gb": ram_total_gb,
            "disk_percent": round(disk_pct, 1),
            "disk_used_gb": disk_used_gb,
            "disk_total_gb": disk_total_gb,
            "cpu_temp": temp_c,
            "uptime_seconds": uptime_sec,
            "has_battery": bat_info["has_battery"],
            "battery_percent": bat_info["battery_percent"],
            "is_charging": bat_info["is_charging"],
            "is_laptop": bat_info["is_laptop"],
            "os_type": self.os_info.get("os_type", "linux"),
            "timestamp": time.time()
        }
