package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MintGreenPrimary

/**
 * Renders custom vector logos for Windows, macOS, Linux, or generic PC
 */
@Composable
fun OsLogoIcon(
    osType: String,
    osName: String = "",
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    val cleanType = osType.lowercase().trim()
    val cleanName = osName.lowercase().trim()
    val isWin = cleanType == "windows" || cleanName.contains("windows") || cleanName.contains("win1") || cleanName.contains("win7")
    val isMac = cleanType == "macos" || cleanType == "darwin" || cleanType == "apple" ||
            cleanName.contains("mac") || cleanName.contains("darwin") || cleanName.contains("apple") || cleanName.contains("osx")
    val isLinux = !isWin && !isMac && (cleanType == "linux" || cleanName.contains("linux") || cleanName.contains("ubuntu") || cleanName.contains("mint") || cleanName.contains("fedora") || cleanName.contains("debian") || cleanName.contains("arch"))

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when {
            isWin -> {
                // Windows 11/10 4-pane icon
                val c = tint ?: Color(0xFF00ADEF)
                val gap = w * 0.12f
                val paneW = (w - gap) / 2f
                val paneH = (h - gap) / 2f
                val corner = CornerRadius(w * 0.05f)

                // Top-Left
                drawRoundRect(c, Offset(0f, 0f), Size(paneW, paneH), corner)
                // Top-Right
                drawRoundRect(c, Offset(paneW + gap, 0f), Size(paneW, paneH), corner)
                // Bottom-Left
                drawRoundRect(c, Offset(0f, paneH + gap), Size(paneW, paneH), corner)
                // Bottom-Right
                drawRoundRect(c, Offset(paneW + gap, paneH + gap), Size(paneW, paneH), corner)
            }
            isMac -> {
                // Apple logo silhouette
                val c = tint ?: Color(0xFFE2E8F0)
                val path = Path().apply {
                    val cx = w * 0.5f
                    val cy = h * 0.55f
                    val r = w * 0.38f
                    // Apple body
                    addOval(androidx.compose.ui.geometry.Rect(cx - r, cy - r * 0.95f, cx + r, cy + r * 1.05f))
                }
                drawPath(path, c, style = Fill)

                // Apple bite cutout
                drawCircle(
                    color = Color.Black.copy(alpha = 0.85f),
                    radius = w * 0.17f,
                    center = Offset(w * 0.86f, h * 0.52f)
                )

                // Apple top leaf
                val leafPath = Path().apply {
                    moveTo(w * 0.50f, h * 0.18f)
                    quadraticTo(w * 0.65f, h * 0.06f, w * 0.72f, h * 0.14f)
                    quadraticTo(w * 0.60f, h * 0.28f, w * 0.50f, h * 0.18f)
                    close()
                }
                drawPath(leafPath, c, style = Fill)
            }
            isLinux -> {
                // Linux Tux / Mint leaf shape
                val c = tint ?: MintGreenPrimary
                val leafPath = Path().apply {
                    moveTo(w * 0.15f, h * 0.85f)
                    cubicTo(w * 0.05f, h * 0.45f, w * 0.35f, h * 0.10f, w * 0.85f, h * 0.12f)
                    cubicTo(w * 0.95f, h * 0.55f, w * 0.65f, h * 0.90f, w * 0.15f, h * 0.85f)
                    close()
                }
                drawPath(leafPath, c, style = Fill)

                // Leaf stem vein
                drawLine(
                    color = Color.Black.copy(alpha = 0.5f),
                    start = Offset(w * 0.15f, h * 0.85f),
                    end = Offset(w * 0.68f, h * 0.32f),
                    strokeWidth = w * 0.08f
                )
            }
            else -> {
                // Generic PC Monitor Icon for unknown/custom OS
                val c = tint ?: Color(0xFF94A3B8)
                val monitorW = w * 0.85f
                val monitorH = h * 0.62f
                val corner = CornerRadius(w * 0.08f)
                drawRoundRect(
                    c,
                    Offset(w * 0.075f, h * 0.12f),
                    Size(monitorW, monitorH),
                    corner,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.09f)
                )
                // Stand
                drawLine(c, Offset(w * 0.5f, h * 0.74f), Offset(w * 0.5f, h * 0.88f), strokeWidth = w * 0.09f)
                drawLine(c, Offset(w * 0.3f, h * 0.88f), Offset(w * 0.7f, h * 0.88f), strokeWidth = w * 0.09f)
            }
        }
    }
}

/**
 * Beautiful M3 Pill Badge displaying OS indicator & name
 */
@Composable
fun OsBadge(
    osType: String,
    osName: String = "",
    modifier: Modifier = Modifier,
    iconSize: Dp = 14.dp
) {
    val cleanType = osType.lowercase().trim()
    val cleanName = osName.lowercase().trim()
    val isWin = cleanType == "windows" || cleanName.contains("windows") || cleanName.contains("win1") || cleanName.contains("win7")
    val isMac = cleanType == "macos" || cleanType == "darwin" || cleanType == "apple" ||
            cleanName.contains("mac") || cleanName.contains("darwin") || cleanName.contains("apple") || cleanName.contains("osx")
    val isLinux = !isWin && !isMac && (cleanType == "linux" || cleanName.contains("linux") || cleanName.contains("ubuntu") || cleanName.contains("mint") || cleanName.contains("fedora") || cleanName.contains("debian") || cleanName.contains("arch"))

    val (accentColor, label) = when {
        isWin -> CyberCyan to (if (osName.isNotBlank() && !osName.equals("Desktop PC", ignoreCase = true)) osName else "Windows")
        isMac -> Color(0xFFE2E8F0) to (if (osName.isNotBlank() && !osName.equals("Desktop PC", ignoreCase = true)) osName else "macOS")
        isLinux -> MintGreenPrimary to (if (osName.isNotBlank() && !osName.equals("Desktop PC", ignoreCase = true)) osName else "Linux")
        else -> Color(0xFF94A3B8) to (if (osName.isNotBlank()) osName else "Desktop PC")
    }

    Surface(
        modifier = modifier,
        color = accentColor.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OsLogoIcon(
                osType = osType,
                osName = osName,
                modifier = Modifier.size(iconSize),
                tint = accentColor
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
