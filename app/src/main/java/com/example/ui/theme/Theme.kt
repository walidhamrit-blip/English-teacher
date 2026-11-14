package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = DeepEmerald,
    onPrimary = Color.White,
    primaryContainer = SoftSage,
    onPrimaryContainer = DeepEmeraldDark,
    secondary = SunlitGold,
    onSecondary = Color.White,
    secondaryContainer = SunlitGoldLight,
    onSecondaryContainer = Color(0xFF4B2B00),
    tertiary = Terracotta,
    onTertiary = Color.White,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = Color(0xFF410E00),
    background = CreamCanvas,
    onBackground = InkCharcoal,
    surface = CreamCard,
    onSurface = InkCharcoal,
    surfaceVariant = CreamVariant,
    onSurfaceVariant = InkMuted,
    outline = OutlineWarm,
    error = Terracotta,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = MintPrimary,
    onPrimary = Color(0xFF003830),
    primaryContainer = SoftSageDark,
    onPrimaryContainer = Color(0xFFB4F1E5),
    secondary = SunlitGoldDark,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF5C3A08),
    onSecondaryContainer = Color(0xFFFFDEA8),
    tertiary = Color(0xFFFFB4A1),
    onTertiary = Color(0xFF641502),
    tertiaryContainer = Color(0xFF832711),
    onTertiaryContainer = Color(0xFFFFDAD2),
    background = NightCanvas,
    onBackground = MintInk,
    surface = NightCard,
    onSurface = MintInk,
    surfaceVariant = NightVariant,
    onSurfaceVariant = MintMuted,
    outline = OutlineNight,
    error = Color(0xFFFFB4A1),
    onError = Color(0xFF641502)
)

private val NoorShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun NoorEnglishTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NoorTypography,
        shapes = NoorShapes,
        content = content
    )
}
