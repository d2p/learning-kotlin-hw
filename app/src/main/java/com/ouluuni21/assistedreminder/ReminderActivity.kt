package com.ouluuni21.assistedreminder

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Build.*
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.drawToBitmap
import androidx.room.Room
import com.ouluuni21.assistedreminder.db.AppDatabase
import com.ouluuni21.assistedreminder.db.ReminderInfo
import java.io.ByteArrayOutputStream
import java.io.InputStream
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
            val dpd = DatePickerDialog(this, { _, year, monthOfYear, dayOfMonth ->
                // Display selected date in TextView
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                val date = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
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
            val dpd = TimePickerDialog(this, { _, hour, minute ->
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
            var time = timeView.text.toString()
            if (time.isEmpty()) {
                time = "00:00"
                Toast.makeText(
                    applicationContext,
                    "Time was not set, updated to 00:00.",
                    Toast.LENGTH_SHORT
                ).show()
                // return@setOnClickListener
            }

            val current = Calendar.getInstance().time
            val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val datetime: Date = format.parse("$date $time")!!

            val image = findViewById<ImageView>(R.id.thumbnail).drawToBitmap()
            val stream = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val imageInByte: ByteArray = stream.toByteArray()

            val message = findViewById<EditText>(R.id.inpReminderMessage).text.toString()
            val reminder = ReminderInfo(
                null,
                creator_id = uid,
                creation_time = current,
                reminder_time = datetime,
                message = message,
                location_x = "",
                location_y = "",
                reminder_seen = false,
                image = imageInByte
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
                            "Reminder for $date has been created."
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

        // https://devofandroid.blogspot.com/2018/09/pick-image-from-gallery-android-studio_15.html
        // https://stackoverflow.com/questions/35484767/activitycompat-requestpermissions-not-showing-dialog-box
        findViewById<TextView>(R.id.addThumbnail).setOnClickListener {
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
                    requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSION_CODE)
                }
            }
            else{
                // Permission already granted
                pickImageFromGallery()
            }
        }

        findViewById<ImageView>(R.id.btnSpeech).setOnClickListener {
            // Check runtime permission
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_DENIED){
                // Permission denied
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    )) {
                    showExplanation(
                        "Permission Needed",
                        "Requesting permission to record audio to convert speech to text",
                        Manifest.permission.RECORD_AUDIO, RECORD_AUDIO_CODE
                    )
                } else {
                    // Show popup to request runtime permission
                    requestPermission(Manifest.permission.RECORD_AUDIO, RECORD_AUDIO_CODE)
                }
            }
            else {
                // Permission already granted
                this.convertSpeechToText()
            }
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

    private fun showExplanation(
        title: String,
        message: String,
        permission: String,
        permissionRequestCode: Int
    ) {
        val builder = AlertDialog.Builder(this@ReminderActivity)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, { _, _ ->
                requestPermission(
                    permission,
                    permissionRequestCode
                )
            })
        builder.create().show()
    }

    private fun requestPermission(permissionName: String, permissionRequestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permissionName), permissionRequestCode)
    }

    // Handle requested permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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
                    PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission from popup granted
                    this.convertSpeechToText()
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
            findViewById<ImageView>(R.id.thumbnail).setImageBitmap(selectedImage)
            //findViewById<ImageView>(R.id.thumbnail).setImageURI(data?.data)
        }
    }

    private fun convertSpeechToText() {
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val editText = findViewById<EditText>(R.id.inpReminderMessage)
        val micButton = findViewById<ImageView>(R.id.btnSpeech)

        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
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
                return false
            }
        })
    }
}
