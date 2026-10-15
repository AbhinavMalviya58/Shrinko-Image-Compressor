package com.cheatpot.shrinko.domain.usecase

import com.cheatpot.shrinko.domain.model.CompressionResult
import com.cheatpot.shrinko.domain.model.CompressionSettings
import com.cheatpot.shrinko.domain.model.HistoryItem
import com.cheatpot.shrinko.domain.model.ImageInfo
import com.cheatpot.shrinko.domain.model.ResizeSettings
import com.cheatpot.shrinko.domain.repository.ImageRepository

class CompressImageUseCase(
    private val imageRepository: ImageRepository
) {
    suspend operator fun invoke(
        imageInfo: ImageInfo,
        compressionSettings: CompressionSettings,
        resizeSettings: ResizeSettings
    ): Result<CompressionResult> {
        val result = imageRepository.processImage(imageInfo, compressionSettings, resizeSettings)
        result.getOrNull()?.let { compResult ->
            imageRepository.addToHistory(
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
        return result
    }
}
