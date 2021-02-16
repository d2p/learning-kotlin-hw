package com.ouluuni21.assistedreminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class ReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)

        val uid = refreshView()

        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        val dateView = findViewById<EditText>(R.id.inpReminderDate)
        dateView.showSoftInputOnFocus = false
        dateView.setOnClickListener {
            val dpd = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                // Display selected date in TextView
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                val date = LocalDate.of(year, monthOfYear+1, dayOfMonth)
                val formattedDate = date.format(formatter)
                dateView.setText(formattedDate)
            }, year, month, day)
            dpd.show()
        }

        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        val timeView = findViewById<EditText>(R.id.inpReminderTime)
        timeView.showSoftInputOnFocus = false
        timeView.setOnClickListener {
            val dpd = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                // Display selected date in TextView
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                val time = LocalTime.of(hour, minute)
                val formattedDate = time.format(formatter)
                timeView.setText(formattedDate)
            }, hour, minute, true)
            dpd.show()
        }

        findViewById<Button>(R.id.btnCreate).setOnClickListener {
            Log.d("hw_project", "Create reminder button clicked")

            // Validate entry values here
            val date = dateView.text.toString()
            if (date.isEmpty()) {
                Toast.makeText(
                        applicationContext,
                        "Date should not be empty and should be in dd.mm.yyyy format!",
                        Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val time = timeView.text.toString()
            if (time.isEmpty()) {
                Toast.makeText(
                    applicationContext,
                    "Time should not be empty and should be in HH:mm format!",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val current = Calendar.getInstance().time
            val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val datetime: Date = format.parse(date + " " + time)!!

            val message = findViewById<EditText>(R.id.inpReminderMessage).text.toString()
            val reminder = ReminderInfo(
                null,
                creator_id = uid,
                creation_time = current,
                reminder_time = datetime,
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
