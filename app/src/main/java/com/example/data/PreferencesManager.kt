package com.example.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistent state and preferences for Linux Remote,
 * including remembering the last successfully connected Linux host IP,
 * auto-reconnect preferences, and input sensitivity settings.
 */
class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var lastConnectedIp: String?
        get() = prefs.getString(KEY_LAST_CONNECTED_IP, null)
        set(value) = prefs.edit().putString(KEY_LAST_CONNECTED_IP, value).apply()

    var lastConnectedPort: Int
        get() = prefs.getInt(KEY_LAST_CONNECTED_PORT, 8765)
        set(value) = prefs.edit().putInt(KEY_LAST_CONNECTED_PORT, value).apply()

    var lastConnectedName: String?
        get() = prefs.getString(KEY_LAST_CONNECTED_NAME, null)
        set(value) = prefs.edit().putString(KEY_LAST_CONNECTED_NAME, value).apply()

    var lastConnectedOsName: String?
        get() = prefs.getString(KEY_LAST_CONNECTED_OS_NAME, null)
        set(value) = prefs.edit().putString(KEY_LAST_CONNECTED_OS_NAME, value).apply()

    var lastConnectedOsType: String?
        get() = prefs.getString(KEY_LAST_CONNECTED_OS_TYPE, null)
        set(value) = prefs.edit().putString(KEY_LAST_CONNECTED_OS_TYPE, value).apply()

    var lastConnectedToken: String?
        get() = prefs.getString(KEY_LAST_CONNECTED_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_LAST_CONNECTED_TOKEN, value).apply()

    var lastConnectedTimestamp: Long
        get() = prefs.getLong(KEY_LAST_CONNECTED_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CONNECTED_TIMESTAMP, value).apply()

    var isUsbMode: Boolean
        get() = prefs.getBoolean(KEY_IS_USB_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_USB_MODE, value).apply()

    var autoReconnectEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RECONNECT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RECONNECT_ENABLED, value).apply()

    var autoReconnectMaxRetries: Int
        get() = prefs.getInt(KEY_AUTO_RECONNECT_MAX_RETRIES, 5)
        set(value) = prefs.edit().putInt(KEY_AUTO_RECONNECT_MAX_RETRIES, value).apply()

    var mouseSensitivity: Float
        get() = prefs.getFloat(KEY_MOUSE_SENSITIVITY, 1.3f)
        set(value) = prefs.edit().putFloat(KEY_MOUSE_SENSITIVITY, value).apply()

    var scrollSpeed: Float
        get() = prefs.getFloat(KEY_SCROLL_SPEED, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SCROLL_SPEED, value).apply()

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, value).apply()

    var tapToClickEnabled: Boolean
        get() = prefs.getBoolean(KEY_TAP_TO_CLICK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TAP_TO_CLICK_ENABLED, value).apply()

    var singleTapAction: String
        get() = prefs.getString(KEY_SINGLE_TAP_ACTION, "left") ?: "left"
        set(value) = prefs.edit().putString(KEY_SINGLE_TAP_ACTION, value).apply()

    var naturalScrolling: Boolean
        get() = prefs.getBoolean(KEY_NATURAL_SCROLLING, false)
        set(value) = prefs.edit().putBoolean(KEY_NATURAL_SCROLLING, value).apply()

    /**
     * Checks if there is a previously remembered Linux host IP.
     */
    fun hasSavedConnection(): Boolean {
        return !lastConnectedIp.isNullOrBlank()
    }

    /**
     * Stores all credentials and connection info for the last successfully connected Linux host.
     */
    fun saveLastSuccessfulConnection(
        ip: String,
        port: Int,
        name: String,
        token: String? = null,
        timestamp: Long = System.currentTimeMillis(),
        osName: String? = null,
        osType: String? = null
    ) {
        val isUsb = ip == "127.0.0.1" || ip.lowercase().contains("usb") || name.contains("USB", ignoreCase = true)
        val editor = prefs.edit()
            .putString(KEY_LAST_CONNECTED_IP, ip)
            .putInt(KEY_LAST_CONNECTED_PORT, port)
            .putString(KEY_LAST_CONNECTED_NAME, name)
            .putLong(KEY_LAST_CONNECTED_TIMESTAMP, timestamp)
            .putBoolean(KEY_IS_USB_MODE, isUsb)
        if (!token.isNullOrBlank()) {
            editor.putString(KEY_LAST_CONNECTED_TOKEN, token)
        }
        if (!osName.isNullOrBlank()) {
            editor.putString(KEY_LAST_CONNECTED_OS_NAME, osName)
        }
        if (!osType.isNullOrBlank()) {
            editor.putString(KEY_LAST_CONNECTED_OS_TYPE, osType)
        }
        editor.apply()
    }

    /**
     * Clears the remembered last host IP and connection details.
     */
    fun clearLastConnection() {
        prefs.edit()
            .remove(KEY_LAST_CONNECTED_IP)
            .remove(KEY_LAST_CONNECTED_PORT)
            .remove(KEY_LAST_CONNECTED_NAME)
            .remove(KEY_LAST_CONNECTED_OS_NAME)
            .remove(KEY_LAST_CONNECTED_OS_TYPE)
            .remove(KEY_LAST_CONNECTED_TOKEN)
            .remove(KEY_LAST_CONNECTED_TIMESTAMP)
            .remove(KEY_IS_USB_MODE)
            .apply()
    }

    /**
     * Clears only the saved authorization token (e.g. when revoked or server reset).
     */
    fun clearSavedToken(ip: String? = null) {
        if (ip == null || ip == lastConnectedIp) {
            prefs.edit().remove(KEY_LAST_CONNECTED_TOKEN).apply()
        }
    }

    /**
     * Returns a human-friendly representation of when the host was last connected.
     */
    fun getFormattedLastConnectedTime(): String {
        val ts = lastConnectedTimestamp
        if (ts <= 0L) return "Never"
        val diff = System.currentTimeMillis() - ts
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hr ago"
            days == 1L -> "Yesterday"
            else -> "$days days ago"
        }
    }

    companion object {
        private const val PREFS_NAME = "linux_remote_prefs"
        private const val KEY_LAST_CONNECTED_IP = "last_connected_ip"
        private const val KEY_LAST_CONNECTED_PORT = "last_connected_port"
        private const val KEY_LAST_CONNECTED_NAME = "last_connected_name"
        private const val KEY_LAST_CONNECTED_OS_NAME = "last_connected_os_name"
        private const val KEY_LAST_CONNECTED_OS_TYPE = "last_connected_os_type"
        private const val KEY_LAST_CONNECTED_TOKEN = "last_connected_token"
        private const val KEY_LAST_CONNECTED_TIMESTAMP = "last_connected_timestamp"
        private const val KEY_IS_USB_MODE = "is_usb_mode"
        private const val KEY_AUTO_RECONNECT_ENABLED = "auto_reconnect_enabled"
        private const val KEY_AUTO_RECONNECT_MAX_RETRIES = "auto_reconnect_max_retries"
        private const val KEY_MOUSE_SENSITIVITY = "mouse_sensitivity"
        private const val KEY_SCROLL_SPEED = "scroll_speed"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
        private const val KEY_TAP_TO_CLICK_ENABLED = "tap_to_click_enabled"
        private const val KEY_SINGLE_TAP_ACTION = "single_tap_action"
        private const val KEY_NATURAL_SCROLLING = "natural_scrolling"
    }
}
