package com.vysotsky.attendance

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vysotsky.attendance.databinding.ActivityMainBinding

//TODO: if there is a persistent state, go to that activity right away
//check how to bypass activity

const val T = "myTag"

class MainActivity: AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //check if already logged in
        val sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        if (sharedPreferences.contains(getString(R.string.saved_first_name)) &&
                sharedPreferences.contains(getString(R.string.saved_second_name))) {
            val intent = Intent(this, QRCodeActivity::class.java)
            startActivity(intent)
            finish()
        }

        if (sharedPreferences.contains(getString(R.string.saved_email))) {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.studentButton.setOnClickListener {
            val intent = Intent(this, StudentLogInActivity::class.java)
            startActivity(intent)
        }

        binding.professorButton.setOnClickListener {
            val intent = Intent(this, ProfessorLogInActivity::class.java)
            startActivity(intent)
        }
    }
}