package com.ouluuni21.assistedreminder

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.ouluuni21.assistedreminder.db.AppDatabase
import com.ouluuni21.assistedreminder.db.ReminderInfo

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        showUser()
        refreshListView()

        listView = findViewById<ListView>(R.id.reminderListView)
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, id ->
            // Retrieve selected Item
            val selectedReminder = listView.adapter.getItem(position) as ReminderInfo
            val message =
                "Do you want to delete ${selectedReminder.uid} reminder, on ${selectedReminder.date} from ${selectedReminder.author} ?"

            // Show AlertDialog to delete the reminder
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Delete reminder?")
                .setMessage(message)
                .setPositiveButton("Delete") { _, _ ->
                    // Update UI

                    // Delete from database
                    AsyncTask.execute {
                        val db = Room
                            .databaseBuilder(
                                applicationContext,
                                AppDatabase::class.java,
                                getString(R.string.dbFileName)
                            )
                            .build()
                        db.reminderDao().delete(selectedReminder.uid!!)
                        db.close()
                    }
                    // Cancel pending time based reminder
                    // cancelReminder(applicationContext, selectedReminder.uid!!)

                    // Refresh payments list
                    refreshListView()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    // Do nothing
                    dialog.dismiss()
                }
                .show()
        }

        findViewById<Button>(R.id.logout).setOnClickListener {
            Log.d("hw_project", "Logout button clicked")

            logout()

            if( logout() == 1) {
                this.startActivity(Intent(applicationContext, LoginActivity::class.java))
            }
        }

        findViewById<Button>(R.id.new_reminder).setOnClickListener {
            this.startActivity(Intent(applicationContext, ReminderActivity::class.java))
        }
    }

    private fun showUser() {
        val sharedPref = applicationContext.getSharedPreferences(
            getString(R.string.preference_file), MODE_PRIVATE
        )
        val currentUser = sharedPref.getString("Username", "")
        val user = findViewById<TextView>(R.id.currentUser)
        user.text = currentUser
    }

    override fun onResume() {
        super.onResume()
        showUser()
        refreshListView()
    }

    private fun refreshListView() {
        val refreshTask = LoadReminderInfoEntries()
        refreshTask.execute()
    }

    private fun logout(): Int {
        val sharedPref = applicationContext.getSharedPreferences(
            getString(R.string.preference_file), MODE_PRIVATE
        )
        sharedPref.edit().putInt("LoginStatus", 0).apply()
        return 1
    }

    inner class LoadReminderInfoEntries : AsyncTask<String?, String?, List<ReminderInfo>>() {
        override fun doInBackground(vararg params: String?): List<ReminderInfo> {
            val db = Room
                .databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    getString(R.string.dbFileName)
                )
                //.fallbackToDestructiveMigration()
                .build()
            val reminderInfos = db.reminderDao().getReminderInfos()
            db.close()
            return reminderInfos
        }

        override fun onPostExecute(reminderInfos: List<ReminderInfo>?) {
            super.onPostExecute(reminderInfos)
            if (reminderInfos != null) {
                if (reminderInfos.isNotEmpty()) {
                    val adaptor = ReminderHistoryAdaptor(applicationContext, reminderInfos)
                    listView.adapter = adaptor
                } else {
                    //listView.adapter = null
                    val toast =
                        Toast.makeText(applicationContext, "No items present now. Populating fake view.", Toast.LENGTH_SHORT)
                    toast.setGravity(Gravity.BOTTOM, 0, 400)
                    toast.show()
                    val dummyInfos = dummyList()
                    val adaptor = ReminderHistoryAdaptor(applicationContext, dummyInfos)
                    listView.adapter = adaptor
                }
            }
        }

        private fun dummyList(): List<ReminderInfo> {
            val reminderInfos = mutableListOf<ReminderInfo>()
            for (i in 0 until 10) {
                reminderInfos += ReminderInfo(i, "Author " + i, "01/01/2021", "Dummy reminder text entry")
            }
            return reminderInfos
        }
    }
}