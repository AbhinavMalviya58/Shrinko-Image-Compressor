package com.cheatpot.shrinko.data.repository

import android.content.Context
import android.net.Uri
import com.cheatpot.shrinko.data.image.ImageCompressor
import com.cheatpot.shrinko.data.image.ImageDecoder
import com.cheatpot.shrinko.data.image.ImageEncoder
import com.cheatpot.shrinko.data.image.ImageMetadataReader
import com.cheatpot.shrinko.data.image.ImageResizer
import com.cheatpot.shrinko.data.image.ImageSaver
import com.cheatpot.shrinko.data.preferences.HistoryDataStore
import com.cheatpot.shrinko.data.preferences.SettingsDataStore
import com.cheatpot.shrinko.domain.model.AppSettings
import com.cheatpot.shrinko.domain.model.CompressionResult
import com.cheatpot.shrinko.domain.model.CompressionSettings
import com.cheatpot.shrinko.domain.model.HistoryItem
import com.cheatpot.shrinko.domain.model.ImageInfo
import com.cheatpot.shrinko.domain.model.ResizeSettings
import com.cheatpot.shrinko.domain.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class ImageRepositoryImpl(
    private val context: Context,
    private val metadataReader: ImageMetadataReader = ImageMetadataReader(context),
    private val decoder: ImageDecoder = ImageDecoder(context),
    private val resizer: ImageResizer = ImageResizer(),
    private val encoder: ImageEncoder = ImageEncoder(),
    private val compressor: ImageCompressor = ImageCompressor(context, decoder, resizer, encoder),
    private val saver: ImageSaver = ImageSaver(context),
    private val settingsDataStore: SettingsDataStore = SettingsDataStore(context),
    private val historyDataStore: HistoryDataStore = HistoryDataStore(context)
) : ImageRepository {

    override suspend fun getImageInfo(uri: Uri): Result<ImageInfo> = withContext(Dispatchers.IO) {
        metadataReader.readImageInfo(uri)
    }

    override suspend fun processImage(
        imageInfo: ImageInfo,
        compressionSettings: CompressionSettings,
        resizeSettings: ResizeSettings
    ): Result<CompressionResult> = withContext(Dispatchers.Default) {
        compressor.compressImage(imageInfo, compressionSettings, resizeSettings)
    }

    override suspend fun processBatch(
        images: List<ImageInfo>,
        compressionSettings: CompressionSettings,
        resizeSettings: ResizeSettings,
        onProgress: (processed: Int, total: Int, currentResult: CompressionResult?) -> Unit
    ): List<Result<CompressionResult>> = withContext(Dispatchers.Default) {
        val results = mutableListOf<Result<CompressionResult>>()
        val semaphore = Semaphore(2) // Bounded concurrency to protect memory on large images
        var processedCount = 0

        images.forEach { imageInfo ->
            semaphore.withPermit {
                val res = compressor.compressImage(imageInfo, compressionSettings, resizeSettings)
                results.add(res)
                processedCount++

                val compResult = res.getOrNull()
                if (compResult != null) {
                    historyDataStore.addHistoryItem(
                        HistoryItem(
                            id = compResult.id,
                            originalUriString = compResult.originalInfo.uriString,
                            outputUriString = compResult.outputFileUri,
                            filename = compResult.originalInfo.name,
                            originalSizeBytes = compResult.originalInfo.sizeBytes,
                            outputSizeBytes = compResult.outputSizeBytes,
                            savedPercentage = compResult.savedPercentage,
                            outputWidth = compResult.outputWidth,
                            outputHeight = compResult.outputHeight,
                            outputFormat = compResult.outputFormat
                        )
                    )
                }

                onProgress(processedCount, images.size, compResult)
            }
        }
        results
    }

    override suspend fun saveImageToGallery(result: CompressionResult): Result<Uri> = withContext(Dispatchers.IO) {
        saver.saveToGallery(result)
    }

    override fun getHistory(): Flow<List<HistoryItem>> = historyDataStore.historyFlow

    override suspend fun addToHistory(item: HistoryItem) = historyDataStore.addHistoryItem(item)

    override suspend fun removeFromHistory(id: String) = historyDataStore.removeHistoryItem(id)

    override suspend fun clearHistory() = historyDataStore.clearHistory()

    override fun getAppSettings(): Flow<AppSettings> = settingsDataStore.appSettingsFlow

    override suspend fun updateAppSettings(settings: AppSettings) = settingsDataStore.updateSettings(settings)
}
