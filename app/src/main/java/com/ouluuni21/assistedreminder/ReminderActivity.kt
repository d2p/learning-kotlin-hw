package com.ouluuni21.assistedreminder

import android.Manifest
import android.app.*
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
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.drawToBitmap
import androidx.room.Room
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
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

        initLocCoords()

        val uid = refreshView()

        val current = Calendar.getInstance()
        val year = current.get(Calendar.YEAR)
        val month = current.get(Calendar.MONTH)
        val day = current.get(Calendar.DAY_OF_MONTH)
        val dateView = findViewById<EditText>(R.id.inpReminderDate)
        dateView.showSoftInputOnFocus = false
        dateView.inputType = InputType.TYPE_NULL
        dateView.isClickable = true
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

        val hour = current.get(Calendar.HOUR_OF_DAY)
        val minute = current.get(Calendar.MINUTE)
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
            // Very quick and dirty way to get latest marker info
            val sharedPref = applicationContext.getSharedPreferences(
                    getString(R.string.preference_file), Context.MODE_PRIVATE
            )

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

            val currentCal = Calendar.getInstance()
            val dateFormat = "dd.MM.yyyy HH:mm"
            val format = SimpleDateFormat(dateFormat, Locale.getDefault())
            val datetime: Date = format.parse("$date $time")!!

            val image = findViewById<ImageView>(R.id.thumbnail).drawToBitmap()
            val stream = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val imageInByte: ByteArray = stream.toByteArray()

            val reminderCalender = GregorianCalendar.getInstance()
            reminderCalender.time = datetime

            val doNotif = findViewById<Switch>(R.id.switchNotif).isChecked
            var isFutureReminder = false
            if (reminderCalender.timeInMillis > currentCal.timeInMillis) {
                isFutureReminder = true
            }

            val latitude = sharedPref.getFloat("latitude", 0.0F).toDouble()
            val longitude = sharedPref.getFloat("longitude", 0.0F).toDouble()

            val message = findViewById<EditText>(R.id.inpReminderMessage).text.toString()
            val reminder = ReminderInfo(
                null,
                creator_id = uid,
                creation_time = current.time,
                reminder_time = datetime,
                message = message,
                location_x = latitude,
                location_y = longitude,
                reminder_seen = !isFutureReminder,
                show_notif = doNotif,
                image = imageInByte
            )

            AsyncTask.execute {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    getString(R.string.dbFileName)
                ).build()
                val ruid = db.reminderDao().insert(reminder).toInt()
                val newReminder = db.reminderDao().getReminderEntry(ruid!!.toInt())
                db.close()

                // Reminder happens in the future set reminder
                if (isFutureReminder && doNotif) {
                    val title = "$date $time from ${newReminder.creator}"
                    val latLng = LatLng(latitude, longitude)
                    MainActivity.setReminder(
                        applicationContext,
                        ruid,
                        reminderCalender.timeInMillis,
                        title,
                        message,
                        latLng
                    )
                }
            }

            if (isFutureReminder && doNotif) {
                Toast.makeText(
                    applicationContext,
                    "Reminder for future reminder saved.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            finish()
        }

        findViewById<ImageView>(R.id.addMapLocation).setOnClickListener {
            this.startActivity(Intent(applicationContext, MapActivity::class.java))
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

    private fun initLocCoords() {
        val sharedPref = applicationContext.getSharedPreferences(
                getString(R.string.preference_file), Context.MODE_PRIVATE
        )
        sharedPref.edit().putFloat("latitude", 0.0F).apply()
        sharedPref.edit().putFloat("longitude", 0.0F).apply()
    }

    private fun refreshView(): Int {
        val sharedPref = applicationContext.getSharedPreferences(
            getString(R.string.preference_file), Context.MODE_PRIVATE
        )
        val username = findViewById<TextView>(R.id.reminderAuthor)
        val uid = sharedPref.getInt("Uid", 0)
        username.text = sharedPref.getString("Username", "")

        val latitude = sharedPref.getFloat("latitude", 0.0F).toDouble()
        val longitude = sharedPref.getFloat("longitude", 0.0F).toDouble()

        val locCoords = findViewById<TextView>(R.id.txtLocCoords)
        locCoords.text = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f\nLng: %2$.5f",
                latitude,
                longitude)

        return uid
    }

    private fun pickImageFromGallery() {
        //Intent to pick image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    companion object {
        // Record audio request code
        private const val RECORD_AUDIO_CODE = 1
        // Image pick code
        private const val IMAGE_PICK_CODE = 1000
        // Permission code
        private const val PERMISSION_CODE = 1001
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
