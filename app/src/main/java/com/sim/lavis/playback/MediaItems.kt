package com.sim.lavis.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.sim.lavis.data.db.SongWithSingers

/** What the notification / lock screen shows under the title: the singers, else the playlist. */
fun SongWithSingers.artistLabel(): String =
    singers.joinToString(", ") { it.name }.ifEmpty { song.playlist }

/**
 * The one place a song becomes a player item, shared by in-app playback and playback
 * resumption so both produce identical items (mediaId = song id, which the listen tracker reads).
 */
fun SongWithSingers.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(song.id.toString())
        .setUri(song.contentUri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(artistLabel())
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build()
        )
        .build()
