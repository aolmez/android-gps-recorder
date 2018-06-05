package com.moovel.gpsrecorderplayer.repo

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query

@Dao
internal interface SignalsDao : BaseDao<SignalStamp> {
    @Query("SELECT * from signals")
    fun get(): List<SignalStamp>

    @Query("SELECT * from signals WHERE record_id == :recordId")
    fun getByRecordId(recordId: String): List<SignalStamp>

    @Query("SELECT * from signals WHERE record_id == :recordId AND `index` == :index")
    fun getByRecordIdAndId(recordId: String, index: Long): SignalStamp?
}
