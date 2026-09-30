package com.sim.lavis.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System sans (MiSans on HyperOS, Roboto elsewhere) — tight tracking on large sizes.
private fun sans(size: Int, weight: FontWeight, lineHeight: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp
)

val Typography = Typography(
    displayLarge = sans(48, FontWeight.Black, 52, -1.5),
    displayMedium = sans(40, FontWeight.Black, 44, -1.2),
    displaySmall = sans(34, FontWeight.ExtraBold, 40, -1.0),
    headlineLarge = sans(30, FontWeight.ExtraBold, 36, -0.8),
    headlineMedium = sans(26, FontWeight.Bold, 32, -0.6),
    headlineSmall = sans(22, FontWeight.Bold, 28, -0.4),
    titleLarge = sans(20, FontWeight.Bold, 26, -0.2),
    titleMedium = sans(16, FontWeight.SemiBold, 22),
    titleSmall = sans(14, FontWeight.SemiBold, 20),
    bodyLarge = sans(16, FontWeight.Normal, 22),
    bodyMedium = sans(14, FontWeight.Normal, 20),
    bodySmall = sans(12, FontWeight.Normal, 16),
    labelLarge = sans(14, FontWeight.SemiBold, 20),
    labelMedium = sans(12, FontWeight.Medium, 16, 0.2),
    labelSmall = sans(11, FontWeight.SemiBold, 14, 1.2)
)
