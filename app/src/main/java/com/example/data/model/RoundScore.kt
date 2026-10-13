package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "round_scores",
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
        )
    ],
    indices = [
        Index("roundId"),
        Index("playerId"),
        Index("gameId"),
        Index(value = ["roundId", "playerId"], unique = true)
    ]
)
data class RoundScore(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val roundId: Long,
    val gameId: Long,
    val playerId: Long,
    val rawPoints: Int, // Ursprüngliche Spielpunkte vor Multiplikator (z.B. +2 oder -2)
    val gamePoints: Int = 0, // Spielpunkte nach Multiplikator (rawPoints * bockMultiplier)
    val weddingPoints: Int = 0, // Punktwirkung aus Hochzeit ((Mitspieler-1)*M bzw. -1*M)
    val finalPoints: Int, // Gesamte resultierende Punkte dieser Runde (gamePoints + weddingPoints)
    val isWinner: Boolean = false,
    val isSittingOut: Boolean = false
)
