package com.moovel.gpsrecorderplayer.repo

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.location.Location

@Entity(
        tableName = "records",
        primaryKeys = ["id"],
        indices = [Index("id")]
)
data class Record(
        val id: String,
        val name: String,
        val start: Long = System.currentTimeMillis()
)

@Entity(
        tableName = "positions",
        primaryKeys = ["index", "record_id"],
        indices = [Index("record_id"), Index("record_id", "index")],
        foreignKeys = [ForeignKey(entity = Record::class, parentColumns = ["id"], childColumns = ["record_id"], onDelete = ForeignKey.CASCADE)]
)
data class Position(
        @ColumnInfo(name = "record_id")
        val recordId: String,
        val index: Long,
        val created: Long = System.currentTimeMillis(),

        val provider: String,
        val time: Long,
        val elapsedRealtimeNanos: Long,
        val latitude: Double,
        val longitude: Double,
        val altitude: Double? = null,
        val speed: Float? = null,
        val bearing: Float? = null,
        val horizontalAccuracyMeters: Float? = null,
        val verticalAccuracyMeters: Float? = null,
        val speedAccuracyMetersPerSecond: Float? = null,
        val bearingAccuracyDegrees: Float? = null
)

private fun Location.toLocationEntity(
        recordId: String,
        index: Long,
        created: Long = System.currentTimeMillis()
): Position {
    return Position(recordId,
            index,
            created,
            provider,
            time,
            elapsedRealtimeNanos,
            latitude,
            longitude,
            if (hasAltitude()) altitude else null,
            if (hasSpeed()) speed else null,
            if (hasBearing()) bearing else null,
            if (hasAccuracy()) accuracy else null,
            if (hasVerticalAccuracy()) verticalAccuracyMeters else null,
            if (hasSpeedAccuracy()) speedAccuracyMetersPerSecond else null,
            if (hasBearingAccuracy()) bearingAccuracyDegrees else null
    )
}

private fun Position.toLocation(): Location {
    val l = Location(provider)
    l.time = time
    l.elapsedRealtimeNanos = elapsedRealtimeNanos
    l.latitude = latitude
    l.longitude = longitude
    altitude?.let { l.altitude = it }
    speed?.let { l.speed = it }
    bearing?.let { l.bearing = it }
    bearingAccuracyDegrees?.let { l.bearingAccuracyDegrees = it }
    speedAccuracyMetersPerSecond?.let { l.speedAccuracyMetersPerSecond = it }
    horizontalAccuracyMeters?.let { l.accuracy = it }
    verticalAccuracyMeters?.let { l.verticalAccuracyMeters = it }
    return l
}

//@Entity(
//        tableName = "signals",
//        foreignKeys = [ForeignKey(entity = Record::class, parentColumns = ["id"], childColumns = ["record_id"], onDelete = ForeignKey.CASCADE)]
//)
//data class SignalEntity(
//        val recordId: String
//)