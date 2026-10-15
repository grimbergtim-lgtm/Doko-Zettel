package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.BockOrange
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.NeutralZero

@Composable
fun GameLiveStatsSection(
    gameData: FullGameData,
    modifier: Modifier = Modifier
) {
    val stats = remember(gameData.roundsWithScores) { gameData.liveStats }
    var selectedPartnerPlayerId by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Anzahl der Siege pro Spieler
        LiveWinsCard(stats = stats.playerWins)

        // 2. Sieg- und Niederlagenserien
        LiveStreaksCard(stats = stats.playerStreaks)

        // 3. Erfolge in Bockrunden
        LiveBockStatsCard(stats = stats.playerBockStats)

        // 4. Häufigste gemeinsame Spielpartner
        LivePartnerFrequencyCard(
            partnerStats = stats.partnerStats,
            players = gameData.players,
            selectedPlayerId = selectedPartnerPlayerId,
            onSelectPlayer = { selectedPartnerPlayerId = it }
        )

        // 5. Erfolgsbilanz mit den einzelnen Spielpartnern
        LivePartnerPerformanceCard(
            partnerStats = stats.partnerStats,
            players = gameData.players,
            selectedPlayerId = selectedPartnerPlayerId,
            onSelectPlayer = { selectedPartnerPlayerId = it }
        )
    }
}

/**
 * 1. Anzahl der Siege pro Spieler im laufenden Spiel
 */
@Composable
fun LiveWinsCard(
    stats: List<PlayerRoundWinStats>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_wins_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Rundensiege pro Spieler",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (stats.all { it.roundsPlayed == 0 }) {
                Text(
                    text = "Noch keine abgeschlossenen Runden vorhanden.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                stats.forEachIndexed { index, item ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                PlayerAvatar(player = item.player, size = 28.dp, showBorder = false)
                                Column {
                                    Text(
                                        text = item.player.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${item.roundsPlayed} Runden (${item.winsCount} S / ${item.lossesCount} N / ${item.drawsCount} U)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${item.winsCount} ${if (item.winsCount == 1) "Sieg" else "Siege"}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.winsCount > 0) PositiveGreen else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = String.format("%.1f %% Quote", item.winRatePercent),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Fortschrittsbalken für Siegquote
                        val progress = (item.winRatePercent / 100.0).toFloat().coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = item.player.color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    if (index < stats.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2. Sieg- und Niederlagenserien
 */
@Composable
fun LiveStreaksCard(
    stats: List<PlayerStreakStats>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_streaks_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = BockOrange,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Sieg- und Niederlagenserien",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Transparente Regeldokumentation für Serien
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .padding(8.dp)
            ) {
                Text(
                    text = "ℹ️ Serien basieren chronologisch auf gespielten Runden. Unentschiedene Runden (0 Rohpunkte) setzen Serien zurück. Aussetzende Spieler pausieren ihre Serie.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }

            if (stats.isEmpty()) {
                Text(
                    text = "Noch keine Serien vorhanden.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                stats.forEachIndexed { index, item ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PlayerAvatar(player = item.player, size = 26.dp, showBorder = false)
                                Text(
                                    text = item.player.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }

                            // Aktueller Serien-Status
                            when {
                                item.currentWinStreak > 0 -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(PositiveGreen.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "🔥 ${item.currentWinStreak} Sieg(e) aktuell",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PositiveGreen
                                        )
                                    }
                                }
                                item.currentLossStreak > 0 -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(NegativeRed.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "❄️ ${item.currentLossStreak} Niederl. aktuell",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NegativeRed
                                        )
                                    }
                                }
                                else -> {
                                    Text(
                                        text = "Keine aktive Serie",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Längste Serien
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 34.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = PositiveGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Längste Siegesserie: ",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${item.maxWinStreak} R.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = PositiveGreen
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = NegativeRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Längste Niederlagenserie: ",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${item.maxLossStreak} R.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = NegativeRed
                                )
                            }
                        }
                    }

                    if (index < stats.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. Erfolge in Bockrunden (ab ×2)
 */
@Composable
fun LiveBockStatsCard(
    stats: List<PlayerBockStats>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_bock_stats_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = BockOrange,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Erfolge in Bockrunden",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Auswertung aller Runden mit Multiplikator ≥ ×2",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val anyBockPlayed = stats.any { it.bockRoundsPlayed > 0 }
            if (!anyBockPlayed) {
                Text(
                    text = "Bisher wurden im laufenden Spiel noch keine Bockrunden gespielt.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                stats.forEachIndexed { index, item ->
                    var isExpanded by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (item.breakdownByMultiplier.isNotEmpty()) {
                                        isExpanded = !isExpanded
                                    }
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PlayerAvatar(player = item.player, size = 26.dp, showBorder = false)
                                Column {
                                    Text(
                                        text = item.player.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${item.bockRoundsPlayed} Bockrunden (${item.bockWins} Siege • ${String.format("%.0f", item.bockWinRatePercent)} %)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val ptsText = if (item.bockPoints > 0) "+${item.bockPoints}" else "${item.bockPoints}"
                                Text(
                                    text = "$ptsText Pkt.",
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.bockPoints > 0) PositiveGreen else if (item.bockPoints < 0) NegativeRed else NeutralZero,
                                    fontSize = 13.sp
                                )
                                if (item.breakdownByMultiplier.isNotEmpty()) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Details einklappen" else "Details ausklappen",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Aufschlüsselung nach Bockhöhe
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 34.dp, top = 4.dp, bottom = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                item.breakdownByMultiplier.forEach { multStats ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = multStats.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "${multStats.winsCount}/${multStats.roundsPlayed} Siege (${String.format("%.0f", multStats.winRatePercent)} %)",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            val mPtsText = if (multStats.points > 0) "+${multStats.points}" else "${multStats.points}"
                                            Text(
                                                text = "$mPtsText Pkt.",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (multStats.points > 0) PositiveGreen else if (multStats.points < 0) NegativeRed else NeutralZero
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (index < stats.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4. Häufigste gemeinsame Spielpartner
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LivePartnerFrequencyCard(
    partnerStats: List<PartnerStats>,
    players: List<Player>,
    selectedPlayerId: Long?,
    onSelectPlayer: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_partner_frequency_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Häufigste gemeinsame Spielpartner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gemeinsam in derselben Partei gespielte Runden",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Spielerfilter-Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = selectedPlayerId == null,
                    onClick = { onSelectPlayer(null) },
                    label = { Text("Alle Paare", fontSize = 11.sp) }
                )
                players.forEach { p ->
                    FilterChip(
                        selected = selectedPlayerId == p.id,
                        onClick = { onSelectPlayer(if (selectedPlayerId == p.id) null else p.id) },
                        label = { Text(p.name, fontSize = 11.sp) },
                        leadingIcon = { PlayerAvatar(player = p, size = 16.dp, showBorder = false) }
                    )
                }
            }

            val filtered = if (selectedPlayerId == null) {
                partnerStats
            } else {
                partnerStats.filter { it.containsPlayer(selectedPlayerId) }
            }.sortedByDescending { it.roundsTogether }

            if (filtered.isEmpty() || filtered.all { it.roundsTogether == 0 }) {
                Text(
                    text = "Noch keine gemeinsamen Runden in dieser Konstellation gespielt.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxTogether = filtered.maxOfOrNull { it.roundsTogether } ?: 1

                filtered.forEachIndexed { index, pair ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row {
                                    PlayerAvatar(player = pair.player1, size = 24.dp, showBorder = false)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    PlayerAvatar(player = pair.player2, size = 24.dp, showBorder = false)
                                }
                                Text(
                                    text = "${pair.player1.name} & ${pair.player2.name}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${pair.roundsTogether} ${if (pair.roundsTogether == 1) "Runde" else "Runden"}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = String.format("%.1f %% Häufigkeit", pair.frequencyPercent),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Relativer Anteil
                        val barProgress = if (maxTogether > 0) (pair.roundsTogether.toFloat() / maxTogether) else 0f
                        LinearProgressIndicator(
                            progress = { barProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    if (index < filtered.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 5. Erfolgsbilanz mit den einzelnen Spielpartnern
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LivePartnerPerformanceCard(
    partnerStats: List<PartnerStats>,
    players: List<Player>,
    selectedPlayerId: Long?,
    onSelectPlayer: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_partner_performance_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Handshake,
                    contentDescription = null,
                    tint = PositiveGreen,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Erfolgsbilanz mit Spielpartnern",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gemeinsame Siege, Siegquote und Punktebilanz",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Spielerfilter-Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = selectedPlayerId == null,
                    onClick = { onSelectPlayer(null) },
                    label = { Text("Alle Paare", fontSize = 11.sp) }
                )
                players.forEach { p ->
                    FilterChip(
                        selected = selectedPlayerId == p.id,
                        onClick = { onSelectPlayer(if (selectedPlayerId == p.id) null else p.id) },
                        label = { Text(p.name, fontSize = 11.sp) },
                        leadingIcon = { PlayerAvatar(player = p, size = 16.dp, showBorder = false) }
                    )
                }
            }

            val filtered = if (selectedPlayerId == null) {
                partnerStats
            } else {
                partnerStats.filter { it.containsPlayer(selectedPlayerId) }
            }.sortedWith(
                compareByDescending<PartnerStats> { it.winRatePercent }
                    .thenByDescending { it.winsTogether }
                    .thenByDescending { it.jointPointsBalance }
            )

            if (filtered.isEmpty() || filtered.all { it.roundsTogether == 0 }) {
                Text(
                    text = "Noch keine gemeinsamen Spiele zum Auswerten vorhanden.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                filtered.filter { it.roundsTogether > 0 }.forEachIndexed { index, pair ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Zeile 1: Spielerpaar + Bilanz
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row {
                                    PlayerAvatar(player = pair.player1, size = 24.dp, showBorder = false)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    PlayerAvatar(player = pair.player2, size = 24.dp, showBorder = false)
                                }
                                Text(
                                    text = "${pair.player1.name} & ${pair.player2.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            val balText = if (pair.jointPointsBalance > 0) "+${pair.jointPointsBalance}" else "${pair.jointPointsBalance}"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (pair.jointPointsBalance > 0) PositiveGreen.copy(alpha = 0.15f)
                                        else if (pair.jointPointsBalance < 0) NegativeRed.copy(alpha = 0.15f)
                                        else NeutralZero.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Bilanz: $balText Pkt.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (pair.jointPointsBalance > 0) PositiveGreen else if (pair.jointPointsBalance < 0) NegativeRed else NeutralZero
                                )
                            }
                        }

                        // Zeile 2: Runden, Siege, Quote und Punkte aus Siegen
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "${pair.winsTogether}/${pair.roundsTogether} Siege",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "•",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("%.0f %% Siegquote", pair.winRatePercent),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (pair.winRatePercent >= 50.0) PositiveGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "Siege: +${pair.wonRoundsPointsSum} Pkt.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
