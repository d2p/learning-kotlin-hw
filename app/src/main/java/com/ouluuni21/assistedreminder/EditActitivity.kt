package com.ouluuni21.assistedreminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.ouluuni21.assistedreminder.db.AppDatabase
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class EditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        val rid : Int = intent!!.getIntExtra("rid", 0)
        Log.d("hw_project", "Read extra rid $rid")

        refreshView(rid)

        val dateView = findViewById<EditText>(R.id.inpEditReminderDate)
        dateView.showSoftInputOnFocus = false
        dateView.setOnClickListener {
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            val date = LocalDate.parse(dateView.text.toString(), formatter)
            val dpd = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                // Display selected date in TextView
                val dateUpd = LocalDate.of(year, monthOfYear+1, dayOfMonth)
                val formattedDate = dateUpd.format(formatter)
                dateView.setText(formattedDate)
            }, date.year, date.monthValue-1, date.dayOfMonth)
            dpd.show()
        }

        val timeView = findViewById<EditText>(R.id.inpEditReminderTime)
        timeView.showSoftInputOnFocus = false
        timeView.setOnClickListener {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            var timeInp = timeView.text.toString()
            if (timeInp.isEmpty()) timeInp = "00:00"
            val time = LocalTime.parse(timeInp, formatter)
            val dpd = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                // Display selected date in TextView
                val timeUpd = LocalTime.of(hour, minute)
                val formattedDate = timeUpd.format(formatter)
                timeView.setText(formattedDate)
            }, time.hour, time.minute, true)
            dpd.show()
        }

        findViewById<Button>(R.id.btnUpdate).setOnClickListener {
            Log.d("hw_project", "Update reminder button clicked")

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

            val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val datetime: Date = format.parse(date + " " + time)!!

            val creator = findViewById<TextView>(R.id.editReminderAuthor).text.toString()
            val message = findViewById<EditText>(R.id.inpEditReminderMessage).text.toString()
            val reminder = Reminder(
                uid = rid,
                creator = creator,
                reminder_time = datetime,
                message = message
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
                db.reminderDao().updateReminderEntry(reminder.uid, reminder.reminder_time, reminder.message)
                db.close()

                // Reminder happens in the future set reminder
                if (reminderCalender.timeInMillis > Calendar.getInstance().timeInMillis) {
                    // reminder happens in the future set reminder
                    val notif =
                        "Reminder for ${date} has been created."
                    MainActivity.setReminder(
                        applicationContext,
                        rid,
                        reminderCalender.timeInMillis,
                        notif
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
        val rid : Int = intent!!.getIntExtra("rid",0)
        refreshView(rid)
    }

    private fun refreshView(rid: Int) {
        val refreshTask = LoadReminderInfoEntry()
        refreshTask.execute(rid.toString())
    }

    private fun convertLongToDate(time: Date): String {
        val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return format.format(time)
    }

    private fun convertLongToTime(time: Date): String {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(time)
    }


    inner class LoadReminderInfoEntry : AsyncTask<String?, String?, Reminder>() {
        override fun doInBackground(vararg params: String?): Reminder {
            val rid = params[0]
            val db = Room
                .databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    getString(R.string.dbFileName)
                )
                //.fallbackToDestructiveMigration()
                .build()
            val reminder = db.reminderDao().getReminderEntry(rid!!.toInt())
            db.close()
            return reminder
        }

        override fun onPostExecute(reminder: Reminder?) {
            super.onPostExecute(reminder)
            if (reminder != null) {
                val editAuthor = findViewById<TextView>(R.id.editReminderAuthor) as TextView
                val editDate = findViewById<EditText>(R.id.inpEditReminderDate) as EditText
                val editTime = findViewById<EditText>(R.id.inpEditReminderTime) as EditText
                val editText = findViewById<EditText>(R.id.inpEditReminderMessage) as EditText
                editAuthor.text = reminder.creator
                editDate.setText(convertLongToDate(reminder.reminder_time))
                editTime.setText(convertLongToTime(reminder.reminder_time))
                editText.setText(reminder.message)
            } else {
                val toast =
                    Toast.makeText(applicationContext, "No reminder with such id found.", Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.BOTTOM, 0, 400)
                toast.show()
            }
        }
    }
}