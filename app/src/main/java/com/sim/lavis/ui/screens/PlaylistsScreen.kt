package com.sim.lavis.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sim.lavis.data.db.PlaylistRow
import com.sim.lavis.ui.LibraryViewModel
import com.sim.lavis.ui.components.CoverArt
import com.sim.lavis.ui.components.EmptyState
import com.sim.lavis.ui.components.LavisIcons
import com.sim.lavis.ui.components.ScreenHeader
import com.sim.lavis.ui.components.formatDurationLong
import com.sim.lavis.ui.components.songCount
import com.sim.lavis.ui.theme.TextMuted
import java.time.LocalTime

@Composable
fun PlaylistsScreen(
    viewModel: LibraryViewModel,
    onOpenPlaylist: (String) -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val scanning by viewModel.scanning.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                ScreenHeader(
                    title = "Your library",
                    eyebrow = greeting(),
                    // The grid already insets 16dp; keep the title aligned with the cards.
                    startPadding = 4.dp
                ) {
                    RescanButton(scanning = scanning, onClick = viewModel::scan)
                }
                if (playlists.isNotEmpty()) {
                    val songs = playlists.sumOf { it.songCount }
                    Text(
                        text = "${playlists.size} playlists · ${songCount(songs)} · " +
                            formatDurationLong(playlists.sumOf { it.totalDurationMs }),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                }
            }
        }

        if (playlists.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    icon = LavisIcons.Folder,
                    title = if (scanning) "Scanning your music…" else "No playlists yet",
                    message = "Every folder inside Music/ on your phone becomes a playlist. " +
                        "Copy some folders over, then tap refresh."
                )
            }
        } else {
            items(playlists, key = { it.playlist }) { playlist ->
                PlaylistCard(playlist, onClick = { onOpenPlaylist(playlist.playlist) })
            }
        }
    }
}

@Composable
private fun RescanButton(scanning: Boolean, onClick: () -> Unit) {
    // Only spin (and only run an animation at all) while a scan is in progress.
    val spin = if (scanning) {
        val angle by rememberInfiniteTransition(label = "scan").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
            label = "scanAngle"
        )
        Modifier.graphicsLayer { rotationZ = angle }
    } else {
        Modifier
    }
    IconButton(onClick = onClick, enabled = !scanning) {
        Icon(
            imageVector = LavisIcons.Refresh,
            contentDescription = "Rescan music folder",
            tint = if (scanning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = spin
        )
    }
}

@Composable
private fun PlaylistCard(row: PlaylistRow, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Box {
            CoverArt(
                image = null,
                key = row.playlist,
                size = Dp.Unspecified,
                shape = RoundedCornerShape(20.dp),
                iconFraction = 0.34f,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            )
            Text(
                text = row.playlist.take(1).uppercase(),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)
            )
        }
        Text(
            text = row.playlist,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp, start = 2.dp)
        )
        Text(
            text = "${songCount(row.songCount)} · ${formatDurationLong(row.totalDurationMs)}",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
        )
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    in 18..22 -> "Good evening"
    else -> "Late night listening"
}
