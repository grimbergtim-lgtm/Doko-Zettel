package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: Long = 0xFF2563EBL,
    val avatarSymbol: String = "♠",
    val createdAt: Long = System.currentTimeMillis()
)
