package com.cheatpot.shrinko.data.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import com.cheatpot.shrinko.domain.model.OutputFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ImageEncoder {

    fun encodeToBytes(bitmap: Bitmap, format: OutputFormat, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        val processedBitmap = prepareBitmapForFormat(bitmap, format)
        val compressFormat = getCompressFormat(format)
        val clampedQuality = quality.coerceIn(1, 100)

        processedBitmap.compress(compressFormat, clampedQuality, stream)

        if (processedBitmap != bitmap) {
            processedBitmap.recycle()
        }

        return stream.toByteArray()
    }

    fun encodeToFile(bitmap: Bitmap, format: OutputFormat, quality: Int, outputFile: File): Long {
        val processedBitmap = prepareBitmapForFormat(bitmap, format)
        val compressFormat = getCompressFormat(format)
        val clampedQuality = quality.coerceIn(1, 100)

        FileOutputStream(outputFile).use { out ->
            processedBitmap.compress(compressFormat, clampedQuality, out)
            out.flush()
        }

        if (processedBitmap != bitmap) {
            processedBitmap.recycle()
        }

        return outputFile.length()
    }

    private fun prepareBitmapForFormat(bitmap: Bitmap, format: OutputFormat): Bitmap {
        return if (format == OutputFormat.JPEG && bitmap.hasAlpha()) {
            val solidBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(solidBitmap)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            solidBitmap
        } else {
            bitmap
        }
    }

    @Suppress("DEPRECATION")
    private fun getCompressFormat(format: OutputFormat): Bitmap.CompressFormat {
        return when (format) {
            OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
            OutputFormat.PNG -> Bitmap.CompressFormat.PNG
            OutputFormat.WEBP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    Bitmap.CompressFormat.WEBP
                }
            }
        }
    }
}
