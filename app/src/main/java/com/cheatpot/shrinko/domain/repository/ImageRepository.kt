package com.cheatpot.shrinko.domain.repository

import android.net.Uri
import com.cheatpot.shrinko.domain.model.AppSettings
import com.cheatpot.shrinko.domain.model.CompressionResult
import com.cheatpot.shrinko.domain.model.CompressionSettings
import com.cheatpot.shrinko.domain.model.HistoryItem
import com.cheatpot.shrinko.domain.model.ImageInfo
import com.cheatpot.shrinko.domain.model.ResizeSettings
import kotlinx.coroutines.flow.Flow

interface ImageRepository {
    suspend fun getImageInfo(uri: Uri): Result<ImageInfo>

    suspend fun processImage(
        imageInfo: ImageInfo,
        compressionSettings: CompressionSettings,
        resizeSettings: ResizeSettings
    ): Result<CompressionResult>

    suspend fun processBatch(
        images: List<ImageInfo>,
        compressionSettings: CompressionSettings,
        resizeSettings: ResizeSettings,
        onProgress: (processed: Int, total: Int, currentResult: CompressionResult?) -> Unit
    ): List<Result<CompressionResult>>

    suspend fun saveImageToGallery(result: CompressionResult): Result<Uri>

    fun getHistory(): Flow<List<HistoryItem>>

    suspend fun addToHistory(item: HistoryItem)

    suspend fun removeFromHistory(id: String)

    suspend fun clearHistory()

    fun getAppSettings(): Flow<AppSettings>

    suspend fun updateAppSettings(settings: AppSettings)
}
