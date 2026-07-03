package com.sim.lavis.data

import com.sim.lavis.data.db.LavisDatabase
import com.sim.lavis.data.db.SingerEntity
import com.sim.lavis.data.db.SongSingerCrossRef

class MusicRepository(private val db: LavisDatabase) {

    val songDao get() = db.songDao()
    val singerDao get() = db.singerDao()
    val playEventDao get() = db.playEventDao()

    suspend fun createSinger(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1
        val id = singerDao.insert(SingerEntity(name = trimmed))
        return if (id != -1L) id else singerDao.idByName(trimmed) ?: -1
    }

    suspend fun setSongTitle(songId: Long, title: String) {
        val trimmed = title.trim()
        if (trimmed.isNotEmpty()) songDao.rename(songId, trimmed)
    }

    suspend fun assignSinger(songId: Long, singerId: Long) {
        singerDao.link(SongSingerCrossRef(songId, singerId))
    }

    suspend fun unassignSinger(songId: Long, singerId: Long) {
        singerDao.unlink(SongSingerCrossRef(songId, singerId))
    }

    suspend fun deleteSinger(singerId: Long) {
        singerDao.delete(singerId)
    }
}
