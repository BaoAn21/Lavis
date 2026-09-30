package com.sim.lavis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sim.lavis.data.db.SongWithSingers
import com.sim.lavis.playback.PlayerManager
import com.sim.lavis.ui.LibraryViewModel
import com.sim.lavis.ui.components.CoverArt
import com.sim.lavis.ui.components.EmptyState
import com.sim.lavis.ui.components.LavisIcons
import com.sim.lavis.ui.components.PlayShuffleButtons
import com.sim.lavis.ui.components.PlayingBars
import com.sim.lavis.ui.components.SongCover
import com.sim.lavis.ui.components.formatDurationLong
import com.sim.lavis.ui.components.formatTime
import com.sim.lavis.ui.components.songCount
import com.sim.lavis.ui.theme.TextFaint
import com.sim.lavis.ui.theme.TextMuted
import com.sim.lavis.ui.theme.coverAccent

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

    SongListPage(
        songs = songs,
        currentSongId = playerState.currentSongId,
        isPlaying = playerState.isPlaying,
        onPlay = { index, shuffled -> playerManager.playSongs(songs, index, shuffled) },
        onEdit = { editing = it },
        header = {
            DetailHero(
                accentKey = playlistName,
                title = playlistName,
                meta = "Playlist · ${songCount(songs.size)} · ${formatDurationLong(songs.sumOf { it.song.durationMs })}",
                onBack = onBack,
                cover = {
                    CoverArt(
                        image = null,
                        key = playlistName,
                        size = 196.dp,
                        shape = RoundedCornerShape(28.dp),
                        iconFraction = 0.34f
                    )
                },
                onPlay = { playerManager.playSongs(songs, 0, shuffled = false) },
                onShuffle = { playerManager.playSongs(songs, shuffled = true) },
                showButtons = songs.isNotEmpty()
            )
        },
        empty = {
            EmptyState(
                icon = LavisIcons.MusicNote,
                title = "This playlist is empty",
                message = "Copy songs into Music/$playlistName/ and rescan from the library."
            )
        }
    )

    editing?.let { song ->
        // Re-read from the live list so the dialog reflects singer toggles immediately.
        val live = songs.firstOrNull { it.song.id == song.song.id } ?: song
        EditSongDialog(
            songWithSingers = live,
            allSingers = allSingers,
            viewModel = viewModel,
            onDismiss = { editing = null }
        )
    }
}

/** A hero header + song list, shared by playlist and singer pages. */
@Composable
fun SongListPage(
    songs: List<SongWithSingers>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onPlay: (index: Int, shuffled: Boolean) -> Unit,
    onEdit: (SongWithSingers) -> Unit,
    header: @Composable () -> Unit,
    empty: @Composable () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "header") { header() }
        if (songs.isEmpty()) {
            item(key = "empty") { empty() }
        }
        itemsIndexed(songs, key = { _, s -> s.song.id }) { index, songWithSingers ->
            SongRow(
                item = songWithSingers,
                isCurrent = currentSongId == songWithSingers.song.id,
                isPlaying = isPlaying,
                onPlay = { onPlay(index, false) },
                onEdit = { onEdit(songWithSingers) }
            )
        }
    }
}

/** Glowing header with back button, cover, title and Play/Shuffle. */
@Composable
fun DetailHero(
    accentKey: String,
    title: String,
    meta: String,
    onBack: () -> Unit,
    cover: @Composable () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    showButtons: Boolean,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val accent = coverAccent(accentKey)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.35f), Color.Transparent)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(LavisIcons.Back, contentDescription = "Back")
                }
                Spacer(Modifier.weight(1f))
                actions()
            }
            cover()
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 20.dp, start = 24.dp, end = 24.dp)
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (showButtons) {
                PlayShuffleButtons(
                    onPlay = onPlay,
                    onShuffle = onShuffle,
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp)
                )
            }
        }
    }
}

@Composable
fun SongRow(
    item: SongWithSingers,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onEdit: () -> Unit
) {
    val song = item.song
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            SongCover(contentUri = song.contentUri, key = song.title, size = 52.dp)
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    PlayingBars(playing = isPlaying, height = 18.dp, barWidth = 4.dp)
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.singers.joinToString(", ") { it.name }.ifEmpty { "Unknown singer" } +
                    "  ·  " + formatTime(song.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = LavisIcons.Edit,
                contentDescription = "Edit ${song.title}",
                tint = TextFaint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
