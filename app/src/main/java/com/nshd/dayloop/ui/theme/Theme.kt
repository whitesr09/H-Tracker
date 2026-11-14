package com.nshd.dayloop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DayLoopDarkColors = darkColorScheme(
    primary = AcidGreen,
    onPrimary = Color.Black,
    background = InkBlack,
    onBackground = OffWhite,
    surface = TileDark,
    onSurface = OffWhite,
    surfaceVariant = TileRaised,
    onSurfaceVariant = MutedText,
    outline = TileBorder
)

private val DayLoopLightColors = lightColorScheme(
    primary = AcidGreen,
    onPrimary = Color.Black,
    background = PaperBg,
    onBackground = InkText,
    surface = PaperTile,
    onSurface = InkText,
    surfaceVariant = PaperRaised,
    onSurfaceVariant = MutedInk,
    outline = PaperBorder
)

@Composable
fun DayLoopTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DayLoopDarkColors else DayLoopLightColors,
        typography = DayLoopTypography,
        content = content
    )
}
