package com.sim.lavis.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sim.lavis.data.Wrapped
import com.sim.lavis.data.WrappedPeriod
import com.sim.lavis.ui.WrappedViewModel
import com.sim.lavis.ui.components.EmptyState
import com.sim.lavis.ui.components.InitialsAvatar
import com.sim.lavis.ui.components.LavisIcons
import com.sim.lavis.ui.components.ScreenHeader
import com.sim.lavis.ui.components.formatDurationLong
import com.sim.lavis.ui.theme.NeonPink
import com.sim.lavis.ui.theme.NeonViolet
import com.sim.lavis.ui.theme.TextFaint
import com.sim.lavis.ui.theme.TextMuted
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WrappedScreen(viewModel: WrappedViewModel) {
    val period by viewModel.period.collectAsState()
    val offset by viewModel.offset.collectAsState()
    val wrapped by viewModel.wrapped.collectAsState()

    // Refresh when the screen comes back into view (new events may exist).
    LaunchedEffect(Unit) { viewModel.recompute() }

    val context = LocalContext.current
    // SAF "create document" launcher: the user picks where the CSV goes (no storage permission needed).
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportCsv(context.contentResolver, uri) { count ->
                val msg = if (count != null) "Exported $count listens to CSV" else "Export failed"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        ScreenHeader(title = "Wrapped", eyebrow = "Your listening") {
            IconButton(onClick = { exportLauncher.launch("lavis-history-${LocalDate.now()}.csv") }) {
                Icon(LavisIcons.Download, contentDescription = "Export history as CSV")
            }
        }

        PeriodSelector(
            selected = period,
            onSelect = viewModel::setPeriod,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = viewModel::previousPeriod) {
                Icon(LavisIcons.ChevronLeft, contentDescription = "Previous period")
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = periodTitle(period, offset),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                wrapped?.let {
                    Text(periodRange(it), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
            IconButton(onClick = viewModel::nextPeriod, enabled = offset > 0) {
                Icon(
                    LavisIcons.ChevronRight,
                    contentDescription = "Next period",
                    tint = if (offset > 0) MaterialTheme.colorScheme.onSurface else TextFaint.copy(alpha = 0.4f)
                )
            }
        }

        wrapped?.let { w ->
            if (w.playCount == 0) {
                EmptyState(
                    icon = LavisIcons.Headphones,
                    title = "Nothing here yet",
                    message = "Listens longer than 5 seconds show up here. Go play something!"
                )
            } else {
                WrappedBody(w)
            }
        }
    }
}

@Composable
private fun PeriodSelector(selected: WrappedPeriod, onSelect: (WrappedPeriod) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        WrappedPeriod.entries.forEach { p ->
            val isSelected = p == selected
            val bg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "segment"
            )
            Text(
                text = p.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .clickable { onSelect(p) }
                    .padding(vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun WrappedBody(w: Wrapped) {
    val peakDay = w.byDayOfWeek.maxByOrNull { it.second }?.takeIf { it.second > 0 }
    val peakHour = w.byHour.maxByOrNull { it.second }?.takeIf { it.second > 0 }

    // Hero: total time on a neon gradient.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(NeonPink, NeonViolet)))
            .padding(24.dp)
    ) {
        Text("TIME LISTENED", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
        Text(
            text = formatDurationLong(w.totalListenedMs),
            style = MaterialTheme.typography.displayMedium,
            color = Color.White
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            HeroStat("Plays", w.playCount.toString())
            peakDay?.let { HeroStat("Top day", fullDayName(it.first)) }
            peakHour?.let { HeroStat("Peak hour", hourLabel(it.first)) }
        }
    }

    WrappedCard("Top songs") {
        if (w.topSongs.isEmpty()) MutedLine("None yet")
        w.topSongs.forEachIndexed { i, s ->
            RankRow(
                rank = i + 1,
                title = s.title,
                subtitle = "${s.playCount} plays · ${s.playlist}",
                trailing = formatDurationLong(s.totalMs)
            )
        }
    }

    WrappedCard("Top singers") {
        if (w.topSingers.isEmpty()) MutedLine("Tag songs with singers to see who you play most.")
        w.topSingers.forEachIndexed { i, s ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RankNumber(i + 1)
                InitialsAvatar(name = s.name, size = 40.dp)
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(s.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${s.playCount} plays", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                Text(formatDurationLong(s.totalMs), style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }
        }
    }

    WrappedCard("By day of week") {
        BarChart(
            values = w.byDayOfWeek.map { it.second },
            labels = w.byDayOfWeek.map { it.first.take(1) },
            height = 120.dp,
            spacing = 10.dp
        )
    }

    WrappedCard("By hour of day") {
        BarChart(
            values = w.byHour.map { it.second },
            labels = w.byHour.map { (h, _) -> if (h % 6 == 0) hourLabel(h) else null },
            height = 90.dp,
            spacing = 3.dp
        )
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f))
    }
}

@Composable
private fun WrappedCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(20.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
        content()
    }
}

@Composable
private fun MutedLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
}

@Composable
private fun RankNumber(rank: Int) {
    Text(
        text = rank.toString(),
        style = MaterialTheme.typography.titleLarge,
        color = if (rank == 1) MaterialTheme.colorScheme.primary else TextFaint,
        modifier = Modifier.width(28.dp)
    )
}

@Composable
private fun RankRow(rank: Int, title: String, subtitle: String, trailing: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RankNumber(rank)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(trailing, style = MaterialTheme.typography.labelMedium, color = TextMuted, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Vertical bars scaled to the max; the peak is drawn in full neon, the rest dimmed. */
@Composable
private fun BarChart(values: List<Long>, labels: List<String?>, height: Dp, spacing: Dp) {
    val max = values.maxOrNull() ?: 0L
    Row(
        modifier = Modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEach { v ->
            val target = if (max > 0) (v.toFloat() / max).coerceAtLeast(0.03f) else 0.03f
            val fraction by animateFloatAsState(target, tween(500), label = "bar")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                    .background(
                        if (v == max && max > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    )
            )
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        labels.forEach { label ->
            // Labels may be wider than a thin bar; let them overflow instead of wrapping.
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (label != null) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true)
                    )
                }
            }
        }
    }
}

private fun periodTitle(period: WrappedPeriod, offset: Int): String {
    val unit = period.name.lowercase()
    return when (offset) {
        0 -> "This $unit"
        1 -> "Last $unit"
        else -> "$offset ${unit}s ago"
    }
}

/** "Sep 22 – 28, 2026", "September 2026" or "2026" for the wrapped window. */
private fun periodRange(w: Wrapped): String {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(w.fromMs).atZone(zone).toLocalDate()
    val end = Instant.ofEpochMilli(w.toMs).atZone(zone).toLocalDate().minusDays(1)
    return when (w.period) {
        WrappedPeriod.WEEK -> {
            val startFmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
            val endFmt = if (start.month == end.month) "d, yyyy" else "MMM d, yyyy"
            "${startFmt.format(start)} – ${DateTimeFormatter.ofPattern(endFmt, Locale.ENGLISH).format(end)}"
        }
        WrappedPeriod.MONTH -> DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH).format(start)
        WrappedPeriod.YEAR -> start.year.toString()
    }
}

/** "Mon" -> "Monday" (the repository hands us short English names). */
private fun fullDayName(short: String): String =
    DayOfWeek.entries.firstOrNull { it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) == short }
        ?.getDisplayName(TextStyle.FULL, Locale.ENGLISH) ?: short

private fun hourLabel(hour: Int): String = when {
    hour == 0 -> "12am"
    hour < 12 -> "${hour}am"
    hour == 12 -> "12pm"
    else -> "${hour - 12}pm"
}
