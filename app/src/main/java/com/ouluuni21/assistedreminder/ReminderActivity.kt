package com.ouluuni21.assistedreminder

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder_acitivity)

        refreshView()
    }

    override fun onResume() {
        super.onResume()
        refreshView()
    }

    private fun refreshView() {
        val sharedPref = applicationContext.getSharedPreferences(
            getString(R.string.preference_file), Context.MODE_PRIVATE
        )
        val username = findViewById<TextView>(R.id.reminderAuthor)
        username.text = sharedPref.getString("Username", "")
    }
}