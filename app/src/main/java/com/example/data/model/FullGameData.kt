package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class RoundWithScores(
    @Embedded
    val round: GameRound,
    @Relation(
        parentColumn = "id",
        entityColumn = "roundId"
    )
    val scores: List<RoundScore>
)

data class FullGameData(
    val game: Game,
    val players: List<Player>, // In seat order
    val roundsWithScores: List<RoundWithScores>
) {
    val roundCount: Int get() = roundsWithScores.size

    val playerTotals: Map<Long, Int> by lazy {
        val totals = mutableMapOf<Long, Int>()
        players.forEach { totals[it.id] = 0 }
        roundsWithScores.forEach { roundWithScores ->
            roundWithScores.scores.forEach { score ->
                totals[score.playerId] = (totals[score.playerId] ?: 0) + score.finalPoints
            }
        }
        totals
    }

    val normalRoundCount: Int get() = roundsWithScores.count { !it.round.isSolo }
    val soloRoundCount: Int get() = roundsWithScores.count { it.round.isSolo }

    /**
     * Map von PlayerId zu einer Liste von kumulierten Punkteständen nach jeder Runde (inkl. Start 0).
     * Optional filterbar nach gameType (z.B. "NORMAL" oder "SOLO").
     */
    fun getCumulativeScoreProgression(filterGameType: String? = null): Map<Long, List<Int>> {
        val progression = mutableMapOf<Long, MutableList<Int>>()
        players.forEach { progression[it.id] = mutableListOf(0) }

        val runningTotals = players.associate { it.id to 0 }.toMutableMap()
        val sortedRounds = roundsWithScores
            .filter { filterGameType == null || it.round.gameType == filterGameType }
            .sortedBy { it.round.roundNumber }

        for (roundWithScore in sortedRounds) {
            roundWithScore.scores.forEach { score ->
                val current = runningTotals[score.playerId] ?: 0
                val updated = current + score.finalPoints
                runningTotals[score.playerId] = updated
            }
            players.forEach { player ->
                progression[player.id]?.add(runningTotals[player.id] ?: 0)
            }
        }
        return progression
    }

    /**
     * Ermittelt den aktuellen Geber-Index (0 .. players.size-1) basierend auf der Rundenzahl.
     */
    val currentDealerIndex: Int
        get() = if (players.isNotEmpty()) roundsWithScores.size % players.size else 0

    val currentDealer: Player?
        get() = players.getOrNull(currentDealerIndex)

    /**
     * Bei 5 Spielern sitzt der Geber standardmäßig aus.
     */
    val defaultSittingOutPlayer: Player?
        get() = if (game.playerCount == 5) currentDealer else null

    /**
     * Rangliste sortiert nach Gesamtpunkten absteigend.
     */
    val rankings: List<Pair<Player, Int>>
        get() {
            return players
                .map { player -> player to (playerTotals[player.id] ?: 0) }
                .sortedByDescending { it.second }
        }
}
