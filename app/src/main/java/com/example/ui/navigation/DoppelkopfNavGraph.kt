package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.data.local.AppDatabase
import com.example.data.repository.DoppelkopfRepository
import com.example.data.settings.SettingsRepository
import com.example.ui.screens.ActiveGameScreen
import com.example.ui.screens.GameHistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NewGameScreen
import com.example.ui.screens.PlayerProfilesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.NewGameViewModel
import com.example.ui.viewmodel.PlayersViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.StatsViewModel

@Composable
fun DoppelkopfNavGraph(
    navController: NavHostController,
    database: AppDatabase,
    repository: DoppelkopfRepository,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            val homeVm = viewModel { HomeViewModel(repository) }
            HomeScreen(
                viewModel = homeVm,
                onNavigateNewGame = { navController.navigate(Screen.NewGame.route) },
                onNavigateResumeGame = { gameId ->
                    navController.navigate(Screen.ActiveGame.createRoute(gameId))
                },
                onNavigateHistory = { navController.navigate(Screen.GameHistory.route) },
                onNavigatePlayers = { navController.navigate(Screen.PlayerProfiles.route) },
                onNavigateStats = { navController.navigate(Screen.Statistics.route) },
                onNavigateSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.NewGame.route) {
            val newGameVm = viewModel { NewGameViewModel(repository) }
            NewGameScreen(
                viewModel = newGameVm,
                onNavigateBack = { navController.popBackStack() },
                onGameCreated = { gameId ->
                    navController.navigate(Screen.ActiveGame.createRoute(gameId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(
            route = Screen.ActiveGame.route,
            arguments = listOf(navArgument("gameId") { type = NavType.LongType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getLong("gameId") ?: 0L
            val gameVm = viewModel(key = "game_$gameId") {
                GameViewModel(gameId, repository)
            }
            ActiveGameScreen(
                viewModel = gameVm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.GameHistory.route) {
            val historyVm = viewModel { HistoryViewModel(repository) }
            GameHistoryScreen(
                viewModel = historyVm,
                onNavigateBack = { navController.popBackStack() },
                onOpenGame = { gameId ->
                    navController.navigate(Screen.ActiveGame.createRoute(gameId))
                }
            )
        }

        composable(Screen.PlayerProfiles.route) {
            val playersVm = viewModel { PlayersViewModel(repository) }
            PlayerProfilesScreen(
                viewModel = playersVm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Statistics.route) {
            val statsVm = viewModel { StatsViewModel(repository) }
            StatisticsScreen(
                viewModel = statsVm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            val settingsVm = viewModel { SettingsViewModel(database, settingsRepository) }
            SettingsScreen(
                viewModel = settingsVm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
