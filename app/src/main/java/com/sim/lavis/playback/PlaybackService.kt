package com.sim.lavis.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sim.lavis.data.db.LavisDatabase
import com.sim.lavis.data.db.PlayEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val now = System.currentTimeMillis()
                if (isPlaying) {
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
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        val now = System.currentTimeMillis()
        stopCheckpoints()
        session?.let { s ->
            s.playingSinceMs?.let { s.accumulatedMs += now - it }
            s.playingSinceMs = null
            checkpoint(s, now)
        }
        mediaSession?.run {
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
