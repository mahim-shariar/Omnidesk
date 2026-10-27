package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ConnectionState
import com.example.ui.theme.DarkBorderShine
import com.example.ui.theme.MintGreenPrimary
import com.example.ui.components.ManualConnectDialog
import com.example.ui.components.PairingDialog
import com.example.ui.components.TopStatusBar
import com.example.ui.screens.AppLauncherScreen
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.ClipboardScreen
import com.example.ui.screens.CustomShortcutsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiscoveryScreen
import com.example.ui.screens.GamepadScreen
import com.example.ui.screens.KeyboardScreen
import com.example.ui.screens.MediaScreen
import com.example.ui.screens.PcControlsScreen
import com.example.ui.screens.PcStatusScreen
import com.example.ui.screens.PowerTimerScreen
import com.example.ui.screens.PresentationScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TouchpadScreen
import com.example.ui.theme.OmniDeskTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OmniDeskTheme(darkTheme = true) {
                val viewModel: MainViewModel = viewModel()
                OmniDeskApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun OmniDeskApp(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val pcStatus by viewModel.pcStatus.collectAsStateWithLifecycle()
    val audioStatus by viewModel.audioStatus.collectAsStateWithLifecycle()
    val applications by viewModel.applications.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()

    val customShortcuts by viewModel.customShortcuts.collectAsStateWithLifecycle()
    val pcClipboardText by viewModel.pcClipboardText.collectAsStateWithLifecycle()
    val presentationTimerSeconds by viewModel.presentationTimerSeconds.collectAsStateWithLifecycle()
    val isPresentationTimerRunning by viewModel.isPresentationTimerRunning.collectAsStateWithLifecycle()
    val activePowerTimerMinutes by viewModel.activePowerTimerMinutes.collectAsStateWithLifecycle()

    val mouseSensitivity by viewModel.mouseSensitivity.collectAsStateWithLifecycle()
    val scrollSpeed by viewModel.scrollSpeed.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val tapToClickEnabled by viewModel.tapToClickEnabled.collectAsStateWithLifecycle()
    val singleTapAction by viewModel.singleTapAction.collectAsStateWithLifecycle()
    val naturalScrolling by viewModel.naturalScrolling.collectAsStateWithLifecycle()
    val dragLockActive by viewModel.dragLockActive.collectAsStateWithLifecycle()

    val lastConnectedHostIp by viewModel.lastConnectedHostIp.collectAsStateWithLifecycle()
    val lastConnectedPort by viewModel.lastConnectedPort.collectAsStateWithLifecycle()
    val lastConnectedName by viewModel.lastConnectedName.collectAsStateWithLifecycle()
    val lastConnectedTimestamp by viewModel.lastConnectedTimestamp.collectAsStateWithLifecycle()
    val autoReconnectEnabled by viewModel.autoReconnectEnabled.collectAsStateWithLifecycle()
    val isAutoReconnecting by viewModel.isAutoReconnecting.collectAsStateWithLifecycle()
    val autoReconnectStatusMessage by viewModel.autoReconnectStatusMessage.collectAsStateWithLifecycle()

    val showPairingDialog by viewModel.showPairingDialog.collectAsStateWithLifecycle()
    val targetPairingName by viewModel.targetPairingName.collectAsStateWithLifecycle()
    val pairingError by viewModel.pairingError.collectAsStateWithLifecycle()
    val isPairingSubmitting by viewModel.isPairingSubmitting.collectAsStateWithLifecycle()

    val showManualConnectDialog by viewModel.showManualConnectDialog.collectAsStateWithLifecycle()
    val showQrScanner by viewModel.showQrScanner.collectAsStateWithLifecycle()
    val showWolDialog by viewModel.showWolDialog.collectAsStateWithLifecycle()
    val targetWolMac by viewModel.targetWolMac.collectAsStateWithLifecycle()
    val targetWolName by viewModel.targetWolName.collectAsStateWithLifecycle()
    val userNotification by viewModel.userNotification.collectAsStateWithLifecycle()
    val localDeviceIp by viewModel.localDeviceIp.collectAsStateWithLifecycle()
    val scanStatusMessage by viewModel.scanStatusMessage.collectAsStateWithLifecycle()

    var showSetupGuideDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Intercept Back button when not on dashboard
    BackHandler(enabled = currentTab != AppTab.DASHBOARD) {
        viewModel.setTab(AppTab.DASHBOARD)
    }

    // React to notifications from ViewModel
    LaunchedEffect(userNotification) {
        userNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.userNotification.value = null
        }
    }

    // Auto-prompt pairing dialog if server requests pairing
    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.PairingRequired) {
            val req = connectionState as ConnectionState.PairingRequired
            viewModel.promptPairing(req.host, req.port, req.deviceName)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentTab != AppTab.GAMEPAD) {
                TopStatusBar(
                    connectionState = connectionState,
                    isPaired = pairedDevices.isNotEmpty(),
                    pairedDeviceName = pairedDevices.firstOrNull()?.name,
                    rememberedHostIp = lastConnectedHostIp,
                    isAutoReconnecting = isAutoReconnecting,
                    autoReconnectStatus = autoReconnectStatusMessage,
                    title = when (currentTab) {
                        AppTab.DASHBOARD -> "OmniDesk"
                        AppTab.TOUCHPAD -> "Touchpad Remote"
                        AppTab.KEYBOARD -> "PC Keyboard"
                        AppTab.MEDIA -> "Media Controller"
                        AppTab.CONTROLS -> "PC System Controls"
                        AppTab.APPLICATIONS -> "Applications"
                        AppTab.GAMEPAD -> "Virtual Gamepad"
                        AppTab.STATUS -> "PC Status"
                        AppTab.PRESENTATION -> "Slides Remote"
                        AppTab.BROWSER -> "Browser Remote"
                        AppTab.CLIPBOARD -> "Clipboard Sync"
                        AppTab.POWER_TIMER -> "Sleep & Power Timer"
                        AppTab.CUSTOM_SHORTCUTS -> "Custom Shortcuts"
                        AppTab.DISCOVERY -> "PC Discovery"
                        AppTab.SETTINGS -> "Settings"
                    },
                    canNavigateBack = currentTab != AppTab.DASHBOARD,
                    onNavigateBack = { viewModel.setTab(AppTab.DASHBOARD) },
                    onReconnect = { viewModel.reconnectToLastHost() },
                    onOpenDiscovery = { viewModel.setTab(AppTab.DISCOVERY) }
                )
            }
        },
        bottomBar = {
            if (currentTab != AppTab.GAMEPAD) {
                // Keep bottom nav visible across primary tabs
                NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                DarkBorderShine,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .testTag("main_bottom_nav")
            ) {
                val itemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MintGreenPrimary,
                    selectedTextColor = MintGreenPrimary,
                    indicatorColor = MintGreenPrimary.copy(alpha = 0.16f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.DASHBOARD,
                    onClick = { viewModel.setTab(AppTab.DASHBOARD) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Home") },
                    label = { Text("Home", fontWeight = if (currentTab == AppTab.DASHBOARD) FontWeight.Bold else FontWeight.Normal) },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.TOUCHPAD,
                    onClick = { viewModel.setTab(AppTab.TOUCHPAD) },
                    icon = { Icon(Icons.Default.Mouse, contentDescription = "Touchpad") },
                    label = { Text("Mouse", fontWeight = if (currentTab == AppTab.TOUCHPAD) FontWeight.Bold else FontWeight.Normal) },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_touchpad")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.KEYBOARD,
                    onClick = { viewModel.setTab(AppTab.KEYBOARD) },
                    icon = { Icon(Icons.Default.Keyboard, contentDescription = "Keyboard") },
                    label = { Text("Keyboard", fontWeight = if (currentTab == AppTab.KEYBOARD) FontWeight.Bold else FontWeight.Normal) },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_keyboard")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.MEDIA,
                    onClick = { viewModel.setTab(AppTab.MEDIA) },
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = "Media") },
                    label = { Text("Media", fontWeight = if (currentTab == AppTab.MEDIA) FontWeight.Bold else FontWeight.Normal) },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_media")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.CONTROLS,
                    onClick = { viewModel.setTab(AppTab.CONTROLS) },
                    icon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = "Controls") },
                    label = { Text("Controls", fontWeight = if (currentTab == AppTab.CONTROLS) FontWeight.Bold else FontWeight.Normal) },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_controls")
                )
            }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TabTransition"
            ) { tab ->
                when (tab) {
                    AppTab.DASHBOARD -> DashboardScreen(
                        connectionState = connectionState,
                        pcStatus = pcStatus,
                        pairedDevices = pairedDevices,
                        rememberedHostIp = lastConnectedHostIp,
                        rememberedHostPort = lastConnectedPort,
                        rememberedHostName = lastConnectedName,
                        rememberedTimestamp = lastConnectedTimestamp,
                        isAutoReconnecting = isAutoReconnecting,
                        autoReconnectStatus = autoReconnectStatusMessage,
                        onNavigateTab = { viewModel.setTab(it) },
                        onOpenDiscovery = { viewModel.setTab(AppTab.DISCOVERY) },
                        onDisconnect = { viewModel.disconnect() },
                        onReconnect = { dev ->
                            viewModel.connectToDevice(dev.ip, dev.port, dev.token, dev.name)
                        },
                        onReconnectLastHost = { viewModel.reconnectToLastHost() },
                        onCancelAutoReconnect = { viewModel.cancelAutoReconnect() },
                        onClearSavedHost = { viewModel.clearSavedHost() },
                        onOpenWakeOnLan = { viewModel.openWolDialog() },
                        onConnectUsb = {
                            val existing = pairedDevices.firstOrNull { it.ip == "127.0.0.1" }
                            if (existing != null) {
                                viewModel.connectToDevice("127.0.0.1", 8765, existing.token, "PC (USB Cable)")
                            } else {
                                viewModel.connectToDevice("127.0.0.1", 8765, null, "PC (USB Cable)")
                                viewModel.promptPairing("127.0.0.1", 8765, "PC (USB Cable)")
                            }
                        },
                        onScanQrClick = { viewModel.openQrScanner() }
                    )

                    AppTab.TOUCHPAD -> TouchpadScreen(
                        sensitivity = mouseSensitivity,
                        onSensitivityChange = { viewModel.setSensitivity(it) },
                        scrollSpeed = scrollSpeed,
                        onScrollSpeedChange = { viewModel.setScrollSpeed(it) },
                        tapToClickEnabled = tapToClickEnabled,
                        onTapToClickChange = { viewModel.setTapToClick(it) },
                        singleTapAction = singleTapAction,
                        onSingleTapActionChange = { viewModel.setSingleTapAction(it) },
                        dragLockActive = dragLockActive,
                        onToggleDragLock = { viewModel.toggleDragLock() },
                        naturalScrolling = naturalScrolling,
                        onNaturalScrollingChange = { viewModel.setNaturalScrolling(it) },
                        onMouseMove = { dx, dy -> viewModel.onMouseMove(dx, dy) },
                        onMouseClick = { btn, dbl -> viewModel.onMouseClick(btn, dbl) },
                        onMouseDown = { btn -> viewModel.onMouseDown(btn) },
                        onMouseUp = { btn -> viewModel.onMouseUp(btn) },
                        onMouseScroll = { dx, dy -> viewModel.onMouseScroll(dx, dy) }
                    )

                    AppTab.KEYBOARD -> KeyboardScreen(
                        onSendText = { viewModel.sendText(it) },
                        onSendKey = { viewModel.sendKey(it) },
                        onSendShortcut = { viewModel.sendShortcut(it) },
                        pcStatus = pcStatus
                    )

                    AppTab.MEDIA -> MediaScreen(
                        audioStatus = audioStatus,
                        onSetVolume = { viewModel.setVolume(it) },
                        onAdjustVolume = { viewModel.changeVolume(it) },
                        onToggleMute = { viewModel.toggleMute() },
                        onTogglePlayPause = { viewModel.toggleMedia() },
                        onNext = { viewModel.mediaNext() },
                        onPrevious = { viewModel.mediaPrevious() }
                    )

                    AppTab.CONTROLS -> PcControlsScreen(
                        onExecuteAction = { viewModel.executeSystemAction(it) },
                        onWakeOnLanClick = { viewModel.openWolDialog() },
                        pcStatus = pcStatus
                    )

                    AppTab.APPLICATIONS -> AppLauncherScreen(
                        applications = applications,
                        onLaunchApp = { id, name -> viewModel.launchApp(id, name) },
                        onRefresh = { viewModel.refreshApps() },
                        pcStatus = pcStatus
                    )

                    AppTab.GAMEPAD -> GamepadScreen(
                        onButtonPress = { viewModel.sendGamepadButton(it) },
                        onStickMove = { stick, dx, dy -> viewModel.sendGamepadStick(stick, dx, dy) },
                        onExit = { viewModel.setTab(AppTab.DASHBOARD) }
                    )

                    AppTab.STATUS -> PcStatusScreen(
                        pcStatus = pcStatus,
                        onRefresh = { viewModel.client.fetchStatus() }
                    )

                    AppTab.PRESENTATION -> PresentationScreen(
                        timerSeconds = presentationTimerSeconds,
                        isTimerRunning = isPresentationTimerRunning,
                        onStartTimer = { viewModel.startPresentationTimer() },
                        onStopTimer = { viewModel.stopPresentationTimer() },
                        onResetTimer = { viewModel.resetPresentationTimer() },
                        onNextSlide = { viewModel.nextSlide() },
                        onPrevSlide = { viewModel.prevSlide() },
                        onStartPresentation = { viewModel.startPresentation() },
                        onExitPresentation = { viewModel.exitPresentation() },
                        onToggleBlankScreen = { viewModel.toggleBlankScreen() }
                    )

                    AppTab.BROWSER -> BrowserScreen(
                        onNewTab = { viewModel.browserNewTab() },
                        onCloseTab = { viewModel.browserCloseTab() },
                        onReopenTab = { viewModel.browserReopenTab() },
                        onReload = { viewModel.browserReload() },
                        onFullscreen = { viewModel.browserFullscreen() },
                        onZoomIn = { viewModel.browserZoomIn() },
                        onZoomOut = { viewModel.browserZoomOut() },
                        onAddressBar = { viewModel.browserAddressBar() },
                        onBookmark = { viewModel.browserBookmark() },
                        onBack = { viewModel.browserBack() },
                        onForward = { viewModel.browserForward() },
                        onShortcut = { viewModel.sendShortcut(it) }
                    )

                    AppTab.CLIPBOARD -> ClipboardScreen(
                        pcClipboardText = pcClipboardText,
                        onSendTextToPc = { viewModel.sendTextToPcClipboard(it) },
                        onFetchPcClipboard = { viewModel.fetchPcClipboard() }
                    )

                    AppTab.POWER_TIMER -> PowerTimerScreen(
                        activeTimerMinutes = activePowerTimerMinutes,
                        onSetTimer = { viewModel.setPowerTimer(it) },
                        onCancelTimer = { viewModel.cancelPowerTimer() }
                    )

                    AppTab.CUSTOM_SHORTCUTS -> CustomShortcutsScreen(
                        shortcuts = customShortcuts,
                        onTriggerShortcut = { viewModel.sendShortcut(it) },
                        onAddShortcut = { label, keys, desc, color ->
                            viewModel.addCustomShortcut(label, keys, desc, color)
                        },
                        onDeleteShortcut = { viewModel.deleteCustomShortcut(it) }
                    )

                    AppTab.DISCOVERY -> DiscoveryScreen(
                        isScanning = isScanning,
                        discoveredDevices = discoveredDevices,
                        pairedDevices = pairedDevices,
                        connectionState = connectionState,
                        onStartScan = { viewModel.startDiscovery(forceRestart = true) },
                        onConnectDiscovered = { dev ->
                            val existing = pairedDevices.firstOrNull { it.ip == dev.ip }
                            viewModel.connectToDevice(dev.ip, dev.port, existing?.token, dev.hostname)
                        },
                        onForcePairDiscovered = { dev ->
                            viewModel.forceRePair(dev.ip, dev.port, dev.hostname)
                        },
                        onConnectPaired = { paired ->
                            viewModel.connectToDevice(paired.ip, paired.port, paired.token, paired.name)
                        },
                        onPairAgain = { paired ->
                            viewModel.forceRePair(paired.ip, paired.port, paired.name)
                        },
                        onRemovePaired = { paired ->
                            viewModel.removePairedDevice(paired)
                        },
                        onManualConnectClick = {
                            viewModel.showManualConnectDialog.value = true
                        },
                        onOpenGuideClick = {
                            showSetupGuideDialog = true
                        },
                        onWakeOnLan = { mac ->
                            viewModel.sendWakeOnLan(mac)
                        },
                        onConnectUsb = {
                            val existing = pairedDevices.firstOrNull { it.ip == "127.0.0.1" }
                            viewModel.connectToDevice("127.0.0.1", 8765, existing?.token, "PC (USB Cable)")
                        },
                        localDeviceIp = localDeviceIp,
                        scanStatusMessage = scanStatusMessage,
                        onDirectConnectIp = { ip -> viewModel.forceRePair(ip, 8765, "Direct PC") },
                        onScanQrClick = { viewModel.openQrScanner() }
                    )

                    AppTab.SETTINGS -> SettingsScreen(
                        mouseSensitivity = mouseSensitivity,
                        onSensitivityChange = { viewModel.setSensitivity(it) },
                        scrollSpeed = scrollSpeed,
                        onScrollSpeedChange = { viewModel.setScrollSpeed(it) },
                        hapticsEnabled = hapticsEnabled,
                        onHapticsChange = { viewModel.setHaptics(it) },
                        tapToClickEnabled = tapToClickEnabled,
                        onTapToClickChange = { viewModel.setTapToClick(it) },
                        singleTapAction = singleTapAction,
                        onSingleTapActionChange = { viewModel.setSingleTapAction(it) },
                        naturalScrolling = naturalScrolling,
                        onNaturalScrollingChange = { viewModel.setNaturalScrolling(it) },
                        pairedDevices = pairedDevices,
                        onRemovePairedDevice = { viewModel.removePairedDevice(it) },
                        autoReconnectEnabled = autoReconnectEnabled,
                        onAutoReconnectChange = { viewModel.setAutoReconnect(it) },
                        rememberedHostIp = lastConnectedHostIp,
                        rememberedHostPort = lastConnectedPort,
                        rememberedHostName = lastConnectedName,
                        rememberedTimestamp = lastConnectedTimestamp,
                        onReconnectLastHost = { viewModel.reconnectToLastHost() },
                        onClearSavedHost = { viewModel.clearSavedHost() }
                    )
                }
            }

            // Dialogs
            if (showPairingDialog) {
                PairingDialog(
                    deviceName = targetPairingName,
                    errorMessage = pairingError,
                    isSubmitting = isPairingSubmitting,
                    onDismiss = { viewModel.showPairingDialog.value = false },
                    onSubmitPin = { pin -> viewModel.submitPairingPin(pin) },
                    onScanQrClick = {
                        viewModel.showPairingDialog.value = false
                        viewModel.openQrScanner()
                    },
                    onRequestNewCode = { viewModel.requestNewPairingCode() }
                )
            }

            if (showQrScanner) {
                com.example.ui.qr.QrScannerDialog(
                    onDismiss = { viewModel.closeQrScanner() },
                    onQrCodeScanned = { rawQr ->
                        val payload = com.example.model.QrPairingPayload.parse(rawQr)
                        if (payload != null) {
                            viewModel.pairWithQrPayload(payload) { success, msg ->
                                viewModel.userNotification.value = msg
                            }
                        } else {
                            viewModel.userNotification.value = "Unrecognized QR code format. Expecting OmniDesk code."
                        }
                    }
                )
            }

            if (showManualConnectDialog) {
                ManualConnectDialog(
                    onDismiss = { viewModel.showManualConnectDialog.value = false },
                    onConnect = { ip, port ->
                        val existing = pairedDevices.firstOrNull { it.ip == ip }
                        viewModel.connectToDevice(ip, port, existing?.token, "OmniDesk PC")
                    }
                )
            }

            if (showWolDialog) {
                com.example.ui.components.WakeOnLanDialog(
                    initialMacAddress = targetWolMac,
                    targetDeviceName = targetWolName,
                    pairedDevices = pairedDevices,
                    onDismiss = { viewModel.showWolDialog.value = false },
                    onSendMagicPacket = { mac, ip ->
                        viewModel.sendWakeOnLan(mac, ip)
                    },
                    onSaveMacAddress = { id, mac ->
                        viewModel.saveDeviceMac(id, mac)
                    }
                )
            }

            if (showSetupGuideDialog) {
                com.example.ui.components.ServerSetupGuideDialog(
                    onDismissRequest = { showSetupGuideDialog = false }
                )
            }
        }
    }
}
