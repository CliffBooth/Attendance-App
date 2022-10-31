package com.vysotsky.attendance.camera

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
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

class StartSessionFragment : Fragment() {
    private var _binding: FragmentStartSessionBinding? = null
    private val binding
        get() = _binding!!
    private lateinit var email: String

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
            registerSession()
        }
        Log.d(T, "StartSessionFragment onCreateView()")
        return binding.root
    }

    /**
     * Makes API call to register new session.
     */
    private fun registerSession() {
        lifecycleScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()
            val json = "{\"email\": \"$email\"}"
            val body = json.toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("$API_URL/start")
                .post(body)
                .build()

            client.newCall(request).execute().use { res ->
                if (res.isSuccessful) {
                    parentFragmentManager.commit {
                        add<CameraFragment>(R.id.fragment_container_view)
                        setReorderingAllowed(true)
                        addToBackStack("CameraFragment")
                    }
                } else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.can_t_create_session),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}