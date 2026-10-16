package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FullGameData
import com.example.data.model.GameType
import com.example.data.model.Player
import kotlin.math.max
import kotlin.math.min

/**
 * Findet die zugehörige Activity ausgehend vom aktuellen Compose-Context.
 */
private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Vollbildansicht für das Liniendiagramm „Punkteverlauf über alle Runden“.
 * Dreht das Gerät automatisch ins Querformat (Landscape) und stellt die
 * vorherige Ausrichtung beim Verlassen zuverlässig wieder her.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScoreLineChartFullscreenView(
    gameData: FullGameData,
    chartFilter: GameType?,
    onFilterChange: (GameType?) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val originalOrientation = remember {
        activity?.requestedOrientation?.let {
            if (it == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE ||
                it == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            ) {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } else {
                it
            }
        } ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    // Automatisch ins Querformat drehen und beim Schließen wiederherstellen
    DisposableEffect(activity) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    // Android-System-Zurücktaste schließt den Vollbildmodus
    BackHandler {
        onClose()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("score_line_chart_fullscreen_surface"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Kopfleiste im Vollbildmodus
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_chart_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Vollbildmodus beenden"
                        )
                    }
                    Text(
                        text = "Punkteverlauf über alle Runden",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Filterchips & Schließen-Button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = chartFilter == null,
                        onClick = { onFilterChange(null) },
                        label = { Text("Alle Runden", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = chartFilter == GameType.NORMAL,
                        onClick = { onFilterChange(GameType.NORMAL) },
                        label = { Text("Nur Normale", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = chartFilter == GameType.SOLO,
                        onClick = { onFilterChange(GameType.SOLO) },
                        label = { Text("⭐ Nur Solos", fontSize = 12.sp) }
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("exit_fullscreen_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Vollbild beenden",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Vollbild-Diagramm nutzt den gesamten verfügbaren Bildschirmplatz
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ScoreLineChart(
                    progression = gameData.getCumulativeScoreProgression(filterGameType = chartFilter),
                    players = gameData.players,
                    isFullscreen = true,
                    onCloseFullscreen = onClose,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Liniendiagramm für den Punkteverlauf über alle Runden.
 * Unterstützt sowohl den eingebetteten Modus im Reiter „Graph“ als auch die
 * formatfüllende Vollbildansicht im Querformat.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScoreLineChart(
    progression: Map<Long, List<Int>>,
    players: List<Player>,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    onOpenFullscreen: (() -> Unit)? = null,
    onCloseFullscreen: (() -> Unit)? = null
) {
    if (players.isEmpty() || progression.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(if (isFullscreen) 240.dp else 180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Noch keine Runden für die Punkteentwicklung vorhanden",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxRounds = progression.values.maxOfOrNull { it.size } ?: 0
    if (maxRounds <= 1) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(if (isFullscreen) 240.dp else 180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nach der ersten gespielten Runde erscheint hier das Liniendiagramm.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val allValues = progression.values.flatten()
    val rawMin = allValues.minOrNull() ?: 0
    val rawMax = allValues.maxOrNull() ?: 0

    // Puffer oben und unten
    val paddingSpan = max(5, ((rawMax - rawMin) * 0.15f).toInt())
    val minY = min(0, rawMin - paddingSpan)
    val maxY = max(0, rawMax + paddingSpan)
    val yRange = if (maxY - minY == 0) 10 else (maxY - minY)

    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val zeroLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val textMeasurer = rememberTextMeasurer()

    val axisTextStyle = TextStyle(
        fontSize = if (isFullscreen) 11.sp else 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Normal
    )

    val containerModifier = if (isFullscreen) {
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    } else {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(16.dp)
    }

    Column(modifier = containerModifier) {
        // Im normalen Modus: Titelzeile mit Vollbild-Schaltfläche
        if (!isFullscreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Punkteverlauf über alle Runden",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (onOpenFullscreen != null && maxRounds > 1) {
                    IconButton(
                        onClick = onOpenFullscreen,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("open_chart_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Vollbildmodus aktivieren",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Legende
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isFullscreen) 16.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (isFullscreen) 4.dp else 8.dp)
        ) {
            players.forEach { player ->
                val lastScore = progression[player.id]?.lastOrNull() ?: 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isFullscreen) 12.dp else 10.dp)
                            .clip(CircleShape)
                            .background(player.color)
                    )
                    Text(
                        text = "${player.name}: ",
                        fontSize = if (isFullscreen) 13.sp else 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (lastScore > 0) "+$lastScore" else "$lastScore",
                        fontSize = if (isFullscreen) 13.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (lastScore > 0) Color(0xFF16A34A) else if (lastScore < 0) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isFullscreen) 8.dp else 14.dp))

        // Diagramm-Bereich
        val canvasContainerModifier = if (isFullscreen) {
            Modifier
                .fillMaxWidth()
                .weight(1f)
        } else {
            Modifier
                .fillMaxWidth()
                .height(210.dp)
        }

        BoxWithConstraints(modifier = canvasContainerModifier) {
            val availableWidth = maxWidth
            val availableHeight = maxHeight

            // In Vollbild streckt sich der Schritt dynamisch über die Bildschirmbreite
            val pointCount = maxRounds
            val minStep = if (isFullscreen) 56.dp else 44.dp
            val dynamicStep = if (pointCount > 1) {
                maxOf(minStep, (availableWidth - 56.dp) / (pointCount - 1))
            } else {
                minStep
            }
            val totalWidth = maxOf(availableWidth, dynamicStep * maxOf(1, pointCount - 1) + 56.dp)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
            ) {
                Canvas(
                    modifier = Modifier
                        .width(totalWidth)
                        .height(availableHeight)
                        .padding(top = 10.dp, bottom = 26.dp, start = 32.dp, end = 24.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val topMargin = 12f
                    val bottomMargin = 28f
                    val plotHeight = (canvasHeight - topMargin - bottomMargin).coerceAtLeast(10f)

                    val xSpacing = if (pointCount > 1) canvasWidth / (pointCount - 1) else canvasWidth

                    // Null-Linie
                    val zeroY = topMargin + plotHeight - ((0 - minY).toFloat() / yRange.toFloat()) * plotHeight
                    drawLine(
                        color = zeroLineColor,
                        start = Offset(0f, zeroY),
                        end = Offset(canvasWidth, zeroY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                    // Null-Linie Beschriftung "0"
                    val zeroLayout = textMeasurer.measure("0", axisTextStyle)
                    drawText(
                        textLayoutResult = zeroLayout,
                        topLeft = Offset(4f, zeroY - zeroLayout.size.height / 2f)
                    )

                    // Obere Begrenzung / Max-Score Linie
                    if (maxY > 0) {
                        val maxLabel = "+$maxY"
                        val maxLayout = textMeasurer.measure(maxLabel, axisTextStyle)
                        drawText(
                            textLayoutResult = maxLayout,
                            topLeft = Offset(4f, topMargin - maxLayout.size.height / 2f)
                        )
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, topMargin),
                            end = Offset(canvasWidth, topMargin),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    // Untere Begrenzung / Min-Score Linie
                    if (minY < 0) {
                        val minLabel = "$minY"
                        val minLayout = textMeasurer.measure(minLabel, axisTextStyle)
                        drawText(
                            textLayoutResult = minLayout,
                            topLeft = Offset(4f, topMargin + plotHeight - minLayout.size.height / 2f)
                        )
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, topMargin + plotHeight),
                            end = Offset(canvasWidth, topMargin + plotHeight),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    // Rundenbeschriftung & vertikale Rasterlinien
                    for (i in 0 until pointCount) {
                        val x = i * xSpacing
                        drawLine(
                            color = gridColor,
                            start = Offset(x, topMargin),
                            end = Offset(x, topMargin + plotHeight),
                            strokeWidth = 1f
                        )

                        val roundLabel = if (i == 0) "Start" else "R$i"
                        val roundLayout = textMeasurer.measure(roundLabel, axisTextStyle)
                        drawText(
                            textLayoutResult = roundLayout,
                            topLeft = Offset(
                                x - roundLayout.size.width / 2f,
                                topMargin + plotHeight + 6f
                            )
                        )
                    }

                    // Linien für jeden Spieler zeichnen
                    players.forEach { player ->
                        val series = progression[player.id] ?: return@forEach
                        val playerColor = player.color
                        val path = Path()

                        series.forEachIndexed { index, score ->
                            val x = index * xSpacing
                            val y = topMargin + plotHeight - ((score - minY).toFloat() / yRange.toFloat()) * plotHeight
                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        // Pfad zeichnen
                        drawPath(
                            path = path,
                            color = playerColor,
                            style = Stroke(
                                width = if (isFullscreen) 4f else 3.5f,
                                cap = StrokeCap.Round
                            )
                        )

                        // Datenpunkte zeichnen
                        series.forEachIndexed { index, score ->
                            val x = index * xSpacing
                            val y = topMargin + plotHeight - ((score - minY).toFloat() / yRange.toFloat()) * plotHeight
                            drawCircle(
                                color = Color.White,
                                radius = if (isFullscreen) 5f else 4.5f,
                                center = Offset(x, y)
                            )
                            drawCircle(
                                color = playerColor,
                                radius = if (isFullscreen) 3.5f else 3f,
                                center = Offset(x, y)
                            )

                            // Im Vollbild: Endstand am letzten Punkt gut sichtbar anzeigen
                            if (isFullscreen && index == series.size - 1) {
                                val scoreText = if (score > 0) "+$score" else "$score"
                                val scoreLayout = textMeasurer.measure(
                                    scoreText,
                                    axisTextStyle.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = playerColor,
                                        fontSize = 11.sp
                                    )
                                )
                                val textY = (y - scoreLayout.size.height - 4f).coerceAtLeast(0f)
                                drawText(
                                    textLayoutResult = scoreLayout,
                                    topLeft = Offset(x - scoreLayout.size.width / 2f, textY)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
