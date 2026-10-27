package com.example.model

data class DiscoveredDevice(
    val id: String,
    val hostname: String,
    val ip: String,
    val port: Int = 8765,
    val os: String = "Desktop PC",
    val version: String = "Universal",
    val mac: String = "",
    val isLaptop: Boolean = false,
    val batteryPercent: Float? = null,
    val osType: String = "unknown",
    val lastSeen: Long = System.currentTimeMillis()
) {
    val isWindows: Boolean
        get() = osType.equals("windows", ignoreCase = true) || os.contains("windows", ignoreCase = true) || os.contains("win1", ignoreCase = true) || os.contains("win7", ignoreCase = true)

    val isMac: Boolean
        get() = osType.equals("macos", ignoreCase = true) || osType.equals("darwin", ignoreCase = true) ||
                osType.equals("apple", ignoreCase = true) ||
                os.contains("mac", ignoreCase = true) || os.contains("darwin", ignoreCase = true) || os.contains("apple", ignoreCase = true) || os.contains("osx", ignoreCase = true)

    val isLinux: Boolean
        get() = !isWindows && !isMac && (osType.equals("linux", ignoreCase = true) || os.contains("linux", ignoreCase = true) ||
                os.contains("mint", ignoreCase = true) || os.contains("ubuntu", ignoreCase = true) ||
                os.contains("debian", ignoreCase = true) || os.contains("fedora", ignoreCase = true) ||
                os.contains("arch", ignoreCase = true) || os.contains("manjaro", ignoreCase = true) ||
                os.contains("pop!_os", ignoreCase = true) || os.contains("zorin", ignoreCase = true))

    val osDisplayName: String
        get() = when {
            isWindows -> "Windows"
            isMac -> "macOS"
            isLinux -> "Linux"
            else -> if (os.isNotBlank() && !os.equals("Desktop PC", ignoreCase = true) && !os.equals("Universal", ignoreCase = true)) os else "PC"
        }
}

data class PcStatus(
    val hostname: String = "PC",
    val distro: String = "Desktop PC",
    val version: String = "Universal",
    val ip: String = "",
    val mac: String = "",
    val cpuPercent: Float = 0f,
    val ramPercent: Float = 0f,
    val ramUsedGb: Float = 0f,
    val ramTotalGb: Float = 0f,
    val diskPercent: Float = 0f,
    val diskUsedGb: Float = 0f,
    val diskTotalGb: Float = 0f,
    val cpuTemp: Float = 0f,
    val uptimeSeconds: Long = 0L,
    val hasBattery: Boolean = false,
    val batteryPercent: Float? = null,
    val isCharging: Boolean = false,
    val isLaptop: Boolean = false,
    val osType: String = "unknown",
    val timestamp: Long = System.currentTimeMillis()
) {
    val isWindows: Boolean
        get() = osType.equals("windows", ignoreCase = true) || distro.contains("windows", ignoreCase = true) || distro.contains("win1", ignoreCase = true) || distro.contains("win7", ignoreCase = true)

    val isMac: Boolean
        get() = osType.equals("macos", ignoreCase = true) || osType.equals("darwin", ignoreCase = true) ||
                osType.equals("apple", ignoreCase = true) ||
                distro.contains("mac", ignoreCase = true) || distro.contains("darwin", ignoreCase = true) || distro.contains("apple", ignoreCase = true) || distro.contains("osx", ignoreCase = true)

    val isLinux: Boolean
        get() = !isWindows && !isMac && (osType.equals("linux", ignoreCase = true) || distro.contains("linux", ignoreCase = true) ||
                distro.contains("mint", ignoreCase = true) || distro.contains("ubuntu", ignoreCase = true) ||
                distro.contains("debian", ignoreCase = true) || distro.contains("fedora", ignoreCase = true) ||
                distro.contains("arch", ignoreCase = true) || distro.contains("manjaro", ignoreCase = true) ||
                distro.contains("pop!_os", ignoreCase = true) || distro.contains("zorin", ignoreCase = true))

    val osDisplayName: String
        get() = when {
            isWindows -> "Windows"
            isMac -> "macOS"
            isLinux -> "Linux"
            else -> if (distro.isNotBlank() && !distro.equals("Desktop PC", ignoreCase = true) && !distro.equals("Universal", ignoreCase = true)) distro else "PC"
        }

    val formattedUptime: String
        get() {
            val hours = uptimeSeconds / 3600
            val minutes = (uptimeSeconds % 3600) / 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
}

data class AudioStatus(
    val volume: Int = 50,
    val muted: Boolean = false
)

data class ApplicationItem(
    val id: String,
    val name: String,
    val icon: String = "application-x-executable",
    val exec: String = ""
)

data class ShortcutItem(
    val id: String,
    val label: String,
    val keys: String,
    val description: String = ""
)

data class QrPairingPayload(
    val ip: String,
    val port: Int = 8765,
    val pin: String? = null,
    val token: String? = null,
    val hostname: String = "PC",
    val os: String = "Universal",
    val raw: String = ""
) {
    companion object {
        fun parse(rawText: String): QrPairingPayload? {
            val text = rawText.trim()
            if (text.isEmpty()) return null

            // 1. Try parsing JSON format: {"protocol":"omnidesk","ip":"192.168.1.50","port":8765,"pin":"123456",...}
            if (text.startsWith("{") && text.endsWith("}")) {
                try {
                    val json = org.json.JSONObject(text)
                    val ip = json.optString("ip").ifEmpty { json.optString("host") }
                    if (ip.isNotBlank()) {
                        val port = json.optInt("port", 8765)
                        val pin = json.optString("pin").takeIf { it.isNotBlank() }
                        val token = json.optString("token").takeIf { it.isNotBlank() }
                        val hostname = json.optString("hostname").ifEmpty { json.optString("name", "PC") }
                        val os = json.optString("os", "Universal")
                        return QrPairingPayload(
                            ip = ip,
                            port = if (port in 1..65535) port else 8765,
                            pin = pin,
                            token = token,
                            hostname = hostname,
                            os = os,
                            raw = text
                        )
                    }
                } catch (_: Exception) {
                }
            }

            // 2. Try parsing custom URI format: omnidesk://ip:port?pin=123456&name=Alex-PC
            if (text.startsWith("omnidesk://", ignoreCase = true) || text.startsWith("ws://", ignoreCase = true)) {
                try {
                    val uri = android.net.Uri.parse(text)
                    val host = uri.host ?: ""
                    val port = if (uri.port > 0) uri.port else 8765
                    val pin = uri.getQueryParameter("pin")
                    val token = uri.getQueryParameter("token")
                    val name = uri.getQueryParameter("name") ?: uri.getQueryParameter("hostname") ?: "PC"
                    val os = uri.getQueryParameter("os") ?: "Universal"
                    if (host.isNotBlank()) {
                        return QrPairingPayload(
                            ip = host,
                            port = port,
                            pin = pin,
                            token = token,
                            hostname = name,
                            os = os,
                            raw = text
                        )
                    }
                } catch (_: Exception) {
                }
            }

            // 3. Try parsing simple delimited format: "ip:port:pin" or "ip:port" or "ip"
            val parts = text.split(":")
            if (parts.isNotEmpty()) {
                val candidateIp = parts[0].trim()
                val isIpPattern = candidateIp.matches(Regex("""^(\d{1,3}\.){3}\d{1,3}$""")) || candidateIp.equals("localhost", ignoreCase = true)
                if (isIpPattern) {
                    val port = parts.getOrNull(1)?.toIntOrNull() ?: 8765
                    val pin = parts.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() }
                    return QrPairingPayload(
                        ip = candidateIp,
                        port = if (port in 1..65535) port else 8765,
                        pin = pin,
                        hostname = "PC ($candidateIp)",
                        raw = text
                    )
                }
            }

            return null
        }
    }
}

