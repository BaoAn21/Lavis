package com.sim.lavis.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

data class SongStat(
    val songId: Long,
    val title: String,
    val playlist: String,
    val playCount: Int,
    val totalMs: Long
)

data class SingerStat(
    val singerId: Long,
    val name: String,
    val playCount: Int,
    val totalMs: Long
)

/** Generic bucket (day-of-week "0".."6" or hour-of-day "00".."23") -> listened millis. */
data class BucketStat(
    val bucket: String,
    val totalMs: Long
)

@Dao
interface PlayEventDao {

    @Insert
    suspend fun insert(event: PlayEventEntity)

    @Query("SELECT COALESCE(SUM(listenedMs), 0) FROM play_events WHERE startedAtMs >= :from AND startedAtMs < :to")
    suspend fun totalListenedMs(from: Long, to: Long): Long

    @Query("SELECT COUNT(*) FROM play_events WHERE startedAtMs >= :from AND startedAtMs < :to")
    suspend fun playCount(from: Long, to: Long): Int

    @Query(
        """SELECT songs.id AS songId, songs.title AS title, songs.playlist AS playlist,
                  COUNT(play_events.id) AS playCount, SUM(play_events.listenedMs) AS totalMs
           FROM play_events
           INNER JOIN songs ON songs.id = play_events.songId
           WHERE play_events.startedAtMs >= :from AND play_events.startedAtMs < :to
           GROUP BY songs.id
           ORDER BY totalMs DESC
           LIMIT :limit"""
    )
    suspend fun topSongs(from: Long, to: Long, limit: Int): List<SongStat>

    @Query(
        """SELECT singers.id AS singerId, singers.name AS name,
                  COUNT(play_events.id) AS playCount, SUM(play_events.listenedMs) AS totalMs
           FROM play_events
           INNER JOIN song_singer ON song_singer.songId = play_events.songId
           INNER JOIN singers ON singers.id = song_singer.singerId
           WHERE play_events.startedAtMs >= :from AND play_events.startedAtMs < :to
           GROUP BY singers.id
           ORDER BY totalMs DESC
           LIMIT :limit"""
    )
    suspend fun topSingers(from: Long, to: Long, limit: Int): List<SingerStat>

    // strftime('%w') -> 0=Sunday .. 6=Saturday, in the device's local timezone
    @Query(
        """SELECT strftime('%w', startedAtMs / 1000, 'unixepoch', 'localtime') AS bucket,
                  SUM(listenedMs) AS totalMs
           FROM play_events
           WHERE startedAtMs >= :from AND startedAtMs < :to
           GROUP BY bucket"""
    )
    suspend fun listenedByDayOfWeek(from: Long, to: Long): List<BucketStat>

    @Query(
        """SELECT strftime('%H', startedAtMs / 1000, 'unixepoch', 'localtime') AS bucket,
                  SUM(listenedMs) AS totalMs
           FROM play_events
           WHERE startedAtMs >= :from AND startedAtMs < :to
           GROUP BY bucket"""
    )
    suspend fun listenedByHour(from: Long, to: Long): List<BucketStat>

    @Query("SELECT MIN(startedAtMs) FROM play_events")
    suspend fun firstEventAt(): Long?
}
