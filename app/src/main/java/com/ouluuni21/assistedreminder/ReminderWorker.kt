package com.ouluuni21.assistedreminder

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(appContext: Context, workerParameters: WorkerParameters) :
    Worker(appContext,workerParameters) {

    override fun doWork(): Result {
        val title = inputData.getString("title") // this comes from the reminder parameters
        val message = inputData.getString("message") // this comes from the reminder parameters
        MainActivity.showNotification(applicationContext, title!!, message!!)
        return Result.success()
    }
}