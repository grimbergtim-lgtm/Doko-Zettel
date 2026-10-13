package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BockCalculator
import com.example.data.model.FullGameData
import com.example.data.model.GameType
import com.example.ui.components.PlayerAvatar
import com.example.ui.theme.BockOrange
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.WeddingPink
import com.example.ui.viewmodel.EntryMode
import com.example.ui.viewmodel.GameUiState
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.WinningParty

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoundEntrySheet(
    state: GameUiState,
    viewModel: GameViewModel,
    gameData: FullGameData,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val roundNumber = if (state.editingRoundId != null) {
        gameData.roundsWithScores.find { it.round.id == state.editingRoundId }?.round?.roundNumber ?: 1
    } else {
        gameData.roundCount + 1
    }

    val currentBockLevel = if (state.editingRoundId != null) {
        gameData.roundsWithScores.find { it.round.id == state.editingRoundId }?.round?.bockLevel ?: gameData.game.currentBockLevel
    } else {
        gameData.game.currentBockLevel
    }
    val multiplier = BockCalculator.getMultiplier(currentBockLevel)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        modifier = Modifier
            .imePadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (state.editingRoundId != null) "Runde $roundNumber bearbeiten" else "Neue Runde $roundNumber erfassen",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Bock Stufe $currentBockLevel (${multiplier}x)",
                            color = BockOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (gameData.game.remainingBockRounds > 0) {
                            Text(
                                text = "• Noch ${gameData.game.remainingBockRounds} R.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Schließen")
                }
            }

            // Fehlermeldung
            if (state.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NegativeRed.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = state.errorMessage,
                        color = NegativeRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Spieltyp: Normales Spiel vs. Solospiel
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Spieltyp:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("game_type_selection")
                ) {
                    SegmentedButton(
                        selected = state.gameType == GameType.NORMAL,
                        onClick = { viewModel.setGameType(GameType.NORMAL) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("game_type_normal")
                    ) {
                        Text("Normales Spiel")
                    }
                    SegmentedButton(
                        selected = state.gameType == GameType.SOLO,
                        onClick = { viewModel.setGameType(GameType.SOLO) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("game_type_solo")
                    ) {
                        Text("⭐ Solospiel")
                    }
                }
            }

            // 5-Spieler: Aussetzender Spieler (gilt für normale Spiele und Solospiele gleichermaßen)
            if (gameData.game.playerCount == 5) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Aussetzender Spieler (Geber sitzt aus):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        gameData.players.forEach { player ->
                            val isSitting = state.sittingOutPlayerId == player.id
                            FilterChip(
                                selected = isSitting,
                                onClick = { viewModel.setSittingOutPlayer(player.id) },
                                label = { Text(player.name) },
                                leadingIcon = { PlayerAvatar(player = player, size = 18.dp, showBorder = false) },
                                modifier = Modifier.testTag("sitting_out_${player.id}")
                            )
                        }
                    }
                }
            }

            if (state.gameType == GameType.SOLO) {
                // ==============================
                // SOLOSPIEL: EINGABE & TEAMS
                // ==============================
                val activePlayers = gameData.players.filter { it.id != state.sittingOutPlayerId }
                val currentSoloPlayer = gameData.players.find { it.id == state.soloPlayerId }
                val currentOpponents = activePlayers.filter { it.id != state.soloPlayerId }

                // 1. Auswahl genau eines Solospielers aus den aktiven Teilnehmern
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Solospieler auswählen (spielt allein gegen alle anderen):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activePlayers.forEach { player ->
                            val isSolo = state.soloPlayerId == player.id
                            FilterChip(
                                selected = isSolo,
                                onClick = { viewModel.setSoloPlayer(player.id) },
                                label = { Text(if (isSolo) "⭐ ${player.name} (Solospieler)" else player.name) },
                                leadingIcon = { PlayerAvatar(player = player, size = 18.dp, showBorder = false) },
                                modifier = Modifier.testTag("solo_player_${player.id}")
                            )
                        }
                    }
                }

                // 2. Klare Team-Übersicht: Solospieler vs. Gegenseite
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("solo_team_overview")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Teamzuordnung für dieses Solo:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "⭐ Solospieler:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentSoloPlayer?.name ?: "Kein Spieler ausgewählt",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "🛡️ Gegenseite (${currentOpponents.size} Spieler):",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (currentOpponents.isNotEmpty()) currentOpponents.joinToString { it.name } else "Keine Gegenspieler",
                                fontSize = 13.sp
                            )
                        }
                        if (state.sittingOutPlayerId != null) {
                            val sittingPlayer = gameData.players.find { it.id == state.sittingOutPlayerId }
                            Text(
                                text = "⏸️ Pausiert: ${sittingPlayer?.name ?: ""} (0 Punkte)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 3. Modus-Wahl für Solospiel
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("solo_entry_mode")
                ) {
                    SegmentedButton(
                        selected = state.entryMode == EntryMode.TEAM,
                        onClick = { viewModel.setEntryMode(EntryMode.TEAM) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Solo-Ergebnis")
                    }
                    SegmentedButton(
                        selected = state.entryMode == EntryMode.MANUAL,
                        onClick = { viewModel.setEntryMode(EntryMode.MANUAL) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Manuelle Punkte")
                    }
                }

                if (state.entryMode == EntryMode.TEAM) {
                    // Siegerpartei beim Solo
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Rundenergebnis des Solos:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.setWinningParty(WinningParty.RE) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("solo_won_button"),
                                colors = if (state.winningParty == WinningParty.RE) {
                                    ButtonDefaults.outlinedButtonColors(containerColor = PositiveGreen.copy(alpha = 0.15f))
                                } else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(
                                    text = "Solo gewinnt ⭐",
                                    fontWeight = if (state.winningParty == WinningParty.RE) FontWeight.Bold else FontWeight.Normal,
                                    color = if (state.winningParty == WinningParty.RE) PositiveGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.setWinningParty(WinningParty.KONTRA) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("solo_lost_button"),
                                colors = if (state.winningParty == WinningParty.KONTRA) {
                                    ButtonDefaults.outlinedButtonColors(containerColor = NegativeRed.copy(alpha = 0.15f))
                                } else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(
                                    text = "Gegenseite gewinnt 🛡️",
                                    fontWeight = if (state.winningParty == WinningParty.KONTRA) FontWeight.Bold else FontWeight.Normal,
                                    color = if (state.winningParty == WinningParty.KONTRA) NegativeRed else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Basispunkte Stepper
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Basis-Spielpunkte (vor Bock-Multiplikator):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.setBasePoints(state.basePoints - 1) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "-1")
                            }
                            Text(
                                text = "${state.basePoints} Pkt.",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { viewModel.setBasePoints(state.basePoints + 1) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "+1")
                            }

                            listOf(1, 2, 3, 4, 5, 6).forEach { pts ->
                                FilterChip(
                                    selected = state.basePoints == pts,
                                    onClick = { viewModel.setBasePoints(pts) },
                                    label = { Text("$pts") },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }

                        val soloWon = state.winningParty == WinningParty.RE
                        val oppCount = currentOpponents.size
                        val soloPts = if (soloWon) state.basePoints * oppCount else -state.basePoints * oppCount
                        val oppPts = if (soloWon) -state.basePoints else state.basePoints
                        Text(
                            text = "Verteilung: Solospieler ${if (soloPts > 0) "+$soloPts" else "$soloPts"} Pkt., Gegenspieler je ${if (oppPts > 0) "+$oppPts" else "$oppPts"} Pkt.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Manuelle Eingabe aller Einzelergebnisse beim Solo
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Rohpunkte je Spieler (vor Bock-Multiplikator):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        gameData.players.forEach { player ->
                            val isSolo = player.id == state.soloPlayerId
                            val isSitting = player.id == state.sittingOutPlayerId
                            val rawPts = state.manualRawPoints[player.id] ?: 0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSolo) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PlayerAvatar(player = player, size = 26.dp, showBorder = false)
                                    Column {
                                        Text(
                                            text = if (isSolo) "⭐ ${player.name} (Solospieler)" else player.name,
                                            fontWeight = if (isSolo) FontWeight.Bold else FontWeight.Medium
                                        )
                                        if (isSitting) {
                                            Text("Setzt aus (0)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else if (!isSolo) {
                                            Text("Gegenseite", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                if (isSitting) {
                                    Text("0 Pkt.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.setManualPoint(player.id, rawPts - 1) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("manual_minus_${player.id}")
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "-1")
                                        }
                                        Text(
                                            text = if (rawPts > 0) "+$rawPts" else "$rawPts",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (rawPts > 0) PositiveGreen else if (rawPts < 0) NegativeRed else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.testTag("manual_points_${player.id}")
                                        )
                                        IconButton(
                                            onClick = { viewModel.setManualPoint(player.id, rawPts + 1) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("manual_plus_${player.id}")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "+1")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ==============================
                // NORMALES SPIEL: RE / KONTRA
                // ==============================
                // Modus-Wahl: Team vs. Manuell
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.entryMode == EntryMode.TEAM,
                        onClick = { viewModel.setEntryMode(EntryMode.TEAM) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Team (Re / Kontra)")
                    }
                    SegmentedButton(
                        selected = state.entryMode == EntryMode.MANUAL,
                        onClick = { viewModel.setEntryMode(EntryMode.MANUAL) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Freie Punkteingabe")
                    }
                }

                if (state.entryMode == EntryMode.TEAM) {
                    // Team-Modus: Gewinner-Partei
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Siegerpartei:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.setWinningParty(WinningParty.RE) },
                                modifier = Modifier.weight(1f),
                                colors = if (state.winningParty == WinningParty.RE) {
                                    ButtonDefaults.outlinedButtonColors(containerColor = PositiveGreen.copy(alpha = 0.15f))
                                } else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(
                                    text = "Re gewinnt",
                                    fontWeight = if (state.winningParty == WinningParty.RE) FontWeight.Bold else FontWeight.Normal,
                                    color = if (state.winningParty == WinningParty.RE) PositiveGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.setWinningParty(WinningParty.KONTRA) },
                                modifier = Modifier.weight(1f),
                                colors = if (state.winningParty == WinningParty.KONTRA) {
                                    ButtonDefaults.outlinedButtonColors(containerColor = PositiveGreen.copy(alpha = 0.15f))
                                } else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(
                                    text = "Kontra gewinnt",
                                    fontWeight = if (state.winningParty == WinningParty.KONTRA) FontWeight.Bold else FontWeight.Normal,
                                    color = if (state.winningParty == WinningParty.KONTRA) PositiveGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Re-Spieler auswählen (2 Spieler)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Re-Team Spieler (${state.rePlayerIds.size}/2):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            gameData.players.filter { it.id != state.sittingOutPlayerId }.forEach { player ->
                                val isRe = state.rePlayerIds.contains(player.id)
                                FilterChip(
                                    selected = isRe,
                                    onClick = { viewModel.toggleRePlayer(player.id) },
                                    label = { Text(player.name) },
                                    leadingIcon = { PlayerAvatar(player = player, size = 18.dp, showBorder = false) }
                                )
                            }
                        }
                    }

                    // Basis-Spielpunkte Stepper
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Erspielte Spielpunkte (vor Bock):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.setBasePoints(state.basePoints - 1) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "-1")
                            }
                            Text(
                                text = "${state.basePoints} Pkt.",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { viewModel.setBasePoints(state.basePoints + 1) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "+1")
                            }
                        }
                    }
                } else {
                    // Freie manuelle Eingabe
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Punkte pro Spieler vor Multiplikator:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        gameData.players.forEach { player ->
                            val isSitting = player.id == state.sittingOutPlayerId
                            val rawPts = state.manualRawPoints[player.id] ?: 0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PlayerAvatar(player = player, size = 26.dp, showBorder = false)
                                    Text(player.name, fontWeight = FontWeight.Medium)
                                }

                                if (isSitting) {
                                    Text("Setzt aus (0)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.setManualPoint(player.id, rawPts - 1) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "-1")
                                        }
                                        Text(
                                            text = if (rawPts > 0) "+$rawPts" else "$rawPts",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (rawPts > 0) PositiveGreen else if (rawPts < 0) NegativeRed else MaterialTheme.colorScheme.onSurface
                                        )
                                        IconButton(
                                            onClick = { viewModel.setManualPoint(player.id, rawPts + 1) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "+1")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Hochzeit Zuordnung (nur bei normalem Spiel)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = WeddingPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Hochzeit in dieser Runde (optional):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (state.hochzeitPlayerId != null) {
                        val hp = gameData.players.find { it.id == state.hochzeitPlayerId }
                        val weddingGain = (gameData.players.size - 1) * multiplier
                        val weddingLoss = multiplier
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(WeddingPink.copy(alpha = 0.12f))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "💍 Hochzeitswertung: ${hp?.name} erhält +$weddingGain Pkt., alle anderen Spieler je -$weddingLoss Pkt. (${multiplier}x Bock)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WeddingPink
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        gameData.players.filter { it.id != state.sittingOutPlayerId }.forEach { player ->
                            val isHochzeit = state.hochzeitPlayerId == player.id
                            FilterChip(
                                selected = isHochzeit,
                                onClick = { viewModel.setHochzeitPlayer(player.id) },
                                label = { Text(player.name) },
                                leadingIcon = {
                                    if (isHochzeit) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    else PlayerAvatar(player = player, size = 16.dp, showBorder = false)
                                }
                            )
                        }
                    }
                }
            }

            // Zusätzliche Bockauslöser in dieser Runde (0 bis 8)
            val wouldExceed = BockCalculator.wouldExceedMaxLevel(currentBockLevel, state.bockTriggersCount)
            val maxAllowed = BockCalculator.maxAllowedTriggers(currentBockLevel)
            val resultingLevel = BockCalculator.calculateResultingLevel(currentBockLevel, state.bockTriggersCount)
            val resultingMultiplier = BockCalculator.getMultiplier(resultingLevel)

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (wouldExceed) NegativeRed.copy(alpha = 0.1f)
                    else if (state.bockTriggersCount > 0) BockOrange.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = if (wouldExceed) NegativeRed else BockOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Bockauslöser in dieser Runde:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        // Stepper Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.setBockTriggersCount(state.bockTriggersCount - 1) },
                                enabled = state.bockTriggersCount > 0,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "-1 Bock", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "${state.bockTriggersCount}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = if (wouldExceed) NegativeRed else if (state.bockTriggersCount > 0) BockOrange else MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { viewModel.setBockTriggersCount(state.bockTriggersCount + 1) },
                                enabled = state.bockTriggersCount < 8,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "+1 Bock", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Auslöser-Name
                    Text(
                        text = BockCalculator.getTriggerName(state.bockTriggersCount),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (wouldExceed) NegativeRed else if (state.bockTriggersCount > 0) BockOrange else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Schnellwahl-Chips für 0..8
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            0 to "0 (Kein)",
                            1 to "1 (Einfach)",
                            2 to "2 (Doppel)",
                            3 to "3 (Dreifach)",
                            4 to "4 (Vierfach)",
                            5 to "5",
                            6 to "6",
                            7 to "7",
                            8 to "8"
                        ).forEach { (cnt, label) ->
                            FilterChip(
                                selected = state.bockTriggersCount == cnt,
                                onClick = { viewModel.setBockTriggersCount(cnt) },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }

                    // Vorschau der Auswirkungen oder Fehlermeldung bei Überschreitung
                    if (wouldExceed) {
                        Text(
                            text = "⚠️ Fehler: Maximale Bockstufe 8 (128x) würde überschritten werden! (Aktuell Stufe $currentBockLevel + ${state.bockTriggersCount} Auslöser = Stufe ${currentBockLevel + state.bockTriggersCount}). Bitte auf maximal $maxAllowed Auslöser reduzieren.",
                            color = NegativeRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (state.bockTriggersCount > 0) {
                        val roundsToAdd = state.bockTriggersCount * gameData.game.bockRoundsPerTrigger
                        Text(
                            text = "🔥 Auswirkung: Bockstufe steigt von $currentBockLevel auf $resultingLevel (${resultingMultiplier}x Multiplikator ab nächster Runde, +$roundsToAdd Bockrunden).",
                            color = BockOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "Keine neuen Auslöser. Aktuelle Stufe bleibt Stufe $currentBockLevel (${multiplier}x).",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Optionale Notiz
            OutlinedTextField(
                value = state.note,
                onValueChange = { viewModel.setNote(it) },
                label = { Text("Notiz zur Runde (optional)") },
                placeholder = { Text("z.B. Fuchs gefangen, Karlchen...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Live-Vorschau der Punkte mit transparenter Aufschlüsselung
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Live-Berechnung (${multiplier}x Bock-Multiplikator):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val scorePreviews = viewModel.calculateScorePreviews()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        scorePreviews.forEach { preview ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(preview.player.name, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = if (preview.isSittingOut && preview.weddingPoints == 0) "0"
                                    else if (preview.finalPoints > 0) "+${preview.finalPoints}"
                                    else "${preview.finalPoints}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        preview.finalPoints > 0 -> PositiveGreen
                                        preview.finalPoints < 0 -> NegativeRed
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                if (preview.weddingPoints != 0 && preview.gamePoints != 0) {
                                    Text(
                                        text = "${if (preview.gamePoints > 0) "+${preview.gamePoints}" else "${preview.gamePoints}"} Sp / ${if (preview.weddingPoints > 0) "+${preview.weddingPoints}" else "${preview.weddingPoints}"} Hz",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bestätigungs-Button führt zur Zusammenfassung
            Button(
                onClick = { viewModel.openSummaryDialog() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("preview_summary_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Zur Zusammenfassung", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}
