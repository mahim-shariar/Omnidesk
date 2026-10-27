package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.ConfirmDialog
import com.example.ui.theme.DangerRed
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.WarningOrange

@Composable
fun PcControlsScreen(
    onExecuteAction: (String) -> Unit,
    onWakeOnLanClick: () -> Unit = {},
    pcStatus: com.example.model.PcStatus = com.example.model.PcStatus()
) {
    var pendingAction by remember { mutableStateOf<String?>(null) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var isDestructive by remember { mutableStateOf(false) }

    val isWin = pcStatus.isWindows
    val isMac = pcStatus.isMac
    val isLinux = pcStatus.isLinux

    if (pendingAction != null) {
        ConfirmDialog(
            title = dialogTitle,
            message = dialogMessage,
            confirmButtonText = if (isDestructive) "Proceed" else "Confirm",
            isDestructive = isDestructive,
            onConfirm = {
                pendingAction?.let { onExecuteAction(it) }
                pendingAction = null
            },
            onDismiss = {
                pendingAction = null
            }
        )
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWide = maxWidth > 650.dp
        val horizontalPadding = if (isWide) 24.dp else 16.dp

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 165.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1000.dp)
                .padding(horizontal = horizontalPadding)
                .navigationBarsPadding()
                .testTag("pc_controls_screen"),
            contentPadding = PaddingValues(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            isWin -> "Windows System Controls"
                            isMac -> "macOS System Controls"
                            else -> "Linux System Controls"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    com.example.ui.components.OsBadge(
                        osType = pcStatus.osType,
                        osName = pcStatus.osDisplayName
                    )
                }
            }

            // Lock PC
            item {
                SystemActionCard(
                    title = if (isMac) "Lock Screen" else if (isWin) "Lock Workstation" else "Lock PC",
                    subtitle = when {
                        isWin -> "Lock Windows session"
                        isMac -> "Sleep display & lock"
                        else -> "Lock desktop session"
                    },
                    icon = Icons.Default.Lock,
                    color = TerminalCyan,
                    testTag = "action_lock",
                    onClick = { onExecuteAction("lock") }
                )
            }

            // Show Desktop / Mission Control
            item {
                SystemActionCard(
                    title = if (isMac) "Mission Control" else "Show Desktop",
                    subtitle = when {
                        isWin -> "Minimize all (Win+D)"
                        isMac -> "Show Desktop (Cmd+F3)"
                        else -> "Minimize all windows"
                    },
                    icon = Icons.Default.DesktopWindows,
                    color = MaterialTheme.colorScheme.primary,
                    testTag = "action_show_desktop",
                    onClick = { onExecuteAction("show_desktop") }
                )
            }

            // Take Screenshot
            item {
                SystemActionCard(
                    title = if (isWin) "Snipping Tool" else "Screenshot",
                    subtitle = when {
                        isWin -> "Windows Screen Clip"
                        isMac -> "Capture to clipboard"
                        else -> "Capture screen"
                    },
                    icon = Icons.Default.CameraAlt,
                    color = Color(0xFFFFB74D),
                    testTag = "action_screenshot",
                    onClick = { onExecuteAction("screenshot") }
                )
            }

            // Open Terminal
            item {
                SystemActionCard(
                    title = "Terminal",
                    subtitle = when {
                        isWin -> "Launch PowerShell / WT"
                        isMac -> "Launch macOS Terminal"
                        else -> "Launch bash terminal"
                    },
                    icon = Icons.Default.Terminal,
                    color = Color(0xFF64B5F6),
                    testTag = "action_terminal",
                    onClick = { onExecuteAction("open_terminal") }
                )
            }

            // Turn Off Screen (Display DPMS Sleep)
            item {
                SystemActionCard(
                    title = "Sleep Display",
                    subtitle = when {
                        isWin -> "Turn off PC monitors"
                        isMac -> "Sleep Mac displays"
                        else -> "Turn off monitors (DPMS)"
                    },
                    icon = Icons.Default.DesktopWindows,
                    color = Color(0xFF26A69A),
                    testTag = "action_display_off",
                    onClick = { onExecuteAction("display_off") }
                )
            }

            // System Monitor
            item {
                SystemActionCard(
                    title = if (isMac) "Activity Monitor" else "Task Manager",
                    subtitle = when {
                        isWin -> "Windows Task Manager"
                        isMac -> "macOS Activity Monitor"
                        else -> "Open System Monitor"
                    },
                    icon = Icons.Default.DesktopWindows,
                    color = Color(0xFF7E57C2),
                    testTag = "action_system_monitor",
                    onClick = { onExecuteAction("system_monitor") }
                )
            }

            // Shell Restart (Cinnamon / Explorer / Finder)
            item {
                val actionId = when {
                    isWin -> "explorer_restart"
                    isMac -> "finder_restart"
                    else -> "cinnamon_restart"
                }
                val label = when {
                    isWin -> "Restart Explorer"
                    isMac -> "Relaunch Finder"
                    else -> "Reload Cinnamon"
                }
                val desc = when {
                    isWin -> "Restart Windows Explorer shell without closing apps"
                    isMac -> "Relaunch macOS Finder and Dock"
                    else -> "Restart desktop shell"
                }
                SystemActionCard(
                    title = label,
                    subtitle = desc,
                    icon = Icons.Default.RestartAlt,
                    color = Color(0xFF29B6F6),
                    testTag = "action_cinnamon_restart",
                    onClick = {
                        dialogTitle = label
                        dialogMessage = "$desc. Open windows will not be closed."
                        isDestructive = false
                        pendingAction = actionId
                    }
                )
            }

            // Dangerous Actions Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Power & Session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }

            // Suspend
            item {
                SystemActionCard(
                    title = when {
                        isWin -> "Sleep Windows"
                        isMac -> "Sleep Mac"
                        else -> "Suspend PC"
                    },
                    subtitle = "Put computer to sleep",
                    icon = Icons.Default.NightsStay,
                    color = WarningOrange,
                    testTag = "action_suspend",
                    onClick = {
                        dialogTitle = "Put Computer to Sleep"
                        dialogMessage = "Are you sure you want to put this computer into sleep/suspend mode?"
                        isDestructive = false
                        pendingAction = "suspend"
                    }
                )
            }

            // Wake on LAN
            item {
                SystemActionCard(
                    title = "Wake-on-LAN",
                    subtitle = "Send magic packet",
                    icon = Icons.Default.Power,
                    color = MaterialTheme.colorScheme.primary,
                    testTag = "action_wol",
                    onClick = onWakeOnLanClick
                )
            }

            // Restart
            item {
                SystemActionCard(
                    title = when {
                        isWin -> "Restart Windows"
                        isMac -> "Restart Mac"
                        else -> "Restart PC"
                    },
                    subtitle = "Reboot computer",
                    icon = Icons.Default.RestartAlt,
                    color = WarningOrange,
                    testTag = "action_restart",
                    onClick = {
                        dialogTitle = "Restart Computer"
                        dialogMessage = "Are you sure you want to reboot your computer now? Any unsaved work may be lost."
                        isDestructive = true
                        pendingAction = "restart"
                    }
                )
            }

            // Shut Down
            item(span = { GridItemSpan(maxLineSpan) }) {
                SystemActionCard(
                    title = when {
                        isWin -> "Shut Down Windows"
                        isMac -> "Shut Down Mac"
                        else -> "Shut Down PC"
                    },
                    subtitle = "Safely power off computer",
                    icon = Icons.Default.PowerSettingsNew,
                    color = DangerRed,
                    testTag = "action_shutdown",
                    onClick = {
                        dialogTitle = "Shut Down Computer"
                        dialogMessage = "Are you sure you want to shut down this computer? You will not be able to control it until it is powered back on."
                        isDestructive = true
                        pendingAction = "shutdown"
                    }
                )
            }
        }
    }
}

@Composable
fun SystemActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        color.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.16f))
                    .border(1.dp, color.copy(alpha = 0.40f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
