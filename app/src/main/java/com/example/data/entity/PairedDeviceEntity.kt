package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paired_devices")
data class PairedDeviceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val ip: String,
    val port: Int = 8765,
    val token: String,
    val macOrUid: String = "",
    val osName: String = "Desktop PC",
    val osType: String = "unknown",
    val lastConnected: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
) {
    val isWindows: Boolean
        get() = osType.equals("windows", ignoreCase = true) || osName.contains("windows", ignoreCase = true)

    val isMac: Boolean
        get() = osType.equals("macos", ignoreCase = true) || osType.equals("darwin", ignoreCase = true) ||
                osType.equals("apple", ignoreCase = true) || osName.contains("mac", ignoreCase = true) ||
                osName.contains("darwin", ignoreCase = true) || osName.contains("apple", ignoreCase = true)

    val isLinux: Boolean
        get() = !isWindows && !isMac && (osType.equals("linux", ignoreCase = true) || osName.contains("linux", ignoreCase = true) ||
                osName.contains("ubuntu", ignoreCase = true) || osName.contains("mint", ignoreCase = true) ||
                osName.contains("fedora", ignoreCase = true) || osName.contains("debian", ignoreCase = true))

    val osDisplayName: String
        get() = when {
            isWindows -> if (osName.isNotBlank() && !osName.equals("Desktop PC", ignoreCase = true)) osName else "Windows"
            isMac -> if (osName.isNotBlank() && !osName.equals("Desktop PC", ignoreCase = true)) osName else "macOS"
            isLinux -> if (osName.isNotBlank() && !osName.equals("Desktop PC", ignoreCase = true)) osName else "Linux"
            else -> if (osName.isNotBlank()) osName else "Desktop PC"
        }
}
