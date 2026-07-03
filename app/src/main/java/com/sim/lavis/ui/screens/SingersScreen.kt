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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sim.lavis.data.db.SongWithSingers
import com.sim.lavis.playback.PlayerManager
import com.sim.lavis.ui.LibraryViewModel
import com.sim.lavis.ui.components.DimText
import com.sim.lavis.ui.components.PromptHeader
import com.sim.lavis.ui.theme.TermAmber
import com.sim.lavis.ui.theme.TermCyan
import com.sim.lavis.ui.theme.TermRed

@Composable
fun SingersScreen(
    viewModel: LibraryViewModel,
    onOpenSinger: (Long, String) -> Unit
) {
    val singers by viewModel.singers.collectAsState()
    var newSinger by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        PromptHeader("cat singers.txt")

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newSinger,
                onValueChange = { newSinger = it },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = { DimText("new singer name...") },
                colors = terminalFieldColors()
            )
            Text(
                text = " [+add]",
                style = MaterialTheme.typography.labelLarge,
                color = TermCyan,
                modifier = Modifier.clickable {
                    if (newSinger.isNotBlank()) {
                        viewModel.createSinger(newSinger)
                        newSinger = ""
                    }
                }
            )
        }

        if (singers.isEmpty()) {
            DimText(
                "no singers yet.\ncreate one above, then assign songs\nvia [edit] on any song.",
                modifier = Modifier.padding(top = 24.dp)
            )
        }

        LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
            items(singers, key = { it.id }) { singer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSinger(singer.id, singer.name) }
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = "@ ",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TermAmber
                    )
                    Column {
                        Text(
                            text = singer.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        DimText("${singer.songCount} songs")
                    }
                }
            }
        }
    }
}

@Composable
fun SingerDetailScreen(
    singerId: Long,
    singerName: String,
    viewModel: LibraryViewModel,
    playerManager: PlayerManager,
    onBack: () -> Unit
) {
    val songs by remember(singerId) { viewModel.songsOfSinger(singerId) }
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
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row {
            Text(
                text = "[..] ",
                style = MaterialTheme.typography.titleLarge,
                color = TermCyan,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clickable(onClick = onBack)
            )
            PromptHeader("grep '$singerName'")
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (songs.isNotEmpty()) {
                Row {
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
                DimText("no songs assigned to this singer yet.")
            }
            Text(
                text = "[rm singer]",
                style = MaterialTheme.typography.labelLarge,
                color = TermRed,
                modifier = Modifier.clickable {
                    viewModel.deleteSinger(singerId)
                    onBack()
                }
            )
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
