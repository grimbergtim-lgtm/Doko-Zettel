package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AidaAlert
import com.example.data.model.AidaExtremeMode
import com.example.data.model.BockCalculator
import com.example.data.model.FullGameData
import com.example.data.model.Game
import com.example.data.model.GameRound
import com.example.data.model.GameType
import com.example.data.model.Player
import com.example.data.repository.DoppelkopfRepository
import com.example.data.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class EntryMode {
    TEAM,   // Re vs. Kontra bzw. Solo vs. Gegenseite mit automatischer Punktverteilung
    MANUAL  // Direkte manuelle Eingabe jedes Spielers
}

enum class WinningParty {
    RE,
    KONTRA
}

data class PlayerScorePreview(
    val player: Player,
    val rawGamePoints: Int,
    val gamePoints: Int,
    val weddingPoints: Int,
    val finalPoints: Int,
    val isSittingOut: Boolean
)

data class GameUiState(
    val gameData: FullGameData? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isEntrySheetOpen: Boolean = false,
    val isSummaryDialogOpen: Boolean = false,
    val isBockAdjustDialogOpen: Boolean = false,
    val isSubmitting: Boolean = false,
    val editingRoundId: Long? = null,

    // Rundeneingabe-Zustand
    val entryMode: EntryMode = EntryMode.TEAM,
    val winningParty: WinningParty = WinningParty.RE,
    val basePoints: Int = 1,
    val rePlayerIds: Set<Long> = emptySet(),
    val manualRawPoints: Map<Long, Int> = emptyMap(),
    val sittingOutPlayerId: Long? = null,
    val hochzeitPlayerId: Long? = null,
    val triggerNewBock: Boolean = false,
    val bockTriggersCount: Int = 0,
    val note: String = "",

    // Solo-Spiel
    val gameType: GameType = GameType.NORMAL,
    val soloPlayerId: Long? = null,

    // Manuelle Bock-Korrektur
    val bockAdjustLevel: Int = 1,
    val bockAdjustRemaining: Int = 0,

    // AIDA Extreme Mode
    val aidaAlert: AidaAlert? = null,
    val isAidaModeActive: Boolean = false
)

class GameViewModel(
    val gameId: Long,
    private val repository: DoppelkopfRepository,
    private val settingsRepository: SettingsRepository? = null,
    private val aidaMinIntervalMs: Long = AidaExtremeMode.MIN_INTERVAL_MS,
    private val aidaMaxIntervalMs: Long = AidaExtremeMode.MAX_INTERVAL_MS
) : ViewModel() {

    private val _gameData = repository.getFullGameData(gameId)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isEntrySheetOpen = MutableStateFlow(false)
    private val _isSummaryDialogOpen = MutableStateFlow(false)
    private val _isBockAdjustDialogOpen = MutableStateFlow(false)
    private val _isSubmitting = MutableStateFlow(false)
    private val _editingRoundId = MutableStateFlow<Long?>(null)

    private val _entryMode = MutableStateFlow(EntryMode.TEAM)
    private val _winningParty = MutableStateFlow(WinningParty.RE)
    private val _basePoints = MutableStateFlow(1)
    private val _rePlayerIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _manualRawPoints = MutableStateFlow<Map<Long, Int>>(emptyMap())
    private val _sittingOutPlayerId = MutableStateFlow<Long?>(null)
    private val _hochzeitPlayerId = MutableStateFlow<Long?>(null)
    private val _triggerNewBock = MutableStateFlow(false)
    private val _bockTriggersCount = MutableStateFlow(0)
    private val _note = MutableStateFlow("")

    private val _gameType = MutableStateFlow(GameType.NORMAL)
    private val _soloPlayerId = MutableStateFlow<Long?>(null)

    private val _bockAdjustLevel = MutableStateFlow(1)
    private val _bockAdjustRemaining = MutableStateFlow(0)

    // AIDA Extreme Mode State & Job
    private val _aidaAlert = MutableStateFlow<AidaAlert?>(null)
    private val _isAidaModeActive = MutableStateFlow(false)
    private var aidaJob: Job? = null

    init {
        if (settingsRepository != null) {
            viewModelScope.launch {
                settingsRepository.aidaExtremeModeEnabled.collect { enabled ->
                    _isAidaModeActive.value = enabled
                    if (!enabled) {
                        aidaJob?.cancel()
                        aidaJob = null
                        _aidaAlert.value = null
                    } else {
                        checkAndStartAidaTimer()
                    }
                }
            }

            viewModelScope.launch {
                _gameData.collect { data ->
                    if (data != null && !data.game.isInProgress) {
                        aidaJob?.cancel()
                        aidaJob = null
                        _aidaAlert.value = null
                    } else if (data != null && data.game.isInProgress && _isAidaModeActive.value) {
                        checkAndStartAidaTimer()
                    }
                }
            }
        }
    }

    val uiState: StateFlow<GameUiState> = combine(
        _gameData,
        _errorMessage,
        _isEntrySheetOpen,
        _isSummaryDialogOpen,
        _isBockAdjustDialogOpen,
        _isSubmitting,
        _editingRoundId,
        _entryMode,
        _winningParty,
        _basePoints,
        _rePlayerIds,
        _manualRawPoints,
        _sittingOutPlayerId,
        _hochzeitPlayerId,
        _triggerNewBock,
        _bockTriggersCount,
        _note,
        _bockAdjustLevel,
        _bockAdjustRemaining,
        _gameType,
        _soloPlayerId,
        _aidaAlert,
        _isAidaModeActive
    ) { args: Array<Any?> ->
        val data = args[0] as FullGameData?
        @Suppress("UNCHECKED_CAST")
        GameUiState(
            gameData = data,
            isLoading = false,
            errorMessage = args[1] as String?,
            isEntrySheetOpen = args[2] as Boolean,
            isSummaryDialogOpen = args[3] as Boolean,
            isBockAdjustDialogOpen = args[4] as Boolean,
            isSubmitting = args[5] as Boolean,
            editingRoundId = args[6] as Long?,
            entryMode = args[7] as EntryMode,
            winningParty = args[8] as WinningParty,
            basePoints = args[9] as Int,
            rePlayerIds = args[10] as Set<Long>,
            manualRawPoints = args[11] as Map<Long, Int>,
            sittingOutPlayerId = args[12] as Long?,
            hochzeitPlayerId = args[13] as Long?,
            triggerNewBock = args[14] as Boolean,
            bockTriggersCount = args[15] as Int,
            note = args[16] as String,
            bockAdjustLevel = args[17] as Int,
            bockAdjustRemaining = args[18] as Int,
            gameType = args[19] as GameType,
            soloPlayerId = args[20] as Long?,
            aidaAlert = args[21] as AidaAlert?,
            isAidaModeActive = args[22] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GameUiState(isLoading = true)
    )

    fun openNewRoundEntry() {
        val current = uiState.value.gameData ?: return
        val players = current.players

        // Bei 5 Spielern: Geber setzt automatisch aus
        val defaultSitting = if (current.game.playerCount == 5) {
            current.defaultSittingOutPlayer?.id
        } else null

        // Standardmäßig die ersten beiden aktiven Spieler als Re-Team vorschlagen
        val activePlayers = players.filter { it.id != defaultSitting }
        val defaultRe = activePlayers.take(2).map { it.id }.toSet()

        val initialManualPoints = players.associate { player ->
            player.id to (if (player.id == defaultSitting) 0 else if (defaultRe.contains(player.id)) 1 else -1)
        }

        _editingRoundId.value = null
        _gameType.value = GameType.NORMAL
        _soloPlayerId.value = null
        _entryMode.value = EntryMode.TEAM
        _winningParty.value = WinningParty.RE
        _basePoints.value = 1
        _rePlayerIds.value = defaultRe
        _manualRawPoints.value = initialManualPoints
        _sittingOutPlayerId.value = defaultSitting
        _hochzeitPlayerId.value = null
        _triggerNewBock.value = false
        _bockTriggersCount.value = 0
        _note.value = ""
        _errorMessage.value = null
        _isSummaryDialogOpen.value = false
        _isEntrySheetOpen.value = true
    }

    fun openEditRound(roundWithScores: com.example.data.model.RoundWithScores) {
        val round = roundWithScores.round
        val current = uiState.value.gameData ?: return

        val parsedType = try {
            GameType.valueOf(round.gameType)
        } catch (e: Exception) {
            GameType.NORMAL
        }
        _gameType.value = parsedType
        _soloPlayerId.value = round.soloPlayerId

        _editingRoundId.value = round.id
        _entryMode.value = EntryMode.MANUAL // Bei Bearbeitung genaue Werte laden
        _basePoints.value = round.basePoints
        _sittingOutPlayerId.value = round.sittingOutPlayerId
        _hochzeitPlayerId.value = round.hochzeitPlayerId
        _triggerNewBock.value = round.bockTriggersCount > 0
        _bockTriggersCount.value = round.bockTriggersCount
        _note.value = round.note

        val pointMap = mutableMapOf<Long, Int>()
        current.players.forEach { p ->
            val score = roundWithScores.scores.find { it.playerId == p.id }
            pointMap[p.id] = score?.rawPoints ?: 0
        }
        _manualRawPoints.value = pointMap
        _errorMessage.value = null
        _isSummaryDialogOpen.value = false
        _isEntrySheetOpen.value = true
    }

    fun closeEntrySheet() {
        _isEntrySheetOpen.value = false
        _isSummaryDialogOpen.value = false
    }

    fun setGameType(type: GameType) {
        _gameType.value = type
        if (type == GameType.SOLO) {
            _hochzeitPlayerId.value = null // keine Hochzeit im Solo
            val current = uiState.value.gameData ?: return
            val sitting = _sittingOutPlayerId.value
            val active = current.players.filter { it.id != sitting }
            if (_soloPlayerId.value == null || !active.any { it.id == _soloPlayerId.value }) {
                _soloPlayerId.value = active.firstOrNull()?.id
            }
            syncManualPointsFromTeam()
        } else {
            _soloPlayerId.value = null
            syncManualPointsFromTeam()
        }
    }

    fun setSoloPlayer(playerId: Long) {
        _soloPlayerId.value = playerId
        syncManualPointsFromTeam()
    }

    fun setEntryMode(mode: EntryMode) {
        _entryMode.value = mode
    }

    fun setWinningParty(party: WinningParty) {
        _winningParty.value = party
        syncManualPointsFromTeam()
    }

    fun setBasePoints(points: Int) {
        _basePoints.value = points.coerceIn(0, 25)
        syncManualPointsFromTeam()
    }

    fun openQuickWeddingEntry(playerId: Long) {
        val current = uiState.value.gameData ?: return
        _editingRoundId.value = null
        _gameType.value = GameType.NORMAL
        _soloPlayerId.value = null
        _entryMode.value = EntryMode.TEAM
        _winningParty.value = WinningParty.RE
        _basePoints.value = 0 // 0 Spielpunkte, reine Hochzeitswertung
        _rePlayerIds.value = emptySet()
        _manualRawPoints.value = current.players.associate { it.id to 0 }
        _sittingOutPlayerId.value = null
        _hochzeitPlayerId.value = playerId
        _triggerNewBock.value = false
        _bockTriggersCount.value = 0
        _note.value = "Hochzeitsanmeldung"
        _errorMessage.value = null
        _isSummaryDialogOpen.value = false
        _isEntrySheetOpen.value = true
    }

    fun toggleRePlayer(playerId: Long) {
        val currentRe = _rePlayerIds.value.toMutableSet()
        if (currentRe.contains(playerId)) {
            currentRe.remove(playerId)
        } else {
            // Maximal 2 Re-Spieler bei Normalspiel
            if (currentRe.size < 2) {
                currentRe.add(playerId)
            } else {
                // Ersetze ältesten
                currentRe.remove(currentRe.first())
                currentRe.add(playerId)
            }
        }
        _rePlayerIds.value = currentRe
        syncManualPointsFromTeam()
    }

    fun setSittingOutPlayer(playerId: Long?) {
        _sittingOutPlayerId.value = playerId
        // Wenn dieser Spieler im Re-Team war, entfernen
        if (playerId != null && _rePlayerIds.value.contains(playerId)) {
            _rePlayerIds.value = _rePlayerIds.value - playerId
        }
        // Wenn dieser Spieler als Solospieler ausgewählt war, einen anderen aktiven Spieler wählen
        if (playerId != null && _soloPlayerId.value == playerId) {
            val current = uiState.value.gameData
            val otherActive = current?.players?.filter { it.id != playerId }?.firstOrNull()
            _soloPlayerId.value = otherActive?.id
        }
        syncManualPointsFromTeam()
    }

    fun setHochzeitPlayer(playerId: Long?) {
        // Toggle: wenn bereits ausgewählt, abwählen
        _hochzeitPlayerId.value = if (_hochzeitPlayerId.value == playerId) null else playerId
    }

    fun setTriggerNewBock(trigger: Boolean) {
        _triggerNewBock.value = trigger
        if (!trigger) {
            _bockTriggersCount.value = 0
            _errorMessage.value = null
        } else if (_bockTriggersCount.value == 0) {
            setBockTriggersCount(1)
        }
    }

    fun setBockTriggersCount(count: Int) {
        val safeCount = count.coerceIn(0, 8)
        _bockTriggersCount.value = safeCount
        _triggerNewBock.value = safeCount > 0

        val current = uiState.value.gameData ?: return
        val currentBock = if (_editingRoundId.value != null) {
            current.roundsWithScores.find { it.round.id == _editingRoundId.value }?.round?.bockLevel ?: current.game.currentBockLevel
        } else {
            current.game.currentBockLevel
        }

        if (currentBock + safeCount > BockCalculator.MAX_LEVEL) {
            _errorMessage.value = "Maximaler Bock (128x) würde überschritten werden. Maximal ${BockCalculator.maxAllowedTriggers(currentBock)} zusätzliche(r) Auslöser erlaubt."
        } else {
            if (_errorMessage.value?.contains("Maximaler Bock") == true) {
                _errorMessage.value = null
            }
        }
    }

    fun setNote(newNote: String) {
        _note.value = newNote
    }

    fun setManualPoint(playerId: Long, points: Int) {
        val map = _manualRawPoints.value.toMutableMap()
        map[playerId] = points
        _manualRawPoints.value = map
    }

    private fun syncManualPointsFromTeam() {
        val current = uiState.value.gameData ?: return
        val sitting = _sittingOutPlayerId.value
        val pts = _basePoints.value
        val party = _winningParty.value

        val map = mutableMapOf<Long, Int>()
        if (_gameType.value == GameType.SOLO) {
            val soloId = _soloPlayerId.value
            val activeOpponents = current.players.filter { it.id != sitting && it.id != soloId }
            val opponentsCount = activeOpponents.size
            val soloWon = party == WinningParty.RE

            current.players.forEach { p ->
                when (p.id) {
                    sitting -> map[p.id] = 0
                    soloId -> map[p.id] = if (soloWon) pts * opponentsCount else -pts * opponentsCount
                    else -> map[p.id] = if (soloWon) -pts else pts
                }
            }
        } else {
            val re = _rePlayerIds.value
            current.players.forEach { p ->
                if (p.id == sitting) {
                    map[p.id] = 0
                } else if (re.contains(p.id)) {
                    map[p.id] = if (party == WinningParty.RE) pts else -pts
                } else {
                    map[p.id] = if (party == WinningParty.RE) -pts else pts
                }
            }
        }
        _manualRawPoints.value = map
    }

    fun calculateCurrentRawPoints(): Map<Long, Int> {
        return if (_entryMode.value == EntryMode.TEAM) {
            val current = uiState.value.gameData ?: return emptyMap()
            val sitting = _sittingOutPlayerId.value
            val pts = _basePoints.value
            val party = _winningParty.value

            if (_gameType.value == GameType.SOLO) {
                val soloId = _soloPlayerId.value
                val activeOpponents = current.players.filter { it.id != sitting && it.id != soloId }
                val opponentsCount = activeOpponents.size
                val soloWon = party == WinningParty.RE

                current.players.associate { p ->
                    p.id to when (p.id) {
                        sitting -> 0
                        soloId -> if (soloWon) pts * opponentsCount else -pts * opponentsCount
                        else -> if (soloWon) -pts else pts
                    }
                }
            } else {
                val re = _rePlayerIds.value
                current.players.associate { p ->
                    p.id to when {
                        p.id == sitting -> 0
                        re.contains(p.id) -> if (party == WinningParty.RE) pts else -pts
                        else -> if (party == WinningParty.RE) -pts else pts
                    }
                }
            }
        } else {
            _manualRawPoints.value
        }
    }

    fun calculateScorePreviews(): List<PlayerScorePreview> {
        val current = uiState.value.gameData ?: return emptyList()
        val rawMap = calculateCurrentRawPoints()
        val totalCount = current.players.size
        val currentBock = if (uiState.value.editingRoundId != null) {
            current.roundsWithScores.find { it.round.id == uiState.value.editingRoundId }?.round?.bockLevel ?: current.game.currentBockLevel
        } else {
            current.game.currentBockLevel
        }
        val multiplier = BockCalculator.getMultiplier(currentBock)
        val hochzeitId = uiState.value.hochzeitPlayerId
        val sittingId = uiState.value.sittingOutPlayerId

        val weddingGain = (totalCount - 1) * multiplier
        val weddingLoss = -1 * multiplier

        return current.players.map { p ->
            val isSitting = p.id == sittingId
            val raw = if (isSitting) 0 else (rawMap[p.id] ?: 0)
            val gamePts = if (isSitting) 0 else raw * multiplier
            val weddingPts = if (hochzeitId != null) {
                if (p.id == hochzeitId) weddingGain else weddingLoss
            } else 0
            val finalPts = gamePts + weddingPts
            PlayerScorePreview(
                player = p,
                rawGamePoints = raw,
                gamePoints = gamePts,
                weddingPoints = weddingPts,
                finalPoints = finalPts,
                isSittingOut = isSitting
            )
        }
    }

    /**
     * Öffnet die Zusammenfassung vor dem endgültigen Speichern.
     */
    fun openSummaryDialog() {
        val current = uiState.value.gameData ?: return

        // Validierung: Bei 5 Spielern muss genau 1 Spieler aussetzen (sofern Spielpunkte vergeben werden)
        if (current.game.playerCount == 5 && _basePoints.value > 0 && _sittingOutPlayerId.value == null) {
            _errorMessage.value = "Bei 5 Spielern muss ein aussetzender Spieler ausgewählt werden."
            return
        }

        // Bei normalem Spiel mit 4 Spielern im Team-Modus prüfen, ob 2 Re-Spieler gewählt wurden
        if (_gameType.value == GameType.NORMAL && _entryMode.value == EntryMode.TEAM && _basePoints.value > 0 && _rePlayerIds.value.size != 2 && current.game.playerCount == 4) {
            _errorMessage.value = "Bitte 2 Spieler für das Re-Team auswählen."
            return
        }

        // Bei Solospiel prüfen, ob ein Solospieler ausgewählt wurde
        if (_gameType.value == GameType.SOLO && _soloPlayerId.value == null) {
            _errorMessage.value = "Bitte genau einen Solospieler auswählen."
            return
        }

        // Validierung: Maximale Bockstufe 8 darf nicht überschritten werden
        val currentBock = if (_editingRoundId.value != null) {
            current.roundsWithScores.find { it.round.id == _editingRoundId.value }?.round?.bockLevel ?: current.game.currentBockLevel
        } else {
            current.game.currentBockLevel
        }
        val triggers = _bockTriggersCount.value
        if (currentBock + triggers > BockCalculator.MAX_LEVEL) {
            _errorMessage.value = "Maximaler Bock (128x) würde überschritten werden. Maximal ${BockCalculator.maxAllowedTriggers(currentBock)} zusätzliche(r) Auslöser erlaubt."
            return
        }

        _errorMessage.value = null
        _isSummaryDialogOpen.value = true
    }

    fun dismissSummaryDialog() {
        _isSummaryDialogOpen.value = false
    }

    /**
     * Speichert die Runde endgültig ab mit Doppelbuchungsschutz (isSubmitting).
     */
    fun commitRound() {
        val current = uiState.value.gameData ?: return
        if (_isSubmitting.value) return // Schutz vor doppelter Buchung

        val rawPoints = calculateCurrentRawPoints()
        val editingId = _editingRoundId.value
        val effectiveType = _gameType.value
        val effectiveSoloPlayerId = if (effectiveType == GameType.SOLO) _soloPlayerId.value else null
        val effectiveHochzeitPlayerId = if (effectiveType == GameType.SOLO) null else _hochzeitPlayerId.value

        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null
            try {
                if (editingId == null) {
                    val result = repository.addRound(
                        gameId = gameId,
                        basePoints = _basePoints.value,
                        rawPlayerPoints = rawPoints,
                        hochzeitPlayerId = effectiveHochzeitPlayerId,
                        sittingOutPlayerId = _sittingOutPlayerId.value,
                        note = _note.value,
                        triggerNewBock = _bockTriggersCount.value > 0,
                        bockTriggersCount = _bockTriggersCount.value,
                        gameType = effectiveType.name,
                        soloPlayerId = effectiveSoloPlayerId
                    )
                    if (result.isFailure) {
                        _errorMessage.value = result.exceptionOrNull()?.message ?: "Fehler beim Speichern der Runde"
                    } else {
                        _isSummaryDialogOpen.value = false
                        _isEntrySheetOpen.value = false
                    }
                } else {
                    val existingRound = current.roundsWithScores.find { it.round.id == editingId }?.round
                    val bockLevel = existingRound?.bockLevel ?: current.game.currentBockLevel
                    val result = repository.updateRound(
                        roundId = editingId,
                        gameId = gameId,
                        basePoints = _basePoints.value,
                        rawPlayerPoints = rawPoints,
                        bockLevel = bockLevel,
                        hochzeitPlayerId = effectiveHochzeitPlayerId,
                        sittingOutPlayerId = _sittingOutPlayerId.value,
                        note = _note.value,
                        bockTriggersCount = _bockTriggersCount.value,
                        gameType = effectiveType.name,
                        soloPlayerId = effectiveSoloPlayerId
                    )
                    if (result.isFailure) {
                        _errorMessage.value = result.exceptionOrNull()?.message ?: "Fehler beim Aktualisieren der Runde"
                    } else {
                        _isSummaryDialogOpen.value = false
                        _isEntrySheetOpen.value = false
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unerwarteter Fehler beim Buchen der Runde"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun deleteRound(roundId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteRound(roundId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Fehler beim Löschen der Runde"
            }
        }
    }

    // --- Bockstufen-Verwaltung ---
    fun openBockAdjustDialog() {
        val current = uiState.value.gameData ?: return
        _bockAdjustLevel.value = current.game.currentBockLevel
        _bockAdjustRemaining.value = current.game.remainingBockRounds
        _isBockAdjustDialogOpen.value = true
    }

    fun closeBockAdjustDialog() {
        _isBockAdjustDialogOpen.value = false
    }

    fun setAdjustBockLevel(level: Int) {
        _bockAdjustLevel.value = BockCalculator.coerceLevel(level)
    }

    fun setAdjustBockRemaining(remaining: Int) {
        _bockAdjustRemaining.value = remaining.coerceAtLeast(0)
    }

    fun applyBockAdjustment() {
        viewModelScope.launch {
            repository.setGameBockState(
                gameId = gameId,
                level = _bockAdjustLevel.value,
                remainingRounds = _bockAdjustRemaining.value
            )
            _isBockAdjustDialogOpen.value = false
        }
    }

    fun finishGame() {
        aidaJob?.cancel()
        aidaJob = null
        _aidaAlert.value = null
        viewModelScope.launch {
            repository.finishGame(gameId)
        }
    }

    fun reopenGame() {
        viewModelScope.launch {
            repository.reopenGame(gameId)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // AIDA Extreme Mode Methoden
    fun checkAndStartAidaTimer() {
        if (aidaJob?.isActive == true) return
        if (!_isAidaModeActive.value) return
        val currentData = uiState.value.gameData ?: return
        if (!currentData.game.isInProgress) return

        aidaJob = viewModelScope.launch {
            while (isActive && _isAidaModeActive.value) {
                val delayMs = AidaExtremeMode.calculateNextIntervalMs(aidaMinIntervalMs, aidaMaxIntervalMs)
                delay(delayMs)

                if (!isActive || !_isAidaModeActive.value) break

                val latestData = uiState.value.gameData
                if (latestData == null || !latestData.game.isInProgress) break

                val players = latestData.players
                if (players.isEmpty()) continue

                val customTasks = settingsRepository?.getAidaCustomTasksSync() ?: emptyList()
                val task = AidaExtremeMode.getRandomTask(customTasks)
                val player = AidaExtremeMode.getRandomPlayer(players)

                if (task != null && player != null) {
                    _aidaAlert.value = AidaAlert(player = player, task = task)

                    // Warte bis die Meldung vom Spieler geschlossen wurde, bevor das nächste Intervall startet
                    while (isActive && _aidaAlert.value != null && _isAidaModeActive.value) {
                        delay(500L)
                    }
                }
            }
        }
    }

    fun dismissAidaAlert() {
        _aidaAlert.value = null
    }

    fun triggerAidaAlertNow() {
        val currentData = uiState.value.gameData ?: return
        if (currentData.players.isEmpty()) return
        viewModelScope.launch {
            val customTasks = settingsRepository?.getAidaCustomTasksSync() ?: emptyList()
            val task = AidaExtremeMode.getRandomTask(customTasks)
            val player = AidaExtremeMode.getRandomPlayer(currentData.players)
            if (task != null && player != null) {
                _aidaAlert.value = AidaAlert(player = player, task = task)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        aidaJob?.cancel()
        aidaJob = null
    }
}
