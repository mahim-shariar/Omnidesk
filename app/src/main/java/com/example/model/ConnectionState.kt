package com.example.model

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Discovering : ConnectionState()
    data class Connecting(val host: String, val port: Int) : ConnectionState()
    data class PairingRequired(val host: String, val port: Int, val deviceName: String) : ConnectionState()
    data class Connected(
        val host: String,
        val port: Int,
        val pcName: String,
        val osInfo: String,
        val pingMs: Long = 0L
    ) : ConnectionState()
    data class Error(val message: String) : ConnectionState()

    val isConnected: Boolean
        get() = this is Connected
}
