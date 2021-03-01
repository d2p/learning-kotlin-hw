package com.ouluuni21.assistedreminder.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import java.util.*

@Dao
interface ReminderDao {
    @Transaction
    @Insert
    fun insert(reminderInfo: ReminderInfo): Long

    @Query("DELETE FROM reminderInfo WHERE uid = :id")
    fun delete(id: Int)

    @Query("SELECT r.uid, u.username AS creator, r.reminder_time, r.message, r.show_notif, r.image FROM reminderInfo AS r, userInfo as u WHERE u.uid = r.creator_id AND r.reminder_time < :current_time ORDER BY r.reminder_time")
    fun getReminderInfos(current_time: Date): List<Reminder>

    @Query("SELECT r.uid, u.username AS creator, r.reminder_time, r.message, r.show_notif, r.image FROM reminderInfo AS r, userInfo as u WHERE u.uid = r.creator_id ORDER BY r.reminder_time")
    fun getAllReminderInfos(): List<Reminder>

    @Query("SELECT r.uid, u.username AS creator, r.reminder_time, r.message, r.show_notif, r.image FROM reminderInfo AS r, userInfo as u WHERE u.uid = r.creator_id AND r.uid = :id")
    fun getReminderEntry(id: Int): Reminder

    @Query("UPDATE reminderInfo SET reminder_time = :date, message = :message, show_notif = :showNotif, image = :image, reminder_seen = :isSeen WHERE uid = :id")
    fun updateReminderEntry(id: Int, date: Date, message: String, showNotif: Boolean, image: ByteArray, isSeen: Boolean)
}