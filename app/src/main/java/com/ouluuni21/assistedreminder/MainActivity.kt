package com.ouluuni21.assistedreminder

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Html.FROM_HTML_MODE_LEGACY
import android.text.Spanned
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.ouluuni21.assistedreminder.db.AppDatabase
import com.ouluuni21.assistedreminder.db.Reminder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

const val GEOFENCE_ID = "REMINDER_GEOFENCE_ID"
const val GEOFENCE_PRETIME = 30 * 60 * 1000 // 30 mins for now
const val GEOFENCE_EXPIRATION = 3 * 60 * 60 * 1000 // 3 hours
const val GEOFENCE_DWELL_DELAY =  10 * 1000 // 10 secs
const val MOTIF_MESSAGE_LENGTH = 37

class MainActivity : AppCompatActivity() {

    private lateinit var geofencingClient: GeofencingClient
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        geofencingClient = LocationServices.getGeofencingClient(this)

        val showAll: Boolean = intent!!.getBooleanExtra("showall", false)

        updateView()
        refreshListView()

        listView = findViewById(R.id.reminderListView)
        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, v, position, _ ->
            // Retrieve selected Item
            val selectedReminder = listView.adapter.getItem(position) as Reminder
            showMenu(v, R.menu.popup_menu, selectedReminder)
        }

        findViewById<Button>(R.id.btnShowAll).setOnClickListener {
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.putExtra("showall", !showAll)
            this.startActivity(intent)
        }

        findViewById<Button>(R.id.new_reminder).setOnClickListener {
            this.startActivity(Intent(applicationContext, ReminderActivity::class.java))
        }

        findViewById<TextView>(R.id.currentUser).setOnClickListener {
            this.startActivity(Intent(applicationContext, ProfileActivity::class.java))
        }
    }

    private fun updateView() {
        val sharedPref = applicationContext.getSharedPreferences(
            getString(R.string.preference_file), MODE_PRIVATE
        )
        val currentUser = sharedPref.getString("Username", "")
        val user = findViewById<TextView>(R.id.currentUser)
        user.text = currentUser
    }

    override fun onResume() {
        super.onResume()
        updateView()
        refreshListView()
    }

    private fun refreshListView() {
        val refreshTask = LoadReminderInfoEntries()
        val showAll: Boolean = intent!!.getBooleanExtra("showall", false)
        val button = findViewById<Button>(R.id.btnShowAll)
        if (showAll) {
            button.setText("Show old")
        }
        else {
            button.setText(" Show all")
        }
        refreshTask.execute()
    }

    private fun showMenu(v: View, @MenuRes menuRes: Int, reminder: Reminder) {
        val popup = PopupMenu(this, v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        // https://resocoder.com/2018/02/02/popup-menu-with-icons-android-kotlin-tutorial-code/
        val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
        fieldMPopup.isAccessible = true
        val mPopup = fieldMPopup.get(popup)
        mPopup.javaClass
            .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
            .invoke(mPopup, true)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.menuEditReminder -> {
                    editReminder(this, reminder.uid)
                }
                R.id.menuCreateCalendar -> {
                    addCalendarEvent(reminder)
                }
                R.id.menuDeleteReminder -> {
                    deleteDialog(reminder)
                }
                else -> {
                }
            }
            true
        }
        popup.setOnDismissListener {
            // Respond to popup being dismissed.
        }
        popup.show()
    }

    private fun Date.convertLongToDate(): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(this)
    }

    private fun deleteDialog(reminder: Reminder) {
        val message =
            getString(
                R.string.delete_alert,
                reminder.reminder_time.convertLongToDate(),
                reminder.creator
            )
        val styledText: Spanned = Html.fromHtml(message, FROM_HTML_MODE_LEGACY)

        // Show AlertDialog to delete the reminder
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Delete reminder?")
            .setMessage(styledText)
            .setPositiveButton("Delete") { _, _ ->
                // Delete from database
                AsyncTask.execute {
                    val db = Room
                            .databaseBuilder(
                                applicationContext,
                                AppDatabase::class.java,
                                getString(R.string.dbFileName)
                            )
                            .build()
                    db.reminderDao().delete(reminder.uid)
                    db.close()
                }
                // Cancel pending time based reminder
                cancelReminder(applicationContext, reminder.uid)

                // Refresh payments list
                refreshListView()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                // Do nothing
                dialog.dismiss()
            }
            .show()
    }

    private fun addCalendarEvent(r: Reminder) {
        val calendarEvent: Calendar = Calendar.getInstance()
        calendarEvent.time = r.reminder_time
        val intent = Intent(Intent.ACTION_EDIT)
        intent.type = "vnd.android.cursor.item/event"
        intent.putExtra("beginTime", calendarEvent.timeInMillis)
        intent.putExtra("allDay", false)
        intent.putExtra("endTime", calendarEvent.timeInMillis + 60 * 60 * 1000)
        intent.putExtra("title", "Reminder from " + r.creator)
        intent.putExtra("location", "N/A")
        intent.putExtra("description", r.message)
        startActivity(intent)
    }

    inner class LoadReminderInfoEntries : AsyncTask<String?, String?, List<Reminder>>() {
        override fun doInBackground(vararg params: String?): List<Reminder> {
            val showAll: Boolean = intent!!.getBooleanExtra("showall", false)
            val db = Room
                .databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    getString(R.string.dbFileName)
                )
                //.fallbackToDestructiveMigration()
                .build()
            val reminderInfos = if (showAll) {
                db.reminderDao().getAllReminderInfos()
            } else {
                db.reminderDao().getReminderInfos(Calendar.getInstance().time)
            }
            db.close()
            return reminderInfos
        }

        override fun onPostExecute(reminderInfos: List<Reminder>?) {
            super.onPostExecute(reminderInfos)
            if (reminderInfos != null) {
                if (reminderInfos.isNotEmpty()) {
                    val adaptor = ReminderHistoryAdaptor(applicationContext, reminderInfos)
                    listView.adapter = adaptor
                } else {
                    //listView.adapter = null
                    val toast =
                        Toast.makeText(
                            applicationContext,
                            "No occurred reminders present yet or list is empty.",
                            Toast.LENGTH_SHORT
                        )
                    toast.setGravity(Gravity.BOTTOM, 0, 400)
                    toast.show()
                    //val dummyInfos = dummyList()
                    //val adaptor = ReminderHistoryAdaptor(applicationContext, dummyInfos)
                    //listView.adapter = adaptor
                }
            }
        }

        private fun currentTime(): Date {
            return Calendar.getInstance().time
        }

        private fun dummyList(): List<Reminder> {
            val reminderInfos = mutableListOf<Reminder>()
            for (i in 0 until 10) {
                reminderInfos += Reminder(
                    i,
                    "Author $i",
                    currentTime(),
                    "Dummy reminder text entry",
                    0.0,
                    0.0,
                    false,
                    ByteArray(0)
                )
            }
            return reminderInfos
        }
    }

    companion object {
        @SuppressLint("MissingPermission")
        fun createGeoFence(context: Context, location: LatLng, key: String, title: String, timeInMillis: Long) {
            Log.d("hw_project", "Created geofence job for $key")

            val geofence = Geofence.Builder()
                    .setRequestId(GEOFENCE_ID)
                    .setCircularRegion(location.latitude, location.longitude, GEOFENCE_RADIUS.toFloat())
                    // Set expiration to the reminder time + some hours
                    .setExpirationDuration(timeInMillis + GEOFENCE_EXPIRATION.toLong())
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER) // or Geofence.GEOFENCE_TRANSITION_DWELL)
                    .setLoiteringDelay(GEOFENCE_DWELL_DELAY)
                    .build()

            val geofenceRequest = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER) // + GeofencingRequest.INITIAL_TRIGGER_DWELL)
                    .addGeofence(geofence)
                    .build()

            val loc = String.format(
                    Locale.getDefault(),
                    "Lat: %1$.5f, Lng: %2$.5f",
                    location.latitude,
                    location.longitude
            )

            val intent = Intent(context, GeofenceReceiver::class.java)
                    .putExtra("key", key)
                    .putExtra("title", title)
                    .putExtra("message", "Geofence alert - $loc")

            val pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
            )

            LocationServices.getGeofencingClient(context).addGeofences(geofenceRequest, pendingIntent)
        }

        fun removeGeofences(context: Context, triggeringGeofenceList: MutableList<Geofence>) {
            Log.d("hw_project", "Geofence event removed")
            val geofenceIdList = mutableListOf<String>()
            for (entry in triggeringGeofenceList) {
                geofenceIdList.add(entry.requestId)
            }
            LocationServices.getGeofencingClient(context).removeGeofences(geofenceIdList)
        }

        fun showNotification(context: Context, title: String, message: String) {
            val channelID = "REMINDER_APP_NOTIFICATION_CHANNEL"
            val notificationId = Random.nextInt(10, 1000) + 5

            // Create an explicit intent for an Activity in your app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

            val notificationBuilder = NotificationCompat.Builder(context, channelID)
                    .setSmallIcon(R.drawable.ic_assignment)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_label_important))
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setGroup(channelID)
                    .setAutoCancel(true)

            val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Notification channel needed since Android 8
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelID,
                    context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.app_name)
                }
                notificationManager.createNotificationChannel(channel)
            }

            notificationManager.notify(notificationId, notificationBuilder.build())
        }

        fun setReminder(context: Context, rid: Int, timeInMillis: Long, title: String, message: String, latLng: LatLng) {
            // Remove old work task if it has been scheduled earlier
            cancelReminder(context, rid)

            if (latLng.latitude != 0.0 && latLng.longitude != 0.0) {
                Log.d("hw_project", "Create location notification reminder $rid")
                createGeoFence(context, latLng, rid.toString(), title, timeInMillis)
            }
            else {
                Log.d("hw_project", "Create timed notification reminder $rid")

                val trimMsg = if (message.length > MOTIF_MESSAGE_LENGTH) {
                    message.substring(0, MOTIF_MESSAGE_LENGTH) + "..."
                } else {
                    message
                }

                // Prepare work task
                val reminderParameters = Data.Builder()
                        .putString("title", title)
                        .putString("message", trimMsg)
                        .putInt("uid", rid)
                        .build()

                // Get minutes from now until reminder
                var minutesFromNow = 0L
                if (timeInMillis > System.currentTimeMillis())
                    minutesFromNow = timeInMillis - System.currentTimeMillis()

                val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                        .setInputData(reminderParameters)
                        .setInitialDelay(minutesFromNow, TimeUnit.MILLISECONDS)
                        .addTag(rid.toString())
                        .build()

                WorkManager.getInstance(context).enqueue(reminderRequest)
            }
        }

        fun editReminder(context: Context, rid: Int) {
            val intent = Intent(context, EditActivity::class.java)
            intent.putExtra("rid", rid)
            Log.d("hw_project", "Edit timed reminder $rid")
            context.startActivity(intent)
        }

        fun cancelReminder(context: Context, rid: Int) {
            Log.d("hw_project", "Remove timed notification reminder $rid")
            WorkManager.getInstance(context).cancelAllWorkByTag(rid.toString())
            // TODO removing respective geofence
        }
    }
}