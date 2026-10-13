package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.ScoreBarChart
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.WeddingPink
import com.example.ui.viewmodel.StatsMetric
import com.example.ui.viewmodel.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatisticsScreen(
    viewModel: StatsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Statistiken & Ranglisten",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("stats_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Highlight Cards (König, Rekord)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Gesamt-Führender
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                Text("Spitzenreiter", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = state.topPlayer?.player?.name ?: "-",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.topPlayer?.let { "${it.totalPoints} Punkte" } ?: "Keine Daten",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Hochzeits-König
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = WeddingPink.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = WeddingPink, modifier = Modifier.size(16.dp))
                                Text("Hochzeitskönig", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WeddingPink)
                            }
                            Text(
                                text = state.weddingKing?.player?.name ?: "-",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.weddingKing?.let { "${it.weddingCount} Hochzeiten" } ?: "0 Hochzeiten",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Metrik-Filter für Balkendiagramm
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Metrik auswählen:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedMetric == StatsMetric.TOTAL_POINTS,
                            onClick = { viewModel.setMetric(StatsMetric.TOTAL_POINTS) },
                            label = { Text("Gesamtpunkte") }
                        )
                        FilterChip(
                            selected = state.selectedMetric == StatsMetric.AVG_PER_ROUND,
                            onClick = { viewModel.setMetric(StatsMetric.AVG_PER_ROUND) },
                            label = { Text("Ø pro Runde") }
                        )
                        FilterChip(
                            selected = state.selectedMetric == StatsMetric.WINS,
                            onClick = { viewModel.setMetric(StatsMetric.WINS) },
                            label = { Text("Siege") }
                        )
                        FilterChip(
                            selected = state.selectedMetric == StatsMetric.WEDDINGS,
                            onClick = { viewModel.setMetric(StatsMetric.WEDDINGS) },
                            label = { Text("Hochzeiten") }
                        )
                        FilterChip(
                            selected = state.selectedMetric == StatsMetric.SOLO_AS_SOLOIST,
                            onClick = { viewModel.setMetric(StatsMetric.SOLO_AS_SOLOIST) },
                            label = { Text("⭐ Solospiele") }
                        )
                        FilterChip(
                            selected = state.selectedMetric == StatsMetric.SOLO_POINTS,
                            onClick = { viewModel.setMetric(StatsMetric.SOLO_POINTS) },
                            label = { Text("⭐ Solo-Punkte") }
                        )
                    }
                }
            }

            // Balkendiagramm
            item {
                val chartTitle = when (state.selectedMetric) {
                    StatsMetric.TOTAL_POINTS -> "Vergleich: Gesamtpunkte aller Spieler"
                    StatsMetric.AVG_PER_ROUND -> "Vergleich: Durchschnittliche Punkte pro Runde"
                    StatsMetric.WINS -> "Vergleich: Anzahl gewonnener Partien (Platz 1)"
                    StatsMetric.WEDDINGS -> "Vergleich: Häufigkeit von Hochzeiten"
                    StatsMetric.SOLO_AS_SOLOIST -> "Vergleich: Anzahl gespielter Soli (als Solospieler)"
                    StatsMetric.SOLO_POINTS -> "Vergleich: Punkteentwicklung in Solopartien"
                }
                ScoreBarChart(
                    items = state.barItems,
                    title = chartTitle
                )
            }

            // Rangliste aller Spieler
            item {
                Text(
                    text = "Ewige Bestenliste",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(state.playerStats) { index, stat ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val medalText = when (index) {
                                0 -> "🥇"
                                1 -> "🥈"
                                2 -> "🥉"
                                else -> "${index + 1}."
                            }
                            Text(
                                text = medalText,
                                fontSize = if (index < 3) 18.sp else 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            PlayerAvatar(player = stat.player, size = 32.dp, showBorder = false)
                            Column {
                                Text(stat.player.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = "${stat.totalGames} Spiele • ${stat.totalRoundsPlayed} Runden",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (stat.totalPoints > 0) "+${stat.totalPoints}" else "${stat.totalPoints}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (stat.totalPoints > 0) PositiveGreen else if (stat.totalPoints < 0) NegativeRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Ø ${String.format("%.2f", stat.averagePointsPerRound)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
