package com.example.data.backup

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.model.Game
import com.example.data.model.GamePlayerCrossRef
import com.example.data.model.GameRound
import com.example.data.model.Player
import com.example.data.model.RoundScore
import com.example.data.model.WeddingEvent
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    suspend fun exportToJson(database: AppDatabase): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        val players = database.playerDao().getAllPlayersSync()
        val playersArray = JSONArray()
        players.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("colorHex", p.colorHex)
            obj.put("avatarSymbol", p.avatarSymbol)
            obj.put("createdAt", p.createdAt)
            playersArray.put(obj)
        }
        root.put("players", playersArray)

        val games = database.gameDao().getAllGamesSync()
        val gamesArray = JSONArray()
        games.forEach { g ->
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("title", g.title)
            obj.put("playerCount", g.playerCount)
            obj.put("createdAt", g.createdAt)
            if (g.completedAt != null) obj.put("completedAt", g.completedAt)
            obj.put("status", g.status)
            obj.put("currentBockLevel", g.currentBockLevel)
            obj.put("remainingBockRounds", g.remainingBockRounds)
            obj.put("bockRoundsPerTrigger", g.bockRoundsPerTrigger)
            obj.put("activeBockSeries", g.activeBockSeries)
            obj.put("notes", g.notes)
            gamesArray.put(obj)
        }
        root.put("games", gamesArray)

        val crossRefs = database.gameDao().getAllGamePlayerCrossRefsSync()
        val crossRefsArray = JSONArray()
        crossRefs.forEach { cr ->
            val obj = JSONObject()
            obj.put("gameId", cr.gameId)
            obj.put("playerId", cr.playerId)
            obj.put("seatOrder", cr.seatOrder)
            crossRefsArray.put(obj)
        }
        root.put("gamePlayers", crossRefsArray)

        val rounds = database.roundDao().getAllRoundsSync()
        val roundsArray = JSONArray()
        rounds.forEach { r ->
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("gameId", r.gameId)
            obj.put("roundNumber", r.roundNumber)
            obj.put("timestamp", r.timestamp)
            obj.put("basePoints", r.basePoints)
            obj.put("bockLevel", r.bockLevel)
            obj.put("bockMultiplier", r.bockMultiplier)
            obj.put("gameType", r.gameType)
            if (r.soloPlayerId != null) obj.put("soloPlayerId", r.soloPlayerId)
            if (r.hochzeitPlayerId != null) obj.put("hochzeitPlayerId", r.hochzeitPlayerId)
            if (r.sittingOutPlayerId != null) obj.put("sittingOutPlayerId", r.sittingOutPlayerId)
            obj.put("note", r.note)
            obj.put("triggeredBock", r.triggeredBock)
            obj.put("bockTriggersCount", r.bockTriggersCount)
            obj.put("previousBockLevel", r.previousBockLevel)
            obj.put("resultingBockLevel", r.resultingBockLevel)
            obj.put("resultingBockMultiplier", r.resultingBockMultiplier)
            obj.put("remainingBockRoundsAfter", r.remainingBockRoundsAfter)
            roundsArray.put(obj)
        }
        root.put("gameRounds", roundsArray)

        val scores = database.roundDao().getAllScoresSync()
        val scoresArray = JSONArray()
        scores.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("roundId", s.roundId)
            obj.put("gameId", s.gameId)
            obj.put("playerId", s.playerId)
            obj.put("rawPoints", s.rawPoints)
            obj.put("gamePoints", s.gamePoints)
            obj.put("weddingPoints", s.weddingPoints)
            obj.put("finalPoints", s.finalPoints)
            obj.put("isWinner", s.isWinner)
            obj.put("isSittingOut", s.isSittingOut)
            scoresArray.put(obj)
        }
        root.put("roundScores", scoresArray)

        val weddings = database.weddingDao().getAllWeddingsSync()
        val weddingsArray = JSONArray()
        weddings.forEach { w ->
            val obj = JSONObject()
            obj.put("id", w.id)
            obj.put("gameId", w.gameId)
            obj.put("roundId", w.roundId)
            obj.put("playerId", w.playerId)
            obj.put("bockMultiplier", w.bockMultiplier)
            obj.put("timestamp", w.timestamp)
            weddingsArray.put(obj)
        }
        root.put("weddingEvents", weddingsArray)

        return root.toString(2)
    }

    suspend fun importFromJson(database: AppDatabase, jsonString: String): Result<Int> {
        return runCatching {
            val root = JSONObject(jsonString)
            if (!root.has("players") || !root.has("games")) {
                throw IllegalArgumentException("Ungültiges Sicherungsformat: Fehlende Datenfelder.")
            }

            database.withTransaction {
                val playersArray = root.getJSONArray("players")
                for (i in 0 until playersArray.length()) {
                    val obj = playersArray.getJSONObject(i)
                    val player = Player(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        colorHex = obj.optLong("colorHex", 0xFF2563EBL),
                        avatarSymbol = obj.optString("avatarSymbol", "♠"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    database.playerDao().insertPlayer(player)
                }

                val gamesArray = root.getJSONArray("games")
                for (i in 0 until gamesArray.length()) {
                    val obj = gamesArray.getJSONObject(i)
                    val game = Game(
                        id = obj.getLong("id"),
                        title = obj.optString("title", "Importierte Partie"),
                        playerCount = obj.optInt("playerCount", 4),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        completedAt = if (obj.has("completedAt")) obj.getLong("completedAt") else null,
                        status = obj.optString("status", Game.STATUS_COMPLETED),
                        currentBockLevel = obj.optInt("currentBockLevel", 1),
                        remainingBockRounds = obj.optInt("remainingBockRounds", 0),
                        bockRoundsPerTrigger = obj.optInt("bockRoundsPerTrigger", 4),
                        activeBockSeries = obj.optString("activeBockSeries", ""),
                        notes = obj.optString("notes", "")
                    )
                    database.gameDao().insertGame(game)
                }

                if (root.has("gamePlayers")) {
                    val crossRefsArray = root.getJSONArray("gamePlayers")
                    val list = mutableListOf<GamePlayerCrossRef>()
                    for (i in 0 until crossRefsArray.length()) {
                        val obj = crossRefsArray.getJSONObject(i)
                        list.add(
                            GamePlayerCrossRef(
                                gameId = obj.getLong("gameId"),
                                playerId = obj.getLong("playerId"),
                                seatOrder = obj.optInt("seatOrder", i)
                            )
                        )
                    }
                    database.gameDao().insertGamePlayers(list)
                }

                if (root.has("gameRounds")) {
                    val roundsArray = root.getJSONArray("gameRounds")
                    for (i in 0 until roundsArray.length()) {
                        val obj = roundsArray.getJSONObject(i)
                        val round = GameRound(
                            id = obj.getLong("id"),
                            gameId = obj.getLong("gameId"),
                            roundNumber = obj.getInt("roundNumber"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            basePoints = obj.optInt("basePoints", 1),
                            bockLevel = obj.optInt("bockLevel", 1),
                            bockMultiplier = obj.optInt("bockMultiplier", 1),
                            gameType = obj.optString("gameType", "NORMAL"),
                            soloPlayerId = if (obj.has("soloPlayerId")) obj.getLong("soloPlayerId") else null,
                            hochzeitPlayerId = if (obj.has("hochzeitPlayerId")) obj.getLong("hochzeitPlayerId") else null,
                            sittingOutPlayerId = if (obj.has("sittingOutPlayerId")) obj.getLong("sittingOutPlayerId") else null,
                            note = obj.optString("note", ""),
                            triggeredBock = obj.optBoolean("triggeredBock", false),
                            bockTriggersCount = obj.optInt("bockTriggersCount", if (obj.optBoolean("triggeredBock", false)) 1 else 0),
                            previousBockLevel = obj.optInt("previousBockLevel", obj.optInt("bockLevel", 1)),
                            resultingBockLevel = obj.optInt("resultingBockLevel", obj.optInt("bockLevel", 1)),
                            resultingBockMultiplier = obj.optInt("resultingBockMultiplier", obj.optInt("bockMultiplier", 1)),
                            remainingBockRoundsAfter = obj.optInt("remainingBockRoundsAfter", 0)
                        )
                        database.roundDao().insertRound(round)
                    }
                }

                if (root.has("roundScores")) {
                    val scoresArray = root.getJSONArray("roundScores")
                    val list = mutableListOf<RoundScore>()
                    for (i in 0 until scoresArray.length()) {
                        val obj = scoresArray.getJSONObject(i)
                        list.add(
                            RoundScore(
                                id = obj.getLong("id"),
                                roundId = obj.getLong("roundId"),
                                gameId = obj.getLong("gameId"),
                                playerId = obj.getLong("playerId"),
                                rawPoints = obj.getInt("rawPoints"),
                                gamePoints = obj.optInt("gamePoints", 0),
                                weddingPoints = obj.optInt("weddingPoints", 0),
                                finalPoints = obj.getInt("finalPoints"),
                                isWinner = obj.optBoolean("isWinner", false),
                                isSittingOut = obj.optBoolean("isSittingOut", false)
                            )
                        )
                    }
                    database.roundDao().insertRoundScores(list)
                }

                if (root.has("weddingEvents")) {
                    val weddingsArray = root.getJSONArray("weddingEvents")
                    for (i in 0 until weddingsArray.length()) {
                        val obj = weddingsArray.getJSONObject(i)
                        val wedding = WeddingEvent(
                            id = obj.getLong("id"),
                            gameId = obj.getLong("gameId"),
                            roundId = obj.getLong("roundId"),
                            playerId = obj.getLong("playerId"),
                            bockMultiplier = obj.optInt("bockMultiplier", 1),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                        database.weddingDao().insertWedding(wedding)
                    }
                }

                gamesArray.length()
            }
        }
    }
}
