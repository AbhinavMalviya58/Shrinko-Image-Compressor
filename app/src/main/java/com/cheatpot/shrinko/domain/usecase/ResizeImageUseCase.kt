package com.cheatpot.shrinko.domain.usecase

import com.cheatpot.shrinko.domain.model.CompressionResult
import com.cheatpot.shrinko.domain.model.CompressionSettings
import com.cheatpot.shrinko.domain.model.ImageInfo
import com.cheatpot.shrinko.domain.model.ResizeSettings
import com.cheatpot.shrinko.domain.repository.ImageRepository

class ResizeImageUseCase(
    private val imageRepository: ImageRepository
) {
    suspend operator fun invoke(
        imageInfo: ImageInfo,
        resizeSettings: ResizeSettings,
        compressionSettings: CompressionSettings
    ): Result<CompressionResult> {
        return imageRepository.processImage(imageInfo, compressionSettings, resizeSettings)
    }
}
