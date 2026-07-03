package com.sim.lavis.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class SingerRow(
    val id: Long,
    val name: String,
    val songCount: Int
)

@Dao
interface SingerDao {

    @Query(
        """SELECT singers.id AS id, singers.name AS name, COUNT(song_singer.songId) AS songCount
           FROM singers
           LEFT JOIN song_singer ON song_singer.singerId = singers.id
           GROUP BY singers.id ORDER BY singers.name COLLATE NOCASE"""
    )
    fun observeSingers(): Flow<List<SingerRow>>

    @Query("SELECT * FROM singers ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<SingerEntity>>

    @Query("SELECT * FROM singers WHERE id = :id")
    suspend fun getById(id: Long): SingerEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(singer: SingerEntity): Long

    @Query("SELECT id FROM singers WHERE name = :name")
    suspend fun idByName(name: String): Long?

    @Query("DELETE FROM singers WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(ref: SongSingerCrossRef)

    @Delete
    suspend fun unlink(ref: SongSingerCrossRef)
}
