package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.model.BockCalculator
import com.example.data.model.FullGameData
import com.example.data.model.Game
import com.example.data.model.GamePlayerCrossRef
import com.example.data.model.GameRound
import com.example.data.model.GameType
import com.example.data.model.Player
import com.example.data.model.PlayerStats
import com.example.data.model.RoundScore
import com.example.data.model.RoundWithScores
import com.example.data.model.WeddingEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DoppelkopfRepository(
    private val database: AppDatabase
) {
    private val playerDao = database.playerDao()
    private val gameDao = database.gameDao()
    private val roundDao = database.roundDao()
    private val weddingDao = database.weddingDao()

    // --- Spieler ---
    fun getAllPlayers(): Flow<List<Player>> = playerDao.getAllPlayers()

    suspend fun getAllPlayersSync(): List<Player> = playerDao.getAllPlayersSync()

    fun getPlayerById(id: Long): Flow<Player?> = playerDao.getPlayerById(id)

    suspend fun getPlayerByIdSync(id: Long): Player? = playerDao.getPlayerByIdSync(id)

    suspend fun insertPlayer(
        name: String,
        colorHex: Long = Player.DEFAULT_COLOR_HEX,
        avatarSymbol: String = "♠"
    ): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Spielername darf nicht leer sein" }
        val player = Player(
            name = trimmed,
            colorHex = Player.sanitizeColorHex(colorHex),
            avatarSymbol = avatarSymbol
        )
        return playerDao.insertPlayer(player)
    }

    suspend fun updatePlayer(player: Player) {
        val trimmed = player.name.trim()
        require(trimmed.isNotEmpty()) { "Spielername darf nicht leer sein" }
        playerDao.updatePlayer(
            player.copy(
                name = trimmed,
                colorHex = Player.sanitizeColorHex(player.colorHex)
            )
        )
    }

    suspend fun deletePlayer(player: Player) {
        playerDao.deletePlayer(player)
    }

    // --- Spiele ---
    fun getAllGames(): Flow<List<Game>> = gameDao.getAllGames()

    fun getActiveGames(): Flow<List<Game>> = gameDao.getActiveGames()

    fun getLatestActiveGame(): Flow<Game?> = gameDao.getLatestActiveGame()

    fun getGameById(gameId: Long): Flow<Game?> = gameDao.getGameById(gameId)

    suspend fun getGameByIdSync(gameId: Long): Game? = gameDao.getGameByIdSync(gameId)

    /**
     * Erstellt eine neue Partie mit 4 oder 5 Spielern.
     * Prüft auf Duplikate und Teilnehmeranzahl.
     */
    suspend fun createGame(
        title: String,
        playerIds: List<Long>,
        bockRoundsPerTrigger: Int = 4
    ): Long {
        require(playerIds.size == 4 || playerIds.size == 5) {
            "Eine Doppelkopf-Partie muss genau 4 oder 5 Spieler haben"
        }
        val uniqueIds = playerIds.distinct()
        require(uniqueIds.size == playerIds.size) {
            "Ein Spieler darf nicht mehrfach in derselben Partie ausgewählt werden"
        }

        val defaultRounds = if (playerIds.size == 5 && bockRoundsPerTrigger == 4) 5 else bockRoundsPerTrigger

        return database.withTransaction {
            val game = Game(
                title = title.trim().ifEmpty { "Spielrunde #${System.currentTimeMillis() % 10000}" },
                playerCount = playerIds.size,
                currentBockLevel = 1,
                remainingBockRounds = 0,
                bockRoundsPerTrigger = defaultRounds.coerceAtLeast(1),
                activeBockSeries = ""
            )
            val gameId = gameDao.insertGame(game)
            val crossRefs = playerIds.mapIndexed { index, playerId ->
                GamePlayerCrossRef(
                    gameId = gameId,
                    playerId = playerId,
                    seatOrder = index
                )
            }
            gameDao.insertGamePlayers(crossRefs)
            gameId
        }
    }

    fun getFullGameData(gameId: Long): Flow<FullGameData?> {
        val gameFlow = gameDao.getGameById(gameId)
        val playersFlow = gameDao.getPlayersForGame(gameId)
        val roundsFlow = roundDao.getRoundWithScoresForGame(gameId)

        return combine(gameFlow, playersFlow, roundsFlow) { game, players, rounds ->
            if (game == null) null
            else FullGameData(
                game = game,
                players = players,
                roundsWithScores = rounds
            )
        }.distinctUntilChanged()
    }

    suspend fun getFullGameDataSync(gameId: Long): FullGameData? {
        val game = gameDao.getGameByIdSync(gameId) ?: return null
        val players = gameDao.getPlayersForGameSync(gameId)
        val rounds = roundDao.getRoundWithScoresForGameSync(gameId)
        return FullGameData(game, players, rounds)
    }

    /**
     * Erfasst eine neue Runde atomar mit Schutz vor Doppelbuchung.
     * Hochzeitswertung:
     * Der Hochzeitsspieler erhält von jedem anderen teilnehmenden Spieler (Anzahl - 1) genau 1 Punkt,
     * multipliziert mit dem aktuellen Bockmultiplikator.
     * Jeder andere teilnehmende Spieler verliert 1 Punkt * Bockmultiplikator.
     */
    /**
     * Erfasst eine neue Runde atomar mit Schutz vor Doppelbuchung und Unterstützung
     * für bis zu 8 zusätzliche Bockauslöser.
     *
     * Hochzeitswertung:
     * Der Hochzeitsspieler erhält von jedem anderen teilnehmenden Spieler (Anzahl - 1) genau 1 Punkt,
     * multipliziert mit dem aktuellen Bockmultiplikator.
     * Jeder andere teilnehmende Spieler verliert 1 Punkt * Bockmultiplikator.
     */
    suspend fun addRound(
        gameId: Long,
        basePoints: Int,
        rawPlayerPoints: Map<Long, Int>,
        hochzeitPlayerId: Long? = null,
        sittingOutPlayerId: Long? = null,
        note: String = "",
        triggerNewBock: Boolean = false,
        bockTriggersCount: Int = if (triggerNewBock) 1 else 0,
        gameType: String = GameType.NORMAL.name,
        soloPlayerId: Long? = null
    ): Result<Long> {
        return runCatching {
            database.withTransaction {
                val game = gameDao.getGameByIdSync(gameId)
                    ?: throw IllegalStateException("Partie $gameId nicht gefunden")

                val duration = BockCalculator.getDurationForPlayerCount(game.playerCount, game.bockRoundsPerTrigger)
                val triggers = bockTriggersCount.coerceIn(0, 8)

                // Aktive Bockfolgen vor dieser Runde ermitteln
                val currentSeries = if (game.activeBockSeries.isNotBlank()) {
                    BockCalculator.parseActiveSeries(game.activeBockSeries)
                } else if (game.initialRemainingBockRounds > 0 && game.initialBockLevel > 1) {
                    val count = (game.initialBockLevel - 1).coerceIn(1, 7)
                    List(count) { game.initialRemainingBockRounds }
                } else {
                    emptyList()
                }

                val currentBockLevel = BockCalculator.calculateLevelFromActiveSeriesCount(currentSeries.size)
                val multiplier = BockCalculator.getMultiplier(currentBockLevel)

                // Prüfung: Überschreitung der maximalen Bockstufe 8 verhindern
                if (currentBockLevel + triggers > BockCalculator.MAX_LEVEL) {
                    throw IllegalArgumentException(
                        "Maximale Bockstufe 8 (128x) würde überschritten werden: Stufe $currentBockLevel + $triggers = Stufe ${currentBockLevel + triggers}. Maximal ${BockCalculator.maxAllowedTriggers(currentBockLevel)} zusätzliche(r) Auslöser erlaubt."
                    )
                }

                val participatingPlayers = gameDao.getPlayersForGameSync(gameId)
                val totalPlayerCount = participatingPlayers.size

                val maxRound = roundDao.getMaxRoundNumberForGame(gameId) ?: 0
                val nextRoundNumber = maxRound + 1

                // Nach dieser Runde:
                // 1. Jede bisher aktive Folge wird genau 1x heruntergezählt (0 entfernt).
                // 2. Jede neue Auslösung erzeugt eine eigene Bockfolge mit voller Restlaufzeit (4 bzw. 5).
                val nextSeries = BockCalculator.advanceActiveSeries(
                    currentSeries = currentSeries,
                    newTriggers = triggers,
                    durationPerSeries = duration
                )
                val resLevel = BockCalculator.calculateLevelFromActiveSeriesCount(nextSeries.size)
                val resMultiplier = BockCalculator.getMultiplier(resLevel)
                val remainingAfter = nextSeries.maxOrNull() ?: 0
                val nextSeriesStr = BockCalculator.serializeActiveSeries(nextSeries)

                val effectiveSoloPlayerId = if (gameType == GameType.SOLO.name) soloPlayerId else null

                val round = GameRound(
                    gameId = gameId,
                    roundNumber = nextRoundNumber,
                    timestamp = System.currentTimeMillis(),
                    basePoints = basePoints,
                    bockLevel = currentBockLevel,
                    bockMultiplier = multiplier,
                    hochzeitPlayerId = if (gameType == GameType.SOLO.name) null else hochzeitPlayerId,
                    sittingOutPlayerId = sittingOutPlayerId,
                    note = note.trim(),
                    triggeredBock = triggers > 0,
                    bockTriggersCount = triggers,
                    previousBockLevel = currentBockLevel,
                    resultingBockLevel = resLevel,
                    resultingBockMultiplier = resMultiplier,
                    remainingBockRoundsAfter = remainingAfter,
                    gameType = gameType,
                    soloPlayerId = effectiveSoloPlayerId
                )
                val roundId = roundDao.insertRound(round)

                // Hochzeitswertungsberechnung
                val weddingPointsMap: Map<Long, Int> = if (hochzeitPlayerId != null) {
                    val weddingOpponentsCount = totalPlayerCount - 1
                    val weddingGains = weddingOpponentsCount * multiplier
                    val weddingLoss = -1 * multiplier

                    participatingPlayers.associate { player ->
                        player.id to if (player.id == hochzeitPlayerId) weddingGains else weddingLoss
                    }
                } else {
                    participatingPlayers.associate { it.id to 0 }
                }

                val scores = participatingPlayers.map { player ->
                    val playerId = player.id
                    val isSitting = (playerId == sittingOutPlayerId)
                    val rawPts = rawPlayerPoints[playerId] ?: 0
                    val gamePts = if (isSitting) 0 else rawPts * multiplier
                    val weddingPts = weddingPointsMap[playerId] ?: 0
                    val totalFinalPts = gamePts + weddingPts

                    RoundScore(
                        roundId = roundId,
                        gameId = gameId,
                        playerId = playerId,
                        rawPoints = if (isSitting) 0 else rawPts,
                        gamePoints = gamePts,
                        weddingPoints = weddingPts,
                        finalPoints = totalFinalPts,
                        isWinner = totalFinalPts > 0,
                        isSittingOut = isSitting
                    )
                }
                roundDao.insertRoundScores(scores)

                // Speichere die Hochzeit als eigenes Wertungsereignis
                if (hochzeitPlayerId != null) {
                    val weddingEvent = WeddingEvent(
                        gameId = gameId,
                        roundId = roundId,
                        playerId = hochzeitPlayerId,
                        bockMultiplier = multiplier,
                        timestamp = System.currentTimeMillis()
                    )
                    weddingDao.insertWedding(weddingEvent)
                }

                val updatedGame = game.copy(
                    currentBockLevel = resLevel,
                    remainingBockRounds = remainingAfter,
                    activeBockSeries = nextSeriesStr
                )
                gameDao.updateGame(updatedGame)

                roundId
            }
        }
    }

    /**
     * Ermöglicht das Bearbeiten einer bestehenden Runde und aktualisiert die Punkte
     * sowie die Bockstufen aller nachfolgenden Runden konsistent.
     */
    suspend fun updateRound(
        roundId: Long,
        gameId: Long,
        basePoints: Int,
        rawPlayerPoints: Map<Long, Int>,
        bockLevel: Int? = null,
        hochzeitPlayerId: Long? = null,
        sittingOutPlayerId: Long? = null,
        note: String = "",
        bockTriggersCount: Int? = null,
        gameType: String? = null,
        soloPlayerId: Long? = null
    ): Result<Unit> {
        return runCatching {
            database.withTransaction {
                val existingRound = roundDao.getRoundByIdSync(roundId)
                    ?: throw IllegalStateException("Runde $roundId nicht gefunden")

                val updatedTriggers = bockTriggersCount ?: existingRound.bockTriggersCount
                val targetBockLevel = bockLevel ?: existingRound.bockLevel

                // Überschreitung von Stufe 8 prüfen
                if (targetBockLevel + updatedTriggers > BockCalculator.MAX_LEVEL) {
                    throw IllegalArgumentException(
                        "Maximale Bockstufe 8 (128x) würde überschritten werden: Stufe $targetBockLevel + $updatedTriggers = Stufe ${targetBockLevel + updatedTriggers}."
                    )
                }

                val participatingPlayers = gameDao.getPlayersForGameSync(gameId)
                val totalPlayerCount = participatingPlayers.size

                val safeLevel = BockCalculator.coerceLevel(targetBockLevel)
                val multiplier = BockCalculator.getMultiplier(safeLevel)

                val effectiveGameType = gameType ?: existingRound.gameType
                val effectiveSoloPlayerId = if (effectiveGameType == GameType.SOLO.name) {
                    soloPlayerId ?: existingRound.soloPlayerId
                } else null

                val updatedRound = existingRound.copy(
                    basePoints = basePoints,
                    bockLevel = safeLevel,
                    bockMultiplier = multiplier,
                    hochzeitPlayerId = if (effectiveGameType == GameType.SOLO.name) null else hochzeitPlayerId,
                    sittingOutPlayerId = sittingOutPlayerId,
                    note = note.trim(),
                    triggeredBock = updatedTriggers > 0,
                    bockTriggersCount = updatedTriggers,
                    gameType = effectiveGameType,
                    soloPlayerId = effectiveSoloPlayerId
                )
                roundDao.updateRound(updatedRound)

                // Früheres Hochzeitsereignis löschen
                weddingDao.deleteWeddingByRoundId(roundId)

                // Hochzeitswertungsberechnung
                val weddingPointsMap: Map<Long, Int> = if (hochzeitPlayerId != null) {
                    val weddingOpponentsCount = totalPlayerCount - 1
                    val weddingGains = weddingOpponentsCount * multiplier
                    val weddingLoss = -1 * multiplier

                    participatingPlayers.associate { player ->
                        player.id to if (player.id == hochzeitPlayerId) weddingGains else weddingLoss
                    }
                } else {
                    participatingPlayers.associate { it.id to 0 }
                }

                roundDao.deleteRoundScoresByRoundId(roundId)
                val newScores = participatingPlayers.map { player ->
                    val playerId = player.id
                    val isSitting = (playerId == sittingOutPlayerId)
                    val rawPts = rawPlayerPoints[playerId] ?: 0
                    val gamePts = if (isSitting) 0 else rawPts * multiplier
                    val weddingPts = weddingPointsMap[playerId] ?: 0
                    val totalFinalPts = gamePts + weddingPts

                    RoundScore(
                        roundId = roundId,
                        gameId = gameId,
                        playerId = playerId,
                        rawPoints = if (isSitting) 0 else rawPts,
                        gamePoints = gamePts,
                        weddingPoints = weddingPts,
                        finalPoints = totalFinalPts,
                        isWinner = totalFinalPts > 0,
                        isSittingOut = isSitting
                    )
                }
                roundDao.insertRoundScores(newScores)

                // Neues Hochzeitsereignis speichern
                if (hochzeitPlayerId != null) {
                    val weddingEvent = WeddingEvent(
                        gameId = gameId,
                        roundId = roundId,
                        playerId = hochzeitPlayerId,
                        bockMultiplier = multiplier,
                        timestamp = System.currentTimeMillis()
                    )
                    weddingDao.insertWedding(weddingEvent)
                }

                // Konsistente Neuberechnung aller nachfolgenden Runden
                recalculateAllRoundsForGame(gameId)
            }
        }
    }

    /**
     * Löscht eine Runde und berechnet alle nachfolgenden Runden und Punktestände konsistent neu.
     */
    suspend fun deleteRound(roundId: Long) {
        database.withTransaction {
            val existingRound = roundDao.getRoundByIdSync(roundId) ?: return@withTransaction
            val gameId = existingRound.gameId
            weddingDao.deleteWeddingByRoundId(roundId)
            roundDao.deleteRoundScoresByRoundId(roundId)
            roundDao.deleteRoundById(roundId)

            // Fortlaufende Rundennummerierung wiederherstellen
            val remainingRounds = roundDao.getRoundsForGameSync(gameId).sortedBy { it.roundNumber }
            remainingRounds.forEachIndexed { index, r ->
                if (r.roundNumber != index + 1) {
                    roundDao.updateRound(r.copy(roundNumber = index + 1))
                }
            }

            // Konsistente Neuberechnung der verbleibenden Runden
            recalculateAllRoundsForGame(gameId)
        }
    }

    /**
     * Berechnet die Bockstufen, Multiplikatoren, Rundenergebnisse und Hochzeitswertungen
     * aller Runden einer Partie konsistent neu (z.B. nach Bearbeitung oder Löschung).
     */
    suspend fun recalculateAllRoundsForGame(gameId: Long) {
        val game = gameDao.getGameByIdSync(gameId) ?: return
        val rounds = roundDao.getRoundsForGameSync(gameId).sortedBy { it.roundNumber }
        val participatingPlayers = gameDao.getPlayersForGameSync(gameId)
        val totalPlayerCount = participatingPlayers.size

        val duration = BockCalculator.getDurationForPlayerCount(game.playerCount, game.bockRoundsPerTrigger)

        // Initial-Zustand zu Beginn der Partie
        val initialSeries = if (game.initialRemainingBockRounds > 0 && game.initialBockLevel > 1) {
            val count = (game.initialBockLevel - 1).coerceIn(1, 7)
            List(count) { game.initialRemainingBockRounds }
        } else {
            emptyList()
        }

        if (rounds.isEmpty()) {
            val initialLevel = BockCalculator.calculateLevelFromActiveSeriesCount(initialSeries.size)
            val initialRemaining = initialSeries.maxOrNull() ?: 0
            gameDao.updateGame(
                game.copy(
                    currentBockLevel = initialLevel,
                    remainingBockRounds = initialRemaining,
                    activeBockSeries = BockCalculator.serializeActiveSeries(initialSeries)
                )
            )
            return
        }

        var currentSeries = initialSeries

        for (round in rounds) {
            val levelForThisRound = BockCalculator.calculateLevelFromActiveSeriesCount(currentSeries.size)
            val multiplierForThisRound = BockCalculator.getMultiplier(levelForThisRound)

            val triggers = round.bockTriggersCount.coerceIn(0, 8)
            val prevLevel = levelForThisRound

            val nextSeries = BockCalculator.advanceActiveSeries(
                currentSeries = currentSeries,
                newTriggers = triggers,
                durationPerSeries = duration
            )
            val resLevel = BockCalculator.calculateLevelFromActiveSeriesCount(nextSeries.size)
            val resMultiplier = BockCalculator.getMultiplier(resLevel)
            val remainingAfter = nextSeries.maxOrNull() ?: 0

            val updatedRound = round.copy(
                bockLevel = levelForThisRound,
                bockMultiplier = multiplierForThisRound,
                triggeredBock = triggers > 0,
                bockTriggersCount = triggers,
                previousBockLevel = prevLevel,
                resultingBockLevel = resLevel,
                resultingBockMultiplier = resMultiplier,
                remainingBockRoundsAfter = remainingAfter
            )
            roundDao.updateRound(updatedRound)

            // Punkte neu berechnen basierend auf dem neu ermittelten multiplierForThisRound
            val weddingGains = (totalPlayerCount - 1) * multiplierForThisRound
            val weddingLoss = -1 * multiplierForThisRound

            val scores = roundDao.getScoresForRoundSync(round.id)
            if (scores.isNotEmpty()) {
                val updatedScores = scores.map { score ->
                    val isSitting = score.isSittingOut || (score.playerId == round.sittingOutPlayerId)
                    val raw = score.rawPoints
                    val gamePts = if (isSitting) 0 else raw * multiplierForThisRound
                    val weddingPts = if (round.hochzeitPlayerId != null) {
                        if (score.playerId == round.hochzeitPlayerId) weddingGains else weddingLoss
                    } else 0
                    val finalPts = gamePts + weddingPts
                    score.copy(
                        gamePoints = gamePts,
                        weddingPoints = weddingPts,
                        finalPoints = finalPts,
                        isWinner = finalPts > 0
                    )
                }
                roundDao.updateRoundScores(updatedScores)
            }

            // Hochzeitsereignis Multiplikator aktualisieren
            if (round.hochzeitPlayerId != null) {
                val wedding = weddingDao.getWeddingByRoundIdSync(round.id)
                if (wedding != null) {
                    weddingDao.updateWedding(wedding.copy(bockMultiplier = multiplierForThisRound))
                }
            }

            currentSeries = nextSeries
        }

        // Finale Bockstufe und aktive Folgen der Partie aktualisieren
        val finalLevel = BockCalculator.calculateLevelFromActiveSeriesCount(currentSeries.size)
        val finalRemaining = currentSeries.maxOrNull() ?: 0
        val finalSeriesStr = BockCalculator.serializeActiveSeries(currentSeries)

        gameDao.updateGame(
            game.copy(
                currentBockLevel = finalLevel,
                remainingBockRounds = finalRemaining,
                activeBockSeries = finalSeriesStr
            )
        )
    }

    /**
     * Manuelle Konfiguration und Korrektur der aktuellen Bockstufe und Restrunden.
     */
    suspend fun setGameBockState(gameId: Long, level: Int, remainingRounds: Int) {
        val safeLevel = BockCalculator.coerceLevel(level)
        val safeRemaining = remainingRounds.coerceAtLeast(0)
        val game = gameDao.getGameByIdSync(gameId) ?: return
        val rounds = roundDao.getRoundsForGameSync(gameId)
        val isAtStart = rounds.isEmpty()

        val activeCount = if (safeRemaining == 0 || safeLevel == 1) 0 else safeLevel - 1
        val newSeries = if (activeCount > 0) List(activeCount) { safeRemaining } else emptyList()
        val newSeriesStr = BockCalculator.serializeActiveSeries(newSeries)

        gameDao.updateGame(
            game.copy(
                currentBockLevel = if (safeRemaining == 0) 1 else safeLevel,
                remainingBockRounds = safeRemaining,
                activeBockSeries = newSeriesStr,
                initialBockLevel = if (isAtStart) (if (safeRemaining == 0) 1 else safeLevel) else game.initialBockLevel,
                initialRemainingBockRounds = if (isAtStart) safeRemaining else game.initialRemainingBockRounds
            )
        )
    }

    suspend fun updateGameDetails(game: Game) {
        gameDao.updateGame(game)
    }

    suspend fun finishGame(gameId: Long) {
        val game = gameDao.getGameByIdSync(gameId) ?: return
        gameDao.updateGame(
            game.copy(
                status = Game.STATUS_COMPLETED,
                completedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun reopenGame(gameId: Long) {
        val game = gameDao.getGameByIdSync(gameId) ?: return
        gameDao.updateGame(
            game.copy(
                status = Game.STATUS_IN_PROGRESS,
                completedAt = null
            )
        )
    }

    suspend fun archiveGame(gameId: Long) {
        val game = gameDao.getGameByIdSync(gameId) ?: return
        gameDao.updateGame(
            game.copy(status = Game.STATUS_ARCHIVED)
        )
    }

    suspend fun deleteGame(gameId: Long) {
        database.withTransaction {
            val rounds = roundDao.getRoundsForGameSync(gameId)
            rounds.forEach {
                weddingDao.deleteWeddingByRoundId(it.id)
                roundDao.deleteRoundScoresByRoundId(it.id)
            }
            gameDao.deleteGameById(gameId)
        }
    }

    /**
     * Berechnet die vollständigen Statistiken für alle Spieler ausschließlich aus persistenten Daten.
     */
    fun getAllPlayersStats(): Flow<List<PlayerStats>> {
        return combine(
            playerDao.getAllPlayers(),
            gameDao.getAllGames(),
            roundDao.getRoundsForGame(0)
        ) { _, _, _ ->
            computeAllPlayerStatsSync()
        }
    }

    suspend fun computeAllPlayerStatsSync(): List<PlayerStats> {
        val players = playerDao.getAllPlayersSync()
        val allGames = gameDao.getAllGamesSync().associateBy { it.id }
        val allRoundsList = roundDao.getAllRoundsSync()
        val allRounds = allRoundsList.groupBy { it.gameId }
        val allRoundsById = allRoundsList.associateBy { it.id }
        val allScores = roundDao.getAllScoresSync().groupBy { it.playerId }
        val allCrossRefs = gameDao.getAllGamePlayerCrossRefsSync().groupBy { it.playerId }
        val allWeddings = weddingDao.getAllWeddingsSync().groupBy { it.playerId }

        return players.map { player ->
            val playerScores = allScores[player.id] ?: emptyList()
            val participatedGameIds = allCrossRefs[player.id]?.map { it.gameId }?.toSet() ?: emptySet()
            val totalGames = participatedGameIds.size

            val completedGames = participatedGameIds.count { gameId ->
                allGames[gameId]?.status == Game.STATUS_COMPLETED
            }

            val activeScores = playerScores.filter { !it.isSittingOut }
            val totalRounds = activeScores.size
            val totalPoints = playerScores.sumOf { it.finalPoints }

            val avgPerRound = if (totalRounds > 0) totalPoints.toDouble() / totalRounds else 0.0
            val avgPerGame = if (totalGames > 0) totalPoints.toDouble() / totalGames else 0.0

            val bestRound = activeScores.maxOfOrNull { it.finalPoints }
            val worstRound = activeScores.minOfOrNull { it.finalPoints }

            val weddingCount = allWeddings[player.id]?.size ?: 0

            // Solo-spezifische Auswertung
            var normalRounds = 0
            var normalPts = 0
            var soloAsSoloist = 0
            var soloWinsSoloist = 0
            var soloPtsSoloist = 0
            var soloAsOpponent = 0
            var soloWinsOpponent = 0
            var soloPtsOpponent = 0

            activeScores.forEach { s ->
                val round = allRoundsById[s.roundId]
                if (round != null) {
                    if (round.isSolo) {
                        if (round.soloPlayerId == player.id) {
                            soloAsSoloist++
                            soloPtsSoloist += s.finalPoints
                            if (s.finalPoints > 0) soloWinsSoloist++
                        } else {
                            soloAsOpponent++
                            soloPtsOpponent += s.finalPoints
                            if (s.finalPoints > 0) soloWinsOpponent++
                        }
                    } else {
                        normalRounds++
                        normalPts += s.finalPoints
                    }
                }
            }

            // Siege: Berechne bei abgeschlossenen Spielen, ob dieser Spieler Platz 1 hatte
            var wins = 0
            participatedGameIds.forEach { gId ->
                val game = allGames[gId]
                if (game?.status == Game.STATUS_COMPLETED) {
                    val gameRounds = allRounds[gId] ?: emptyList()
                    val roundIds = gameRounds.map { it.id }.toSet()
                    val gameScores = roundDao.getAllScoresSync().filter { it.roundId in roundIds }
                    val totalsByPlayer = gameScores.groupBy { it.playerId }
                        .mapValues { entry -> entry.value.sumOf { it.finalPoints } }
                    val maxScore = totalsByPlayer.values.maxOrNull()
                    if (maxScore != null && totalsByPlayer[player.id] == maxScore) {
                        wins++
                    }
                }
            }

            val winRate = if (completedGames > 0) (wins.toDouble() / completedGames) * 100.0 else 0.0

            val highestBock = allRounds.values.flatten()
                .filter { r -> playerScores.any { s -> s.roundId == r.id } }
                .maxOfOrNull { it.bockLevel } ?: 1

            PlayerStats(
                player = player,
                totalGames = totalGames,
                completedGames = completedGames,
                totalRoundsPlayed = totalRounds,
                totalPoints = totalPoints,
                averagePointsPerRound = avgPerRound,
                averagePointsPerGame = avgPerGame,
                bestRoundScore = bestRound,
                worstRoundScore = worstRound,
                weddingCount = weddingCount,
                winsCount = wins,
                winRatePercent = winRate,
                highestBockLevelPlayed = highestBock,
                normalRoundsPlayed = normalRounds,
                normalPoints = normalPts,
                soloRoundsAsSoloist = soloAsSoloist,
                soloWinsAsSoloist = soloWinsSoloist,
                soloPointsAsSoloist = soloPtsSoloist,
                soloRoundsAsOpponent = soloAsOpponent,
                soloWinsAsOpponent = soloWinsOpponent,
                soloPointsAsOpponent = soloPtsOpponent
            )
        }.sortedByDescending { it.totalPoints }
    }

    suspend fun getPlayerStatsSync(playerId: Long): PlayerStats? {
        val all = computeAllPlayerStatsSync()
        return all.find { it.player.id == playerId }
    }
}
