package com.sim.lavis.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun mono(size: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = weight,
    fontSize = size.sp
)

val Typography = Typography(
    displayLarge = mono(34, FontWeight.Bold),
    displayMedium = mono(28, FontWeight.Bold),
    displaySmall = mono(24, FontWeight.Bold),
    headlineLarge = mono(22, FontWeight.Bold),
    headlineMedium = mono(20, FontWeight.Bold),
    headlineSmall = mono(18, FontWeight.Bold),
    titleLarge = mono(18, FontWeight.Bold),
    titleMedium = mono(16, FontWeight.Bold),
    titleSmall = mono(14, FontWeight.Bold),
    bodyLarge = mono(15),
    bodyMedium = mono(14),
    bodySmall = mono(12),
    labelLarge = mono(14, FontWeight.Bold),
    labelMedium = mono(12),
    labelSmall = mono(11)
)
