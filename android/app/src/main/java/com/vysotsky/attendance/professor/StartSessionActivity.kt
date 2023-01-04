package com.vysotsky.attendance.professor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.viewModelScope
import com.vysotsky.attendance.API_URL
import com.vysotsky.attendance.MenuActivity
import com.vysotsky.attendance.R
import com.vysotsky.attendance.T
import com.vysotsky.attendance.databinding.ActivityStartSessionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * intermediate activity between MainActivity and ProfessorActivity
 * it goes like this: MainActivity -> StartSessionActivity -> ProfessorActivity(where all the fragments are)
*/

class StartSessionActivity: MenuActivity() {

    lateinit var binding: ActivityStartSessionBinding
    private val viewModel: StartSessionVIewModel by viewModels()

    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(T, "StartSessionActivity onCreate()")
        binding = ActivityStartSessionBinding.inflate(layoutInflater)
        val sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key),
            Context.MODE_PRIVATE)
        email = sharedPreferences.getString(getString(R.string.saved_email), "error") ?: "error"
        setContentView(binding.root)

        binding.startButton.setOnClickListener {
            registerSession()
        }

        subscribe()
    }

    private fun subscribe() {
        viewModel.spinnerVisibility.observe(this) {
            Log.d(T, "StartSessionActivity: observer!")
            binding.spinner.isVisible = it
        }
    }

    /**
     * Makes API call to register new session.
     */
    private fun registerSession() {
        val errorToast = Toast.makeText(
            this,
            "Internet error",
            Toast.LENGTH_SHORT
        )
        val cantCrateSessionToast = Toast.makeText(
            this,
            getString(R.string.can_t_create_session),
            Toast.LENGTH_SHORT
        )
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()
            val json = "{\"email\": \"$email\"}"
            val body = json.toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("$API_URL/start")
                .post(body)
                .build()

            try {
                runOnUiThread {
                    viewModel.spinnerVisibility.value = true
                }
                client.newCall(request).execute().use { res ->
                    Handler(Looper.getMainLooper()).post {
                        viewModel.spinnerVisibility.value = false
                    }
                    if (res.isSuccessful) {
                        val usingGeolocation = binding.locationCheckbox.isChecked
                        val intent = Intent(this@StartSessionActivity, ProfessorActivity::class.java).apply {
                            val bundle = Bundle().apply {
                                putBoolean(getString(R.string.geolocation_bundle_key), usingGeolocation)
                            }
                            putExtras(bundle)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            cantCrateSessionToast.show()
                        }
                    }
                }
            } catch (e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    viewModel.spinnerVisibility.value = false
                    errorToast.show()
                }
            }

        }
    }
}