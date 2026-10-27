package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun ManualConnectDialog(
    initialIp: String = "192.168.1.",
    initialPort: Int = 8765,
    onDismiss: () -> Unit,
    onConnect: (ip: String, port: Int) -> Unit
) {
    var ipText by remember { mutableStateOf(initialIp) }
    var portText by remember { mutableStateOf(initialPort.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lan,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(text = "Connect Manually")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter your computer's local IP address (Windows, macOS, or Linux), or use USB Cable mode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                AssistChip(
                    onClick = {
                        ipText = "127.0.0.1"
                        portText = "8765"
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Usb,
                            contentDescription = "USB",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Use USB Cable (127.0.0.1)") },
                    shape = RoundedCornerShape(8.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        leadingIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = ipText,
                    onValueChange = { ipText = it.trim() },
                    label = { Text("PC IP Address") },
                    placeholder = { Text("192.168.1.105") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_ip_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Port") },
                    placeholder = { Text("8765") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_port_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = ipText.trim()
                        .removePrefix("ws://")
                        .removePrefix("wss://")
                        .removePrefix("http://")
                        .removePrefix("https://")
                        .substringBefore("/")
                    val finalIp = clean.substringBefore(":")
                    val finalPort = if (clean.contains(":")) {
                        clean.substringAfter(":").toIntOrNull() ?: (portText.toIntOrNull() ?: 8765)
                    } else {
                        portText.toIntOrNull() ?: 8765
                    }
                    onConnect(finalIp, finalPort)
                    onDismiss()
                },
                enabled = ipText.isNotBlank(),
                modifier = Modifier.testTag("manual_connect_button")
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("manual_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
