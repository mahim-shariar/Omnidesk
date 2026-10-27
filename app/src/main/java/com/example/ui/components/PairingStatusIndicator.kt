package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusConnecting
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusPairing
import com.example.ui.theme.WarningOrange

/**
 * Visual paired connection status representation:
 * - PAIRED_CONNECTED: Active authenticated session to Linux server (Green dot)
 * - CONNECTING: In process of establishing session (Yellow dot, pulsing)
 * - PAIRING: Handshake/PIN verification active (Blue dot, pulsing)
 * - PAIRED_OFFLINE: Device is paired in database, but server currently disconnected (Amber dot)
 * - UNPAIRED: No paired server configured (Red dot)
 * - ERROR: Socket or network error (Red dot)
 */
enum class PairedConnectionCategory {
    PAIRED_CONNECTED,
    CONNECTING,
    PAIRING,
    PAIRED_OFFLINE,
    UNPAIRED,
    ERROR
}

fun resolveCategory(connectionState: ConnectionState, isPaired: Boolean): PairedConnectionCategory {
    return when (connectionState) {
        is ConnectionState.Connected -> PairedConnectionCategory.PAIRED_CONNECTED
        is ConnectionState.Connecting -> PairedConnectionCategory.CONNECTING
        is ConnectionState.PairingRequired, is ConnectionState.Discovering -> PairedConnectionCategory.PAIRING
        is ConnectionState.Error -> PairedConnectionCategory.ERROR
        is ConnectionState.Disconnected -> {
            if (isPaired) PairedConnectionCategory.PAIRED_OFFLINE else PairedConnectionCategory.UNPAIRED
        }
    }
}

fun getCategoryColor(category: PairedConnectionCategory): Color {
    return when (category) {
        PairedConnectionCategory.PAIRED_CONNECTED -> StatusConnected
        PairedConnectionCategory.CONNECTING -> StatusConnecting
        PairedConnectionCategory.PAIRING -> StatusPairing
        PairedConnectionCategory.PAIRED_OFFLINE -> WarningOrange
        PairedConnectionCategory.UNPAIRED -> StatusDisconnected
        PairedConnectionCategory.ERROR -> StatusDisconnected
    }
}

/**
 * Pulsing or solid color-coded status dot
 */
@Composable
fun PulsingStatusDot(
    category: PairedConnectionCategory,
    modifier: Modifier = Modifier,
    dotSize: Dp = 10.dp,
    animated: Boolean = true
) {
    val color = getCategoryColor(category)
    val shouldAnimate = animated && (category == PairedConnectionCategory.PAIRED_CONNECTED ||
            category == PairedConnectionCategory.CONNECTING ||
            category == PairedConnectionCategory.PAIRING)

    if (shouldAnimate) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulse_scale"
        )
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.55f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulse_alpha"
        )

        Box(
            modifier = modifier.size(dotSize * 2),
            contentAlignment = Alignment.Center
        ) {
            // Ripple ring
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(color.copy(alpha = pulseAlpha))
            )
            // Solid center dot
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Compact chip/badge showing color dot + pairing status label
 */
@Composable
fun PairingStatusChip(
    connectionState: ConnectionState,
    isPaired: Boolean,
    pairedDeviceName: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val category = resolveCategory(connectionState, isPaired)
    val color = getCategoryColor(category)

    val labelText = when (category) {
        PairedConnectionCategory.PAIRED_CONNECTED -> {
            val pc = (connectionState as ConnectionState.Connected).pcName
            if (pc.isNotBlank()) "Paired: $pc" else "Paired & Connected"
        }
        PairedConnectionCategory.CONNECTING -> "Connecting..."
        PairedConnectionCategory.PAIRING -> "Pairing with PC..."
        PairedConnectionCategory.PAIRED_OFFLINE -> {
            if (!pairedDeviceName.isNullOrBlank()) "Paired: $pairedDeviceName (Offline)" else "Paired (Offline)"
        }
        PairedConnectionCategory.UNPAIRED -> "Not Paired"
        PairedConnectionCategory.ERROR -> "Connection Error"
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .testTag("pairing_status_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulsingStatusDot(
                category = category,
                dotSize = 8.dp,
                animated = true
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Full detailed Pairing Status Banner (for Dashboard or Settings)
 */
@Composable
fun PairingStatusBanner(
    connectionState: ConnectionState,
    isPaired: Boolean,
    pairedDeviceName: String? = null,
    pairedDeviceIp: String? = null,
    modifier: Modifier = Modifier,
    onPrimaryAction: () -> Unit = {}
) {
    val category = resolveCategory(connectionState, isPaired)
    val color = getCategoryColor(category)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.4f)),
        tonalElevation = 2.dp,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .testTag("pairing_status_banner")
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                PulsingStatusDot(
                    category = category,
                    dotSize = 12.dp,
                    animated = true
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    val title = when (category) {
                        PairedConnectionCategory.PAIRED_CONNECTED -> "Paired & Connected"
                        PairedConnectionCategory.CONNECTING -> "Connecting to PC"
                        PairedConnectionCategory.PAIRING -> "Pairing Handshake"
                        PairedConnectionCategory.PAIRED_OFFLINE -> "Paired (Server Offline)"
                        PairedConnectionCategory.UNPAIRED -> "Not Paired"
                        PairedConnectionCategory.ERROR -> "Connection Error"
                    }

                    val subtitle = when (category) {
                        PairedConnectionCategory.PAIRED_CONNECTED -> {
                            val conn = connectionState as ConnectionState.Connected
                            "${conn.pcName} • ${conn.host}:${conn.port}"
                        }
                        PairedConnectionCategory.CONNECTING -> {
                            val conn = connectionState as ConnectionState.Connecting
                            "Connecting to ${conn.host}:${conn.port}..."
                        }
                        PairedConnectionCategory.PAIRING -> "Enter pairing code from your PC"
                        PairedConnectionCategory.PAIRED_OFFLINE -> {
                            val name = pairedDeviceName ?: "PC"
                            val ip = if (!pairedDeviceIp.isNullOrBlank()) " ($pairedDeviceIp)" else ""
                            "Saved: $name$ip • Tap to reconnect"
                        }
                        PairedConnectionCategory.UNPAIRED -> "Scan local network or enter IP to pair"
                        PairedConnectionCategory.ERROR -> {
                            (connectionState as ConnectionState.Error).message
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
