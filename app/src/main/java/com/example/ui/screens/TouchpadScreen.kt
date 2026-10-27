package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.Touchpad

@Composable
fun TouchpadScreen(
    sensitivity: Float,
    onSensitivityChange: (Float) -> Unit,
    scrollSpeed: Float = 1.0f,
    onScrollSpeedChange: ((Float) -> Unit)? = null,
    tapToClickEnabled: Boolean = true,
    onTapToClickChange: ((Boolean) -> Unit)? = null,
    singleTapAction: String = "left",
    onSingleTapActionChange: ((String) -> Unit)? = null,
    dragLockActive: Boolean,
    onToggleDragLock: () -> Unit,
    naturalScrolling: Boolean = false,
    onNaturalScrollingChange: ((Boolean) -> Unit)? = null,
    onMouseMove: (dx: Float, dy: Float) -> Unit,
    onMouseClick: (button: String, double: Boolean) -> Unit,
    onMouseDown: (button: String) -> Unit,
    onMouseUp: (button: String) -> Unit,
    onMouseScroll: (dx: Float, dy: Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .testTag("touchpad_screen"),
        contentAlignment = Alignment.Center
    ) {
        Touchpad(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp),
            sensitivity = sensitivity,
            onSensitivityChange = onSensitivityChange,
            scrollSpeed = scrollSpeed,
            onScrollSpeedChange = onScrollSpeedChange,
            tapToClickEnabled = tapToClickEnabled,
            onTapToClickChange = onTapToClickChange,
            singleTapAction = singleTapAction,
            onSingleTapActionChange = onSingleTapActionChange,
            dragLockActive = dragLockActive,
            onToggleDragLock = onToggleDragLock,
            naturalScrolling = naturalScrolling,
            onNaturalScrollingChange = onNaturalScrollingChange,
            onMouseMove = onMouseMove,
            onMouseClick = onMouseClick,
            onMouseDown = onMouseDown,
            onMouseUp = onMouseUp,
            onMouseScroll = onMouseScroll
        )
    }
}
