package com.cheatpot.shrinko.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Log

class ImageDecoder(private val context: Context) {

    fun decodeBitmap(uri: Uri, targetMaxDimension: Int = 4096, orientation: Int = 0): Bitmap? {
        return try {
            val decoded: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                decodeWithModernApi(uri, targetMaxDimension)
            } else {
                decodeWithBitmapFactory(uri, targetMaxDimension)
            }
            if (decoded == null) {
                Log.w("PixelShrink", "decode returned null for $uri")
                return null
            }
            applyOrientation(decoded, orientation)
        } catch (primary: Exception) {
            Log.e("PixelShrink", "Primary decode failed for $uri", primary)
            try {
                val fallback = decodeWithBitmapFactory(uri, targetMaxDimension) ?: return null
                applyOrientation(fallback, orientation)
            } catch (e2: Exception) {
                Log.e("PixelShrink", "Fallback also failed", e2)
                null
            }
        }
    }

    private fun decodeWithModernApi(uri: Uri, targetMaxDimension: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        @Suppress("NewApi")
        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
        @Suppress("NewApi")
        return android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val origW = info.size.width
            val origH = info.size.height
            val sample = calculateInSampleSize(origW, origH, targetMaxDimension, targetMaxDimension)
            if (sample > 1) {
                decoder.setTargetSize(
                    (origW / sample).coerceAtLeast(1),
                    (origH / sample).coerceAtLeast(1)
                )
            }
            @Suppress("NewApi")
            decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    private fun decodeWithBitmapFactory(uri: Uri, targetMaxDimension: Int): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        } ?: return null
        val sample = calculateInSampleSize(
            boundsOptions.outWidth, boundsOptions.outHeight, targetMaxDimension, targetMaxDimension
        )
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        if (orientation == 0) return bitmap
        val matrix = Matrix().apply { postRotate(orientation.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun calculateInSampleSize(
        width: Int, height: Int, reqWidth: Int, reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfH = height / 2
            val halfW = width / 2
            while (halfH / inSampleSize >= reqHeight && halfW / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}