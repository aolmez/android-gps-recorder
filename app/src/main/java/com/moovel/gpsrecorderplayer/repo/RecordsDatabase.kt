package com.moovel.gpsrecorderplayer.repo

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database(version = 1,
        entities = [
            Record::class,
            Position::class
        ])
internal abstract class RecordsDatabase : RoomDatabase() {
    abstract fun recordsDao(): RecordsDao
    abstract fun positionsDao(): PositionsDao
}