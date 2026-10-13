package com.example.data.model

object BockCalculator {
    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 8

    /**
     * Stufe n hat den Multiplikator 2^(n-1).
     * Stufe 1 = 1x (2^0)
     * Stufe 2 = 2x (2^1)
     * Stufe 3 = 4x (2^2)
     * Stufe 4 = 8x (2^3)
     * Stufe 5 = 16x (2^4)
     * Stufe 6 = 32x (2^5)
     * Stufe 7 = 64x (2^6)
     * Stufe 8 = 128x (2^7)
     * Stufe 8 wird nicht überschritten.
     */
    fun getMultiplier(level: Int): Int {
        val safeLevel = coerceLevel(level)
        return 1 shl (safeLevel - 1)
    }

    fun coerceLevel(level: Int): Int {
        return level.coerceIn(MIN_LEVEL, MAX_LEVEL)
    }

    /**
     * Erhöht die Bockstufe um 1, gedeckelt bei MAX_LEVEL (8).
     */
    fun increaseLevel(currentLevel: Int): Int {
        return (currentLevel + 1).coerceAtMost(MAX_LEVEL)
    }

    /**
     * Verringert die Bockstufe um 1, Mindestwert MIN_LEVEL (1).
     */
    fun decreaseLevel(currentLevel: Int): Int {
        return (currentLevel - 1).coerceAtLeast(MIN_LEVEL)
    }

    /**
     * Berechnet die resultierende Bockstufe bei Hinzufügen von [triggers] Auslösern.
     */
    fun calculateResultingLevel(currentLevel: Int, triggers: Int): Int {
        val safeCurrent = coerceLevel(currentLevel)
        return (safeCurrent + triggers.coerceAtLeast(0)).coerceAtMost(MAX_LEVEL)
    }

    /**
     * Prüft, ob [triggers] zusätzliche Auslöser die maximale Bockstufe von 8 überschreiten würden.
     */
    fun wouldExceedMaxLevel(currentLevel: Int, triggers: Int): Boolean {
        return (currentLevel + triggers) > MAX_LEVEL
    }

    /**
     * Ermittelt die maximal erlaubten zusätzlichen Auslöser für die aktuelle Stufe.
     */
    fun maxAllowedTriggers(currentLevel: Int): Int {
        return (MAX_LEVEL - currentLevel).coerceIn(0, MAX_LEVEL)
    }

    /**
     * Bezeichnung der neu ausgelösten Bockeinheiten:
     * 0 -> Kein zusätzlicher Bock
     * 1 -> Einfacher Bock
     * 2 -> Doppelbock
     * 3 -> Dreifachbock
     * 4 -> Vierfachbock
     * 5 -> Fünffachbock
     * 6 -> Sechsfachbock
     * 7 -> Siebenfachbock
     * 8 -> Achtfachbock
     */
    fun getTriggerName(triggers: Int): String = when (triggers) {
        0 -> "Kein zusätzlicher Bock"
        1 -> "Einfacher Bock"
        2 -> "Doppelbock"
        3 -> "Dreifachbock"
        4 -> "Vierfachbock"
        5 -> "Fünffachbock"
        6 -> "Sechsfachbock"
        7 -> "Siebenfachbock"
        8 -> "Achtfachbock"
        else -> "${triggers}-fach Bock"
    }

    /**
     * Formatiert den Text für die Historie gemäß Anforderung:
     * z.B. „Dreifachbock ausgelöst – Bockstufe von 2 auf 5“
     */
    fun formatTriggerHistoryText(triggers: Int, prevLevel: Int, resLevel: Int): String {
        if (triggers <= 0) return ""
        val name = getTriggerName(triggers)
        return "$name ausgelöst – Bockstufe von $prevLevel auf $resLevel"
    }

    /**
     * Berechnet die finalen Punkte unter Berücksichtigung des Multiplikators.
     */
    fun calculateFinalPoints(rawPoints: Int, level: Int): Int {
        return rawPoints * getMultiplier(level)
    }

    /**
     * Ermittelt die Bockstufe (1 bis 8) basierend auf der Anzahl gleichzeitig aktiver Bockfolgen.
     * 0 aktive Folgen -> Stufe 1 (1x)
     * 1 aktive Folge  -> Stufe 2 (2x)
     * 2 aktive Folgen -> Stufe 3 (4x)
     * 3 aktive Folgen -> Stufe 4 (8x)
     * ...
     * 7+ aktive Folgen -> Stufe 8 (128x, Maximum)
     */
    fun calculateLevelFromActiveSeriesCount(activeCount: Int): Int {
        return (1 + activeCount.coerceAtLeast(0)).coerceIn(MIN_LEVEL, MAX_LEVEL)
    }

    /**
     * Ermittelt den Multiplikator direkt aus der Anzahl aktiver Bockfolgen.
     */
    fun getMultiplierForActiveSeriesCount(activeCount: Int): Int {
        return getMultiplier(calculateLevelFromActiveSeriesCount(activeCount))
    }

    /**
     * Parst einen CSV-String aktiver Restlaufzeiten in eine Liste von Ganzzahlen.
     * Z. B. "3,1" -> listOf(3, 1)
     */
    fun parseActiveSeries(seriesString: String): List<Int> {
        if (seriesString.isBlank()) return emptyList()
        return seriesString.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }
    }

    /**
     * Serialisiert eine Liste aktiver Restlaufzeiten in einen CSV-String.
     * Z. B. listOf(3, 1) -> "3,1"
     */
    fun serializeActiveSeries(series: List<Int>): String {
        return series.filter { it > 0 }.joinToString(",")
    }

    /**
     * Simuliert den Übergang einer Spielrunde:
     * 1. Jede bisher aktive Bockfolge wird genau einmal um 1 dekrementiert.
     *    Abgelaufene Folgen (Restlaufzeit <= 0) werden entfernt.
     * 2. Für jeden neuen Auslöser wird eine eigene neue Folge mit der vollen
     *    Laufzeit (4 Runden bei 4 Spielern, 5 Runden bei 5 Spielern) hinzugefügt.
     *    Bestehende Restlaufzeiten werden dabei nicht zurückgesetzt.
     */
    fun advanceActiveSeries(
        currentSeries: List<Int>,
        newTriggers: Int,
        durationPerSeries: Int
    ): List<Int> {
        val decremented = currentSeries.map { it - 1 }.filter { it > 0 }
        val newSeries = if (newTriggers > 0 && durationPerSeries > 0) {
            List(newTriggers.coerceIn(0, MAX_LEVEL)) { durationPerSeries }
        } else {
            emptyList()
        }
        return decremented + newSeries
    }

    /**
     * Liefert die Dauer einer Bockfolge basierend auf der Spieleranzahl:
     * Bei 4 Spielern 4 Spielrunden, bei 5 Spielern 5 Spielrunden.
     */
    fun getDurationForPlayerCount(playerCount: Int, fallback: Int = 4): Int {
        return when (playerCount) {
            4 -> 4
            5 -> 5
            else -> fallback.coerceAtLeast(1)
        }
    }
}
