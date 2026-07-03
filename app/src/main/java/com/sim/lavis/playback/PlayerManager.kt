package com.sim.lavis.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.sim.lavis.data.db.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val currentSongId: Long? = null,
    val title: String = "",
    val subtitle: String = "",
    val isPlaying: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val queueIndex: Int = 0,
    val queueSize: Int = 0
)

/** Wraps the MediaController connection to PlaybackService and exposes state as a flow. */
class PlayerManager(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null
    private var positionJob: Job? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    init {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    refreshState()
                }
            })
            refreshState()
        }, MoreExecutors.directExecutor())
    }

    private fun refreshState() {
        val c = controller ?: return
        _state.value = PlayerUiState(
            currentSongId = c.currentMediaItem?.mediaId?.toLongOrNull(),
            title = c.currentMediaItem?.mediaMetadata?.title?.toString() ?: "",
            subtitle = c.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "",
            isPlaying = c.isPlaying,
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            positionMs = c.currentPosition.coerceAtLeast(0),
            durationMs = c.duration.coerceAtLeast(0),
            queueIndex = c.currentMediaItemIndex,
            queueSize = c.mediaItemCount
        )
        startOrStopTicker()
    }

    private fun startOrStopTicker() {
        val playing = _state.value.isPlaying
        if (playing && positionJob == null) {
            positionJob = scope.launch {
                while (isActive) {
                    delay(500)
                    controller?.let { c ->
                        _state.value = _state.value.copy(
                            positionMs = c.currentPosition.coerceAtLeast(0),
                            durationMs = c.duration.coerceAtLeast(0)
                        )
                    }
                }
            }
        } else if (!playing) {
            positionJob?.cancel()
            positionJob = null
        }
    }

    /** Replace the queue with [songs], start at [startIndex]. Set [shuffled] for random play. */
    fun playSongs(songs: List<SongEntity>, startIndex: Int = 0, shuffled: Boolean = false, singersBySong: Map<Long, String> = emptyMap()) {
        val c = controller ?: return
        val items = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.contentUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(singersBySong[song.id] ?: song.playlist)
                        .build()
                )
                .build()
        }
        c.setMediaItems(items, startIndex, 0)
        c.shuffleModeEnabled = shuffled
        c.prepare()
        c.play()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() = controller?.seekToNextMediaItem() ?: Unit
    fun previous() = controller?.seekToPreviousMediaItem() ?: Unit

    fun seekToFraction(fraction: Float) {
        val c = controller ?: return
        val dur = c.duration
        if (dur > 0) c.seekTo((dur * fraction.coerceIn(0f, 1f)).toLong())
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
}
