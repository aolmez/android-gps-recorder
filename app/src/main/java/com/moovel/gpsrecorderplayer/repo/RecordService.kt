package com.moovel.gpsrecorderplayer.repo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import com.google.android.gms.maps.model.LatLng
import com.moovel.gpsrecorderplayer.R
import java.util.UUID

class RecordService : Service(), IRecordService {
    companion object {
        private const val NOTIFICATION_ID = 0x4554
        private const val NOTIFICATION_CHANNEL_ID = "record"

        fun of(binder: IBinder): IRecordService {
            return (binder as RecordBinder).service
        }

        private fun Location.toPosition(
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
    }

    private val db = RecordsDatabase.getInstance(this)
    private val recordsDao = db.recordsDao()
    private val positionsDao = db.positionsDao()

    private val handlerThread = HandlerThread("RecordService")
    private val handler = Handler(handlerThread.apply { start() }.looper)
    private val mainHandler = Handler()

    private var record: Record? = null
    private var index = 0L
    private val location by lazy { LocationLiveData(this) }
    private val recording = MutableLiveData<Boolean>()
    private val polyline = MutableLiveData<List<LatLng>>()
    private var polylineList = emptyList<LatLng>()
        set(value) {
            field = value
            polyline.value = value
        }

    private val locationObserver = Observer<Location> {
        it?.let {
            polylineList = polylineList.plus(LatLng(it.latitude, it.longitude))
            record?.insertPositionAsync(index++, it)
        }
    }

    override fun onCreate() {
        super.onCreate()
        setupNotificationChannel()
    }

    override fun start(name: String) {
        if (record != null) throw IllegalStateException("Stop recording before")
        record = Record(UUID.randomUUID().toString(), name)
        record?.insertRecordAsync()
        location.observeForever(locationObserver)
        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentText(getString(R.string.recording))
                .build()
        startForeground(NOTIFICATION_ID, notification)
        recording.value = true
    }

    override fun stop() {
        location.removeObserver(locationObserver)
        polylineList = emptyList()
        record?.complete()
        record = null
        index = 0L
        recording.value = false
    }

    override fun current(): Record? {
        return record
    }

    override fun rename(name: String) {
        if (record == null) throw IllegalStateException("Not recording")
        record = record?.copy(name = name)
        record?.updateRecordAsync()
    }

    override fun polyline(): LiveData<List<LatLng>> {
        return polyline
    }

    override fun locations(): LiveData<Location> {
        return location
    }

    override fun isRecording(): LiveData<Boolean> {
        return recording
    }

    private fun Record.insertRecordAsync() {
        handler.post { recordsDao.insert(this) }
    }

    private fun Record.updateRecordAsync() {
        handler.post { recordsDao.update(this) }
    }

    private fun Record.insertPositionAsync(index: Long, location: Location) {
        handler.post { positionsDao.insert(location.toPosition(this.id, index)) }
    }

    private fun Record.complete() {
        handler.post {
            mainHandler.post {
                if (record?.id == null) {
                    stopForeground(true)
                }
            }
        }
    }

    private fun setupNotificationChannel() {
        val name = getString(R.string.record_new_record)
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        handlerThread.quitSafely()
    }

    override fun onBind(intent: Intent?): IBinder {
        return RecordBinder(this)
    }

    private class RecordBinder(val service: RecordService) : Binder()
}
