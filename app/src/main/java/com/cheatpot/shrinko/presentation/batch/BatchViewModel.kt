package com.cheatpot.shrinko.presentation.batch

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cheatpot.shrinko.domain.model.CompressionResult
import com.cheatpot.shrinko.domain.model.CompressionSettings
import com.cheatpot.shrinko.domain.model.ImageInfo
import com.cheatpot.shrinko.domain.model.ResizeSettings
import com.cheatpot.shrinko.domain.repository.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BatchUiState(
    val totalCount: Int = 0,
    val processedCount: Int = 0,
    val isProcessing: Boolean = false,
    val results: List<CompressionResult> = emptyList(),
    val savedUris: List<Uri> = emptyList(),
    val isSavingAll: Boolean = false
)

class BatchViewModel(
    private val repository: ImageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BatchUiState())
    val uiState: StateFlow<BatchUiState> = _uiState.asStateFlow()

    fun startBatchProcessing(
        images: List<ImageInfo>,
        compressionSettings: CompressionSettings,
        resizeSettings: ResizeSettings
    ) {
        viewModelScope.launch {
            _uiState.value = BatchUiState(
                totalCount = images.size,
                processedCount = 0,
                isProcessing = true,
                results = emptyList()
            )

            val resultsList = mutableListOf<CompressionResult>()

            repository.processBatch(
                images = images,
                compressionSettings = compressionSettings,
                resizeSettings = resizeSettings,
                onProgress = { processed, total, currentResult ->
                    if (currentResult != null) {
                        resultsList.add(currentResult)
                    }
                    _uiState.value = _uiState.value.copy(
                        processedCount = processed,
                        results = resultsList.toList(),
                        isProcessing = processed < total
                    )
                }
            )

            _uiState.value = _uiState.value.copy(isProcessing = false)
        }
    }

    fun saveAllToGallery(onComplete: (Int) -> Unit) {
        val results = _uiState.value.results
        if (results.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingAll = true)
            val savedUris = mutableListOf<Uri>()

            results.forEach { result ->
                val res = repository.saveImageToGallery(result)
                res.getOrNull()?.let { uri -> savedUris.add(uri) }
            }

            _uiState.value = _uiState.value.copy(
                savedUris = savedUris,
                isSavingAll = false
            )

            onComplete(savedUris.size)
        }
    }
}
