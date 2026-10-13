package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.local.AppDatabase
import com.example.data.model.BockCalculator
import com.example.data.model.Game
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
        assertEquals("Dreifachbock ausgelöst – Bockstufe von 2 auf 5", historyText)

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
}

