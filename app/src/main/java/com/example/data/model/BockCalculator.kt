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
     * Ermittelt die Anzahl der wirksamen Bockstufen für einen gegebenen Multiplikator.
     * Formel: Multiplikator = 2^AnzahlDerBockstufen
     *
     * 0 Bockstufen = Multiplikator ×1
     * 1 Bockstufe  = Multiplikator ×2
     * 2 Bockstufen = Multiplikator ×4
     * 3 Bockstufen = Multiplikator ×8
     * 4 Bockstufen = Multiplikator ×16
     * 5 Bockstufen = Multiplikator ×32
     * 6 Bockstufen = Multiplikator ×64
     * 7 Bockstufen = Multiplikator ×128
     * 8 Bockstufen = Multiplikator ×256
     */
    fun getBockStufenForMultiplier(multiplier: Int): Int {
        if (multiplier <= 1) return 0
        var m = multiplier
        var stufen = 0
        while (m > 1) {
            m = m shr 1
            stufen++
        }
        return stufen
    }

    /**
     * Ermittelt die lesbare Bock-Bezeichnung für die Anzahl der wirksamen Bockstufen:
     * 0 Bockstufen -> Normales Spiel (kein Bock)
     * 1 Bockstufe  -> Einfacher Bock
     * 2 Bockstufen -> Doppelter Bock
     * 3 Bockstufen -> Dreifacher Bock
     * 4 Bockstufen -> Vierfacher Bock
     * 5 Bockstufen -> Fünffacher Bock
     * 6 Bockstufen -> Sechsfacher Bock
     * 7 Bockstufen -> Siebenfacher Bock
     * 8 Bockstufen -> Achtfacher Bock
     */
    fun getBockNameForStufen(bockStufen: Int): String = when (bockStufen) {
        0 -> "Normales Spiel"
        1 -> "Einfacher Bock"
        2 -> "Doppelter Bock"
        3 -> "Dreifacher Bock"
        4 -> "Vierfacher Bock"
        5 -> "Fünffacher Bock"
        6 -> "Sechsfacher Bock"
        7 -> "Siebenfacher Bock"
        8 -> "Achtfacher Bock"
        else -> if (bockStufen < 0) "Normales Spiel" else "${bockStufen}-facher Bock"
    }

    /**
     * Ermittelt die lesbare Bock-Bezeichnung für einen gegebenen Multiplikator.
     * Die Bezeichnung richtet sich nach der Anzahl der wirksamen Bockstufen (Multiplikator = 2^AnzahlDerBockstufen).
     *
     * Multiplikator ×1   -> Normales Spiel (0 Bockstufen, kein Bock)
     * Multiplikator ×2   -> Einfacher Bock (1 Bockstufe)
     * Multiplikator ×4   -> Doppelter Bock (2 Bockstufen)
     * Multiplikator ×8   -> Dreifacher Bock (3 Bockstufen)
     * Multiplikator ×16  -> Vierfacher Bock (4 Bockstufen)
     * Multiplikator ×32  -> Fünffacher Bock (5 Bockstufen)
     * Multiplikator ×64  -> Sechsfacher Bock (6 Bockstufen)
     * Multiplikator ×128 -> Siebenfacher Bock (7 Bockstufen)
     * Multiplikator ×256 -> Achtfacher Bock (8 Bockstufen)
     */
    fun getBockName(multiplier: Int): String {
        val stufen = getBockStufenForMultiplier(multiplier)
        return getBockNameForStufen(stufen)
    }

    /**
     * Formatiert die Bock-Bezeichnung zusammen mit dem Multiplikator-Faktor,
     * z. B. „Normales Spiel (×1)“, „Einfacher Bock (×2)“, „Doppelter Bock (×4)“, „Dreifacher Bock (×8)“.
     */
    fun formatBockWithMultiplier(multiplier: Int): String {
        val name = getBockName(multiplier)
        val safeMultiplier = multiplier.coerceAtLeast(1)
        return "$name (×$safeMultiplier)"
    }

    /**
     * Formatiert den Text für die Historie:
     * z.B. „Dreifachbock ausgelöst – von Einfacher Bock (×2) auf Vierfacher Bock (×16)“
     */
    fun formatTriggerHistoryText(triggers: Int, prevLevel: Int, resLevel: Int): String {
        if (triggers <= 0) return ""
        val name = getTriggerName(triggers)
        val prevMult = getMultiplier(prevLevel)
        val resMult = getMultiplier(resLevel)
        return "$name ausgelöst – von ${formatBockWithMultiplier(prevMult)} auf ${formatBockWithMultiplier(resMult)}"
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

    /**
     * Repräsentiert eine zusammengefasste Gruppe ausstehender Bockrunden mit gleichem Multiplikator.
     */
    data class OutstandingBockRounds(
        val roundCount: Int,
        val multiplier: Int,
        val displayName: String
    )

    /**
     * Ermittelt die Liste aktiver Restlaufzeiten für ein Spiel.
     * Nutzt primär [activeBockSeries]. Falls leer, aber [remainingBockRounds] > 0 und [currentBockLevel] > 1,
     * wird ein Fallback anhand der Stufe und Restrunden gebildet.
     */
    fun getActiveSeriesForGame(
        activeBockSeries: String,
        currentBockLevel: Int,
        remainingBockRounds: Int
    ): List<Int> {
        val parsed = parseActiveSeries(activeBockSeries)
        if (parsed.isNotEmpty()) return parsed
        if (remainingBockRounds > 0 && currentBockLevel > 1) {
            val count = (currentBockLevel - 1).coerceIn(1, MAX_LEVEL - 1)
            return List(count) { remainingBockRounds }
        }
        return emptyList()
    }

    /**
     * Berechnet die noch ausstehenden Bockrunden gruppiert nach Multiplikator.
     * Simuliert die zukünftigen Runden auf Basis der aktiven Restlaufzeiten:
     * In jeder Runde wird der Multiplikator anhand der Anzahl aktiver Folgen ermittelt
     * und jede Folge um 1 dekrementiert (abgelaufene Folgen fallen weg).
     */
    fun calculateOutstandingBockRounds(series: List<Int>): List<OutstandingBockRounds> {
        var current = series.filter { it > 0 }
        if (current.isEmpty()) return emptyList()

        val multiplierCounts = mutableMapOf<Int, Int>()
        while (current.isNotEmpty()) {
            val mult = getMultiplierForActiveSeriesCount(current.size)
            multiplierCounts[mult] = (multiplierCounts[mult] ?: 0) + 1
            current = current.map { it - 1 }.filter { it > 0 }
        }

        return multiplierCounts.entries
            .sortedByDescending { it.key }
            .map { (mult, count) ->
                val roundLabel = if (count == 1) "1 Runde" else "$count Runden"
                val bockName = formatBockWithMultiplier(mult)
                OutstandingBockRounds(
                    roundCount = count,
                    multiplier = mult,
                    displayName = "$roundLabel – $bockName"
                )
            }
    }

    /**
     * Überladung zur direkten Berechnung aus den Spieldaten.
     */
    fun calculateOutstandingBockRounds(
        activeBockSeries: String,
        currentBockLevel: Int,
        remainingBockRounds: Int
    ): List<OutstandingBockRounds> {
        val series = getActiveSeriesForGame(activeBockSeries, currentBockLevel, remainingBockRounds)
        return calculateOutstandingBockRounds(series)
    }
}
