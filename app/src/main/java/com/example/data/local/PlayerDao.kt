package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Player
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY name COLLATE NOCASE ASC")
    fun getAllPlayers(): Flow<List<Player>>

    @Query("SELECT * FROM players ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllPlayersSync(): List<Player>

    @Query("SELECT * FROM players WHERE id = :id")
    fun getPlayerById(id: Long): Flow<Player?>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerByIdSync(id: Long): Player?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: Player): Long

    @Update
    suspend fun updatePlayer(player: Player)

    @Delete
    suspend fun deletePlayer(player: Player)

    @Query("SELECT COUNT(*) FROM players")
    suspend fun getPlayerCount(): Int
}
