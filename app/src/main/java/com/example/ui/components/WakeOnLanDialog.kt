package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.PairedDeviceEntity
import com.example.network.WakeOnLanManager
import com.example.ui.theme.MintGreenPrimary

@Composable
fun WakeOnLanDialog(
    initialMacAddress: String = "",
    targetDeviceName: String = "PC",
    pairedDevices: List<PairedDeviceEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSendMagicPacket: (macAddress: String, targetIp: String) -> Unit,
    onSaveMacAddress: ((deviceId: Int, mac: String) -> Unit)? = null
) {
    var macInput by remember { mutableStateOf(initialMacAddress) }
    var broadcastIp by remember { mutableStateOf("255.255.255.255") }
    var selectedDevice by remember { mutableStateOf<PairedDeviceEntity?>(null) }
    var showHelp by remember { mutableStateOf(false) }

    val isValid = remember(macInput) {
        WakeOnLanManager.isValidMacAddress(macInput)
    }

    val normalized = remember(macInput) {
        WakeOnLanManager.normalizeMacAddress(macInput) ?: ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Power,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Wake-on-LAN (WoL)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Send a network magic packet to remotely power on your sleeping computer over Wi-Fi/LAN.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick selector if multiple paired devices exist
                if (pairedDevices.isNotEmpty()) {
                    Text(
                        text = "Select Paired Computer:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        pairedDevices.forEach { dev ->
                            val isSelected = selectedDevice?.id == dev.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedDevice = dev
                                        if (dev.macOrUid.isNotBlank()) {
                                            macInput = dev.macOrUid
                                        }
                                        if (dev.ip.isNotBlank()) {
                                            broadcastIp = dev.ip
                                        }
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = dev.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = dev.ip,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // MAC Address Field
                OutlinedTextField(
                    value = macInput,
                    onValueChange = { macInput = it },
                    label = { Text("PC MAC Address") },
                    placeholder = { Text("e.g. 70:85:C2:5B:14:8A") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wol_mac_input"),
                    trailingIcon = {
                        if (macInput.isNotEmpty()) {
                            Icon(
                                imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (isValid) MintGreenPrimary else MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    supportingText = {
                        if (isValid) {
                            Text(
                                text = "Standardized: $normalized",
                                color = MintGreenPrimary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else if (macInput.isNotBlank()) {
                            Text(
                                text = "12 hex characters required (0-9, A-F)",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                )

                // Broadcast IP field
                OutlinedTextField(
                    value = broadcastIp,
                    onValueChange = { broadcastIp = it },
                    label = { Text("Broadcast / Target IP") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Setup Guide Expander
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showHelp = !showHelp }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "How to find MAC & enable WoL",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = if (showHelp) "Hide" else "Show",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                AnimatedVisibility(visible = showHelp) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "• Windows:\n  Device Manager > Network Adapters > Properties > Power Management (Check 'Allow this device to wake the computer'). Run 'ipconfig /all' for MAC address.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• macOS:\n  System Settings > Energy Saver > Turn on 'Wake for network access'. Run 'ifconfig' in Terminal for ether address.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Linux:\n  Run: sudo ethtool -s eth0 wol g\n  Run 'ip link' to find interface & MAC address.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• All Systems: Enable 'Wake on LAN' in your computer's BIOS/UEFI Power Management.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalMac = normalized.ifEmpty { macInput.trim() }
                    onSendMagicPacket(finalMac, broadcastIp.trim())
                    selectedDevice?.let { dev ->
                        onSaveMacAddress?.invoke(dev.id, finalMac)
                    }
                    onDismiss()
                },
                enabled = isValid,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("wol_send_button")
            ) {
                Icon(imageVector = Icons.Default.Power, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send Magic Packet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
