package com.ouluuni21.assistedreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.util.Log
import androidx.room.Room
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.ouluuni21.assistedreminder.db.AppDatabase
import com.ouluuni21.assistedreminder.db.Reminder
import java.util.*

class GeofenceReceiver : BroadcastReceiver() {
    lateinit var key: String
    lateinit var title: String
    lateinit var message: String
    lateinit var applicationContext: Context
    private var isNotifable: Boolean = false

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            val geofencingTransition = geofencingEvent.geofenceTransition

            applicationContext = context.applicationContext

            if (geofencingTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofencingTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                // Retrieve data from intent
                if (intent != null) {
                    key = intent.getStringExtra("key")!!
                    title = intent.getStringExtra("title")!!
                    message = intent.getStringExtra("message")!!
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
                LoadReminderInfoEntry().execute(key).get()

                // Remove geofence
                Log.d("hw_project", "onReceive isNotifable: $isNotifable")
                if (isNotifable) {
                    val triggeringGeofences = geofencingEvent.triggeringGeofences
                    MainActivity.removeGeofences(context, triggeringGeofences)
                }
            }
        }
    }

    inner class LoadReminderInfoEntry : AsyncTask<String?, String?, Reminder>() {
        override fun doInBackground(vararg params: String?): Reminder {
            val rid = params[0]
            val db = Room
                    .databaseBuilder(
                            applicationContext,
                            AppDatabase::class.java,
                            applicationContext.getString(R.string.dbFileName)
                    )
                    //.fallbackToDestructiveMigration()
                    .build()
            val reminder = db.reminderDao().getReminderEntry(rid!!.toInt())
            db.close()

            if (reminder != null) {
                val reminderCalender = GregorianCalendar.getInstance()
                reminderCalender.time = reminder.reminder_time
                // Notify only if on location at the time of the reminder's target datetime
                // with possible ahead time window GEOFENCE_PRETIME
                if ((reminderCalender.timeInMillis - GEOFENCE_PRETIME) <=
                        Calendar.getInstance().timeInMillis) {
                    isNotifable = true
                    Log.d("hw_project", "Location spotted and the time is right")
                }
                if (isNotifable) {
                    val trimMsg = if (reminder.message.length > MOTIF_MESSAGE_LENGTH) {
                        reminder.message.substring(0, MOTIF_MESSAGE_LENGTH) + "..."
                    } else {
                        reminder.message
                    }
                    MainActivity
                            .showNotification(
                                    applicationContext,
                                    title,
                                    message + "\n" + trimMsg
                            )
                }
                else {
                    Log.d("hw_project", "Location spotted but the time is not there yet")
                }
            }
            return reminder
        }
    }
}