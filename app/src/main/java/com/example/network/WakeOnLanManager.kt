package com.example.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object WakeOnLanManager {
    private const val TAG = "WakeOnLan"
    const val DEFAULT_WOL_PORT = 9
    const val SECONDARY_WOL_PORT = 7

    /**
     * Normalizes and validates a MAC address string into a standardized "AA:BB:CC:DD:EE:FF" format.
     * Returns null if the MAC is invalid.
     */
    fun normalizeMacAddress(macStr: String): String? {
        val clean = macStr.replace(":", "")
            .replace("-", "")
            .replace(".", "")
            .replace(" ", "")
            .trim()
            .uppercase()

        if (clean.length != 12 || !clean.all { it in "0123456789ABCDEF" }) {
            return null
        }

        return clean.chunked(2).joinToString(":")
    }

    /**
     * Checks if a MAC address string is valid.
     */
    fun isValidMacAddress(macStr: String): Boolean {
        return normalizeMacAddress(macStr) != null
    }

    /**
     * Sends Wake-on-LAN magic packets across multiple standard ports and addresses
     * to maximize chance of router/switch traversal.
     */
    suspend fun sendMagicPacket(
        macAddress: String,
        targetIp: String = "255.255.255.255"
    ): Result<String> = withContext(Dispatchers.IO) {
        val normalizedMac = normalizeMacAddress(macAddress)
            ?: return@withContext Result.failure(
                IllegalArgumentException("Invalid MAC address: '$macAddress'. Must be 12 hex digits (e.g., 70:85:C2:5B:14:8A).")
            )

        try {
            val macBytes = getMacBytes(normalizedMac)
            // Magic packet = 6 bytes of 0xFF followed by MAC address repeated 16 times (total 102 bytes)
            val packetBytes = ByteArray(6 + 16 * macBytes.size)

            for (i in 0 until 6) {
                packetBytes[i] = 0xff.toByte()
            }

            var offset = 6
            for (i in 0 until 16) {
                System.arraycopy(macBytes, 0, packetBytes, offset, macBytes.size)
                offset += macBytes.size
            }

            // Send to both port 9 and port 7, and broadcast as well as target IP if different
            val ports = listOf(DEFAULT_WOL_PORT, SECONDARY_WOL_PORT)
            val targets = mutableListOf<String>()
            targets.add("255.255.255.255")
            if (targetIp.isNotBlank() && targetIp != "255.255.255.255" && !targetIp.startsWith("127.")) {
                targets.add(targetIp)
            }

            val socket = DatagramSocket()
            socket.broadcast = true

            var packetsSent = 0
            for (dest in targets) {
                val address = InetAddress.getByName(dest)
                for (port in ports) {
                    val packet = DatagramPacket(packetBytes, packetBytes.size, address, port)
                    socket.send(packet)
                    packetsSent++
                }
            }
            socket.close()

            Log.d(TAG, "Sent $packetsSent Wake-on-LAN magic packet(s) for $normalizedMac")
            Result.success("Sent magic packet to $normalizedMac ($packetsSent packets)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send WoL packet: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun getMacBytes(cleanMac: String): ByteArray {
        val raw = cleanMac.replace(":", "")
        val bytes = ByteArray(6)
        for (i in 0 until 6) {
            val hex = raw.substring(i * 2, i * 2 + 2)
            bytes[i] = hex.toInt(16).toByte()
        }
        return bytes
    }
}
