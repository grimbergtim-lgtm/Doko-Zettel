package com.example.data.model

import kotlin.random.Random

/**
 * Modell und Standardaufgaben für den „AIDA Extreme Mode“.
 */
object AidaExtremeMode {
    val DEFAULT_TASKS: List<String> = listOf(
        "Du hast sehr lange Augenkontakt mit der Kellnerin gehabt. Bestelle eine neue Runde.",
        "Dir sind scheinbar Karten heruntergefallen. Strafschluck deiner Wahl!",
        "Die Dame am Nebentisch hat dich freundlich angelächelt. Wenn sie es möchte, lade sie auf einen Drink ein.",
        "Du hast gerade so selbstbewusst gespielt, dass du jetzt eine Runde bestellen musst.",
        "Der Tisch braucht Nachschub. Bestelle eine neue Runde für zwei Personen.",
        "Du hast so lange über deine Karten nachgedacht, dass du eine Runde Wasser für den Tisch bestellst.",
        "Stoße mit deinem linken Nachbarn an.",
        "Du strahlst heute pure Urlaubsenergie aus. Stoße mit einem Mitspieler an.",
        "Du bist heute für die Unterhaltung zuständig. Halte eine kurze Ansprache wie der Kapitän.",
        "Du musst den nächsten Toast mit maximal fünf Wörtern halten.",
        "Du bist heute Getränkesommelier. Empfiehl jedem am Tisch ein Getränk.",
        "Du hast eine Runde verloren. Erkläre dein Blatt, als wärst du Bundestrainer.",
        "Du bist heute der Bockmeister. Stoße auf die nächste Bockrunde an.",
        "Mache einem Mitspieler ein ernst gemeintes Kompliment.",
        "Alle am Tisch heben kurz ihr Glas.",
        "Du bist VIP. Bestimme, wer mit dir anstößt.",
        "Bestelle dir etwas Alkoholfreies und starte mental neu.",
        "Du hast gerade einen spektakulären Spielzug gemacht. Erzähle ihn in einem Satz nach.",
        "Wähle einen Mitspieler: Ihr müsst gemeinsam einen kreativen Namen für eure nächste Spielpartei finden.",
        "Der Kapitän verlangt Nachschub. Bestelle dein nächstes Getränk."
    )

    const val MIN_INTERVAL_MINUTES = 5
    const val MAX_INTERVAL_MINUTES = 25
    const val MIN_INTERVAL_MS = MIN_INTERVAL_MINUTES * 60 * 1000L
    const val MAX_INTERVAL_MS = MAX_INTERVAL_MINUTES * 60 * 1000L

    /**
     * Erstellt den kombinierten Aufgabenpool aus den unveränderbaren Standardaufgaben
     * und den gespeicherten eigenen Aufgaben.
     */
    fun getCombinedTasks(customTasks: List<String>): List<String> {
        val validCustom = customTasks
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return DEFAULT_TASKS + validCustom
    }

    /**
     * Wählt zufällig eine Aufgabe aus dem kombinierten Pool.
     */
    fun getRandomTask(customTasks: List<String>): String? {
        val pool = getCombinedTasks(customTasks)
        return if (pool.isNotEmpty()) pool.random() else null
    }

    /**
     * Wählt zufällig einen Spieler aus den aktiven Spielern der Partie.
     */
    fun getRandomPlayer(players: List<Player>): Player? {
        return if (players.isNotEmpty()) players.random() else null
    }

    /**
     * Berechnet die nächste zufällige Wartezeit im Bereich von [minMs] bis [maxMs].
     */
    fun calculateNextIntervalMs(minMs: Long = MIN_INTERVAL_MS, maxMs: Long = MAX_INTERVAL_MS): Long {
        return if (maxMs > minMs) {
            Random.nextLong(minMs, maxMs + 1L)
        } else {
            minMs
        }
    }
}

/**
 * Repräsentiert eine aktive AIDA-Extreme-Meldung für einen zufällig gewählten Spieler.
 */
data class AidaAlert(
    val player: Player,
    val task: String
)
