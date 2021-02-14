package com.ouluuni21.assistedreminder

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.ouluuni21.assistedreminder.db.AppDatabase

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        findViewById<Button>(R.id.btnRegisterL).setOnClickListener {
            Log.d("hw_project", "Register button clicked")
            this.startActivity(Intent(applicationContext, RegisterActivity::class.java))
        }

        findViewById<Button>(R.id.login).setOnClickListener {
            Log.d("hw_project", "Login button clicked")

            if( checkLoginStatus() == 1) {
                this.startActivity(Intent(applicationContext, MainActivity::class.java))
            }
            else {
                this.startActivity(Intent(applicationContext, LoginActivity::class.java))
            }
//            else {
//                Toast.makeText(
//                    applicationContext,
//                    "Invalid username or password combination.",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
        }

        checkLoginStatus()
    }

    override fun onResume() {
        super.onResume()
        checkLoginStatus()
    }

    private fun checkLoginStatus(): Int {
        val sharedPref = applicationContext.getSharedPreferences(
            getString(R.string.preference_file), Context.MODE_PRIVATE
        )
        var loginStatus = sharedPref.getInt("LoginStatus", 0)
/*
        AsyncTask.execute {
            val user = UserInfo(null,"123456", "qwerty")
            val db = Room
                .databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    getString(R.string.dbFileName)
                )
                .build()
            db.userDao().insert(user)
            db.close()
        }

        sharedPref.edit().putString("Username", "tester").apply()
        sharedPref.edit().putString("Password", "qwerty").apply()
*/
        if (loginStatus != 1) {
            AsyncTask.execute {
                val username = findViewById<EditText>(R.id.username).text.toString()
                val password = findViewById<EditText>(R.id.password).text.toString()
//                val defUsername = sharedPref.getString("Username", "")
//                val defPassword = sharedPref.getString("Password", "")

                val db = Room
                    .databaseBuilder(
                        applicationContext,
                        AppDatabase::class.java,
                        getString(R.string.dbFileName)
                    )
                    //.fallbackToDestructiveMigration()
                    .build()
                val user = db.userDao().findByUsername(username)
                db.close()

                Log.d("hw_project", "Find by ${username} User: ${user.username}, pass: ${user.password}")
                if( user.username == username && user.password == password) {
                    sharedPref.edit().putInt("LoginStatus", 1).apply()
                    sharedPref.edit().putString("Username", username).apply()
                    sharedPref.edit().putInt("Uid", user.uid).apply()
                    this.startActivity(Intent(applicationContext, MainActivity::class.java))
                    loginStatus = 1
                }
            }
        }
        return loginStatus
    }
}