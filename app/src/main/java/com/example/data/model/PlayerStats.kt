package com.example.data.model

data class PlayerStats(
    val player: Player,
    val totalGames: Int = 0,
    val completedGames: Int = 0,
    val totalRoundsPlayed: Int = 0,
    val totalPoints: Int = 0,
    val averagePointsPerRound: Double = 0.0,
    val averagePointsPerGame: Double = 0.0,
    val bestRoundScore: Int? = null,
    val worstRoundScore: Int? = null,
    val weddingCount: Int = 0,
    val winsCount: Int = 0, // Spiele auf Platz 1 beendet
    val winRatePercent: Double = 0.0,
    val highestBockLevelPlayed: Int = 1,

    // Solo-spezifische Statistiken
    val soloRoundsAsSoloist: Int = 0, // Wie oft als Solospieler gespielt
    val soloPointsAsSoloist: Int = 0, // Gesamtpunkte erzielt als Solospieler
    val soloWinsAsSoloist: Int = 0, // Gewonnene Solorunden (Punkte > 0)
    val soloRoundsAsOpponent: Int = 0, // Wie oft als Gegenspieler gegen Solo angetreten
    val soloPointsAsOpponent: Int = 0, // Punkte erzielt als Gegenspieler gegen Solo
    val normalRoundsPlayed: Int = 0, // Runden in Normalspielen
    val normalPoints: Int = 0 // Punkte in Normalspielen
)
