package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.local.AppDatabase
import com.example.data.model.BockCalculator
import com.example.data.model.Game
import com.example.data.model.GameType
import com.example.data.repository.DoppelkopfRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DoppelkopfFullTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: DoppelkopfRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DoppelkopfRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testBockMultiplierAndLevelCap() {
        // Stufe 1 bis 8 Formel: 2^(n-1)
        assertEquals(1, BockCalculator.getMultiplier(1))
        assertEquals(2, BockCalculator.getMultiplier(2))
        assertEquals(4, BockCalculator.getMultiplier(3))
        assertEquals(8, BockCalculator.getMultiplier(4))
        assertEquals(16, BockCalculator.getMultiplier(5))
        assertEquals(32, BockCalculator.getMultiplier(6))
        assertEquals(64, BockCalculator.getMultiplier(7))
        assertEquals(128, BockCalculator.getMultiplier(8))

        // Begrenzung auf maximal Stufe 8 (128x)
        assertEquals(8, BockCalculator.coerceLevel(9))
        assertEquals(8, BockCalculator.coerceLevel(15))
        assertEquals(128, BockCalculator.getMultiplier(9))
        assertEquals(128, BockCalculator.getMultiplier(15))
        assertEquals(8, BockCalculator.increaseLevel(8))

        // Minimum Stufe 1 (1x)
        assertEquals(1, BockCalculator.coerceLevel(0))
        assertEquals(1, BockCalculator.decreaseLevel(1))
    }

    @Test
    fun testFourPlayerGameCreationAndDuplicatePrevention() = runBlocking {
        val p1 = repository.insertPlayer("Alice")
        val p2 = repository.insertPlayer("Bob")
        val p3 = repository.insertPlayer("Charlie")
        val p4 = repository.insertPlayer("Diana")

        // Erfolgreiche Erstellung mit 4 Spielern
        val gameId = repository.createGame("Test-Partie", listOf(p1, p2, p3, p4))
        assertTrue(gameId > 0)

        val fullData = repository.getFullGameDataSync(gameId)
        assertNotNull(fullData)
        assertEquals(4, fullData?.players?.size)

        // Duplikaterkennung: Spieler darf nicht mehrfach teilnehmen
        var duplicateCaught = false
        try {
            repository.createGame("Duplikat", listOf(p1, p1, p3, p4))
        } catch (e: IllegalArgumentException) {
            duplicateCaught = true
        }
        assertTrue("Duplikate müssen verhindert werden", duplicateCaught)
    }

    @Test
    fun testFivePlayerGameAndSittingOutPlayer() = runBlocking {
        val p1 = repository.insertPlayer("P1")
        val p2 = repository.insertPlayer("P2")
        val p3 = repository.insertPlayer("P3")
        val p4 = repository.insertPlayer("P4")
        val p5 = repository.insertPlayer("P5")

        val gameId = repository.createGame("5er Runde", listOf(p1, p2, p3, p4, p5))
        assertEquals(5, repository.getFullGameDataSync(gameId)?.players?.size)

        // Runde 1: P5 setzt aus. Re: P1, P2 (+2). Kontra: P3, P4 (-2)
        val roundResult = repository.addRound(
            gameId = gameId,
            basePoints = 2,
            rawPlayerPoints = mapOf(p1 to 2, p2 to 2, p3 to -2, p4 to -2, p5 to 0),
            sittingOutPlayerId = p5
        )
        assertTrue(roundResult.isSuccess)

        val data = repository.getFullGameDataSync(gameId)
        assertNotNull(data)
        assertEquals(2, data?.playerTotals?.get(p1))
        assertEquals(2, data?.playerTotals?.get(p2))
        assertEquals(-2, data?.playerTotals?.get(p3))
        assertEquals(-2, data?.playerTotals?.get(p4))
        assertEquals(0, data?.playerTotals?.get(p5)) // Pausierender Spieler bleibt bei 0
    }

    @Test
    fun testBockCalculationAndWeddingTracking() = runBlocking {
        val p1 = repository.insertPlayer("Spieler 1")
        val p2 = repository.insertPlayer("Spieler 2")
        val p3 = repository.insertPlayer("Spieler 3")
        val p4 = repository.insertPlayer("Spieler 4")

        val gameId = repository.createGame("Bock & Hochzeit", listOf(p1, p2, p3, p4))

        // Setze Bockstufe manuell auf Stufe 3 (Multiplikator 4x)
        repository.setGameBockState(gameId, level = 3, remainingRounds = 2)

        // Runde mit Hochzeit für p1 und 4x Bockmultiplikator
        val res = repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1),
            hochzeitPlayerId = p1
        )
        assertTrue(res.isSuccess)

        val data = repository.getFullGameDataSync(gameId)
        // P1: 1 * 4x (4 Spielpunkte) + (4-1)*4x (12 Hochzeitspunkte) = 16 Punkte
        // P2: 1 * 4x (4 Spielpunkte) - 4x (4 Hochzeitspunkte Abzug) = 0 Punkte
        // P3: -1 * 4x (-4 Spielpunkte) - 4x (4 Hochzeitspunkte Abzug) = -8 Punkte
        // P4: -1 * 4x (-4 Spielpunkte) - 4x (4 Hochzeitspunkte Abzug) = -8 Punkte
        assertEquals(16, data?.playerTotals?.get(p1))
        assertEquals(0, data?.playerTotals?.get(p2))
        assertEquals(-8, data?.playerTotals?.get(p3))
        assertEquals(-8, data?.playerTotals?.get(p4))

        // Hochzeit Statistik prüfen
        val stats = repository.computeAllPlayerStatsSync()
        val p1Stats = stats.find { it.player.id == p1 }
        assertEquals(1, p1Stats?.weddingCount)
        assertEquals(3, p1Stats?.highestBockLevelPlayed)
    }

    @Test
    fun testRoundUpdateAndConsistentRecalculation() = runBlocking {
        val p1 = repository.insertPlayer("A")
        val p2 = repository.insertPlayer("B")
        val p3 = repository.insertPlayer("C")
        val p4 = repository.insertPlayer("D")

        val gameId = repository.createGame("Korrektur-Test", listOf(p1, p2, p3, p4))

        val roundId = repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1)
        ).getOrThrow()

        var data = repository.getFullGameDataSync(gameId)
        assertEquals(1, data?.playerTotals?.get(p1))

        // Runde nachträglich korrigieren auf 3 Punkte
        val updateRes = repository.updateRound(
            roundId = roundId,
            gameId = gameId,
            basePoints = 3,
            rawPlayerPoints = mapOf(p1 to 3, p2 to 3, p3 to -3, p4 to -3),
            bockLevel = 1
        )
        assertTrue(updateRes.isSuccess)

        data = repository.getFullGameDataSync(gameId)
        assertEquals(3, data?.playerTotals?.get(p1))
        assertEquals(3, data?.playerTotals?.get(p2))
        assertEquals(-3, data?.playerTotals?.get(p3))
        assertEquals(-3, data?.playerTotals?.get(p4))

        // Runde löschen
        repository.deleteRound(roundId)
        data = repository.getFullGameDataSync(gameId)
        assertEquals(0, data?.roundsWithScores?.size)
        assertEquals(0, data?.playerTotals?.get(p1))
    }

    @Test
    fun testPlayerRenamePreservesIdAndHistory() = runBlocking {
        val playerId = repository.insertPlayer("OldName")
        val p2 = repository.insertPlayer("P2")
        val p3 = repository.insertPlayer("P3")
        val p4 = repository.insertPlayer("P4")

        val gameId = repository.createGame("Rename-Test", listOf(playerId, p2, p3, p4))
        repository.addRound(
            gameId = gameId,
            basePoints = 2,
            rawPlayerPoints = mapOf(playerId to 2, p2 to 2, p3 to -2, p4 to -2)
        )

        // Namensänderung
        val player = repository.getPlayerByIdSync(playerId)
        assertNotNull(player)
        repository.updatePlayer(player!!.copy(name = "NewName"))

        // Prüfung: ID bleibt identisch, Punkte bleiben erhalten
        val updatedData = repository.getFullGameDataSync(gameId)
        val renamed = updatedData?.players?.find { it.id == playerId }
        assertEquals("NewName", renamed?.name)
        assertEquals(2, updatedData?.playerTotals?.get(playerId))
    }

    @Test
    fun testJsonBackupAndRestore() = runBlocking {
        val p1 = repository.insertPlayer("BackupPlayer1")
        val p2 = repository.insertPlayer("BackupPlayer2")
        val p3 = repository.insertPlayer("BackupPlayer3")
        val p4 = repository.insertPlayer("BackupPlayer4")

        val gameId = repository.createGame("Backup-Partie", listOf(p1, p2, p3, p4))
        repository.addRound(
            gameId = gameId,
            basePoints = 5,
            rawPlayerPoints = mapOf(p1 to 5, p2 to 5, p3 to -5, p4 to -5)
        )

        // Exportieren
        val json = BackupManager.exportToJson(database)
        assertTrue(json.contains("BackupPlayer1"))
        assertTrue(json.contains("Backup-Partie"))

        // Datenbank leeren (neue Instanz)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val freshDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Wiederherstellen
        val importResult = BackupManager.importFromJson(freshDb, json)
        assertTrue(importResult.isSuccess)
        assertEquals(1, importResult.getOrNull())

        // Überprüfen, ob Daten in der frischen DB vorhanden sind
        val freshRepo = DoppelkopfRepository(freshDb)
        val restoredData = freshRepo.getFullGameDataSync(gameId)
        assertNotNull(restoredData)
        assertEquals("Backup-Partie", restoredData?.game?.title)
        assertEquals(5, restoredData?.playerTotals?.get(p1))

        freshDb.close()
    }

    @Test
    fun testMultipleTriggersInSameRoundAndTransitions() = runBlocking {
        val p1 = repository.insertPlayer("P1")
        val p2 = repository.insertPlayer("P2")
        val p3 = repository.insertPlayer("P3")
        val p4 = repository.insertPlayer("P4")

        val gameId = repository.createGame("Multi-Trigger-Test", listOf(p1, p2, p3, p4))

        // Runde 1: Startet bei Stufe 1. Doppelbock (2 Auslöser) ausgelöst!
        val r1Id = repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1),
            bockTriggersCount = 2
        ).getOrThrow()

        val dataAfterR1 = repository.getFullGameDataSync(gameId)
        assertNotNull(dataAfterR1)
        val r1 = dataAfterR1!!.roundsWithScores.find { it.round.id == r1Id }!!.round
        assertEquals(1, r1.bockLevel)
        assertEquals(1, r1.bockMultiplier)
        assertEquals(2, r1.bockTriggersCount)
        assertEquals(1, r1.previousBockLevel)
        assertEquals(3, r1.resultingBockLevel)
        assertEquals(4, r1.resultingBockMultiplier)
        assertEquals(3, dataAfterR1.game.currentBockLevel)

        // Text-Formatierung prüfen
        val historyText = BockCalculator.formatTriggerHistoryText(3, 2, 5)
        assertEquals("Dreifachbock ausgelöst – von Einfacher Bock (×2) auf Vierfacher Bock (×16)", historyText)

        // Runde 2: Wird mit Stufe 3 (Multiplikator 4x) gespielt. Dreifachbock (3 Auslöser) kommt hinzu!
        val r2Id = repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1),
            bockTriggersCount = 3
        ).getOrThrow()

        val dataAfterR2 = repository.getFullGameDataSync(gameId)
        val r2 = dataAfterR2!!.roundsWithScores.find { it.round.id == r2Id }!!.round
        assertEquals(3, r2.bockLevel)
        assertEquals(4, r2.bockMultiplier)
        assertEquals(3, r2.bockTriggersCount)
        assertEquals(3, r2.previousBockLevel)
        assertEquals(6, r2.resultingBockLevel)
        assertEquals(32, r2.resultingBockMultiplier)
        assertEquals(6, dataAfterR2.game.currentBockLevel)
    }

    @Test
    fun testReachingLevel8AndExceedingLevel8Prevented() = runBlocking {
        val p1 = repository.insertPlayer("P1")
        val p2 = repository.insertPlayer("P2")
        val p3 = repository.insertPlayer("P3")
        val p4 = repository.insertPlayer("P4")

        val gameId = repository.createGame("Level8-Test", listOf(p1, p2, p3, p4))
        repository.setGameBockState(gameId, level = 6, remainingRounds = 4)

        // Stufe 6 + 2 Auslöser (Doppelbock) = Stufe 8 (128x) -> Erlaubt!
        val roundRes = repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1),
            bockTriggersCount = 2
        )
        assertTrue(roundRes.isSuccess)

        val gameData = repository.getFullGameDataSync(gameId)
        assertEquals(8, gameData?.game?.currentBockLevel)

        // Bei Stufe 8: Versuch, noch einen weiteren Auslöser hinzuzufügen -> Muss scheitern!
        val failRes = repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1),
            bockTriggersCount = 1
        )
        assertTrue(failRes.isFailure)
        assertTrue(failRes.exceptionOrNull() is IllegalArgumentException)
        assertTrue(failRes.exceptionOrNull()?.message?.contains("Maximale Bockstufe 8") == true)
    }

    @Test
    fun testSubsequentRecalculationOnRoundEditAndRoundDelete() = runBlocking {
        val p1 = repository.insertPlayer("A")
        val p2 = repository.insertPlayer("B")
        val p3 = repository.insertPlayer("C")
        val p4 = repository.insertPlayer("D")

        val gameId = repository.createGame("Recalc-Test", listOf(p1, p2, p3, p4))

        // Runde 1: Gespielt bei Stufe 1. 2 Auslöser -> Stufe 3 (4x)
        val r1Id = repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1),
            bockTriggersCount = 2
        ).getOrThrow()

        // Runde 2: Gespielt bei Stufe 3 (4x). 0 Auslöser
        val r2Id = repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1),
            bockTriggersCount = 0
        ).getOrThrow()

        // Runde 3: Gespielt bei Stufe 3 (4x). 0 Auslöser
        val r3Id = repository.addRound(
            gameId = gameId,
            basePoints = 2,
            rawPlayerPoints = mapOf(p1 to 2, p2 to 2, p3 to -2, p4 to -2),
            bockTriggersCount = 0
        ).getOrThrow()

        var data = repository.getFullGameDataSync(gameId)
        // Vor Korrektur:
        // R1: 1 * 1x = 1
        // R2: 1 * 4x = 4
        // R3: 2 * 4x = 8
        // Summe P1: 1 + 4 + 8 = 13
        assertEquals(13, data?.playerTotals?.get(p1))

        // Jetzt Runde 1 nachträglich bearbeiten: Nur 1 Auslöser (Einfacher Bock) statt 2!
        // Dadurch sinkt die Folgestufe von 3 (4x) auf 2 (2x)!
        val updateRes = repository.updateRound(
            roundId = r1Id,
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1),
            bockTriggersCount = 1
        )
        assertTrue(updateRes.isSuccess)

        // Überprüfen, ob Runde 2 und Runde 3 konsistent mit 2x neu berechnet wurden:
        // R1: 1 * 1x = 1
        // R2: 1 * 2x = 2
        // R3: 2 * 2x = 4
        // Neue Summe P1: 1 + 2 + 4 = 7!
        data = repository.getFullGameDataSync(gameId)
        assertEquals(7, data?.playerTotals?.get(p1))
        val r2Updated = data?.roundsWithScores?.find { it.round.id == r2Id }?.round
        assertEquals(2, r2Updated?.bockLevel)
        assertEquals(2, r2Updated?.bockMultiplier)

        // Jetzt Runde 1 komplett löschen:
        repository.deleteRound(r1Id)
        data = repository.getFullGameDataSync(gameId)
        assertEquals(2, data?.roundsWithScores?.size)

        // Die ehemalige Runde 2 ist jetzt Runde 1 und wird mit 1x gewertet (da kein vorheriger Bock mehr da ist)
        // R2 (neu R1): 1 * 1x = 1
        // R3 (neu R2): 2 * 1x = 2
        // Summe P1: 1 + 2 = 3
        assertEquals(3, data?.playerTotals?.get(p1))
    }

    @Test
    fun testWeddingsWithMultipleBockLevelsInFourAndFivePlayers() = runBlocking {
        // 4 Spieler Test
        val p1 = repository.insertPlayer("P1")
        val p2 = repository.insertPlayer("P2")
        val p3 = repository.insertPlayer("P3")
        val p4 = repository.insertPlayer("P4")

        val game4Id = repository.createGame("4er Hochzeit Bock", listOf(p1, p2, p3, p4))
        // Runde 1 mit 1 Auslöser -> Stufe 2 (2x)
        repository.addRound(
            gameId = game4Id,
            basePoints = 0,
            rawPlayerPoints = mapOf(p1 to 0, p2 to 0, p3 to 0, p4 to 0),
            bockTriggersCount = 1
        )

        // Runde 2 läuft mit 2x Bock: P1 hat Hochzeit!
        // P1 erhält (4-1)*2 = +6 Punkte, P2, P3, P4 je -2 Punkte
        repository.addRound(
            gameId = game4Id,
            basePoints = 0,
            rawPlayerPoints = mapOf(p1 to 0, p2 to 0, p3 to 0, p4 to 0),
            hochzeitPlayerId = p1
        )
        val data4 = repository.getFullGameDataSync(game4Id)
        assertEquals(6, data4?.playerTotals?.get(p1))
        assertEquals(-2, data4?.playerTotals?.get(p2))
        assertEquals(-2, data4?.playerTotals?.get(p3))
        assertEquals(-2, data4?.playerTotals?.get(p4))

        // 5 Spieler Test
        val p5 = repository.insertPlayer("P5")
        val game5Id = repository.createGame("5er Hochzeit Bock", listOf(p1, p2, p3, p4, p5))
        // Doppelbock (2 Auslöser) -> Stufe 3 (4x)
        repository.addRound(
            gameId = game5Id,
            basePoints = 0,
            rawPlayerPoints = mapOf(p1 to 0, p2 to 0, p3 to 0, p4 to 0, p5 to 0),
            sittingOutPlayerId = p5,
            bockTriggersCount = 2
        )

        // Runde 2 mit Stufe 3 (4x): P1 hat Hochzeit!
        // P1 erhält (5-1)*4 = +16 Punkte, alle anderen je -4 Punkte
        repository.addRound(
            gameId = game5Id,
            basePoints = 0,
            rawPlayerPoints = mapOf(p1 to 0, p2 to 0, p3 to 0, p4 to 0, p5 to 0),
            sittingOutPlayerId = p5,
            hochzeitPlayerId = p1
        )
        val data5 = repository.getFullGameDataSync(game5Id)
        assertEquals(16, data5?.playerTotals?.get(p1))
        assertEquals(-4, data5?.playerTotals?.get(p2))
        assertEquals(-4, data5?.playerTotals?.get(p3))
        assertEquals(-4, data5?.playerTotals?.get(p4))
        assertEquals(-4, data5?.playerTotals?.get(p5))
    }

    @Test
    fun testSoloGameFourPlayers() = runBlocking {
        val p1 = repository.insertPlayer("Soloist")
        val p2 = repository.insertPlayer("Opponent1")
        val p3 = repository.insertPlayer("Opponent2")
        val p4 = repository.insertPlayer("Opponent3")

        val gameId = repository.createGame("4er Solo Partie", listOf(p1, p2, p3, p4))

        // Solospiel: P1 gewinnt (Solo gewinnt gegen 3 Gegenspieler)
        // P1 erhält +3 Punkte, P2, P3, P4 erhalten je -1 Punkt
        val resultRoundId = repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 3, p2 to -1, p3 to -1, p4 to -1),
            gameType = GameType.SOLO.name,
            soloPlayerId = p1
        ).getOrThrow()

        val gameData = repository.getFullGameDataSync(gameId)
        assertNotNull(gameData)
        assertEquals(1, gameData?.roundCount)
        assertEquals(1, gameData?.soloRoundCount)
        assertEquals(0, gameData?.normalRoundCount)

        val round = gameData?.roundsWithScores?.first()?.round
        assertNotNull(round)
        assertEquals(GameType.SOLO.name, round?.gameType)
        assertEquals(p1, round?.soloPlayerId)
        assertTrue(round?.isSolo == true)

        assertEquals(3, gameData?.playerTotals?.get(p1))
        assertEquals(-1, gameData?.playerTotals?.get(p2))
        assertEquals(-1, gameData?.playerTotals?.get(p3))
        assertEquals(-1, gameData?.playerTotals?.get(p4))
    }

    @Test
    fun testSoloGameFivePlayers() = runBlocking {
        val p1 = repository.insertPlayer("S1")
        val p2 = repository.insertPlayer("S2")
        val p3 = repository.insertPlayer("S3")
        val p4 = repository.insertPlayer("S4")
        val p5 = repository.insertPlayer("S5")

        val gameId = repository.createGame("5er Solo Partie", listOf(p1, p2, p3, p4, p5))

        // P5 sitzt aus (Geber). P1 spielt Solo gegen P2, P3, P4.
        // Solo verliert: P1 erhält -3 Punkte, P2, P3, P4 erhalten je +1 Punkt, P5 erhält 0 Punkte
        repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to -3, p2 to 1, p3 to 1, p4 to 1, p5 to 0),
            sittingOutPlayerId = p5,
            gameType = GameType.SOLO.name,
            soloPlayerId = p1
        )

        val gameData = repository.getFullGameDataSync(gameId)
        assertEquals(-3, gameData?.playerTotals?.get(p1))
        assertEquals(1, gameData?.playerTotals?.get(p2))
        assertEquals(1, gameData?.playerTotals?.get(p3))
        assertEquals(1, gameData?.playerTotals?.get(p4))
        assertEquals(0, gameData?.playerTotals?.get(p5))
    }

    @Test
    fun testSelectAndSwitchSoloPlayer() = runBlocking {
        val p1 = repository.insertPlayer("Alice")
        val p2 = repository.insertPlayer("Bob")
        val p3 = repository.insertPlayer("Charlie")
        val p4 = repository.insertPlayer("Dave")

        val gameId = repository.createGame("Solo Wechsel Test", listOf(p1, p2, p3, p4))

        // Zuerst Runde mit P1 als Solospieler buchen
        val roundId = repository.addRound(
            gameId = gameId,
            basePoints = 2,
            rawPlayerPoints = mapOf(p1 to 6, p2 to -2, p3 to -2, p4 to -2),
            gameType = GameType.SOLO.name,
            soloPlayerId = p1
        ).getOrThrow()

        var gameData = repository.getFullGameDataSync(gameId)
        assertEquals(p1, gameData?.roundsWithScores?.first()?.round?.soloPlayerId)

        // Nachträgliches Wechseln des Solospielers auf P2
        repository.updateRound(
            roundId = roundId,
            gameId = gameId,
            basePoints = 2,
            rawPlayerPoints = mapOf(p2 to 6, p1 to -2, p3 to -2, p4 to -2),
            gameType = GameType.SOLO.name,
            soloPlayerId = p2
        )

        gameData = repository.getFullGameDataSync(gameId)
        val updatedRound = gameData?.roundsWithScores?.first()?.round
        assertEquals(p2, updatedRound?.soloPlayerId)
        assertEquals(-2, gameData?.playerTotals?.get(p1))
        assertEquals(6, gameData?.playerTotals?.get(p2))
        assertEquals(-2, gameData?.playerTotals?.get(p3))
        assertEquals(-2, gameData?.playerTotals?.get(p4))
    }

    @Test
    fun testManualScoreInputSoloGame() = runBlocking {
        val p1 = repository.insertPlayer("M1")
        val p2 = repository.insertPlayer("M2")
        val p3 = repository.insertPlayer("M3")
        val p4 = repository.insertPlayer("M4")

        val gameId = repository.createGame("Manuelle Punkte Solo", listOf(p1, p2, p3, p4))

        // Individuelle Punktvergabe (z.B. spezielle lokale Spielpunkte)
        repository.addRound(
            gameId = gameId,
            basePoints = 0,
            rawPlayerPoints = mapOf(p1 to 9, p2 to -3, p3 to -3, p4 to -3),
            gameType = GameType.SOLO.name,
            soloPlayerId = p1
        )

        val gameData = repository.getFullGameDataSync(gameId)
        assertEquals(9, gameData?.playerTotals?.get(p1))
        assertEquals(-3, gameData?.playerTotals?.get(p2))
        assertEquals(-3, gameData?.playerTotals?.get(p3))
        assertEquals(-3, gameData?.playerTotals?.get(p4))
    }

    @Test
    fun testBockMultiplierAppliedToSoloGame() = runBlocking {
        val p1 = repository.insertPlayer("B1")
        val p2 = repository.insertPlayer("B2")
        val p3 = repository.insertPlayer("B3")
        val p4 = repository.insertPlayer("B4")

        val gameId = repository.createGame("Bock Multiplikator Solo", listOf(p1, p2, p3, p4))

        // Runde 1: Normalspiel löst Doppelbock aus (+2 Auslöser) -> Bockstufe 3 (4x)
        repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1),
            bockTriggersCount = 2
        )

        // Runde 2: Solospiel läuft nun unter Bockstufe 3 (4x Multiplikator)!
        // Rohpunkte: P1 gewinnt mit +3, Gegenspieler je -1
        // Endpunkte nach 4x Bock: P1 erhält +12 Punkte, Gegenspieler erhalten je -4 Punkte
        repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 3, p2 to -1, p3 to -1, p4 to -1),
            gameType = GameType.SOLO.name,
            soloPlayerId = p1
        )

        val gameData = repository.getFullGameDataSync(gameId)
        val round2 = gameData?.roundsWithScores?.find { it.round.roundNumber == 2 }
        assertNotNull(round2)
        assertEquals(3, round2?.round?.bockLevel)
        assertEquals(4, round2?.round?.bockMultiplier)
        assertTrue(round2?.round?.isSolo == true)

        val p1Round2Score = round2?.scores?.find { it.playerId == p1 }?.finalPoints
        val p2Round2Score = round2?.scores?.find { it.playerId == p2 }?.finalPoints
        assertEquals(12, p1Round2Score)
        assertEquals(-4, p2Round2Score)
    }

    @Test
    fun testSoloStatisticsUpdate() = runBlocking {
        val p1 = repository.insertPlayer("StatP1")
        val p2 = repository.insertPlayer("StatP2")
        val p3 = repository.insertPlayer("StatP3")
        val p4 = repository.insertPlayer("StatP4")

        val gameId = repository.createGame("Statistik Test Game", listOf(p1, p2, p3, p4))

        // Runde 1: Normales Spiel (P1 und P2 gewinnen Re)
        repository.addRound(
            gameId = gameId,
            basePoints = 2,
            rawPlayerPoints = mapOf(p1 to 2, p2 to 2, p3 to -2, p4 to -2),
            gameType = GameType.NORMAL.name
        )

        // Runde 2: Solospiel von P1 (P1 gewinnt +6, P2, P3, P4 je -2)
        repository.addRound(
            gameId = gameId,
            basePoints = 2,
            rawPlayerPoints = mapOf(p1 to 6, p2 to -2, p3 to -2, p4 to -2),
            gameType = GameType.SOLO.name,
            soloPlayerId = p1
        )

        // Runde 3: Solospiel von P2 (P2 verliert: P2 -6, P1, P3, P4 je +2)
        repository.addRound(
            gameId = gameId,
            basePoints = 2,
            rawPlayerPoints = mapOf(p2 to -6, p1 to 2, p3 to 2, p4 to 2),
            gameType = GameType.SOLO.name,
            soloPlayerId = p2
        )

        val allStats = repository.computeAllPlayerStatsSync()
        val s1 = allStats.find { it.player.id == p1 }
        val s2 = allStats.find { it.player.id == p2 }

        assertNotNull(s1)
        assertNotNull(s2)

        // P1 Auswertung:
        // Normalspiele: 1 Runde gespielt, +2 Punkte
        assertEquals(1, s1?.normalRoundsPlayed)
        assertEquals(2, s1?.normalPoints)
        // Als Solospieler: 1 Runde gespielt, 1 Sieg, +6 Punkte
        assertEquals(1, s1?.soloRoundsAsSoloist)
        assertEquals(1, s1?.soloWinsAsSoloist)
        assertEquals(6, s1?.soloPointsAsSoloist)
        // Als Gegenspieler: 1 Runde gegen P2's Solo, 1 Sieg (da P2 verlor und P1 +2 bekam), +2 Punkte
        assertEquals(1, s1?.soloRoundsAsOpponent)
        assertEquals(1, s1?.soloWinsAsOpponent)
        assertEquals(2, s1?.soloPointsAsOpponent)

        // P2 Auswertung:
        // Normalspiele: 1 Runde gespielt, +2 Punkte
        assertEquals(1, s2?.normalRoundsPlayed)
        assertEquals(2, s2?.normalPoints)
        // Als Solospieler: 1 Runde gespielt, 0 Siege, -6 Punkte
        assertEquals(1, s2?.soloRoundsAsSoloist)
        assertEquals(0, s2?.soloWinsAsSoloist)
        assertEquals(-6, s2?.soloPointsAsSoloist)
        // Als Gegenspieler: 1 Runde gegen P1's Solo, 0 Siege, -2 Punkte
        assertEquals(1, s2?.soloRoundsAsOpponent)
        assertEquals(0, s2?.soloWinsAsOpponent)
        assertEquals(-2, s2?.soloPointsAsOpponent)
    }

    @Test
    fun testEditAndDeleteSoloRound() = runBlocking {
        val p1 = repository.insertPlayer("E1")
        val p2 = repository.insertPlayer("E2")
        val p3 = repository.insertPlayer("E3")
        val p4 = repository.insertPlayer("E4")

        val gameId = repository.createGame("Edit Delete Solo", listOf(p1, p2, p3, p4))

        // Runde 1: Solo P1 gewinnt (+3)
        val r1 = repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 3, p2 to -1, p3 to -1, p4 to -1),
            gameType = GameType.SOLO.name,
            soloPlayerId = p1
        ).getOrThrow()

        // Runde 2: Normales Spiel P1 & P2 gewinnen (+2)
        repository.addRound(
            gameId = gameId,
            basePoints = 2,
            rawPlayerPoints = mapOf(p1 to 2, p2 to 2, p3 to -2, p4 to -2),
            gameType = GameType.NORMAL.name
        )

        var data = repository.getFullGameDataSync(gameId)
        assertEquals(5, data?.playerTotals?.get(p1))
        assertEquals(1, data?.playerTotals?.get(p2))

        // Runde 1 bearbeiten: Punkte korrigieren auf Solo verliert (-3)
        repository.updateRound(
            roundId = r1,
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to -3, p2 to 1, p3 to 1, p4 to 1),
            gameType = GameType.SOLO.name,
            soloPlayerId = p1
        )

        data = repository.getFullGameDataSync(gameId)
        assertEquals(-1, data?.playerTotals?.get(p1)) // -3 + 2 = -1
        assertEquals(3, data?.playerTotals?.get(p2))  // 1 + 2 = 3

        // Runde 1 löschen: nur noch Runde 2 übrig
        repository.deleteRound(r1)
        data = repository.getFullGameDataSync(gameId)
        assertEquals(1, data?.roundCount)
        assertEquals(2, data?.playerTotals?.get(p1))
        assertEquals(2, data?.playerTotals?.get(p2))
    }

    @Test
    fun testBockCalculatorUnitLogic() {
        // Dauer je Spieleranzahl
        assertEquals(4, BockCalculator.getDurationForPlayerCount(4))
        assertEquals(5, BockCalculator.getDurationForPlayerCount(5))

        // Level und Multiplikatoren aus Anzahl aktiver Folgen
        assertEquals(1, BockCalculator.calculateLevelFromActiveSeriesCount(0))
        assertEquals(1, BockCalculator.getMultiplierForActiveSeriesCount(0)) // 1x

        assertEquals(2, BockCalculator.calculateLevelFromActiveSeriesCount(1))
        assertEquals(2, BockCalculator.getMultiplierForActiveSeriesCount(1)) // 2x

        assertEquals(3, BockCalculator.calculateLevelFromActiveSeriesCount(2))
        assertEquals(4, BockCalculator.getMultiplierForActiveSeriesCount(2)) // 4x

        assertEquals(4, BockCalculator.calculateLevelFromActiveSeriesCount(3))
        assertEquals(8, BockCalculator.getMultiplierForActiveSeriesCount(3)) // 8x

        assertEquals(8, BockCalculator.calculateLevelFromActiveSeriesCount(7))
        assertEquals(128, BockCalculator.getMultiplierForActiveSeriesCount(7)) // 128x

        // Capping bei Stufe 8 (128x)
        assertEquals(8, BockCalculator.calculateLevelFromActiveSeriesCount(8))
        assertEquals(128, BockCalculator.getMultiplierForActiveSeriesCount(8))
        assertEquals(8, BockCalculator.calculateLevelFromActiveSeriesCount(15))
        assertEquals(128, BockCalculator.getMultiplierForActiveSeriesCount(15))

        // Serialization & Parsing
        assertEquals(listOf(4, 2), BockCalculator.parseActiveSeries("4,2"))
        assertEquals("4,2", BockCalculator.serializeActiveSeries(listOf(4, 2)))
        assertEquals(emptyList<Int>(), BockCalculator.parseActiveSeries(""))
        assertEquals("", BockCalculator.serializeActiveSeries(emptyList()))

        // advanceActiveSeries: Dekrementierung, Entfernung abgelaufener Folgen und neue Auslösungen
        // Start: [2, 1], 1 neuer Auslöser mit Dauer 4
        // [2-1, 1-1] -> [1] (1-1=0 abgelaufen und entfernt) + [4] -> [1, 4]
        val advanced = BockCalculator.advanceActiveSeries(listOf(2, 1), newTriggers = 1, durationPerSeries = 4)
        assertEquals(listOf(1, 4), advanced)
    }

    @Test
    fun testOverlappingBockSeriesFourPlayers() = runBlocking {
        val p1 = repository.insertPlayer("A")
        val p2 = repository.insertPlayer("B")
        val p3 = repository.insertPlayer("C")
        val p4 = repository.insertPlayer("D")

        val gameId = repository.createGame("4er Überlappende Böcke", listOf(p1, p2, p3, p4))

        // Runde 1: Normal (1x). 1 Bockauslöser! (Dauer 4 Runden)
        val r1 = repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1),
            bockTriggersCount = 1
        ).getOrThrow()

        var data = repository.getFullGameDataSync(gameId)
        val round1 = data?.roundsWithScores?.find { it.round.id == r1 }?.round
        assertEquals(1, round1?.bockLevel)
        assertEquals(1, round1?.bockMultiplier)
        assertEquals(2, round1?.resultingBockLevel)
        assertEquals(2, round1?.resultingBockMultiplier)
        assertEquals(4, round1?.remainingBockRoundsAfter)
        assertEquals(2, data?.game?.currentBockLevel)
        assertEquals(4, data?.game?.remainingBockRounds)
        assertEquals("4", data?.game?.activeBockSeries)

        // Runde 2: Gespielt mit Stufe 2 (2x). 0 Auslöser.
        // Folge 1 Restlaufzeit sinkt von 4 auf 3.
        val r2 = repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1),
            bockTriggersCount = 0
        ).getOrThrow()

        data = repository.getFullGameDataSync(gameId)
        val round2 = data?.roundsWithScores?.find { it.round.id == r2 }?.round
        assertEquals(2, round2?.bockLevel)
        assertEquals(2, round2?.bockMultiplier)
        assertEquals(2, round2?.resultingBockLevel)
        assertEquals(2, round2?.resultingBockMultiplier)
        assertEquals(3, round2?.remainingBockRoundsAfter)
        assertEquals("3", data?.game?.activeBockSeries)

        // Runde 3: Gespielt mit Stufe 2 (2x). Weiterer Bock ausgelöst! (Überlappung!)
        // Folge 1 sinkt von 3 auf 2. Neue Folge 2 startet mit 4. Aktive Folgen: [2, 4].
        // Ab nächster Runde: 2 aktive Folgen -> Stufe 3 (4x Multiplikator)!
        val r3 = repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1),
            bockTriggersCount = 1
        ).getOrThrow()

        data = repository.getFullGameDataSync(gameId)
        val round3 = data?.roundsWithScores?.find { it.round.id == r3 }?.round
        assertEquals(2, round3?.bockLevel)
        assertEquals(2, round3?.bockMultiplier)
        assertEquals(3, round3?.resultingBockLevel)
        assertEquals(4, round3?.resultingBockMultiplier)
        assertEquals(4, round3?.remainingBockRoundsAfter)
        assertEquals(3, data?.game?.currentBockLevel)
        assertEquals("2,4", data?.game?.activeBockSeries)

        // Runde 4: Gespielt mit Stufe 3 (4x Multiplikator!). 0 Auslöser.
        // Folgen sinken auf [1, 3].
        val r4 = repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1),
            bockTriggersCount = 0
        ).getOrThrow()

        data = repository.getFullGameDataSync(gameId)
        val round4 = data?.roundsWithScores?.find { it.round.id == r4 }?.round
        assertEquals(3, round4?.bockLevel)
        assertEquals(4, round4?.bockMultiplier)
        assertEquals(3, round4?.resultingBockLevel)
        assertEquals(4, round4?.resultingBockMultiplier)
        assertEquals(3, round4?.remainingBockRoundsAfter)
        assertEquals("1,3", data?.game?.activeBockSeries)

        // Runde 5: Gespielt mit Stufe 3 (4x Multiplikator!). 0 Auslöser.
        // Folge 1 läuft nach dieser Runde ab (1 - 1 = 0). Folge 2 sinkt auf 2 -> [2].
        // Ab nächster Runde: Nur noch 1 aktive Folge -> Stufe 2 (2x Multiplikator)!
        val r5 = repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1),
            bockTriggersCount = 0
        ).getOrThrow()

        data = repository.getFullGameDataSync(gameId)
        val round5 = data?.roundsWithScores?.find { it.round.id == r5 }?.round
        assertEquals(3, round5?.bockLevel)
        assertEquals(4, round5?.bockMultiplier)
        assertEquals(2, round5?.resultingBockLevel) // Sinkt wieder auf Stufe 2!
        assertEquals(2, round5?.resultingBockMultiplier) // Multiplikator 2x ab nächster Runde!
        assertEquals(2, round5?.remainingBockRoundsAfter)
        assertEquals(2, data?.game?.currentBockLevel)
        assertEquals("2", data?.game?.activeBockSeries)

        // Runde 6: Gespielt mit Stufe 2 (2x). 0 Auslöser.
        // Folge 2 sinkt auf 1 -> [1].
        val r6 = repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1),
            bockTriggersCount = 0
        ).getOrThrow()

        data = repository.getFullGameDataSync(gameId)
        val round6 = data?.roundsWithScores?.find { it.round.id == r6 }?.round
        assertEquals(2, round6?.bockLevel)
        assertEquals(2, round6?.bockMultiplier)
        assertEquals("1", data?.game?.activeBockSeries)

        // Runde 7: Gespielt mit Stufe 2 (2x). 0 Auslöser.
        // Folge 2 läuft nach dieser Runde ab (1 - 1 = 0) -> [].
        // Ab nächster Runde: Keine Bockfolgen mehr aktiv -> Stufe 1 (1x)!
        val r7 = repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1),
            bockTriggersCount = 0
        ).getOrThrow()

        data = repository.getFullGameDataSync(gameId)
        val round7 = data?.roundsWithScores?.find { it.round.id == r7 }?.round
        assertEquals(2, round7?.bockLevel)
        assertEquals(2, round7?.bockMultiplier)
        assertEquals(1, round7?.resultingBockLevel) // Zurück auf Normalspiel!
        assertEquals(1, round7?.resultingBockMultiplier)
        assertEquals(0, round7?.remainingBockRoundsAfter)
        assertEquals(1, data?.game?.currentBockLevel)
        assertEquals(0, data?.game?.remainingBockRounds)
        assertEquals("", data?.game?.activeBockSeries)

        // Runde 8: Gespielt mit Normalstufe 1 (1x).
        val r8 = repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1),
            bockTriggersCount = 0
        ).getOrThrow()

        data = repository.getFullGameDataSync(gameId)
        val round8 = data?.roundsWithScores?.find { it.round.id == r8 }?.round
        assertEquals(1, round8?.bockLevel)
        assertEquals(1, round8?.bockMultiplier)

        // Punkteprüfung für Spieler A über alle 8 Runden:
        // R1: 1 * 1x = 1
        // R2: 1 * 2x = 2
        // R3: 1 * 2x = 2
        // R4: 1 * 4x = 4
        // R5: 1 * 4x = 4
        // R6: 1 * 2x = 2
        // R7: 1 * 2x = 2
        // R8: 1 * 1x = 1
        // Summe: 1 + 2 + 2 + 4 + 4 + 2 + 2 + 1 = 18 Punkte!
        assertEquals(18, data?.playerTotals?.get(p1))
        assertEquals(18, data?.playerTotals?.get(p2))
        assertEquals(-18, data?.playerTotals?.get(p3))
        assertEquals(-18, data?.playerTotals?.get(p4))
    }

    @Test
    fun testOverlappingBockSeriesFivePlayers() = runBlocking {
        val p1 = repository.insertPlayer("P1")
        val p2 = repository.insertPlayer("P2")
        val p3 = repository.insertPlayer("P3")
        val p4 = repository.insertPlayer("P4")
        val p5 = repository.insertPlayer("P5")

        val gameId = repository.createGame("5er Überlappende Böcke", listOf(p1, p2, p3, p4, p5))

        // In einer 5-Spieler-Partie muss jede Bockfolge genau 5 Spielrunden dauern!
        val game = repository.getGameByIdSync(gameId)
        assertEquals(5, game?.playerCount)
        assertEquals(5, game?.bockRoundsPerTrigger)

        // Runde 1: Normalspiel (1x). 1 Bockauslöser (Dauer 5 Runden). P5 sitzt aus.
        repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1, p5 to 0),
            sittingOutPlayerId = p5,
            bockTriggersCount = 1
        )

        var data = repository.getFullGameDataSync(gameId)
        assertEquals(2, data?.game?.currentBockLevel)
        assertEquals(5, data?.game?.remainingBockRounds)
        assertEquals("5", data?.game?.activeBockSeries)

        // Runde 2: Stufe 2 (2x). Weiterer Bockauslöser! P1 sitzt aus.
        // Folge 1 sinkt von 5 auf 4. Folge 2 startet mit 5. Aktive Folgen: [4, 5].
        // 2 aktive Folgen -> Stufe 3 (4x Multiplikator ab nächster Runde).
        repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 0, p2 to 1, p3 to 1, p4 to -1, p5 to -1),
            sittingOutPlayerId = p1,
            bockTriggersCount = 1
        )

        data = repository.getFullGameDataSync(gameId)
        assertEquals(3, data?.game?.currentBockLevel)
        assertEquals(5, data?.game?.remainingBockRounds) // max(4, 5) = 5
        assertEquals("4,5", data?.game?.activeBockSeries)

        // Runden 3, 4, 5, 6 laufen alle mit 4x (Stufe 3):
        // Nach R3: [3, 4] -> Stufe 3
        // Nach R4: [2, 3] -> Stufe 3
        // Nach R5: [1, 2] -> Stufe 3
        // Nach R6: Folge 1 läuft nach 5 Runden ab (1 - 1 = 0)! Folge 2 hat noch 1 Runde übrig -> [1] -> Stufe 2 (2x)
        for (i in 3..6) {
            val sitting = when (i) {
                3 -> p2
                4 -> p3
                5 -> p4
                else -> p5
            }
            repository.addRound(
                gameId = gameId,
                basePoints = 1,
                rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1, p5 to 0),
                sittingOutPlayerId = sitting,
                bockTriggersCount = 0
            )
        }

        data = repository.getFullGameDataSync(gameId)
        // Nach Runde 6 ist Folge 1 (die in Runde 1 ausgelöst wurde und in R2, R3, R4, R5, R6 aktiv war = genau 5 Runden) abgelaufen!
        // Nur Folge 2 ist noch für 1 Runde aktiv -> Stufe 2 (2x)!
        assertEquals(2, data?.game?.currentBockLevel)
        assertEquals(1, data?.game?.remainingBockRounds)
        assertEquals("1", data?.game?.activeBockSeries)

        // Runde 7: Läuft mit Stufe 2 (2x). Nach dieser Runde läuft auch Folge 2 ab.
        repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1, p5 to 0),
            sittingOutPlayerId = p1,
            bockTriggersCount = 0
        )

        data = repository.getFullGameDataSync(gameId)
        assertEquals(1, data?.game?.currentBockLevel)
        assertEquals(0, data?.game?.remainingBockRounds)
        assertEquals("", data?.game?.activeBockSeries)

        // Runde 8: Wieder Normalspiel (1x).
        repository.addRound(
            gameId = gameId,
            basePoints = 1,
            rawPlayerPoints = mapOf(p1 to 1, p2 to 1, p3 to -1, p4 to -1, p5 to 0),
            sittingOutPlayerId = p2,
            bockTriggersCount = 0
        )

        data = repository.getFullGameDataSync(gameId)
        val round8 = data?.roundsWithScores?.last()?.round
        assertEquals(1, round8?.bockLevel)
        assertEquals(1, round8?.bockMultiplier)
    }

    @Test
    fun testBockTerminologyAndNaming() {
        // 0 Bockstufen = Normales Spiel, Multiplikator ×1
        // 1 Bockstufe = Einfacher Bock, Multiplikator ×2
        // 2 Bockstufen = Doppelter Bock, Multiplikator ×4
        // 3 Bockstufen = Dreifacher Bock, Multiplikator ×8
        // 4 Bockstufen = Vierfacher Bock, Multiplikator ×16
        // 5 Bockstufen = Fünffacher Bock, Multiplikator ×32
        // 6 Bockstufen = Sechsfacher Bock, Multiplikator ×64
        // 7 Bockstufen = Siebenfacher Bock, Multiplikator ×128
        // 8 Bockstufen = Achtfacher Bock, Multiplikator ×256

        assertEquals("Normales Spiel", BockCalculator.getBockName(1))
        assertEquals("Einfacher Bock", BockCalculator.getBockName(2))
        assertEquals("Doppelter Bock", BockCalculator.getBockName(4))
        assertEquals("Dreifacher Bock", BockCalculator.getBockName(8))
        assertEquals("Vierfacher Bock", BockCalculator.getBockName(16))
        assertEquals("Fünffacher Bock", BockCalculator.getBockName(32))
        assertEquals("Sechsfacher Bock", BockCalculator.getBockName(64))
        assertEquals("Siebenfacher Bock", BockCalculator.getBockName(128))
        assertEquals("Achtfacher Bock", BockCalculator.getBockName(256))

        assertEquals("Normales Spiel (×1)", BockCalculator.formatBockWithMultiplier(1))
        assertEquals("Einfacher Bock (×2)", BockCalculator.formatBockWithMultiplier(2))
        assertEquals("Doppelter Bock (×4)", BockCalculator.formatBockWithMultiplier(4))
        assertEquals("Dreifacher Bock (×8)", BockCalculator.formatBockWithMultiplier(8))
        assertEquals("Vierfacher Bock (×16)", BockCalculator.formatBockWithMultiplier(16))
        assertEquals("Fünffacher Bock (×32)", BockCalculator.formatBockWithMultiplier(32))
        assertEquals("Sechsfacher Bock (×64)", BockCalculator.formatBockWithMultiplier(64))
        assertEquals("Siebenfacher Bock (×128)", BockCalculator.formatBockWithMultiplier(128))
        assertEquals("Achtfacher Bock (×256)", BockCalculator.formatBockWithMultiplier(256))

        // Stufen direkt prüfen
        assertEquals("Normales Spiel", BockCalculator.getBockNameForStufen(0))
        assertEquals("Einfacher Bock", BockCalculator.getBockNameForStufen(1))
        assertEquals("Doppelter Bock", BockCalculator.getBockNameForStufen(2))
        assertEquals("Dreifacher Bock", BockCalculator.getBockNameForStufen(3))
        assertEquals("Vierfacher Bock", BockCalculator.getBockNameForStufen(4))
        assertEquals("Fünffacher Bock", BockCalculator.getBockNameForStufen(5))
        assertEquals("Sechsfacher Bock", BockCalculator.getBockNameForStufen(6))
        assertEquals("Siebenfacher Bock", BockCalculator.getBockNameForStufen(7))
        assertEquals("Achtfacher Bock", BockCalculator.getBockNameForStufen(8))
    }

    @Test
    fun testCalculateOutstandingBockRounds() {
        // Fall 1: 3 Runden Doppelbock (×4) und 1 Runde einfacher Bock (×2)
        // Entsteht z.B. bei zwei aktiven Folgen mit Restlaufzeiten 4 und 3
        val res1 = BockCalculator.calculateOutstandingBockRounds(listOf(4, 3))
        assertEquals(2, res1.size)
        assertEquals(3, res1[0].roundCount)
        assertEquals(4, res1[0].multiplier)
        assertEquals("3 Runden – Doppelter Bock (×4)", res1[0].displayName)

        assertEquals(1, res1[1].roundCount)
        assertEquals(2, res1[1].multiplier)
        assertEquals("1 Runde – Einfacher Bock (×2)", res1[1].displayName)

        // Auch mit umgekehrter Reihenfolge listOf(3, 4)
        val res1b = BockCalculator.calculateOutstandingBockRounds(listOf(3, 4))
        assertEquals(2, res1b.size)
        assertEquals("3 Runden – Doppelter Bock (×4)", res1b[0].displayName)
        assertEquals("1 Runde – Einfacher Bock (×2)", res1b[1].displayName)

        // Fall 2: Keine ausstehenden Bockrunden
        val resEmpty = BockCalculator.calculateOutstandingBockRounds(emptyList())
        assertTrue(resEmpty.isEmpty())

        val resEmptyString = BockCalculator.calculateOutstandingBockRounds("", currentBockLevel = 1, remainingBockRounds = 0)
        assertTrue(resEmptyString.isEmpty())

        // Fall 3: Nur eine aktive Folge mit 4 Runden (Einfacher Bock x2)
        val resSingle = BockCalculator.calculateOutstandingBockRounds(listOf(4))
        assertEquals(1, resSingle.size)
        assertEquals(4, resSingle[0].roundCount)
        assertEquals(2, resSingle[0].multiplier)
        assertEquals("4 Runden – Einfacher Bock (×2)", resSingle[0].displayName)

        // Fall 4: Drei überlappende Folgen z.B. listOf(5, 4, 2)
        // Runde 1 & 2: 3 Folgen aktiv -> Multiplikator 8 (2 Runden Dreifacher Bock)
        // Runde 3 & 4: 2 Folgen aktiv -> Multiplikator 4 (2 Runden Doppelter Bock)
        // Runde 5: 1 Folge aktiv -> Multiplikator 2 (1 Runde Einfacher Bock)
        val resTriple = BockCalculator.calculateOutstandingBockRounds(listOf(5, 4, 2))
        assertEquals(3, resTriple.size)
        assertEquals("2 Runden – Dreifacher Bock (×8)", resTriple[0].displayName)
        assertEquals("2 Runden – Doppelter Bock (×4)", resTriple[1].displayName)
        assertEquals("1 Runde – Einfacher Bock (×2)", resTriple[2].displayName)
    }
}


