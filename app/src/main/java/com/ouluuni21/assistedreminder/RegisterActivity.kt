package com.ouluuni21.assistedreminder

import android.content.Intent
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
import com.ouluuni21.assistedreminder.db.UserInfo

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        findViewById<TextView>(R.id.textGoback).setOnClickListener {
            this.startActivity(Intent(applicationContext, LoginActivity::class.java))
        }

        findViewById<Button>(R.id.register).setOnClickListener {
            Log.d("hw_project", "Register new user button clicked")

            val username = findViewById<EditText>(R.id.inpRegisterUsername).text.toString()
            val password = findViewById<EditText>(R.id.inpRegisterPassword).text.toString()
            val password2= findViewById<EditText>(R.id.inpRegisterPassword2).text.toString()

            if (username.isEmpty()) {
                Toast.makeText(
                    applicationContext,
                    "Empty username field!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else if (username.length < 3) {
                Toast.makeText(
                    applicationContext,
                    "Username is too short. Must be at least 3 symbols long!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else if (password.isEmpty()) {
                Toast.makeText(
                    applicationContext,
                    "Empty password field!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else if (password.length < 6) {
                Toast.makeText(
                    applicationContext,
                    "Password is too short. Must be at least 6 symbols long!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else if( password != password2) {
                Toast.makeText(
                    applicationContext,
                    "Passwords do not match!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else {
                register()
                this.startActivity(Intent(applicationContext, LoginActivity::class.java))
            }
        }
    }

    private fun register() {
        AsyncTask.execute {
            val username = findViewById<EditText>(R.id.inpRegisterUsername).text.toString()
            val password = findViewById<EditText>(R.id.inpRegisterPassword).text.toString()

            val db = Room
                .databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    getString(R.string.dbFileName)
                )
                .build()
            val user = db.userDao().findByUsername(username)
            Log.d("hw_project", "Find by $username User: ${user.username}, pass: ${user.password}")

            if( user.username == null) {
                val new = UserInfo(null,username, password)
                Log.d("hw_project", "Create new user: $username, pass: $password")
                db.userDao().insert(new)
            }
            db.close()
        }
    }
}