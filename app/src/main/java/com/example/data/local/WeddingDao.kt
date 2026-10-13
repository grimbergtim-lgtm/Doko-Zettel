package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.WeddingEvent

@Dao
interface WeddingDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWedding(wedding: WeddingEvent): Long

    @androidx.room.Update
    suspend fun updateWedding(wedding: WeddingEvent)

    @Query("DELETE FROM wedding_events WHERE roundId = :roundId")
    suspend fun deleteWeddingByRoundId(roundId: Long)

    @Query("SELECT * FROM wedding_events WHERE roundId = :roundId LIMIT 1")
    suspend fun getWeddingByRoundIdSync(roundId: Long): WeddingEvent?

    @Query("SELECT * FROM wedding_events WHERE gameId = :gameId")
    suspend fun getWeddingsForGameSync(gameId: Long): List<WeddingEvent>

    @Query("SELECT * FROM wedding_events WHERE playerId = :playerId")
    suspend fun getWeddingsForPlayerSync(playerId: Long): List<WeddingEvent>

    @Query("SELECT * FROM wedding_events")
    suspend fun getAllWeddingsSync(): List<WeddingEvent>
}
