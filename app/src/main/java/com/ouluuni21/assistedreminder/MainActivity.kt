package com.ouluuni21.assistedreminder

import android.app.*
import android.content.Context
import android.content.Intent
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
import com.ouluuni21.assistedreminder.db.AppDatabase
import com.ouluuni21.assistedreminder.db.Reminder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        showUser()
        refreshListView()

        listView = findViewById(R.id.reminderListView)
        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, v, position, _ ->
            // Retrieve selected Item
            val selectedReminder = listView.adapter.getItem(position) as Reminder
            showMenu(v, R.menu.popup_menu, selectedReminder)
            //showWindow(listView.adapter.getView(position, v, parent), selectedReminder)
        }

        findViewById<Button>(R.id.logout).setOnClickListener {
            Log.d("hw_project", "Logout button clicked")
            if( logout() == 1) {
                this.startActivity(Intent(applicationContext, LoginActivity::class.java))
            }
        }

        findViewById<Button>(R.id.new_reminder).setOnClickListener {
            this.startActivity(Intent(applicationContext, ReminderActivity::class.java))
        }
    }

    private fun showUser() {
        val sharedPref = applicationContext.getSharedPreferences(
            getString(R.string.preference_file), MODE_PRIVATE
        )
        val currentUser = sharedPref.getString("Username", "")
        val user = findViewById<TextView>(R.id.currentUser)
        user.text = currentUser
    }

    override fun onResume() {
        super.onResume()
        showUser()
        refreshListView()
    }

    private fun refreshListView() {
        val refreshTask = LoadReminderInfoEntries()
        refreshTask.execute()
    }

    private fun logout(): Int {
        val sharedPref = applicationContext.getSharedPreferences(
            getString(R.string.preference_file), MODE_PRIVATE
        )
        sharedPref.edit().putInt("LoginStatus", 0).apply()
        return 1
    }

    private fun showWindow(v: View, reminder: Reminder) {
        val popup = ListPopupWindow(this, null, R.attr.listPopupWindowStyle)

        // Set button as the list popup's anchor
        popup.anchorView = v

        // Set list popup's contentresources.getString(
        val items = listOf(
            resources.getString(R.string.edit),
            resources.getString(R.string.create_calendar),
            resources.getString(R.string.delete)
        )
        val adapter = ArrayAdapter(this, R.layout.popup_window, items)
        popup.setAdapter(adapter)

        // Set list popup's item click listener
        popup.setOnItemClickListener { _, _, position: Int, _ ->
            when (position) {
                0 -> {
                    editReminder(this, reminder.uid)
                }
                1 -> {
                    addCalendarEvent(reminder)
                }
                2 -> {
                    deleteDialog(reminder)
                }
                else -> {
                }
            }

            // Dismiss popup.
            popup.dismiss()
        }

        popup.show()
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
                // Update UI

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
        intent.putExtra("rule", "FREQ=YEARLY")
        intent.putExtra("endTime", calendarEvent.timeInMillis + 60 * 60 * 1000)
        intent.putExtra("title", "Reminder from " + r.creator)
        intent.putExtra("location", "N/A")
        intent.putExtra("description", r.message)
        startActivity(intent)
    }

    inner class LoadReminderInfoEntries : AsyncTask<String?, String?, List<Reminder>>() {
        override fun doInBackground(vararg params: String?): List<Reminder> {
            val db = Room
                .databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    getString(R.string.dbFileName)
                )
                //.fallbackToDestructiveMigration()
                .build()
            val reminderInfos = db.reminderDao().getReminderInfos()
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
                            "No items present now. Populating fake view.",
                            Toast.LENGTH_SHORT
                        )
                    toast.setGravity(Gravity.BOTTOM, 0, 400)
                    toast.show()
                    val dummyInfos = dummyList()
                    val adaptor = ReminderHistoryAdaptor(applicationContext, dummyInfos)
                    listView.adapter = adaptor
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
                    ByteArray(0)
                )
            }
            return reminderInfos
        }
    }

    companion object {
        fun showNotification(context: Context, message: String) {
            val channelID = "REMINDER_APP_NOTIFICATION_CHANNEL"
            val notificationId = Random.nextInt(10, 1000) + 5
            // notificationId += Random(notificationId).nextInt(1, 500)

            val notificationBuilder = NotificationCompat.Builder(context, channelID)
                    .setSmallIcon(R.drawable.ic_notif)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setGroup(channelID)

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

        fun setReminder(context: Context, uid: Int, timeInMillis: Long, message: String) {
            val intent = Intent(context, ReminderReceiver::class.java)
            intent.putExtra("uid", uid)
            intent.putExtra("message", message)

            // Create a pending intent to a future action with a unique request code i.e uid
            val pendingIntent =
                    PendingIntent.getBroadcast(context, uid, intent, PendingIntent.FLAG_ONE_SHOT)

            // Create a service to monitor and execute the future action.
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExact(AlarmManager.RTC, timeInMillis, pendingIntent)
        }

        fun editReminder(context: Context, rid: Int) {
            val intent = Intent(context, EditActivity::class.java)
            intent.putExtra("rid", rid)
            Log.d("hw_project", "Set rid to $rid")
            context.startActivity(intent)
        }

        fun cancelReminder(context: Context, pendingIntentId: Int) {
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        pendingIntentId,
                        intent,
                        PendingIntent.FLAG_ONE_SHOT
                    )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }
}