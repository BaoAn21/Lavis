package com.sim.lavis.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.sim.lavis.data.db.SongDao
import com.sim.lavis.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans the device's Music directory via MediaStore and syncs it into Room.
 * Every direct subfolder of Music/ becomes a playlist; files sitting directly
 * in Music/ land in the "unsorted" playlist.
 */
class MediaScanner(
    private val context: Context,
    private val songDao: SongDao
) {

    suspend fun sync() = withContext(Dispatchers.IO) {
        val found = queryMediaStore()
        val existing = songDao.getAll().associateBy { it.path }
        val foundPaths = found.map { it.path }.toHashSet()

        for (scanned in found) {
            val old = existing[scanned.path]
            if (old == null) {
                songDao.insert(scanned)
            } else {
                // Keep the user's edited title; refresh everything technical.
                songDao.update(
                    old.copy(
                        contentUri = scanned.contentUri,
                        playlist = scanned.playlist,
                        durationMs = scanned.durationMs,
                        available = true
                    )
                )
            }
        }

        val gone = existing.values.filter { it.available && it.path !in foundPaths }.map { it.id }
        if (gone.isNotEmpty()) songDao.markUnavailable(gone)
    }

    private fun queryMediaStore(): List<SongEntity> {
        val songs = mutableListOf<SongEntity>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.RELATIVE_PATH
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE 'Music/%'"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, null, null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val relPathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val fileName = cursor.getString(nameCol) ?: continue
                val relPath = cursor.getString(relPathCol) ?: "Music/"
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                songs += SongEntity(
                    path = relPath + fileName,
                    contentUri = uri.toString(),
                    fileName = fileName,
                    title = cursor.getString(titleCol) ?: fileName.substringBeforeLast('.'),
                    playlist = playlistFromRelativePath(relPath),
                    durationMs = cursor.getLong(durCol),
                    available = true
                )
            }
        }
        return songs
    }

    /** "Music/vpop/" -> "vpop"; "Music/" -> "unsorted"; deeper nesting keeps first level. */
    private fun playlistFromRelativePath(relPath: String): String {
        val parts = relPath.trim('/').split('/')
        return if (parts.size <= 1) "unsorted" else parts[1]
    }
}
