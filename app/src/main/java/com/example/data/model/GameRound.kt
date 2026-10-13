package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class GameType {
    NORMAL,
    SOLO
}

@Entity(
    tableName = "game_rounds",
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("gameId"),
        Index(value = ["gameId", "roundNumber"], unique = true)
    ]
)
data class GameRound(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val roundNumber: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val basePoints: Int = 1, // Roh-Gewinnpunkte der Runde (z.B. 1, 2, 3...)
    val bockLevel: Int = 1, // Stufe 1 bis 8 (die für DIESE Runde galt)
    val bockMultiplier: Int = 1, // Multiplikator 2^(bockLevel - 1)
    val gameType: String = GameType.NORMAL.name, // NORMAL oder SOLO
    val soloPlayerId: Long? = null, // ID des Solospielers bei einem Solospiel
    val hochzeitPlayerId: Long? = null, // Hochzeit zugeordneter Spieler
    val sittingOutPlayerId: Long? = null, // Bei 5 Spielern der Aussetzer
    val note: String = "",
    val triggeredBock: Boolean = false, // Hat diese Runde neue Bockrunde(n) ausgelöst?
    val bockTriggersCount: Int = 0, // Anzahl der neu ausgelösten Bockeinheiten (0 bis 8)
    val previousBockLevel: Int = 1, // Vorherige Bockstufe vor den neuen Auslösern
    val resultingBockLevel: Int = 1, // Resultierende Bockstufe nach den neuen Auslösern
    val resultingBockMultiplier: Int = 1, // Resultierender Multiplikator 2^(resultingBockLevel - 1)
    val remainingBockRoundsAfter: Int = 0 // Verbleibende Bockrunden nach dieser Runde
) {
    val isSolo: Boolean get() = gameType == GameType.SOLO.name
}
