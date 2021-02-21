package com.ouluuni21.assistedreminder.db

import androidx.room.Entity
import java.util.*

@Entity
data class Reminder(
        var uid: Int,
        var creator: String,
        var reminder_time: Date,
        var message: String,
        var image: ByteArray
)