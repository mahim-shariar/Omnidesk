package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.PcStatus
import com.example.ui.theme.MintGreenPrimary
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.WarningOrange

@Composable
fun PcStatusScreen(
    pcStatus: PcStatus,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .testTag("pc_status_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // System Identification Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (pcStatus.isLaptop) Icons.Default.Laptop else Icons.Default.Computer,
                                contentDescription = if (pcStatus.isLaptop) "Laptop" else "Computer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = pcStatus.hostname,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${pcStatus.distro} (${pcStatus.version})${if (pcStatus.isLaptop) " • Laptop" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.components.OsBadge(
                            osType = pcStatus.osType,
                            osName = pcStatus.osDisplayName
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(StatusConnected)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailChip(icon = Icons.Default.Wifi, label = "IP", value = pcStatus.ip.ifEmpty { "Connected" })
                    DetailChip(icon = Icons.Default.Timer, label = "Uptime", value = pcStatus.formattedUptime)
                    if (pcStatus.hasBattery && pcStatus.batteryPercent != null) {
                        DetailChip(
                            icon = if (pcStatus.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                            label = if (pcStatus.isCharging) "Charging" else "Battery",
                            value = "${pcStatus.batteryPercent.toInt()}%"
                        )
                    } else if (pcStatus.cpuTemp > 0) {
                        DetailChip(icon = Icons.Default.DeviceThermostat, label = "Temp", value = "${pcStatus.cpuTemp.toInt()}°C")
                    }
                }
            }
        }

        // Laptop Battery Metric Card (shown if system has battery)
        if (pcStatus.hasBattery && pcStatus.batteryPercent != null) {
            val batPct = pcStatus.batteryPercent
            val batColor = when {
                pcStatus.isCharging -> MintGreenPrimary
                batPct > 30f -> MintGreenPrimary
                batPct > 15f -> WarningOrange
                else -> MaterialTheme.colorScheme.error
            }
            val statusText = when {
                pcStatus.isCharging && batPct >= 99f -> "AC Power Connected • Fully Charged"
                pcStatus.isCharging -> "Charging on AC Power"
                batPct <= 15f -> "Low Battery • Please Connect Charger"
                else -> "Running on Battery"
            }
            TelemetryBarCard(
                title = "Laptop Battery",
                subtitle = statusText,
                percent = batPct,
                detail = "${batPct.toInt()}%",
                color = batColor,
                icon = if (pcStatus.isCharging) Icons.Default.BatteryChargingFull else if (batPct > 20f) Icons.Default.BatteryFull else Icons.Default.BatteryAlert
            )
        }

        // CPU Metric Card
        TelemetryBarCard(
            title = "CPU Usage",
            subtitle = "Processor load",
            percent = pcStatus.cpuPercent,
            detail = "${pcStatus.cpuPercent.toInt()}%",
            color = if (pcStatus.cpuPercent > 80f) MaterialTheme.colorScheme.error else MintGreenPrimary,
            icon = Icons.Default.Memory
        )

        // RAM Metric Card
        TelemetryBarCard(
            title = "Memory (RAM)",
            subtitle = if (pcStatus.ramTotalGb > 0) "${pcStatus.ramUsedGb} GB of ${pcStatus.ramTotalGb} GB" else "System memory",
            percent = pcStatus.ramPercent,
            detail = "${pcStatus.ramPercent.toInt()}%",
            color = if (pcStatus.ramPercent > 85f) MaterialTheme.colorScheme.error else TerminalCyan,
            icon = Icons.Default.Storage
        )

        // Storage / Disk Metric Card
        TelemetryBarCard(
            title = if (pcStatus.isWindows) "System Drive ( C: )" else "Root Storage ( / )",
            subtitle = if (pcStatus.diskTotalGb > 0) "${pcStatus.diskUsedGb} GB of ${pcStatus.diskTotalGb} GB" else "Disk capacity",
            percent = pcStatus.diskPercent,
            detail = "${pcStatus.diskPercent.toInt()}%",
            color = if (pcStatus.diskPercent > 90f) MaterialTheme.colorScheme.error else WarningOrange,
            icon = Icons.Default.Storage
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Refresh Button
        Button(
            onClick = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("refresh_status_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refresh System Telemetry")
        }
    }
}

@Composable
fun TelemetryBarCard(
    title: String,
    subtitle: String,
    percent: Float,
    detail: String,
    color: Color,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = detail,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (percent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surface,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DetailChip(icon: ImageVector, label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
