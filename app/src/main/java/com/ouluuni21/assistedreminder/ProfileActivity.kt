package com.ouluuni21.assistedreminder

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        showUser()

        findViewById<Button>(R.id.logout).setOnClickListener {
            Log.d("hw_project", "Logout button clicked")
            if( logout() == 1) {
                this.startActivity(Intent(applicationContext, LoginActivity::class.java))
            }
        }
    }

    private fun showUser() {
        val sharedPref = applicationContext.getSharedPreferences(
            getString(R.string.preference_file), MODE_PRIVATE
        )
        val currentUser = sharedPref.getString("Username", "")
        val user = findViewById<TextView>(R.id.profileName)
        user.text = currentUser
    }

    private fun logout(): Int {
        val sharedPref = applicationContext.getSharedPreferences(
            getString(R.string.preference_file), MODE_PRIVATE
        )
        sharedPref.edit().putInt("LoginStatus", 0).apply()
        return 1
    }
}