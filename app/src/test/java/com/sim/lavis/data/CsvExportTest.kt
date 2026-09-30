package com.sim.lavis.data

import com.sim.lavis.data.db.PlayEventExport
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvExportTest {

    private fun row(title: String, singers: String = "") = PlayEventExport(
        id = 1, songId = 7, title = title, playlist = "vpop", singers = singers,
        listenedMs = 42_000, startedAtMs = 0
    )

    private fun fieldsOf(csv: String) = csv.lines()[1]

    @Test
    fun headerAndPlainRow() {
        val csv = CsvExport.playEvents(listOf(row("Hello")))
        assertEquals("id,started_at,started_at_ms,song_id,title,playlist,singers,listened_ms", csv.lines()[0])
        assertEquals("0,7,Hello,vpop,,42000", fieldsOf(csv).substringAfter(",").substringAfter(","))
    }

    @Test
    fun quotesFieldsWithCommasAndQuotes() {
        val csv = CsvExport.playEvents(listOf(row("Say \"hi\", ok", singers = "A; B")))
        val line = fieldsOf(csv)
        assert(line.contains("\"Say \"\"hi\"\", ok\"")) { line }
        assert(line.contains(",A; B,")) { line }
    }

    @Test
    fun emptyLogIsJustHeader() {
        assertEquals(1, CsvExport.playEvents(emptyList()).trimEnd().lines().size)
    }
}
