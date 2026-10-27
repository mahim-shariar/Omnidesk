package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.CustomShortcutEntity
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MintGreenPrimary

data class ShortcutPreset(
    val label: String,
    val keys: String,
    val description: String,
    val osBadge: String,
    val colorHex: Long
)

@Composable
fun CustomShortcutsScreen(
    shortcuts: List<CustomShortcutEntity>,
    onTriggerShortcut: (keys: String) -> Unit,
    onAddShortcut: (label: String, keys: String, description: String, colorHex: Long) -> Unit,
    onDeleteShortcut: (id: Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedOsIndex by remember { mutableIntStateOf(0) }

    val osCategories = listOf("All OS", "Windows", "macOS", "Linux")

    // Global curated presets for each OS ecosystem
    val universalPresets = listOf(
        ShortcutPreset("New Browser Tab", "ctrl+t", "Open blank web tab", "Universal", 0xFF00E5FF),
        ShortcutPreset("Close Tab / Window", "ctrl+w", "Close active tab", "Universal", 0xFFFF5252),
        ShortcutPreset("Reopen Closed Tab", "ctrl+shift+t", "Restore last closed tab", "Universal", 0xFF69F0AE),
        ShortcutPreset("Copy Selection", "ctrl+c", "Copy to clipboard", "Universal", 0xFF448AFF),
        ShortcutPreset("Paste Clipboard", "ctrl+v", "Paste from clipboard", "Universal", 0xFFB388FF),
        ShortcutPreset("Select All", "ctrl+a", "Select all document contents", "Universal", 0xFFFFD740),
        ShortcutPreset("Refresh Page / App", "f5", "Hard reload current view", "Universal", 0xFF00E676),
        ShortcutPreset("Toggle Fullscreen", "f11", "Maximize view to display edges", "Universal", 0xFFFF4081)
    )

    val windowsPresets = listOf(
        ShortcutPreset("File Explorer", "super+e", "Open Windows File Explorer", "Windows", 0xFF0078D4),
        ShortcutPreset("Show Desktop", "super+d", "Minimize all open windows", "Windows", 0xFF00B4D8),
        ShortcutPreset("Lock Workstation", "super+l", "Instantly lock Windows session", "Windows", 0xFFFF6B6B),
        ShortcutPreset("Snipping Tool", "super+shift+s", "Open screen snip & capture", "Windows", 0xFFFFB703),
        ShortcutPreset("Task Manager", "ctrl+shift+escape", "Open Windows Task Manager", "Windows", 0xFF7209B7),
        ShortcutPreset("Task View", "super+tab", "Show virtual desktops & tasks", "Windows", 0xFF4361EE),
        ShortcutPreset("Run Dialog", "super+r", "Open Windows Run box", "Windows", 0xFF4895EF),
        ShortcutPreset("Security Screen", "ctrl+alt+delete", "Open Windows lock & security", "Windows", 0xFFD90429)
    )

    val macPresets = listOf(
        ShortcutPreset("Spotlight Search", "command+space", "Open Spotlight global search", "macOS", 0xFFE0E0E0),
        ShortcutPreset("App Switcher", "command+tab", "Cycle through running apps", "macOS", 0xFF00C49F),
        ShortcutPreset("Force Quit Menu", "command+alt+escape", "Open Force Quit Applications", "macOS", 0xFFFF4D4F),
        ShortcutPreset("Screenshot Snip", "command+shift+4", "Capture selected screen region", "macOS", 0xFFFFA940),
        ShortcutPreset("Quit Application", "command+q", "Completely exit frontmost app", "macOS", 0xFFF5222D),
        ShortcutPreset("Close Window", "command+w", "Close active Finder/app window", "macOS", 0xFFFAAD14),
        ShortcutPreset("Hide Application", "command+h", "Hide frontmost application", "macOS", 0xFF1890FF),
        ShortcutPreset("Finder New Window", "command+alt+space", "Open new Finder search window", "macOS", 0xFF722ED1)
    )

    val linuxPresets = listOf(
        ShortcutPreset("Open Terminal", "ctrl+alt+t", "Launch default bash / zsh terminal", "Linux", 0xFF00E676),
        ShortcutPreset("Show Desktop", "super+d", "Minimize all application windows", "Linux", 0xFF00B0FF),
        ShortcutPreset("Lock Screen", "super+l", "Lock desktop session", "Linux", 0xFFFF1744),
        ShortcutPreset("App Overview", "super", "Open GNOME / Cinnamon menu", "Linux", 0xFFFFAB00),
        ShortcutPreset("Run Command Prompt", "alt+f2", "Open quick runner dialog", "Linux", 0xFF7C4DFF),
        ShortcutPreset("System Monitor", "ctrl+shift+escape", "View running Linux processes", "Linux", 0xFF2979FF),
        ShortcutPreset("Switch Window", "alt+tab", "Cycle through open windows", "Linux", 0xFF00E5FF),
        ShortcutPreset("Terminal Clear", "ctrl+l", "Clear terminal screen buffer", "Linux", 0xFF1DE9B6)
    )

    val displayedPresets = when (selectedOsIndex) {
        1 -> windowsPresets
        2 -> macPresets
        3 -> linuxPresets
        else -> universalPresets
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .navigationBarsPadding()
            .testTag("custom_shortcuts_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Global Shortcuts & Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Instant 1-tap triggers for Windows, macOS & Linux",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Custom")
            }
        }

        // OS Perspective Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            osCategories.forEachIndexed { index, title ->
                FilterChip(
                    selected = selectedOsIndex == index,
                    onClick = { selectedOsIndex = index },
                    label = { Text(title, fontWeight = if (selectedOsIndex == index) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Preset Shortcuts Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${osCategories[selectedOsIndex]} Presets (Tap to Run)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "1-Tap Trigger",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Presets Cards
            items(displayedPresets, key = { "preset_${it.keys}_${it.label}" }) { preset ->
                PresetShortcutCard(
                    preset = preset,
                    onTrigger = { onTriggerShortcut(preset.keys) },
                    onSave = {
                        onAddShortcut(preset.label, preset.keys, preset.description, preset.colorHex)
                    }
                )
            }

            // User's Custom Saved Shortcuts Section
            item {
                Text(
                    text = "My Custom Saved Shortcuts (${shortcuts.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }

            if (shortcuts.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No custom shortcuts saved yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap '+ Bookmark' on any preset above or 'Custom' to create your own combinations.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(shortcuts, key = { it.id }) { shortcut ->
                    CustomShortcutCard(
                        shortcut = shortcut,
                        onTrigger = { onTriggerShortcut(shortcut.keys) },
                        onDelete = { onDeleteShortcut(shortcut.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddShortcutDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { label, keys, desc, color ->
                onAddShortcut(label, keys, desc, color)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun PresetShortcutCard(
    preset: ShortcutPreset,
    onTrigger: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onTrigger)
            .testTag("preset_${preset.label.replace(" ", "_").lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(preset.colorHex).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = Color(preset.colorHex),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = preset.osBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = preset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = preset.keys.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onSave,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = "Save to Custom",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomShortcutCard(
    shortcut: CustomShortcutEntity,
    onTrigger: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onTrigger)
            .testTag("custom_shortcut_${shortcut.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(shortcut.colorHex).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(shortcut.colorHex),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = shortcut.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = shortcut.keys.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AddShortcutDialog(
    onDismiss: () -> Unit,
    onConfirm: (label: String, keys: String, description: String, colorHex: Long) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var keys by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(0xFF00E5FF) }

    val colors = listOf(0xFF00E5FF, 0xFF00E676, 0xFFFFB703, 0xFFFF5252, 0xFFB388FF, 0xFF448AFF)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Custom Shortcut") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Name / Action") },
                    placeholder = { Text("e.g. Open Files, Snipping Tool") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = keys,
                    onValueChange = { keys = it },
                    label = { Text("Key Combination") },
                    placeholder = { Text("e.g. super+e, command+space, ctrl+alt+t") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Tip: Supports 'ctrl', 'alt', 'shift', 'super' (Windows key), 'command' (Mac Cmd), function keys ('f1'-'f12'), etc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { col ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(col))
                                .clickable { selectedColor = col },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == col) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isNotEmpty() && keys.isNotEmpty()) {
                        onConfirm(label, keys, description, selectedColor)
                    }
                },
                enabled = label.isNotEmpty() && keys.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
