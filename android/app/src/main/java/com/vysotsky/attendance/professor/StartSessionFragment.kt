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
import com.vysotsky.attendance.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * it goes like this: MainActivity -> ProfessorHomeActivity -> ProfessorActivity(where all the fragments are)
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
            if (binding.subjectName.text?.isNotEmpty() == true) {
                viewModel.startButtonEnabled.value = false
                registerSession()
            } else {
                Toast.makeText(requireContext(), "Please, fill in the subject name!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.goToExistingSession.setOnClickListener {
            //IS NAME OF THE RUNNING SESSION SAVED ANYWHERE????
            val intent = Intent(requireContext(), SessionActivity::class.java).apply {
                val bundle = Bundle().apply {
                    putBoolean(SessionActivity.GEOLOCATION_KEY, false)
                    putString(SessionActivity.SUBJECT_NAME_KEY, viewModel.subjectName)
                }
                putExtras(bundle)
            }
            startActivity(intent)
            requireActivity().finish()
        }

        subscribe()
        viewModel.checkSession(email)
    }

    private fun subscribe() {
        viewModel.spinnerVisibility.observe(viewLifecycleOwner) {
            Log.d(TAG, "StartSessionActivity: observer!")
            binding.spinner.isVisible = it
        }

        viewModel.startButtonEnabled.observe(viewLifecycleOwner) {
            binding.startButton.isEnabled = it
        }

        viewModel.startButtonDisplayed.observe(viewLifecycleOwner) {
            binding.startButton.isVisible = it
            binding.goToExistingSession.isVisible = !it
            binding.subjectName.isVisible = it
            binding.locationCheckbox.isVisible = it
        }

        viewModel.requestStatus.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Loading -> {
                    viewModel.spinnerVisibility.value = true
                    viewModel.startButtonEnabled.value = false
                }

                is Resource.Success -> {
                    viewModel.spinnerVisibility.value = false
                    viewModel.startButtonEnabled.value = true

                    when (it.data) {
                        200 -> {
                            viewModel.startButtonDisplayed.value = false
                        }

                        401 -> {
                            viewModel.startButtonDisplayed.value = true
                        }

                        else -> {
                            Toast.makeText(
                                requireContext(),
                                "something went wrong...",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                is Resource.Error -> {
                    viewModel.spinnerVisibility.value = false
                    viewModel.startButtonEnabled.value = true

                    Toast.makeText(
                        requireContext(),
                        it.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
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
            val json = "{\"email\": \"$email\", \"subjectName\": \"${binding.subjectName.text.toString()}\"}"
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
                                putBoolean(SessionActivity.GEOLOCATION_KEY, usingGeolocation)
                                putString(SessionActivity.SUBJECT_NAME_KEY, binding.subjectName.text.toString())
                            }
                            putExtras(bundle)
                        }
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            viewModel.startButtonEnabled.value = true
                            cantCrateSessionToast.show()
                        }
                    }
                }
            } catch (e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    viewModel.startButtonEnabled.value = true
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