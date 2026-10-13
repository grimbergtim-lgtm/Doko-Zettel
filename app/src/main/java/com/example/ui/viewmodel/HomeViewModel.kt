package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Game
import com.example.data.model.Player
import com.example.data.repository.DoppelkopfRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val activeGames: List<Game> = emptyList(),
    val latestActiveGame: Game? = null,
    val completedGamesCount: Int = 0,
    val totalGamesCount: Int = 0,
    val totalPlayersCount: Int = 0,
    val recentPlayers: List<Player> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val repository: DoppelkopfRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getActiveGames(),
        repository.getAllGames(),
        repository.getAllPlayers()
    ) { activeGames, allGames, players ->
        HomeUiState(
            activeGames = activeGames,
            latestActiveGame = activeGames.firstOrNull(),
            completedGamesCount = allGames.count { it.status == Game.STATUS_COMPLETED },
            totalGamesCount = allGames.size,
            totalPlayersCount = players.size,
            recentPlayers = players.take(5),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun initializeSamplePlayersIfEmpty() {
        viewModelScope.launch {
            val players = repository.getAllPlayersSync()
            if (players.isEmpty()) {
                repository.insertPlayer("Anna", 0xFF2563EBL, "♠")
                repository.insertPlayer("Ben", 0xFFDC2626L, "♥")
                repository.insertPlayer("Clara", 0xFF16A34AL, "♦")
                repository.insertPlayer("David", 0xFFD97706L, "♣")
                repository.insertPlayer("Eva", 0xFF7C3AEDL, "👑")
            }
        }
    }
}
