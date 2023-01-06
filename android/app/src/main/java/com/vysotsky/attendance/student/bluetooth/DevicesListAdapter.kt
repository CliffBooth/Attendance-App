package com.vysotsky.attendance.student.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class DevicesListAdapter(
    context: Context,
    private val data: MutableList<BluetoothDevice>,
) : ArrayAdapter<BluetoothDevice>(context, android.R.layout.simple_list_item_1, data) {

    @SuppressLint("MissingPermission")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val device = data[position]
        val result: View = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        val textView = result.findViewById<TextView>(android.R.id.text1)
        textView.text = "${device.name}\n${device.address}"
        return result
    }
}