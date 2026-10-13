package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Game
import com.example.data.repository.DoppelkopfRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter {
    ALL,
    IN_PROGRESS,
    COMPLETED,
    ARCHIVED
}

data class HistoryUiState(
    val games: List<Game> = emptyList(),
    val filter: HistoryFilter = HistoryFilter.ALL,
    val isLoading: Boolean = false
)

class HistoryViewModel(
    private val repository: DoppelkopfRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(HistoryFilter.ALL)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.getAllGames(),
        _filter
    ) { allGames, filter ->
        val filtered = when (filter) {
            HistoryFilter.ALL -> allGames
            HistoryFilter.IN_PROGRESS -> allGames.filter { it.status == Game.STATUS_IN_PROGRESS }
            HistoryFilter.COMPLETED -> allGames.filter { it.status == Game.STATUS_COMPLETED }
            HistoryFilter.ARCHIVED -> allGames.filter { it.status == Game.STATUS_ARCHIVED }
        }
        HistoryUiState(
            games = filtered,
            filter = filter,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState(isLoading = true)
    )

    fun setFilter(filter: HistoryFilter) {
        _filter.value = filter
    }

    fun finishGame(gameId: Long) {
        viewModelScope.launch { repository.finishGame(gameId) }
    }

    fun reopenGame(gameId: Long) {
        viewModelScope.launch { repository.reopenGame(gameId) }
    }

    fun archiveGame(gameId: Long) {
        viewModelScope.launch { repository.archiveGame(gameId) }
    }

    fun deleteGame(gameId: Long) {
        viewModelScope.launch { repository.deleteGame(gameId) }
    }
}
