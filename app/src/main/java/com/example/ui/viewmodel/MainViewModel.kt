package com.example.ui.viewmodel

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.data.entity.CustomShortcutEntity
import com.example.data.entity.PairedDeviceEntity
import com.example.data.repository.DeviceRepository
import com.example.model.ApplicationItem
import com.example.model.AudioStatus
import com.example.model.ConnectionState
import com.example.model.DiscoveredDevice
import com.example.model.PcStatus
import com.example.model.QrPairingPayload
import com.example.network.DiscoveryManager
import com.example.network.OmniDeskClient
import com.example.network.WakeOnLanManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppTab {
    DASHBOARD,
    TOUCHPAD,
    KEYBOARD,
    MEDIA,
    CONTROLS,
    APPLICATIONS,
    GAMEPAD,
    STATUS,
    PRESENTATION,
    BROWSER,
    CLIPBOARD,
    POWER_TIMER,
    CUSTOM_SHORTCUTS,
    DISCOVERY,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = DeviceRepository(db.pairedDeviceDao())
    private val shortcutDao = db.customShortcutDao()
    val discoveryManager = DiscoveryManager(application)
    val client = OmniDeskClient(viewModelScope)

    val connectionState: StateFlow<ConnectionState> = client.connectionState
    val pcStatus: StateFlow<PcStatus> = client.pcStatus
    val audioStatus: StateFlow<AudioStatus> = client.audioStatus
    val applications: StateFlow<List<ApplicationItem>> = client.applications

    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = discoveryManager.discoveredDevices
    val isScanning: StateFlow<Boolean> = discoveryManager.isScanning
    val localDeviceIp: StateFlow<String> = discoveryManager.localDeviceIp
    val scanStatusMessage: StateFlow<String> = discoveryManager.scanStatusMessage

    val pairedDevices: StateFlow<List<PairedDeviceEntity>> = repository.pairedDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customShortcuts = shortcutDao.getAllShortcuts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Clipboard state
    val pcClipboardText = MutableStateFlow("")

    // Presentation Timer
    val presentationTimerSeconds = MutableStateFlow(0L)
    val isPresentationTimerRunning = MutableStateFlow(false)
    private var presentationJob: Job? = null

    // Power Timer state
    val activePowerTimerMinutes = MutableStateFlow<Int?>(null)

    // UI Navigation State
    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    val preferencesManager = PreferencesManager(application)

    // Remembered State: Last successfully connected Linux host IP & connection properties
    val lastConnectedHostIp = MutableStateFlow(preferencesManager.lastConnectedIp)
    val lastConnectedPort = MutableStateFlow(preferencesManager.lastConnectedPort)
    val lastConnectedName = MutableStateFlow(preferencesManager.lastConnectedName)
    val lastConnectedTimestamp = MutableStateFlow(preferencesManager.lastConnectedTimestamp)
    val isLastConnectedUsb = MutableStateFlow(preferencesManager.isUsbMode)
    val autoReconnectEnabled = MutableStateFlow(preferencesManager.autoReconnectEnabled)

    // Auto-reconnect observable states
    val isAutoReconnecting: StateFlow<Boolean> = client.isAutoReconnecting
    val autoReconnectAttempt: StateFlow<Int> = client.autoReconnectAttempt
    val autoReconnectMaxAttempts: StateFlow<Int> = client.autoReconnectMaxAttempts
    val autoReconnectStatusMessage: StateFlow<String?> = client.autoReconnectStatusMessage

    // User Preferences (Persisted)
    val mouseSensitivity = MutableStateFlow(preferencesManager.mouseSensitivity)
    val scrollSpeed = MutableStateFlow(preferencesManager.scrollSpeed)
    val hapticsEnabled = MutableStateFlow(preferencesManager.hapticsEnabled)
    val tapToClickEnabled = MutableStateFlow(preferencesManager.tapToClickEnabled)
    val singleTapAction = MutableStateFlow(preferencesManager.singleTapAction)
    val naturalScrolling = MutableStateFlow(preferencesManager.naturalScrolling)
    val dragLockActive = MutableStateFlow(false)

    // Pairing dialog state
    val showPairingDialog = MutableStateFlow(false)
    val targetPairingHost = MutableStateFlow("")
    val targetPairingPort = MutableStateFlow(8765)
    val targetPairingName = MutableStateFlow("Desktop PC")
    val pairingError = MutableStateFlow<String?>(null)
    val isPairingSubmitting = MutableStateFlow(false)

    // Manual connect dialog state
    val showManualConnectDialog = MutableStateFlow(false)

    // QR Code Scanner dialog state
    val showQrScanner = MutableStateFlow(false)

    // Wake-on-LAN dialog state
    val showWolDialog = MutableStateFlow(false)
    val targetWolMac = MutableStateFlow("")
    val targetWolName = MutableStateFlow("Desktop PC")

    // Status snackbar / toast message
    val userNotification = MutableStateFlow<String?>(null)

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = application.getSystemService(VibratorManager::class.java)
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(Vibrator::class.java)
        }
    }

    init {
        // 1. Start discovery on local network (mDNS, UDP broadcast & USB probe)
        startDiscovery()

        // 2. State-saving auto-reconnect on app launch:
        // Automatically reconnects to the last successfully connected Linux host IP
        viewModelScope.launch(Dispatchers.IO) {
            delay(350) // Small delay to let network interface & database initialize
            if (preferencesManager.autoReconnectEnabled && preferencesManager.hasSavedConnection()) {
                val savedIp = preferencesManager.lastConnectedIp
                val savedPort = preferencesManager.lastConnectedPort
                val savedName = preferencesManager.lastConnectedName ?: "Desktop PC"
                var savedToken = preferencesManager.lastConnectedToken

                if (!savedIp.isNullOrBlank() && connectionState.value is ConnectionState.Disconnected) {
                    if (savedToken.isNullOrBlank()) {
                        savedToken = repository.getDeviceByIp(savedIp)?.token
                    }
                    android.util.Log.d("MainViewModel", "Auto-reconnecting on app launch to last host: $savedIp:$savedPort")
                    connectToDevice(
                        host = savedIp,
                        port = savedPort,
                        token = savedToken,
                        name = savedName,
                        isAutoReconnect = true
                    )
                }
            } else if (preferencesManager.autoReconnectEnabled) {
                // Fallback to default paired device if no direct IP saved
                delay(250)
                val list = pairedDevices.value
                val defaultDev = list.firstOrNull { it.isDefault } ?: list.firstOrNull()
                if (defaultDev != null && connectionState.value is ConnectionState.Disconnected) {
                    connectToDevice(
                        host = defaultDev.ip,
                        port = defaultDev.port,
                        token = defaultDev.token,
                        name = defaultDev.name,
                        isAutoReconnect = true
                    )
                }
            }
        }

        // 3. Fallback auto-connect observer if paired devices emit later and nothing connected
        viewModelScope.launch(Dispatchers.IO) {
            pairedDevices.collect { list ->
                if (list.isNotEmpty() && connectionState.value is ConnectionState.Disconnected &&
                    !preferencesManager.hasSavedConnection() && preferencesManager.autoReconnectEnabled &&
                    !client.isAutoReconnecting.value
                ) {
                    val defaultDev = list.firstOrNull { it.isDefault } ?: list.first()
                    connectToDevice(
                        host = defaultDev.ip,
                        port = defaultDev.port,
                        token = defaultDev.token,
                        name = defaultDev.name,
                        isAutoReconnect = true
                    )
                }
            }
        }

        // 4. Collect errors from network client to display to the user
        viewModelScope.launch(Dispatchers.Main) {
            client.lastError.collect { err ->
                userNotification.value = err
                if (showPairingDialog.value && pairingError.value == null) {
                    pairingError.value = err
                }
            }
        }

        // 5. State-saving observer: Whenever successfully connected, remember host IP, port, token & name
        viewModelScope.launch(Dispatchers.IO) {
            connectionState.collect { state ->
                if (state is ConnectionState.Connected) {
                    val host = state.host
                    val port = state.port
                    val pcName = state.pcName
                    val token = client.getCurrentToken()
                        ?: pairedDevices.value.firstOrNull { it.ip == host }?.token
                        ?: preferencesManager.lastConnectedToken
                    val now = System.currentTimeMillis()

                    preferencesManager.saveLastSuccessfulConnection(
                        ip = host,
                        port = port,
                        name = pcName,
                        token = token,
                        timestamp = now
                    )
                    lastConnectedHostIp.value = host
                    lastConnectedPort.value = port
                    lastConnectedName.value = pcName
                    lastConnectedTimestamp.value = now
                    isLastConnectedUsb.value = host == "127.0.0.1" || pcName.contains("USB", ignoreCase = true)

                    // Update timestamp on paired device in Room database
                    val matchingDevice = repository.getDeviceByIp(host)
                    if (matchingDevice != null) {
                        repository.updateDevice(matchingDevice.copy(lastConnected = now))
                    }
                } else if (state is ConnectionState.PairingRequired) {
                    // Stale/revoked token detected - clear stale token from preferences and DB
                    preferencesManager.clearSavedToken(state.host)
                    val matchingDevice = repository.getDeviceByIp(state.host)
                    if (matchingDevice != null && matchingDevice.token.isNotBlank()) {
                        repository.updateDevice(matchingDevice.copy(token = ""))
                    }
                }
            }
        }

        // 6. Auto-sync full PC metadata (OS, distro, hostname, MAC) to Room database
        viewModelScope.launch(Dispatchers.IO) {
            pcStatus.collect { status ->
                if (status.ip.isNotBlank()) {
                    val matchingDevice = repository.getDeviceByIp(status.ip)
                    if (matchingDevice != null) {
                        val realName = if (status.hostname.isNotBlank() && status.hostname != "PC") status.hostname else matchingDevice.name
                        val realOs = if (status.distro.isNotBlank() && status.distro != "Universal") "${status.distro} ${status.version}".trim() else matchingDevice.osName
                        val realOsType = if (status.osType.isNotBlank() && status.osType != "unknown") status.osType else matchingDevice.osType
                        val realMac = if (status.mac.isNotBlank()) status.mac else matchingDevice.macOrUid

                        if (realName != matchingDevice.name || realOs != matchingDevice.osName ||
                            realOsType != matchingDevice.osType || realMac != matchingDevice.macOrUid) {
                            repository.updateDevice(
                                matchingDevice.copy(
                                    name = realName,
                                    osName = realOs,
                                    osType = realOsType,
                                    macOrUid = realMac,
                                    lastConnected = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun startDiscovery(forceRestart: Boolean = false) {
        discoveryManager.startDiscovery(viewModelScope, forceRestart = forceRestart)
    }

    fun stopDiscovery() {
        discoveryManager.stopDiscovery()
    }

    private var lastPairRequestTimestamp = 0L
    private var lastPairRequestHost = ""

    fun connectToDevice(
        host: String,
        port: Int,
        token: String? = null,
        name: String = "Desktop PC",
        isAutoReconnect: Boolean = false,
        maxRetries: Int = preferencesManager.autoReconnectMaxRetries
    ) {
        targetPairingHost.value = host
        targetPairingPort.value = port
        targetPairingName.value = name

        client.connect(
            host = host,
            port = port,
            token = token,
            deviceName = Build.MODEL ?: "Android Phone",
            deviceId = "android_${Build.ID}",
            pcName = name,
            isAutoReconnect = isAutoReconnect,
            maxRetries = maxRetries
        )
    }

    fun promptPairing(host: String, port: Int, pcName: String) {
        val now = System.currentTimeMillis()
        targetPairingHost.value = host
        targetPairingPort.value = port
        if (pcName.isNotBlank() && pcName != "OmniDesk PC" && pcName != "Desktop PC") {
            targetPairingName.value = pcName
        } else if (targetPairingName.value.isBlank() || targetPairingName.value == "Desktop PC") {
            targetPairingName.value = pcName
        }
        pairingError.value = null
        showPairingDialog.value = true

        // Prevent duplicate pair_request within 2.5 seconds for the same host
        if (host == lastPairRequestHost && now - lastPairRequestTimestamp < 2500L) {
            return
        }
        lastPairRequestTimestamp = now
        lastPairRequestHost = host

        // Send pair request to fetch/confirm PIN on the server without forcing a new one
        client.requestPairingCode(forceNew = false) { ok, msg ->
            if (!ok) {
                pairingError.value = "Failed to request code from PC: $msg"
            }
        }
    }

    fun requestNewPairingCode() {
        pairingError.value = null
        isPairingSubmitting.value = true
        lastPairRequestTimestamp = System.currentTimeMillis()
        client.requestPairingCode(forceNew = true) { ok, msg ->
            isPairingSubmitting.value = false
            if (!ok) {
                pairingError.value = "Failed to request new code: $msg"
            } else {
                userNotification.value = "New pairing code generated on PC"
            }
        }
    }

    fun forceRePair(host: String, port: Int = 8765, name: String = "Desktop PC") {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesManager.clearSavedToken(host)
            val existing = repository.getDeviceByIp(host)
            if (existing != null) {
                repository.updateDevice(existing.copy(token = ""))
            }
            client.disconnect()
            delay(150)
            connectToDevice(host, port, token = null, name = name)
            promptPairing(host, port, name)
        }
    }

    fun submitPairingPin(pin: String) {
        val cleanPin = pin.filter { it.isLetterOrDigit() }.trim()
        if (cleanPin.length < 4) {
            pairingError.value = "Please enter the full pairing code"
            return
        }
        isPairingSubmitting.value = true
        pairingError.value = null

        client.verifyPairingCode(cleanPin) { success, token, message ->
            isPairingSubmitting.value = false
            if (success && token != null) {
                showPairingDialog.value = false
                vibrateTactile(true)
                // Save to Room database
                val discovered = discoveredDevices.value.firstOrNull { it.ip == targetPairingHost.value }
                val discoveredMac = discovered?.mac ?: ""
                val discoveredOs = discovered?.os ?: "Desktop PC"
                val discoveredOsType = discovered?.osType?.takeIf { it.isNotBlank() && it != "unknown" }
                    ?: when {
                        discoveredOs.contains("mac", true) || targetPairingName.value.contains("mac", true) || targetPairingName.value.contains("apple", true) -> "macos"
                        discoveredOs.contains("windows", true) || targetPairingName.value.contains("win", true) -> "windows"
                        discoveredOs.contains("linux", true) || targetPairingName.value.contains("linux", true) -> "linux"
                        else -> "unknown"
                    }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveDevice(
                        PairedDeviceEntity(
                            name = targetPairingName.value,
                            ip = targetPairingHost.value,
                            port = targetPairingPort.value,
                            token = token,
                            macOrUid = discoveredMac,
                            osName = discoveredOs,
                            osType = discoveredOsType,
                            lastConnected = System.currentTimeMillis(),
                            isDefault = true
                        )
                    )
                }
                userNotification.value = "Paired with ${targetPairingName.value}!"
            } else {
                pairingError.value = message
                vibrateError()
            }
        }
    }

    fun openQrScanner() {
        showQrScanner.value = true
    }

    fun closeQrScanner() {
        showQrScanner.value = false
    }

    fun pairWithQrPayload(payload: QrPairingPayload, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        closeQrScanner()
        targetPairingHost.value = payload.ip
        targetPairingPort.value = payload.port
        targetPairingName.value = payload.hostname

        // If an authorized session token is provided directly in the QR code
        if (!payload.token.isNullOrBlank()) {
            userNotification.value = "Connecting to ${payload.hostname} via QR token..."
            connectToDevice(
                host = payload.ip,
                port = payload.port,
                token = payload.token,
                name = payload.hostname
            )
            onResult(true, "Connecting with authorized token")
            return
        }

        userNotification.value = "Pairing with ${payload.hostname} (${payload.ip})..."

        // Connect first
        client.connect(
            host = payload.ip,
            port = payload.port,
            token = null,
            deviceName = Build.MODEL ?: "Android Phone",
            deviceId = "android_${Build.ID}"
        )

        if (!payload.pin.isNullOrBlank()) {
            viewModelScope.launch {
                // Wait for socket to establish connection before submitting PIN
                var waited = 0
                while (client.connectionState.value !is ConnectionState.Connected &&
                    client.connectionState.value !is ConnectionState.PairingRequired &&
                    waited < 40
                ) {
                    delay(100)
                    waited++
                }

                submitPairingPin(payload.pin)
                onResult(true, "Pairing code verified from QR!")
            }
        } else {
            // QR had IP/Port but no PIN; prompt PIN dialog
            promptPairing(payload.ip, payload.port, payload.hostname)
            onResult(true, "Connected to ${payload.hostname}. Enter PIN from PC.")
        }
    }

    fun disconnect() {
        client.disconnect()
        userNotification.value = "Disconnected"
    }

    fun reconnectToLastHost(isAutoReconnect: Boolean = true) {
        val host = preferencesManager.lastConnectedIp
        if (!host.isNullOrBlank()) {
            val port = preferencesManager.lastConnectedPort
            val token = preferencesManager.lastConnectedToken ?: pairedDevices.value.firstOrNull { it.ip == host }?.token
            val name = preferencesManager.lastConnectedName ?: "Desktop PC"
            connectToDevice(host, port, token, name, isAutoReconnect = isAutoReconnect)
            userNotification.value = "Reconnecting to $host..."
        } else {
            val paired = pairedDevices.value.firstOrNull()
            if (paired != null) {
                connectToDevice(paired.ip, paired.port, paired.token, paired.name, isAutoReconnect = isAutoReconnect)
                userNotification.value = "Connecting to ${paired.name}..."
            } else {
                userNotification.value = "No previous connection saved"
            }
        }
    }

    fun cancelAutoReconnect() {
        client.cancelAutoReconnect()
        userNotification.value = "Auto-reconnect cancelled"
    }

    fun clearSavedHost() {
        preferencesManager.clearLastConnection()
        lastConnectedHostIp.value = null
        lastConnectedName.value = null
        lastConnectedTimestamp.value = 0L
        isLastConnectedUsb.value = false
        userNotification.value = "Remembered host cleared"
    }

    fun setAutoReconnect(enabled: Boolean) {
        preferencesManager.autoReconnectEnabled = enabled
        autoReconnectEnabled.value = enabled
        userNotification.value = if (enabled) "Auto-reconnect enabled" else "Auto-reconnect disabled"
    }

    fun setSensitivity(value: Float) {
        preferencesManager.mouseSensitivity = value
        mouseSensitivity.value = value
    }

    fun setScrollSpeed(value: Float) {
        preferencesManager.scrollSpeed = value
        scrollSpeed.value = value
    }

    fun setHaptics(value: Boolean) {
        preferencesManager.hapticsEnabled = value
        hapticsEnabled.value = value
    }

    fun setTapToClick(enabled: Boolean) {
        preferencesManager.tapToClickEnabled = enabled
        tapToClickEnabled.value = enabled
        userNotification.value = if (enabled) "Tap to click enabled" else "Tap to click disabled"
    }

    fun setSingleTapAction(action: String) {
        val safeAction = if (action.equals("right", ignoreCase = true)) "right" else "left"
        preferencesManager.singleTapAction = safeAction
        singleTapAction.value = safeAction
        userNotification.value = if (safeAction == "right") "1-Finger Tap: Right Click" else "1-Finger Tap: Left Click"
    }

    fun setNaturalScrolling(enabled: Boolean) {
        preferencesManager.naturalScrolling = enabled
        naturalScrolling.value = enabled
        userNotification.value = if (enabled) "Natural scrolling enabled" else "Standard scrolling enabled"
    }

    fun removePairedDevice(device: PairedDeviceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            client.unpairDevice(device.token)
            repository.removeDevice(device)
            if (connectionState.value is ConnectionState.Connected) {
                disconnect()
            }
        }
        userNotification.value = "Device removed"
    }

    // Touchpad & Mouse actions
    fun onMouseMove(dx: Float, dy: Float) {
        client.queueMouseMove(dx, dy, mouseSensitivity.value)
    }

    fun onMouseClick(button: String = "left", double: Boolean = false) {
        vibrateMouseClick(button, double)
        client.sendMouseClick(button, double)
    }

    fun onMouseDown(button: String = "left") {
        vibrateMouseDown()
        client.sendMouseDown(button)
    }

    fun onMouseUp(button: String = "left") {
        vibrateMouseUp()
        client.sendMouseUp(button)
    }

    fun toggleDragLock() {
        val newState = !dragLockActive.value
        dragLockActive.value = newState
        if (newState) {
            vibrateMouseDown()
            client.sendMouseDown("left")
        } else {
            vibrateMouseUp()
            client.sendMouseUp("left")
        }
    }

    fun onMouseScroll(dx: Float, dy: Float) {
        // Silent scrolling without vibration per user requirement
        client.sendMouseScroll(dx * scrollSpeed.value, dy * scrollSpeed.value)
    }

    // Keyboard actions
    fun sendText(text: String) {
        vibrateKeyChar()
        client.sendText(text)
    }

    fun sendKey(keyName: String) {
        vibrateKeyPress(keyName)
        client.sendKeyPress(keyName)
    }

    fun sendShortcut(shortcut: String) {
        vibrateTactile(heavy = true)
        client.sendShortcut(shortcut)
        userNotification.value = "Sent $shortcut"
    }

    // Media actions
    fun setVolume(percent: Int) {
        client.setVolume(percent)
    }

    fun changeVolume(delta: Int) {
        vibrateTactile()
        client.changeVolume(delta)
    }

    fun toggleMedia() {
        vibrateTactile()
        client.mediaToggle()
    }

    fun mediaNext() {
        vibrateTactile()
        client.mediaNext()
    }

    fun mediaPrevious() {
        vibrateTactile()
        client.mediaPrevious()
    }

    fun toggleMute() {
        vibrateTactile()
        client.mediaMuteToggle()
    }

    // System actions
    fun executeSystemAction(action: String) {
        vibrateTactile()
        client.sendSystemAction(action)
        userNotification.value = "Triggered: $action"
    }

    // App launcher
    fun launchApp(appId: String, appName: String) {
        vibrateTactile()
        client.launchApplication(appId)
        userNotification.value = "Launching $appName on PC"
    }

    fun refreshApps() {
        client.fetchApplications()
    }

    // Gamepad
    fun sendGamepadButton(button: String) {
        vibrateTactile()
        client.sendGamepadButton(button)
    }

    fun sendGamepadStick(stick: String, dx: Float, dy: Float) {
        client.sendGamepadAxis(stick, dx, dy)
    }

    // --- High-Fidelity Haptic Feedback Engine ---
    fun vibrateMouseClick(button: String = "left", double: Boolean = false) {
        if (!hapticsEnabled.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = if (double) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                } else if (button.equals("right", ignoreCase = true)) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                } else {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                }
                vibrator?.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (double) {
                    VibrationEffect.createWaveform(longArrayOf(0, 18, 50, 18), intArrayOf(0, 200, 0, 200), -1)
                } else if (button.equals("right", ignoreCase = true)) {
                    VibrationEffect.createOneShot(28, 200)
                } else {
                    VibrationEffect.createOneShot(18, 140)
                }
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (double) 40L else 20L)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun vibrateMouseDown() {
        if (!hapticsEnabled.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(24, 180))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25L)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun vibrateMouseUp() {
        if (!hapticsEnabled.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(12, 90))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(10L)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun vibrateScrollTick() {
        // Disabled per user request (no vibration for scrolling)
    }

    fun vibrateKeyChar() {
        if (!hapticsEnabled.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(12, 85))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(12L)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun vibrateKeyPress(keyName: String) {
        if (!hapticsEnabled.value) return
        val isMajor = keyName.uppercase() in setOf("ENTER", "RETURN", "BACKSPACE", "DELETE", "SPACE", "ESC", "SUPER", "WIN")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = if (isMajor) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                } else {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                }
                vibrator?.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amp = if (isMajor) 150 else 90
                val dur = if (isMajor) 22L else 14L
                vibrator?.vibrate(VibrationEffect.createOneShot(dur, amp))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (isMajor) 20L else 12L)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun vibrateTactile(heavy: Boolean = false) {
        if (!hapticsEnabled.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = if (heavy) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                } else {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                }
                vibrator?.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (heavy) {
                    VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
                } else {
                    VibrationEffect.createOneShot(16, 90)
                }
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (heavy) 35L else 16L)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun vibrateError() {
        if (!hapticsEnabled.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100L)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    // 1. Clipboard Sync
    fun sendTextToPcClipboard(text: String) {
        if (text.isEmpty()) return
        vibrateTactile()
        client.sendClipboardText(text)
        userNotification.value = "Copied to PC clipboard"
    }

    fun fetchPcClipboard() {
        client.fetchClipboardText { text ->
            pcClipboardText.value = text
            userNotification.value = if (text.isNotEmpty()) "Fetched from PC" else "PC clipboard is empty"
        }
    }

    // 2. Presentation Remote
    fun nextSlide() {
        vibrateTactile()
        client.sendKeyPress("PAGE_DOWN")
    }

    fun prevSlide() {
        vibrateTactile()
        client.sendKeyPress("PAGE_UP")
    }

    fun startPresentation() {
        vibrateTactile(heavy = true)
        client.sendKeyPress("F5")
        userNotification.value = "Started presentation (F5)"
        startPresentationTimer()
    }

    fun exitPresentation() {
        vibrateTactile()
        client.sendKeyPress("ESC")
        userNotification.value = "Exited presentation"
    }

    fun toggleBlankScreen() {
        vibrateTactile()
        client.sendKeyPress("b")
    }

    fun startPresentationTimer() {
        if (isPresentationTimerRunning.value) return
        isPresentationTimerRunning.value = true
        presentationJob?.cancel()
        presentationJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive && isPresentationTimerRunning.value) {
                delay(1000)
                presentationTimerSeconds.value += 1
            }
        }
    }

    fun stopPresentationTimer() {
        isPresentationTimerRunning.value = false
        presentationJob?.cancel()
    }

    fun resetPresentationTimer() {
        stopPresentationTimer()
        presentationTimerSeconds.value = 0L
    }

    // 3. Web Browser Remote
    fun browserNewTab() {
        vibrateTactile()
        client.sendShortcut("ctrl+t")
        userNotification.value = "New Tab"
    }

    fun browserCloseTab() {
        vibrateTactile()
        client.sendShortcut("ctrl+w")
        userNotification.value = "Closed Tab"
    }

    fun browserReload() {
        vibrateTactile()
        client.sendKeyPress("F5")
    }

    fun browserReopenTab() {
        vibrateTactile()
        client.sendShortcut("ctrl+shift+t")
    }

    fun browserFullscreen() {
        vibrateTactile()
        client.sendKeyPress("F11")
    }

    fun browserZoomIn() {
        vibrateTactile()
        client.sendShortcut("ctrl++")
    }

    fun browserZoomOut() {
        vibrateTactile()
        client.sendShortcut("ctrl+-")
    }

    fun browserAddressBar() {
        vibrateTactile()
        client.sendShortcut("ctrl+l")
    }

    fun browserBookmark() {
        vibrateTactile()
        client.sendShortcut("ctrl+d")
    }

    fun browserBack() {
        vibrateTactile()
        client.sendShortcut("alt+left")
    }

    fun browserForward() {
        vibrateTactile()
        client.sendShortcut("alt+right")
    }

    // 4. Power Sleep Timer
    fun setPowerTimer(minutes: Int) {
        vibrateTactile(heavy = true)
        client.schedulePower(minutes) { ok ->
            if (ok) {
                activePowerTimerMinutes.value = minutes
                userNotification.value = "PC set to turn off in $minutes min"
            } else {
                userNotification.value = "Failed to schedule timer"
            }
        }
    }

    fun cancelPowerTimer() {
        vibrateTactile()
        client.cancelPower { ok ->
            activePowerTimerMinutes.value = null
            userNotification.value = if (ok) "Scheduled shutdown canceled" else "Canceled"
        }
    }

    // 5. Custom Shortcuts
    fun addCustomShortcut(label: String, keys: String, description: String = "", colorHex: Long = 0xFF2ECC71) {
        viewModelScope.launch(Dispatchers.IO) {
            shortcutDao.insertShortcut(
                CustomShortcutEntity(
                    label = label,
                    keys = keys,
                    description = description,
                    colorHex = colorHex
                )
            )
        }
        userNotification.value = "Shortcut added: $label"
    }

    fun deleteCustomShortcut(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            shortcutDao.deleteById(id)
        }
    }

    // 6. Wake-on-LAN
    fun openWolDialog(mac: String = "", name: String = "Desktop PC") {
        targetWolMac.value = mac.ifEmpty {
            pairedDevices.value.firstOrNull { it.macOrUid.isNotBlank() }?.macOrUid ?: ""
        }
        targetWolName.value = name
        showWolDialog.value = true
    }

    fun saveDeviceMac(deviceId: Int, mac: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMacAddress(deviceId, mac)
            userNotification.value = "Saved MAC address"
        }
    }

    fun wakeDefaultPc() {
        val defaultDevice = pairedDevices.value.firstOrNull { it.isDefault } ?: pairedDevices.value.firstOrNull()
        if (defaultDevice != null && defaultDevice.macOrUid.isNotBlank()) {
            sendWakeOnLan(defaultDevice.macOrUid, defaultDevice.ip)
        } else if (defaultDevice != null) {
            openWolDialog("", defaultDevice.name)
        } else {
            openWolDialog("", "Desktop PC")
        }
    }

    fun wakeDevice(device: PairedDeviceEntity) {
        if (device.macOrUid.isNotBlank()) {
            sendWakeOnLan(device.macOrUid, device.ip)
        } else {
            openWolDialog("", device.name)
        }
    }

    fun sendWakeOnLan(macAddress: String, targetIp: String = "255.255.255.255") {
        viewModelScope.launch(Dispatchers.IO) {
            vibrateTactile(heavy = true)
            val result = WakeOnLanManager.sendMagicPacket(macAddress, targetIp)
            result.fold(
                onSuccess = {
                    userNotification.value = "Magic Packet sent to $macAddress"
                },
                onFailure = { err ->
                    userNotification.value = err.message ?: "Failed to send Wake-on-LAN packet"
                }
            )
        }
    }
}
