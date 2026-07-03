package com.sim.lavis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TerminalColorScheme = darkColorScheme(
    primary = TermGreen,
    onPrimary = TermBlack,
    secondary = TermAmber,
    onSecondary = TermBlack,
    tertiary = TermCyan,
    onTertiary = TermBlack,
    background = TermBlack,
    onBackground = TermGreen,
    surface = TermBlack,
    onSurface = TermGreen,
    surfaceVariant = TermSurface,
    onSurfaceVariant = TermGray,
    outline = TermDarkLine,
    error = TermRed,
    onError = TermBlack
)

@Composable
fun LavisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TerminalColorScheme,
        typography = Typography,
        content = content
    )
}
