package com.ouluuni21.assistedreminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.ouluuni21.assistedreminder.db.AppDatabase
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        showUser()
        refreshListView()

        listView = findViewById<ListView>(R.id.reminderListView)
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            // Retrieve selected Item
            val selectedReminder = listView.adapter.getItem(position) as Reminder
            val message =
                "Do you want to delete ${selectedReminder.uid} reminder on ${selectedReminder.reminder_time} from ${selectedReminder.creator} ?"

            // Show AlertDialog to delete the reminder
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Delete reminder?")
                .setMessage(message)
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
                        db.reminderDao().delete(selectedReminder.uid)
                        db.close()
                    }
                    // Cancel pending time based reminder
                    cancelReminder(applicationContext, selectedReminder.uid)

                    // Refresh payments list
                    refreshListView()
                }
                .setNeutralButton("Edit") { _, _ ->
                    // Go to editing activity
                    editReminder(this, selectedReminder.uid)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    // Do nothing
                    dialog.dismiss()
                }
                .show()
        }

        findViewById<Button>(R.id.logout).setOnClickListener {
            Log.d("hw_project", "Logout button clicked")

            logout()

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
                        Toast.makeText(applicationContext, "No items present now. Populating fake view.", Toast.LENGTH_SHORT)
                    toast.setGravity(Gravity.BOTTOM, 0, 400)
                    toast.show()
                    val dummyInfos = dummyList()
                    val adaptor = ReminderHistoryAdaptor(applicationContext, dummyInfos)
                    listView.adapter = adaptor
                }
            }
        }

        private fun dummyList(): List<Reminder> {
            val reminderInfos = mutableListOf<Reminder>()
            for (i in 0 until 10) {
                reminderInfos += Reminder(i, "Author " + i, "01/01/2021", "Dummy reminder text entry")
            }
            return reminderInfos
        }
    }

    companion object {
        fun showNofitication(context: Context, message: String) {

            val CHANNEL_ID = "REMINDER_APP_NOTIFICATION_CHANNEL"
            val notificationId = Random.nextInt(10, 1000) + 5
            // notificationId += Random(notificationId).nextInt(1, 500)

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notif)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setGroup(CHANNEL_ID)

            val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Notification channel needed since Android 8
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                        CHANNEL_ID,
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