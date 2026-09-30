package com.sim.lavis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sim.lavis.data.db.SongWithSingers
import com.sim.lavis.playback.PlayerManager
import com.sim.lavis.ui.LibraryViewModel
import com.sim.lavis.ui.components.EmptyState
import com.sim.lavis.ui.components.InitialsAvatar
import com.sim.lavis.ui.components.LavisIcons
import com.sim.lavis.ui.components.ScreenHeader
import com.sim.lavis.ui.components.songCount
import com.sim.lavis.ui.theme.TextFaint
import com.sim.lavis.ui.theme.TextMuted

@Composable
fun SingersScreen(
    viewModel: LibraryViewModel,
    onOpenSinger: (Long, String) -> Unit
) {
    val singers by viewModel.singers.collectAsState()
    var newSinger by remember { mutableStateOf("") }

    fun add() {
        if (newSinger.isNotBlank()) {
            viewModel.createSinger(newSinger)
            newSinger = ""
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "header") {
            Column {
                ScreenHeader(
                    title = "Singers",
                    eyebrow = when (singers.size) {
                        0 -> null
                        1 -> "1 artist"
                        else -> "${singers.size} artists"
                    }
                )
                OutlinedTextField(
                    value = newSinger,
                    onValueChange = { newSinger = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    placeholder = { Text("Add a singer", color = TextFaint) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { add() }),
                    trailingIcon = {
                        IconButton(onClick = ::add, enabled = newSinger.isNotBlank()) {
                            Icon(LavisIcons.Add, contentDescription = "Add singer")
                        }
                    },
                    colors = lavisFieldColors()
                )
            }
        }

        if (singers.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = LavisIcons.Person,
                    title = "No singers yet",
                    message = "Add one above, then tag songs with the edit button on any song."
                )
            }
        }

        items(singers, key = { it.id }) { singer ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSinger(singer.id, singer.name) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InitialsAvatar(name = singer.name, size = 52.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp)
                ) {
                    Text(
                        text = singer.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = songCount(singer.songCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Icon(LavisIcons.ChevronRight, contentDescription = null, tint = TextFaint)
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
    var confirmDelete by remember { mutableStateOf(false) }

    SongListPage(
        songs = songs,
        currentSongId = playerState.currentSongId,
        isPlaying = playerState.isPlaying,
        onPlay = { index, shuffled -> playerManager.playSongs(songs, index, shuffled) },
        onEdit = { editing = it },
        header = {
            DetailHero(
                accentKey = singerName,
                title = singerName,
                meta = "Singer · ${songCount(songs.size)}",
                onBack = onBack,
                cover = { InitialsAvatar(name = singerName, size = 168.dp) },
                onPlay = { playerManager.playSongs(songs, 0, shuffled = false) },
                onShuffle = { playerManager.playSongs(songs, shuffled = true) },
                showButtons = songs.isNotEmpty(),
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(LavisIcons.Delete, contentDescription = "Delete singer", tint = TextMuted)
                    }
                }
            )
        },
        empty = {
            EmptyState(
                icon = LavisIcons.MusicNote,
                title = "No songs tagged yet",
                message = "Open any song's edit menu and tick $singerName to add it here."
            )
        }
    )

    editing?.let { song ->
        val live = songs.firstOrNull { it.song.id == song.song.id } ?: song
        EditSongDialog(
            songWithSingers = live,
            allSingers = allSingers,
            viewModel = viewModel,
            onDismiss = { editing = null }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete $singerName?") },
            text = { Text("Songs stay in your library; they just won't be tagged with this singer anymore.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteSinger(singerId)
                    onBack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}
