package com.sim.lavis.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// "Tokyo by night": near-black indigo surfaces, neon accents.
val Night = Color(0xFF0A0A12)
val NightSurface = Color(0xFF12121C)
val NightSurfaceHigh = Color(0xFF1B1B29)
val NightSurfaceHighest = Color(0xFF262638)
val NightOutline = Color(0xFF2E2E44)

val TextPrimary = Color(0xFFF4F2FF)
val TextMuted = Color(0xFF9A98B5)
val TextFaint = Color(0xFF5F5D78)

val NeonPink = Color(0xFFFF4D8D)
val NeonPinkDeep = Color(0xFF3A1428)
val NeonCyan = Color(0xFF4DE1FF)
val NeonViolet = Color(0xFFA78BFA)
val Danger = Color(0xFFFF6B6B)

/** Cover gradients for songs / playlists / singers without artwork. */
private val coverGradients = listOf(
    Color(0xFFFF4D8D) to Color(0xFF6A2BD9),
    Color(0xFF4DE1FF) to Color(0xFF3A47D5),
    Color(0xFFFFA14D) to Color(0xFFE23E57),
    Color(0xFFA78BFA) to Color(0xFF2E1A6B),
    Color(0xFF2EE59D) to Color(0xFF136A8A),
    Color(0xFFFF6FD8) to Color(0xFF3813C2),
    Color(0xFFFFD166) to Color(0xFFEF476F),
    Color(0xFF5EEAD4) to Color(0xFF6D28D9)
)

/** Stable per-name gradient, so a playlist or singer always gets the same cover. */
fun coverGradient(key: String): Brush {
    val (a, b) = coverGradients[Math.floorMod(key.hashCode(), coverGradients.size)]
    return Brush.linearGradient(listOf(a, b))
}

/** The first color of [key]'s gradient; used to tint backgrounds around a cover. */
fun coverAccent(key: String): Color =
    coverGradients[Math.floorMod(key.hashCode(), coverGradients.size)].first
