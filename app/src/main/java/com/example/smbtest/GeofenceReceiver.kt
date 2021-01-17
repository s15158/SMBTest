package com.example.smbtest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {
    var id = 0

    override fun onReceive(context: Context?, intent: Intent?) {
        val geoEvent = GeofencingEvent.fromIntent(intent)
        val triggering = geoEvent.triggeringGeofences
        for (geo in triggering) {
            when (geoEvent.geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Log.i(TAG, "Entered")
                    val notification = NotificationCompat.Builder(context!!, "GEO")
                        .setSmallIcon(R.mipmap.ic_launcher).setContentTitle("Hello")
                        .setContentText("Welcome to ${geo.requestId}").setAutoCancel(true).build()

                    val notificationManager = NotificationManagerCompat.from(context)
                    notificationManager.notify(id++, notification)
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    Log.i(TAG, "Left")
                    val notification = NotificationCompat.Builder(context!!, "GEO")
                        .setSmallIcon(R.mipmap.ic_launcher).setContentTitle("Goodbye")
                        .setContentText("We hope to see you soon back at ${geo.requestId}").setAutoCancel(true).build()

                    val notificationManager = NotificationManagerCompat.from(context)
                    notificationManager.notify(id++, notification)
                }
                else -> {
                    Log.d(TAG, "Error")
                }
            }
        }
    }

//    private fun createNotificationChannel(context: Context) {
//        val notificationChannel =
//            NotificationChannel("GEO", "GEOFENCE", NotificationManager.IMPORTANCE_DEFAULT)
//        notificationChannel.description = "Channel description"
//
//        val notificationManager = NotificationManagerCompat.from(context)
//        notificationManager.createNotificationChannel(notificationChannel)
//    }

    companion object {
        const val TAG = "GEO_ACTIVITY"
    }
}