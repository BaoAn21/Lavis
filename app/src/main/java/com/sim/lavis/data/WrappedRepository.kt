package com.sim.lavis.data

import com.sim.lavis.data.db.PlayEventDao
import com.sim.lavis.data.db.SingerStat
import com.sim.lavis.data.db.SongStat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

enum class WrappedPeriod { WEEK, MONTH, YEAR }

data class Wrapped(
    val period: WrappedPeriod,
    /** e.g. "2026-W27", "2026-06", "2026". */
    val label: String,
    val fromMs: Long,
    val toMs: Long,
    val totalListenedMs: Long,
    val playCount: Int,
    val topSongs: List<SongStat>,
    val topSingers: List<SingerStat>,
    /** Monday..Sunday order, label -> ms. */
    val byDayOfWeek: List<Pair<String, Long>>,
    /** 0..23, hour -> ms. */
    val byHour: List<Pair<Int, Long>>
)

/**
 * Wrapped is computed on demand straight from the play_events log,
 * so it is always correct — no scheduled job needed.
 * offset = 0 is the current (still running) period, 1 is the previous one, etc.
 */
class WrappedRepository(private val playEventDao: PlayEventDao) {

    suspend fun compute(period: WrappedPeriod, offset: Int): Wrapped {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        val (start, end, label) = when (period) {
            WrappedPeriod.WEEK -> {
                val monday = today.with(DayOfWeek.MONDAY).minusWeeks(offset.toLong())
                Triple(monday, monday.plusWeeks(1), "week of ${monday}")
            }
            WrappedPeriod.MONTH -> {
                val first = today.withDayOfMonth(1).minusMonths(offset.toLong())
                Triple(first, first.plusMonths(1), "%04d-%02d".format(first.year, first.monthValue))
            }
            WrappedPeriod.YEAR -> {
                val first = today.withDayOfYear(1).minusYears(offset.toLong())
                Triple(first, first.plusYears(1), first.year.toString())
            }
        }

        val fromMs = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val toMs = end.atStartOfDay(zone).toInstant().toEpochMilli()

        val dowRaw = playEventDao.listenedByDayOfWeek(fromMs, toMs)
            .associate { it.bucket.toInt() to it.totalMs }
        // strftime %w: 0=Sunday..6=Saturday; present Monday-first.
        val byDay = listOf(1, 2, 3, 4, 5, 6, 0).map { dow ->
            val name = DayOfWeek.of(if (dow == 0) 7 else dow)
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            name to (dowRaw[dow] ?: 0L)
        }

        val hourRaw = playEventDao.listenedByHour(fromMs, toMs)
            .associate { it.bucket.toInt() to it.totalMs }
        val byHour = (0..23).map { it to (hourRaw[it] ?: 0L) }

        return Wrapped(
            period = period,
            label = label,
            fromMs = fromMs,
            toMs = toMs,
            totalListenedMs = playEventDao.totalListenedMs(fromMs, toMs),
            playCount = playEventDao.playCount(fromMs, toMs),
            topSongs = playEventDao.topSongs(fromMs, toMs, 5),
            topSingers = playEventDao.topSingers(fromMs, toMs, 5),
            byDayOfWeek = byDay,
            byHour = byHour
        )
    }
}
