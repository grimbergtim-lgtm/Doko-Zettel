package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val playerCount: Int = 4, // 4 or 5
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val status: String = STATUS_IN_PROGRESS, // IN_PROGRESS, COMPLETED, ARCHIVED
    val currentBockLevel: Int = 1, // Stufe 1 bis 8
    val remainingBockRounds: Int = 0, // Verbleibende Bockrunden
    val bockRoundsPerTrigger: Int = 4, // z.B. 4 oder 5 Runden pro Bock
    val notes: String = "",
    val initialBockLevel: Int = 1, // Ursprüngliche Start-Bockstufe
    val initialRemainingBockRounds: Int = 0, // Ursprüngliche verbleibende Bockrunden
    val activeBockSeries: String = "" // Restlaufzeiten der aktiven Bockfolgen als CSV, z. B. "3,1"
) {
    companion object {
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_ARCHIVED = "ARCHIVED"
    }

    val isInProgress: Boolean get() = status == STATUS_IN_PROGRESS
    val isCompleted: Boolean get() = status == STATUS_COMPLETED
    val isArchived: Boolean get() = status == STATUS_ARCHIVED
}
