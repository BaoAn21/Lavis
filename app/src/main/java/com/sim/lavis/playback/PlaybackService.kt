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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // --- listening tracker state ---
    private var trackedSongId: Long? = null
    private var songStartedAtMs: Long = 0
    private var accumulatedMs: Long = 0
    private var playingSinceMs: Long? = null

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
                    if (trackedSongId == null) {
                        trackedSongId = player.currentMediaItem?.mediaId?.toLongOrNull()
                        songStartedAtMs = now
                        accumulatedMs = 0
                    }
                    playingSinceMs = now
                } else {
                    playingSinceMs?.let { accumulatedMs += now - it }
                    playingSinceMs = null
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val now = System.currentTimeMillis()
                val wasPlaying = playingSinceMs != null
                playingSinceMs?.let { accumulatedMs += now - it }
                flushEvent()
                trackedSongId = mediaItem?.mediaId?.toLongOrNull()
                songStartedAtMs = now
                accumulatedMs = 0
                playingSinceMs = if (wasPlaying) now else null
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
    }

    /** Persist the accumulated listen of the current song, if it was long enough to count. */
    private fun flushEvent() {
        val songId = trackedSongId ?: return
        val listened = accumulatedMs
        val startedAt = songStartedAtMs
        if (listened >= MIN_LISTEN_MS) {
            scope.launch {
                LavisDatabase.get(this@PlaybackService).playEventDao()
                    .insert(PlayEventEntity(songId = songId, startedAtMs = startedAt, listenedMs = listened))
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
        playingSinceMs?.let { accumulatedMs += now - it }
        playingSinceMs = null
        flushEvent()
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
    }
}
