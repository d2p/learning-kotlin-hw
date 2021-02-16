package com.ouluuni21.assistedreminder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class ReminderHistoryAdaptor(context: Context, private val list:List<Reminder>): BaseAdapter() {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private fun Date.convertLongToDate(): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(this)
    }
    override fun getView(position: Int, convertView: View?, container: ViewGroup?): View? {
        val row = inflater.inflate(R.layout.reminder_history_item, container, false)

        val txtReminderAuthor = row.findViewById(R.id.txtReminderAuthor) as TextView
        val txtReminderDate = row.findViewById(R.id.txtReminderDate) as TextView
        val txtReminderText = row.findViewById(R.id.txtReminderText) as TextView

        txtReminderAuthor.text = list[position].creator
        txtReminderDate.text = list[position].reminder_time.convertLongToDate()
        txtReminderText.text = list[position].message

        return row
    }
    override fun getItem(position: Int): Any {
        return list[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return list.size
    }

}
