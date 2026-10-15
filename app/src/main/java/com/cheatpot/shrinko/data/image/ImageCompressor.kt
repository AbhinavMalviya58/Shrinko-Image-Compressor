package com.cheatpot.shrinko.data.image

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cheatpot.shrinko.domain.model.AutoQualityPreference
import com.cheatpot.shrinko.domain.model.CompressionMode
import com.cheatpot.shrinko.domain.model.CompressionResult
import com.cheatpot.shrinko.domain.model.CompressionSettings
import com.cheatpot.shrinko.domain.model.ImageInfo
import com.cheatpot.shrinko.domain.model.OutputFormat
import com.cheatpot.shrinko.domain.model.ResizeMode
import com.cheatpot.shrinko.domain.model.ResizeSettings
import java.io.File
import java.util.UUID

class ImageCompressor(
    private val context: Context,
    private val decoder: ImageDecoder,
    private val resizer: ImageResizer,
    private val encoder: ImageEncoder
) {

    fun compressImage(
        originalInfo: ImageInfo,
        compressionSettings: CompressionSettings,
        resizeSettings: ResizeSettings
    ): Result<CompressionResult> {
        val startTime = System.currentTimeMillis()

        return runCatching {
            val cacheDir = File(context.cacheDir, "compressed_images").apply { if (!exists()) mkdirs() }

            // 1. Initial dimensions calculation
            var (targetWidth, targetHeight) = resizer.calculateDimensions(
                originalInfo.width,
                originalInfo.height,
                resizeSettings
            )

            val uri = Uri.parse(originalInfo.uriString)

            // Decode initial bitmap respecting target dimensions
            val maxDim = maxOf(targetWidth, targetHeight, 1024)
            val currentBitmap = decoder.decodeBitmap(uri, targetMaxDimension = maxDim, orientation = originalInfo.orientation)
                ?: throw IllegalStateException("Failed to decode image bitmap from URI")
            Log.d("ImageCompressor", "Initial bitmap dimensions: ${currentBitmap.width}x${currentBitmap.height}")

            var resizedBitmap = resizer.resizeBitmap(currentBitmap, targetWidth, targetHeight)

            val format = compressionSettings.outputFormat
            var bestQualityUsed = compressionSettings.quality
            var iterations = 0
            var dimensionsWereReduced = (targetWidth < originalInfo.width || targetHeight < originalInfo.height)

            var bestFileBytes: ByteArray? = null
            var targetAchieved = true

            when (compressionSettings.mode) {
                CompressionMode.QUALITY -> {
                    iterations = 1
                    bestQualityUsed = compressionSettings.quality
                    bestFileBytes = encoder.encodeToBytes(resizedBitmap, format, bestQualityUsed)
                }

                CompressionMode.AUTO -> {
                    val pref = compressionSettings.autoQualityPreference
                    bestQualityUsed = pref.defaultQuality

                    if (pref == AutoQualityPreference.SMALLER_FILE && resizeSettings.mode == ResizeMode.NONE && originalInfo.sizeBytes > 1024 * 1024) {
                        targetWidth = (targetWidth * 0.85f).toInt().coerceAtLeast(1)
                        targetHeight = (targetHeight * 0.85f).toInt().coerceAtLeast(1)
                        val newResized = resizer.resizeBitmap(currentBitmap, targetWidth, targetHeight)
                        if (newResized != resizedBitmap && resizedBitmap != currentBitmap) resizedBitmap.recycle()
                        resizedBitmap = newResized
                        dimensionsWereReduced = true
                    }

                    iterations = 1
                    bestFileBytes = encoder.encodeToBytes(resizedBitmap, format, bestQualityUsed)
                }

                CompressionMode.TARGET_SIZE, CompressionMode.CUSTOM -> {
                    val targetSizeBytes = compressionSettings.targetSizeBytes.coerceAtLeast(5 * 1024L)

                    if (!format.supportsQuality) {
                        // Lossless PNG format: step down dimensions if target size not met
                        var scaleFactor = 1.0f
                        while (iterations < 6) {
                            iterations++
                            val bytes = encoder.encodeToBytes(resizedBitmap, format, 100)
                            bestFileBytes = bytes

                            if (bytes.size <= targetSizeBytes || scaleFactor <= 0.3f) {
                                break
                            }

                            scaleFactor *= 0.8f
                            val newW = (targetWidth * scaleFactor).toInt().coerceAtLeast(1)
                            val newH = (targetHeight * scaleFactor).toInt().coerceAtLeast(1)
                            val scaled = resizer.resizeBitmap(currentBitmap, newW, newH)
                            if (scaled != resizedBitmap && resizedBitmap != currentBitmap) resizedBitmap.recycle()
                            resizedBitmap = scaled
                            dimensionsWereReduced = true
                        }
                        targetAchieved = (bestFileBytes?.size?.toLong() ?: Long.MAX_VALUE) <= targetSizeBytes
                    } else {
                        // Lossy format (JPEG / WebP): Binary search quality optimization
                        var lowQuality = 10
                        var highQuality = 95
                        var optimalBytes: ByteArray? = null
                        var optimalQuality = compressionSettings.quality
                        var dimensionScale = 1.0f

                        while (iterations < 12) {
                            iterations++

                            if (lowQuality > highQuality) {
                                if (optimalBytes == null && dimensionScale > 0.4f &&
                                    (resizeSettings.mode == ResizeMode.AUTO_RESIZE || compressionSettings.mode == CompressionMode.TARGET_SIZE)
                                ) {
                                    // Scale down dimensions and retry search
                                    dimensionScale *= 0.8f
                                    val newW = (targetWidth * dimensionScale).toInt().coerceAtLeast(1)
                                    val newH = (targetHeight * dimensionScale).toInt().coerceAtLeast(1)
                                    val scaled = resizer.resizeBitmap(currentBitmap, newW, newH)
                                    if (scaled != resizedBitmap && resizedBitmap != currentBitmap) resizedBitmap.recycle()
                                    resizedBitmap = scaled
                                    dimensionsWereReduced = true

                                    lowQuality = 10
                                    highQuality = 90
                                    continue
                                } else {
                                    break
                                }
                            }

                            val midQuality = (lowQuality + highQuality) / 2
                            val candidateBytes = encoder.encodeToBytes(resizedBitmap, format, midQuality)
                            val candidateSize = candidateBytes.size.toLong()

                            if (candidateSize <= targetSizeBytes) {
                                optimalBytes = candidateBytes
                                optimalQuality = midQuality
                                lowQuality = midQuality + 1
                            } else {
                                highQuality = midQuality - 1
                            }
                        }

                        if (optimalBytes == null) {
                            optimalBytes = encoder.encodeToBytes(resizedBitmap, format, 10)
                            optimalQuality = 10
                            targetAchieved = optimalBytes.size <= targetSizeBytes
                        } else {
                            targetAchieved = optimalBytes.size <= (targetSizeBytes * 1.05)
                        }

                        bestFileBytes = optimalBytes
                        bestQualityUsed = optimalQuality
                    }
                }
            }

            val finalWidth = resizedBitmap.width
            val finalHeight = resizedBitmap.height

            // Cleanup bitmaps safely AFTER recording finalWidth and finalHeight
            if (resizedBitmap != currentBitmap && !resizedBitmap.isRecycled) {
                resizedBitmap.recycle()
            }
            if (!currentBitmap.isRecycled) {
                currentBitmap.recycle()
            }

            val finalBytes = bestFileBytes ?: throw IllegalStateException("Compression produced empty output")

            val id = UUID.randomUUID().toString()
            val outputFile = File(cacheDir, "Shrinko_${id}${format.extension}")
            outputFile.writeBytes(finalBytes)

            val endTime = System.currentTimeMillis()

            CompressionResult(
                id = id,
                originalInfo = originalInfo,
                outputFileUri = Uri.fromFile(outputFile).toString(),
                outputSizeBytes = outputFile.length(),
                outputWidth = finalWidth,
                outputHeight = finalHeight,
                outputFormat = format,
                qualityUsed = bestQualityUsed,
                iterationsCount = iterations,
                targetSizeBytes = if (compressionSettings.mode == CompressionMode.TARGET_SIZE) compressionSettings.targetSizeBytes else null,
                targetAchieved = targetAchieved,
                dimensionsReduced = dimensionsWereReduced,
                processingTimeMs = endTime - startTime
            )
        }
    }
}
