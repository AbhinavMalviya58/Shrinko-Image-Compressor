package com.cheatpot.shrinko.data.image

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.cheatpot.shrinko.domain.model.ImageInfo
import java.io.File

class ImageMetadataReader(private val context: Context) {

    fun readImageInfo(uri: Uri): Result<ImageInfo> {
        return runCatching {
            var name = "image_${System.currentTimeMillis()}"
            var sizeBytes = 0L

            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) {
                            val displayName = cursor.getString(nameIndex)
                            if (!displayName.isNullOrBlank()) name = displayName
                        }
                        if (sizeIndex != -1) {
                            sizeBytes = cursor.getLong(sizeIndex)
                        }
                    }
                }

                if (sizeBytes <= 0L) {
                    runCatching {
                        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                            if (afd.length > 0) {
                                sizeBytes = afd.length
                            }
                        }
                    }
                }
            } else if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    name = file.name
                    sizeBytes = file.length()
                }
            }

            // Fallback for file size if query returned 0
            if (sizeBytes <= 0L) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    var count = 0L
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (stream.read(buffer).also { read = it } != -1) {
                        count += read
                    }
                    sizeBytes = count
                }
            }

            // Decode dimensions only (inJustDecodeBounds reads JPEG/PNG/WebP header bytes only,
            // it does NOT decompress pixel data, so it works fine with content:// URIs).
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            val rawWidth = options.outWidth
            val rawHeight = options.outHeight
            val finalMimeType = options.outMimeType
                ?: context.contentResolver.getType(uri)
                ?: "image/jpeg"

            // Read EXIF orientation
            var orientationDegrees = 0
            context.contentResolver.openInputStream(uri)?.use { stream ->
                runCatching {
                    val exif = ExifInterface(stream)
                    val exifOrientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    orientationDegrees = when (exifOrientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }
            }

            // Swap dimensions if orientation is rotated by 90 or 270 degrees
            val finalWidth = if (orientationDegrees == 90 || orientationDegrees == 270) rawHeight else rawWidth
            val finalHeight = if (orientationDegrees == 90 || orientationDegrees == 270) rawWidth else rawHeight

            if (finalWidth <= 0 || finalHeight <= 0) {
                throw IllegalArgumentException("Unable to decode valid image dimensions for $uri")
            }

            ImageInfo(
                uriString = uri.toString(),
                name = name,
                sizeBytes = sizeBytes,
                width = finalWidth,
                height = finalHeight,
                mimeType = finalMimeType,
                orientation = orientationDegrees
            )
        }
    }
}
