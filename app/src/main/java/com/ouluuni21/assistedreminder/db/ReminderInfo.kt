package com.ouluuni21.assistedreminder.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "reminderInfo")
data class ReminderInfo(
        @PrimaryKey(autoGenerate = true) var uid: Int?,
        @ColumnInfo(name = "creator_id") var creator_id: Int,
        @ColumnInfo(name = "creation_time") var creation_time: Date?,
        @ColumnInfo(name = "reminder_time") var reminder_time: Date?,
        @ColumnInfo(name = "message") var message: String,
        @ColumnInfo(name = "location_x") var location_x: String,
        @ColumnInfo(name = "location_y") var location_y: String,
        @ColumnInfo(name = "reminder_seen") var reminder_seen: Boolean,
        @ColumnInfo(typeAffinity = ColumnInfo.BLOB) var image: ByteArray?
)