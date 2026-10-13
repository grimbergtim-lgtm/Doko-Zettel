package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Player
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScoreLineChart(
    progression: Map<Long, List<Int>>,
    players: List<Player>,
    modifier: Modifier = Modifier
) {
    if (players.isEmpty() || progression.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
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
                .height(180.dp),
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
    val zeroLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(16.dp)
    ) {
        Text(
            text = "Punkteverlauf über alle Runden",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Legende
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            players.forEach { player ->
                val lastScore = progression[player.id]?.lastOrNull() ?: 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(player.colorHex))
                    )
                    Text(
                        text = "${player.name}: ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (lastScore > 0) "+$lastScore" else "$lastScore",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (lastScore > 0) Color(0xFF16A34A) else if (lastScore < 0) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scrollbarer Canvas, wenn viele Runden gespielt wurden
        val stepWidth = 44.dp
        val totalWidth = maxOf(300.dp, stepWidth * maxRounds)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Canvas(
                modifier = Modifier
                    .width(totalWidth)
                    .height(200.dp)
                    .padding(top = 10.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val pointCount = maxRounds
                val xSpacing = if (pointCount > 1) canvasWidth / (pointCount - 1) else canvasWidth

                // Null-Linie
                val zeroY = canvasHeight - ((0 - minY).toFloat() / yRange.toFloat()) * canvasHeight
                drawLine(
                    color = zeroLineColor,
                    start = Offset(0f, zeroY),
                    end = Offset(canvasWidth, zeroY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Rundenbeschriftung & vertikale Rasterlinien
                for (i in 0 until pointCount) {
                    val x = i * xSpacing
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, canvasHeight),
                        strokeWidth = 1f
                    )
                }

                // Linien für jeden Spieler zeichnen
                players.forEach { player ->
                    val series = progression[player.id] ?: return@forEach
                    val playerColor = Color(player.colorHex)
                    val path = Path()

                    series.forEachIndexed { index, score ->
                        val x = index * xSpacing
                        val y = canvasHeight - ((score - minY).toFloat() / yRange.toFloat()) * canvasHeight
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
                            width = 3.5f,
                            cap = StrokeCap.Round
                        )
                    )

                    // Datenpunkte zeichnen
                    series.forEachIndexed { index, score ->
                        val x = index * xSpacing
                        val y = canvasHeight - ((score - minY).toFloat() / yRange.toFloat()) * canvasHeight
                        drawCircle(
                            color = Color.White,
                            radius = 4.5f,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = playerColor,
                            radius = 3f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}
