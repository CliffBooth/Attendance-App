package com.vysotsky.attendance.professor.attendeeList

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import com.vysotsky.attendance.R
import com.vysotsky.attendance.TAG

class AttendeeAdapter(
    context: Context,
    private val data: MutableList<Attendee>,
    private val onDelete : (student: Attendee) -> Unit
) :
    ArrayAdapter<Attendee>(context, R.layout.list_item, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val attendee = data[position]
        Log.d(TAG, "AttendeeAdapter: $position) $attendee")
        val result: View =
            convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)

        val deleteImage = result.findViewById<ImageView>(R.id.delete_image)
        deleteImage.setOnClickListener {
            //TODO: launch dialog
            val dialogBuilder = AlertDialog.Builder(context).apply {
                setTitle(context.getString(R.string.are_you_sure))
                setPositiveButton(context.getString(R.string.yes)) {dialog, id ->
                    val removed = data.removeAt(position)
                    notifyDataSetChanged()
                    onDelete(removed)
                }
                setNegativeButton(context.getString(R.string.cancel)) {_, _ -> }
            }
            dialogBuilder.create().show()
        }

        if (attendee.id != null) {
            deleteImage.isVisible = false
        } else {
            deleteImage.isVisible = true
        }

        result.findViewById<TextView>(R.id.first_name).text = attendee.firstName
        result.findViewById<TextView>(R.id.second_name).text = attendee.secondName
        val statusText = result.findViewById<TextView>(R.id.attendee_status_text)

        result.findViewById<TextView>(R.id.counter_text).text = (position + 1).toString()

        when (attendee.status) {
            Status.OK -> {
                result.background = AppCompatResources.getDrawable(context, R.drawable.item_background_ok)
                statusText.visibility = View.GONE
            }
            Status.NO_DATA -> {
                result.background = AppCompatResources.getDrawable(context, R.drawable.item_background_error)
                statusText.text = context.getString(R.string.no_location_data)
                statusText.visibility = View.VISIBLE
            }
            Status.OUT_OF_RANGE -> {
                result.background = AppCompatResources.getDrawable(context, R.drawable.item_background_error)
                statusText.text = context.getString(R.string.location_out_of_range)
                statusText.visibility = View.VISIBLE
            }
        }

        //gone == 8, visible == 0
//        Log.d(T, "statusText.visibility = ${statusText.visibility}")

        return result
    }
}