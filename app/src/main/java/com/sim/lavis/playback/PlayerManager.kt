package com.sim.lavis.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.sim.lavis.data.db.SongWithSingers
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
    /** Embedded cover art extracted by the player from the file's tags, if any. */
    val artwork: ByteArray? = null,
    val isPlaying: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val queueIndex: Int = 0,
    val queueSize: Int = 0
)

/**
 * Wraps the MediaController connection to PlaybackService and exposes state as a flow.
 *
 * App-scoped (outlives activities), so it must survive the service going away: if the controller
 * disconnects, the next command transparently reconnects instead of being silently dropped by
 * a dead controller — the old cause of taps on play/next doing nothing after a while.
 */
class PlayerManager(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainExecutor = ContextCompat.getMainExecutor(appContext)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null

    /** Commands issued before the controller is connected; run once it is. */
    private val pending = ArrayDeque<(MediaController) -> Unit>()

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    init {
        connect()
    }

    /** Bind to the service if not connected or connecting. Safe to call repeatedly. */
    fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token)
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) {
                    this@PlayerManager.controller = null
                    controllerFuture = null
                    positionJob?.cancel()
                    positionJob = null
                    _state.value = PlayerUiState()
                }
            })
            .buildAsync()
        controllerFuture = future
        future.addListener({
            val c = try {
                future.get()
            } catch (e: Exception) {
                controllerFuture = null
                pending.clear()
                return@addListener
            }
            controller = c
            c.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    refreshState()
                }
            })
            refreshState()
            while (pending.isNotEmpty()) pending.removeFirst().invoke(c)
        }, mainExecutor)
    }

    private fun withController(action: (MediaController) -> Unit) {
        val c = controller
        if (c != null && c.isConnected) {
            action(c)
        } else {
            pending.addLast(action)
            connect()
        }
    }

    private fun refreshState() {
        val c = controller ?: return
        val metadata = c.mediaMetadata
        _state.value = PlayerUiState(
            currentSongId = c.currentMediaItem?.mediaId?.toLongOrNull(),
            title = c.currentMediaItem?.mediaMetadata?.title?.toString() ?: "",
            subtitle = c.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "",
            artwork = metadata.artworkData,
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

    /**
     * Replace the queue with [songs] and start at [startIndex].
     * With [shuffled], shuffle mode is turned on and the first song is picked at random
     * (previously shuffle always opened on the first song of the list).
     */
    fun playSongs(songs: List<SongWithSingers>, startIndex: Int = 0, shuffled: Boolean = false) {
        if (songs.isEmpty()) return
        val items = songs.map { it.toMediaItem() }
        val start = if (shuffled) songs.indices.random() else startIndex.coerceIn(songs.indices)
        withController { c ->
            c.shuffleModeEnabled = shuffled
            c.setMediaItems(items, start, 0)
            c.prepare()
            c.play()
        }
    }

    fun togglePlayPause() = withController { c ->
        if (c.isPlaying) c.pause() else c.play()
    }

    // seekToNext/Previous (not ...MediaItem) behave like every other player: previous restarts
    // the song when you're a few seconds in, and both respect shuffle order and repeat mode.
    fun next() = withController { it.seekToNext() }
    fun previous() = withController { it.seekToPrevious() }

    fun seekToFraction(fraction: Float) = withController { c ->
        val dur = c.duration
        if (dur > 0) c.seekTo((dur * fraction.coerceIn(0f, 1f)).toLong())
    }

    fun toggleShuffle() = withController { c ->
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() = withController { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
}
