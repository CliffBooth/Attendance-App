package com.vysotsky.attendance

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.vysotsky.attendance.api.RetrofitInstance
import com.vysotsky.attendance.databinding.ActivityChangeIpactivityBinding

class ChangeIPActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChangeIpactivityBinding
    private val addresses = arrayOf("custom", "192.168.119.144")
    private var selectedItem = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChangeIpactivityBinding.inflate(layoutInflater)
        binding.ipButton.setOnClickListener {
            if (selectedItem != 0) {
                val ip = addresses[selectedItem]
                Log.d(TAG, "ChangeIPActivity: ip = $ip")
                API_URL = "http://$ip:7000"
            } else {
                val text = binding.ipTextEdit.text.toString()
                if (text.contains(":")) {
                    API_URL = "http://$text"
                } else {
                    API_URL = "http://$text:7000"
                }
                Toast.makeText(this, "api set", Toast.LENGTH_SHORT).show()
            }
            RetrofitInstance.changeURL(API_URL)
            Log.d(TAG, "button clickListener, retrofit url changed")
            finish()
        }

        binding.spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, addresses)
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d(TAG, "ChangeIPActivity: position = $position")
                selectedItem = position
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        setContentView(binding.root)
    }


}