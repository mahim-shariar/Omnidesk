package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.PairedDeviceEntity
import com.example.model.ConnectionState
import com.example.model.DiscoveredDevice
import com.example.ui.theme.MintGreenPrimary
import com.example.ui.theme.StatusConnected

@Composable
fun DiscoveryScreen(
    isScanning: Boolean,
    discoveredDevices: List<DiscoveredDevice>,
    pairedDevices: List<PairedDeviceEntity>,
    connectionState: ConnectionState = ConnectionState.Disconnected,
    onStartScan: () -> Unit,
    onConnectDiscovered: (DiscoveredDevice) -> Unit,
    onConnectPaired: (PairedDeviceEntity) -> Unit,
    onRemovePaired: (PairedDeviceEntity) -> Unit,
    onManualConnectClick: () -> Unit,
    onOpenGuideClick: () -> Unit,
    onWakeOnLan: (macAddress: String) -> Unit = {},
    onConnectUsb: () -> Unit = {},
    localDeviceIp: String = "",
    scanStatusMessage: String = "",
    onDirectConnectIp: (String) -> Unit = {},
    onScanQrClick: () -> Unit = {},
    onPairAgain: (PairedDeviceEntity) -> Unit = {},
    onForcePairDiscovered: (DiscoveredDevice) -> Unit = {}
) {
    var showWolDialog by remember { mutableStateOf(false) }
    var targetWolMac by remember { mutableStateOf("") }
    var showTroubleshooting by remember { mutableStateOf(false) }
    val connectedHostIp = (connectionState as? ConnectionState.Connected)?.host

    // Pre-calculate local subnet prefix for quick IP connect
    val defaultPrefix = remember(localDeviceIp) {
        if (localDeviceIp.isNotBlank() && localDeviceIp.contains(".")) {
            localDeviceIp.substringBeforeLast(".") + "."
        } else {
            "192.168.1."
        }
    }
    var manualIpInput by remember(defaultPrefix) { mutableStateOf(defaultPrefix) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .testTag("discovery_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // OmniDesk Brand Hero Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("omnidesk_brand_header"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.omnidesk_logo_1792167721144),
                        contentDescription = "OmniDesk Logo",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Column {
                        Text(
                            text = "OmniDesk",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Universal PC Remote • Windows, Mac & Linux",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Scanner Banner Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .scale(if (isScanning) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Wi-Fi & Network Discovery",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isScanning) {
                                    if (scanStatusMessage.isNotBlank()) scanStatusMessage else "Sweeping Wi-Fi subnet & mDNS..."
                                } else {
                                    "Scan complete (${discoveredDevices.size} PC found)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (localDeviceIp.isNotBlank()) {
                                Text(
                                    text = "Your phone IP: $localDeviceIp",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MintGreenPrimary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onStartScan,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh scan",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Primary Fast Pairing: QR Code Scanner Card
        item {
            Card(
                onClick = onScanQrClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scan_pc_qr_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, MintGreenPrimary.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MintGreenPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan PC QR Code",
                            tint = MintGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scan PC QR Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Instant 1-second pairing by scanning your PC screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MintGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Action Buttons: USB Cable, Manual IP & Install Guide
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onConnectUsb,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("usb_pc_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Usb,
                        contentDescription = "USB Cable Connection",
                        modifier = Modifier.size(22.dp),
                        tint = MintGreenPrimary
                    )
                }

                OutlinedButton(
                    onClick = onManualConnectClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("manual_pc_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lan,
                        contentDescription = "Manual IP Connect",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedButton(
                    onClick = onOpenGuideClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("server_setup_guide_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Server Setup Guide",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Dedicated USB Cable Pairing Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConnectUsb() }
                    .testTag("usb_cable_mode_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MintGreenPrimary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MintGreenPrimary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Usb,
                                contentDescription = "USB Cable",
                                tint = MintGreenPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "USB Cable Connection (0ms Latency)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "No Wi-Fi needed: Plug USB cable with ADB reverse or USB tethering",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onConnectUsb,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MintGreenPrimary)
                            .testTag("connect_usb_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Connect via USB",
                            tint = Color.Black
                        )
                    }
                }
            }
        }

        // Direct Wi-Fi IP Quick-Connect Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Direct Wi-Fi Connect (IP Address)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "If router blocks automatic broadcast, enter your PC IP address directly:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = manualIpInput,
                            onValueChange = { manualIpInput = it.trim() },
                            placeholder = { Text("e.g. 192.168.1.50") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("direct_ip_input_field")
                        )
                        Button(
                            onClick = {
                                if (manualIpInput.isNotBlank()) {
                                    val ip = manualIpInput.trim()
                                    val dev = DiscoveredDevice(
                                        id = "$ip:8765",
                                        hostname = "Direct PC",
                                        ip = ip,
                                        port = 8765,
                                        os = "PC",
                                        version = "Direct Wi-Fi",
                                        mac = "",
                                        osType = "unknown",
                                        lastSeen = System.currentTimeMillis()
                                    )
                                    onConnectDiscovered(dev)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreenPrimary),
                            modifier = Modifier.testTag("direct_ip_connect_btn")
                        ) {
                            Text("Connect", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Available Discovered Computers
        item {
            Text(
                text = "Available Computers (${discoveredDevices.size})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        if (discoveredDevices.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isScanning) "Searching your Wi-Fi network..." else "No computers detected yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Why isn't my PC showing up?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MintGreenPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            TextButton(onClick = { showTroubleshooting = !showTroubleshooting }) {
                                Text(if (showTroubleshooting) "Hide Tips" else "Fix It")
                            }
                        }

                        if (showTroubleshooting) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onOpenGuideClick,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MintGreenPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_pc_guide_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Computer,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Guided PC Setup Wizard", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "• Windows:\n  Allow port 8765 in Windows Defender Firewall or run:\n  netsh advfirewall firewall add rule name=\"OmniDesk\" dir=in action=allow protocol=TCP localport=8765\n  Run 'ipconfig' in CMD for your PC's Wi-Fi IP.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• macOS:\n  Ensure Python has Accessibility & Screen Recording permissions in System Settings > Privacy & Security. Allow port 8765 if Mac firewall is enabled. Run 'ipconfig getifaddr en0' in Terminal for IP.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Linux:\n  Allow port 8765 in UFW: sudo ufw allow 8765/tcp\n  Run 'hostname -I' for your Linux PC IP.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Network Requirement:\n  Both phone and PC must be on the same local Wi-Fi router (avoid isolated guest Wi-Fi).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        } else {
            items(discoveredDevices, key = { it.id }) { device ->
                DiscoveredDeviceCard(
                    device = device,
                    isConnected = connectedHostIp != null && connectedHostIp == device.ip,
                    onConnect = { onConnectDiscovered(device) },
                    onPairClick = { onForcePairDiscovered(device) }
                )
            }
        }

        // Paired History Section
        if (pairedDevices.isNotEmpty()) {
            item {
                Text(
                    text = if (pairedDevices.any { it.ip == connectedHostIp }) "Connected & Saved Computers (${pairedDevices.size})" else "Saved Computers (${pairedDevices.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            items(pairedDevices, key = { it.id }) { paired ->
                PairedDeviceCard(
                    device = paired,
                    isConnected = connectedHostIp != null && connectedHostIp == paired.ip,
                    onConnect = { onConnectPaired(paired) },
                    onPairAgain = { onPairAgain(paired) },
                    onRemove = { onRemovePaired(paired) },
                    onWakeClick = {
                        targetWolMac = paired.macOrUid.ifEmpty { "00:11:22:33:44:55" }
                        showWolDialog = true
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showWolDialog) {
        com.example.ui.components.WakeOnLanDialog(
            initialMacAddress = targetWolMac,
            targetDeviceName = "PC",
            pairedDevices = pairedDevices,
            onDismiss = { showWolDialog = false },
            onSendMagicPacket = { mac, _ ->
                onWakeOnLan(mac)
                showWolDialog = false
            }
        )
    }
}

@Composable
fun DiscoveredDeviceCard(
    device: DiscoveredDevice,
    isConnected: Boolean = false,
    onConnect: () -> Unit,
    onPairClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("discovered_card_${device.ip}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (isConnected) androidx.compose.foundation.BorderStroke(1.5.dp, StatusConnected.copy(alpha = 0.8f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val isUsb = device.ip == "127.0.0.1" || device.hostname.contains("USB", ignoreCase = true)
            val pcIcon = when {
                isUsb -> Icons.Default.Usb
                device.isLaptop -> Icons.Default.Laptop
                else -> Icons.Default.Computer
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isConnected) StatusConnected.copy(alpha = 0.18f) else MintGreenPrimary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = pcIcon,
                        contentDescription = if (isUsb) "USB Connected PC" else if (device.isLaptop) "Laptop" else "Network PC",
                        tint = if (isConnected) StatusConnected else MintGreenPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.hostname,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        com.example.ui.components.OsBadge(
                            osType = device.osType,
                            osName = device.os
                        )
                        if (isConnected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = StatusConnected.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusConnected.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(StatusConnected)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "CONNECTED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusConnected
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "${device.os} • ${device.ip}:${device.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(StatusConnected)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isConnected) "Active Connection • ${device.mac.ifEmpty { "Wi-Fi" }}" else if (device.mac.isNotBlank()) "Available • ${device.mac}" else "Available • Wi-Fi",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusConnected
                        )
                        if (device.batteryPercent != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MintGreenPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BatteryStd,
                                        contentDescription = "Battery",
                                        tint = MintGreenPrimary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${device.batteryPercent.toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MintGreenPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isConnected) {
                Surface(
                    color = StatusConnected.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusConnected.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = StatusConnected,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusConnected,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onPairClick != null) {
                        IconButton(
                            onClick = onPairClick,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("pair_btn_${device.ip}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Pair with ${device.hostname}",
                                tint = MintGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onConnect,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("connect_btn_${device.ip}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Connect to ${device.hostname}",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PairedDeviceCard(
    device: PairedDeviceEntity,
    isConnected: Boolean = false,
    onConnect: () -> Unit,
    onPairAgain: (() -> Unit)? = null,
    onRemove: () -> Unit,
    onWakeClick: () -> Unit = {}
) {
    val isUsbDevice = device.ip == "127.0.0.1" || device.name.contains("USB", ignoreCase = true)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
        border = if (isConnected) androidx.compose.foundation.BorderStroke(1.5.dp, StatusConnected.copy(alpha = 0.8f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) StatusConnected.copy(alpha = 0.18f) else if (isUsbDevice) MintGreenPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUsbDevice) Icons.Default.Usb else Icons.Default.Computer,
                        contentDescription = null,
                        tint = if (isConnected) StatusConnected else if (isUsbDevice) MintGreenPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        com.example.ui.components.OsBadge(
                            osType = device.osType,
                            osName = device.osName
                        )
                        if (isConnected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = StatusConnected.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusConnected.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(StatusConnected)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "CONNECTED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusConnected
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "${device.ip}:${device.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onWakeClick) {
                    Icon(
                        imageVector = Icons.Default.Power,
                        contentDescription = "Wake-on-LAN",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (isConnected) {
                    Surface(
                        color = StatusConnected.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusConnected.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = StatusConnected,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusConnected,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    if (onPairAgain != null) {
                        IconButton(
                            onClick = onPairAgain,
                            modifier = Modifier.testTag("repair_btn_${device.ip}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Pair again with ${device.name}",
                                tint = MintGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onConnect,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("connect_paired_btn_${device.ip}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Connect to ${device.name}",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove paired device",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
