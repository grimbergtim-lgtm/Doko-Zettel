package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Player
import com.example.data.model.PlayerStats
import com.example.data.repository.DoppelkopfRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayersUiState(
    val playerStatsList: List<PlayerStats> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class PlayersViewModel(
    private val repository: DoppelkopfRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<PlayersUiState> = combine(
        repository.getAllPlayers(),
        repository.getAllGames(),
        _refreshTrigger,
        _errorMessage
    ) { _, _, _, error ->
        val stats = repository.computeAllPlayerStatsSync()
        PlayersUiState(
            playerStatsList = stats,
            isLoading = false,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayersUiState(isLoading = true)
    )

    fun addPlayer(name: String, colorHex: Long, avatarSymbol: String) {
        viewModelScope.launch {
            try {
                repository.insertPlayer(name, colorHex, avatarSymbol)
                _refreshTrigger.value += 1
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Fehler beim Erstellen des Spielers"
            }
        }
    }

    fun updatePlayer(player: Player) {
        viewModelScope.launch {
            try {
                repository.updatePlayer(player)
                _refreshTrigger.value += 1
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Fehler beim Bearbeiten des Spielers"
            }
        }
    }

    fun deletePlayer(player: Player) {
        viewModelScope.launch {
            try {
                repository.deletePlayer(player)
                _refreshTrigger.value += 1
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Spieler kann nicht gelöscht werden, da er in bestehenden Partien verknüpft ist."
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
