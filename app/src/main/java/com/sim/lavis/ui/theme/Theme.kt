package com.sim.lavis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NightColorScheme = darkColorScheme(
    primary = NeonPink,
    onPrimary = Night,
    primaryContainer = NeonPinkDeep,
    onPrimaryContainer = NeonPink,
    secondary = NeonCyan,
    onSecondary = Night,
    tertiary = NeonViolet,
    onTertiary = Night,
    background = Night,
    onBackground = TextPrimary,
    surface = Night,
    onSurface = TextPrimary,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = TextMuted,
    surfaceContainerLowest = Night,
    surfaceContainerLow = NightSurface,
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightSurfaceHigh,
    surfaceContainerHighest = NightSurfaceHighest,
    outline = NightOutline,
    outlineVariant = NightOutline,
    error = Danger,
    onError = Night
)

@Composable
fun LavisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NightColorScheme,
        typography = Typography,
        content = content
    )
}
