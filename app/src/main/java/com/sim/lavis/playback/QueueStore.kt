package com.sim.lavis.playback

import android.content.Context
import androidx.core.content.edit
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.sim.lavis.data.db.SongDao

/**
 * Remembers the last queue across process death, so the system media card (which outlives
 * the app, e.g. HyperOS after swiping Lavis away) can resume playback instead of doing nothing.
 * Only song ids are stored; items are rebuilt from the DB so edits and deleted files are respected.
 */
class QueueStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("queue", Context.MODE_PRIVATE)

    val shuffle: Boolean get() = prefs.getBoolean(KEY_SHUFFLE, false)
    val repeatMode: Int get() = prefs.getInt(KEY_REPEAT, Player.REPEAT_MODE_OFF)

    /** Snapshot [player]'s queue, position and modes. Cheap enough to call on every transition. */
    fun save(player: Player) {
        if (player.mediaItemCount == 0) return
        val ids = (0 until player.mediaItemCount).joinToString(",") { player.getMediaItemAt(it).mediaId }
        prefs.edit {
            putString(KEY_IDS, ids)
            putString(KEY_CURRENT_ID, player.currentMediaItem?.mediaId)
            putLong(KEY_POSITION, player.currentPosition.coerceAtLeast(0))
            putBoolean(KEY_SHUFFLE, player.shuffleModeEnabled)
            putInt(KEY_REPEAT, player.repeatMode)
        }
    }

    /** Rebuild the saved queue, dropping songs that no longer exist. Null when there is nothing to resume. */
    suspend fun restore(songDao: SongDao): MediaItemsWithStartPosition? {
        val ids = prefs.getString(KEY_IDS, null)
            ?.split(',')
            ?.mapNotNull { it.toLongOrNull() }
            .orEmpty()
        if (ids.isEmpty()) return null

        // Chunked to stay under SQLite's bound-variable limit on very long queues.
        val byId = ids.distinct().chunked(900)
            .flatMap { songDao.getWithSingers(it) }
            .associateBy { it.song.id }
        val songs = ids.mapNotNull { byId[it] }
        if (songs.isEmpty()) return null

        val currentId = prefs.getString(KEY_CURRENT_ID, null)?.toLongOrNull()
        val index = songs.indexOfFirst { it.song.id == currentId }
        return MediaItemsWithStartPosition(
            songs.map { it.toMediaItem() },
            index.coerceAtLeast(0),
            if (index >= 0) prefs.getLong(KEY_POSITION, 0) else C.TIME_UNSET
        )
    }

    private companion object {
        const val KEY_IDS = "ids"
        const val KEY_CURRENT_ID = "current_id"
        const val KEY_POSITION = "position_ms"
        const val KEY_SHUFFLE = "shuffle"
        const val KEY_REPEAT = "repeat"
    }
}
