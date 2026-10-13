package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Player
import com.example.data.model.PlayerStats
import com.example.ui.components.PlayerAvatar
import com.example.ui.theme.BockOrange
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.WeddingPink
import com.example.ui.viewmodel.PlayersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfilesScreen(
    viewModel: PlayersViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedStatsForDetail by remember { mutableStateOf<PlayerStats?>(null) }
    var playerToEdit by remember { mutableStateOf<Player?>(null) }
    var playerToDelete by remember { mutableStateOf<Player?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Spielerprofile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("players_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("add_player_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Spieler anlegen")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NegativeRed.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = state.errorMessage ?: "",
                        color = NegativeRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.playerStatsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Noch keine Spielerprofile vorhanden. Tippe auf '+', um ein Profil anzulegen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.playerStatsList, key = { it.player.id }) { stats ->
                        PlayerProfileCard(
                            stats = stats,
                            onClick = { selectedStatsForDetail = stats },
                            onEdit = { playerToEdit = stats.player },
                            onDelete = { playerToDelete = stats.player }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Detail-Dialog für Spielerstatistiken
    selectedStatsForDetail?.let { stats ->
        PlayerDetailDialog(
            stats = stats,
            onDismiss = { selectedStatsForDetail = null }
        )
    }

    // Spieler anlegen
    if (showCreateDialog) {
        CreatePlayerDialog(
            onConfirm = { name, colorHex, symbol ->
                viewModel.addPlayer(name, colorHex, symbol)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    // Spieler bearbeiten (stabile ID)
    playerToEdit?.let { player ->
        EditPlayerDialog(
            player = player,
            onConfirm = { updated ->
                viewModel.updatePlayer(updated)
                playerToEdit = null
            },
            onDismiss = { playerToEdit = null }
        )
    }

    // Spieler löschen
    playerToDelete?.let { player ->
        AlertDialog(
            onDismissRequest = { playerToDelete = null },
            title = { Text("Spieler '${player.name}' löschen?") },
            text = { Text("Falls dieser Spieler bereits in Partien mitgespielt hat, kann er zur Erhaltung der Spielhistorie nicht gelöscht werden.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlayer(player)
                        playerToDelete = null
                    }
                ) {
                    Text("Löschen", color = NegativeRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playerToDelete = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun PlayerProfileCard(
    stats: PlayerStats,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val player = stats.player

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlayerAvatar(player = player, size = 44.dp)
                Column {
                    Text(
                        text = player.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${stats.totalGames} Spiele • ${stats.totalRoundsPlayed} Runden",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (stats.totalPoints > 0) "+${stats.totalPoints} Pkt." else "${stats.totalPoints} Pkt.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (stats.totalPoints > 0) PositiveGreen else if (stats.totalPoints < 0) NegativeRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ø ${String.format("%.1f", stats.averagePointsPerRound)}/Runde",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Löschen", modifier = Modifier.size(16.dp), tint = NegativeRed)
                }
            }
        }
    }
}

@Composable
private fun PlayerDetailDialog(
    stats: PlayerStats,
    onDismiss: () -> Unit
) {
    val player = stats.player

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlayerAvatar(player = player, size = 40.dp)
                Column {
                    Text(player.name, fontWeight = FontWeight.Bold)
                    Text("Spieler-ID: #${player.id}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gesamtpunkte:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (stats.totalPoints > 0) "+${stats.totalPoints}" else "${stats.totalPoints}",
                        fontWeight = FontWeight.Bold,
                        color = if (stats.totalPoints > 0) PositiveGreen else if (stats.totalPoints < 0) NegativeRed else MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ø Punkte pro Runde:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format("%.2f", stats.averagePointsPerRound)} Pkt.", fontWeight = FontWeight.Medium)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gespielte Runden / Partien:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${stats.totalRoundsPlayed} / ${stats.totalGames}", fontWeight = FontWeight.Medium)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bestes Rundenergebnis:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stats.bestRoundScore?.let { if (it > 0) "+$it" else "$it" } ?: "-", fontWeight = FontWeight.Bold, color = PositiveGreen)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Schlechtestes Rundenergebnis:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stats.worstRoundScore?.toString() ?: "-", fontWeight = FontWeight.Bold, color = NegativeRed)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Hochzeiten:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = WeddingPink, modifier = Modifier.size(14.dp))
                        Text("${stats.weddingCount}", fontWeight = FontWeight.Bold, color = WeddingPink)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Siege (Platz 1 beendet):", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${stats.winsCount} (${String.format("%.0f", stats.winRatePercent)}%)", fontWeight = FontWeight.Medium)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Höchste gespielte Bockstufe:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = BockOrange, modifier = Modifier.size(14.dp))
                        Text("Stufe ${stats.highestBockLevelPlayed}", fontWeight = FontWeight.Bold, color = BockOrange)
                    }
                }

                HorizontalDivider()
                Text("Solo-Statistiken & Spieltyp-Vergleich:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Als Solospieler:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${stats.soloRoundsAsSoloist} Runden (${stats.soloWinsAsSoloist} Siege, ${if (stats.soloPointsAsSoloist > 0) "+${stats.soloPointsAsSoloist}" else "${stats.soloPointsAsSoloist}"} Pkt.)",
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Als Gegenspieler:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${stats.soloRoundsAsOpponent} Runden (${stats.soloWinsAsOpponent} Siege, ${if (stats.soloPointsAsOpponent > 0) "+${stats.soloPointsAsOpponent}" else "${stats.soloPointsAsOpponent}"} Pkt.)",
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Normalspiele (Runden/Pkt.):", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${stats.normalRoundsPlayed} Runden (${if (stats.normalPoints > 0) "+${stats.normalPoints}" else "${stats.normalPoints}"} Pkt.)", fontWeight = FontWeight.Medium)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Punktevergleich Normal vs. Solo:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${stats.normalPoints} Pkt. (Normal) / ${stats.soloPointsAsSoloist + stats.soloPointsAsOpponent} Pkt. (Solo)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditPlayerDialog(
    player: Player,
    onConfirm: (Player) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(player.name) }
    var selectedColor by remember { mutableLongStateOf(player.colorHex) }
    var selectedSymbol by remember { mutableStateOf(player.avatarSymbol) }
    val symbols = listOf("♠", "♥", "♦", "♣", "👑", "⭐", "🎯", "🃏")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profil bearbeiten", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Farbe wählen:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlayerColors.forEach { color ->
                        val hex = color.value.toLong()
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                Text("Symbol wählen:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    symbols.forEach { sym ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedSymbol == sym) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedSymbol = sym },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = sym, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.trim().isNotEmpty()) {
                        onConfirm(player.copy(name = name.trim(), colorHex = selectedColor, avatarSymbol = selectedSymbol))
                    }
                },
                enabled = name.trim().isNotEmpty()
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
