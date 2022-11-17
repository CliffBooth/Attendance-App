package com.vysotsky.attendance.camera

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.vysotsky.attendance.API_URL
import com.vysotsky.attendance.R
import com.vysotsky.attendance.T
import com.vysotsky.attendance.databinding.FragmentStartSessionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class StartSessionFragment : Fragment() {
    private var _binding: FragmentStartSessionBinding? = null
    private val binding
        get() = _binding!!
    private lateinit var email: String
    private val viewModel: CameraViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(T, "StartSessionFragment onCreate()")
        val c = requireContext()
        val sharedPreferences = c.getSharedPreferences(getString(R.string.preference_file_key),
                AppCompatActivity.MODE_PRIVATE
            )
        email = sharedPreferences.getString(c.getString(R.string.saved_email), "error") ?: "error"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStartSessionBinding.inflate(inflater, container, false)
        binding.startButton.setOnClickListener {
            if (binding.locationCheckbox.isChecked)
                viewModel.usingGeolocation = true
            registerSession()
        }
        binding.locationCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.isUsingGeodata = isChecked
        }

        Log.d(T, "StartSessionFragment onCreateView()")
        viewModel.spinnerVisibility.observe(requireActivity()) {
            Log.d(T, "StartSessionFragment: observer!")
            binding.spinner.visibility = it
        }
        return binding.root
    }

    /**
     * Makes API call to register new session.
     */
    private fun registerSession() {
        val errorToast = Toast.makeText(
            context,
            "Internet error",
            Toast.LENGTH_SHORT
        )
        val cantCrateSessionToast = Toast.makeText(
            requireContext(),
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
                requireActivity().runOnUiThread {
                    viewModel.spinnerVisibility.value = View.VISIBLE
                }
                client.newCall(request).execute().use { res ->
                    Handler(Looper.getMainLooper()).post {
                        viewModel.spinnerVisibility.value = View.GONE
                    }
                    if (res.isSuccessful) {
                        parentFragmentManager.commit {
                            replace<CameraFragment>(R.id.fragment_container_view)
                            setReorderingAllowed(true)
                            //addToBackStack("CameraFragment")
                        }
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            cantCrateSessionToast.show()
                        }
                    }
                }
            } catch (e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    viewModel.spinnerVisibility.value = View.GONE
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