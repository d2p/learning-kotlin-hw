package com.ouluuni21.assistedreminder.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = arrayOf(ReminderInfo::class, UserInfo::class), version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun userDao(): UserDao
}
