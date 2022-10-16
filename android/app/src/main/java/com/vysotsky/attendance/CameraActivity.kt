package com.vysotsky.attendance

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.vysotsky.attendance.databinding.ActivityCameraBinding

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        val email = sharedPreferences.getString(getString(R.string.saved_email), "ERROR!");
        Log.d(T, "Camera activity: email: $email")

    }
}