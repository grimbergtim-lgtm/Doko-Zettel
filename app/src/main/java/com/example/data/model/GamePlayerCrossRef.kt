package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "game_players",
    primaryKeys = ["gameId", "playerId"],
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
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
        Index("gameId"),
        Index("playerId")
    ]
)
data class GamePlayerCrossRef(
    val gameId: Long,
    val playerId: Long,
    val seatOrder: Int // 0, 1, 2, 3 (, 4)
)
