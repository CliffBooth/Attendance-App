package com.vysotsky.attendance

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import com.vysotsky.attendance.databinding.ActivityStudentBinding

//TODO: when exit, clear app preferences (delete name from the storage)
class StudentLogInActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudentBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)

        binding.logInButton.setOnClickListener {
            //save name in persistent memory
            //launch next activity
            val firstName = binding.firstNameEditText.text?.toString() ?: ""
            val secondName = binding.secondNameEditText.text?.toString() ?: ""

            if (firstName.isBlank() || secondName.isBlank()) {
                Snackbar.make(it, getString(R.string.name_should_not_be_empty), Snackbar.LENGTH_LONG)
                    .show()
            } else {
                with (sharedPreferences.edit()) {
                    putString(getString(R.string.saved_first_name), firstName)
                    putString(getString(R.string.saved_second_name), secondName)
                    apply()
                }
                val intent = Intent(this, QRCodeActivity::class.java)
                startActivity(intent)
            }

        }
    }
}