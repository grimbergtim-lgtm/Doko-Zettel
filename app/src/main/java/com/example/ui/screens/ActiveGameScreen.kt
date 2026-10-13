package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.data.model.GameType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.BockCalculator
import com.example.data.model.FullGameData
import com.example.data.model.RoundWithScores
import com.example.ui.components.BockBadge
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.ScoreLineChart
import com.example.ui.theme.BockOrange
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.WeddingPink
import com.example.ui.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveGameScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val gameData = state.gameData

    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var roundToDelete by remember { mutableStateOf<RoundWithScores?>(null) }
    var showFinishConfirmDialog by remember { mutableStateOf(false) }
    var showQuickWeddingDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = gameData?.game?.title?.ifEmpty { "Spielrunde" } ?: "Doppelkopf",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (gameData?.game?.isInProgress == true) "Laufend • ${gameData.roundCount} Runden"
                            else "Beendet • ${gameData?.roundCount ?: 0} Runden",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("game_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    if (gameData != null) {
                        BockBadge(
                            level = gameData.game.currentBockLevel,
                            remainingRounds = gameData.game.remainingBockRounds,
                            onClick = { viewModel.openBockAdjustDialog() }
                        )
                    }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Optionen")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Bockstufen anpassen") },
                            onClick = {
                                showMenu = false
                                viewModel.openBockAdjustDialog()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = BockOrange)
                            }
                        )
                        if (gameData?.game?.isInProgress == true) {
                            DropdownMenuItem(
                                text = { Text("Hochzeit anmelden") },
                                onClick = {
                                    showMenu = false
                                    showQuickWeddingDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = WeddingPink)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Partie beenden") },
                                onClick = {
                                    showMenu = false
                                    showFinishConfirmDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PositiveGreen)
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Partie wiedereröffnen") },
                                onClick = {
                                    showMenu = false
                                    viewModel.reopenGame()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (gameData?.game?.isInProgress == true) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openNewRoundEntry() },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Runde erfassen", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("add_round_fab")
                )
            }
        }
    ) { innerPadding ->
        if (state.isLoading || gameData == null) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live-Scoreboard Karte oben fixiert
            LiveScoreboardCard(
                gameData = gameData,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Tabs: "Runden" vs. "Punkteverlauf"
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Runden (${gameData.roundCount})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Graph") }
                )
            }

            if (selectedTab == 0) {
                // Runden-Liste mit Spieltyp-Filter
                var roundFilter by remember { mutableStateOf<GameType?>(null) }
                val filteredRounds = when (roundFilter) {
                    null -> gameData.roundsWithScores
                    GameType.NORMAL -> gameData.roundsWithScores.filter { !it.round.isSolo }
                    GameType.SOLO -> gameData.roundsWithScores.filter { it.round.isSolo }
                }

                if (gameData.roundsWithScores.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Noch keine Runden gespielt",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tippe unten auf 'Runde erfassen', um das erste Rundenergebnis einzugeben.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = roundFilter == null,
                                    onClick = { roundFilter = null },
                                    label = { Text("Alle (${gameData.roundCount})") }
                                )
                                FilterChip(
                                    selected = roundFilter == GameType.NORMAL,
                                    onClick = { roundFilter = GameType.NORMAL },
                                    label = { Text("Normal (${gameData.normalRoundCount})") }
                                )
                                FilterChip(
                                    selected = roundFilter == GameType.SOLO,
                                    onClick = { roundFilter = GameType.SOLO },
                                    label = { Text("⭐ Solo (${gameData.soloRoundCount})") }
                                )
                            }
                        }
                        items(filteredRounds.reversed(), key = { it.round.id }) { roundWithScores ->
                            RoundCard(
                                roundWithScores = roundWithScores,
                                gameData = gameData,
                                onEdit = { viewModel.openEditRound(roundWithScores) },
                                onDelete = { roundToDelete = roundWithScores }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            } else {
                // Liniendiagramm Punkteverlauf mit Spieltyp-Filter
                var chartFilter by remember { mutableStateOf<GameType?>(null) }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = chartFilter == null,
                                onClick = { chartFilter = null },
                                label = { Text("Alle Runden") }
                            )
                            FilterChip(
                                selected = chartFilter == GameType.NORMAL,
                                onClick = { chartFilter = GameType.NORMAL },
                                label = { Text("Nur Normale") }
                            )
                            FilterChip(
                                selected = chartFilter == GameType.SOLO,
                                onClick = { chartFilter = GameType.SOLO },
                                label = { Text("⭐ Nur Solospiele") }
                            )
                        }
                    }
                    item {
                        ScoreLineChart(
                            progression = gameData.getCumulativeScoreProgression(filterGameType = chartFilter),
                            players = gameData.players
                        )
                    }
                    item {
                        // Rangliste der aktuellen Partie
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Aktuelle Rangliste",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                gameData.rankings.forEachIndexed { index, (player, score) ->
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
                                                fontSize = 14.sp
                                            )
                                            PlayerAvatar(player = player, size = 26.dp, showBorder = false)
                                            Text(player.name, fontWeight = FontWeight.Medium)
                                        }
                                        Text(
                                            text = if (score > 0) "+$score" else "$score",
                                            fontWeight = FontWeight.Bold,
                                            color = if (score > 0) PositiveGreen else if (score < 0) NegativeRed else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet zur Rundeneingabe
    if (state.isEntrySheetOpen && gameData != null) {
        RoundEntrySheet(
            state = state,
            viewModel = viewModel,
            gameData = gameData,
            onDismiss = { viewModel.closeEntrySheet() }
        )
    }

    // Zusammenfassungs-Dialog vor Speichern
    if (state.isSummaryDialogOpen && gameData != null) {
        val nextNum = if (state.editingRoundId != null) {
            gameData.roundsWithScores.find { it.round.id == state.editingRoundId }?.round?.roundNumber ?: 1
        } else {
            gameData.roundCount + 1
        }
        val currentBock = if (state.editingRoundId != null) {
            gameData.roundsWithScores.find { it.round.id == state.editingRoundId }?.round?.bockLevel ?: gameData.game.currentBockLevel
        } else {
            gameData.game.currentBockLevel
        }

        RoundSummaryDialog(
            gameData = gameData,
            roundNumber = nextNum,
            rawPoints = viewModel.calculateCurrentRawPoints(),
            bockLevel = currentBock,
            hochzeitPlayerId = state.hochzeitPlayerId,
            sittingOutPlayerId = state.sittingOutPlayerId,
            bockTriggersCount = state.bockTriggersCount,
            isSubmitting = state.isSubmitting,
            gameType = state.gameType,
            soloPlayerId = state.soloPlayerId,
            onConfirm = { viewModel.commitRound() },
            onDismiss = { viewModel.dismissSummaryDialog() }
        )
    }

    // Dialog zum manuellen Bock-Anpassen
    if (state.isBockAdjustDialogOpen) {
        BockAdjustDialog(
            currentLevel = state.bockAdjustLevel,
            currentRemaining = state.bockAdjustRemaining,
            onLevelChange = { viewModel.setAdjustBockLevel(it) },
            onRemainingChange = { viewModel.setAdjustBockRemaining(it) },
            onApply = { viewModel.applyBockAdjustment() },
            onDismiss = { viewModel.closeBockAdjustDialog() }
        )
    }

    // Lösch-Bestätigung für eine Runde
    roundToDelete?.let { roundWithScores ->
        AlertDialog(
            onDismissRequest = { roundToDelete = null },
            title = { Text("Runde ${roundWithScores.round.roundNumber} löschen?") },
            text = { Text("Möchtest du diese Runde wirklich unwiderruflich aus der Partie entfernen? Die Punktestände werden automatisch neu berechnet.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRound(roundWithScores.round.id)
                        roundToDelete = null
                    }
                ) {
                    Text("Löschen", color = NegativeRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { roundToDelete = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Partie beenden Bestätigung
    if (showFinishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showFinishConfirmDialog = false },
            title = { Text("Partie abschließen?") },
            text = { Text("Möchtest du die Partie als beendet markieren? Du kannst sie später in der Historie jederzeit wieder öffnen.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.finishGame()
                        showFinishConfirmDialog = false
                    }
                ) {
                    Text("Beenden", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirmDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Schnelle Hochzeitsanmeldung Dialog
    if (showQuickWeddingDialog && gameData != null) {
        QuickWeddingDialog(
            gameData = gameData,
            onSelectPlayer = { playerId ->
                showQuickWeddingDialog = false
                viewModel.openQuickWeddingEntry(playerId)
            },
            onDismiss = { showQuickWeddingDialog = false }
        )
    }
}

@Composable
private fun LiveScoreboardCard(
    gameData: FullGameData,
    modifier: Modifier = Modifier
) {
    val highestScore = gameData.playerTotals.values.maxOrNull() ?: 0

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Punktestand",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (gameData.game.playerCount == 5 && gameData.currentDealer != null) {
                    Text(
                        text = "Geber: ${gameData.currentDealer?.name}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                gameData.players.forEachIndexed { index, player ->
                    val total = gameData.playerTotals[player.id] ?: 0
                    val isLeader = total == highestScore && total > 0
                    val isCurrentDealer = index == gameData.currentDealerIndex

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            PlayerAvatar(player = player, size = 36.dp)
                            if (isLeader) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD97706)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = "Führender",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = player.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )

                        Text(
                            text = if (total > 0) "+$total" else "$total",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                total > 0 -> PositiveGreen
                                total < 0 -> NegativeRed
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )

                        if (isCurrentDealer) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Geber",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundCard(
    roundWithScores: RoundWithScores,
    gameData: FullGameData,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val round = roundWithScores.round

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Rundennummer, Bock, Hochzeit, Edit & Delete
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
                        text = "Runde ${round.roundNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (round.isSolo) {
                        val soloPlayer = gameData.players.find { it.id == round.soloPlayerId }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "⭐ Solo: ${soloPlayer?.name ?: "Solospieler"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (round.bockLevel > 1) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BockOrange.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Stufe ${round.bockLevel} (${round.bockMultiplier}x)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BockOrange
                            )
                        }
                    }
                    if (!round.isSolo && round.hochzeitPlayerId != null) {
                        val hp = gameData.players.find { it.id == round.hochzeitPlayerId }
                        val weddingGain = (gameData.players.size - 1) * round.bockMultiplier
                        val weddingLoss = round.bockMultiplier
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(WeddingPink.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = WeddingPink, modifier = Modifier.size(10.dp))
                                Text(
                                    text = "💍 Hochzeit: ${hp?.name} (+${weddingGain}/-${weddingLoss})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WeddingPink
                                )
                            }
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen", modifier = Modifier.size(16.dp), tint = NegativeRed)
                    }
                }
            }

            // Teams bei Solospiel deutlich anzeigen
            if (round.isSolo) {
                val soloPlayer = gameData.players.find { it.id == round.soloPlayerId }
                val opponents = gameData.players.filter { it.id != round.soloPlayerId && it.id != round.sittingOutPlayerId }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⭐ Solospieler: ${soloPlayer?.name ?: "-"}  •  Gegenseite: ${opponents.joinToString { it.name }}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Historie-Hinweis: z.B. „Dreifachbock ausgelöst – Bockstufe von 2 auf 5“
            if (round.bockTriggersCount > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(BockOrange.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = BockOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = BockCalculator.formatTriggerHistoryText(
                                round.bockTriggersCount,
                                round.previousBockLevel,
                                round.resultingBockLevel
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BockOrange
                        )
                    }
                }
            }

            // Spieler Punkte dieser Runde mit transparenter Aufschlüsselung
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                gameData.players.forEach { player ->
                    val score = roundWithScores.scores.find { it.playerId == player.id }
                    val finalPts = score?.finalPoints ?: 0
                    val gamePts = score?.gamePoints ?: 0
                    val weddingPts = score?.weddingPoints ?: 0
                    val isSitting = score?.isSittingOut == true || player.id == round.sittingOutPlayerId

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = player.name,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isSitting && weddingPts == 0) "0 (Pause)"
                            else if (finalPts > 0) "+$finalPts"
                            else "$finalPts",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                finalPts > 0 -> PositiveGreen
                                finalPts < 0 -> NegativeRed
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (weddingPts != 0 && gamePts != 0) {
                            Text(
                                text = "${if (gamePts > 0) "+$gamePts" else "$gamePts"} Sp / ${if (weddingPts > 0) "+$weddingPts" else "$weddingPts"} Hz",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (gamePts == 0 && weddingPts != 0) {
                            Text(
                                text = "${if (weddingPts > 0) "+$weddingPts" else "$weddingPts"} Hz",
                                fontSize = 9.sp,
                                color = WeddingPink
                            )
                        }
                    }
                }
            }

            if (round.note.isNotEmpty()) {
                Text(
                    text = "Hinweis: ${round.note}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QuickWeddingDialog(
    gameData: FullGameData,
    onSelectPlayer: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val multiplier = BockCalculator.getMultiplier(gameData.game.currentBockLevel)
    val gain = (gameData.players.size - 1) * multiplier
    val loss = multiplier

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = WeddingPink)
                Text("Hochzeit anmelden", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Wer hat eine Hochzeit angemeldet? Der Hochzeitsspieler erhält +$gain Punkte, alle ${gameData.players.size - 1} Mitspieler je -$loss Punkte (${multiplier}x Bock).",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                gameData.players.forEach { player ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectPlayer(player.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PlayerAvatar(player = player, size = 28.dp, showBorder = false)
                                Text(player.name, fontWeight = FontWeight.Medium)
                            }
                            Text("+$gain Pkt.", fontWeight = FontWeight.Bold, color = PositiveGreen)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
