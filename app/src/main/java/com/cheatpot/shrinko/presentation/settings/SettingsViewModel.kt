package com.cheatpot.shrinko.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cheatpot.shrinko.domain.model.AppSettings
import com.cheatpot.shrinko.domain.model.CompressionMode
import com.cheatpot.shrinko.domain.model.OutputFormat
import com.cheatpot.shrinko.domain.model.ResizeMode
import com.cheatpot.shrinko.domain.model.ThemeMode
import com.cheatpot.shrinko.domain.repository.ImageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: ImageRepository
) : ViewModel() {

    val appSettings: StateFlow<AppSettings> = repository.getAppSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun setThemeMode(mode: ThemeMode) {
        val current = appSettings.value
        viewModelScope.launch {
            repository.updateAppSettings(current.copy(themeMode = mode))
        }
    }

    fun setDefaultFormat(format: OutputFormat) {
        val current = appSettings.value
        viewModelScope.launch {
            repository.updateAppSettings(current.copy(defaultOutputFormat = format))
        }
    }

    fun setDefaultCompressionMode(mode: CompressionMode) {
        val current = appSettings.value
        viewModelScope.launch {
            repository.updateAppSettings(current.copy(defaultCompressionMode = mode))
        }
    }

    fun setDefaultQuality(quality: Int) {
        val current = appSettings.value
        viewModelScope.launch {
            repository.updateAppSettings(current.copy(defaultQuality = quality.coerceIn(1, 100)))
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
