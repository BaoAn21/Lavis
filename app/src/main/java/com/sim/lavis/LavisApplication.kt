package com.sim.lavis

import android.app.Application
import com.sim.lavis.data.MediaScanner
import com.sim.lavis.data.MusicRepository
import com.sim.lavis.data.SongDownloader
import com.sim.lavis.data.WrappedRepository
import com.sim.lavis.data.db.LavisDatabase
import com.sim.lavis.playback.PlayerManager

/** Poor-man's DI: app-wide singletons, no framework needed at this size. */
class LavisApplication : Application() {

    val database by lazy { LavisDatabase.get(this) }
    val repository by lazy { MusicRepository(database) }
    val wrappedRepository by lazy { WrappedRepository(database.playEventDao()) }
    val mediaScanner by lazy { MediaScanner(this, database.songDao()) }
    val playerManager by lazy { PlayerManager(this) }
    val songDownloader by lazy { SongDownloader(this) }
}
