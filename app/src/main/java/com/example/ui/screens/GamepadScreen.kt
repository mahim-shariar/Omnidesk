package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.gamepad.ControlLayoutConfig
import com.example.ui.gamepad.GamepadControlId
import com.example.ui.gamepad.GamepadLayoutState
import com.example.ui.gamepad.GamepadLayoutStorage
import com.example.ui.gamepad.GamepadPreset
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorderShine
import com.example.ui.theme.DarkCardElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceGlass
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MintAccent
import com.example.ui.theme.MintGreenLight
import com.example.ui.theme.MintGreenPrimary
import com.example.ui.theme.MintNeon
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun GamepadScreen(
    onButtonPress: (String) -> Unit,
    onStickMove: (stick: String, dx: Float, dy: Float) -> Unit,
    onExit: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity

    // 1. Force landscape orientation when Gamepad is open, restore previous on exit
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    // 2. Intercept hardware back button to exit Gamepad
    BackHandler {
        onExit()
    }

    // Load saved layout state
    var layoutState by remember {
        mutableStateOf(GamepadLayoutStorage.loadLayout(context))
    }

    // UI Customization states
    var isEditMode by remember { mutableStateOf(false) }
    var showPresetsDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedEditingControl by remember { mutableStateOf<GamepadControlId?>(null) }
    var hudExpanded by remember { mutableStateOf(true) }

    fun playHaptic() {
        if (layoutState.hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun handleButton(btn: String) {
        playHaptic()
        onButtonPress(btn)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        DarkSurfaceVariant.copy(alpha = 0.85f),
                        DarkBackground
                    ),
                    center = Offset(0.5f, 0.5f),
                    radius = 1200f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("gamepad_screen")
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Decorative subtle gaming grid pattern
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val step = 60.dp.toPx()
                    val gridColor = Color(0x0800F5A0)
                    var x = 0f
                    while (x < size.width) {
                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
                        x += step
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
                        y += step
                    }
                }
        )

        // Render each customizable control group based on layoutState
        layoutState.controls.forEach { (controlId, config) ->
            if (config.visible || isEditMode) {
                val posX = (screenWidth * config.normX)
                val posY = (screenHeight * config.normY)

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (posX.toPx() - (100.dp.toPx() * config.scale / 2)).roundToInt(),
                                (posY.toPx() - (100.dp.toPx() * config.scale / 2)).roundToInt()
                            )
                        }
                        .scale(config.scale)
                        .alpha(if (!config.visible) 0.35f else layoutState.opacity)
                        .then(
                            if (isEditMode) {
                                Modifier
                                    .border(
                                        width = if (selectedEditingControl == controlId) 2.dp else 1.5.dp,
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                if (selectedEditingControl == controlId) MintNeon else CyberCyan,
                                                MintGreenPrimary
                                            )
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(6.dp)
                                    .pointerInput(controlId) {
                                        detectDragGestures(
                                            onDragStart = {
                                                selectedEditingControl = controlId
                                                playHaptic()
                                            },
                                            onDragEnd = {
                                                // Save updated positions immediately
                                                GamepadLayoutStorage.saveLayout(context, layoutState)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val newNormX = (config.normX + dragAmount.x / screenWidth.toPx()).coerceIn(0.08f, 0.92f)
                                                val newNormY = (config.normY + dragAmount.y / screenHeight.toPx()).coerceIn(0.12f, 0.90f)
                                                val updatedMap = layoutState.controls.toMutableMap()
                                                updatedMap[controlId] = config.copy(normX = newNormX, normY = newNormY)
                                                layoutState = layoutState.copy(controls = updatedMap)
                                            }
                                        )
                                    }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Control Component rendering
                    when (controlId) {
                        GamepadControlId.SHOULDER_LEFT -> {
                            ShoulderTriggersCluster(
                                isLeft = true,
                                onButtonPress = ::handleButton,
                                isEditMode = isEditMode
                            )
                        }

                        GamepadControlId.SHOULDER_RIGHT -> {
                            ShoulderTriggersCluster(
                                isLeft = false,
                                onButtonPress = ::handleButton,
                                isEditMode = isEditMode
                            )
                        }

                        GamepadControlId.MENU_BUTTONS -> {
                            MenuButtonsCluster(
                                onButtonPress = ::handleButton,
                                isEditMode = isEditMode
                            )
                        }

                        GamepadControlId.DPAD -> {
                            TactileDPad(
                                onDirectionPress = ::handleButton,
                                isEditMode = isEditMode
                            )
                        }

                        GamepadControlId.LEFT_STICK -> {
                            PremiumVirtualJoystick(
                                label = "L-STICK",
                                onMove = { dx, dy -> onStickMove("left", dx, dy) },
                                onClick = { handleButton("L3") },
                                isEditMode = isEditMode
                            )
                        }

                        GamepadControlId.RIGHT_STICK -> {
                            PremiumVirtualJoystick(
                                label = "R-STICK",
                                onMove = { dx, dy -> onStickMove("right", dx, dy) },
                                onClick = { handleButton("R3") },
                                isEditMode = isEditMode
                            )
                        }

                        GamepadControlId.ABXY -> {
                            JewelAbxyDiamond(
                                onButtonPress = ::handleButton,
                                isEditMode = isEditMode
                            )
                        }

                        GamepadControlId.TURBO -> {
                            TurboSpecialButton(
                                onButtonPress = ::handleButton,
                                isEditMode = isEditMode
                            )
                        }
                    }

                    // Edit mode overlay overlay badge
                    if (isEditMode) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-8).dp)
                                .clickable {
                                    selectedEditingControl = controlId
                                },
                            shape = CircleShape,
                            color = if (selectedEditingControl == controlId) MintNeon else CyberCyan,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Edit Control",
                                tint = Color.Black,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Top Floating HUD & Control Bar (Glassmorphic & Collapsible)
        AnimatedVisibility(
            visible = hudExpanded,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurfaceGlass,
                border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(DarkBorderShine, GlassBorder))),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back / Exit Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.clickable { onExit() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Exit Gamepad",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Exit",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x2210B981),
                            border = BorderStroke(1.dp, Color(0x4410B981))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MintNeon)
                                )
                                Text(
                                    text = "GAMEPAD (LANDSCAPE)",
                                    color = MintNeon,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    }

                    // Center: Layout Preset Info
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.clickable { showPresetsDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gamepad,
                                contentDescription = "Preset",
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = layoutState.preset.label,
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Right Actions: Customize / Edit Toggle + Settings
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Customize Mode Toggle Button (Prominent!)
                        val editBtnBg = if (isEditMode) MintNeon else MaterialTheme.colorScheme.primary
                        val editBtnText = if (isEditMode) Color.Black else Color.White

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = editBtnBg,
                            modifier = Modifier
                                .clickable {
                                    isEditMode = !isEditMode
                                    if (!isEditMode) {
                                        selectedEditingControl = null
                                        GamepadLayoutStorage.saveLayout(context, layoutState)
                                    }
                                    playHaptic()
                                }
                                .testTag("btn_customize_gamepad")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.OpenWith,
                                    contentDescription = "Customize Layout",
                                    tint = editBtnText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isEditMode) "Done Editing" else "Customize Layout",
                                    color = editBtnText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Presets Button
                        IconButton(
                            onClick = { showPresetsDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = "Presets",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Settings Button
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Minimize HUD button
                        IconButton(
                            onClick = { hudExpanded = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hide HUD",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Show mini floating button when HUD is collapsed
        if (!hudExpanded) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .clickable { hudExpanded = true },
                shape = CircleShape,
                color = DarkSurfaceGlass,
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = "Show HUD",
                        tint = MintGreenPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text("Menu", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Edit Mode Helper Banner at bottom center
        if (isEditMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xDD0B0F14),
                border = BorderStroke(1.dp, MintNeon)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenWith,
                        contentDescription = null,
                        tint = MintNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Drag any control to reposition. Tap its badge to resize or hide.",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    TextButton(
                        onClick = {
                            layoutState = GamepadLayoutStorage.resetLayout(context, layoutState.preset)
                            playHaptic()
                        }
                    ) {
                        Text("Reset Layout", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Dialog for selected control properties in Edit Mode
        selectedEditingControl?.let { ctrlId ->
            val config = layoutState.controls[ctrlId] ?: ControlLayoutConfig(ctrlId, 0.5f, 0.5f)
            AlertDialog(
                onDismissRequest = { selectedEditingControl = null },
                containerColor = DarkSurface,
                title = {
                    Text(
                        text = "Customize ${ctrlId.displayName}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Size Scale: ${(config.scale * 100).toInt()}%",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(0.8f, 1.0f, 1.2f, 1.4f).forEach { s ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val updated = layoutState.controls.toMutableMap()
                                            updated[ctrlId] = config.copy(scale = s)
                                            layoutState = layoutState.copy(controls = updated)
                                            GamepadLayoutStorage.saveLayout(context, layoutState)
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (config.scale == s) MintGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, GlassBorder)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${(s * 100).toInt()}%",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (config.scale == s) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Visible on Screen", color = Color.White, fontSize = 14.sp)
                            Switch(
                                checked = config.visible,
                                onCheckedChange = { isVis ->
                                    val updated = layoutState.controls.toMutableMap()
                                    updated[ctrlId] = config.copy(visible = isVis)
                                    layoutState = layoutState.copy(controls = updated)
                                    GamepadLayoutStorage.saveLayout(context, layoutState)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MintNeon,
                                    checkedTrackColor = MintGreenPrimary.copy(alpha = 0.5f)
                                )
                            )
                        }

                        TextButton(
                            onClick = {
                                val defaults = GamepadLayoutStorage.getPresetDefaults(layoutState.preset)
                                val defaultForThis = defaults[ctrlId]
                                if (defaultForThis != null) {
                                    val updated = layoutState.controls.toMutableMap()
                                    updated[ctrlId] = defaultForThis
                                    layoutState = layoutState.copy(controls = updated)
                                    GamepadLayoutStorage.saveLayout(context, layoutState)
                                }
                                selectedEditingControl = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reset this button to default", color = CyberCyan)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedEditingControl = null }) {
                        Text("Done", color = MintGreenPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Preset Selector Modal
        if (showPresetsDialog) {
            AlertDialog(
                onDismissRequest = { showPresetsDialog = false },
                containerColor = DarkSurface,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Gamepad, contentDescription = null, tint = MintGreenPrimary)
                        Text("Gamepad Presets", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GamepadPreset.values().forEach { preset ->
                            val isSelected = layoutState.preset == preset
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MintGreenPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) MintGreenPrimary else GlassBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        layoutState = GamepadLayoutStorage.resetLayout(context, preset)
                                        showPresetsDialog = false
                                        playHaptic()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Gamepad,
                                        contentDescription = null,
                                        tint = if (isSelected) MintNeon else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = preset.label,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) MintNeon else Color.White
                                        )
                                        Text(
                                            text = preset.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPresetsDialog = false }) {
                        Text("Close", color = MintGreenPrimary)
                    }
                }
            )
        }

        // Settings Dialog (Haptics, Opacity, Reset)
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                containerColor = DarkSurface,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = MintGreenPrimary)
                        Text("Gamepad Preferences", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Vibration, contentDescription = null, tint = CyberCyan)
                                Text("Haptic Feedback", color = Color.White)
                            }
                            Switch(
                                checked = layoutState.hapticsEnabled,
                                onCheckedChange = {
                                    layoutState = layoutState.copy(hapticsEnabled = it)
                                    GamepadLayoutStorage.saveLayout(context, layoutState)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = MintNeon)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Button Opacity: ${(layoutState.opacity * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Slider(
                                value = layoutState.opacity,
                                onValueChange = {
                                    layoutState = layoutState.copy(opacity = it)
                                    GamepadLayoutStorage.saveLayout(context, layoutState)
                                },
                                valueRange = 0.4f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MintNeon,
                                    activeTrackColor = MintGreenPrimary
                                )
                            )
                        }

                        TextButton(
                            onClick = {
                                layoutState = GamepadLayoutStorage.resetLayout(context, layoutState.preset)
                                showSettingsDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = CyberCyan)
                            Spacer(Modifier.width(6.dp))
                            Text("Reset Layout to Factory Defaults", color = CyberCyan)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Save & Close", color = MintGreenPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

// -------------------------------------------------------------
// Component: Shoulder Bumpers & Triggers (L1/L2 and R1/R2)
// -------------------------------------------------------------
@Composable
fun ShoulderTriggersCluster(
    isLeft: Boolean,
    onButtonPress: (String) -> Unit,
    isEditMode: Boolean
) {
    val b1 = if (isLeft) "L1" else "R1"
    val b2 = if (isLeft) "L2" else "R2"

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLeft) {
            ShoulderPedal(label = b2, isTrigger = true) { if (!isEditMode) onButtonPress(b2) }
            ShoulderPedal(label = b1, isTrigger = false) { if (!isEditMode) onButtonPress(b1) }
        } else {
            ShoulderPedal(label = b1, isTrigger = false) { if (!isEditMode) onButtonPress(b1) }
            ShoulderPedal(label = b2, isTrigger = true) { if (!isEditMode) onButtonPress(b2) }
        }
    }
}

@Composable
fun ShoulderPedal(
    label: String,
    isTrigger: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) onClick()
    }

    val width = if (isTrigger) 78.dp else 68.dp
    val height = 44.dp
    val shape = RoundedCornerShape(12.dp)

    Surface(
        modifier = Modifier
            .size(width = width, height = height)
            .clickable(interactionSource = interactionSource, indication = null) {},
        shape = shape,
        color = if (isPressed) MintGreenPrimary.copy(alpha = 0.4f) else DarkSurfaceVariant,
        border = BorderStroke(
            width = if (isPressed) 2.dp else 1.dp,
            brush = Brush.verticalGradient(
                listOf(
                    if (isPressed) MintNeon else DarkBorderShine,
                    Color(0x33FFFFFF)
                )
            )
        ),
        shadowElevation = if (isPressed) 0.dp else 6.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (isPressed) MintNeon else Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// -------------------------------------------------------------
// Component: Center Menu Buttons (Select / Start)
// -------------------------------------------------------------
@Composable
fun MenuButtonsCluster(
    onButtonPress: (String) -> Unit,
    isEditMode: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MenuButtonPill(label = "SELECT") { if (!isEditMode) onButtonPress("SELECT") }
        MenuButtonPill(label = "START") { if (!isEditMode) onButtonPress("START") }
    }
}

@Composable
fun MenuButtonPill(
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) onClick()
    }

    Surface(
        modifier = Modifier
            .size(width = 86.dp, height = 36.dp)
            .clickable(interactionSource = interactionSource, indication = null) {},
        shape = RoundedCornerShape(18.dp),
        color = if (isPressed) MintGreenPrimary.copy(alpha = 0.3f) else DarkSurfaceVariant.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, if (isPressed) MintNeon else GlassBorder),
        shadowElevation = if (isPressed) 0.dp else 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (isPressed) MintNeon else Color(0xFFD1D5DB),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

// -------------------------------------------------------------
// Component: Tactile Cross D-Pad
// -------------------------------------------------------------
@Composable
fun TactileDPad(
    onDirectionPress: (String) -> Unit,
    isEditMode: Boolean
) {
    Box(
        modifier = Modifier
            .size(168.dp)
            .clip(CircleShape)
            .background(DarkSurfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, GlassBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Cross Arms
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.size(160.dp)
        ) {
            DPadButton(Icons.Default.ArrowUpward, "UP") { if (!isEditMode) onDirectionPress("UP") }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DPadButton(Icons.AutoMirrored.Filled.ArrowBack, "LEFT") { if (!isEditMode) onDirectionPress("LEFT") }
                // Center anchor dot
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                )
                DPadButton(Icons.AutoMirrored.Filled.ArrowForward, "RIGHT") { if (!isEditMode) onDirectionPress("RIGHT") }
            }

            DPadButton(Icons.Default.ArrowDownward, "DOWN") { if (!isEditMode) onDirectionPress("DOWN") }
        }
    }
}

@Composable
fun DPadButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    direction: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) onClick()
    }

    Surface(
        modifier = Modifier
            .size(52.dp)
            .clickable(interactionSource = interactionSource, indication = null) {},
        shape = RoundedCornerShape(12.dp),
        color = if (isPressed) MintGreenPrimary.copy(alpha = 0.45f) else DarkSurface,
        border = BorderStroke(1.dp, if (isPressed) MintNeon else GlassBorder),
        shadowElevation = if (isPressed) 0.dp else 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = "D-Pad $direction",
                tint = if (isPressed) MintNeon else Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// Component: Jewel Action Buttons Diamond (A, B, X, Y)
// -------------------------------------------------------------
@Composable
fun JewelAbxyDiamond(
    onButtonPress: (String) -> Unit,
    isEditMode: Boolean
) {
    Box(
        modifier = Modifier
            .size(168.dp)
            .clip(CircleShape)
            .background(DarkSurfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, GlassBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.size(160.dp)
        ) {
            JewelButton(label = "Y", color = Color(0xFFFFB300), glowColor = Color(0xFFFFD54F)) {
                if (!isEditMode) onButtonPress("Y")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                JewelButton(label = "X", color = Color(0xFF1E88E5), glowColor = Color(0xFF64B5F6)) {
                    if (!isEditMode) onButtonPress("X")
                }
                JewelButton(label = "B", color = Color(0xFFE53935), glowColor = Color(0xFFEF5350)) {
                    if (!isEditMode) onButtonPress("B")
                }
            }

            JewelButton(label = "A", color = Color(0xFF00C853), glowColor = Color(0xFF69F0AE)) {
                if (!isEditMode) onButtonPress("A")
            }
        }
    }
}

@Composable
fun JewelButton(
    label: String,
    color: Color,
    glowColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) onClick()
    }

    val elevation by animateFloatAsState(if (isPressed) 0f else 6f, label = "elevation")
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1.0f, label = "scale")

    Box(
        modifier = Modifier
            .size(52.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        glowColor,
                        color,
                        color.copy(alpha = 0.8f)
                    ),
                    radius = 80f
                )
            )
            .border(
                width = if (isPressed) 2.5.dp else 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.7f),
                        color
                    )
                ),
                shape = CircleShape
            )
            .clickable(interactionSource = interactionSource, indication = null) {},
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = Color.White
        )
    }
}

// -------------------------------------------------------------
// Component: Premium Virtual Analog Thumbstick
// -------------------------------------------------------------
@Composable
fun PremiumVirtualJoystick(
    label: String,
    onMove: (dx: Float, dy: Float) -> Unit,
    onClick: () -> Unit,
    isEditMode: Boolean
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val maxRadius = 45f

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(118.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            DarkCardElevated,
                            DarkBackground
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            MintGreenPrimary,
                            CyberCyan,
                            Color(0xFF2A3647),
                            MintGreenPrimary
                        )
                    ),
                    shape = CircleShape
                )
                .pointerInput(isEditMode) {
                    if (!isEditMode) {
                        detectDragGestures(
                            onDragEnd = {
                                offsetX = 0f
                                offsetY = 0f
                                onMove(0f, 0f)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val targetX = offsetX + dragAmount.x
                                val targetY = offsetY + dragAmount.y
                                val dist = hypot(targetX, targetY)

                                if (dist <= maxRadius) {
                                    offsetX = targetX
                                    offsetY = targetY
                                } else {
                                    val angle = atan2(targetY, targetX)
                                    offsetX = cos(angle) * maxRadius
                                    offsetY = sin(angle) * maxRadius
                                }
                                onMove(offsetX / maxRadius, offsetY / maxRadius)
                            }
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Guide crosshairs
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .drawBehind {
                        val stroke = 1.dp.toPx()
                        val color = Color(0x22FFFFFF)
                        drawLine(color, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), stroke)
                        drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), stroke)
                    }
            )

            // Analog Knob
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MintGreenLight,
                                MintGreenPrimary,
                                Color(0xFF065F46)
                            )
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                // Inner grip ring
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0x33000000))
                        .border(1.dp, Color(0x44FFFFFF), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 0.8.sp
        )
    }
}

// -------------------------------------------------------------
// Component: Turbo / Special Hotkey Button
// -------------------------------------------------------------
@Composable
fun TurboSpecialButton(
    onButtonPress: (String) -> Unit,
    isEditMode: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) onButtonPress("TURBO")
    }

    Surface(
        modifier = Modifier
            .size(width = 80.dp, height = 36.dp)
            .clickable(interactionSource = interactionSource, indication = null) {},
        shape = RoundedCornerShape(12.dp),
        color = if (isPressed) MintNeon.copy(alpha = 0.4f) else DarkSurfaceVariant,
        border = BorderStroke(1.dp, if (isPressed) MintNeon else GlassBorder),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Turbo",
                tint = if (isPressed) MintNeon else Color(0xFFFBBF24),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "TURBO",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isPressed) MintNeon else Color.White
            )
        }
    }
}
