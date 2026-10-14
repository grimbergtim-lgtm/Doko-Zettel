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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.components.PlayerAvatar
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.PlayerColors
import com.example.ui.viewmodel.NewGameViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewGameScreen(
    viewModel: NewGameViewModel,
    onNavigateBack: () -> Unit,
    onGameCreated: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showCreatePlayerDialog by remember { mutableStateOf(false) }

    // Navigiere zu Spiel, sobald es erstellt wurde
    LaunchedEffect(state.createdGameId) {
        state.createdGameId?.let { gameId ->
            onGameCreated(gameId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Neues Spiel vorbereiten",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("new_game_back_button")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fehlermeldung
            if (state.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NegativeRed.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.errorMessage ?: "",
                            color = NegativeRed,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Schließen", tint = NegativeRed)
                        }
                    }
                }
            }

            // 1. Spieleranzahl: 4 oder 5 Spieler
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Spieleranzahl",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = state.playerCount == 4,
                            onClick = { viewModel.setPlayerCount(4) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier.testTag("select_4_players_button")
                        ) {
                            Text("4 Spieler", fontWeight = FontWeight.Bold)
                        }
                        SegmentedButton(
                            selected = state.playerCount == 5,
                            onClick = { viewModel.setPlayerCount(5) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier.testTag("select_5_players_button")
                        ) {
                            Text("5 Spieler", fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = if (state.playerCount == 4) "Alle 4 Spieler spielen jede Runde mit."
                        else "Bei 5 Spielern setzt in jeder Runde ein Spieler aus (standardmäßig der Geber).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Optionaler Spielname & Bockrunden pro Auslöser
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Partie-Einstellungen",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = state.title,
                        onValueChange = { viewModel.setTitle(it) },
                        label = { Text("Name der Spielrunde (optional)") },
                        placeholder = { Text("z.B. Spieleabend bei Lukas") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("game_title_input"),
                        singleLine = true
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Runden pro Bockphase: ${state.bockRoundsPerTrigger}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(4, 5, 8).forEach { r ->
                                FilterChip(
                                    selected = state.bockRoundsPerTrigger == r,
                                    onClick = { viewModel.setBockRounds(r) },
                                    label = { Text("$r Runden") }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Teilnehmer auswählen (genau 4 oder 5, keine Duplikate)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Teilnehmer auswählen",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ausgewählt: ${state.selectedPlayerIds.size} von ${state.playerCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (state.selectedPlayerIds.size == state.playerCount) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = { showCreatePlayerDialog = true },
                            modifier = Modifier.testTag("add_new_player_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Neuer Spieler")
                        }
                    }

                    // Ausgewählte Spieler in Sitzreihenfolge anzeigen
                    if (state.selectedPlayerIds.isNotEmpty()) {
                        Text(
                            text = "Sitzreihenfolge (Geber-Reihenfolge):",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.selectedPlayerIds.forEachIndexed { index, pId ->
                                val player = state.availablePlayers.find { it.id == pId }
                                FilterChip(
                                    selected = true,
                                    onClick = { viewModel.togglePlayerSelection(pId) },
                                    label = { Text("${index + 1}. ${player?.name ?: ""}") },
                                    leadingIcon = { PlayerAvatar(player = player, size = 18.dp, showBorder = false) },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Abwählen", modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }

                    // Alle verfügbaren Profile zur Auswahl
                    Text(
                        text = "Vorhandene Profile (Tippen zum Hinzufügen / Entfernen):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.availablePlayers.forEach { player ->
                            val isSelected = state.selectedPlayerIds.contains(player.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.togglePlayerSelection(player.id) },
                                label = { Text(player.name) },
                                leadingIcon = {
                                    if (isSelected) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    else PlayerAvatar(player = player, size = 18.dp, showBorder = false)
                                },
                                modifier = Modifier.testTag("player_chip_${player.name}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Spiel starten Button
            val isReady = state.selectedPlayerIds.size == state.playerCount
            Button(
                onClick = { viewModel.createGame() },
                enabled = isReady && !state.isCreating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_game_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Partie wird gestartet...")
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Text(
                            text = if (isReady) "Partie starten" else "Bitte noch ${state.playerCount - state.selectedPlayerIds.size} Spieler wählen",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Dialog zum Anlegen eines neuen Spielers
    if (showCreatePlayerDialog) {
        CreatePlayerDialog(
            onConfirm = { name, colorHex, symbol ->
                viewModel.createNewPlayer(name, colorHex, symbol)
                showCreatePlayerDialog = false
            },
            onDismiss = { showCreatePlayerDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatePlayerDialog(
    onConfirm: (String, Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(0xFF2563EBL) }
    var selectedSymbol by remember { mutableStateOf("♠") }

    val symbols = listOf("♠", "♥", "♦", "♣", "👑", "⭐", "🎯", "🃏")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Neues Spielerprofil", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Spielername") },
                    placeholder = { Text("z.B. Max") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_player_name_input")
                )

                // Farbe wählen
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
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Ausgewählt",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Symbol wählen
                Text("Kartensymbol / Avatar:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                            Text(
                                text = sym,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedSymbol == sym) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isNotEmpty()) {
                        onConfirm(name.trim(), selectedColor, selectedSymbol)
                    }
                },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag("save_new_player_button")
            ) {
                Text("Profil anlegen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
