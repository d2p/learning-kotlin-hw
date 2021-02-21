package com.ouluuni21.assistedreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        // Retrieve data from intent
        // val uid = intent?.getIntExtra("uid", 0)
        val text = intent?.getStringExtra("message")

        MainActivity.showNotification(context!!,text!!)
    }
}