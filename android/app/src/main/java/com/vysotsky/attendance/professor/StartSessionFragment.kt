package com.vysotsky.attendance.professor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import com.vysotsky.attendance.API_URL
import com.vysotsky.attendance.R
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.databinding.FragmentStartSessionBinding
import com.vysotsky.attendance.httpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * intermediate activity between MainActivity and ProfessorActivity
 * it goes like this: MainActivity -> StartSessionActivity -> ProfessorActivity(where all the fragments are)
*/

//make a drop down with all the used classes names. To get all the used classes names,
//first make a request to the database.
//create a "class" model-class (data, to later send to the backend or save to the local db)

class StartSessionFragment: Fragment() {
    private var _binding: FragmentStartSessionBinding? = null
    private val binding
        get() = _binding!!


    private val viewModel: StartSessionVIewModel by viewModels()

    private lateinit var email: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStartSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPreferences = requireContext().getSharedPreferences(getString(R.string.preference_file_key),
            Context.MODE_PRIVATE)
        email = sharedPreferences.getString(getString(R.string.saved_email), "error") ?: "error"

        binding.startButton.setOnClickListener {
            binding.startButton.isEnabled = false
            registerSession()
        }

        subscribe()
    }

    private fun subscribe() {
        viewModel.spinnerVisibility.observe(viewLifecycleOwner) {
            Log.d(TAG, "StartSessionActivity: observer!")
            binding.spinner.isVisible = it
        }
    }

    /**
     * Makes API call to register new session.
     */
    private fun registerSession() {
        val errorToast = Toast.makeText(
            requireContext(),
            "Internet error",
            Toast.LENGTH_SHORT
        )
        val cantCrateSessionToast = Toast.makeText(
            requireContext(),
            getString(R.string.can_t_create_session),
            Toast.LENGTH_SHORT
        )
        viewModel.viewModelScope.launch(Dispatchers.IO) {
//            val client = OkHttpClient()
            val json = "{\"email\": \"$email\"}"
            val body = json.toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("$API_URL/start")
                .post(body)
                .build()

            try {
                viewModel.spinnerVisibility.postValue(true)
                httpClient.newCall(request).execute().use { res ->
                    Handler(Looper.getMainLooper()).post {
                        viewModel.spinnerVisibility.value = false
                    }
                    if (res.isSuccessful) {
                        val usingGeolocation = binding.locationCheckbox.isChecked
                        val intent = Intent(requireContext(), SessionActivity::class.java).apply {
                            val bundle = Bundle().apply {
                                putBoolean(getString(R.string.geolocation_bundle_key), usingGeolocation)
                            }
                            putExtras(bundle)
                        }
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            binding.startButton.isEnabled = true
                            cantCrateSessionToast.show()
                        }
                    }
                }
            } catch (e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    binding.startButton.isEnabled = true
                    viewModel.spinnerVisibility.value = false
                    errorToast.show()
                }
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}