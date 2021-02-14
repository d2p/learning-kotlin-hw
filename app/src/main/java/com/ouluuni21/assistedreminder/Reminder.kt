package com.ouluuni21.assistedreminder

import androidx.room.Entity

@Entity
data class Reminder (
    var uid: Int,
    var creator: String,
    var reminder_time: String,
    var message: String
)