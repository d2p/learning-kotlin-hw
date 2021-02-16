package com.ouluuni21.assistedreminder.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.ouluuni21.assistedreminder.User

@Dao
interface UserDao {
    @Transaction
    @Insert
    fun insert(userInfo: UserInfo): Long

    @Query("DELETE FROM userInfo WHERE uid = :id")
    fun delete(id: Int)

    @Query("SELECT * FROM userInfo WHERE username = :username")
    fun findByUsername(username: String): User
}