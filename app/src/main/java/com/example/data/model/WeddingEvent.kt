package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wedding_events",
    foreignKeys = [
        ForeignKey(
            entity = GameRound::class,
            parentColumns = ["id"],
            childColumns = ["roundId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["roundId"], unique = true), // Genau ein Hochzeitsereignis pro Runde verhindert Doppelbuchungen
        Index("playerId"),
        Index("gameId")
    ]
)
data class WeddingEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val roundId: Long,
    val playerId: Long,
    val bockMultiplier: Int, // Zum Wertungszeitpunkt gültiger Bockmultiplikator
    val timestamp: Long = System.currentTimeMillis()
)
