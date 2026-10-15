package com.cheatpot.shrinko.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cheatpot.shrinko.domain.model.AutoQualityPreference
import com.cheatpot.shrinko.domain.model.CompressionMode
import com.cheatpot.shrinko.domain.model.CompressionResult
import com.cheatpot.shrinko.domain.model.CompressionSettings
import com.cheatpot.shrinko.domain.model.ImageInfo
import com.cheatpot.shrinko.domain.model.OutputFormat
import com.cheatpot.shrinko.domain.model.ResizeMode
import com.cheatpot.shrinko.domain.model.ResizeSettings
import com.cheatpot.shrinko.domain.model.SocialPreset
import com.cheatpot.shrinko.domain.usecase.CompressImageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state used by both the combined ImageEditorScreen and the single-tool screens.
 *
 * [compressEnabled] / [resizeEnabled] / [convertEnabled] act as per-operation toggles
 * for the combined workflow. For single-tool screens they are pre-set by the screen
 * itself (via LaunchedEffect) so the ViewModel stays free of routing logic.
 */
data class ImageEditorUiState(
    val imageInfo: ImageInfo? = null,
    val compressionSettings: CompressionSettings = CompressionSettings(),
    val resizeSettings: ResizeSettings = ResizeSettings(),
    // Op toggles (combined workflow)
    val compressEnabled: Boolean = true,
    val resizeEnabled: Boolean = false,
    val convertEnabled: Boolean = false,
    val isProcessing: Boolean = false,
    val result: CompressionResult? = null,
    val errorMessage: String? = null
)

// Keep backward compat alias so any code referencing EditorUiState still compiles
typealias EditorUiState = ImageEditorUiState

class ImageEditorViewModel(
    private val compressImageUseCase: CompressImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageEditorUiState())
    val uiState: StateFlow<ImageEditorUiState> = _uiState.asStateFlow()

    // ── Image ─────────────────────────────────────────────────────────────────
    fun setImageInfo(info: ImageInfo) {
        _uiState.value = _uiState.value.copy(
            imageInfo = info,
            resizeSettings = _uiState.value.resizeSettings.copy(
                customWidth = info.width,
                customHeight = info.height
            )
        )
    }

    // ── Op toggles ────────────────────────────────────────────────────────────
    fun setCompressEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(compressEnabled = enabled)
    }

    fun setResizeEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(resizeEnabled = enabled)
    }

    fun setConvertEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(convertEnabled = enabled)
    }

    // ── Compression settings ──────────────────────────────────────────────────
    fun setCompressionMode(mode: CompressionMode) {
        _uiState.value = _uiState.value.copy(
            compressionSettings = _uiState.value.compressionSettings.copy(mode = mode)
        )
    }

    fun setQuality(quality: Int) {
        _uiState.value = _uiState.value.copy(
            compressionSettings = _uiState.value.compressionSettings.copy(
                quality = quality.coerceIn(1, 100)
            )
        )
    }

    fun setTargetSizeBytes(bytes: Long) {
        _uiState.value = _uiState.value.copy(
            compressionSettings = _uiState.value.compressionSettings.copy(targetSizeBytes = bytes)
        )
    }

    fun setAutoQualityPreference(pref: AutoQualityPreference) {
        _uiState.value = _uiState.value.copy(
            compressionSettings = _uiState.value.compressionSettings.copy(autoQualityPreference = pref)
        )
    }

    fun setOutputFormat(format: OutputFormat) {
        _uiState.value = _uiState.value.copy(
            compressionSettings = _uiState.value.compressionSettings.copy(outputFormat = format)
        )
    }

    // ── Resize settings ───────────────────────────────────────────────────────
    fun setResizeMode(mode: ResizeMode) {
        _uiState.value = _uiState.value.copy(
            resizeSettings = _uiState.value.resizeSettings.copy(mode = mode)
        )
    }

    fun setResizePercentage(pct: Int) {
        _uiState.value = _uiState.value.copy(
            resizeSettings = _uiState.value.resizeSettings.copy(percentage = pct)
        )
    }

    fun setCustomWidth(width: Int) {
        val current = _uiState.value.resizeSettings
        val info = _uiState.value.imageInfo
        val newHeight = if (current.lockAspectRatio && info != null && info.width > 0) {
            (width / info.aspectRatio).toInt().coerceAtLeast(1)
        } else {
            current.customHeight
        }
        _uiState.value = _uiState.value.copy(
            resizeSettings = current.copy(customWidth = width, customHeight = newHeight)
        )
    }

    fun setCustomHeight(height: Int) {
        val current = _uiState.value.resizeSettings
        val info = _uiState.value.imageInfo
        val newWidth = if (current.lockAspectRatio && info != null && info.height > 0) {
            (height * info.aspectRatio).toInt().coerceAtLeast(1)
        } else {
            current.customWidth
        }
        _uiState.value = _uiState.value.copy(
            resizeSettings = current.copy(customHeight = height, customWidth = newWidth)
        )
    }

    fun toggleLockAspectRatio() {
        val current = _uiState.value.resizeSettings
        _uiState.value = _uiState.value.copy(
            resizeSettings = current.copy(lockAspectRatio = !current.lockAspectRatio)
        )
    }

    fun resetDimensions() {
        val info = _uiState.value.imageInfo ?: return
        _uiState.value = _uiState.value.copy(
            resizeSettings = _uiState.value.resizeSettings.copy(
                customWidth = info.width,
                customHeight = info.height,
                percentage = 100,
                mode = ResizeMode.NONE
            )
        )
    }

    fun setSocialPreset(preset: SocialPreset) {
        _uiState.value = _uiState.value.copy(
            resizeSettings = _uiState.value.resizeSettings.copy(
                mode = ResizeMode.SOCIAL_PRESET,
                socialPreset = preset,
                customWidth = preset.width,
                customHeight = preset.height
            )
        )
    }

    // ── Processing ────────────────────────────────────────────────────────────
    /**
     * Builds effective [CompressionSettings] and [ResizeSettings] that respect the
     * op toggles, then delegates to [CompressImageUseCase].
     *
     * - If compression is disabled: uses QUALITY mode at 100% (lossless passthrough).
     * - If resize is disabled: forces ResizeMode.NONE.
     * - If format conversion is disabled: preserves the original format (detected from mimeType).
     */
    fun processCompression(onSuccess: (CompressionResult) -> Unit) {
        val state = _uiState.value
        val info = state.imageInfo ?: return

        viewModelScope.launch {
            _uiState.value = state.copy(isProcessing = true, errorMessage = null)

            // --- Build effective settings from toggles ---
            val effectiveCompression: CompressionSettings = when {
                state.compressEnabled -> state.compressionSettings.let { cs ->
                    // If convert is also disabled, keep original format
                    if (!state.convertEnabled) {
                        cs.copy(outputFormat = formatFromMime(info.mimeType))
                    } else {
                        cs
                    }
                }
                else -> {
                    // No compression: run at max quality; still respect format toggle
                    val outputFormat = if (state.convertEnabled)
                        state.compressionSettings.outputFormat
                    else
                        formatFromMime(info.mimeType)
                    CompressionSettings(
                        mode = CompressionMode.QUALITY,
                        quality = 100,
                        outputFormat = outputFormat
                    )
                }
            }

            // If resize is disabled, force NONE
            val effectiveResize: ResizeSettings = if (state.resizeEnabled) {
                state.resizeSettings
            } else {
                ResizeSettings(mode = ResizeMode.NONE)
            }

            val res = compressImageUseCase(
                imageInfo = info,
                compressionSettings = effectiveCompression,
                resizeSettings = effectiveResize
            )

            _uiState.value = _uiState.value.copy(isProcessing = false)

            res.onSuccess { result ->
                _uiState.value = _uiState.value.copy(result = result)
                onSuccess(result)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = err.localizedMessage ?: "Processing failed"
                )
            }
        }
    }

    private fun formatFromMime(mimeType: String): OutputFormat = when {
        mimeType.contains("png", ignoreCase = true) -> OutputFormat.PNG
        mimeType.contains("webp", ignoreCase = true) -> OutputFormat.WEBP
        else -> OutputFormat.JPEG
    }
}
