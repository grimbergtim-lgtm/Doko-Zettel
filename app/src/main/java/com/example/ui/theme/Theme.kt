package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ForestGreenPrimaryDark,
    onPrimary = ForestGreenOnPrimaryDark,
    primaryContainer = ForestGreenPrimaryContainerDark,
    onPrimaryContainer = ForestGreenOnPrimaryContainerDark,
    secondary = AmberGoldSecondaryDark,
    onSecondary = AmberGoldOnSecondaryDark,
    secondaryContainer = AmberGoldSecondaryContainerDark,
    onSecondaryContainer = AmberGoldOnSecondaryContainerDark,
    tertiary = CardCrimsonTertiaryDark,
    onTertiary = CardCrimsonOnTertiaryDark,
    tertiaryContainer = CardCrimsonTertiaryContainerDark,
    onTertiaryContainer = CardCrimsonOnTertiaryContainerDark,
    background = NeutralDarkBackground,
    surface = NeutralDarkSurface,
    surfaceVariant = NeutralDarkSurfaceVariant,
    onBackground = NeutralDarkOnSurface,
    onSurface = NeutralDarkOnSurface,
    onSurfaceVariant = NeutralDarkOnSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreenPrimary,
    onPrimary = ForestGreenOnPrimary,
    primaryContainer = ForestGreenPrimaryContainer,
    onPrimaryContainer = ForestGreenOnPrimaryContainer,
    secondary = AmberGoldSecondary,
    onSecondary = AmberGoldOnSecondary,
    secondaryContainer = AmberGoldSecondaryContainer,
    onSecondaryContainer = AmberGoldOnSecondaryContainer,
    tertiary = CardCrimsonTertiary,
    onTertiary = CardCrimsonOnTertiary,
    tertiaryContainer = CardCrimsonTertiaryContainer,
    onTertiaryContainer = CardCrimsonOnTertiaryContainer,
    background = NeutralLightBackground,
    surface = NeutralLightSurface,
    surfaceVariant = NeutralLightSurfaceVariant,
    onBackground = NeutralLightOnSurface,
    onSurface = NeutralLightOnSurface,
    onSurfaceVariant = NeutralLightOnSurfaceVariant
)

@Composable
fun DoppelkopfTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve card felt aesthetic by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
