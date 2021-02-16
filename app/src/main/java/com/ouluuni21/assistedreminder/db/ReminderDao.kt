package com.ouluuni21.assistedreminder.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.ouluuni21.assistedreminder.Reminder
import java.util.*

@Dao
interface ReminderDao {
    @Transaction
    @Insert
    fun insert(reminderInfo: ReminderInfo): Long

    @Query("DELETE FROM reminderInfo WHERE uid = :id")
    fun delete(id: Int)

    @Query("SELECT r.uid, u.username AS creator, r.reminder_time, r.message FROM reminderInfo AS r, userInfo as u WHERE u.uid = r.creator_id")
    fun getReminderInfos(): List<Reminder>

    @Query("SELECT r.uid, u.username AS creator, r.reminder_time, r.message FROM reminderInfo AS r, userInfo as u WHERE u.uid = r.creator_id AND r.uid = :id")
    fun getReminderEntry(id: Int): Reminder

    @Query("UPDATE reminderInfo SET reminder_time = :date, message = :message WHERE uid = :id")
    fun updateReminderEntry(id: Int, date: Date, message: String)
}