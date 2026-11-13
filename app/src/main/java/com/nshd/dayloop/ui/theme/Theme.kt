package com.nshd.dayloop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DayLoopColors = darkColorScheme(
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

@Composable
fun DayLoopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DayLoopColors,
        typography = DayLoopTypography,
        content = content
    )
}
