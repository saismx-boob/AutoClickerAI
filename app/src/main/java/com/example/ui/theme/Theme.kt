package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = NaturalForestGreen,
    onPrimary = Color.White,
    primaryContainer = NaturalSageLight,
    onPrimaryContainer = NaturalForestGreen,

    secondary = NaturalForestGreenLight,
    onSecondary = Color.White,
    secondaryContainer = NaturalOlivePill,
    onSecondaryContainer = NaturalTextPrimary,

    tertiary = NaturalAmberWarm,
    onTertiary = Color.White,
    tertiaryContainer = NaturalAmberLight,
    onTertiaryContainer = NaturalAmberDark,

    background = NaturalBackground,
    onBackground = NaturalTextPrimary,

    surface = NaturalSurface,
    onSurface = NaturalTextPrimary,
    surfaceVariant = NaturalSurfaceVariant,
    onSurfaceVariant = NaturalTextSecondary,

    outline = NaturalCardBorder,
    outlineVariant = NaturalOliveBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = NaturalForestGreenLight,
    onPrimary = Color(0xFF0E1F07),
    primaryContainer = NaturalSageDark,
    onPrimaryContainer = NaturalSageLight,

    secondary = NaturalAmberWarm,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3E2000),
    onSecondaryContainer = NaturalAmberLight,

    tertiary = NaturalSageLight,
    onTertiary = Color(0xFF0E1F07),
    tertiaryContainer = NaturalForestGreenDark,
    onTertiaryContainer = NaturalSageLight,

    background = NaturalDarkBackground,
    onBackground = Color(0xFFE2E4DC),

    surface = NaturalDarkSurface,
    onSurface = Color(0xFFE2E4DC),
    surfaceVariant = NaturalDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC5C8BA),

    outline = NaturalDarkCardBorder,
    outlineVariant = Color(0xFF283023)
)

private val AmoledColorScheme = DarkColorScheme.copy(
    background = AmoledPureBlack,
    surface = Color(0xFF0C0F0A),
    surfaceVariant = Color(0xFF141911)
)

@Composable
fun AutoClickerTheme(
    isAmoledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isAmoledMode) AmoledColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

