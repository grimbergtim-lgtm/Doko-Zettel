package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Game
import com.example.data.model.Player
import com.example.data.model.PlayerStats
import com.example.data.repository.DoppelkopfRepository
import com.example.ui.components.BarItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StatsMetric {
    TOTAL_POINTS,
    AVG_PER_ROUND,
    WINS,
    WEDDINGS
}

data class StatsUiState(
    val playerStats: List<PlayerStats> = emptyList(),
    val allGames: List<Game> = emptyList(),
    val selectedGameId: Long? = null, // null = Alle Spiele
    val selectedMetric: StatsMetric = StatsMetric.TOTAL_POINTS,
    val barItems: List<BarItem> = emptyList(),
    val topPlayer: PlayerStats? = null,
    val weddingKing: PlayerStats? = null,
    val highestScoreRecord: Pair<Player, Int>? = null,
    val lowestScoreRecord: Pair<Player, Int>? = null,
    val isLoading: Boolean = false
)

class StatsViewModel(
    private val repository: DoppelkopfRepository
) : ViewModel() {

    private val _selectedGameId = MutableStateFlow<Long?>(null)
    private val _selectedMetric = MutableStateFlow(StatsMetric.TOTAL_POINTS)

    val uiState: StateFlow<StatsUiState> = combine(
        repository.getAllGames(),
        _selectedGameId,
        _selectedMetric
    ) { games, selectedGameId, metric ->
        val stats = repository.computeAllPlayerStatsSync()

        val barItems = when (metric) {
            StatsMetric.TOTAL_POINTS -> stats.map {
                BarItem(
                    player = it.player,
                    value = it.totalPoints.toDouble(),
                    displayValue = if (it.totalPoints > 0) "+${it.totalPoints} Pkt." else "${it.totalPoints} Pkt."
                )
            }
            StatsMetric.AVG_PER_ROUND -> stats.map {
                BarItem(
                    player = it.player,
                    value = it.averagePointsPerRound,
                    displayValue = String.format("%.2f Pkt./R.", it.averagePointsPerRound)
                )
            }.sortedByDescending { it.value }
            StatsMetric.WINS -> stats.map {
                BarItem(
                    player = it.player,
                    value = it.winsCount.toDouble(),
                    displayValue = "${it.winsCount} Siege (${String.format("%.0f", it.winRatePercent)}%)"
                )
            }.sortedByDescending { it.value }
            StatsMetric.WEDDINGS -> stats.map {
                BarItem(
                    player = it.player,
                    value = it.weddingCount.toDouble(),
                    displayValue = "${it.weddingCount} Hochzeiten"
                )
            }.sortedByDescending { it.value }
        }

        val top = stats.maxByOrNull { it.totalPoints }
        val weddingK = stats.filter { it.weddingCount > 0 }.maxByOrNull { it.weddingCount }
        val bestScore = stats.mapNotNull { s -> s.bestRoundScore?.let { s.player to it } }.maxByOrNull { it.second }
        val worstScore = stats.mapNotNull { s -> s.worstRoundScore?.let { s.player to it } }.minByOrNull { it.second }

        StatsUiState(
            playerStats = stats,
            allGames = games,
            selectedGameId = selectedGameId,
            selectedMetric = metric,
            barItems = barItems,
            topPlayer = top,
            weddingKing = weddingK,
            highestScoreRecord = bestScore,
            lowestScoreRecord = worstScore,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState(isLoading = true)
    )

    fun setMetric(metric: StatsMetric) {
        _selectedMetric.value = metric
    }

    fun setSelectedGameId(gameId: Long?) {
        _selectedGameId.value = gameId
    }
}
