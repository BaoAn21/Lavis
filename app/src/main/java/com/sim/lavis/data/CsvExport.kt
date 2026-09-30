package com.sim.lavis.data

import com.sim.lavis.data.db.PlayEventExport
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Flattens the listening log into RFC 4180 CSV, for analysis in a spreadsheet. */
object CsvExport {

    private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun playEvents(rows: List<PlayEventExport>): String {
        val zone = ZoneId.systemDefault()
        return buildString {
            appendLine("id,started_at,started_at_ms,song_id,title,playlist,singers,listened_ms")
            for (r in rows) {
                val startedAt = timestampFormat.format(Instant.ofEpochMilli(r.startedAtMs).atZone(zone))
                appendLine(
                    listOf(
                        r.id.toString(),
                        startedAt,
                        r.startedAtMs.toString(),
                        r.songId.toString(),
                        escape(r.title),
                        escape(r.playlist),
                        escape(r.singers),
                        r.listenedMs.toString()
                    ).joinToString(",")
                )
            }
        }
    }

    /** Quote a field if it contains a delimiter, quote or newline; double any embedded quotes. */
    private fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
}
