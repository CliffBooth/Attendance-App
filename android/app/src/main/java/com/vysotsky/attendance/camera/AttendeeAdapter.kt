package com.vysotsky.attendance.camera

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.vysotsky.attendance.R
import com.vysotsky.attendance.T

class AttendeeAdapter(context: Context, private val data: MutableList<Attendee>) :
    ArrayAdapter<Attendee>(context, R.layout.list_item, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val attendee = data[position]
        Log.d(T, "AttendeeAdapter: $position) $attendee")
        val result: View =
            convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)

        val deleteImage = result.findViewById<ImageView>(R.id.delete_image)
        deleteImage.setOnClickListener {
            //TODO: delete from the server!
            //TODO: display toast (or snack) with undo option
            data.removeAt(position)
            notifyDataSetChanged()
        }

        result.findViewById<TextView>(R.id.first_name).text = attendee.firstName
        result.findViewById<TextView>(R.id.second_name).text = attendee.secondName
        val statusText = result.findViewById<TextView>(R.id.attendee_status_text)
        //gone == 8, visible == 0
        Log.e(T, "statusText.visibility = ${statusText.visibility}")


        when (attendee.status) {
            Status.OK -> {
                result.background = AppCompatResources.getDrawable(context, R.drawable.item_background_ok)
                statusText.visibility = View.GONE
            }
            Status.NO_DATA -> {
                result.background = AppCompatResources.getDrawable(context, R.drawable.item_background_error)
                statusText.text = context.getString(R.string.no_location_data)
            }
            Status.OUT_OF_RANGE -> {
                result.background = AppCompatResources.getDrawable(context, R.drawable.item_background_error)
                statusText.text = context.getString(R.string.location_out_of_range)
            }
        }

        return result
    }
}