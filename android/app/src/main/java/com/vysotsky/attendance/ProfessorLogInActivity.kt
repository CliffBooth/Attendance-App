package com.vysotsky.attendance

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.vysotsky.attendance.databinding.ActivityProfessorLoginBinding
import com.vysotsky.attendance.professor.ProfessorHomeActivity
import com.vysotsky.attendance.util.Resource

/**
 * User presses login button and if the response is 401 (not registered) display dialog Do you want to register? yes - no.
 */
class ProfessorLogInActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfessorLoginBinding
    private lateinit var sharedPreferences: SharedPreferences

    private val viewModel: ProfessorLoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfessorLoginBinding.inflate(layoutInflater)
        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            val email = binding.emailEditText.text?.toString() ?: ""
            val password = binding.passwordEditText.text?.toString() ?: ""
            //TODO: check if matches email regex
            if (email.isBlank()) {
                Snackbar.make(it, getString(R.string.email_error_text), Snackbar.LENGTH_LONG)
                    .show()
            } else {
                if (password.isBlank()) {
                    Toast.makeText(this, "Please enter password", Toast.LENGTH_LONG).show()

                } else {
                    viewModel.enteredEmail = email
                    viewModel.enteredPassword = password
                    viewModel.login(email, password)
                }
            }
        }
        subscribe()
    }

    private fun subscribe() {
        viewModel.loginRequestStatus.observe(this) {
            when(it) {
                is Resource.Loading -> {
                    viewModel.isPBVisible.value = true
                }
                is Resource.Success<LoginResponse> -> {
                    viewModel.isPBVisible.value = false
                    when (it.data?.status) {
                        200 -> {
                            //proceed to ProfessorHome
                            startProfessorActivity(viewModel.enteredEmail, it.data.data!!.token)
                        }
                        401 -> {
                            //display signup dialog
                            Toast.makeText(this, "wrong password!", Toast.LENGTH_LONG).show()
                        }
                        409 -> {
                            //display signup dialog
                            displayDialog()
                        }
                    }
                }
                is Resource.Error -> {
                    viewModel.isPBVisible.value = false
                    Toast.makeText(
                        this,
                        it.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        viewModel.signupRequestStatus.observe(this) {
            when(it) {
                is Resource.Loading -> {
                    viewModel.isPBVisible.value = true
                }
                is Resource.Success -> {
                    viewModel.isPBVisible.value = false
                    startProfessorActivity(viewModel.enteredEmail, it.data!!.token)
                }
                is Resource.Error -> {
                    viewModel.isPBVisible.value = false
                    Toast.makeText(
                        this,
                        it.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        viewModel.isPBVisible.observe(this) {
            binding.progressBar2.isVisible = it
        }
    }

    private fun displayDialog() {
        val dialogBuilder = AlertDialog.Builder(this).apply {
            setTitle("Non existent email! Do you want to register with this email?")
            setPositiveButton(getString(R.string.yes)) { dialog, id ->
                viewModel.signup(viewModel.enteredEmail, viewModel.enteredPassword)
            }
            setNegativeButton(getString(R.string.cancel)) {_, _ -> }
        }
        dialogBuilder.create().show()
    }

    private fun startProfessorActivity(email: String, token: String) {
        with (sharedPreferences.edit()) {
            putString(getString(R.string.saved_email), email)
            putString(getString(R.string.access_token), "Bearer $token")
            apply()
        }
        val intent = Intent(this, ProfessorHomeActivity::class.java)
        startActivity(intent)
        setResult(Activity.RESULT_OK)
        finish()
    }
}