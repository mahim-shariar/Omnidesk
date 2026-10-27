package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorderShine
import com.example.ui.theme.DarkSurfaceGlass
import com.example.ui.theme.DarkTouchpadSurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.MintAccent
import com.example.ui.theme.MintGreenPrimary
import com.example.ui.theme.TerminalCyan
import kotlin.math.hypot

enum class MouseButtonType {
    LEFT,
    MIDDLE,
    RIGHT
}

/**
 * Custom vector icon clearly representing physical mouse buttons:
 * - LEFT: Left click button area highlighted.
 * - MIDDLE: Center scroll wheel highlighted.
 * - RIGHT: Right click button area highlighted.
 */
@Composable
fun MouseButtonIcon(
    button: MouseButtonType,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    bodyColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    outlineColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val mouseW = w * 0.72f
        val mouseH = h * 0.92f
        val left = (w - mouseW) / 2f
        val top = (h - mouseH) / 2f
        val right = left + mouseW
        val bottom = top + mouseH
        val cornerRadius = mouseW * 0.42f
        val buttonDivideY = top + mouseH * 0.44f
        val centerX = left + mouseW / 2f

        val mouseBounds = RoundRect(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )

        // Clip and draw highlighted button section
        when (button) {
            MouseButtonType.LEFT -> {
                val leftPath = Path().apply {
                    addRoundRect(mouseBounds)
                }
                // Clip to top-left quadrant
                val clipPath = Path().apply {
                    addRect(Rect(0f, 0f, centerX, buttonDivideY))
                }
                leftPath.op(leftPath, clipPath, androidx.compose.ui.graphics.PathOperation.Intersect)
                drawPath(leftPath, color = highlightColor)
            }
            MouseButtonType.RIGHT -> {
                val rightPath = Path().apply {
                    addRoundRect(mouseBounds)
                }
                // Clip to top-right quadrant
                val clipPath = Path().apply {
                    addRect(Rect(centerX, 0f, w, buttonDivideY))
                }
                rightPath.op(rightPath, clipPath, androidx.compose.ui.graphics.PathOperation.Intersect)
                drawPath(rightPath, color = highlightColor)
            }
            MouseButtonType.MIDDLE -> {
                // Center wheel pill
                val wheelW = mouseW * 0.22f
                val wheelH = mouseH * 0.26f
                val wheelX = centerX - wheelW / 2f
                val wheelY = top + mouseH * 0.10f
                drawRoundRect(
                    color = highlightColor,
                    topLeft = Offset(wheelX, wheelY),
                    size = Size(wheelW, wheelH),
                    cornerRadius = CornerRadius(wheelW / 2f, wheelW / 2f)
                )
            }
        }

        // Mouse outer outline
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(left, top),
            size = Size(mouseW, mouseH),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = 2.dp.toPx())
        )

        // Horizontal separator between buttons & palm rest
        drawLine(
            color = outlineColor.copy(alpha = 0.6f),
            start = Offset(left, buttonDivideY),
            end = Offset(right, buttonDivideY),
            strokeWidth = 1.5.dp.toPx()
        )

        // Vertical separator between left & right buttons
        drawLine(
            color = outlineColor.copy(alpha = 0.6f),
            start = Offset(centerX, top),
            end = Offset(centerX, buttonDivideY),
            strokeWidth = 1.5.dp.toPx()
        )

        // If not middle button, draw middle wheel outline
        if (button != MouseButtonType.MIDDLE) {
            val wheelW = mouseW * 0.18f
            val wheelH = mouseH * 0.22f
            val wheelX = centerX - wheelW / 2f
            val wheelY = top + mouseH * 0.11f
            drawRoundRect(
                color = outlineColor.copy(alpha = 0.5f),
                topLeft = Offset(wheelX, wheelY),
                size = Size(wheelW, wheelH),
                cornerRadius = CornerRadius(wheelW / 2f, wheelW / 2f),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}

/**
 * State representing visual touch indicators for active pointers.
 */
data class ActiveTouchPoint(
    val id: Long,
    val position: Offset,
    val fingerIndex: Int
)

/**
 * Highly responsive, multi-touch Touchpad component in Jetpack Compose:
 * - Adapts dynamically across all device sizes (phones, foldables, tablets in portrait & landscape).
 * - Multi-touch gestures (1-finger cursor, 2-finger scroll, 1-finger left click, 2-finger right click, 3-finger middle click).
 * - Visual touch indicators on Canvas.
 * - Icon-only buttons for Left, Middle, and Right clicks replacing text clutter.
 */
@Composable
fun Touchpad(
    modifier: Modifier = Modifier,
    sensitivity: Float = 1.3f,
    onSensitivityChange: ((Float) -> Unit)? = null,
    scrollSpeed: Float = 1.0f,
    onScrollSpeedChange: ((Float) -> Unit)? = null,
    tapToClickEnabled: Boolean = true,
    onTapToClickChange: ((Boolean) -> Unit)? = null,
    singleTapAction: String = "left",
    onSingleTapActionChange: ((String) -> Unit)? = null,
    dragLockActive: Boolean = false,
    onToggleDragLock: () -> Unit = {},
    naturalScrolling: Boolean = false,
    onNaturalScrollingChange: ((Boolean) -> Unit)? = null,
    showScrollStrip: Boolean = true,
    showPhysicalButtons: Boolean = true,
    onMouseMove: (dx: Float, dy: Float) -> Unit,
    onMouseClick: (button: String, double: Boolean) -> Unit,
    onMouseDown: (button: String) -> Unit = {},
    onMouseUp: (button: String) -> Unit = {},
    onMouseScroll: (dx: Float, dy: Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var showSettingsPanel by remember { mutableStateOf(false) }
    var showGestureHelpDialog by remember { mutableStateOf(false) }
    var activeGestureLabel by remember { mutableStateOf("Ready") }

    // Multi-touch visual positions map
    val touchPoints = remember { mutableStateMapOf<Long, ActiveTouchPoint>() }

    // Tap & gesture threshold tracking (24dp provides forgiving touch-down without unintended drag cancel)
    val tapSlopPx = remember(density) { with(density) { 24.dp.toPx() } }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val isWide = maxWidth > 650.dp
        val isLandscape = maxWidth > maxHeight && maxHeight < 560.dp
        val buttonHeight = if (isLandscape) 52.dp else 66.dp
        val horizontalPadding = if (isWide) 24.dp else 12.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .padding(horizontal = horizontalPadding, vertical = 8.dp)
                .testTag("touchpad_component")
        ) {
            // Top Status & Settings Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active gesture status badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, GlassBorderSubtle),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = MintGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activeGestureLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Controls row: Drag Lock, Gesture Guide, Settings
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Drag Lock Quick Indicator / Toggle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (dragLockActive) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (dragLockActive) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) else BorderStroke(1.dp, GlassBorderSubtle),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleDragLock()
                            }
                            .testTag("touchpad_drag_lock_quick")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (dragLockActive) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (dragLockActive) "Drag Lock Active" else "Drag Lock Inactive",
                                tint = if (dragLockActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Gestures Help Button
                    IconButton(
                        onClick = { showGestureHelpDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(10.dp))
                            .testTag("touchpad_gesture_help_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Trackpad Gestures Guide",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (onSensitivityChange != null) {
                        IconButton(
                            onClick = { showSettingsPanel = !showSettingsPanel },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (showSettingsPanel) MintGreenPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (showSettingsPanel) MintGreenPrimary.copy(alpha = 0.4f) else GlassBorderSubtle, RoundedCornerShape(10.dp))
                                .testTag("touchpad_quick_settings_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Touchpad Settings",
                                tint = if (showSettingsPanel) MintGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Expandable Quick Settings Slider Panel
            AnimatedVisibility(visible = showSettingsPanel) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (onSensitivityChange != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Pointer Sensitivity",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = String.format("%.1fx", sensitivity),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = sensitivity,
                                onValueChange = onSensitivityChange,
                                valueRange = 0.5f..3.0f,
                                steps = 10,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (onScrollSpeedChange != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Two-Finger Scroll Speed",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = String.format("%.1fx", scrollSpeed),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = scrollSpeed,
                                onValueChange = onScrollSpeedChange,
                                valueRange = 0.5f..2.5f,
                                steps = 8,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Tap to Click Toggle
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tap to Click",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tap trackpad with 1 finger to click without pressing bottom buttons",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = tapToClickEnabled,
                                onCheckedChange = { onTapToClickChange?.invoke(it) },
                                modifier = Modifier.testTag("touchpad_tap_to_click_switch")
                            )
                        }

                        // Single Tap Action: Left Click vs Right Click
                        if (tapToClickEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "1-Finger Tap Action",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = singleTapAction == "left",
                                    onClick = { onSingleTapActionChange?.invoke("left") },
                                    label = { Text("Left Click (Default)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    modifier = Modifier.weight(1f).testTag("tap_action_left")
                                )
                                FilterChip(
                                    selected = singleTapAction == "right",
                                    onClick = { onSingleTapActionChange?.invoke("right") },
                                    label = { Text("Right Click") },
                                    modifier = Modifier.weight(1f).testTag("tap_action_right")
                                )
                            }
                        }

                        // Natural Scrolling Toggle
                        if (onNaturalScrollingChange != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Natural Scrolling",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Reverse 2-finger scroll direction",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = naturalScrolling,
                                    onCheckedChange = onNaturalScrollingChange,
                                    modifier = Modifier.testTag("touchpad_natural_scrolling_switch")
                                )
                            }
                        }
                    }
                }
            }

            // Master 1-Tap Action Capsule (Mouse Tab Option)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(DarkBorderShine, GlassBorderSubtle))),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("touchpad_tap_mode_capsule")
            ) {
                Column(modifier = Modifier.padding(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1-Tap: Left Click (Select)
                        val isLeftSelected = tapToClickEnabled && singleTapAction == "left"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLeftSelected) MintGreenPrimary.copy(alpha = 0.20f) else Color.Transparent,
                            border = if (isLeftSelected) BorderStroke(1.2.dp, MintGreenPrimary) else BorderStroke(1.dp, Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTapToClickChange?.invoke(true)
                                    onSingleTapActionChange?.invoke("left")
                                }
                                .testTag("quick_tap_left")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                MouseButtonIcon(
                                    button = MouseButtonType.LEFT,
                                    size = 18.dp,
                                    highlightColor = if (isLeftSelected) MintGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "1-Tap: Select (Left)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isLeftSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isLeftSelected) MintGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 1-Tap: Right Click (Context Menu)
                        val isRightSelected = tapToClickEnabled && singleTapAction == "right"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isRightSelected) CyberCyan.copy(alpha = 0.20f) else Color.Transparent,
                            border = if (isRightSelected) BorderStroke(1.2.dp, CyberCyan) else BorderStroke(1.dp, Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTapToClickChange?.invoke(true)
                                    onSingleTapActionChange?.invoke("right")
                                }
                                .testTag("quick_tap_right")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                MouseButtonIcon(
                                    button = MouseButtonType.RIGHT,
                                    size = 18.dp,
                                    highlightColor = if (isRightSelected) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "1-Tap: Right Click",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isRightSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isRightSelected) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Helpful micro-guide
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (singleTapAction == "right") {
                                "⚡ 1 Tap = Right Click • 2 Taps = Select"
                            } else {
                                "✨ 1 Tap = Select • 2 Taps = Right Click"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (singleTapAction == "right") CyberCyan else MintGreenPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "2 Fingers = Scroll",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            // Main Touchpad Area & Edge Scroll Zone
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Interactive Touchpad Surface
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .testTag("multi_touchpad_surface")
                        .pointerInput(sensitivity, scrollSpeed, naturalScrolling, dragLockActive, tapToClickEnabled, singleTapAction) {
                            awaitPointerEventScope {
                                var sessionStartTime = 0L
                                var maxPointersInSession = 0
                                var maxMovedDistance = 0f
                                val previousPositions = mutableMapOf<PointerId, Offset>()
                                var previousPointerCount = 0
                                var lastTapTime = 0L
                                var lastTapButton = ""
                                var twoFingerAccumX = 0f
                                var twoFingerAccumY = 0f
                                var smoothedDx = 0f
                                var smoothedDy = 0f

                                val maxJumpAllowedPx = 300.dp.toPx()
                                val notchThresholdPx = 28.dp.toPx()

                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    val changes = event.changes
                                    val pressedChanges = changes.filter { it.pressed }
                                    val pressedCount = pressedChanges.size

                                    // ALL FINGERS RELEASED -> EVALUATE TAP IMMEDIATELY
                                    if (pressedCount == 0) {
                                        touchPoints.clear()
                                        val now = System.currentTimeMillis()
                                        val sessionDuration = if (sessionStartTime > 0L) now - sessionStartTime else 0L

                                        if (tapToClickEnabled && sessionStartTime > 0L && sessionDuration in 10L..420L && maxMovedDistance < tapSlopPx) {
                                            when (maxPointersInSession) {
                                                1 -> {
                                                    if (singleTapAction == "right") {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        onMouseClick("right", false)
                                                        activeGestureLabel = "Right Click"
                                                        lastTapTime = 0L
                                                        lastTapButton = ""
                                                    } else {
                                                        val isDouble = (now - lastTapTime) < 280L && lastTapButton == "left"
                                                        haptic.performHapticFeedback(
                                                            if (isDouble) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                                                        )
                                                        onMouseClick("left", isDouble)
                                                        lastTapTime = if (isDouble) 0L else now
                                                        lastTapButton = if (isDouble) "" else "left"
                                                        activeGestureLabel = if (isDouble) "Double Left Click" else "Left Click"
                                                    }
                                                }
                                                2 -> {
                                                    // When 1-tap is right click, 2-tap acts as left click so user still has both options!
                                                    val twoFingerBtn = if (singleTapAction == "right") "left" else "right"
                                                    haptic.performHapticFeedback(
                                                        if (twoFingerBtn == "left") HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress
                                                    )
                                                    onMouseClick(twoFingerBtn, false)
                                                    activeGestureLabel = if (twoFingerBtn == "left") "Left Click" else "Right Click"
                                                    lastTapTime = 0L
                                                    lastTapButton = ""
                                                }
                                                3 -> {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onMouseClick("middle", false)
                                                    activeGestureLabel = "Middle Click"
                                                    lastTapTime = 0L
                                                    lastTapButton = ""
                                                }
                                            }
                                        } else {
                                            activeGestureLabel = "Ready"
                                        }

                                        // Clean reset of all gesture tracking state
                                        sessionStartTime = 0L
                                        maxPointersInSession = 0
                                        maxMovedDistance = 0f
                                        previousPositions.clear()
                                        previousPointerCount = 0
                                        twoFingerAccumX = 0f
                                        twoFingerAccumY = 0f
                                        smoothedDx = 0f
                                        smoothedDy = 0f
                                        continue
                                    }

                                    // FINGERS ARE CURRENTLY TOUCHING THE SCREEN
                                    if (previousPointerCount == 0) {
                                        sessionStartTime = System.currentTimeMillis()
                                        maxPointersInSession = pressedCount
                                        maxMovedDistance = 0f
                                    } else if (pressedCount > maxPointersInSession) {
                                        maxPointersInSession = pressedCount
                                    }

                                    // Update visual touch point coordinates
                                    touchPoints.clear()
                                    pressedChanges.forEachIndexed { index, change ->
                                        touchPoints[change.id.value] = ActiveTouchPoint(
                                            id = change.id.value,
                                            position = change.position,
                                            fingerIndex = index + 1
                                        )
                                    }

                                    // Pointer count change transition (e.g. going 1->2 fingers or 2->1 fingers)
                                    // Re-anchor previousPositions so that transition frame does not produce huge movement
                                    if (pressedCount != previousPointerCount) {
                                        previousPositions.clear()
                                        pressedChanges.forEach { previousPositions[it.id] = it.position }
                                        previousPointerCount = pressedCount
                                        twoFingerAccumX = 0f
                                        twoFingerAccumY = 0f
                                        smoothedDx = 0f
                                        smoothedDy = 0f

                                        activeGestureLabel = when (pressedCount) {
                                            1 -> if (dragLockActive) "Dragging" else "Move Cursor"
                                            2 -> "2-Finger Scroll"
                                            3 -> "3-Finger Middle"
                                            else -> "$pressedCount Fingers Active"
                                        }
                                        continue
                                    }

                                    // 1-FINGER DRAG (Responsive, Butter-Smooth Cursor Movement)
                                    if (pressedCount == 1) {
                                        val change = pressedChanges[0]
                                        val prevPos = previousPositions[change.id] ?: change.position
                                        val rawDelta = change.position - prevPos
                                        previousPositions[change.id] = change.position

                                        val moveDist = hypot(rawDelta.x, rawDelta.y)

                                        // Ignore only unrealistic finger-switch teleports (>300dp)
                                        if (moveDist > maxJumpAllowedPx) {
                                            smoothedDx = 0f
                                            smoothedDy = 0f
                                            change.consume()
                                            continue
                                        }

                                        maxMovedDistance += moveDist
                                        change.consume()

                                        // Highly responsive smoothing (0.88 gives zero drag-lag while absorbing touch sensor jitter)
                                        val alpha = 0.88f
                                        smoothedDx = alpha * rawDelta.x + (1f - alpha) * smoothedDx
                                        smoothedDy = alpha * rawDelta.y + (1f - alpha) * smoothedDy

                                        // Continuous smooth ballistic acceleration (replaces tiered steps)
                                        val speedMultiplier = (1.0f + (moveDist / 20f)).coerceIn(1.0f, 2.2f)

                                        onMouseMove(
                                            smoothedDx * speedMultiplier,
                                            smoothedDy * speedMultiplier
                                        )
                                        activeGestureLabel = if (dragLockActive) "Dragging" else "Move Cursor"
                                    }
                                    // 2-FINGER DRAG (Controlled Notch Scroll - Strictly No Vibration & No Runaway Loops)
                                    else if (pressedCount == 2) {
                                        val c1 = pressedChanges[0]
                                        val c2 = pressedChanges[1]

                                        if (!c1.previousPressed || !c2.previousPressed) {
                                            previousPositions[c1.id] = c1.position
                                            previousPositions[c2.id] = c2.position
                                            twoFingerAccumX = 0f
                                            twoFingerAccumY = 0f
                                            continue
                                        }

                                        val prev1 = previousPositions[c1.id] ?: c1.position
                                        val prev2 = previousPositions[c2.id] ?: c2.position

                                        val delta1 = c1.position - prev1
                                        val delta2 = c2.position - prev2
                                        previousPositions[c1.id] = c1.position
                                        previousPositions[c2.id] = c2.position

                                        val avgDeltaX = (delta1.x + delta2.x) / 2f
                                        val avgDeltaY = (delta1.y + delta2.y) / 2f

                                        c1.consume()
                                        c2.consume()

                                        val maxScrollFrame = 24.dp.toPx()
                                        val clampedDeltaY = avgDeltaY.coerceIn(-maxScrollFrame, maxScrollFrame)
                                        val clampedDeltaX = avgDeltaX.coerceIn(-maxScrollFrame, maxScrollFrame)

                                        maxMovedDistance += hypot(clampedDeltaX, clampedDeltaY)
                                        twoFingerAccumY += clampedDeltaY
                                        twoFingerAccumX += clampedDeltaX

                                        val scrollMultiplierY = if (naturalScrolling) 1f else -1f

                                        if (kotlin.math.abs(twoFingerAccumY) >= notchThresholdPx) {
                                            val dir = if (twoFingerAccumY > 0) 1f else -1f
                                            twoFingerAccumY -= dir * notchThresholdPx
                                            onMouseScroll(0f, -dir * scrollMultiplierY)
                                            activeGestureLabel = "2-Finger Scroll"
                                        }
                                    }
                                    // 3-FINGER DRAG
                                    else if (pressedCount >= 3) {
                                        val avgDeltaX = pressedChanges.map {
                                            val prev = previousPositions[it.id] ?: it.position
                                            it.position.x - prev.x
                                        }.average().toFloat()
                                        val avgDeltaY = pressedChanges.map {
                                            val prev = previousPositions[it.id] ?: it.position
                                            it.position.y - prev.y
                                        }.average().toFloat()
                                        pressedChanges.forEach {
                                            previousPositions[it.id] = it.position
                                            it.consume()
                                        }
                                        maxMovedDistance += hypot(avgDeltaX, avgDeltaY)
                                        activeGestureLabel = "$pressedCount-Finger Gesture"
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Center Hint Graphics with generous padding
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PanTool,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Trackpad",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (singleTapAction == "right") {
                                "1 Finger: Move Cursor • 1 Tap: Right Click (Menu)\n2 Fingers: Drag to Scroll • 2 Taps: Left Click (Select)\n3 Fingers: Tap for Middle Click"
                            } else {
                                "1 Finger: Move Cursor • 1 Tap: Left Click (Select)\n2 Fingers: Drag to Scroll • 2 Taps: Right Click (Menu)\n3 Fingers: Tap for Middle Click"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }

                    // Visual Glowing Finger Tracking Rings on Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        touchPoints.values.forEach { pt ->
                            val ringColor = when (pt.fingerIndex) {
                                1 -> MintGreenPrimary
                                2 -> TerminalCyan
                                else -> MintAccent
                            }

                            drawCircle(
                                color = ringColor.copy(alpha = 0.25f),
                                radius = 48.dp.toPx(),
                                center = pt.position
                            )
                            drawCircle(
                                color = ringColor.copy(alpha = 0.85f),
                                radius = 28.dp.toPx(),
                                center = pt.position,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            drawCircle(
                                color = ringColor,
                                radius = 8.dp.toPx(),
                                center = pt.position
                            )
                        }
                    }
                }

                // Dedicated Precision Vertical Scroll Strip
                if (showScrollStrip) {
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .testTag("touchpad_scroll_strip")
                            .pointerInput(naturalScrolling) {
                                awaitPointerEventScope {
                                    var stripAccumY = 0f
                                    var lastStripY = 0f
                                    var isDraggingStrip = false
                                    val stripNotchPx = 22.dp.toPx()
                                    val maxScrollFrame = 25.dp.toPx()

                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        val pressed = event.changes.filter { it.pressed }
                                        if (pressed.isNotEmpty()) {
                                            val change = pressed[0]
                                            // Re-anchor on initial touch down to eliminate huge delta jumps!
                                            if (!isDraggingStrip || !change.previousPressed) {
                                                isDraggingStrip = true
                                                lastStripY = change.position.y
                                                stripAccumY = 0f
                                                change.consume()
                                                continue
                                            }

                                            val rawDeltaY = change.position.y - lastStripY
                                            lastStripY = change.position.y
                                            change.consume()

                                            // Clamp delta per frame to prevent runaway jumps
                                            val clampedDeltaY = rawDeltaY.coerceIn(-maxScrollFrame, maxScrollFrame)
                                            stripAccumY += clampedDeltaY

                                            val scrollMultiplierY = if (naturalScrolling) 1f else -1f

                                            // Emit at most 1 wheel notch per tick, strictly NO vibration
                                            if (kotlin.math.abs(stripAccumY) >= stripNotchPx) {
                                                val dir = if (stripAccumY > 0) 1f else -1f
                                                stripAccumY -= dir * stripNotchPx
                                                onMouseScroll(0f, -dir * scrollMultiplierY)
                                                activeGestureLabel = "Scroll Strip"
                                            }
                                        } else {
                                            isDraggingStrip = false
                                            stripAccumY = 0f
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Scroll Strip",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tactile Physical Action Buttons with clear iconography and labels
            if (showPhysicalButtons) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // LEFT CLICK (Select)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(DarkBorderShine, GlassBorderSubtle))),
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onMouseClick("left", false)
                            }
                            .testTag("touchpad_left_click")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            MouseButtonIcon(
                                button = MouseButtonType.LEFT,
                                size = 26.dp,
                                highlightColor = MintGreenPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Left (Select)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // MIDDLE CLICK (Wheel)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, GlassBorderSubtle),
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onMouseClick("middle", false)
                            }
                            .testTag("touchpad_middle_click")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            MouseButtonIcon(
                                button = MouseButtonType.MIDDLE,
                                size = 26.dp,
                                highlightColor = TerminalCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Wheel",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // DRAG LOCK
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (dragLockActive) MaterialTheme.colorScheme.error.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (dragLockActive) MaterialTheme.colorScheme.error else GlassBorderSubtle),
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleDragLock()
                            }
                            .testTag("touchpad_drag_lock")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (dragLockActive) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Toggle Drag Lock",
                                tint = if (dragLockActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (dragLockActive) "Locked" else "Drag Lock",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (dragLockActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (dragLockActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // RIGHT CLICK (Menu)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(DarkBorderShine, GlassBorderSubtle))),
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onMouseClick("right", false)
                            }
                            .testTag("touchpad_right_click")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            MouseButtonIcon(
                                button = MouseButtonType.RIGHT,
                                size = 26.dp,
                                highlightColor = CyberCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Right (Menu)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    // Gestures Guide Modal Dialog
    if (showGestureHelpDialog) {
        AlertDialog(
            onDismissRequest = { showGestureHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MintGreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Touchpad Gestures Guide",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GestureHelpRow(
                        title = "1-Finger Drag",
                        description = "Move mouse cursor smoothly across screen"
                    )
                    GestureHelpRow(
                        title = "1-Finger Tap",
                        description = if (singleTapAction == "right") {
                            "Currently configured to trigger Right Click (Context Menu)"
                        } else {
                            "Select, click links, activate buttons (Left Click)"
                        }
                    )
                    GestureHelpRow(
                        title = "2-Finger Tap",
                        description = if (singleTapAction == "right") {
                            "Triggers Left Click (Select) when 1-tap is set to Right Click"
                        } else {
                            "Opens context menu (Right Click) anywhere"
                        }
                    )
                    GestureHelpRow(
                        title = "2-Finger Drag",
                        description = "Smooth 2-way vertical & horizontal scrolling"
                    )
                    GestureHelpRow(
                        title = "3-Finger Tap",
                        description = "Middle Click (opens browser links in background tab)"
                    )
                    GestureHelpRow(
                        title = "Right Strip",
                        description = "Dedicated 1-finger vertical scroll wheel"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showGestureHelpDialog = false }) {
                    Text("Got It", color = MintGreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun GestureHelpRow(title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MintGreenPrimary.copy(alpha = 0.15f),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Box(modifier = Modifier.size(8.dp))
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
