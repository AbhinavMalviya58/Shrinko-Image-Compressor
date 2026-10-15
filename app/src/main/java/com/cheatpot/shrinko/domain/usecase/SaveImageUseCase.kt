package com.cheatpot.shrinko.domain.usecase

import android.net.Uri
import com.cheatpot.shrinko.domain.model.CompressionResult
import com.cheatpot.shrinko.domain.repository.ImageRepository

class SaveImageUseCase(
    private val imageRepository: ImageRepository
) {
    suspend operator fun invoke(result: CompressionResult): Result<Uri> {
        return imageRepository.saveImageToGallery(result)
    }
}
