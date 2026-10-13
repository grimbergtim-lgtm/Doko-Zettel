package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.local.AppDatabase
import com.example.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val defaultBockRounds: Int = 4,
    val exportJsonString: String? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false
)

class SettingsViewModel(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _internalState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = combine(
        _internalState,
        settingsRepository.defaultBockRounds
    ) { current, bockRounds ->
        current.copy(defaultBockRounds = bockRounds)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setDefaultBockRounds(rounds: Int) {
        viewModelScope.launch {
            settingsRepository.setDefaultBockRounds(rounds)
            _internalState.update { it.copy(infoMessage = "Standard-Bockrunden auf $rounds aktualisiert.") }
        }
    }

    fun exportDatabase() {
        viewModelScope.launch {
            _internalState.update { it.copy(isExporting = true, errorMessage = null) }
            try {
                val json = BackupManager.exportToJson(database)
                _internalState.update {
                    it.copy(
                        exportJsonString = json,
                        infoMessage = "Daten erfolgreich im JSON-Format exportiert!",
                        isExporting = false
                    )
                }
            } catch (e: Exception) {
                _internalState.update {
                    it.copy(
                        errorMessage = "Export fehlgeschlagen: ${e.message}",
                        isExporting = false
                    )
                }
            }
        }
    }

    fun importDatabase(jsonString: String) {
        viewModelScope.launch {
            _internalState.update { it.copy(isImporting = true, errorMessage = null) }
            try {
                val result = BackupManager.importFromJson(database, jsonString)
                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    _internalState.update {
                        it.copy(
                            infoMessage = "Import erfolgreich! $count Partien wiederhergestellt.",
                            isImporting = false
                        )
                    }
                } else {
                    _internalState.update {
                        it.copy(
                            errorMessage = "Import fehlgeschlagen: ${result.exceptionOrNull()?.message}",
                            isImporting = false
                        )
                    }
                }
            } catch (e: Exception) {
                _internalState.update {
                    it.copy(
                        errorMessage = "Import fehlgeschlagen: ${e.message}",
                        isImporting = false
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _internalState.update { it.copy(infoMessage = null, errorMessage = null) }
    }

    fun clearExportString() {
        _internalState.update { it.copy(exportJsonString = null) }
    }
}
