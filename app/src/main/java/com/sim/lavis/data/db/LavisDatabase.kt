package com.sim.lavis.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongEntity::class,
        SingerEntity::class,
        SongSingerCrossRef::class,
        PlayEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LavisDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun singerDao(): SingerDao
    abstract fun playEventDao(): PlayEventDao

    companion object {
        @Volatile
        private var instance: LavisDatabase? = null

        fun get(context: Context): LavisDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LavisDatabase::class.java,
                    "lavis.db"
                ).build().also { instance = it }
            }
    }
}
