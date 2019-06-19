package com.valdizz.locationnotification

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*

/**
 * Foreground service receives location updates and sends notification when the user enters the marked area.
 *
 * @autor Vlad Kornev
 */
class LocationUpdatesService : Service() {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    override fun onCreate() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                Log.i(TAG, "LocationResult: " + locationResult?.lastLocation?.latitude + " : "+locationResult?.lastLocation?.longitude)
            }
        }
        getLastLocation()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val startedFromNotification = intent?.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false) ?: false
        if (startedFromNotification) {
            geofencingEvent(intent)
        } else {
            startForegroundService()
            requestLocationUpdates()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationClient?.lastLocation?.addOnCompleteListener {
            if (it.isSuccessful && it.result != null) {
                Log.i(TAG, "LastLocationResult: " + it.result?.latitude + " : "+it.result?.longitude)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        fusedLocationClient?.requestLocationUpdates(createLocationRequest(), locationCallback, Looper.myLooper())
    }

    @SuppressLint("MissingPermission")
    private fun removeLocationUpdates() {
        fusedLocationClient?.removeLocationUpdates(locationCallback)
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest().apply {
            interval = UPDATE_INTERVAL
            fastestInterval = FASTEST_UPDATE_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(CHANNEL_ID, CHANNEL_NAME)
        }
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setContentTitle(getString(R.string.notification_service_title))
            setSmallIcon(R.drawable.ic_location_searching_white_24dp)
            setOngoing(true)
        }
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun sendNotification() {
        val notification = createNotification()
        with(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
            notify(GEOFENCE_NOTIFICATION_ID, notification)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String) {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        with(getSystemService(NOTIFICATION_SERVICE) as NotificationManager) {
            createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MapsActivity::class.java)
        val pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentIntent(pIntent)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_location_on_white_24dp)
            .setAutoCancel(true)
            .build()
    }

    private fun geofencingEvent(intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            Log.e(TAG, GeofenceErrorMessages.getErrorString(this, geofencingEvent.errorCode))
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            sendNotification()
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(
                Intent(this, MapsActivity::class.java).apply { action = BROADCAST_REMOVE_GEOFENCE })
            PreferenceManager.getDefaultSharedPreferences(applicationContext).edit().clear().apply()
            removeLocationUpdates()
            stopForeground(false)
            stopSelf()
        }
    }

    companion object {
        const val TAG = "LocationUpdatesService"
        const val BROADCAST_REMOVE_GEOFENCE = "com.valdizz.locationnotification.broadcastremovegeofence"
        const val EXTRA_STARTED_FROM_NOTIFICATION = "com.valdizz.locationnotification.notification"
        private const val CHANNEL_ID = "LocationUpdatesServiceChannelID"
        private const val CHANNEL_NAME = "LocationUpdatesServiceChannel"
        private const val NOTIFICATION_ID = 122
        private const val GEOFENCE_NOTIFICATION_ID = 125
        private const val UPDATE_INTERVAL = 5000L
        private const val FASTEST_UPDATE_INTERVAL = 1000L
    }
}
