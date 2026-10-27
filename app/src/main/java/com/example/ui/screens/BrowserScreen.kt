package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrowserScreen(
    onNewTab: () -> Unit,
    onCloseTab: () -> Unit,
    onReopenTab: () -> Unit,
    onReload: () -> Unit,
    onFullscreen: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onAddressBar: () -> Unit,
    onBookmark: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onShortcut: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .testTag("browser_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Navigation & History Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Navigation & Address",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BrowserActionSquare(
                        label = "Back",
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        modifier = Modifier.weight(1f),
                        onClick = onBack
                    )
                    BrowserActionSquare(
                        label = "Forward",
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        modifier = Modifier.weight(1f),
                        onClick = onForward
                    )
                    BrowserActionSquare(
                        label = "Reload",
                        icon = Icons.Default.Refresh,
                        modifier = Modifier.weight(1f),
                        onClick = onReload
                    )
                    BrowserActionSquare(
                        label = "URL Bar",
                        icon = Icons.Default.OpenInBrowser,
                        modifier = Modifier.weight(1.2f),
                        onClick = onAddressBar
                    )
                }
            }
        }

        // Tabs Management Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Tab Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BrowserActionSquare(
                        label = "New Tab",
                        icon = Icons.Default.Add,
                        modifier = Modifier.weight(1f),
                        onClick = onNewTab
                    )
                    BrowserActionSquare(
                        label = "Close Tab",
                        icon = Icons.Default.Close,
                        modifier = Modifier.weight(1f),
                        onClick = onCloseTab
                    )
                    BrowserActionSquare(
                        label = "Reopen Tab",
                        icon = Icons.Default.Restore,
                        modifier = Modifier.weight(1f),
                        onClick = onReopenTab
                    )
                }
            }
        }

        // View & Zoom Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "View & Display Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BrowserActionSquare(
                        label = "Fullscreen",
                        icon = Icons.Default.Fullscreen,
                        modifier = Modifier.weight(1f),
                        onClick = onFullscreen
                    )
                    BrowserActionSquare(
                        label = "Zoom In",
                        icon = Icons.Default.ZoomIn,
                        modifier = Modifier.weight(1f),
                        onClick = onZoomIn
                    )
                    BrowserActionSquare(
                        label = "Zoom Out",
                        icon = Icons.Default.ZoomOut,
                        modifier = Modifier.weight(1f),
                        onClick = onZoomOut
                    )
                    BrowserActionSquare(
                        label = "Bookmark",
                        icon = Icons.Default.Bookmark,
                        modifier = Modifier.weight(1f),
                        onClick = onBookmark
                    )
                }
            }
        }

        // Quick Browser Hotkeys
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Quick Hotkeys",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShortcutPill(label = "Find in Page (Ctrl+F)") { onShortcut("ctrl+f") }
                    ShortcutPill(label = "Downloads (Ctrl+J)") { onShortcut("ctrl+j") }
                    ShortcutPill(label = "History (Ctrl+H)") { onShortcut("ctrl+h") }
                    ShortcutPill(label = "Reset Zoom (Ctrl+0)") { onShortcut("ctrl+0") }
                    ShortcutPill(label = "DevTools (F12)") { onShortcut("f12") }
                }
            }
        }
    }
}

@Composable
fun BrowserActionSquare(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
