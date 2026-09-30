package com.sim.lavis.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.sim.lavis.data.db.LavisDatabase
import com.sim.lavis.data.db.PlayEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A MediaLibraryService (not just a MediaSessionService) so the system UI can bind to it for
 * playback resumption: that is what makes the leftover media card on the lock screen /
 * HyperOS control center work after the app has been closed.
 */
class PlaybackService : MediaLibraryService() {

    private var mediaSession: MediaLibrarySession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queueStore by lazy { QueueStore(this) }

    /** Errors in a row without anything playing; stops auto-skip from looping over a broken queue. */
    private var consecutiveErrors = 0

    // --- listening tracker state ---
    /** The listen currently being measured, or null when nothing is tracked. */
    private var session: ListenSession? = null
    /** Repeating job that flushes progress to disk while playing, so a kill can't lose it. */
    private var checkpointJob: Job? = null
    /** Serializes DB writes so two overlapping checkpoints can't create duplicate rows. */
    private val persistMutex = Mutex()

    /**
     * Mutable state for one in-progress listen. Kept in its own object (not loose fields) so the
     * DB row id stays bound to this exact listen — otherwise an async write for a finished song
     * could land on the next song when tracks change quickly.
     */
    private class ListenSession(val songId: Long?, val startedAtMs: Long) {
        var accumulatedMs: Long = 0        // playing time banked so far
        var playingSinceMs: Long? = null   // when the current playing stretch began, or null if paused
        var rowId: Long? = null            // the play_events row, once this listen is first persisted
    }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            // Keep the CPU awake while playing with the screen off; aggressive OEM battery
            // savers (HyperOS) otherwise stall playback.
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.shuffleModeEnabled = queueStore.shuffle
        player.repeatMode = queueStore.repeatMode

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val now = System.currentTimeMillis()
                if (isPlaying) {
                    consecutiveErrors = 0
                    // Open a session on first play; start the stopwatch and periodic checkpoints.
                    val s = session ?: openSession(player.currentMediaItem, now)
                    s.playingSinceMs = now
                    startCheckpoints()
                } else {
                    // Pausing: bank the stretch and persist immediately, so pausing then sleeping is safe.
                    session?.let { s ->
                        s.playingSinceMs?.let { s.accumulatedMs += now - it }
                        s.playingSinceMs = null
                        checkpoint(s, now)
                    }
                    stopCheckpoints()
                    queueStore.save(player)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val now = System.currentTimeMillis()
                // Close the outgoing song: bank its final stretch and write it, then start the next.
                val wasPlaying = session?.playingSinceMs != null
                session?.let { s ->
                    s.playingSinceMs?.let { s.accumulatedMs += now - it }
                    s.playingSinceMs = null  // stop the stopwatch so checkpoint() can't re-add this stretch
                    checkpoint(s, now)
                }
                openSession(mediaItem, now).playingSinceMs = if (wasPlaying) now else null
                queueStore.save(player)
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                    consecutiveErrors = 0  // a new queue gets a fresh error budget
                    queueStore.save(player)
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = queueStore.save(player)

            override fun onRepeatModeChanged(repeatMode: Int) = queueStore.save(player)

            /**
             * A file that can't be played (deleted since the last scan, unsupported codec...) puts
             * ExoPlayer into STATE_IDLE and it stays stuck there — every later skip only moved the
             * index, so the app looked like it "couldn't skip". Move past the broken song instead.
             */
            override fun onPlayerError(error: PlaybackException) {
                consecutiveErrors++
                if (consecutiveErrors < player.mediaItemCount && player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.prepare()
                }
            }
        })

        mediaSession = MediaLibrarySession.Builder(this, RecoveringPlayer(player), LibraryCallback()).build()
    }

    /**
     * Skips issued while the player sits in an error state (from the app, notification or
     * headset) re-prepare it, so the next song actually starts instead of staying silent.
     */
    private class RecoveringPlayer(player: Player) : ForwardingPlayer(player) {
        private fun recover() {
            if (playbackState == Player.STATE_IDLE && playerError != null) prepare()
        }

        override fun seekToNext() { super.seekToNext(); recover() }
        override fun seekToNextMediaItem() { super.seekToNextMediaItem(); recover() }
        override fun seekToPrevious() { super.seekToPrevious(); recover() }
        override fun seekToPreviousMediaItem() { super.seekToPreviousMediaItem(); recover() }
        override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
            super.seekTo(mediaItemIndex, positionMs)
            recover()
        }
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {
        /**
         * Called when a media button or the system's "recent media" card asks to play while the
         * player is empty (typically after the app process was killed). Hands back the saved queue.
         */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaItemsWithStartPosition> {
            val result = SettableFuture.create<MediaItemsWithStartPosition>()
            scope.launch {
                try {
                    val dao = LavisDatabase.get(this@PlaybackService).songDao()
                    val restored = queueStore.restore(dao)
                    if (restored != null) result.set(restored)
                    else result.setException(UnsupportedOperationException("no saved queue"))
                } catch (e: Exception) {
                    result.setException(e)
                }
            }
            return result
        }
    }

    /** Begin tracking a new song, replacing any current session. Returns the new session. */
    private fun openSession(mediaItem: MediaItem?, now: Long): ListenSession =
        ListenSession(songId = mediaItem?.mediaId?.toLongOrNull(), startedAtMs = now).also { session = it }

    /** Start the repeating job that persists progress every [CHECKPOINT_INTERVAL_MS] while playing. */
    private fun startCheckpoints() {
        if (checkpointJob != null) return
        // Runs on Main so it reads session fields on the same thread the player callbacks write them;
        // only the DB write itself is dispatched off-thread (inside checkpoint()).
        checkpointJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(CHECKPOINT_INTERVAL_MS)
                val now = System.currentTimeMillis()
                session?.let { s -> if (s.playingSinceMs != null) checkpoint(s, now) }
                mediaSession?.player?.let { queueStore.save(it) }
            }
        }
    }

    private fun stopCheckpoints() {
        checkpointJob?.cancel()
        checkpointJob = null
    }

    /**
     * Persist [s]'s listened-so-far. First qualifying write inserts a row and remembers its id;
     * later writes update that same row, so one listen stays one play but its duration grows durably.
     * Called on the Main thread; all values are snapshotted here so the coroutine only touches [rowId].
     */
    private fun checkpoint(s: ListenSession, now: Long) {
        val songId = s.songId ?: return
        val startedAt = s.startedAtMs
        val listened = s.accumulatedMs + (s.playingSinceMs?.let { now - it } ?: 0)
        if (listened < MIN_LISTEN_MS) return  // too short to count yet
        scope.launch {
            // Mutex keeps the insert-then-remember-id step atomic against overlapping checkpoints.
            persistMutex.withLock {
                val dao = LavisDatabase.get(this@PlaybackService).playEventDao()
                val id = s.rowId
                if (id == null) {
                    s.rowId = dao.insert(
                        PlayEventEntity(songId = songId, startedAtMs = startedAt, listenedMs = listened)
                    )
                } else {
                    dao.updateListened(id, listened)
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    // onTaskRemoved: Media3's default is right — keep the service while playing, otherwise
    // pause and stop. The saved queue lets the system card bring playback back later.

    override fun onDestroy() {
        val now = System.currentTimeMillis()
        stopCheckpoints()
        session?.let { s ->
            s.playingSinceMs?.let { s.accumulatedMs += now - it }
            s.playingSinceMs = null
            checkpoint(s, now)
        }
        mediaSession?.run {
            queueStore.save(player)
            player.release()
            release()
            mediaSession = null
        }
        // scope is cancelled after a short grace period is not needed; inserts are fast,
        // but keep the scope alive until the process dies rather than racing the flush.
        super.onDestroy()
    }

    companion object {
        /** Listens shorter than this are noise (skipping through a playlist). */
        const val MIN_LISTEN_MS = 5_000L

        /** How often an in-progress listen is flushed to disk. A crash loses at most this much time. */
        const val CHECKPOINT_INTERVAL_MS = 10_000L
    }
}
