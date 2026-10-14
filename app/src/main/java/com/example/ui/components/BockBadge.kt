package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BockCalculator
import com.example.ui.theme.BockOrange

@Composable
fun BockBadge(
    level: Int,
    remainingRounds: Int = 0,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val safeLevel = BockCalculator.coerceLevel(level)
    val multiplier = BockCalculator.getMultiplier(safeLevel)
    val isBockActive = safeLevel > 1 || remainingRounds > 0

    val bgColor = if (isBockActive) BockOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isBockActive) BockOrange else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isBockActive) BockOrange else Color.Transparent

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (safeLevel >= 4) Icons.Default.ElectricBolt else Icons.Default.LocalFireDepartment,
                contentDescription = "Bock-Status",
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = BockCalculator.getBockName(multiplier),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "(×${multiplier})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }
                if (remainingRounds > 0) {
                    Text(
                        text = "$remainingRounds ${if (remainingRounds == 1) "Runde" else "Runden"} übrig",
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
