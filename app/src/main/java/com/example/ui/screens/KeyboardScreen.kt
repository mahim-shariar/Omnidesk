package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.DarkBorderShine
import com.example.ui.theme.GlassBorderSubtle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyboardScreen(
    onSendText: (String) -> Unit,
    onSendKey: (String) -> Unit,
    onSendShortcut: (String) -> Unit,
    pcStatus: com.example.model.PcStatus = com.example.model.PcStatus()
) {
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val isMac = pcStatus.isMac
    val isWin = pcStatus.isWindows

    // Active typing mode: 0 = Live Typing, 1 = Compose & Send (Multiline Buffer)
    var selectedModeTab by remember { mutableIntStateOf(0) }

    // Visible Live Typing input text (Characters remain fully visible as you type!)
    var liveText by remember { mutableStateOf("") }
    var autoClearOnEnter by remember { mutableStateOf(false) }

    // Real-time visual feedback status & recent history
    var lastActionLabel by remember { mutableStateOf<String?>(null) }
    var recentTokens by remember { mutableStateOf<List<String>>(emptyList()) }

    fun addRecentToken(token: String) {
        recentTokens = (listOf(token) + recentTokens).take(4)
    }

    // Buffer mode text state for multi-line scripting / pasting
    var bufferText by remember { mutableStateOf("") }

    // Modifier keys state (Sticky toggles)
    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }
    var shiftActive by remember { mutableStateOf(false) }
    var superActive by remember { mutableStateOf(false) }

    LaunchedEffect(selectedModeTab) {
        if (selectedModeTab == 0) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .testTag("keyboard_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode Selector: Live Typing vs Compose & Send
        TabRow(
            selectedTabIndex = selectedModeTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedModeTab == 0,
                onClick = { selectedModeTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Live Typing", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedModeTab == 1,
                onClick = { selectedModeTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compose & Send", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // --- MODE 0: LIVE STREAM TYPING ---
        if (selectedModeTab == 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(DarkBorderShine, GlassBorderSubtle)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header with Real-time Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Keyboard (Visible & Responsive)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        if (lastActionLabel != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = lastActionLabel ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary Live Text Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = liveText,
                            onValueChange = { newText ->
                                val oldText = liveText
                                val oldLen = oldText.length
                                val newLen = newText.length

                                if (newLen > oldLen) {
                                    // User typed new character(s) or word
                                    val added = newText.substring(oldLen)
                                    onSendText(added)
                                    lastActionLabel = "Sent: \"$added\""
                                    addRecentToken(added)
                                } else if (newLen < oldLen) {
                                    // User pressed backspace on mobile keyboard
                                    val deletedCount = oldLen - newLen
                                    repeat(deletedCount) {
                                        onSendKey("BACKSPACE")
                                    }
                                    lastActionLabel = if (deletedCount > 1) "⌫ Del x$deletedCount" else "⌫ Backspace"
                                    addRecentToken("⌫ x$deletedCount")
                                }

                                // Retain the visible text so the input bar NEVER feels empty!
                                liveText = newText
                            },
                            placeholder = { Text("Type here to stream live to PC...") },
                            singleLine = true,
                            trailingIcon = {
                                if (liveText.isNotEmpty()) {
                                    IconButton(onClick = {
                                        liveText = ""
                                        lastActionLabel = "Cleared input"
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear input",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSendKey("ENTER")
                                    lastActionLabel = "↵ Enter"
                                    addRecentToken("↵ Enter")
                                    if (autoClearOnEnter) {
                                        liveText = ""
                                    }
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown) {
                                        when (event.key) {
                                            Key.Backspace -> {
                                                onSendKey("BACKSPACE")
                                                lastActionLabel = "⌫ Backspace"
                                                addRecentToken("⌫")
                                                if (liveText.isNotEmpty()) {
                                                    liveText = liveText.dropLast(1)
                                                }
                                                true
                                            }
                                            Key.Enter -> {
                                                onSendKey("ENTER")
                                                lastActionLabel = "↵ Enter"
                                                addRecentToken("↵ Enter")
                                                if (autoClearOnEnter) {
                                                    liveText = ""
                                                }
                                                true
                                            }
                                            Key.Tab -> {
                                                onSendKey("TAB")
                                                lastActionLabel = "⇥ Tab"
                                                addRecentToken("⇥ Tab")
                                                true
                                            }
                                            Key.Escape -> {
                                                onSendKey("ESC")
                                                lastActionLabel = "Esc"
                                                addRecentToken("Esc")
                                                true
                                            }
                                            Key.DirectionUp -> {
                                                onSendKey("UP")
                                                lastActionLabel = "↑ Up"
                                                true
                                            }
                                            Key.DirectionDown -> {
                                                onSendKey("DOWN")
                                                lastActionLabel = "↓ Down"
                                                true
                                            }
                                            Key.DirectionLeft -> {
                                                onSendKey("LEFT")
                                                lastActionLabel = "← Left"
                                                true
                                            }
                                            Key.DirectionRight -> {
                                                onSendKey("RIGHT")
                                                lastActionLabel = "→ Right"
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                                .testTag("keyboard_text_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Dedicated Fast Backspace Button (Works even if input box is empty!)
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSendKey("BACKSPACE")
                                lastActionLabel = "⌫ Backspace"
                                addRecentToken("⌫")
                                if (liveText.isNotEmpty()) {
                                    liveText = liveText.dropLast(1)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .testTag("keyboard_backspace_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Send Backspace",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Dedicated Enter Button
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSendKey("ENTER")
                                lastActionLabel = "↵ Enter"
                                addRecentToken("↵ Enter")
                                if (autoClearOnEnter) {
                                    liveText = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("keyboard_enter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardReturn,
                                contentDescription = "Send Enter",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stream Ticker & Options Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Recent Sent Keystrokes Pill Stream
                        if (recentTokens.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sent:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                recentTokens.forEach { token ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    ) {
                                        Text(
                                            text = token,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Keystrokes stream immediately to PC",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        // Auto-clear after Enter toggle chip
                        FilterChip(
                            selected = autoClearOnEnter,
                            onClick = { autoClearOnEnter = !autoClearOnEnter },
                            label = {
                                Text(
                                    text = "Auto-clear on ↵",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // --- MODE 1: COMPOSE & SEND (BUFFER / SCRIPT / PASTE) ---
        if (selectedModeTab == 1) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Compose Multi-line Command or Script",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = bufferText,
                        onValueChange = { bufferText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        placeholder = { Text("Write command, shell script, or paste text here before sending to PC...") },
                        maxLines = 5,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { bufferText = "" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Clear")
                        }

                        Button(
                            onClick = {
                                if (bufferText.isNotEmpty()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSendText(bufferText)
                                    lastActionLabel = "Sent ${bufferText.length} chars"
                                    addRecentToken("${bufferText.length} chars")
                                }
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send")
                        }

                        Button(
                            onClick = {
                                if (bufferText.isNotEmpty()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSendText(bufferText)
                                    onSendKey("ENTER")
                                    lastActionLabel = "Sent + ↵ Enter"
                                    addRecentToken("Command + ↵")
                                    bufferText = ""
                                }
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.KeyboardReturn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send + ↵")
                        }
                    }
                }
            }
        }

        // --- STICKY MODIFIER KEYS (Ctrl, Alt, Shift, Super) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Modifier Keys",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                val isMac = pcStatus.isMac
                val isWin = pcStatus.isWindows
                val superLabel = when {
                    isMac -> "CMD ⌘"
                    isWin -> "WIN ⊞"
                    else -> "SUPER ❖"
                }
                val altLabel = if (isMac) "OPT ⌥" else "ALT"
                val superKeyName = if (isMac) "CMD" else "SUPER"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModifierChip(label = "CTRL", active = ctrlActive) {
                        ctrlActive = !ctrlActive
                        onSendKey(if (ctrlActive) "CTRL_DOWN" else "CTRL_UP")
                    }
                    ModifierChip(label = altLabel, active = altActive) {
                        altActive = !altActive
                        onSendKey(if (altActive) "ALT_DOWN" else "ALT_UP")
                    }
                    ModifierChip(label = "SHIFT", active = shiftActive) {
                        shiftActive = !shiftActive
                        onSendKey(if (shiftActive) "SHIFT_DOWN" else "SHIFT_UP")
                    }
                    ModifierChip(label = superLabel, active = superActive) {
                        superActive = !superActive
                        onSendKey(if (superActive) "${superKeyName}_DOWN" else "${superKeyName}_UP")
                    }
                }
            }
        }

        // --- SPECIAL KEYS GRID & NAVIGATION ARROWS ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Special & Navigation Keys",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Primary row: ESC | TAB | ENTER | BACKSPACE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeyButton(label = "ESC", modifier = Modifier.weight(1f)) {
                        onSendKey("ESC")
                        lastActionLabel = "Esc"
                    }
                    KeyButton(label = "TAB ⇥", modifier = Modifier.weight(1f)) {
                        onSendKey("TAB")
                        lastActionLabel = "⇥ Tab"
                    }
                    KeyButton(
                        label = "ENTER ↵",
                        modifier = Modifier.weight(1.3f),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        onSendKey("ENTER")
                        lastActionLabel = "↵ Enter"
                    }
                    KeyButton(
                        label = "⌫ BACK",
                        modifier = Modifier.weight(1.3f),
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        onSendKey("BACKSPACE")
                        lastActionLabel = "⌫ Backspace"
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigation Row: SPACE | HOME | END | PG UP | PG DN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeyButton(label = "␣ SPACE", modifier = Modifier.weight(1.4f)) {
                        onSendKey("SPACE")
                        lastActionLabel = "␣ Space"
                    }
                    KeyButton(label = "HOME", modifier = Modifier.weight(1f)) {
                        onSendKey("HOME")
                        lastActionLabel = "Home"
                    }
                    KeyButton(label = "END", modifier = Modifier.weight(1f)) {
                        onSendKey("END")
                        lastActionLabel = "End"
                    }
                    KeyButton(label = "PG UP", modifier = Modifier.weight(1f)) {
                        onSendKey("PAGE_UP")
                        lastActionLabel = "Page Up"
                    }
                    KeyButton(label = "PG DN", modifier = Modifier.weight(1f)) {
                        onSendKey("PAGE_DOWN")
                        lastActionLabel = "Page Down"
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Directional Arrow Keys Cross
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSendKey("UP")
                            lastActionLabel = "↑ Up"
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Up")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSendKey("LEFT")
                                lastActionLabel = "← Left"
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Left")
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSendKey("DOWN")
                                lastActionLabel = "↓ Down"
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Down")
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSendKey("RIGHT")
                                lastActionLabel = "→ Right"
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Right")
                        }
                    }
                }
            }
        }

        // --- QUICK SYSTEM SHORTCUTS (OS-ADAPTIVE) ---
        val shortcutsTitle = when {
            isMac -> "macOS Shortcuts"
            isWin -> "Windows Shortcuts"
            else -> "Linux Shortcuts"
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = shortcutsTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    com.example.ui.components.OsBadge(
                        osType = pcStatus.osType,
                        osName = pcStatus.osDisplayName
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when {
                        isMac -> {
                            ShortcutPill(label = "Cmd+C (Copy)") { onSendShortcut("cmd+c") }
                            ShortcutPill(label = "Cmd+V (Paste)") { onSendShortcut("cmd+v") }
                            ShortcutPill(label = "Cmd+X (Cut)") { onSendShortcut("cmd+x") }
                            ShortcutPill(label = "Cmd+Z (Undo)") { onSendShortcut("cmd+z") }
                            ShortcutPill(label = "Cmd+A (Select All)") { onSendShortcut("cmd+a") }
                            ShortcutPill(label = "Cmd+Space (Spotlight)") { onSendShortcut("cmd+space") }
                            ShortcutPill(label = "Cmd+Tab (App Switcher)") { onSendShortcut("cmd+tab") }
                            ShortcutPill(label = "Cmd+W (Close Tab)") { onSendShortcut("cmd+w") }
                            ShortcutPill(label = "Cmd+Q (Quit App)") { onSendShortcut("cmd+q") }
                            ShortcutPill(label = "Cmd+Shift+4 (Screenshot)") { onSendShortcut("cmd+shift+4") }
                            ShortcutPill(label = "Ctrl+Cmd+Q (Lock Screen)") { onSendShortcut("ctrl+cmd+q") }
                        }
                        isWin -> {
                            ShortcutPill(label = "Ctrl+C (Copy)") { onSendShortcut("ctrl+c") }
                            ShortcutPill(label = "Ctrl+V (Paste)") { onSendShortcut("ctrl+v") }
                            ShortcutPill(label = "Ctrl+X (Cut)") { onSendShortcut("ctrl+x") }
                            ShortcutPill(label = "Ctrl+Z (Undo)") { onSendShortcut("ctrl+z") }
                            ShortcutPill(label = "Ctrl+A (Select All)") { onSendShortcut("ctrl+a") }
                            ShortcutPill(label = "Win (Start Menu)") { onSendKey("WIN") }
                            ShortcutPill(label = "Win+D (Desktop)") { onSendShortcut("win+d") }
                            ShortcutPill(label = "Win+E (Explorer)") { onSendShortcut("win+e") }
                            ShortcutPill(label = "Win+L (Lock Screen)") { onSendShortcut("win+l") }
                            ShortcutPill(label = "Alt+Tab (Switch App)") { onSendShortcut("alt+tab") }
                            ShortcutPill(label = "Alt+F4 (Close Window)") { onSendShortcut("alt+f4") }
                            ShortcutPill(label = "Ctrl+Shift+Esc (Task Manager)") { onSendShortcut("ctrl+shift+esc") }
                        }
                        else -> {
                            ShortcutPill(label = "Ctrl+C (Copy)") { onSendShortcut("ctrl+c") }
                            ShortcutPill(label = "Ctrl+V (Paste)") { onSendShortcut("ctrl+v") }
                            ShortcutPill(label = "Ctrl+X (Cut)") { onSendShortcut("ctrl+x") }
                            ShortcutPill(label = "Ctrl+Z (Undo)") { onSendShortcut("ctrl+z") }
                            ShortcutPill(label = "Ctrl+A (Select All)") { onSendShortcut("ctrl+a") }
                            ShortcutPill(label = "Alt+Tab (Switch App)") { onSendShortcut("alt+tab") }
                            ShortcutPill(label = "Alt+F4 (Close Window)") { onSendShortcut("alt+f4") }
                            ShortcutPill(label = "Super (Menu)") { onSendKey("SUPER") }
                            ShortcutPill(label = "Super+D (Desktop)") { onSendShortcut("super+d") }
                            ShortcutPill(label = "Ctrl+Alt+T (Terminal)") { onSendShortcut("ctrl+alt+t") }
                            ShortcutPill(label = "Ctrl+L (Clear Screen)") { onSendShortcut("ctrl+l") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModifierChip(label: String, active: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    FilterChip(
        selected = active,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        label = {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(10.dp)
    )
}

@Composable
fun KeyButton(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }),
        color = containerColor ?: MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ShortcutPill(label: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
