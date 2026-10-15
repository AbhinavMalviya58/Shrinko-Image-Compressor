package com.cheatpot.shrinko.data.image

import android.graphics.Bitmap
import com.cheatpot.shrinko.domain.model.ImageInfo
import com.cheatpot.shrinko.domain.model.ResizeMode
import com.cheatpot.shrinko.domain.model.ResizeSettings
import kotlin.math.roundToInt

class ImageResizer {

    fun calculateDimensions(
        originalWidth: Int,
        originalHeight: Int,
        resizeSettings: ResizeSettings
    ): Pair<Int, Int> {
        if (originalWidth <= 0 || originalHeight <= 0) return Pair(100, 100)

        val aspect = originalWidth.toFloat() / originalHeight.toFloat()

        return when (resizeSettings.mode) {
            ResizeMode.NONE -> Pair(originalWidth, originalHeight)

            ResizeMode.PERCENTAGE -> {
                val scale = (resizeSettings.percentage.coerceIn(1, 100) / 100f)
                val targetW = (originalWidth * scale).roundToInt().coerceAtLeast(1)
                val targetH = (originalHeight * scale).roundToInt().coerceAtLeast(1)
                Pair(targetW, targetH)
            }

            ResizeMode.CUSTOM_DIMENSIONS -> {
                val customW = resizeSettings.customWidth.coerceAtLeast(1)
                val customH = resizeSettings.customHeight.coerceAtLeast(1)

                if (resizeSettings.lockAspectRatio) {
                    if (resizeSettings.fitInBounds) {
                        // Fit within bounding box of customW x customH while preserving original aspect ratio
                        if (originalWidth.toFloat() / customW > originalHeight.toFloat() / customH) {
                            val newW = customW
                            val newH = (customW / aspect).roundToInt().coerceAtLeast(1)
                            Pair(newW, newH)
                        } else {
                            val newH = customH
                            val newW = (customH * aspect).roundToInt().coerceAtLeast(1)
                            Pair(newW, newH)
                        }
                    } else {
                        val newW = customW
                        val newH = (customW / aspect).roundToInt().coerceAtLeast(1)
                        Pair(newW, newH)
                    }
                } else {
                    Pair(customW, customH)
                }
            }

            ResizeMode.SOCIAL_PRESET -> {
                val preset = resizeSettings.socialPreset
                if (preset != null) {
                    if (preset.width == preset.height) {
                        // Square (e.g. 1080x1080)
                        Pair(preset.width, preset.height)
                    } else if (resizeSettings.fitInBounds) {
                        // Fit in bounds preset
                        if (originalWidth.toFloat() / preset.width > originalHeight.toFloat() / preset.height) {
                            val newW = preset.width
                            val newH = (preset.width / aspect).roundToInt().coerceAtLeast(1)
                            Pair(newW, newH)
                        } else {
                            val newH = preset.height
                            val newW = (preset.height * aspect).roundToInt().coerceAtLeast(1)
                            Pair(newW, newH)
                        }
                    } else {
                        Pair(preset.width, preset.height)
                    }
                } else {
                    Pair(originalWidth, originalHeight)
                }
            }

            ResizeMode.AUTO_RESIZE -> {
                // Auto fit max bounds (e.g. 1920 max)
                val maxDim = 1920
                if (originalWidth > maxDim || originalHeight > maxDim) {
                    if (originalWidth >= originalHeight) {
                        val newW = maxDim
                        val newH = (maxDim / aspect).roundToInt().coerceAtLeast(1)
                        Pair(newW, newH)
                    } else {
                        val newH = maxDim
                        val newW = (maxDim * aspect).roundToInt().coerceAtLeast(1)
                        Pair(newW, newH)
                    }
                } else {
                    Pair(originalWidth, originalHeight)
                }
            }
        }
    }

    fun resizeBitmap(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        if (source.width == targetWidth && source.height == targetHeight) {
            return source
        }
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }
}
