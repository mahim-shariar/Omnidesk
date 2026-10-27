package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorderShine
import com.example.ui.theme.DarkSurfaceGlass
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.MintNeon
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.model.PcStatus
import com.example.ui.components.PairedConnectionCategory
import com.example.ui.components.PulsingStatusDot
import com.example.ui.theme.MintAccent
import com.example.ui.theme.MintGreenPrimary
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusConnecting
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.AppTab

@Composable
fun DashboardScreen(
    connectionState: ConnectionState,
    pcStatus: PcStatus,
    pairedDevices: List<com.example.data.entity.PairedDeviceEntity> = emptyList(),
    rememberedHostIp: String? = null,
    rememberedHostPort: Int = 8765,
    rememberedHostName: String? = null,
    rememberedTimestamp: Long = 0L,
    isAutoReconnecting: Boolean = false,
    autoReconnectStatus: String? = null,
    onNavigateTab: (AppTab) -> Unit,
    onOpenDiscovery: () -> Unit,
    onDisconnect: () -> Unit,
    onReconnect: (com.example.data.entity.PairedDeviceEntity) -> Unit = {},
    onReconnectLastHost: () -> Unit = {},
    onCancelAutoReconnect: () -> Unit = {},
    onClearSavedHost: () -> Unit = {},
    onOpenWakeOnLan: () -> Unit = {},
    onConnectUsb: () -> Unit = {},
    onScanQrClick: () -> Unit = {}
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWide = maxWidth > 650.dp
        val contentPadding = if (isWide) 24.dp else 16.dp
        val spacing = if (isWide) 16.dp else 14.dp

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 165.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1000.dp)
                .testTag("dashboard_screen"),
            contentPadding = PaddingValues(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            // Hero Connection Card
            item(span = { GridItemSpan(maxLineSpan) }) {
                ConnectionHeroCard(
                    connectionState = connectionState,
                    pcStatus = pcStatus,
                    pairedDevices = pairedDevices,
                    rememberedHostIp = rememberedHostIp,
                    rememberedHostPort = rememberedHostPort,
                    rememberedHostName = rememberedHostName,
                    rememberedTimestamp = rememberedTimestamp,
                    isAutoReconnecting = isAutoReconnecting,
                    autoReconnectStatus = autoReconnectStatus,
                    onOpenDiscovery = onOpenDiscovery,
                    onDisconnect = onDisconnect,
                    onReconnect = onReconnect,
                    onReconnectLastHost = onReconnectLastHost,
                    onCancelAutoReconnect = onCancelAutoReconnect,
                    onClearSavedHost = onClearSavedHost,
                    onOpenWakeOnLan = onOpenWakeOnLan,
                    onConnectUsb = onConnectUsb,
                    onScanQrClick = onScanQrClick
                )
            }

            // Quick Access Dock Strip
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickPillAction(
                        label = "Mouse Remote",
                        icon = Icons.Default.Mouse,
                        accentColor = MintGreenPrimary,
                        onClick = { onNavigateTab(AppTab.TOUCHPAD) }
                    )
                    QuickPillAction(
                        label = "Keyboard",
                        icon = Icons.Default.Keyboard,
                        accentColor = TerminalCyan,
                        onClick = { onNavigateTab(AppTab.KEYBOARD) }
                    )
                    QuickPillAction(
                        label = "Media",
                        icon = Icons.Default.MusicNote,
                        accentColor = Color(0xFFFFB74D),
                        onClick = { onNavigateTab(AppTab.MEDIA) }
                    )
                    QuickPillAction(
                        label = "PC Power",
                        icon = Icons.Default.PowerSettingsNew,
                        accentColor = Color(0xFFEF5350),
                        onClick = { onNavigateTab(AppTab.CONTROLS) }
                    )
                    QuickPillAction(
                        label = "Wake On LAN",
                        icon = Icons.Default.Power,
                        accentColor = MintNeon,
                        onClick = onOpenWakeOnLan
                    )
                    QuickPillAction(
                        label = "USB Cable",
                        icon = Icons.Default.Usb,
                        accentColor = CyberCyan,
                        onClick = onConnectUsb
                    )
                }
            }

            // Section Title: Remote Controls
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Controls & Input",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }

            // Card 1: Mouse Touchpad
            item {
                FeatureCard(
                    title = "Touchpad",
                    subtitle = "Virtual Mouse & Gestures",
                    icon = Icons.Default.Mouse,
                    accentColor = MintGreenPrimary,
                    tag = "Precision",
                    testTag = "card_touchpad",
                    onClick = { onNavigateTab(AppTab.TOUCHPAD) }
                )
            }

            // Card 2: Keyboard
            item {
                FeatureCard(
                    title = "Keyboard",
                    subtitle = "Type & Special Keys",
                    icon = Icons.Default.Keyboard,
                    accentColor = TerminalCyan,
                    tag = "Terminal",
                    testTag = "card_keyboard",
                    onClick = { onNavigateTab(AppTab.KEYBOARD) }
                )
            }

            // Card 3: Media
            item {
                FeatureCard(
                    title = "Media",
                    subtitle = "Volume & Playback",
                    icon = Icons.Default.MusicNote,
                    accentColor = Color(0xFFFFB74D),
                    tag = "MPRIS",
                    testTag = "card_media",
                    onClick = { onNavigateTab(AppTab.MEDIA) }
                )
            }

            // Card 4: PC Controls
            item {
                FeatureCard(
                    title = "PC Controls",
                    subtitle = "Lock, Suspend, Power",
                    icon = Icons.Default.PowerSettingsNew,
                    accentColor = Color(0xFFEF5350),
                    tag = "System",
                    testTag = "card_controls",
                    onClick = { onNavigateTab(AppTab.CONTROLS) }
                )
            }

            // Section Title: Productivity & Presentation
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Productivity & Media",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }

            // Card: Presentation Remote
            item {
                FeatureCard(
                    title = "Slides Remote",
                    subtitle = "Impress & Keynote",
                    icon = Icons.Default.Slideshow,
                    accentColor = Color(0xFF26C6DA),
                    tag = "Impress",
                    testTag = "card_presentation",
                    onClick = { onNavigateTab(AppTab.PRESENTATION) }
                )
            }

            // Card: Browser Remote
            item {
                FeatureCard(
                    title = "Web Browser",
                    subtitle = "Tabs, Zoom & URL",
                    icon = Icons.Default.Language,
                    accentColor = Color(0xFF42A5F5),
                    tag = "Web",
                    testTag = "card_browser",
                    onClick = { onNavigateTab(AppTab.BROWSER) }
                )
            }

            // Card: Clipboard Sync
            item {
                FeatureCard(
                    title = "Clipboard Sync",
                    subtitle = "Bi-directional Text",
                    icon = Icons.Default.ContentPaste,
                    accentColor = Color(0xFF66BB6A),
                    tag = "Sync",
                    testTag = "card_clipboard",
                    onClick = { onNavigateTab(AppTab.CLIPBOARD) }
                )
            }

            // Card: Custom Shortcuts
            item {
                FeatureCard(
                    title = "Custom Macros",
                    subtitle = "Personal Hotkeys",
                    icon = Icons.Default.Tune,
                    accentColor = Color(0xFFFFA726),
                    tag = "Hotkeys",
                    testTag = "card_custom_shortcuts",
                    onClick = { onNavigateTab(AppTab.CUSTOM_SHORTCUTS) }
                )
            }

            // Card: Sleep & Power Timer
            item {
                FeatureCard(
                    title = "Sleep Timer",
                    subtitle = "Schedule Shutdown",
                    icon = Icons.Default.NightsStay,
                    accentColor = Color(0xFFAB47BC),
                    tag = "Timer",
                    testTag = "card_power_timer",
                    onClick = { onNavigateTab(AppTab.POWER_TIMER) }
                )
            }

            // Section Title: Tools & Utilities
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Tools & Utilities",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // Card 5: Apps Launcher
            item {
                FeatureCard(
                    title = "App Launcher",
                    subtitle = "Launch Desktop Apps",
                    icon = Icons.Default.Apps,
                    accentColor = Color(0xFFAB47BC),
                    tag = "Desktop",
                    testTag = "card_apps",
                    onClick = { onNavigateTab(AppTab.APPLICATIONS) }
                )
            }

            // Card 6: Gamepad
            item {
                FeatureCard(
                    title = "Gamepad",
                    subtitle = "Virtual Controller",
                    icon = Icons.Default.Gamepad,
                    accentColor = Color(0xFF26A69A),
                    tag = "Analog",
                    testTag = "card_gamepad",
                    onClick = { onNavigateTab(AppTab.GAMEPAD) }
                )
            }

            // Card 7: PC Status
            item {
                FeatureCard(
                    title = "PC Status",
                    subtitle = "CPU, RAM & Telemetry",
                    icon = Icons.Default.Speed,
                    accentColor = Color(0xFF42A5F5),
                    tag = "Telemetry",
                    testTag = "card_status",
                    onClick = { onNavigateTab(AppTab.STATUS) }
                )
            }

            // Card 8: Settings
            item {
                FeatureCard(
                    title = "Settings",
                    subtitle = "Sensitivity & Pairing",
                    icon = Icons.Default.Settings,
                    accentColor = Color(0xFF78909C),
                    tag = "Config",
                    testTag = "card_settings",
                    onClick = { onNavigateTab(AppTab.SETTINGS) }
                )
            }

            // Card 9: Wake on LAN
            item {
                FeatureCard(
                    title = "Wake on LAN",
                    subtitle = "Power On Sleeping PC",
                    icon = Icons.Default.Power,
                    accentColor = Color(0xFF00E676),
                    tag = "WOL",
                    testTag = "card_wol",
                    onClick = onOpenWakeOnLan
                )
            }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
}

@Composable
fun ConnectionHeroCard(
    connectionState: ConnectionState,
    pcStatus: PcStatus,
    pairedDevices: List<com.example.data.entity.PairedDeviceEntity> = emptyList(),
    rememberedHostIp: String? = null,
    rememberedHostPort: Int = 8765,
    rememberedHostName: String? = null,
    rememberedTimestamp: Long = 0L,
    isAutoReconnecting: Boolean = false,
    autoReconnectStatus: String? = null,
    onOpenDiscovery: () -> Unit,
    onDisconnect: () -> Unit,
    onReconnect: (com.example.data.entity.PairedDeviceEntity) -> Unit = {},
    onReconnectLastHost: () -> Unit = {},
    onCancelAutoReconnect: () -> Unit = {},
    onClearSavedHost: () -> Unit = {},
    onOpenWakeOnLan: () -> Unit = {},
    onConnectUsb: () -> Unit = {},
    onScanQrClick: () -> Unit = {}
) {
    val isPaired = pairedDevices.isNotEmpty()
    val lastPaired = pairedDevices.firstOrNull()
    val effectiveHostIp = rememberedHostIp ?: lastPaired?.ip
    val effectiveHostPort = if (!rememberedHostIp.isNullOrBlank()) rememberedHostPort else (lastPaired?.port ?: 8765)
    val effectiveHostName = rememberedHostName ?: lastPaired?.name ?: "PC"
    val hasRememberedHost = !effectiveHostIp.isNullOrBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.16f),
                        MintGreenPrimary.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.03f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("connection_hero_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            when {
                connectionState is ConnectionState.Connected -> {
                    val isUsb = connectionState.host == "127.0.0.1" || connectionState.host.startsWith("127.") || connectionState.host.lowercase() == "localhost"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isUsb) {
                                Icon(
                                    imageVector = Icons.Default.Usb,
                                    contentDescription = "USB Cable",
                                    tint = MintGreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                PulsingStatusDot(
                                    category = PairedConnectionCategory.PAIRED_CONNECTED,
                                    dotSize = 10.dp,
                                    animated = true
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isUsb) "Paired via USB Cable" else "Paired & Connected",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isUsb) MintGreenPrimary else StatusConnected,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "${connectionState.host}:${connectionState.port}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = pcStatus.hostname.ifEmpty { connectionState.pcName },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        com.example.ui.components.OsBadge(
                            osType = pcStatus.osType,
                            osName = pcStatus.osDisplayName
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${pcStatus.distro} • ${pcStatus.version}${if (pcStatus.isLaptop) " • Laptop" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (pcStatus.hasBattery && pcStatus.batteryPercent != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = (if (pcStatus.isCharging) MintGreenPrimary else CyberCyan).copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (pcStatus.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                                        contentDescription = "Laptop Battery",
                                        tint = if (pcStatus.isCharging) MintGreenPrimary else CyberCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${pcStatus.batteryPercent.toInt()}%${if (pcStatus.isCharging) " ⚡" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (pcStatus.isCharging) MintGreenPrimary else CyberCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mini Telemetry metrics row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MiniMetric(
                            label = "CPU",
                            value = "${pcStatus.cpuPercent.toInt()}%",
                            percent = pcStatus.cpuPercent,
                            accentColor = MintGreenPrimary
                        )
                        MiniMetric(
                            label = "RAM",
                            value = "${pcStatus.ramPercent.toInt()}%",
                            percent = pcStatus.ramPercent,
                            accentColor = CyberCyan
                        )
                        if (pcStatus.hasBattery && pcStatus.batteryPercent != null) {
                            MiniMetric(
                                label = if (pcStatus.isCharging) "Charging" else "Battery",
                                value = "${pcStatus.batteryPercent.toInt()}%",
                                percent = pcStatus.batteryPercent,
                                accentColor = if (pcStatus.isCharging) MintGreenPrimary else if (pcStatus.batteryPercent > 20f) MintGreenPrimary else WarningOrange
                            )
                        } else {
                            MiniMetric(
                                label = "Uptime",
                                value = pcStatus.formattedUptime
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onDisconnect,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .testTag("hero_disconnect_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LinkOff,
                                contentDescription = "Disconnect from PC",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                isAutoReconnecting -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingStatusDot(
                                category = PairedConnectionCategory.CONNECTING,
                                dotSize = 10.dp,
                                animated = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Auto-Reconnecting",
                                style = MaterialTheme.typography.labelLarge,
                                color = StatusConnecting,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!effectiveHostIp.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = "$effectiveHostIp:$effectiveHostPort",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = effectiveHostName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = autoReconnectStatus ?: "Attempting automatic reconnection to remembered PC host...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onCancelAutoReconnect,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("cancel_auto_reconnect_button")
                        ) {
                            Text("Cancel Reconnect")
                        }

                        IconButton(
                            onClick = onConnectUsb,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MintGreenPrimary.copy(alpha = 0.15f))
                                .testTag("hero_usb_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Usb,
                                contentDescription = "Connect via USB Cable",
                                tint = MintGreenPrimary
                            )
                        }

                        IconButton(
                            onClick = onOpenWakeOnLan,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .testTag("hero_wol_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Power,
                                contentDescription = "Wake PC",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                connectionState is ConnectionState.Connecting -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulsingStatusDot(
                            category = PairedConnectionCategory.CONNECTING,
                            dotSize = 10.dp,
                            animated = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connecting to ${connectionState.host}...",
                            style = MaterialTheme.typography.labelLarge,
                            color = StatusConnecting,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Establishing secure WebSocket connection with PC...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                hasRememberedHost -> {
                    val timeAgo = formatTimestampAgo(rememberedTimestamp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingStatusDot(
                                category = if (isPaired) PairedConnectionCategory.PAIRED_OFFLINE else PairedConnectionCategory.PAIRED_OFFLINE,
                                dotSize = 10.dp,
                                animated = false
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPaired) "Paired (Offline)" else "Remembered Host (Offline)",
                                style = MaterialTheme.typography.labelLarge,
                                color = WarningOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "$effectiveHostIp:$effectiveHostPort",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = effectiveHostName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (lastPaired != null) {
                            com.example.ui.components.OsBadge(
                                osType = lastPaired.osType,
                                osName = lastPaired.osDisplayName
                            )
                        }
                    }
                    Text(
                        text = "Last connected: $timeAgo • Tap Reconnect to resume remote control.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (rememberedHostIp != null) {
                                    onReconnectLastHost()
                                } else if (lastPaired != null) {
                                    onReconnect(lastPaired)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarningOrange
                            ),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("hero_reconnect_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reconnect",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reconnect")
                        }

                        IconButton(
                            onClick = onConnectUsb,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MintGreenPrimary.copy(alpha = 0.15f))
                                .testTag("hero_usb_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Usb,
                                contentDescription = "Connect via USB Cable",
                                tint = MintGreenPrimary
                            )
                        }

                        IconButton(
                            onClick = onOpenWakeOnLan,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .testTag("hero_wol_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Power,
                                contentDescription = "Wake PC",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = onOpenDiscovery,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .testTag("hero_scan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = "Find other PCs",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingStatusDot(
                                category = PairedConnectionCategory.UNPAIRED,
                                dotSize = 10.dp,
                                animated = false
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Not Paired",
                                style = MaterialTheme.typography.labelLarge,
                                color = StatusDisconnected,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "No Computer Paired",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pair this phone with your Windows, Mac, or Linux computer over Wi-Fi to start controlling touch, keyboard, media, and system power.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onOpenDiscovery,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hero_connect_button")
                        ) {
                            Icon(imageVector = Icons.Default.Sensors, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pair PC")
                        }

                        IconButton(
                            onClick = onScanQrClick,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MintGreenPrimary.copy(alpha = 0.2f))
                                .testTag("hero_scan_qr_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan PC QR Code",
                                tint = MintGreenPrimary
                            )
                        }

                        IconButton(
                            onClick = onConnectUsb,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MintGreenPrimary.copy(alpha = 0.15f))
                                .testTag("hero_usb_unpaired_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Usb,
                                contentDescription = "Pair via USB Cable",
                                tint = MintGreenPrimary
                            )
                        }

                        IconButton(
                            onClick = onOpenWakeOnLan,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .testTag("hero_wol_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Power,
                                contentDescription = "Wake PC",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatTimestampAgo(timestamp: Long): String {
    if (timestamp <= 0L) return "Never"
    val diff = System.currentTimeMillis() - timestamp
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

@Composable
fun MiniMetric(
    label: String,
    value: String,
    percent: Float = 0f,
    accentColor: Color = MintGreenPrimary
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.widthIn(min = 80.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (percent > 0f) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(54.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                            .background(accentColor)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickPillAction(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(DarkBorderShine, GlassBorderSubtle))),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    tag: String? = null,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(138.dp)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.14f),
                        accentColor.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (!tag.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
