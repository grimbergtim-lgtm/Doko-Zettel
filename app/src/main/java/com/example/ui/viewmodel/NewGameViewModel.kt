package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Player
import com.example.data.repository.DoppelkopfRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewGameUiState(
    val playerCount: Int = 4, // 4 or 5
    val title: String = "",
    val availablePlayers: List<Player> = emptyList(),
    val selectedPlayerIds: List<Long> = emptyList(),
    val bockRoundsPerTrigger: Int = 4,
    val errorMessage: String? = null,
    val isCreating: Boolean = false,
    val createdGameId: Long? = null
)

class NewGameViewModel(
    private val repository: DoppelkopfRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(NewGameUiState())

    val uiState: StateFlow<NewGameUiState> = combine(
        _formState,
        repository.getAllPlayers()
    ) { form, players ->
        form.copy(availablePlayers = players)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NewGameUiState()
    )

    fun setPlayerCount(count: Int) {
        if (count == 4 || count == 5) {
            _formState.update { current ->
                val adjustedSelection = if (count == 4 && current.selectedPlayerIds.size > 4) {
                    current.selectedPlayerIds.take(4)
                } else current.selectedPlayerIds

                current.copy(
                    playerCount = count,
                    bockRoundsPerTrigger = count,
                    selectedPlayerIds = adjustedSelection,
                    errorMessage = null
                )
            }
        }
    }

    fun setTitle(newTitle: String) {
        _formState.update { it.copy(title = newTitle) }
    }

    fun setBockRounds(rounds: Int) {
        _formState.update { it.copy(bockRoundsPerTrigger = rounds.coerceIn(1, 10)) }
    }

    fun togglePlayerSelection(playerId: Long) {
        _formState.update { current ->
            val list = current.selectedPlayerIds.toMutableList()
            if (list.contains(playerId)) {
                list.remove(playerId)
                current.copy(selectedPlayerIds = list, errorMessage = null)
            } else {
                if (list.size < current.playerCount) {
                    list.add(playerId)
                    current.copy(selectedPlayerIds = list, errorMessage = null)
                } else {
                    current.copy(errorMessage = "Es können maximal ${current.playerCount} Spieler ausgewählt werden.")
                }
            }
        }
    }

    fun createNewPlayer(name: String, colorHex: Long, avatarSymbol: String) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) {
                _formState.update { it.copy(errorMessage = "Bitte einen Spielernamen eingeben.") }
                return@launch
            }
            try {
                val newId = repository.insertPlayer(trimmed, colorHex, avatarSymbol)
                _formState.update { current ->
                    val list = current.selectedPlayerIds.toMutableList()
                    if (list.size < current.playerCount && !list.contains(newId)) {
                        list.add(newId)
                    }
                    current.copy(selectedPlayerIds = list, errorMessage = null)
                }
            } catch (e: Exception) {
                _formState.update { it.copy(errorMessage = e.message ?: "Fehler beim Anlegen des Spielers") }
            }
        }
    }

    fun createGame() {
        val current = _formState.value
        val selected = current.selectedPlayerIds
        val targetCount = current.playerCount

        if (selected.size != targetCount) {
            _formState.update { it.copy(errorMessage = "Bitte genau $targetCount Spieler auswählen (aktuell: ${selected.size}).") }
            return
        }

        if (selected.distinct().size != targetCount) {
            _formState.update { it.copy(errorMessage = "Ein Spieler darf nicht mehrfach teilnehmen.") }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isCreating = true, errorMessage = null) }
            try {
                val gameId = repository.createGame(
                    title = current.title,
                    playerIds = selected,
                    bockRoundsPerTrigger = current.bockRoundsPerTrigger
                )
                _formState.update { it.copy(createdGameId = gameId, isCreating = false) }
            } catch (e: Exception) {
                _formState.update { it.copy(errorMessage = e.message ?: "Fehler beim Erstellen der Partie", isCreating = false) }
            }
        }
    }

    fun clearError() {
        _formState.update { it.copy(errorMessage = null) }
    }
}
