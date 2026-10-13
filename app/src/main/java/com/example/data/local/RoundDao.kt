package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.GameRound
import com.example.data.model.RoundScore
import com.example.data.model.RoundWithScores
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {
    @Query("SELECT * FROM game_rounds WHERE gameId = :gameId ORDER BY roundNumber ASC")
    fun getRoundsForGame(gameId: Long): Flow<List<GameRound>>

    @Query("SELECT * FROM game_rounds WHERE gameId = :gameId ORDER BY roundNumber ASC")
    suspend fun getRoundsForGameSync(gameId: Long): List<GameRound>

    @Transaction
    @Query("SELECT * FROM game_rounds WHERE gameId = :gameId ORDER BY roundNumber ASC")
    fun getRoundWithScoresForGame(gameId: Long): Flow<List<RoundWithScores>>

    @Transaction
    @Query("SELECT * FROM game_rounds WHERE gameId = :gameId ORDER BY roundNumber ASC")
    suspend fun getRoundWithScoresForGameSync(gameId: Long): List<RoundWithScores>

    @Query("SELECT * FROM game_rounds WHERE id = :roundId")
    suspend fun getRoundByIdSync(roundId: Long): GameRound?

    @Query("SELECT MAX(roundNumber) FROM game_rounds WHERE gameId = :gameId")
    suspend fun getMaxRoundNumberForGame(gameId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRound(round: GameRound): Long

    @Update
    suspend fun updateRound(round: GameRound)

    @Query("DELETE FROM game_rounds WHERE id = :roundId")
    suspend fun deleteRoundById(roundId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoundScores(scores: List<RoundScore>)

    @Query("SELECT * FROM round_scores WHERE roundId = :roundId")
    suspend fun getScoresForRoundSync(roundId: Long): List<RoundScore>

    @Update
    suspend fun updateRoundScores(scores: List<RoundScore>)

    @Query("DELETE FROM round_scores WHERE roundId = :roundId")
    suspend fun deleteRoundScoresByRoundId(roundId: Long)

    @Query("SELECT * FROM round_scores WHERE playerId = :playerId")
    fun getAllScoresForPlayer(playerId: Long): Flow<List<RoundScore>>

    @Query("SELECT * FROM round_scores WHERE playerId = :playerId")
    suspend fun getAllScoresForPlayerSync(playerId: Long): List<RoundScore>

    @Query("SELECT * FROM game_rounds WHERE hochzeitPlayerId = :playerId")
    suspend fun getWeddingsForPlayerSync(playerId: Long): List<GameRound>

    @Query("SELECT * FROM game_rounds")
    suspend fun getAllRoundsSync(): List<GameRound>

    @Query("SELECT * FROM round_scores")
    suspend fun getAllScoresSync(): List<RoundScore>
}
