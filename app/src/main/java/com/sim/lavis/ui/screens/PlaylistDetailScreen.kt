package com.sim.lavis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sim.lavis.data.db.SongWithSingers
import com.sim.lavis.playback.PlayerManager
import com.sim.lavis.ui.LibraryViewModel
import com.sim.lavis.ui.components.DimText
import com.sim.lavis.ui.components.PromptHeader
import com.sim.lavis.ui.components.formatTime
import com.sim.lavis.ui.theme.TermAmber
import com.sim.lavis.ui.theme.TermCyan

@Composable
fun PlaylistDetailScreen(
    playlistName: String,
    viewModel: LibraryViewModel,
    playerManager: PlayerManager,
    onBack: () -> Unit
) {
    val songs by remember(playlistName) { viewModel.playlistSongs(playlistName) }
        .collectAsState(initial = emptyList())
    val allSingers by viewModel.allSingers.collectAsState()
    val playerState by playerManager.state.collectAsState()
    var editing by remember { mutableStateOf<SongWithSingers?>(null) }

    fun play(startIndex: Int, shuffled: Boolean) {
        playerManager.playSongs(
            songs = songs.map { it.song },
            startIndex = startIndex,
            shuffled = shuffled,
            singersBySong = songs.associate { sws ->
                sws.song.id to sws.singers.joinToString(", ") { it.name }
                    .ifEmpty { sws.song.playlist }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                Text(
                    text = "[..] ",
                    style = MaterialTheme.typography.titleLarge,
                    color = TermCyan,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable(onClick = onBack)
                )
                PromptHeader("cd $playlistName")
            }
        }

        if (songs.isNotEmpty()) {
            Row(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "[> play all]",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { play(0, shuffled = false) }
                )
                Text(
                    text = "  [~ shuffle]",
                    style = MaterialTheme.typography.labelLarge,
                    color = TermAmber,
                    modifier = Modifier.clickable { play(0, shuffled = true) }
                )
            }
        } else {
            DimText("empty. copy files into /Music/$playlistName/ and rescan.")
        }

        LazyColumn {
            itemsIndexed(songs, key = { _, s -> s.song.id }) { index, songWithSingers ->
                SongLine(
                    item = songWithSingers,
                    index = index,
                    isCurrent = playerState.currentSongId == songWithSingers.song.id,
                    onPlay = { play(index, shuffled = false) },
                    onEdit = { editing = songWithSingers }
                )
            }
        }
    }

    editing?.let { song ->
        EditSongDialog(
            songWithSingers = song,
            allSingers = allSingers,
            viewModel = viewModel,
            onDismiss = { editing = null }
        )
    }
}

@Composable
fun SongLine(
    item: SongWithSingers,
    index: Int,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onEdit: () -> Unit
) {
    val song = item.song
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isCurrent) "> " else "%02d ".format(index + 1),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) TermAmber else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCurrent) TermAmber else MaterialTheme.colorScheme.primary
                )
                DimText(
                    item.singers.joinToString(", ") { it.name }
                        .ifEmpty { "unknown singer" } + " · " + formatTime(song.durationMs)
                )
            }
        }
        Text(
            text = "[edit]",
            style = MaterialTheme.typography.labelMedium,
            color = TermCyan,
            modifier = Modifier
                .clickable(onClick = onEdit)
                .padding(start = 8.dp, top = 2.dp)
        )
    }
}
