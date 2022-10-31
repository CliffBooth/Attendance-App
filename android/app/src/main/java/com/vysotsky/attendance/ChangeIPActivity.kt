package com.vysotsky.attendance

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.vysotsky.attendance.databinding.ActivityChangeIpactivityBinding

class ChangeIPActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChangeIpactivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChangeIpactivityBinding.inflate(layoutInflater)
        binding.ipButton.setOnClickListener {
            val text = binding.ipTextEdit.text.toString()
            if (text.contains(":")) {
                API_URL = "http://$text"
            } else {
                API_URL = "http://$text:7000"
            }
            Toast.makeText(this, "api set", Toast.LENGTH_SHORT).show()
            finish()
        }

        setContentView(binding.root)
    }
}