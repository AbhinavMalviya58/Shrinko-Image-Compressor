package com.cheatpot.shrinko.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cheatpot.shrinko.domain.model.AppSettings
import com.cheatpot.shrinko.domain.model.HistoryItem
import com.cheatpot.shrinko.domain.repository.ImageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val repository: ImageRepository
) : ViewModel() {

    val historyState: StateFlow<List<HistoryItem>> = repository.getHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val appSettings: StateFlow<AppSettings> = repository.getAppSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )
}
