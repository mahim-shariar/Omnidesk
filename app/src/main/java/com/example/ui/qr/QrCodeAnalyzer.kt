package com.example.ui.qr

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.atomic.AtomicBoolean

class QrCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val isScanning = AtomicBoolean(true)
    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true,
                DecodeHintType.CHARACTER_SET to "UTF-8"
            )
        )
    }

    override fun analyze(imageProxy: ImageProxy) {
        if (!isScanning.get()) {
            imageProxy.close()
            return
        }

        try {
            val planes = imageProxy.planes
            if (planes.isNotEmpty()) {
                val yPlane = planes[0]
                val buffer = yPlane.buffer
                val rowStride = yPlane.rowStride
                val pixelStride = yPlane.pixelStride
                val width = imageProxy.width
                val height = imageProxy.height

                // Extract clean contiguous luminance byte array respecting rowStride & pixelStride
                val cleanData = ByteArray(width * height)
                buffer.rewind()

                if (pixelStride == 1 && rowStride == width) {
                    buffer.get(cleanData, 0, width * height)
                } else {
                    val rowBuffer = ByteArray(rowStride)
                    var outOffset = 0
                    for (row in 0 until height) {
                        val remaining = buffer.remaining()
                        val toRead = minOf(rowStride, remaining)
                        buffer.get(rowBuffer, 0, toRead)
                        for (col in 0 until width) {
                            cleanData[outOffset++] = rowBuffer[col * pixelStride]
                        }
                    }
                }

                // Handle camera sensor rotation (portrait phones typically report 90 or 270)
                val rotation = imageProxy.imageInfo.rotationDegrees
                val finalData: ByteArray
                val finalWidth: Int
                val finalHeight: Int

                when (rotation) {
                    90 -> {
                        finalData = ByteArray(width * height)
                        for (y in 0 until height) {
                            for (x in 0 until width) {
                                finalData[x * height + (height - y - 1)] = cleanData[y * width + x]
                            }
                        }
                        finalWidth = height
                        finalHeight = width
                    }
                    180 -> {
                        finalData = ByteArray(width * height)
                        for (i in 0 until width * height) {
                            finalData[width * height - 1 - i] = cleanData[i]
                        }
                        finalWidth = width
                        finalHeight = height
                    }
                    270 -> {
                        finalData = ByteArray(width * height)
                        for (y in 0 until height) {
                            for (x in 0 until width) {
                                finalData[(width - x - 1) * height + y] = cleanData[y * width + x]
                            }
                        }
                        finalWidth = height
                        finalHeight = width
                    }
                    else -> {
                        finalData = cleanData
                        finalWidth = width
                        finalHeight = height
                    }
                }

                val source = PlanarYUVLuminanceSource(
                    finalData,
                    finalWidth,
                    finalHeight,
                    0,
                    0,
                    finalWidth,
                    finalHeight,
                    false
                )
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                var scannedText: String? = null
                try {
                    val result = reader.decodeWithState(binaryBitmap)
                    scannedText = result?.text
                } catch (_: Exception) {
                    // Try inverted luminance source for dark-mode QR codes
                    try {
                        val invertedBitmap = BinaryBitmap(HybridBinarizer(source.invert()))
                        val result = reader.decodeWithState(invertedBitmap)
                        scannedText = result?.text
                    } catch (_: Exception) {
                        // QR not detected in this frame
                    }
                } finally {
                    reader.reset()
                }

                if (!scannedText.isNullOrBlank()) {
                    if (isScanning.compareAndSet(true, false)) {
                        onQrCodeScanned(scannedText)
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            imageProxy.close()
        }
    }

    fun resumeScanning() {
        isScanning.set(true)
    }

    fun pauseScanning() {
        isScanning.set(false)
    }
}
