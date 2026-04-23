package com.morseglyph.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MorseColorScheme = darkColorScheme(
    background = Black,
    surface = NothingSurface,
    onBackground = NothingAccent,
    onSurface = NothingAccent,
    primary = NothingAccent,
    onPrimary = Black,
    secondary = NothingInactive,
    onSecondary = NothingAccent,
    outline = NothingBorder,
    error = NothingError,
    onError = Black
)

@Composable
fun MorseGlyphTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MorseColorScheme,
        typography = MorseTypography,
        content = content
    )
}
