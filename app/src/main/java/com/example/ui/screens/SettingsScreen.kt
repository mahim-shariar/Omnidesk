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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.unit.sp
import com.example.data.entity.PairedDeviceEntity
import com.example.ui.theme.MintGreenPrimary

@Composable
fun SettingsScreen(
    mouseSensitivity: Float,
    onSensitivityChange: (Float) -> Unit,
    scrollSpeed: Float,
    onScrollSpeedChange: (Float) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    tapToClickEnabled: Boolean = true,
    onTapToClickChange: (Boolean) -> Unit = {},
    singleTapAction: String = "left",
    onSingleTapActionChange: (String) -> Unit = {},
    naturalScrolling: Boolean = false,
    onNaturalScrollingChange: (Boolean) -> Unit = {},
    pairedDevices: List<PairedDeviceEntity>,
    onRemovePairedDevice: (PairedDeviceEntity) -> Unit,
    autoReconnectEnabled: Boolean = true,
    onAutoReconnectChange: (Boolean) -> Unit = {},
    rememberedHostIp: String? = null,
    rememberedHostPort: Int = 8765,
    rememberedHostName: String? = null,
    rememberedTimestamp: Long = 0L,
    onReconnectLastHost: () -> Unit = {},
    onClearSavedHost: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connection & Auto-Reconnect Settings
        Text(
            text = "Connection & Auto-Reconnect",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth().testTag("auto_reconnect_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Auto-reconnect toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Auto-Reconnect on Launch", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Automatically connect to the last PC when opening the app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoReconnectEnabled,
                        onCheckedChange = onAutoReconnectChange,
                        modifier = Modifier.testTag("auto_reconnect_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Remembered Host Details
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Remembered Host",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        if (!rememberedHostIp.isNullOrBlank()) {
                            Text(
                                text = rememberedHostName ?: "PC",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "IP: $rememberedHostIp:$rememberedHostPort",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Last connected: ${formatTimestampAgo(rememberedTimestamp)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onReconnectLastHost,
                                    modifier = Modifier.weight(1f).testTag("settings_reconnect_btn")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reconnect")
                                }

                                OutlinedButton(
                                    onClick = onClearSavedHost,
                                    modifier = Modifier.testTag("settings_forget_host_btn")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Forget")
                                }
                            }
                        } else {
                            Text(
                                text = "No host saved yet. Once you connect to a PC (Windows, macOS, or Linux), it will be remembered here for auto-reconnect.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        // Touch & Mouse Settings
        Text(
            text = "Mouse & Touchpad",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Sensitivity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Pointer Sensitivity", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = String.format("%.1fx", mouseSensitivity),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = mouseSensitivity,
                    onValueChange = onSensitivityChange,
                    valueRange = 0.5f..3.0f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Scroll Speed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Scroll Speed", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = String.format("%.1fx", scrollSpeed),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = scrollSpeed,
                    onValueChange = onScrollSpeedChange,
                    valueRange = 0.5f..2.5f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Tap to Click Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Tap to Click", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Tap on trackpad to perform click without using bottom buttons",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = tapToClickEnabled,
                        onCheckedChange = onTapToClickChange,
                        modifier = Modifier.testTag("settings_tap_to_click_switch")
                    )
                }

                // 1-Finger Tap Action (Left vs Right Click)
                if (tapToClickEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "1-Finger Tap Action", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Choose whether a single tap selects/opens (Left Click) or shows context menu (Right Click)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = singleTapAction == "left",
                            onClick = { onSingleTapActionChange("left") },
                            label = { Text("Left Click (Default)") },
                            leadingIcon = {
                                Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f).testTag("settings_tap_left")
                        )
                        FilterChip(
                            selected = singleTapAction == "right",
                            onClick = { onSingleTapActionChange("right") },
                            label = { Text("Right Click") },
                            modifier = Modifier.weight(1f).testTag("settings_tap_right")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Natural Scrolling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Natural Scrolling", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Invert 2-finger scroll direction",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = naturalScrolling,
                        onCheckedChange = onNaturalScrollingChange,
                        modifier = Modifier.testTag("settings_natural_scrolling_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Haptic feedback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Haptic Vibration", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Vibrate on clicks and button taps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = onHapticsChange
                    )
                }
            }
        }

        // Security & Paired Devices
        Text(
            text = "Security & Paired Devices",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (pairedDevices.isEmpty()) {
                    Text(
                        text = "No paired devices. Connect to a PC using a 6-digit code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    pairedDevices.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MintGreenPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = device.name, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MintGreenPrimary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Paired",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MintGreenPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${device.ip} • Token: ***${device.token.takeLast(6)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onRemovePairedDevice(device) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Revoke device",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // Companion Server Setup Guide (Multi-OS: Windows, macOS, Linux)
        var setupGuideTab by remember { mutableIntStateOf(0) }

        Text(
            text = "Companion Server Setup Guide",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth().testTag("setup_guide_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Beginner-Friendly Interactive Guided Wizards",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MintGreenPrimary
                )
                Text(
                    text = "Each wizard walks you through prompts one by one on your PC, checks dependencies, sets up permissions, and shows your pairing code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                TabRow(
                    selectedTabIndex = setupGuideTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = setupGuideTab == 0,
                        onClick = { setupGuideTab = 0 },
                        text = { Text("Windows", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = setupGuideTab == 1,
                        onClick = { setupGuideTab = 1 },
                        text = { Text("macOS", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = setupGuideTab == 2,
                        onClick = { setupGuideTab = 2 },
                        text = { Text("Linux", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = setupGuideTab == 3,
                        onClick = { setupGuideTab = 3 },
                        text = { Text("Any OS", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val copyCommand: (String, String) -> Unit = { cmd, osLabel ->
                    clipboardManager.setText(AnnotatedString(cmd))
                    Toast.makeText(context, "$osLabel command copied to clipboard!", Toast.LENGTH_SHORT).show()
                }

                when (setupGuideTab) {
                    0 -> {
                        // Windows
                        Text(
                            text = "Windows 10 / 11 (Command Prompt or PowerShell):",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "cd omnidesk-server && install.bat",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MintGreenPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "(Or simply double-click 'install.bat' in Windows Explorer!)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { copyCommand("cd omnidesk-server && install.bat", "Windows") },
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreenPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Windows Install Command", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Interactive Prompts You'll See on PC:\n" +
                                    "1. Python check: Detects Python 3.9+ (or provides download link)\n" +
                                    "2. Dependencies: Installs websockets, pyautogui, pynput, zeroconf\n" +
                                    "3. Firewall: Adds Windows Defender Firewall rules for Ports 8765/8766\n" +
                                    "4. Auto-Start: Optionally adds to Windows Startup folder\n" +
                                    "5. Desktop Shortcut: Creates 'Start OmniDesk.bat' on your desktop\n" +
                                    "6. Launch: Displays 6-digit pairing code and QR code in terminal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    1 -> {
                        // macOS
                        Text(
                            text = "macOS (Sonoma, Ventura, Monterey, Sequoia+):",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "cd omnidesk-server && ./install-mac.sh",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MintGreenPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { copyCommand("cd omnidesk-server && chmod +x install-mac.sh && ./install-mac.sh", "macOS") },
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreenPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy macOS Install Command", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Interactive Prompts You'll See on Mac:\n" +
                                    "1. Python detection: Uses Python 3 or offers xcode-select tools\n" +
                                    "2. Virtual Environment: Creates isolated .venv to keep Mac clean\n" +
                                    "3. System Permissions: Prompts and opens System Settings directly to Privacy & Security > Accessibility so you can toggle Terminal permission\n" +
                                    "4. Auto-Start: Optionally sets up launchd LaunchAgent for login\n" +
                                    "5. Launch: Starts server with pairing PIN and QR code",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    2 -> {
                        // Linux
                        Text(
                            text = "Linux (Mint, Ubuntu, Debian, Fedora, Arch, SUSE):",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "cd omnidesk-server && ./install.sh",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MintGreenPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { copyCommand("cd omnidesk-server && chmod +x install.sh && ./install.sh", "Linux") },
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreenPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Linux Install Command", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Interactive Prompts You'll See on Linux:\n" +
                                    "1. Package tools: Installs xdotool, playerctl, xclip, pulseaudio\n" +
                                    "2. Python venv: Installs isolated Python environment\n" +
                                    "3. UFW Firewall: Prompts to allow 8765/tcp, 8766/udp, 5353/udp\n" +
                                    "4. Uinput rules: Configures /dev/uinput group for virtual gamepad\n" +
                                    "5. Systemd Service: Enables user service to run in background\n" +
                                    "6. Launch: Shows live pairing PIN and QR code in terminal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    3 -> {
                        // Universal Python Wizard
                        Text(
                            text = "Universal Python Wizard (Works on ALL Operating Systems):",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "cd omnidesk-server && python3 setup.py",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MintGreenPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { copyCommand("cd omnidesk-server && python3 setup.py", "Universal Python") },
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreenPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Universal Setup Command", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "The cross-platform Python wizard (install_wizard.py) auto-detects Windows, macOS, or Linux, walks you through 5 simple [Y/n] questions, sets up permissions, and launches the server.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Master Command Reference Card (All OS)
        var showFullCommands by remember { mutableStateOf(false) }

        Text(
            text = "Master Command Reference (All OS)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("all_commands_ref_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MintGreenPrimary
                    )
                    Text(
                        text = "ALL_COMMANDS_ALL_OS.txt",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "A complete, standalone text file with every command for Windows, macOS, and Linux (firewalls, services, IP discovery, USB cable mode, process kill, and system actions) is saved in the project root.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val allCommandsSnippet = """
=====================================================
OMNIDESK - QUICK ONE-LINERS FOR ALL OS
=====================================================
# WINDOWS (CMD / PowerShell):
cd omnidesk-server && install.bat && run.bat
# Windows Firewall (Admin CMD):
netsh advfirewall firewall add rule name="OmniDesk" dir=in action=allow protocol=TCP localport=8765

# MACOS (Terminal):
cd omnidesk-server && chmod +x run-mac.sh && ./run-mac.sh
# Permission: System Settings > Privacy & Security > Accessibility

# LINUX (Ubuntu / Debian / Fedora / Arch / Mint):
cd omnidesk-server && chmod +x install.sh && ./install.sh
# Linux Firewall:
sudo ufw allow 8765/tcp && sudo ufw allow 8766/udp

# USB CABLE MODE (Zero-Latency / All OS):
adb reverse tcp:8765 tcp:8765
=====================================================
Full guide available in ALL_COMMANDS_ALL_OS.txt
""".trimIndent()
                            clipboardManager.setText(AnnotatedString(allCommandsSnippet))
                            Toast.makeText(context, "Commands copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MintGreenPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy One-Liners", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = { showFullCommands = !showFullCommands },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (showFullCommands) "Hide Cheatsheet" else "View Cheatsheet", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (showFullCommands) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = """
• WINDOWS:
  Install:    python -m pip install -r requirements.txt
  Firewall:   netsh advfirewall firewall add rule name="OmniDesk" dir=in action=allow protocol=TCP localport=8765
  Run:        run.bat (or: python server.py)
  Find PIN:   type "%USERPROFILE%\.config\linux-remote\current_pin.txt"
  Stop:       taskkill /F /IM python.exe

• MACOS:
  Install:    brew install python3 && ./venv/bin/pip install -r requirements.txt
  Run:        ./run-mac.sh
  Access:     Grant Terminal in Privacy & Security > Accessibility
  Find PIN:   cat ~/.config/linux-remote/current_pin.txt
  Kill:       pkill -f "python3.*server.py"

• LINUX:
  Install:    ./install.sh (sets up systemd user service)
  Status:     ./status.sh (or: systemctl --user status linux-remote.service)
  Firewall:   sudo ufw allow 8765/tcp && sudo ufw allow 8766/udp
  Restart:    systemctl --user restart linux-remote.service
  Find PIN:   cat ~/.config/linux-remote/current_pin.txt

• USB WIRED (All OS):
  Setup:      adb reverse tcp:8765 tcp:8765
""".trimIndent(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // About & Architecture
        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "OmniDesk for Android", fontWeight = FontWeight.Bold)
                Text(
                    text = "Universal Remote (Windows, macOS & Linux)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "High-performance peer-to-peer WebSocket connection over your local Wi-Fi. Supports mouse touchpad, live keyboard streaming, media playback, system actions, presentation remote, and power timer without internet or third-party servers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
