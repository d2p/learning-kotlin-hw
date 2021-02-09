package com.ouluuni21.assistedreminder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.ouluuni21.assistedreminder.db.ReminderInfo

class ReminderHistoryAdaptor(context: Context, private val list:List<ReminderInfo>): BaseAdapter() {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, container: ViewGroup?): View? {
        val row = inflater.inflate(R.layout.reminder_history_item, container, false)

        val txtReminderAuthor = row.findViewById(R.id.txtReminderAuthor) as TextView
        val txtReminderDate = row.findViewById(R.id.txtReminderDate) as TextView
        val txtReminderText = row.findViewById(R.id.txtReminderText) as TextView

        txtReminderAuthor.text = list[position].author
        txtReminderDate.text = list[position].date
        txtReminderText.text = list[position].reminder

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
