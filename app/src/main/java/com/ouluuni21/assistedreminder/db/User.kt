package com.ouluuni21.assistedreminder.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey var uid: Int,
    var username: String?,
    var password: String?
)