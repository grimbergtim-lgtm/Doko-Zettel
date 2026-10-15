package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Player

@Composable
fun PlayerAvatar(
    player: Player?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    showBorder: Boolean = true
) {
    val bgColor = player?.color ?: MaterialTheme.colorScheme.surfaceVariant
    val symbol = player?.avatarSymbol ?: "♠"
    val initial = player?.name?.take(1)?.uppercase() ?: "?"

    val fontSize = (size.value * 0.45).sp

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .then(
                if (showBorder) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = CircleShape
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (symbol.isNotEmpty() && symbol != " ") symbol else initial,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}
