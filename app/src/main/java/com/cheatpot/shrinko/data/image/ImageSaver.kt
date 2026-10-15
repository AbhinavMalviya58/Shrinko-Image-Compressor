package com.cheatpot.shrinko.data.image

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.cheatpot.shrinko.domain.model.CompressionResult
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageSaver(private val context: Context) {

    fun saveToGallery(result: CompressionResult): Result<Uri> {
        return runCatching {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "Shrinko_${timestamp}${result.outputFormat.extension}"
            val mimeType = result.outputFormat.mimeType

            val sourceFile = File(Uri.parse(result.outputFileUri).path ?: "")
            if (!sourceFile.exists()) {
                throw IllegalStateException("Compressed file not found at path: ${result.outputFileUri}")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Shrinko")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = context.contentResolver.insert(collection, contentValues)
                    ?: throw IllegalStateException("Failed to insert MediaStore record")

                context.contentResolver.openOutputStream(itemUri)?.use { outStream ->
                    FileInputStream(sourceFile).use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(itemUri, contentValues, null, null)

                itemUri
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val shrinkoFolder = File(picturesDir, "Shrinko").apply { if (!exists()) mkdirs() }
                val destFile = File(shrinkoFolder, filename)

                FileInputStream(sourceFile).use { inStream ->
                    FileOutputStream(destFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, destFile.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: Uri.fromFile(destFile)
            }
        }
    }
}
