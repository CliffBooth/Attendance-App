package com.vysotsky.attendance

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import com.vysotsky.attendance.databinding.ActivityProfessorBinding

class ProfessorLogInActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfessorBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfessorBinding.inflate(layoutInflater)
        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            val email = binding.emailEditText.text?.toString() ?: ""
            //TODO: check if matches email regex
            if (email.isBlank()) {
                Snackbar.make(it, getString(R.string.email_error_text), Snackbar.LENGTH_LONG)
                    .show()
            } else {
                with (sharedPreferences.edit()) {
                    putString(getString(R.string.saved_email), email)
                    apply()
                }
                val intent = Intent(this, CameraActivity::class.java)
                startActivity(intent)
            }
        }
    }
}