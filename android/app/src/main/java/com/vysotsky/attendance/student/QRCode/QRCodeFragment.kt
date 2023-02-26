package com.vysotsky.attendance.student.QRCode

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.JsonReader
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import com.vysotsky.attendance.API_URL
import com.vysotsky.attendance.R
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.databinding.FragmentQrcodeBinding
import com.vysotsky.attendance.httpClient
import com.vysotsky.attendance.polling
import com.vysotsky.attendance.student.StudentViewModel
import com.vysotsky.attendance.util.checkPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.InputStreamReader

class QRCodeFragment : Fragment() {
    private var _binding: FragmentQrcodeBinding? = null
    private val binding: FragmentQrcodeBinding
        get() = _binding!!

    private val activityViewModel: StudentViewModel by activityViewModels()
    private val fragmentViewModel: QRCodeViewModel by viewModels()

    private var dimen = 0
    private lateinit var stringToQR: String
    private lateinit var stringToSend: String
    private lateinit var pollingJob: Job

    //request that will be made with and without polling
    private lateinit var request: Request

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions(requireContext(), this, permissions) {
            Toast.makeText(
                requireContext(),
                getString(R.string.location_permissions_error),
                Toast.LENGTH_LONG
            ).show()
        }

        val manager =
            requireActivity().getSystemService(AppCompatActivity.WINDOW_SERVICE) as WindowManager
        val display = manager.defaultDisplay

        val point = Point()
        display.getSize(point)
        dimen = if (point.x < point.y) point.x * 3 / 4 else point.y * 3 / 4
        stringToQR =
            "${activityViewModel.firstName}:${activityViewModel.secondName}:${activityViewModel.deviceID}:${fragmentViewModel.locationString.value}"
        stringToSend =
            "${activityViewModel.firstName}:${activityViewModel.secondName}:${activityViewModel.deviceID}"
        val json = "{\"data\":\"$stringToSend\"}"
        val body = json.toRequestBody("application/json".toMediaTypeOrNull())
        request = Request.Builder()
            .url("$API_URL/student")
            .post(body)
            .build()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrcodeBinding.inflate(layoutInflater, container, false)

        if (polling) {
            binding.checkButton.isVisible = false
            binding.tryAgainButton.setOnClickListener {
                runPolling()
                fragmentViewModel.tryAgainButtonVisibility.value = false
            }
            if (!fragmentViewModel.isRunningPolling) {
                fragmentViewModel.isRunningPolling = true
                runPolling()
            }
            Log.d(TAG, "QRCodeFragment calling runPolling()")
        } else {
            binding.checkButton.setOnClickListener {
                Log.d(TAG, "QRCodeFragment: api call")
                sendStudent()
            }
        }

        binding.locationCheckbox.isVisible = false
        binding.qrCodeText.isVisible = false

        binding.locationCheckbox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed)
                return@setOnCheckedChangeListener
            fragmentViewModel.isCheckBoxEnabled.value = false //this should be on top
            Log.d(TAG, "QRCodeFragment: clickable disabled: ${binding.locationCheckbox.isEnabled}")
            if (!isChecked) {
                fragmentViewModel.updateLocation(requireContext(), false);
            } else {
                val permitted = checkPermissions(requireContext(), this, permissions)
                if (permitted)
                    fragmentViewModel.updateLocation(requireContext(), true)
                else {
                    Log.d(TAG, "QRCodeFragment: No locatoin permission")
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.location_permissions_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        subscribe()

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "QRCodeFragment: onStart()")
        fragmentViewModel.isPollingActive = true
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "QRCodeFragment: onStop()")
        fragmentViewModel.isPollingActive = false
    }

    //when rotate screen should continue -> hence using viewModelScope
    //when close fragment, should stop -> doesn't work
    //TODO how to stop coroutine when close the app?
    private fun runPolling() {
        pollingJob = fragmentViewModel.viewModelScope.launch(Dispatchers.IO) {
//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.whenStarted {
//                Log.d(T, "QRCodeFragment inside whenStarted{")
//                withContext(Dispatchers.IO) {
            try {
                var i = 0
                var gotResult = false
                while (!gotResult) {
                    if (!fragmentViewModel.isPollingActive) {
                        delay(1000)
                        continue
                    }
                    Log.d(TAG, "making polling request... ${++i}")
                    httpClient.newCall(request).execute().use { res ->
                        when (res.code) {
                            200 -> {
                                //token = adapter.fromJson(res.body!!.source())!!.token
                                Log.i(TAG, "QRCodeActivity: inside 200")
                                val reader =
                                    JsonReader(InputStreamReader(res.body!!.byteStream()))
                                //reader.isLenient = true
                                Log.i(TAG, "1")
                                try {
                                    reader.beginObject()
                                    reader.nextName()
                                    val token = reader.nextString()
                                    Log.d(TAG, "read token: $token")
                                    Handler(Looper.getMainLooper()).post {
                                        fragmentViewModel.token.value = token
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "exception: $e")
                                }
                                gotResult = true
                            }

                            else -> {
                                delay(1000)
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                val context = context
                Handler(Looper.getMainLooper()).post {
                    if (context != null) {
                        Toast.makeText(
                            context,
                            getString(R.string.internet_error),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    fragmentViewModel.tryAgainButtonVisibility.value = true
                }
            }
        }
//            }
//        }
    }

    private fun sendStudent() {
        fragmentViewModel.spinnerVisibility.value = true
        fragmentViewModel.viewModelScope.launch(Dispatchers.IO) {
            Log.i(TAG, "QRCodeFragment: enter coroutine")
            try {
                httpClient.newCall(request).execute().use { res ->
                    Handler(Looper.getMainLooper()).post {
                        fragmentViewModel.spinnerVisibility.value = false
                    }
                    when (res.code) {
                        200 -> {
                            //token = adapter.fromJson(res.body!!.source())!!.token
                            Log.i(TAG, "QRCodeActivity: inside 200")
                            val reader = JsonReader(InputStreamReader(res.body!!.byteStream()))
                            //reader.isLenient = true
                            Log.i(TAG, "1")
                            try {
                                reader.beginObject()
                                reader.nextName()
                                val token = reader.nextString()
                                Log.d(TAG, "read token: $token")
                                Handler(Looper.getMainLooper()).post {
                                    fragmentViewModel.token.value = token
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "exception: $e")
                            }
                        }

                        401 -> {
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    requireContext(),
                                    "can't get token",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            Log.d(TAG, "the phone hasn't been scanned, doing nothing")
                        }

                        406 -> {
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    requireContext(),
                                    "ERROR SENDING REQUEST",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            Log.d(TAG, "QRCodeActivity: error sending request")
                        }

                        else -> Unit
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "QRCodeFragment: sendStudent() error", e)
                Handler(Looper.getMainLooper()).post {
                    fragmentViewModel.spinnerVisibility.value = false
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.internet_error),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setImage(str: String) {
        val tokenBitmap = getBitMap(str)
        if (tokenBitmap == null) {
            //TODO: HANDLE ERROR
            Log.d(TAG, "QR Code image is null!")
        } else {
            binding.qrCodeImage.setImageBitmap(tokenBitmap)
            binding.qrCodeText.text = str
            //binding.root.setBackgroundColor(resources.getColor(R.color.green))
        }
    }

    private fun getBitMap(string: String): Bitmap? =
        QRGEncoder(string, null, QRGContents.Type.TEXT, dimen).bitmap


    private fun subscribe() {
        fragmentViewModel.spinnerVisibility.observe(viewLifecycleOwner) {
            binding.spinner.isVisible = it
        }

        fragmentViewModel.tryAgainButtonVisibility.observe(viewLifecycleOwner) {
            binding.tryAgainButton.isVisible = it
        }

        fragmentViewModel.token.observe(viewLifecycleOwner) {
            if (it == null) {
                setImage(stringToQR)
                binding.statusText.text = "QR CODE"
            } else {
                setImage(it)
                binding.locationCheckbox.isVisible = false
                binding.statusText.text = "TOKEN"
            }
        }

        fragmentViewModel.locationString.observe(viewLifecycleOwner) { location ->
            Log.d(TAG, "inside observer!")
            stringToQR =
                "${activityViewModel.firstName}:${activityViewModel.secondName}:${activityViewModel.deviceID}:${location}"
            setImage(stringToQR)
            Log.d(TAG, "clickable enabled: ${binding.locationCheckbox.isEnabled}")
        }

        fragmentViewModel.isCheckBoxEnabled.observe(viewLifecycleOwner) {
            if (it == null)
                return@observe
            Log.i(TAG, "inside isCheckBoxEnabled.observe(), ${it}")
            binding.locationCheckbox.isEnabled = it
            Log.i(TAG, "locationCheckBox.isEnabled = ${binding.locationCheckbox.isEnabled}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}