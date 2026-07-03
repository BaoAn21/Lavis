package com.sim.lavis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sim.lavis.data.db.PlaylistRow
import com.sim.lavis.ui.components.DimText
import com.sim.lavis.ui.components.PromptHeader
import com.sim.lavis.ui.components.formatDurationLong
import com.sim.lavis.ui.LibraryViewModel
import com.sim.lavis.ui.theme.TermAmber
import com.sim.lavis.ui.theme.TermCyan

@Composable
fun PlaylistsScreen(
    viewModel: LibraryViewModel,
    onOpenPlaylist: (String) -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val scanning by viewModel.scanning.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PromptHeader("ls ~/music")
            Text(
                text = if (scanning) "[scanning..]" else "[rescan]",
                style = MaterialTheme.typography.labelLarge,
                color = TermCyan,
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .clickable(enabled = !scanning) { viewModel.scan() }
            )
        }

        if (playlists.isEmpty()) {
            DimText(
                text = "no playlists found.\n\n" +
                    "copy music folders into:\n  /Music/<playlist-name>/\n" +
                    "via usb, then hit [rescan]",
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            LazyColumn {
                items(playlists, key = { it.playlist }) { playlist ->
                    PlaylistLine(playlist, onClick = { onOpenPlaylist(playlist.playlist) })
                }
            }
        }
    }
}

@Composable
private fun PlaylistLine(row: PlaylistRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = "drw ",
            style = MaterialTheme.typography.bodyLarge,
            color = TermAmber
        )
        Column {
            Text(
                text = row.playlist + "/",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            DimText("${row.songCount} songs · ${formatDurationLong(row.totalDurationMs)}")
        }
    }
}
