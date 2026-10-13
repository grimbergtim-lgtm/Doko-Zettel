package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object NewGame : Screen("new_game")
    data object ActiveGame : Screen("game/{gameId}") {
        fun createRoute(gameId: Long) = "game/$gameId"
    }
    data object GameHistory : Screen("history")
    data object GameDetail : Screen("game_detail/{gameId}") {
        fun createRoute(gameId: Long) = "game_detail/$gameId"
    }
    data object PlayerProfiles : Screen("players")
    data object PlayerDetail : Screen("player/{playerId}") {
        fun createRoute(playerId: Long) = "player/$playerId"
    }
    data object Statistics : Screen("statistics")
    data object Settings : Screen("settings")
}
