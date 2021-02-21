package com.ouluuni21.assistedreminder

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.drawToBitmap
import androidx.room.Room
import com.ouluuni21.assistedreminder.db.AppDatabase
import com.ouluuni21.assistedreminder.db.Reminder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class EditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        val rid: Int = intent!!.getIntExtra("rid", 0)
        Log.d("hw_project", "Read extra rid $rid")

        refreshView(rid)

        val dateView = findViewById<EditText>(R.id.inpEditReminderDate)
        dateView.showSoftInputOnFocus = false
        dateView.setOnClickListener {
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            val date = LocalDate.parse(dateView.text.toString(), formatter)
            val dpd = DatePickerDialog(this, { _, year, monthOfYear, dayOfMonth ->
                // Display selected date in TextView
                val dateUpd = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
                val formattedDate = dateUpd.format(formatter)
                dateView.setText(formattedDate)
            }, date.year, date.monthValue - 1, date.dayOfMonth)
            dpd.show()
        }

        val timeView = findViewById<EditText>(R.id.inpEditReminderTime)
        timeView.showSoftInputOnFocus = false
        timeView.setOnClickListener {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            var timeInp = timeView.text.toString()
            if (timeInp.isEmpty()) timeInp = "00:00"
            val time = LocalTime.parse(timeInp, formatter)
            val dpd = TimePickerDialog(this, { _, hour, minute ->
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
            val datetime: Date = format.parse("$date $time")!!

            val creator = findViewById<TextView>(R.id.editReminderAuthor).text.toString()
            val message = findViewById<EditText>(R.id.inpEditReminderMessage).text.toString()
            val image = findViewById<ImageView>(R.id.editThumbnail).drawToBitmap()
            val stream = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val imageInByte: ByteArray = stream.toByteArray()

            val reminder = Reminder(
                uid = rid,
                creator = creator,
                reminder_time = datetime,
                message = message,
                image = imageInByte
            )

            // Convert date string value to Date format using dd.mm.yyyy
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
                db.reminderDao().updateReminderEntry(
                    reminder.uid, reminder.reminder_time, reminder.message, reminder.image)
                db.close()

                // Reminder happens in the future set reminder
                if (reminderCalender.timeInMillis > Calendar.getInstance().timeInMillis) {
                    // reminder happens in the future set reminder
                    val notif =
                            "Reminder for $date has been created."
                    MainActivity.setReminder(
                            applicationContext,
                            rid,
                            reminderCalender.timeInMillis,
                            notif
                    )
                }
            }

            if (reminderCalender.timeInMillis > Calendar.getInstance().timeInMillis) {
                Toast.makeText(
                        applicationContext,
                        "Reminder for future reminder saved.",
                        Toast.LENGTH_SHORT
                ).show()
            }
            finish()
        }

        findViewById<ImageView>(R.id.btnEditSpeech).setOnClickListener {
            // Check runtime permission
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_DENIED) {
                // Permission denied
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.RECORD_AUDIO)) {
                    showExplanation("Permission Needed",
                            "Requesting permission to record audio to convert speech to text",
                            Manifest.permission.RECORD_AUDIO, RECORD_AUDIO_CODE)
                } else {
                    // Show popup to request runtime permission
                    requestPermission(Manifest.permission.RECORD_AUDIO, RECORD_AUDIO_CODE)
                }
            } else {
                // Permission already granted
                convertSpeechToText()
            }
        }

        findViewById<TextView>(R.id.editAddThumbnail).setOnClickListener {
            // Check runtime permission
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_DENIED){
                // Permission denied
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )) {
                    showExplanation(
                        "Permission Needed",
                        "Requesting permission to access gallery for images",
                        Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSION_CODE
                    )
                } else {
                    // Show popup to request runtime permission
                    requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                        PERMISSION_CODE
                    )
                }
            }
            else{
                // Permission already granted
                pickImageFromGallery()
            }
        }
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
                val editAuthor = findViewById<TextView>(R.id.editReminderAuthor)
                val editDate = findViewById<EditText>(R.id.inpEditReminderDate)
                val editTime = findViewById<EditText>(R.id.inpEditReminderTime)
                val editText = findViewById<EditText>(R.id.inpEditReminderMessage)
                val editThumbnail = findViewById<ImageView>(R.id.editThumbnail)
                val imageStream = ByteArrayInputStream(reminder.image)
                val theImage = BitmapFactory.decodeStream(imageStream)

                editAuthor.text = reminder.creator
                editDate.setText(convertLongToDate(reminder.reminder_time))
                editTime.setText(convertLongToTime(reminder.reminder_time))
                editText.setText(reminder.message)
                editThumbnail.setImageBitmap(theImage)
            } else {
                val toast =
                        Toast.makeText(applicationContext, "No reminder with such id found.", Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.BOTTOM, 0, 400)
                toast.show()
            }
        }
    }

    private fun pickImageFromGallery() {
        //Intent to pick image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    companion object {
        // Image pick code
        private const val IMAGE_PICK_CODE = 1000
        // Permission code
        private const val PERMISSION_CODE = 1001
        // Record audio request code
        private const val RECORD_AUDIO_CODE = 1
    }

    private fun showExplanation(title: String,
                                message: String,
                                permission: String,
                                permissionRequestCode: Int) {
        val builder = AlertDialog.Builder(this@EditActivity)
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, {
                    _, _ -> requestPermission(permission, permissionRequestCode) })
        builder.create().show()
    }

    private fun requestPermission(permissionName: String, permissionRequestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permissionName), permissionRequestCode)
    }

    // Handle requested permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission from popup granted
                    pickImageFromGallery()
                } else {
                    // Permission from popup denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            RECORD_AUDIO_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                    // Permission from popup granted
                    convertSpeechToText()
                } else {
                    // Permission from popup denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap? {
        var width = image.width
        var height = image.height
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    // Handle result of picked image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            val imageUri: Uri? = data?.data
            val imageStream: InputStream? = contentResolver.openInputStream(imageUri!!)
            var selectedImage = BitmapFactory.decodeStream(imageStream)
            selectedImage = getResizedBitmap(selectedImage!!,64)
            findViewById<ImageView>(R.id.editThumbnail).setImageBitmap(selectedImage)
        }
    }

    private fun convertSpeechToText() {
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val editText = findViewById<EditText>(R.id.inpEditReminderMessage)
        val micButton = findViewById<ImageView>(R.id.btnEditSpeech)

        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        val listener = SpeechListener(editText, micButton)

        speechRecognizer.setRecognitionListener(listener)

        micButton.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, motionEvent: MotionEvent): Boolean {
                when (motionEvent.action) {
                    MotionEvent.ACTION_UP -> {
                        speechRecognizer.stopListening()
                        v.performClick()
                    }
                    MotionEvent.ACTION_DOWN -> {
                        micButton.setImageResource(R.drawable.ic_mic)
                        speechRecognizer.startListening(speechRecognizerIntent)
                    }
                }
                return true
            }
        })
    }
}
