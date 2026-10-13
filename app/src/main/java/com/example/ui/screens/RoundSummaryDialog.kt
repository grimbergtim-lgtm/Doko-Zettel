package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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

@Composable
fun RoundSummaryDialog(
    gameData: FullGameData,
    roundNumber: Int,
    rawPoints: Map<Long, Int>,
    bockLevel: Int,
    hochzeitPlayerId: Long?,
    sittingOutPlayerId: Long?,
    bockTriggersCount: Int = 0,
    isSubmitting: Boolean,
    gameType: GameType = GameType.NORMAL,
    soloPlayerId: Long? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val multiplier = BockCalculator.getMultiplier(bockLevel)

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Column {
                Text(
                    text = "Zusammenfassung Runde $roundNumber",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (gameType == GameType.SOLO) "Solospiel prüfen & bestätigen" else "Bitte vor dem Verbuchen kurz prüfen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Solo-Banner
                if (gameType == GameType.SOLO) {
                    val soloPlayer = gameData.players.find { it.id == soloPlayerId }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "⭐ Solospiel: ${soloPlayer?.name ?: "Solospieler"} spielt allein gegen alle anderen",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Bock- und Hochzeitsinfo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = BockOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Gespielt mit Stufe $bockLevel (${multiplier}x)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = BockOrange
                        )
                    }

                    if (gameType == GameType.NORMAL && hochzeitPlayerId != null) {
                        val hochzeitPlayer = gameData.players.find { it.id == hochzeitPlayerId }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = WeddingPink,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Hochzeit: ${hochzeitPlayer?.name ?: ""}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = WeddingPink
                            )
                        }
                    }
                }

                // Vorschau auf neue Bockauslöser
                if (bockTriggersCount > 0) {
                    val resLevel = BockCalculator.calculateResultingLevel(bockLevel, bockTriggersCount)
                    val resMult = BockCalculator.getMultiplier(resLevel)
                    val roundsToAdd = bockTriggersCount * gameData.game.bockRoundsPerTrigger
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BockOrange.copy(alpha = 0.12f))
                            .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "🔥 ${BockCalculator.getTriggerName(bockTriggersCount)} ausgelöst!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BockOrange
                            )
                            Text(
                                text = "Bockstufe steigt von $bockLevel auf $resLevel (${resMult}x Multiplikator ab nächster Runde, +$roundsToAdd Bockrunden)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                HorizontalDivider()

                val totalPlayerCount = gameData.players.size
                val weddingGains = (totalPlayerCount - 1) * multiplier
                val weddingLoss = -1 * multiplier

                if (hochzeitPlayerId != null) {
                    val hp = gameData.players.find { it.id == hochzeitPlayerId }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(WeddingPink.copy(alpha = 0.12f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Hochzeitswertung: ${hp?.name} erhält +$weddingGains Pkt. (von jedem Gegner +$multiplier Pkt.), alle anderen Spieler je -$multiplier Pkt.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WeddingPink
                        )
                    }
                }

                // Spieler-Tabelle mit Punkten
                gameData.players.forEach { player ->
                    val raw = rawPoints[player.id] ?: 0
                    val isSitting = player.id == sittingOutPlayerId
                    val gamePts = if (isSitting) 0 else raw * multiplier
                    val weddingPts = if (hochzeitPlayerId != null) {
                        if (player.id == hochzeitPlayerId) weddingGains else weddingLoss
                    } else 0
                    val finalPts = gamePts + weddingPts
                    val currentTotal = gameData.playerTotals[player.id] ?: 0
                    val newTotal = currentTotal + finalPts

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSitting && weddingPts == 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .padding(vertical = 6.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlayerAvatar(player = player, size = 28.dp, showBorder = false)
                            Column {
                                Text(
                                    text = player.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                if (gameType == GameType.SOLO) {
                                    if (player.id == soloPlayerId) {
                                        Text(
                                            text = "⭐ Solospieler",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (isSitting) {
                                        Text(
                                            text = "Setzt aus",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = "Gegenspieler",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else if (isSitting) {
                                    Text(
                                        text = "Setzt aus",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val ptsText = when {
                                hochzeitPlayerId != null && gamePts != 0 -> {
                                    "${if (gamePts > 0) "+$gamePts" else "$gamePts"} Spiel, ${if (weddingPts > 0) "+$weddingPts" else "$weddingPts"} Hochzeit = ${if (finalPts > 0) "+$finalPts" else "$finalPts"} Pkt."
                                }
                                hochzeitPlayerId != null && gamePts == 0 -> {
                                    "Hochzeit: ${if (weddingPts > 0) "+$weddingPts" else "$weddingPts"} Pkt."
                                }
                                isSitting -> "0 Pkt."
                                finalPts > 0 -> "+$finalPts Pkt. ($raw * ${multiplier}x)"
                                else -> "$finalPts Pkt. ($raw * ${multiplier}x)"
                            }
                            Text(
                                text = ptsText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = when {
                                    finalPts > 0 -> PositiveGreen
                                    finalPts < 0 -> NegativeRed
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = "Neu: $newTotal Pkt.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSubmitting,
                modifier = Modifier.testTag("confirm_round_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Speichern...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Runde buchen")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
                modifier = Modifier.testTag("dismiss_summary_button")
            ) {
                Text("Korrektur")
            }
        }
    )
}

@Composable
fun BockAdjustDialog(
    currentLevel: Int,
    currentRemaining: Int,
    onLevelChange: (Int) -> Unit,
    onRemainingChange: (Int) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val multiplier = BockCalculator.getMultiplier(currentLevel)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = BockOrange
                )
                Text(
                    text = "Bockstufen anpassen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Hier kannst du die aktuelle Bockphase manuell korrigieren oder aktivieren.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Stufe Wähler (1..8)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Bockstufe:",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Stufe $currentLevel -> ${multiplier}x Multiplikator",
                            fontWeight = FontWeight.Bold,
                            color = BockOrange,
                            fontSize = 14.sp
                        )
                    }
                    Slider(
                        value = currentLevel.toFloat(),
                        onValueChange = { onLevelChange(it.toInt()) },
                        valueRange = 1f..8f,
                        steps = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Stufe 1 (1x)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Stufe 8 (128x)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Verbleibende Runden
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Verbleibende Runden:",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "$currentRemaining Runden",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Slider(
                        value = currentRemaining.toFloat(),
                        onValueChange = { onRemainingChange(it.toInt()) },
                        valueRange = 0f..20f,
                        steps = 19,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApply,
                modifier = Modifier.testTag("apply_bock_button")
            ) {
                Text("Übernehmen")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
