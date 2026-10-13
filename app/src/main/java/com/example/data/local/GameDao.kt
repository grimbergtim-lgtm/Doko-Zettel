package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Game
import com.example.data.model.GamePlayerCrossRef
import com.example.data.model.Player
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY createdAt DESC")
    fun getAllGames(): Flow<List<Game>>

    @Query("SELECT * FROM games ORDER BY createdAt DESC")
    suspend fun getAllGamesSync(): List<Game>

    @Query("SELECT * FROM games WHERE status = 'IN_PROGRESS' ORDER BY createdAt DESC")
    fun getActiveGames(): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE status = 'IN_PROGRESS' ORDER BY createdAt DESC LIMIT 1")
    fun getLatestActiveGame(): Flow<Game?>

    @Query("SELECT * FROM games WHERE id = :id")
    fun getGameById(id: Long): Flow<Game?>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameByIdSync(id: Long): Game?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: Game): Long

    @Update
    suspend fun updateGame(game: Game)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGameById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGamePlayer(crossRef: GamePlayerCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGamePlayers(crossRefs: List<GamePlayerCrossRef>)

    @Query("""
        SELECT p.* FROM players p
        INNER JOIN game_players gp ON p.id = gp.playerId
        WHERE gp.gameId = :gameId
        ORDER BY gp.seatOrder ASC
    """)
    fun getPlayersForGame(gameId: Long): Flow<List<Player>>

    @Query("""
        SELECT p.* FROM players p
        INNER JOIN game_players gp ON p.id = gp.playerId
        WHERE gp.gameId = :gameId
        ORDER BY gp.seatOrder ASC
    """)
    suspend fun getPlayersForGameSync(gameId: Long): List<Player>

    @Query("SELECT * FROM game_players WHERE gameId = :gameId ORDER BY seatOrder ASC")
    suspend fun getGamePlayerCrossRefsSync(gameId: Long): List<GamePlayerCrossRef>

    @Query("SELECT * FROM game_players")
    suspend fun getAllGamePlayerCrossRefsSync(): List<GamePlayerCrossRef>
}
