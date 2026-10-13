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
}
