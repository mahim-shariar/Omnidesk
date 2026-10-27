package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusConnecting
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusPairing
import com.example.ui.theme.WarningOrange

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.DarkBorderShine
import com.example.ui.theme.DarkSurfaceGlass
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.MintGreenPrimary

@Composable
fun TopStatusBar(
    connectionState: ConnectionState,
    isPaired: Boolean = false,
    pairedDeviceName: String? = null,
    rememberedHostIp: String? = null,
    isAutoReconnecting: Boolean = false,
    autoReconnectStatus: String? = null,
    title: String = "OmniDesk",
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onReconnect: () -> Unit = {},
    onOpenDiscovery: () -> Unit = {}
) {
    val category = if (isAutoReconnecting) {
        PairedConnectionCategory.CONNECTING
    } else {
        resolveCategory(connectionState, isPaired || !rememberedHostIp.isNullOrBlank())
    }
    val categoryColor = getCategoryColor(category)

    Surface(
        color = DarkSurfaceGlass,
        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(DarkBorderShine, GlassBorderSubtle))),
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("top_status_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title and Status section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (canNavigateBack) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("top_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            PulsingStatusDot(
                                category = category,
                                dotSize = 8.dp,
                                animated = true
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = getFormattedStatusLabel(
                                    state = connectionState,
                                    isPaired = isPaired,
                                    pairedDeviceName = pairedDeviceName,
                                    isAutoReconnecting = isAutoReconnecting,
                                    autoReconnectStatus = autoReconnectStatus,
                                    rememberedHostIp = rememberedHostIp
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = categoryColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Action Pill on right
                val isUsb = (connectionState is ConnectionState.Connected && 
                        (connectionState.host == "127.0.0.1" || connectionState.host.startsWith("127.") || connectionState.host.lowercase() == "localhost")) ||
                        (pairedDeviceName?.contains("USB", ignoreCase = true) == true)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (category) {
                        PairedConnectionCategory.PAIRED_CONNECTED -> {
                            val conn = connectionState as ConnectionState.Connected
                            Surface(
                                color = StatusConnected.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusConnected.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .testTag("pill_connected")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isUsb) Icons.Default.Usb else Icons.Default.Computer,
                                        contentDescription = if (isUsb) "Connected via USB Cable" else "Connected PC",
                                        tint = StatusConnected,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isUsb) "USB Cable" else conn.pcName.ifEmpty { "Paired PC" },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = StatusConnected,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        PairedConnectionCategory.CONNECTING -> {
                            Surface(
                                color = StatusConnecting.copy(alpha = 0.15f),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusConnecting.copy(alpha = 0.35f)),
                                modifier = Modifier.clip(CircleShape)
                            ) {
                                Box(
                                    modifier = Modifier.padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Connecting...",
                                        tint = StatusConnecting,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        PairedConnectionCategory.PAIRING -> {
                            Surface(
                                color = StatusPairing.copy(alpha = 0.15f),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusPairing.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onReconnect() }
                            ) {
                                Box(
                                    modifier = Modifier.padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VpnKey,
                                        contentDescription = "Pairing with PC",
                                        tint = StatusPairing,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        PairedConnectionCategory.PAIRED_OFFLINE -> {
                            Surface(
                                color = WarningOrange.copy(alpha = 0.15f),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, WarningOrange.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onReconnect() }
                                    .testTag("pill_reconnect")
                            ) {
                                Box(
                                    modifier = Modifier.padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isUsb) Icons.Default.Usb else Icons.Default.Refresh,
                                        contentDescription = "Reconnect to PC",
                                        tint = WarningOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        PairedConnectionCategory.UNPAIRED, PairedConnectionCategory.ERROR -> {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onOpenDiscovery() }
                                    .testTag("pill_pair_pc")
                            ) {
                                Box(
                                    modifier = Modifier.padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = "Pair with PC",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detailed status label clearly explaining paired state
 */
fun getFormattedStatusLabel(
    state: ConnectionState,
    isPaired: Boolean,
    pairedDeviceName: String?,
    isAutoReconnecting: Boolean = false,
    autoReconnectStatus: String? = null,
    rememberedHostIp: String? = null
): String {
    if (isAutoReconnecting) {
        return autoReconnectStatus ?: "Auto-reconnecting to ${rememberedHostIp ?: "PC"}..."
    }
    return when (state) {
        is ConnectionState.Connected -> "Paired & Connected (${state.host})"
        is ConnectionState.Connecting -> "Connecting to ${state.host}..."
        is ConnectionState.PairingRequired -> "Pairing with ${state.deviceName}..."
        is ConnectionState.Discovering -> "Searching for PCs..."
        is ConnectionState.Error -> "Error: ${state.message}"
        is ConnectionState.Disconnected -> {
            if (isPaired) {
                if (!pairedDeviceName.isNullOrBlank()) "Paired with $pairedDeviceName (Offline)" else "Paired (Offline)"
            } else if (!rememberedHostIp.isNullOrBlank()) {
                "Remembered Host: $rememberedHostIp (Offline)"
            } else {
                "Not Paired"
            }
        }
    }
}
