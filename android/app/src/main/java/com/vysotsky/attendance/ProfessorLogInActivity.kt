package com.vysotsky.attendance

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import com.vysotsky.attendance.databinding.ActivityProfessorLoginBinding
import com.vysotsky.attendance.professor.ProfessorHomeActivity

class ProfessorLogInActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfessorLoginBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfessorLoginBinding.inflate(layoutInflater)
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
                val intent = Intent(this, ProfessorHomeActivity::class.java)
                startActivity(intent)
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }
}