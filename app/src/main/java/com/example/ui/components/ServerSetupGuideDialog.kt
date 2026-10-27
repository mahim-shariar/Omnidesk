package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.MintGreenPrimary

@Composable
fun ServerSetupGuideDialog(
    onDismissRequest: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val copyCommand: (String, String) -> Unit = { cmd, label ->
        clipboardManager.setText(AnnotatedString(cmd))
        Toast.makeText(context, "$label command copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp)
                .testTag("server_setup_guide_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MintGreenPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Computer,
                                contentDescription = null,
                                tint = MintGreenPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "PC Receiver Setup Guide",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Interactive prompt-by-prompt guided installer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("close_guide_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // OS Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    val tabs = listOf("Windows", "macOS", "Linux", "Universal Python")
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> WindowsSetupContent(copyCommand)
                        1 -> MacOsSetupContent(copyCommand)
                        2 -> LinuxSetupContent(copyCommand)
                        3 -> UniversalSetupContent(copyCommand)
                    }

                    // Common USB Cable instructions
                    UsbTetheringCard(copyCommand)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreenPrimary)
                ) {
                    Text("Got It! Back to Scanner", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WindowsSetupContent(copyCommand: (String, String) -> Unit) {
    Text(
        text = "Windows 10 & 11 (Command Prompt or PowerShell):",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "cd omnidesk-server && install.bat",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = MintGreenPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Or double-click 'install.bat' in the omnidesk-server folder!",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { copyCommand("cd omnidesk-server && install.bat", "Windows CMD") },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MintGreenPrimary),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy CMD", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = { copyCommand("cd omnidesk-server; .\\install.ps1", "Windows PowerShell") },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("PowerShell", fontWeight = FontWeight.Bold)
        }
    }

    StepByStepPromptsCard(
        steps = listOf(
            "1. Python Check" to "Detects Python 3.9+ or offers one-click installation via winget.",
            "2. Virtual Environment" to "Creates clean, isolated .venv environment with all dependencies.",
            "3. Windows Defender Firewall" to "Automatically allows inbound connections on TCP 8765 and UDP 8766.",
            "4. Auto-Start on Login" to "Optionally adds OmniDesk to your Windows Startup folder.",
            "5. Desktop Shortcut" to "Creates a 'Start OmniDesk.bat' shortcut on your Desktop.",
            "6. Instant Launch" to "Starts the server, printing your local IP, pairing PIN & QR code!"
        )
    )
}

@Composable
private fun MacOsSetupContent(copyCommand: (String, String) -> Unit) {
    Text(
        text = "macOS (Sonoma, Ventura, Monterey, Sequoia+):",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "cd omnidesk-server && chmod +x install-mac.sh && ./install-mac.sh",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = MintGreenPrimary
            )
        }
    }

    Button(
        onClick = { copyCommand("cd omnidesk-server && chmod +x install-mac.sh && ./install-mac.sh", "macOS") },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MintGreenPrimary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Copy macOS Install Command", color = Color.Black, fontWeight = FontWeight.Bold)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Important macOS Permissions:\nThe wizard will automatically open System Settings > Privacy & Security > Accessibility so you can toggle on Terminal or Python.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }

    StepByStepPromptsCard(
        steps = listOf(
            "1. Python & Xcode Tools" to "Uses system Python 3 or prompts for Xcode Command Line Tools.",
            "2. Virtual Environment" to "Isolates packages in .venv to keep your Mac clean.",
            "3. Accessibility Permission" to "Prompts and opens System Settings so the receiver can move mouse pointer & click.",
            "4. Input Monitoring" to "Opens Input Monitoring settings for global keyboard shortcut injection.",
            "5. Auto-Start LaunchAgent" to "Optionally sets up ~/Library/LaunchAgents/com.omnidesk.server.plist.",
            "6. Instant Launch" to "Displays ASCII QR code and 6-digit PIN in Terminal!"
        )
    )
}

@Composable
private fun LinuxSetupContent(copyCommand: (String, String) -> Unit) {
    Text(
        text = "Linux (Mint, Ubuntu, Debian, Fedora, Arch, openSUSE):",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "cd omnidesk-server && chmod +x install.sh && ./install.sh",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = MintGreenPrimary
            )
        }
    }

    Button(
        onClick = { copyCommand("cd omnidesk-server && chmod +x install.sh && ./install.sh", "Linux") },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MintGreenPrimary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Copy Linux Install Command", color = Color.Black, fontWeight = FontWeight.Bold)
    }

    StepByStepPromptsCard(
        steps = listOf(
            "1. Distro Detection" to "Detects apt, dnf, pacman, or zypper automatically.",
            "2. Desktop Tools" to "Installs xdotool (mouse/keys), playerctl (media), xclip (clipboard), and pulseaudio.",
            "3. UFW Firewall" to "Prompts to allow ports 8765/tcp and 8766/udp if UFW is active.",
            "4. Gamepad /dev/uinput" to "Sets up udev rule and input group for virtual gamepad without root.",
            "5. Systemd User Service" to "Enables background service so it auto-starts on boot.",
            "6. Instant Launch" to "Launches receiver and prints live QR code and pairing PIN!"
        )
    )
}

@Composable
private fun UniversalSetupContent(copyCommand: (String, String) -> Unit) {
    Text(
        text = "Universal Python Installer (Any OS):",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "cd omnidesk-server && python3 setup.py",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = MintGreenPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "(On Windows, run: python setup.py)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Button(
        onClick = { copyCommand("cd omnidesk-server && python3 setup.py", "Universal Python") },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MintGreenPrimary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Copy Universal Setup Command", color = Color.Black, fontWeight = FontWeight.Bold)
    }

    StepByStepPromptsCard(
        steps = listOf(
            "Universal Cross-Platform Wizard" to "Runs 'install_wizard.py' which automatically detects your operating system and adapts all steps.",
            "Simple [Y/n] Prompts" to "Every prompt highlights the recommended answer in green. Just press ENTER to accept!",
            "Immediate Feedback" to "Confirms every step with a green checkmark and suggests the next step in plain English."
        )
    )
}

@Composable
private fun StepByStepPromptsCard(steps: List<Pair<String, String>>) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Interactive Prompts You Will See:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            steps.forEach { (title, desc) ->
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun UsbTetheringCard(copyCommand: (String, String) -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Usb, contentDescription = null, tint = MintGreenPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "USB Cable Mode (0ms Latency / Offline)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "If you have no Wi-Fi router or want 0ms latency for gaming, enable USB Debugging on your phone, connect USB cable, and run:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "adb reverse tcp:8765 tcp:8765",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MintGreenPrimary,
                    modifier = Modifier.padding(8.dp)
                )
            }
            OutlinedButton(
                onClick = { copyCommand("adb reverse tcp:8765 tcp:8765", "ADB Reverse") },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy ADB Reverse Command", fontSize = 12.sp)
            }
        }
    }
}
