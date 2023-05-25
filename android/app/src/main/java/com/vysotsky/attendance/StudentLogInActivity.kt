package com.vysotsky.attendance

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.vysotsky.attendance.databinding.ActivityStudentLoginBinding
import com.vysotsky.attendance.student.StudentActivity
import com.vysotsky.attendance.util.Resource

/**
 * first, just button log in, when pressed, sends request student_login.
 * if the result is ok (200), save first_name and second name in preferences and go to student screen.
 * if the result is not ok (401) display the text inputs and change to sign up button.
 *
 * if debug, allow to input the id and do the same thing.
 *
    (if not debug!!!)
 * WE DON'T NEED TO PRESS "LOGIN" BUTTON, login request should be sent on startup
 */
class StudentLogInActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudentLoginBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val viewModel: StudentLoginViewModel by viewModels()

    private lateinit var id: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)

        if (!debug) {
            binding.androidIdText.isVisible = false
            binding.randomLocatoinCheckbox.isVisible = false
            binding.logInButton.isVisible = false

            id = getId()
            viewModel.login(id)
        }

        binding.randomLocatoinCheckbox.isChecked = true

        binding.logInButton.setOnClickListener {
            id = getId()
            viewModel.login(id)
        }

        binding.signupButton.setOnClickListener {
            id = getId()

            val firstName = binding.firstNameEditText.text?.toString() ?: ""
            val secondName = binding.secondNameEditText.text?.toString() ?: ""

            if (firstName.isBlank() || secondName.isBlank()) {
                Snackbar.make(it, getString(R.string.name_should_not_be_empty), Snackbar.LENGTH_LONG)
                    .show()
            } else {
                viewModel.signup(id, firstName, secondName)
            }
        }

        binding.randomLocatoinCheckbox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed)
                return@setOnCheckedChangeListener
            randomLocation = isChecked
            Log.d(TAG, "StudentLogIn: randomLocation = $randomLocation")
        }

        subscribe()
    }

    private fun subscribe() {
        viewModel.loginRequestStatus.observe(this) {
            when(it) {
                is Resource.Loading -> {
                    viewModel.isPBVisible.value = true
                }
                is Resource.Success -> {
                    viewModel.isPBVisible.value = false

                    when (it.data!!.status) {
                        200 -> {
                            //proceed to StudentActivity
                            startStudentActivity(it.data.firstName!!, it.data.secondName!!)
                        }
                        401 -> {
                            //display signup button
                            viewModel.isLoginTextEditVisible.value = true
                        }
                    }
                }
                is Resource.Error -> {
                    viewModel.isPBVisible.value = false
                    Toast.makeText(
                        this,
//                        it.message,
                        getString(R.string.network_error),
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
                    when (it.data) {
                        200 -> {
                            startStudentActivity(viewModel.signupFirstName, viewModel.signupSecondName)
                        }
                        else -> {
                            Log.d(TAG, "StudentLoginActivity signup status result = ${it.data}")
                            Toast.makeText(
                                this,
                                "error",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                is Resource.Error -> {
                    viewModel.isPBVisible.value = false
                    Toast.makeText(
                        this,
//                        it.message,
                        getString(R.string.network_error),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        viewModel.isLoginTextEditVisible.observe(this) {
            binding.firstNameEditText.isVisible = it
            binding.secondNameEditText.isVisible = it
            binding.signupButton.isVisible = it
            binding.textView2.isVisible = it
        }

        viewModel.isPBVisible.observe(this) {
            binding.pb.isVisible = it
        }
    }

    private fun startStudentActivity(firstName: String, secondName: String) {
        with (sharedPreferences.edit()) {
            putString(getString(R.string.saved_first_name), firstName)
            putString(getString(R.string.saved_second_name), secondName)
            apply()
        }
        val intent = Intent(this, StudentActivity::class.java)
        intent.putExtra("id", id)
        startActivity(intent)
        Log.d(TAG, "StudentLogIn before setResult")
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun getId(): String {
        return if (debug) {
            binding.androidIdText.text?.toString() ?: ""
        } else {
            Settings.Secure.getString(
                applicationContext.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        }
    }
}