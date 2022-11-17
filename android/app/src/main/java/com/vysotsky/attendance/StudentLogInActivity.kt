package com.vysotsky.attendance

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.vysotsky.attendance.QRCode.QRCodeActivity
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

        if (!debug) {
            binding.androidIdText.visibility = View.GONE
            binding.randomLocatoinCheckbox.visibility = View.GONE
        }

        binding.randomLocatoinCheckbox.isChecked = true

        binding.logInButton.setOnClickListener {
            //save name in persistent memory
            //launch next activity
            val firstName = binding.firstNameEditText.text?.toString() ?: ""
            val secondName = binding.secondNameEditText.text?.toString() ?: ""
            val id = binding.androidIdText.text?.toString() ?: ""

            if (firstName.isBlank() || secondName.isBlank()) {
                //TODO: close keyboard for snackbar to be seen!
                //or display snackbar over the keyboard!
                Snackbar.make(it, getString(R.string.name_should_not_be_empty), Snackbar.LENGTH_LONG)
                    .show()
            } else {
                with (sharedPreferences.edit()) {
                    putString(getString(R.string.saved_first_name), firstName)
                    putString(getString(R.string.saved_second_name), secondName)
                    apply()
                }
                val intent = Intent(this, QRCodeActivity::class.java)
                if (debug) {
                    intent.putExtra("id", id)
                }
                startActivity(intent)
                Log.d(T, "StudentLogIn before setResult")
                setResult(Activity.RESULT_OK)
                finish()
            }
        }

        binding.randomLocatoinCheckbox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed)
                return@setOnCheckedChangeListener
            randomLocation = isChecked
            Log.d(T, "StudentLogIn: randomLocation = $randomLocation")
        }
    }
}