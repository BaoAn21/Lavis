package com.sim.lavis.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.sim.lavis.ui.theme.TermGray
import com.sim.lavis.ui.theme.TermGreenDim
import java.util.Locale

/** "$ title" prompt line with a blinking block cursor. */
@Composable
fun PromptHeader(text: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )
    Row(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = "$ ",
            style = MaterialTheme.typography.titleLarge,
            color = TermGreenDim
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "█",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.alpha(alpha)
        )
    }
}

/** ASCII progress bar like [######----------]. */
fun asciiBar(fraction: Float, width: Int = 20): String {
    val f = fraction.coerceIn(0f, 1f)
    val filled = (f * width).toInt()
    return "[" + "#".repeat(filled) + "-".repeat(width - filled) + "]"
}

fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}

/** "2h 14m" style, for wrapped totals. */
fun formatDurationLong(ms: Long): String {
    val totalMin = ms / 60000
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m"
        else -> "${ms / 1000}s"
    }
}

@Composable
fun DimText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = TermGray,
        modifier = modifier
    )
}
