package com.example.network

import android.util.Log
import com.example.model.ApplicationItem
import com.example.model.AudioStatus
import com.example.model.ConnectionState
import com.example.model.PcStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Universal WebSocket Client for OmniDesk remote desktop control.
 * Connects seamlessly to Windows, macOS, and Linux companion servers.
 */
class OmniDeskClient(private val scope: CoroutineScope) {
    private val TAG = "OmniDeskClient"

    private val okHttpClient = OkHttpClient.Builder()
        .pingInterval(10, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket infinite read
        .build()

    private var webSocket: WebSocket? = null
    private var currentHost: String = ""
    private var currentPort: Int = 8765
    private var currentToken: String? = null
    private var currentDeviceName: String = "Android Phone"
    private var currentDeviceId: String = UUID.randomUUID().toString()
    private var currentPcName: String = "Desktop PC"

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _pcStatus = MutableStateFlow(PcStatus())
    val pcStatus: StateFlow<PcStatus> = _pcStatus.asStateFlow()

    private val _audioStatus = MutableStateFlow(AudioStatus())
    val audioStatus: StateFlow<AudioStatus> = _audioStatus.asStateFlow()

    private val _applications = MutableStateFlow<List<ApplicationItem>>(emptyList())
    val applications: StateFlow<List<ApplicationItem>> = _applications.asStateFlow()

    private val _lastError = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val lastError = _lastError

    // Pending callbacks correlated by request_id
    private val pendingRequests = ConcurrentHashMap<String, (JSONObject) -> Unit>()
    private var pendingPairRequestCallback: ((Boolean, String) -> Unit)? = null

    // Mouse movement & scroll throttling buffer (125Hz batching)
    private val mouseDeltaX = AtomicLong(0L)
    private val mouseDeltaY = AtomicLong(0L)
    private val scrollDeltaX = AtomicLong(0L)
    private val scrollDeltaY = AtomicLong(0L)
    private var mouseCoalesceJob: Job? = null

    // Automatic reconnect parameters
    private var shouldAutoReconnect = false
    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null

    // Auto-reconnect observable states
    private val _isAutoReconnecting = MutableStateFlow(false)
    val isAutoReconnecting: StateFlow<Boolean> = _isAutoReconnecting.asStateFlow()

    private val _autoReconnectAttempt = MutableStateFlow(0)
    val autoReconnectAttempt: StateFlow<Int> = _autoReconnectAttempt.asStateFlow()

    private val _autoReconnectMaxAttempts = MutableStateFlow(5)
    val autoReconnectMaxAttempts: StateFlow<Int> = _autoReconnectMaxAttempts.asStateFlow()

    private val _autoReconnectStatusMessage = MutableStateFlow<String?>(null)
    val autoReconnectStatusMessage: StateFlow<String?> = _autoReconnectStatusMessage.asStateFlow()

    init {
        startMouseMotionCoalescer()
    }

    private fun startMouseMotionCoalescer() {
        mouseCoalesceJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(8) // ~125Hz high-speed batching interval for ultra-low latency

                // 1. Mouse move coalescing
                val rawX = mouseDeltaX.getAndSet(0L)
                val rawY = mouseDeltaY.getAndSet(0L)
                if (rawX != 0L || rawY != 0L) {
                    val dx = rawX.toDouble() / 1000.0
                    val dy = rawY.toDouble() / 1000.0
                    val ws = webSocket
                    if (ws != null) {
                        ws.send("""{"type":"mouse_move","dx":$dx,"dy":$dy}""")
                    }
                }

                // 2. Mouse scroll coalescing (prevents runaway event floods)
                val rawScrollX = scrollDeltaX.getAndSet(0L)
                val rawScrollY = scrollDeltaY.getAndSet(0L)
                if (rawScrollX != 0L || rawScrollY != 0L) {
                    val sdx = rawScrollX.toDouble() / 1000.0
                    val sdy = rawScrollY.toDouble() / 1000.0
                    val ws = webSocket
                    if (ws != null) {
                        ws.send("""{"type":"mouse_scroll","dx":$sdx,"dy":$sdy}""")
                    }
                }
            }
        }
    }

    fun getCurrentHost(): String = currentHost
    fun getCurrentPort(): Int = currentPort
    fun getCurrentToken(): String? = currentToken

    fun connect(
        host: String,
        port: Int = 8765,
        token: String? = null,
        deviceName: String = "Android Phone",
        deviceId: String = "",
        pcName: String = "Desktop PC",
        isAutoReconnect: Boolean = false,
        maxRetries: Int = 5
    ) {
        disconnect(isReconnecting = false)

        // Sanitize host and extract port if embedded (e.g. "192.168.1.50:8765" or "ws://192.168.1.50")
        val raw = host.trim()
            .removePrefix("ws://")
            .removePrefix("wss://")
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/")

        val resolvedPort = if (raw.contains(":")) {
            raw.substringAfter(":").toIntOrNull() ?: port
        } else {
            port
        }
        val resolvedHost = raw.substringBefore(":")

        currentHost = resolvedHost
        currentPort = resolvedPort
        currentToken = token
        currentDeviceName = deviceName
        currentDeviceId = deviceId.ifEmpty { currentDeviceId }
        currentPcName = if (pcName.isNotBlank()) pcName else "Desktop PC"
        shouldAutoReconnect = true
        reconnectAttempt = 0
        _autoReconnectMaxAttempts.value = maxRetries

        if (isAutoReconnect) {
            _isAutoReconnecting.value = true
            _autoReconnectAttempt.value = 1
            _autoReconnectStatusMessage.value = "Attempt 1 of $maxRetries..."
        } else {
            _isAutoReconnecting.value = false
            _autoReconnectAttempt.value = 0
            _autoReconnectStatusMessage.value = null
        }

        doConnect()
    }

    private fun doConnect() {
        if (currentHost.isBlank()) {
            _connectionState.value = ConnectionState.Disconnected
            _lastError.tryEmit("Host address is empty")
            return
        }

        _connectionState.value = ConnectionState.Connecting(currentHost, currentPort)

        val wsUrl = "ws://$currentHost:$currentPort"
        val request = Request.Builder().url(wsUrl).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected to $currentHost:$currentPort")

                val pendingPairCb = pendingPairRequestCallback
                pendingPairRequestCallback = null

                // If we have a token, authenticate immediately
                if (!currentToken.isNullOrBlank()) {
                    authenticateWithToken(currentToken!!)
                } else {
                    // Need pairing
                    _isAutoReconnecting.value = false
                    _autoReconnectAttempt.value = 0
                    _autoReconnectStatusMessage.value = null
                    _connectionState.value = ConnectionState.PairingRequired(currentHost, currentPort, currentPcName)
                    if (pendingPairCb != null) {
                        requestPairingCode(forceNew = false, callback = pendingPairCb)
                    }
                }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(1000, null)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code $reason")
                handleDisconnection(reason)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                handleDisconnection(t.message ?: "Connection failed")
            }
        })
    }

    private fun handleDisconnection(reason: String) {
        val wasConnected = _connectionState.value is ConnectionState.Connected
        _connectionState.value = ConnectionState.Disconnected

        val pendingPairCb = pendingPairRequestCallback
        pendingPairRequestCallback = null
        pendingPairCb?.invoke(false, "Connection to PC failed: $reason")

        if (_isAutoReconnecting.value) {
            val max = _autoReconnectMaxAttempts.value
            if (shouldAutoReconnect && reconnectAttempt < max) {
                scheduleAutoReconnect()
            } else {
                _isAutoReconnecting.value = false
                _autoReconnectStatusMessage.value = "Auto-reconnect stopped: host unreachable"
                shouldAutoReconnect = false
            }
        } else if (shouldAutoReconnect && wasConnected) {
            scheduleReconnect()
        }
    }

    private fun scheduleAutoReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch(Dispatchers.IO) {
            reconnectAttempt++
            val backoffMs = minOf(12000L, (1000L * (1 shl minOf(reconnectAttempt - 1, 3)))) // 1s, 2s, 4s, 8s, 12s
            val nextAttempt = reconnectAttempt + 1
            val max = _autoReconnectMaxAttempts.value
            _autoReconnectAttempt.value = reconnectAttempt
            _autoReconnectStatusMessage.value = "Retrying in ${backoffMs / 1000}s (Attempt $nextAttempt of $max)..."
            Log.d(TAG, "Auto-reconnecting in ${backoffMs}ms (attempt $nextAttempt of $max)...")
            delay(backoffMs)
            if (shouldAutoReconnect && _connectionState.value is ConnectionState.Disconnected && _isAutoReconnecting.value) {
                _autoReconnectAttempt.value = nextAttempt
                _autoReconnectStatusMessage.value = "Connecting (Attempt $nextAttempt of $max)..."
                doConnect()
            }
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch(Dispatchers.IO) {
            reconnectAttempt++
            val backoffMs = minOf(30000L, (1000L * (1 shl minOf(reconnectAttempt, 5))))
            Log.d(TAG, "Reconnecting in ${backoffMs}ms (attempt $reconnectAttempt)...")
            delay(backoffMs)
            if (shouldAutoReconnect && _connectionState.value is ConnectionState.Disconnected) {
                doConnect()
            }
        }
    }

    fun cancelAutoReconnect() {
        shouldAutoReconnect = false
        reconnectJob?.cancel()
        _isAutoReconnecting.value = false
        _autoReconnectAttempt.value = 0
        _autoReconnectStatusMessage.value = null
        if (_connectionState.value is ConnectionState.Connecting) {
            try {
                webSocket?.cancel()
            } catch (e: Exception) {
                // Ignore
            }
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun disconnect(isReconnecting: Boolean = false) {
        if (!isReconnecting) {
            shouldAutoReconnect = false
            _isAutoReconnecting.value = false
            _autoReconnectAttempt.value = 0
            _autoReconnectStatusMessage.value = null
        }
        reconnectJob?.cancel()
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (e: Exception) {
            // Ignore
        }
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    private fun authenticateWithToken(token: String) {
        val reqId = UUID.randomUUID().toString()
        val json = JSONObject().apply {
            put("type", "auth")
            put("token", token)
            put("device_id", currentDeviceId)
            put("device_name", currentDeviceName)
            put("request_id", reqId)
        }
        sendJson(json)
    }

    fun requestPairingCode(forceNew: Boolean = false, callback: (Boolean, String) -> Unit) {
        val ws = webSocket
        if (ws == null || _connectionState.value is ConnectionState.Connecting) {
            pendingPairRequestCallback = callback
            if (ws == null && currentHost.isNotBlank()) {
                doConnect()
            }
            return
        }

        val reqId = UUID.randomUUID().toString()
        pendingRequests[reqId] = { res ->
            val ok = res.optBoolean("success", false)
            val msg = res.optString("message", "Pairing initiated")
            callback(ok, msg)
        }
        val json = JSONObject().apply {
            put("type", "pair_request")
            put("device_id", currentDeviceId)
            put("device_name", currentDeviceName)
            put("force_new", forceNew)
            put("request_id", reqId)
        }
        sendJson(json)
    }

    fun verifyPairingCode(pin: String, callback: (Boolean, String?, String) -> Unit) {
        val cleanPin = pin.filter { it.isLetterOrDigit() }.trim()
        if (cleanPin.isEmpty()) {
            callback(false, null, "Please enter the pairing code")
            return
        }

        val ws = webSocket
        if (ws == null || _connectionState.value !is ConnectionState.PairingRequired) {
            if (currentHost.isNotBlank()) {
                scope.launch(Dispatchers.IO) {
                    if (webSocket == null) {
                        doConnect()
                        var waited = 0
                        while (webSocket == null && waited < 30) {
                            delay(100)
                            waited++
                        }
                    }
                    val currentWs = webSocket
                    if (currentWs == null) {
                        callback(false, null, "Cannot connect to PC ($currentHost:$currentPort). Ensure server is running.")
                        return@launch
                    }
                    sendPairVerify(cleanPin, callback)
                }
                return
            } else {
                callback(false, null, "Not connected to PC")
                return
            }
        }

        sendPairVerify(cleanPin, callback)
    }

    private fun sendPairVerify(cleanPin: String, callback: (Boolean, String?, String) -> Unit) {
        val reqId = UUID.randomUUID().toString()
        pendingRequests[reqId] = { res ->
            val ok = res.optBoolean("success", false)
            val token = res.optString("token", null)
            val errorMsg = res.optString("error", "Pairing verification failed")
            if (ok && !token.isNullOrBlank()) {
                currentToken = token
                _isAutoReconnecting.value = false
                _autoReconnectAttempt.value = 0
                _autoReconnectStatusMessage.value = null
                reconnectAttempt = 0
                _connectionState.value = ConnectionState.Connected(
                    host = currentHost,
                    port = currentPort,
                    pcName = currentPcName.ifBlank { "OmniDesk PC" },
                    osInfo = "Desktop PC"
                )
                fetchStatus()
                fetchApplications()
                callback(true, token, "Paired successfully")
            } else {
                callback(false, null, errorMsg)
            }
        }
        val json = JSONObject().apply {
            put("type", "pair_verify")
            put("pin", cleanPin)
            put("device_id", currentDeviceId)
            put("device_name", currentDeviceName)
            put("request_id", reqId)
        }
        sendJson(json)
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")
            val reqId = json.optString("request_id")

            if (reqId.isNotEmpty() && pendingRequests.containsKey(reqId)) {
                pendingRequests.remove(reqId)?.invoke(json)
            }

            when (type) {
                "auth_result" -> {
                    val success = json.optBoolean("success", false)
                    if (success) {
                        val pcInfo = json.optJSONObject("pc_info")
                        val hostName = pcInfo?.optString("hostname") ?: "OmniDesk PC"
                        val distro = pcInfo?.optString("distro") ?: "Universal"
                        val version = pcInfo?.optString("version") ?: ""

                        _isAutoReconnecting.value = false
                        _autoReconnectAttempt.value = 0
                        _autoReconnectStatusMessage.value = null
                        reconnectAttempt = 0

                        _connectionState.value = ConnectionState.Connected(
                            host = currentHost,
                            port = currentPort,
                            pcName = hostName,
                            osInfo = "$distro $version".trim()
                        )
                        if (pcInfo != null) {
                            parseStatusObject(pcInfo)
                        }
                        fetchApplications()
                    } else {
                        val errMsg = json.optString("error", "Authentication failed")
                        // Clear the stale revoked token so subsequent attempts can pair fresh
                        currentToken = null
                        _connectionState.value = ConnectionState.PairingRequired(currentHost, currentPort, "OmniDesk PC")
                        _lastError.tryEmit("PC requires pairing: $errMsg")
                    }
                }

                "telemetry", "get_status_result" -> {
                    val statusObj = json.optJSONObject("status")
                    if (statusObj != null) {
                        parseStatusObject(statusObj)
                    }
                    val audioObj = json.optJSONObject("audio")
                    if (audioObj != null) {
                        _audioStatus.value = AudioStatus(
                            volume = audioObj.optInt("volume", _audioStatus.value.volume),
                            muted = audioObj.optBoolean("muted", _audioStatus.value.muted)
                        )
                    }
                }

                "get_applications_result" -> {
                    val appsArray = json.optJSONArray("applications")
                    if (appsArray != null) {
                        val list = mutableListOf<ApplicationItem>()
                        for (i in 0 until appsArray.length()) {
                            val item = appsArray.getJSONObject(i)
                            list.add(
                                ApplicationItem(
                                    id = item.optString("id"),
                                    name = item.optString("name"),
                                    icon = item.optString("icon", "application-x-executable"),
                                    exec = item.optString("exec")
                                )
                            )
                        }
                        _applications.value = list
                    }
                }

                "volume_set_result", "volume_delta_result", "media_mute_result" -> {
                    val vol = json.optInt("volume", -1)
                    val muted = json.optBoolean("muted", false)
                    if (vol >= 0) {
                        _audioStatus.value = AudioStatus(volume = vol, muted = muted)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing incoming WebSocket text: ${e.message}")
        }
    }

    private fun parseStatusObject(statusObj: JSONObject) {
        val hasBattery = statusObj.optBoolean("has_battery", statusObj.has("battery_percent") && !statusObj.isNull("battery_percent"))
        val batteryPercent = if (statusObj.has("battery_percent") && !statusObj.isNull("battery_percent")) {
            statusObj.optDouble("battery_percent", 0.0).toFloat()
        } else {
            null
        }
        val isCharging = statusObj.optBoolean("is_charging", false)
        val isLaptop = statusObj.optBoolean("is_laptop", hasBattery)
        val rawOsType = statusObj.optString("os_type", "").trim().lowercase()
        val distro = statusObj.optString("distro", "")
        val h = statusObj.optString("hostname", "PC")
        val ver = statusObj.optString("version", "")

        val osType = if (rawOsType.isNotBlank() && rawOsType != "unknown") {
            rawOsType
        } else {
            val d = (distro + " " + h).lowercase()
            when {
                d.contains("windows") || d.contains("win1") || d.contains("win7") -> "windows"
                d.contains("mac") || d.contains("darwin") || d.contains("apple") || d.contains("osx") -> "macos"
                d.contains("linux") || d.contains("ubuntu") || d.contains("mint") || d.contains("debian") || d.contains("fedora") || d.contains("arch") -> "linux"
                else -> "unknown"
            }
        }

        val finalDistro = when {
            distro.isNotBlank() && distro != "Desktop PC" && distro != "Universal" -> distro
            osType == "windows" -> "Windows"
            osType == "macos" -> "macOS"
            osType == "linux" -> "Linux"
            else -> "Desktop PC"
        }

        _pcStatus.value = PcStatus(
            hostname = h,
            distro = finalDistro,
            version = ver,
            ip = statusObj.optString("ip", currentHost),
            mac = statusObj.optString("mac", ""),
            cpuPercent = statusObj.optDouble("cpu_percent", 0.0).toFloat(),
            ramPercent = statusObj.optDouble("ram_percent", 0.0).toFloat(),
            ramUsedGb = statusObj.optDouble("ram_used_gb", 0.0).toFloat(),
            ramTotalGb = statusObj.optDouble("ram_total_gb", 0.0).toFloat(),
            diskPercent = statusObj.optDouble("disk_percent", 0.0).toFloat(),
            diskUsedGb = statusObj.optDouble("disk_used_gb", 0.0).toFloat(),
            diskTotalGb = statusObj.optDouble("disk_total_gb", 0.0).toFloat(),
            cpuTemp = statusObj.optDouble("cpu_temp", 0.0).toFloat(),
            uptimeSeconds = statusObj.optLong("uptime_seconds", 0L),
            hasBattery = hasBattery,
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            isLaptop = isLaptop,
            osType = osType,
            timestamp = System.currentTimeMillis()
        )

        val currentState = _connectionState.value
        if (currentState is ConnectionState.Connected) {
            val osStr = if (finalDistro.isNotBlank()) "$finalDistro $ver".trim() else currentState.osInfo
            val realPcName = if (h.isNotBlank() && h != "PC") h else currentState.pcName
            if (realPcName != currentState.pcName || osStr != currentState.osInfo) {
                _connectionState.value = currentState.copy(
                    pcName = realPcName,
                    osInfo = osStr
                )
            }
        }
    }

    // Touchpad / Mouse commands
    fun queueMouseMove(dx: Float, dy: Float, sensitivity: Float) {
        val scaledDx = (dx * sensitivity * 1000f).toLong()
        val scaledDy = (dy * sensitivity * 1000f).toLong()
        mouseDeltaX.addAndGet(scaledDx)
        mouseDeltaY.addAndGet(scaledDy)
    }

    fun sendMouseClick(button: String = "left", double: Boolean = false) {
        sendJson(
            JSONObject().apply {
                put("type", "mouse_click")
                put("button", button)
                put("double", double)
            }
        )
    }

    fun sendMouseDown(button: String = "left") {
        sendJson(
            JSONObject().apply {
                put("type", "mouse_down")
                put("button", button)
            }
        )
    }

    fun sendMouseUp(button: String = "left") {
        sendJson(
            JSONObject().apply {
                put("type", "mouse_up")
                put("button", button)
            }
        )
    }

    fun sendMouseScroll(dx: Float, dy: Float) {
        val scaledDx = (dx * 1000f).toLong()
        val scaledDy = (dy * 1000f).toLong()
        scrollDeltaX.addAndGet(scaledDx)
        scrollDeltaY.addAndGet(scaledDy)
    }

    // Keyboard commands
    fun sendText(text: String) {
        if (text.isEmpty()) return
        sendJson(
            JSONObject().apply {
                put("type", "text_type")
                put("text", text)
            }
        )
    }

    fun sendKeyPress(key: String) {
        sendJson(
            JSONObject().apply {
                put("type", "key_press")
                put("key", key)
            }
        )
    }

    fun sendShortcut(shortcut: String) {
        sendJson(
            JSONObject().apply {
                put("type", "shortcut")
                put("shortcut", shortcut)
            }
        )
    }

    // Media commands
    fun setVolume(percent: Int) {
        sendJson(
            JSONObject().apply {
                put("type", "volume_set")
                put("value", percent)
            }
        )
    }

    fun changeVolume(delta: Int) {
        sendJson(
            JSONObject().apply {
                put("type", "volume_delta")
                put("delta", delta)
            }
        )
    }

    fun mediaToggle() = sendJson(JSONObject().put("type", "media_toggle"))
    fun mediaPlay() = sendJson(JSONObject().put("type", "media_play"))
    fun mediaPause() = sendJson(JSONObject().put("type", "media_pause"))
    fun mediaNext() = sendJson(JSONObject().put("type", "media_next"))
    fun mediaPrevious() = sendJson(JSONObject().put("type", "media_previous"))
    fun mediaMuteToggle() = sendJson(JSONObject().put("type", "media_mute"))

    // PC System controls
    fun sendSystemAction(action: String) {
        sendJson(
            JSONObject().apply {
                put("type", "system_action")
                put("action", action)
                put("request_id", UUID.randomUUID().toString())
            }
        )
    }

    // Application Launcher
    fun fetchApplications() {
        sendJson(
            JSONObject().apply {
                put("type", "get_applications")
                put("request_id", UUID.randomUUID().toString())
            }
        )
    }

    fun launchApplication(appId: String) {
        sendJson(
            JSONObject().apply {
                put("type", "launch_application")
                put("app_id", appId)
                put("request_id", UUID.randomUUID().toString())
            }
        )
    }

    // Telemetry
    fun fetchStatus() {
        sendJson(
            JSONObject().apply {
                put("type", "get_status")
                put("request_id", UUID.randomUUID().toString())
            }
        )
    }

    // Gamepad
    fun sendGamepadButton(button: String) {
        sendJson(
            JSONObject().apply {
                put("type", "gamepad_input")
                put("button", button)
            }
        )
    }

    fun sendGamepadAxis(axisName: String, dx: Float, dy: Float) {
        sendJson(
            JSONObject().apply {
                put("type", "gamepad_input")
                put("axis", axisName)
                put("dx", dx)
                put("dy", dy)
            }
        )
    }

    // Clipboard Commands
    fun sendClipboardText(text: String) {
        sendJson(
            JSONObject().apply {
                put("type", "clipboard_set")
                put("text", text)
                put("request_id", UUID.randomUUID().toString())
            }
        )
    }

    fun fetchClipboardText(callback: (String) -> Unit) {
        val reqId = UUID.randomUUID().toString()
        pendingRequests[reqId] = { res ->
            val text = res.optString("text", "")
            callback(text)
        }
        sendJson(
            JSONObject().apply {
                put("type", "clipboard_get")
                put("request_id", reqId)
            }
        )
    }

    // Power Timer Commands
    fun schedulePower(minutes: Int, callback: ((Boolean) -> Unit)? = null) {
        val reqId = UUID.randomUUID().toString()
        if (callback != null) {
            pendingRequests[reqId] = { res ->
                val ok = res.optBoolean("scheduled", res.optBoolean("success", false))
                callback(ok)
            }
        }
        sendJson(
            JSONObject().apply {
                put("type", "schedule_power")
                put("minutes", minutes)
                put("request_id", reqId)
            }
        )
    }

    fun cancelPower(callback: ((Boolean) -> Unit)? = null) {
        val reqId = UUID.randomUUID().toString()
        if (callback != null) {
            pendingRequests[reqId] = { res ->
                val ok = res.optBoolean("canceled", res.optBoolean("success", false))
                callback(ok)
            }
        }
        sendJson(
            JSONObject().apply {
                put("type", "cancel_power")
                put("request_id", reqId)
            }
        )
    }

    // Device Revocation
    fun unpairDevice(token: String) {
        sendJson(
            JSONObject().apply {
                put("type", "unpair")
                put("token", token)
            }
        )
    }

    private fun sendJson(json: JSONObject) {
        val ws = webSocket
        if (ws != null) {
            ws.send(json.toString())
        }
    }
}
