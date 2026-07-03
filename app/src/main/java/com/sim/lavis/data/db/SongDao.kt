package com.sim.lavis.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class PlaylistRow(
    val playlist: String,
    val songCount: Int,
    val totalDurationMs: Long
)

@Dao
interface SongDao {

    @Query("SELECT * FROM songs WHERE available = 1 ORDER BY playlist, title COLLATE NOCASE")
    fun observeAllAvailable(): Flow<List<SongEntity>>

    @Query(
        """SELECT playlist, COUNT(*) AS songCount, SUM(durationMs) AS totalDurationMs
           FROM songs WHERE available = 1
           GROUP BY playlist ORDER BY playlist COLLATE NOCASE"""
    )
    fun observePlaylists(): Flow<List<PlaylistRow>>

    @Transaction
    @Query("SELECT * FROM songs WHERE available = 1 AND playlist = :playlist ORDER BY title COLLATE NOCASE")
    fun observePlaylist(playlist: String): Flow<List<SongWithSingers>>

    @Transaction
    @Query(
        """SELECT songs.* FROM songs
           INNER JOIN song_singer ON song_singer.songId = songs.id
           WHERE song_singer.singerId = :singerId AND songs.available = 1
           ORDER BY songs.title COLLATE NOCASE"""
    )
    fun observeSongsOfSinger(singerId: Long): Flow<List<SongWithSingers>>

    @Transaction
    @Query("SELECT * FROM songs WHERE id = :id")
    fun observeSong(id: Long): Flow<SongWithSingers?>

    @Query("SELECT * FROM songs")
    suspend fun getAll(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: Long): SongEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(song: SongEntity): Long

    @Update
    suspend fun update(song: SongEntity)

    @Query("UPDATE songs SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("UPDATE songs SET available = 0 WHERE id IN (:ids)")
    suspend fun markUnavailable(ids: List<Long>)
}
