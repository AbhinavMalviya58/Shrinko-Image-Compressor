package com.cheatpot.shrinko.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cheatpot.shrinko.domain.model.HistoryItem
import com.cheatpot.shrinko.domain.repository.ImageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: ImageRepository
) : ViewModel() {

    val historyItems: StateFlow<List<HistoryItem>> = repository.getHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun removeItem(id: String) {
        viewModelScope.launch {
            repository.removeFromHistory(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
