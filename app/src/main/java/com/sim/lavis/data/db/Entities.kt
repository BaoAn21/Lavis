package com.sim.lavis.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.Embedded

/**
 * One audio file on disk. The relative path (e.g. "Music/vpop/song.mp3")
 * is the stable identity so re-copying files from the laptop keeps edits.
 */
@Entity(
    tableName = "songs",
    indices = [Index(value = ["path"], unique = true)]
)
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val contentUri: String,
    val fileName: String,
    /** Display title: file tag title, or user override. */
    val title: String,
    /** Folder name under the music root = playlist name. */
    val playlist: String,
    val durationMs: Long,
    /** False when the file disappeared from disk; kept so wrapped history survives. */
    val available: Boolean = true
)

@Entity(
    tableName = "singers",
    indices = [Index(value = ["name"], unique = true)]
)
data class SingerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "song_singer",
    primaryKeys = ["songId", "singerId"],
    indices = [Index("singerId")],
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SingerEntity::class,
            parentColumns = ["id"],
            childColumns = ["singerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SongSingerCrossRef(
    val songId: Long,
    val singerId: Long
)

/** One listening session of one song. The raw log that powers wrapped. */
@Entity(
    tableName = "play_events",
    indices = [Index("songId"), Index("startedAtMs")],
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlayEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    /** Wall-clock epoch millis when this listen started. */
    val startedAtMs: Long,
    /** How long the user actually listened, in millis. */
    val listenedMs: Long
)

data class SongWithSingers(
    @Embedded val song: SongEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SongSingerCrossRef::class,
            parentColumn = "songId",
            entityColumn = "singerId"
        )
    )
    val singers: List<SingerEntity>
)
