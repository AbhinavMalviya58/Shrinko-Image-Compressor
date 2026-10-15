package com.cheatpot.shrinko.domain.model

import android.net.Uri

data class ImageInfo(
    val uriString: String,
    val name: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val orientation: Int = 0
) {
    val uri: Uri
        get() = Uri.parse(uriString)

    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height.toFloat() else 1.0f

    val formattedSize: String
        get() = formatFileSize(sizeBytes)

    val extension: String
        get() = when {
            mimeType.contains("png", ignoreCase = true) -> "png"
            mimeType.contains("webp", ignoreCase = true) -> "webp"
            else -> "jpg"
        }

    val originalFormat: OutputFormat
        get() = when {
            mimeType.contains("png", ignoreCase = true) -> OutputFormat.PNG
            mimeType.contains("webp", ignoreCase = true) -> OutputFormat.WEBP
            else -> OutputFormat.JPEG
        }
}

enum class OutputFormat(val displayName: String, val mimeType: String, val extension: String, val supportsQuality: Boolean) {
    JPEG("JPEG", "image/jpeg", ".jpg", true),
    PNG("PNG", "image/png", ".png", false),
    WEBP("WebP", "image/webp", ".webp", true)
}

enum class CompressionMode(val displayName: String, val description: String) {
    AUTO("Auto", "Smart balance of file size and visual quality"),
    TARGET_SIZE("Target Size", "Compress to approach a specific target file size"),
    QUALITY("Quality", "Manual compression quality slider"),
    CUSTOM("Custom", "Full control over format, quality, and dimensions")
}

enum class AutoQualityPreference(val displayName: String, val targetScale: Float, val defaultQuality: Int) {
    MAXIMUM_QUALITY("Maximum Quality", 1.0f, 90),
    BALANCED("Balanced", 0.85f, 75),
    SMALLER_FILE("Smaller File", 0.70f, 50)
}

enum class ResizeMode(val displayName: String) {
    NONE("Original Size"),
    AUTO_RESIZE("Auto Fit"),
    PERCENTAGE("Percentage"),
    CUSTOM_DIMENSIONS("Custom (px)"),
    SOCIAL_PRESET("Social Presets")
}

enum class SocialPreset(val displayName: String, val width: Int, val height: Int, val description: String) {
    INSTAGRAM_POST("Instagram Post", 1080, 1080, "1:1 Square"),
    INSTAGRAM_PORTRAIT("Instagram Portrait", 1080, 1350, "4:5 Portrait"),
    INSTAGRAM_STORY("Instagram Story", 1080, 1920, "9:16 Full Screen"),
    WHATSAPP_SHARE("WhatsApp Share", 1280, 1280, "1280px Max Box"),
    STANDARD_HD("Standard HD", 1920, 1080, "16:9 Landscape"),
    FULL_HD_PORTRAIT("Full HD Portrait", 1080, 1920, "9:16 Portrait")
}

data class CompressionSettings(
    val mode: CompressionMode = CompressionMode.AUTO,
    val quality: Int = 75,
    val targetSizeBytes: Long = 200 * 1024L, // Default 200 KB
    val autoQualityPreference: AutoQualityPreference = AutoQualityPreference.BALANCED,
    val outputFormat: OutputFormat = OutputFormat.JPEG,
    val keepMetadata: Boolean = true
)

data class ResizeSettings(
    val mode: ResizeMode = ResizeMode.NONE,
    val percentage: Int = 100,
    val customWidth: Int = 1080,
    val customHeight: Int = 1080,
    val lockAspectRatio: Boolean = true,
    val fitInBounds: Boolean = true,
    val socialPreset: SocialPreset? = null
)

data class CompressionResult(
    val id: String,
    val originalInfo: ImageInfo,
    val outputFileUri: String,
    val outputSizeBytes: Long,
    val outputWidth: Int,
    val outputHeight: Int,
    val outputFormat: OutputFormat,
    val qualityUsed: Int,
    val iterationsCount: Int,
    val targetSizeBytes: Long? = null,
    val targetAchieved: Boolean = true,
    val dimensionsReduced: Boolean = false,
    val processingTimeMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) {
    val savedBytes: Long
        get() = (originalInfo.sizeBytes - outputSizeBytes).coerceAtLeast(0L)

    val savedPercentage: Float
        get() = if (originalInfo.sizeBytes > 0) {
            ((savedBytes.toFloat() / originalInfo.sizeBytes.toFloat()) * 100f).coerceIn(0f, 99.9f)
        } else 0f

    val formattedOriginalSize: String
        get() = formatFileSize(originalInfo.sizeBytes)

    val formattedOutputSize: String
        get() = formatFileSize(outputSizeBytes)

    val formattedSavedBytes: String
        get() = formatFileSize(savedBytes)
}

data class HistoryItem(
    val id: String,
    val originalUriString: String,
    val outputUriString: String,
    val filename: String,
    val originalSizeBytes: Long,
    val outputSizeBytes: Long,
    val savedPercentage: Float,
    val outputWidth: Int,
    val outputHeight: Int,
    val outputFormat: OutputFormat,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedOriginalSize: String
        get() = formatFileSize(originalSizeBytes)

    val formattedOutputSize: String
        get() = formatFileSize(outputSizeBytes)
}

enum class ThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    DARK("Dark Theme"),
    LIGHT("Light Theme")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultOutputFormat: OutputFormat = OutputFormat.JPEG,
    val defaultCompressionMode: CompressionMode = CompressionMode.AUTO,
    val defaultQuality: Int = 75,
    val defaultResizeMode: ResizeMode = ResizeMode.NONE
)

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    if (digitGroups >= units.size) digitGroups = units.size - 1
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return if (digitGroups == 0) {
        "$bytes B"
    } else {
        String.format("%.1f %s", value, units[digitGroups])
    }
}
