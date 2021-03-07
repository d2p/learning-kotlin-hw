package com.ouluuni21.assistedreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {
    lateinit var key: String
    lateinit var text: String

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            val geofencingTransition = geofencingEvent.geofenceTransition

            if (geofencingTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofencingTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                // Retrieve data from intent
                if (intent != null) {
                    key = intent.getStringExtra("key")!!
                    text = intent.getStringExtra("message")!!
                }
/*
                val firebase = Firebase.database(getString(R.string.db_url))
                val reference = firebase.getReference("reminders")
                val reminderListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val reminder = snapshot.getValue<Reminder>()
                        if (reminder != null) {
                            MainActivity
                                    .showNotification(
                                            context.applicationContext,
                                            "Location\nLat: ${reminder.lat} - Lon: ${reminder.lon}"
                                    )
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        println("reminder:onCancelled: ${error.details}")
                    }
                }
                val child = reference.child(key)
                child.addValueEventListener(reminderListener)
*/
                // remove geofence
                val triggeringGeofences = geofencingEvent.triggeringGeofences
                MapActivity.removeGeofences(context, triggeringGeofences)
            }
        }
    }
}