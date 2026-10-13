package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Game
import com.example.data.model.GamePlayerCrossRef
import com.example.data.model.GameRound
import com.example.data.model.Player
import com.example.data.model.RoundScore
import com.example.data.model.WeddingEvent

@Database(
    entities = [
        Player::class,
        Game::class,
        GamePlayerCrossRef::class,
        GameRound::class,
        RoundScore::class,
        WeddingEvent::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun gameDao(): GameDao
    abstract fun roundDao(): RoundDao
    abstract fun weddingDao(): WeddingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE round_scores ADD COLUMN gamePoints INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE round_scores ADD COLUMN weddingPoints INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `wedding_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `gameId` INTEGER NOT NULL,
                        `roundId` INTEGER NOT NULL,
                        `playerId` INTEGER NOT NULL,
                        `bockMultiplier` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        FOREIGN KEY(`roundId`) REFERENCES `game_rounds`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`playerId`) REFERENCES `players`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`gameId`) REFERENCES `games`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_wedding_events_roundId` ON `wedding_events` (`roundId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wedding_events_playerId` ON `wedding_events` (`playerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wedding_events_gameId` ON `wedding_events` (`gameId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE game_rounds ADD COLUMN bockTriggersCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE game_rounds ADD COLUMN previousBockLevel INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE game_rounds ADD COLUMN resultingBockLevel INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE game_rounds ADD COLUMN resultingBockMultiplier INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE game_rounds ADD COLUMN remainingBockRoundsAfter INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE games ADD COLUMN initialBockLevel INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE games ADD COLUMN initialRemainingBockRounds INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE game_rounds ADD COLUMN gameType TEXT NOT NULL DEFAULT 'NORMAL'")
                db.execSQL("ALTER TABLE game_rounds ADD COLUMN soloPlayerId INTEGER DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "doppelkopf.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
