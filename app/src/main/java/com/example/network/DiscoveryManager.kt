package com.example.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.example.model.DiscoveredDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

/**
 * Manages triple-mode local network discovery for Linux Remote:
 * 1. Active Subnet TCP Sweep (port 8765): Bypasses router AP isolation and UDP broadcast/multicast blocking.
 * 2. Network Service Discovery (NSD / mDNS / DNS-SD) via Android's NsdManager (_linuxremote._tcp).
 * 3. UDP Broadcast probes and responders (port 8766).
 * 4. USB Cable (ADB reverse 127.0.0.1 and USB tethering gateways).
 *
 * This multi-layered approach guarantees device discovery regardless of Wi-Fi router isolation,
 * mesh networks, or corporate/home multicast restrictions.
 */
class DiscoveryManager(private val context: Context) {
    private val TAG = "DiscoveryManager"
    private val NSD_SERVICE_TYPE = "_linuxremote._tcp"
    private val DISCOVERY_PORT = 8766
    private val WS_PORT = 8765

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _localDeviceIp = MutableStateFlow<String>("")
    val localDeviceIp: StateFlow<String> = _localDeviceIp.asStateFlow()

    private val _scanStatusMessage = MutableStateFlow<String>("")
    val scanStatusMessage: StateFlow<String> = _scanStatusMessage.asStateFlow()

    private var scanJob: Job? = null
    private var subnetSweepJob: Job? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var socket: DatagramSocket? = null
    private var discoveryScope: CoroutineScope? = null
    private var resolveTimeoutJob: Job? = null

    private val nsdManager: NsdManager? by lazy {
        context.applicationContext.getSystemService(Context.NSD_SERVICE) as? NsdManager
    }
    private var nsdDiscoveryListener: NsdManager.DiscoveryListener? = null
    private var isNsdDiscovering = false

    // Resolution queue to prevent NsdManager.FAILURE_ALREADY_ACTIVE
    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var isResolving = false

    fun startDiscovery(scope: CoroutineScope, forceRestart: Boolean = false) {
        discoveryScope = scope
        updateLocalIp()

        if (forceRestart && _isScanning.value) {
            stopDiscovery()
        } else if (_isScanning.value) {
            // If already scanning and not force restarting, trigger fresh probes and ensure NSD is active
            if (!isNsdDiscovering) {
                startNsdDiscovery()
            }
            scope.launch(Dispatchers.IO) {
                sendProbe()
                sweepActiveSubnets(scope)
            }
            return
        }

        _isScanning.value = true
        _scanStatusMessage.value = "Searching for PCs (Windows, Mac & Linux)..."

        acquireMulticastLock()
        startNsdDiscovery()

        // 1. Launch active TCP subnet sweep in parallel (bypasses router broadcast blocking)
        sweepActiveSubnets(scope)

        // 2. Launch UDP broadcast and listen loop
        scanJob = scope.launch(Dispatchers.IO) {
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    bind(InetSocketAddress(0)) // Free local UDP port
                    soTimeout = 2000
                }

                // Launch receiver coroutine
                val receiveJob = launch {
                    val buffer = ByteArray(2048)
                    while (isActive) {
                        try {
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket?.receive(packet)
                            val receivedText = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                            val senderIp = packet.address.hostAddress ?: ""
                            handleDiscoveryPacket(receivedText, senderIp)
                        } catch (e: Exception) {
                            // Timeout is normal for UDP receive
                        }
                    }
                }

                // Send initial discovery probe immediately
                sendProbe()

                // Send discovery probe periodically
                var iteration = 0
                while (isActive) {
                    delay(3000)
                    sendProbe()
                    iteration++
                    // Every 9 seconds, run a quick subnet sweep again to catch any newly joined PCs
                    if (iteration % 3 == 0) {
                        sweepActiveSubnets(scope)
                    }
                }

                receiveJob.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Discovery UDP loop error: ${e.message}", e)
            } finally {
                stopDiscovery()
            }
        }
    }

    private fun updateLocalIp() {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces != null && interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                for (addr in iface.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: ""
                        if (ip.isNotBlank()) {
                            _localDeviceIp.value = ip
                            return
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun sendProbe() {
        // 1. Probe USB Cable (adb reverse tcp:8765 tcp:8765 or USB tethering)
        probeUsbConnection()

        val probePayload = "LINUX_REMOTE_DISCOVER".toByteArray(Charsets.UTF_8)
        val broadcastAddresses = getBroadcastAddresses()
        val currentSocket = socket ?: return

        for (bcast in broadcastAddresses) {
            try {
                val packet = DatagramPacket(probePayload, probePayload.size, bcast, DISCOVERY_PORT)
                currentSocket.send(packet)
            } catch (e: Exception) {
                Log.d(TAG, "Error sending probe to $bcast: ${e.message}")
            }
        }
    }

    /**
     * Actively sweeps all local IPv4 /24 subnets for port 8765 using non-blocking parallel coroutines.
     * This ensures the Linux PC is found even if the Wi-Fi router blocks UDP broadcasts or mDNS.
     */
    private fun sweepActiveSubnets(scope: CoroutineScope) {
        subnetSweepJob?.cancel()
        subnetSweepJob = scope.launch(Dispatchers.IO) {
            val prefixes = getSubnetPrefixes()
            if (prefixes.isEmpty()) {
                _scanStatusMessage.value = "Waiting for Wi-Fi connection..."
                return@launch
            }

            val prefixDisplay = prefixes.joinToString(", ") { "${it}x" }
            _scanStatusMessage.value = "Scanning Wi-Fi ($prefixDisplay) & mDNS..."

            val semaphore = Semaphore(35) // Concurrently test up to 35 IPs to stay light on resources

            for (prefix in prefixes) {
                for (octet in 1..254) {
                    if (!isActive) break
                    val targetIp = "$prefix$octet"
                    launch {
                        semaphore.withPermit {
                            probeHostTcp(targetIp, WS_PORT)
                        }
                    }
                }
            }

            // Also probe common router gateway IPs directly (e.g., if PC is serving as router/hotspot)
            for (prefix in prefixes) {
                val gateway = "${prefix}1"
                launch { probeHostTcp(gateway, WS_PORT) }
            }
        }
    }

    private fun probeHostTcp(ip: String, port: Int) {
        try {
            Socket().use { s ->
                s.soTimeout = 600
                s.connect(InetSocketAddress(ip, port), 320)

                // Successfully connected on port 8765! Send WebSocket probe to fetch discovery payload
                val req = "GET /discover HTTP/1.1\r\n" +
                        "Host: $ip:$port\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
                        "Sec-WebSocket-Version: 13\r\n\r\n"
                s.getOutputStream().write(req.toByteArray(Charsets.UTF_8))
                s.getOutputStream().flush()

                val reader = s.getInputStream().bufferedReader()
                val responseLines = mutableListOf<String>()
                var line: String? = reader.readLine()
                var contentLength = 0
                while (!line.isNullOrBlank()) {
                    responseLines.add(line)
                    if (line.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                    line = reader.readLine()
                }

                val body = if (contentLength > 0) {
                    val buf = CharArray(contentLength)
                    val readChars = reader.read(buf, 0, contentLength)
                    if (readChars > 0) String(buf, 0, readChars) else ""
                } else {
                    reader.readText()
                }

                if (body.contains("\"service\"") && (body.contains("linux_remote") || body.contains("omnidesk"))) {
                    val json = JSONObject(body)
                    val hostname = json.optString("hostname", "PC")
                    val os = json.optString("os", "Desktop")
                    val version = json.optString("version", "")
                    val reportedIp = json.optString("ip", ip).ifEmpty { ip }
                    val mac = json.optString("mac", "")
                    val isLaptop = json.optBoolean("is_laptop", false)
                    val batteryPercent = if (json.has("battery_percent") && !json.isNull("battery_percent")) {
                        json.optDouble("battery_percent", 0.0).toFloat()
                    } else null
                    val rawOsType = json.optString("os_type", "").lowercase().trim()
                    val osType = when {
                        rawOsType.isNotBlank() && rawOsType != "unknown" -> rawOsType
                        os.contains("windows", true) || hostname.contains("win", true) -> "windows"
                        os.contains("mac", true) || os.contains("darwin", true) || os.contains("apple", true) || hostname.contains("mac", true) -> "macos"
                        os.contains("linux", true) || os.contains("mint", true) || os.contains("ubuntu", true) || os.contains("debian", true) || os.contains("fedora", true) || os.contains("arch", true) -> "linux"
                        else -> "unknown"
                    }
                    val device = DiscoveredDevice(
                        id = "$reportedIp:$port",
                        hostname = hostname,
                        ip = reportedIp,
                        port = port,
                        os = os,
                        version = version,
                        mac = mac,
                        isLaptop = isLaptop,
                        batteryPercent = batteryPercent,
                        osType = osType,
                        lastSeen = System.currentTimeMillis()
                    )
                    addOrUpdateDiscoveredDevice(device)
                    Log.i(TAG, "Subnet sweep found $osType PC: $hostname at $reportedIp:$port")
                } else if (responseLines.any { it.contains("400") || it.contains("Upgrade") || it.contains("Sec-WebSocket") || it.contains("websockets") }) {
                    // WebSocket server signature detected on port 8765
                    val device = DiscoveredDevice(
                        id = "$ip:$port",
                        hostname = "OmniDesk PC",
                        ip = ip,
                        port = port,
                        os = "Desktop PC",
                        version = "Companion Server",
                        mac = "",
                        osType = "unknown",
                        lastSeen = System.currentTimeMillis()
                    )
                    addOrUpdateDiscoveredDevice(device)
                    Log.i(TAG, "Subnet sweep detected OmniDesk WebSocket on $ip:$port")
                }
            }
        } catch (e: Exception) {
            // Port closed or host unreachable; normal during sweep
        }
    }

    private fun getSubnetPrefixes(): List<String> {
        val prefixes = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces != null && interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                for (addr in iface.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        val parts = host.split(".")
                        if (parts.size == 4) {
                            val prefix = "${parts[0]}.${parts[1]}.${parts[2]}."
                            if (!prefixes.contains(prefix)) {
                                prefixes.add(prefix)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error enumerating subnet prefixes: ${e.message}")
        }
        return prefixes
    }

    /**
     * Probes localhost:8765 (ADB reverse over USB cable) and USB tethering interfaces.
     * When an active server is detected, it is immediately registered as a discovered USB device.
     */
    private fun probeUsbConnection() {
        // Probe localhost (active when user runs `adb reverse tcp:8765 tcp:8765` over USB)
        try {
            Socket().use { s ->
                s.connect(InetSocketAddress("127.0.0.1", WS_PORT), 450)
                val usbDevice = DiscoveredDevice(
                    id = "127.0.0.1:$WS_PORT",
                    hostname = "PC (USB Cable)",
                    ip = "127.0.0.1",
                    port = WS_PORT,
                    os = "Desktop PC (USB Cable)",
                    version = "Direct Wired",
                    mac = "USB-WIRED",
                    lastSeen = System.currentTimeMillis()
                )
                addOrUpdateDiscoveredDevice(usbDevice)
            }
        } catch (e: Exception) {
            // Not reachable on localhost
        }

        // Probe USB tethering network interfaces (rndis / usb)
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces != null && interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val name = iface.name.lowercase()
                if (name.contains("rndis") || name.contains("usb")) {
                    for (addr in iface.inetAddresses) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.")) {
                            val gateway = host.substringBeforeLast(".") + ".1"
                            try {
                                Socket().use { s ->
                                    s.connect(InetSocketAddress(gateway, WS_PORT), 450)
                                    val usbDevice = DiscoveredDevice(
                                        id = "$gateway:$WS_PORT",
                                        hostname = "PC (USB Tethering)",
                                        ip = gateway,
                                        port = WS_PORT,
                                        os = "Desktop PC (USB Tethering)",
                                        version = "Direct Wired",
                                        mac = "USB-TETHER",
                                        lastSeen = System.currentTimeMillis()
                                    )
                                    addOrUpdateDiscoveredDevice(usbDevice)
                                }
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    // ---------------------------------------------------------
    // Network Service Discovery (NSD / mDNS)
    // ---------------------------------------------------------

    private fun startNsdDiscovery() {
        val manager = nsdManager ?: return
        if (isNsdDiscovering) return

        // Stop any lingering listener before re-registering
        if (nsdDiscoveryListener != null) {
            try {
                manager.stopServiceDiscovery(nsdDiscoveryListener)
            } catch (e: Exception) {
                // Ignore
            }
            nsdDiscoveryListener = null
        }

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "NSD Service discovery started: $regType")
                isNsdDiscovering = true
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "NSD Service found: ${service.serviceName}, type: ${service.serviceType}")
                resolveServiceSafely(service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "NSD Service lost: ${service.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "NSD Service discovery stopped: $serviceType")
                isNsdDiscovering = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "NSD startDiscovery failed with error code $errorCode")
                isNsdDiscovering = false
                try {
                    manager.stopServiceDiscovery(this)
                } catch (e: Exception) {
                    // Ignore
                }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "NSD stopDiscovery failed with error code $errorCode")
                isNsdDiscovering = false
            }
        }

        nsdDiscoveryListener = listener
        try {
            manager.discoverServices(NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call discoverServices: ${e.message}", e)
            isNsdDiscovering = false
            nsdDiscoveryListener = null
        }
    }

    private fun stopNsdDiscovery() {
        val manager = nsdManager
        if (manager != null && isNsdDiscovering && nsdDiscoveryListener != null) {
            try {
                manager.stopServiceDiscovery(nsdDiscoveryListener)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping NSD discovery: ${e.message}")
            }
        }
        isNsdDiscovering = false
        nsdDiscoveryListener = null

        resolveTimeoutJob?.cancel()
        resolveTimeoutJob = null

        synchronized(resolveQueue) {
            resolveQueue.clear()
            isResolving = false
        }
    }

    private fun resolveServiceSafely(serviceInfo: NsdServiceInfo) {
        val manager = nsdManager ?: return

        // On Android 14+ (API 34+), use registerServiceInfoCallback to avoid single-listener deadlock
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val callback = object : NsdManager.ServiceInfoCallback {
                    override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                        Log.w(TAG, "NSD ServiceInfoCallback registration failed: $errorCode; fallback to queue")
                        queueResolveService(serviceInfo)
                    }

                    override fun onServiceUpdated(resolvedInfo: NsdServiceInfo) {
                        Log.d(TAG, "NSD Service resolved via ServiceInfoCallback: ${resolvedInfo.serviceName}")
                        handleResolvedNsdService(resolvedInfo)
                        try {
                            manager.unregisterServiceInfoCallback(this)
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }

                    override fun onServiceLost() {
                        Log.d(TAG, "NSD Service lost via callback: ${serviceInfo.serviceName}")
                    }

                    override fun onServiceInfoCallbackUnregistered() {
                        // Cleanup complete
                    }
                }
                manager.registerServiceInfoCallback(serviceInfo, context.mainExecutor, callback)
                return
            } catch (e: Exception) {
                Log.w(TAG, "registerServiceInfoCallback threw: ${e.message}; fallback to legacy queue")
            }
        }

        queueResolveService(serviceInfo)
    }

    private fun queueResolveService(serviceInfo: NsdServiceInfo) {
        synchronized(resolveQueue) {
            if (resolveQueue.none { it.serviceName == serviceInfo.serviceName }) {
                resolveQueue.add(serviceInfo)
            }
            if (!isResolving) {
                resolveNextInQueue()
            }
        }
    }

    private fun resolveNextInQueue() {
        synchronized(resolveQueue) {
            val next = resolveQueue.removeFirstOrNull()
            if (next == null) {
                isResolving = false
                resolveTimeoutJob?.cancel()
                resolveTimeoutJob = null
                return
            }
            isResolving = true

            // Watchdog: If Android OS fails to return callback within 3.5s, unlock queue and proceed
            resolveTimeoutJob?.cancel()
            resolveTimeoutJob = discoveryScope?.launch(Dispatchers.IO) {
                delay(3500)
                synchronized(resolveQueue) {
                    if (isResolving) {
                        Log.w(TAG, "NSD resolve timed out for ${next.serviceName}, unlocking queue")
                        isResolving = false
                        resolveNextInQueue()
                    }
                }
            }

            try {
                nsdManager?.resolveService(next, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.w(TAG, "NSD resolve failed for ${serviceInfo.serviceName} with error code $errorCode")
                        resolveTimeoutJob?.cancel()
                        resolveTimeoutJob = null
                        synchronized(resolveQueue) {
                            isResolving = false
                            resolveNextInQueue()
                        }
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        Log.d(TAG, "NSD Service resolved: ${serviceInfo.serviceName} at ${serviceInfo.host}:${serviceInfo.port}")
                        resolveTimeoutJob?.cancel()
                        resolveTimeoutJob = null
                        handleResolvedNsdService(serviceInfo)
                        synchronized(resolveQueue) {
                            isResolving = false
                            resolveNextInQueue()
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Exception during resolveService: ${e.message}")
                resolveTimeoutJob?.cancel()
                resolveTimeoutJob = null
                isResolving = false
                resolveNextInQueue()
            }
        }
    }

    private fun handleResolvedNsdService(serviceInfo: NsdServiceInfo) {
        try {
            val host = serviceInfo.host
            val rawHostIp = host?.hostAddress ?: ""

            // Extract TXT record attributes if present
            val attributes = serviceInfo.attributes ?: emptyMap()
            val hostnameAttr = attributes["hostname"]?.let { String(it, Charsets.UTF_8).trim() }
            val osAttr = attributes["os"]?.let { String(it, Charsets.UTF_8).trim() }
            val versionAttr = attributes["version"]?.let { String(it, Charsets.UTF_8).trim() }
            val macAttr = attributes["mac"]?.let { String(it, Charsets.UTF_8).trim() }
            val ipAttr = attributes["ip"]?.let { String(it, Charsets.UTF_8).trim() }

            // Prioritize IPv4 addresses so network connections succeed reliably
            val resolvedIp = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                    serviceInfo.hostAddresses.firstOrNull { it is Inet4Address }?.hostAddress
                        ?: if (!ipAttr.isNullOrBlank()) ipAttr
                        else rawHostIp.substringBefore('%')
                }
                host is Inet4Address -> rawHostIp
                !ipAttr.isNullOrBlank() -> ipAttr
                else -> rawHostIp.substringBefore('%')
            }

            if (resolvedIp.isBlank() || resolvedIp.startsWith("127.")) return

            val port = if (serviceInfo.port > 0) serviceInfo.port else WS_PORT

            val cleanServiceName = serviceInfo.serviceName
                .replace("._omnidesk._tcp.local", "")
                .replace("._omnidesk._tcp", "")
                .replace("._linuxremote._tcp.local", "")
                .replace("._linuxremote._tcp", "")
                .trim()

            val hostname = when {
                !hostnameAttr.isNullOrBlank() -> hostnameAttr
                cleanServiceName.isNotBlank() -> cleanServiceName
                else -> "Desktop PC"
            }

            val os = if (!osAttr.isNullOrBlank()) osAttr else "Desktop PC"
            val version = if (!versionAttr.isNullOrBlank()) versionAttr else ""
            val mac = macAttr ?: ""
            val osTypeAttr = attributes["os_type"]?.let { String(it, Charsets.UTF_8).trim().lowercase() }
            val osType = when {
                !osTypeAttr.isNullOrBlank() && osTypeAttr != "unknown" -> osTypeAttr
                os.contains("windows", true) || hostname.contains("win", true) -> "windows"
                os.contains("mac", true) || os.contains("darwin", true) || os.contains("apple", true) || hostname.contains("mac", true) -> "macos"
                os.contains("linux", true) || os.contains("mint", true) || os.contains("ubuntu", true) || os.contains("debian", true) || os.contains("fedora", true) || os.contains("arch", true) -> "linux"
                else -> "unknown"
            }

            val device = DiscoveredDevice(
                id = "$resolvedIp:$port",
                hostname = hostname,
                ip = resolvedIp,
                port = port,
                os = os,
                version = version,
                mac = mac,
                osType = osType,
                lastSeen = System.currentTimeMillis()
            )

            addOrUpdateDiscoveredDevice(device)
            Log.i(TAG, "NSD registered $osType PC: $hostname at $resolvedIp:$port ($os $version)")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing resolved NSD service: ${e.message}", e)
        }
    }

    // ---------------------------------------------------------
    // UDP Packet Parsing & Handling
    // ---------------------------------------------------------

    private fun handleDiscoveryPacket(jsonStr: String, senderIp: String) {
        try {
            if (!jsonStr.startsWith("{")) return
            val json = JSONObject(jsonStr)
            val service = json.optString("service")
            if (service == "linux_remote" || service == "omnidesk") {
                val hostname = json.optString("hostname", "PC")
                val os = json.optString("os", "Desktop")
                val version = json.optString("version", "")
                val port = json.optInt("port", WS_PORT)
                val reportedIp = json.optString("ip", senderIp).ifEmpty { senderIp }
                val mac = json.optString("mac", "")
                val isLaptop = json.optBoolean("is_laptop", false)
                val batteryPercent = if (json.has("battery_percent") && !json.isNull("battery_percent")) {
                    json.optDouble("battery_percent", 0.0).toFloat()
                } else null
                val rawOsType = json.optString("os_type", "").lowercase().trim()
                val osType = when {
                    rawOsType.isNotBlank() && rawOsType != "unknown" -> rawOsType
                    os.contains("windows", true) || hostname.contains("win", true) -> "windows"
                    os.contains("mac", true) || os.contains("darwin", true) || os.contains("apple", true) || hostname.contains("mac", true) -> "macos"
                    os.contains("linux", true) || os.contains("mint", true) || os.contains("ubuntu", true) || os.contains("debian", true) || os.contains("fedora", true) || os.contains("arch", true) -> "linux"
                    else -> "unknown"
                }
                val devId = "$reportedIp:$port"

                val device = DiscoveredDevice(
                    id = devId,
                    hostname = hostname,
                    ip = reportedIp,
                    port = port,
                    os = os,
                    version = version,
                    mac = mac,
                    isLaptop = isLaptop,
                    batteryPercent = batteryPercent,
                    osType = osType,
                    lastSeen = System.currentTimeMillis()
                )

                addOrUpdateDiscoveredDevice(device)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to parse discovery packet: ${e.message}")
        }
    }

    private fun addOrUpdateDiscoveredDevice(device: DiscoveredDevice) {
        val current = _discoveredDevices.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.ip == device.ip && it.port == device.port }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            val resolvedHostname = when {
                device.hostname.isNotBlank() && device.hostname != "Linux Mint PC" && device.hostname != "OmniDesk PC" && device.hostname != "Desktop PC" && device.hostname != "PC" -> device.hostname
                existing.hostname.isNotBlank() && existing.hostname != "Linux Mint PC" && existing.hostname != "OmniDesk PC" && existing.hostname != "Desktop PC" && existing.hostname != "PC" -> existing.hostname
                else -> device.hostname.ifBlank { existing.hostname }
            }
            val resolvedOs = when {
                device.os.isNotBlank() && device.os != "Desktop PC" && device.os != "Linux Mint" && device.os != "Universal" -> device.os
                existing.os.isNotBlank() && existing.os != "Desktop PC" && existing.os != "Linux Mint" && existing.os != "Universal" -> existing.os
                else -> device.os.ifBlank { existing.os }
            }
            val resolvedOsType = when {
                device.osType.isNotBlank() && device.osType != "unknown" -> device.osType
                existing.osType.isNotBlank() && existing.osType != "unknown" -> existing.osType
                resolvedOs.contains("mac", true) || resolvedOs.contains("darwin", true) || resolvedOs.contains("apple", true) || resolvedHostname.contains("mac", true) -> "macos"
                resolvedOs.contains("windows", true) || resolvedHostname.contains("win", true) -> "windows"
                resolvedOs.contains("linux", true) || resolvedOs.contains("mint", true) || resolvedOs.contains("ubuntu", true) || resolvedOs.contains("debian", true) || resolvedOs.contains("fedora", true) || resolvedOs.contains("arch", true) -> "linux"
                else -> "unknown"
            }

            current[existingIndex] = device.copy(
                hostname = resolvedHostname,
                mac = if (device.mac.isNotBlank()) device.mac else existing.mac,
                os = resolvedOs,
                osType = resolvedOsType,
                version = if (device.version.isNotBlank() && device.version != "Universal") device.version else existing.version,
                batteryPercent = device.batteryPercent ?: existing.batteryPercent,
                isLaptop = device.isLaptop || existing.isLaptop,
                lastSeen = System.currentTimeMillis()
            )
        } else {
            current.add(device)
        }
        _discoveredDevices.value = current
    }

    private fun getBroadcastAddresses(): List<InetAddress> {
        val list = mutableListOf<InetAddress>()
        try {
            list.add(InetAddress.getByName("255.255.255.255"))
        } catch (e: Exception) {
            // Ignore
        }
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces != null && interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                for (interfaceAddress in networkInterface.interfaceAddresses) {
                    val broadcast = interfaceAddress.broadcast
                    if (broadcast != null && !list.contains(broadcast)) {
                        list.add(broadcast)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error enumerating subnet broadcasts: ${e.message}")
        }
        return list
    }

    private fun acquireMulticastLock() {
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("linux_remote_multicast_lock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire MulticastLock: ${e.message}")
        }
    }

    fun stopDiscovery() {
        _isScanning.value = false
        stopNsdDiscovery()

        subnetSweepJob?.cancel()
        subnetSweepJob = null

        scanJob?.cancel()
        scanJob = null
        try {
            socket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        socket = null

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        multicastLock = null
    }
}
