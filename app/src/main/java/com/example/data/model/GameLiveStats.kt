package com.example.data.model

/**
 * Datenmodelle und Berechnungslogik für die 5 erweiterten Spielstatistiken
 * des aktuell laufenden Spiels (im Reiter „Graph“).
 */

data class PlayerRoundWinStats(
    val player: Player,
    val roundsPlayed: Int,
    val winsCount: Int,
    val lossesCount: Int,
    val drawsCount: Int,
    val winRatePercent: Double
)

data class PlayerStreakStats(
    val player: Player,
    val currentWinStreak: Int,
    val maxWinStreak: Int,
    val currentLossStreak: Int,
    val maxLossStreak: Int
)

data class BockMultiplierStats(
    val multiplier: Int,
    val bockLevel: Int,
    val displayName: String,
    val roundsPlayed: Int,
    val winsCount: Int,
    val winRatePercent: Double,
    val points: Int
)

data class PlayerBockStats(
    val player: Player,
    val bockRoundsPlayed: Int,
    val bockWins: Int,
    val bockWinRatePercent: Double,
    val bockPoints: Int,
    val breakdownByMultiplier: List<BockMultiplierStats>
)

data class PartnerStats(
    val player1: Player,
    val player2: Player,
    val roundsTogether: Int,
    val frequencyPercent: Double,
    val winsTogether: Int,
    val lossesTogether: Int,
    val drawsTogether: Int,
    val winRatePercent: Double,
    val jointPointsBalance: Int, // Gesamte Punktebilanz beider Spieler zusammen aus gemeinsamen Runden
    val wonRoundsPointsSum: Int  // Summe der Punkte aus den gemeinsam gewonnenen Runden
) {
    fun containsPlayer(playerId: Long): Boolean = player1.id == playerId || player2.id == playerId

    fun otherPlayer(playerId: Long): Player? = when (playerId) {
        player1.id -> player2
        player2.id -> player1
        else -> null
    }

    val pairKey: String get() = "${minOf(player1.id, player2.id)}_${maxOf(player1.id, player2.id)}"
}

data class GameLiveStats(
    val playerWins: List<PlayerRoundWinStats>,
    val playerStreaks: List<PlayerStreakStats>,
    val playerBockStats: List<PlayerBockStats>,
    val partnerStats: List<PartnerStats>
)

object GameLiveStatsCalculator {

    /**
     * Ermittelt die Parteien einer Runde für aktive Spieler (nicht aussetzend).
     * Gibt eine Liste von Parteien (Spieler-IDs je Partei) zurück.
     * - Bei Solo: Solist als 1-Person-Partei, Gegenspieler gemeinsam als gegnerische Partei.
     * - Bei Normalspiel / Hochzeit: Siegerpartei (rawPoints > 0) und Verliererpartei (rawPoints < 0).
     * - Bei unentschiedenen / 0-Punkte-Runden: leere Liste.
     */
    fun getPartiesInRound(rws: RoundWithScores): List<Set<Long>> {
        val round = rws.round
        val activeScores = rws.scores.filter { !it.isSittingOut && it.playerId != round.sittingOutPlayerId }
        if (activeScores.isEmpty()) return emptyList()

        return if (round.isSolo) {
            val soloId = round.soloPlayerId
            if (soloId != null) {
                val soloParty = setOf(soloId)
                val opponentsParty = activeScores.map { it.playerId }.filter { it != soloId }.toSet()
                listOf(soloParty, opponentsParty)
            } else {
                emptyList()
            }
        } else {
            val winners = activeScores.filter { it.rawPoints > 0 }.map { it.playerId }.toSet()
            val losers = activeScores.filter { it.rawPoints < 0 }.map { it.playerId }.toSet()
            if (winners.isNotEmpty() && losers.isNotEmpty()) {
                listOf(winners, losers)
            } else {
                emptyList()
            }
        }
    }

    /**
     * Berechnet die vollständigen Live-Statistiken für ein laufendes Spiel.
     */
    fun calculate(gameData: FullGameData): GameLiveStats {
        val sortedRounds = gameData.roundsWithScores.sortedBy { it.round.roundNumber }
        val players = gameData.players

        return GameLiveStats(
            playerWins = calculateWins(sortedRounds, players),
            playerStreaks = calculateStreaks(sortedRounds, players),
            playerBockStats = calculateBockStats(sortedRounds, players),
            partnerStats = calculatePartnerStats(sortedRounds, players)
        )
    }

    // 1. Anzahl der Siege pro Spieler
    fun calculateWins(
        sortedRounds: List<RoundWithScores>,
        players: List<Player>
    ): List<PlayerRoundWinStats> {
        return players.map { player ->
            val activeRounds = sortedRounds.mapNotNull { rws ->
                val score = rws.scores.find { it.playerId == player.id }
                val isSitting = score?.isSittingOut == true || player.id == rws.round.sittingOutPlayerId
                if (score != null && !isSitting) Pair(rws, score) else null
            }

            val played = activeRounds.size
            var wins = 0
            var losses = 0
            var draws = 0

            for ((_, score) in activeRounds) {
                when {
                    score.rawPoints > 0 -> wins++
                    score.rawPoints < 0 -> losses++
                    else -> draws++
                }
            }

            val winRate = if (played > 0) (wins.toDouble() / played) * 100.0 else 0.0

            PlayerRoundWinStats(
                player = player,
                roundsPlayed = played,
                winsCount = wins,
                lossesCount = losses,
                drawsCount = draws,
                winRatePercent = winRate
            )
        }.sortedByDescending { it.winsCount }
    }

    // 2. Sieg- und Niederlagenserien
    // Regel für unentschiedene Runden: Unentschiedene Runden (0 Rohpunkte) setzen laufende Serien
    // zurück und werden weder als Sieg noch als Niederlage gewertet. Aussetzende Spieler pausieren ihre Serie.
    fun calculateStreaks(
        sortedRounds: List<RoundWithScores>,
        players: List<Player>
    ): List<PlayerStreakStats> {
        return players.map { player ->
            var currentWin = 0
            var maxWin = 0
            var currentLoss = 0
            var maxLoss = 0

            for (rws in sortedRounds) {
                val score = rws.scores.find { it.playerId == player.id }
                val isSitting = score?.isSittingOut == true || player.id == rws.round.sittingOutPlayerId
                if (score == null || isSitting) {
                    // Spieler sitzt aus: Serie bleibt pausiert/unverändert
                    continue
                }

                when {
                    score.rawPoints > 0 -> {
                        currentWin++
                        currentLoss = 0
                        if (currentWin > maxWin) maxWin = currentWin
                    }
                    score.rawPoints < 0 -> {
                        currentLoss++
                        currentWin = 0
                        if (currentLoss > maxLoss) maxLoss = currentLoss
                    }
                    else -> {
                        // Unentschieden: setzt beide laufenden Serien zurück
                        currentWin = 0
                        currentLoss = 0
                    }
                }
            }

            PlayerStreakStats(
                player = player,
                currentWinStreak = currentWin,
                maxWinStreak = maxWin,
                currentLossStreak = currentLoss,
                maxLossStreak = maxLoss
            )
        }
    }

    // 3. Erfolge in Bockrunden (Multiplikator >= 2)
    fun calculateBockStats(
        sortedRounds: List<RoundWithScores>,
        players: List<Player>
    ): List<PlayerBockStats> {
        val bockRounds = sortedRounds.filter { it.round.bockMultiplier >= 2 }

        return players.map { player ->
            val activeBockRounds = bockRounds.mapNotNull { rws ->
                val score = rws.scores.find { it.playerId == player.id }
                val isSitting = score?.isSittingOut == true || player.id == rws.round.sittingOutPlayerId
                if (score != null && !isSitting) Pair(rws, score) else null
            }

            val played = activeBockRounds.size
            val wins = activeBockRounds.count { it.second.rawPoints > 0 }
            val winRate = if (played > 0) (wins.toDouble() / played) * 100.0 else 0.0
            val points = activeBockRounds.sumOf { it.second.finalPoints }

            val byMultiplier = activeBockRounds.groupBy { it.first.round.bockMultiplier }
                .entries
                .sortedBy { it.key }
                .map { (mult, entries) ->
                    val level = entries.firstOrNull()?.first?.round?.bockLevel
                        ?: (BockCalculator.getBockStufenForMultiplier(mult) + 1).coerceIn(1, 8)
                    val name = BockCalculator.formatBockWithMultiplier(mult)
                    val count = entries.size
                    val mWins = entries.count { it.second.rawPoints > 0 }
                    val mRate = if (count > 0) (mWins.toDouble() / count) * 100.0 else 0.0
                    val mPts = entries.sumOf { it.second.finalPoints }

                    BockMultiplierStats(
                        multiplier = mult,
                        bockLevel = level,
                        displayName = name,
                        roundsPlayed = count,
                        winsCount = mWins,
                        winRatePercent = mRate,
                        points = mPts
                    )
                }

            PlayerBockStats(
                player = player,
                bockRoundsPlayed = played,
                bockWins = wins,
                bockWinRatePercent = winRate,
                bockPoints = points,
                breakdownByMultiplier = byMultiplier
            )
        }.sortedByDescending { it.bockWins }
    }

    // 4 & 5. Häufigste gemeinsame Spielpartner & Erfolgsbilanz mit Partnern
    fun calculatePartnerStats(
        sortedRounds: List<RoundWithScores>,
        players: List<Player>
    ): List<PartnerStats> {
        val totalRounds = sortedRounds.size
        val pairsList = mutableListOf<PartnerStats>()

        for (i in 0 until players.size) {
            for (j in i + 1 until players.size) {
                val p1 = players[i]
                val p2 = players[j]

                var roundsTogether = 0
                var winsTogether = 0
                var lossesTogether = 0
                var drawsTogether = 0
                var jointPointsBalance = 0
                var wonRoundsPointsSum = 0

                for (rws in sortedRounds) {
                    val parties = getPartiesInRound(rws)
                    val sameParty = parties.find { it.contains(p1.id) && it.contains(p2.id) }
                    if (sameParty != null) {
                        roundsTogether++
                        val s1 = rws.scores.find { it.playerId == p1.id }
                        val s2 = rws.scores.find { it.playerId == p2.id }
                        val p1Final = s1?.finalPoints ?: 0
                        val p2Final = s2?.finalPoints ?: 0
                        val raw1 = s1?.rawPoints ?: 0

                        val roundTotal = p1Final + p2Final
                        jointPointsBalance += roundTotal

                        when {
                            raw1 > 0 -> {
                                winsTogether++
                                wonRoundsPointsSum += roundTotal
                            }
                            raw1 < 0 -> {
                                lossesTogether++
                            }
                            else -> {
                                drawsTogether++
                            }
                        }
                    }
                }

                val freq = if (totalRounds > 0) (roundsTogether.toDouble() / totalRounds) * 100.0 else 0.0
                val winRate = if (roundsTogether > 0) (winsTogether.toDouble() / roundsTogether) * 100.0 else 0.0

                pairsList.add(
                    PartnerStats(
                        player1 = p1,
                        player2 = p2,
                        roundsTogether = roundsTogether,
                        frequencyPercent = freq,
                        winsTogether = winsTogether,
                        lossesTogether = lossesTogether,
                        drawsTogether = drawsTogether,
                        winRatePercent = winRate,
                        jointPointsBalance = jointPointsBalance,
                        wonRoundsPointsSum = wonRoundsPointsSum
                    )
                )
            }
        }

        return pairsList.sortedWith(
            compareByDescending<PartnerStats> { it.roundsTogether }
                .thenByDescending { it.winsTogether }
                .thenByDescending { it.jointPointsBalance }
        )
    }
}
