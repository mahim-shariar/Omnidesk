package com.example.ui.qr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MintGreenPrimary
import com.example.ui.theme.MintNeon
import java.util.concurrent.Executors

@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit,
    onQrCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isTorchEnabled by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var showManualPayloadDialog by remember { mutableStateOf(false) }
    var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderFuture.get()?.unbindAll()
            } catch (_: Exception) {}
            try {
                cameraExecutor.shutdown()
            } catch (_: Exception) {}
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Rebind camera when camera permission, preview view, or camera lens flips
    LaunchedEffect(hasCameraPermission, useFrontCamera, previewViewInstance) {
        val pv = previewViewInstance ?: return@LaunchedEffect
        if (!hasCameraPermission) return@LaunchedEffect

        try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = pv.surfaceProvider
            }

            val analyzer = QrCodeAnalyzer { scannedText ->
                vibrateOnScan(context)
                onQrCodeScanned(scannedText)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, analyzer)
                }

            val cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            cameraInstance = camera
            camera.cameraControl.enableTorch(isTorchEnabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    BackHandler {
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("qr_scanner_dialog")
        ) {
            if (hasCameraPermission) {
                // CameraX Preview View
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewViewInstance = this
                        }
                    },
                    update = {
                        cameraInstance?.cameraControl?.enableTorch(isTorchEnabled)
                    }
                )

                // Laser Scanning Overlay & Viewfinder
                QrViewfinderOverlay(modifier = Modifier.fillMaxSize())
            } else {
                // Permission Fallback Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Camera Required",
                        tint = MintGreenPrimary,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Access Required",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Point your camera at the QR code generated on your PC to pair instantly without typing codes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = MintGreenPrimary),
                        modifier = Modifier.testTag("grant_camera_permission_button")
                    ) {
                        Text("Grant Camera Permission", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showManualPayloadDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste / Test QR Payload")
                    }
                }
            }

            // Top Overlay Bar: Close, Flashlight, Flip Camera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.60f))
                        .testTag("close_qr_scanner_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Scanner",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Flashlight toggle
                    if (hasCameraPermission && !useFrontCamera) {
                        IconButton(
                            onClick = {
                                isTorchEnabled = !isTorchEnabled
                                cameraInstance?.cameraControl?.enableTorch(isTorchEnabled)
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (isTorchEnabled) MintGreenPrimary else Color.Black.copy(alpha = 0.60f))
                                .testTag("toggle_torch_button")
                        ) {
                            Icon(
                                imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Toggle Torch",
                                tint = if (isTorchEnabled) Color.Black else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Flip camera
                    if (hasCameraPermission) {
                        IconButton(
                            onClick = {
                                useFrontCamera = !useFrontCamera
                                isTorchEnabled = false
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.60f))
                                .testTag("flip_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipCameraAndroid,
                                contentDescription = "Flip Camera",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Instructions & Simulator Button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xDD111827)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Scan PC Pairing QR Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MintGreenPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Run 'python qr_pair.py' in omnidesk-server on your PC or view current pairing QR on desktop screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Paste or Test QR Payload Button (Allows 100% verification in emulators / no-camera setups)
                TextButton(
                    onClick = { showManualPayloadDialog = true },
                    modifier = Modifier.testTag("manual_qr_payload_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Paste or Test QR Code (Simulator)",
                        color = CyberCyan,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Manual / Simulated QR Dialog
            if (showManualPayloadDialog) {
                ManualQrPayloadDialog(
                    onDismiss = { showManualPayloadDialog = false },
                    onApply = { payloadText ->
                        showManualPayloadDialog = false
                        vibrateOnScan(context)
                        onQrCodeScanned(payloadText)
                    }
                )
            }
        }
    }
}

@Composable
fun QrViewfinderOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_progress"
    )

    BoxWithConstraints(modifier = modifier) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        val boxSize = minOf(width * 0.72f, 300.dp.value * 2.75f).coerceIn(240f, 650f)
        val left = (width - boxSize) / 2f
        val top = (height - boxSize) / 2f - 40f
        val right = left + boxSize
        val bottom = top + boxSize
        val cornerLength = 36f
        val cornerRadius = 24f

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Darkened translucent background with transparent hole for camera
            val cutoutPath = androidx.compose.ui.graphics.Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                )
            }

            // Draw translucent dark veil around the frame
            drawRect(
                color = Color.Black.copy(alpha = 0.65f),
                size = Size(width, height)
            )

            // Clear cutout area
            drawPath(
                path = cutoutPath,
                color = Color.Transparent,
                blendMode = BlendMode.Clear
            )

            // Draw inner border
            drawRoundRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = Offset(left, top),
                size = Size(boxSize, boxSize),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 1.5f)
            )

            // Draw Vibrant Corner Brackets (Emerald & MintNeon)
            val strokeWidth = 5.5f
            val cornerColor = MintGreenPrimary

            // Top-Left
            drawLine(
                color = cornerColor,
                start = Offset(left, top + cornerLength),
                end = Offset(left, top + cornerRadius),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawArc(
                color = cornerColor,
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(left, top),
                size = Size(cornerRadius * 2, cornerRadius * 2),
                style = Stroke(strokeWidth)
            )
            drawLine(
                color = cornerColor,
                start = Offset(left + cornerRadius, top),
                end = Offset(left + cornerLength, top),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Top-Right
            drawLine(
                color = cornerColor,
                start = Offset(right - cornerLength, top),
                end = Offset(right - cornerRadius, top),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawArc(
                color = cornerColor,
                startAngle = 270f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(right - cornerRadius * 2, top),
                size = Size(cornerRadius * 2, cornerRadius * 2),
                style = Stroke(strokeWidth)
            )
            drawLine(
                color = cornerColor,
                start = Offset(right, top + cornerRadius),
                end = Offset(right, top + cornerLength),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Bottom-Left
            drawLine(
                color = cornerColor,
                start = Offset(left, bottom - cornerLength),
                end = Offset(left, bottom - cornerRadius),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawArc(
                color = cornerColor,
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(left, bottom - cornerRadius * 2),
                size = Size(cornerRadius * 2, cornerRadius * 2),
                style = Stroke(strokeWidth)
            )
            drawLine(
                color = cornerColor,
                start = Offset(left + cornerRadius, bottom),
                end = Offset(left + cornerLength, bottom),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Bottom-Right
            drawLine(
                color = cornerColor,
                start = Offset(right - cornerLength, bottom),
                end = Offset(right - cornerRadius, bottom),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawArc(
                color = cornerColor,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(right - cornerRadius * 2, bottom - cornerRadius * 2),
                size = Size(cornerRadius * 2, cornerRadius * 2),
                style = Stroke(strokeWidth)
            )
            drawLine(
                color = cornerColor,
                start = Offset(right, bottom - cornerRadius),
                end = Offset(right, bottom - cornerLength),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Animated Laser Line with Gradient Glow
            val laserY = top + (boxSize * laserProgress)
            val laserGlowHeight = 18f

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MintNeon.copy(alpha = 0.35f),
                        MintGreenPrimary,
                        Color.Transparent
                    ),
                    startY = laserY - laserGlowHeight,
                    endY = laserY + laserGlowHeight
                ),
                topLeft = Offset(left + 8f, laserY - laserGlowHeight),
                size = Size(boxSize - 16f, laserGlowHeight * 2)
            )

            drawLine(
                color = MintNeon,
                start = Offset(left + 12f, laserY),
                end = Offset(right - 12f, laserY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun ManualQrPayloadDialog(
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var payloadText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.QrCode, contentDescription = null, tint = MintGreenPrimary)
        },
        title = {
            Text("Simulate / Paste QR Code", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste a QR code payload or tap a quick preset to test pairing without needing a physical camera:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = payloadText,
                    onValueChange = { payloadText = it },
                    placeholder = { Text("{\"protocol\":\"omnidesk\",\"ip\":\"192.168.1.50\",\"port\":8765,\"pin\":\"123456\"}") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("qr_payload_input"),
                    shape = RoundedCornerShape(10.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )

                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            payloadText = "{\"protocol\":\"omnidesk\",\"ip\":\"127.0.0.1\",\"port\":8765,\"pin\":\"123456\",\"hostname\":\"Desktop PC (USB)\"}"
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("USB Localhost", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            payloadText = "omnidesk://192.168.1.100:8765?pin=482731&name=Workstation-PC&os=windows"
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Wi-Fi URI", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(payloadText) },
                enabled = payloadText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MintGreenPrimary),
                modifier = Modifier.testTag("apply_qr_payload_button")
            ) {
                Text("Process Code", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun vibrateOnScan(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(70L, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(70L)
        }
    } catch (_: Exception) {
    }
}
