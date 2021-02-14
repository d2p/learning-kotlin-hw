package com.ouluuni21.assistedreminder

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.ouluuni21.assistedreminder.db.AppDatabase
import com.ouluuni21.assistedreminder.db.ReminderInfo
import java.util.*

class ReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)

        val uid = refreshView()

        findViewById<Button>(R.id.btnCreate).setOnClickListener {
            Log.d("hw_project", "Create reminder button clicked")

            // Validate entry values here
            val date = findViewById<EditText>(R.id.inpReminderDate).text.toString()
            if (date.isEmpty()) {
                Toast.makeText(
                        applicationContext,
                        "Date should not be empty and should be in dd.mm.yyyy format!",
                        Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val message = findViewById<EditText>(R.id.inpReminderMessage).text.toString()
            val reminder = ReminderInfo(
                    null,
                    creator_id = uid,
                    creation_time = "",
                    reminder_time = date,
                    message = message,
                    location_x = "",
                    location_y = "",
                    reminder_seen = false
            )

            // Convert date  string value to Date format using dd.mm.yyyy
            // Here it is assumed that date is in dd.mm.yyyy
            val dateparts = date.split(".").toTypedArray()
            val reminderCalender = GregorianCalendar(
                    dateparts[2].toInt(),
                    dateparts[1].toInt() - 1,
                    dateparts[0].toInt()
            )

            AsyncTask.execute {
                val db = Room.databaseBuilder(
                        applicationContext,
                        AppDatabase::class.java,
                        getString(R.string.dbFileName)
                ).build()
                val ruid = db.reminderDao().insert(reminder).toInt()
                db.close()

                // Reminder happens in the future set reminder
                if (reminderCalender.timeInMillis > Calendar.getInstance().timeInMillis) {
                    // Reminder happens in the future set reminder
                    val msg =
                            "Reminder for ${date} has been created."
                    MainActivity.setReminder(
                        applicationContext,
                        ruid,
                        reminderCalender.timeInMillis,
                        msg
                    )
                }
            }

            if(reminderCalender.timeInMillis > Calendar.getInstance().timeInMillis){
                Toast.makeText(
                    applicationContext,
                    "Reminder for future reminder saved.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshView()
    }

    private fun refreshView(): Int {
        val sharedPref = applicationContext.getSharedPreferences(
            getString(R.string.preference_file), Context.MODE_PRIVATE
        )
        val username = findViewById<TextView>(R.id.reminderAuthor)
        val uid = sharedPref.getInt("Uid", 0)
        username.text = sharedPref.getString("Username", "")
        return uid
    }
}