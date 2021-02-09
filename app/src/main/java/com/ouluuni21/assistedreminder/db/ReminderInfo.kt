package com.ouluuni21.assistedreminder.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminderInfo")
data class ReminderInfo(
    @PrimaryKey(autoGenerate = true) var uid: Int?,
    @ColumnInfo(name = "author") var author: String,
    @ColumnInfo(name = "date") var date: String,
    @ColumnInfo(name = "reminder") var reminder: String
)