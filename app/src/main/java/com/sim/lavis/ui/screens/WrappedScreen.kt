package com.sim.lavis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sim.lavis.data.Wrapped
import com.sim.lavis.data.WrappedPeriod
import com.sim.lavis.ui.WrappedViewModel
import com.sim.lavis.ui.components.DimText
import com.sim.lavis.ui.components.PromptHeader
import com.sim.lavis.ui.components.formatDurationLong
import com.sim.lavis.ui.theme.TermAmber
import com.sim.lavis.ui.theme.TermCyan
import com.sim.lavis.ui.theme.TermGray

@Composable
fun WrappedScreen(viewModel: WrappedViewModel) {
    val period by viewModel.period.collectAsState()
    val offset by viewModel.offset.collectAsState()
    val wrapped by viewModel.wrapped.collectAsState()

    // Refresh when the screen comes back into view (new events may exist).
    LaunchedEffect(Unit) { viewModel.recompute() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        PromptHeader("wrapped --stats")

        Row(modifier = Modifier.padding(bottom = 4.dp)) {
            WrappedPeriod.entries.forEach { p ->
                val selected = p == period
                Text(
                    text = if (selected) "[${p.name.lowercase()}] " else " ${p.name.lowercase()}  ",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) TermAmber else TermGray,
                    modifier = Modifier.clickable { viewModel.setPeriod(p) }
                )
            }
        }

        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "[<prev]",
                style = MaterialTheme.typography.labelLarge,
                color = TermCyan,
                modifier = Modifier.clickable { viewModel.previousPeriod() }
            )
            Text(
                text = "  ${wrapped?.label ?: "..."}  ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (offset > 0) "[next>]" else "       ",
                style = MaterialTheme.typography.labelLarge,
                color = TermCyan,
                modifier = Modifier.clickable(enabled = offset > 0) { viewModel.nextPeriod() }
            )
        }

        wrapped?.let { w ->
            if (w.playCount == 0) {
                DimText("no listening data for this period.", Modifier.padding(top = 16.dp))
            } else {
                WrappedBody(w)
            }
        }
    }
}

@Composable
private fun WrappedBody(w: Wrapped) {
    SectionTitle("## totals")
    StatLine("time listened", formatDurationLong(w.totalListenedMs))
    StatLine("songs played", w.playCount.toString())

    SectionTitle("## top songs")
    if (w.topSongs.isEmpty()) DimText("none")
    w.topSongs.forEachIndexed { i, s ->
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = "${i + 1}. ${s.title}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            DimText("   ${s.playCount} plays · ${formatDurationLong(s.totalMs)} · ${s.playlist}/")
        }
    }

    SectionTitle("## top singers")
    if (w.topSingers.isEmpty()) DimText("none (assign singers via [edit] on songs)")
    w.topSingers.forEachIndexed { i, s ->
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = "${i + 1}. ${s.name}",
                style = MaterialTheme.typography.bodyLarge,
                color = TermAmber
            )
            DimText("   ${s.playCount} plays · ${formatDurationLong(s.totalMs)}")
        }
    }

    SectionTitle("## by day of week")
    AsciiChart(w.byDayOfWeek.map { it.first to it.second })

    SectionTitle("## by hour of day")
    AsciiChart(
        w.byHour.filter { it.second > 0 }
            .map { "%02dh".format(it.first) to it.second }
    )

    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 24.dp))
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = TermCyan,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        DimText("$label: ")
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** Horizontal ASCII bar chart, bars scaled to the max value. */
@Composable
private fun AsciiChart(data: List<Pair<String, Long>>, barWidth: Int = 16) {
    val max = data.maxOfOrNull { it.second } ?: 0L
    if (max == 0L) {
        DimText("no data")
        return
    }
    data.forEach { (label, value) ->
        val filled = ((value.toFloat() / max) * barWidth).toInt()
        val highlight = value == max
        Row(modifier = Modifier.padding(vertical = 1.dp)) {
            Text(
                text = "$label ".padStart(4) + "#".repeat(filled).padEnd(barWidth + 1),
                style = MaterialTheme.typography.bodyMedium,
                color = if (highlight) TermAmber else MaterialTheme.colorScheme.primary
            )
            DimText(formatDurationLong(value))
        }
    }
}
