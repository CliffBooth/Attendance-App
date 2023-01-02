package com.vysotsky.attendance.professor.camera

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationRequest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.JsonReader
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.squareup.moshi.JsonClass
import com.vysotsky.attendance.API_URL
import com.vysotsky.attendance.R
import com.vysotsky.attendance.T
import com.vysotsky.attendance.databinding.FragmentCameraBinding
import com.vysotsky.attendance.professor.Attendee
import com.vysotsky.attendance.professor.GeoLocation
import com.vysotsky.attendance.professor.ProfessorViewModel
import com.vysotsky.attendance.professor.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.InputStreamReader

//TODO: move to constants
const val allowedDistance = 100 //100 meters

class CameraFragment : Fragment() {
    private var _binding: FragmentCameraBinding? = null
    private val binding
        get() = _binding!!
    private lateinit var email: String
    private val attendees = mutableListOf<Attendee>()
    private val englishQRRegex = Regex("^[A-Za-z]+:[A-Za-z]+:\\w+:((-?[\\w\\.]+---?[\\w\\.]+)|null)\$")
    private lateinit var drawerLayout: DrawerLayout
    private val viewModel: ProfessorViewModel by activityViewModels()
    private var locatonOfCurrentAttendee: GeoLocation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(T, "CameraFragment onCreate()")
        val c = requireContext()
        val sharedPreferences = c.getSharedPreferences(
            getString(R.string.preference_file_key),
            AppCompatActivity.MODE_PRIVATE
        )
        email = sharedPreferences.getString(c.getString(R.string.saved_email), "error") ?: "error"
        Log.d(
            T,
            "CameraFragment: viewmodel.lastSent = ${viewModel.lastSent} viewmodel.nameSent = ${viewModel.nameSent}"
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        checkPermission()
        if (viewModel.isUsingGeodata && viewModel.ownLocation == null) {
            val permitted = checkLocationPermission()
            if (permitted) {
                Log.d(T, "CameraFragment: permission granted")
                updateLocation()
            }
        }
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
//        binding.listButton.setOnClickListener {
//            parentFragmentManager.commit {
//                val bundle = Bundle()
//                bundle.putStringArray("list", attendees.toTypedArray())
//                replace<AttendeesListFragment>(R.id.fragment_container_view, null, bundle)
//                setReorderingAllowed(true)
//                addToBackStack("AttendeesList Fragment")
//            }
//        }
        viewModel.status.observe(viewLifecycleOwner) {
            binding.debugText.text = it
        }
        viewModel.intnetErrorMessageVisibility.observe(viewLifecycleOwner) {
            binding.internetErrorMessage.visibility = it
        }
        return binding.root
    }

    private fun updateLocation() {
        try {
            val priority = if (android.os.Build.VERSION.SDK_INT >= 31)
                LocationRequest.QUALITY_HIGH_ACCURACY
            else
                100

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
            fusedLocationClient.getCurrentLocation(
                priority,
                object : CancellationToken() {
                    override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                        CancellationTokenSource().token

                    override fun isCancellationRequested() = false
                }).addOnSuccessListener { location ->
                //get latitude and longitude and then use Location.distance() - on Professor
                if (location == null) {
                    //gps was turned off
                    Log.d(T, "QRCodeViewModel displaying toast...")
                    Toast.makeText(context, "can't retrieve location data!", Toast.LENGTH_LONG)
                        .show()
                } else {
                    val lon = location.longitude
                    val lat = location.latitude
                    Log.d(T, "inside updateLocation: longitude = $lon, latitude = $lat")
                    viewModel.ownLocation = GeoLocation(lon, lat)
                }
            }
        } catch (e: SecurityException) {
            Log.d(T, e.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(T, "CameraFragment onDestroy()")
    }

    private fun checkLocationPermission(): Boolean {
        val check =
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        if (check == PackageManager.PERMISSION_GRANTED) {
            return true
        } else {
            var res = false
            Log.d(T, "CameraFragment: asking for location permission")
            val reqPermission =
                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    when {
                        it.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                            Log.d(T, "location permissions granted")
                            res = true
                        }

                        it.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                            Log.d(T, "Only coarse location permission!")
                            //TODO make different notification
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.location_permissions_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            Log.d(T, "No location permission!")
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.location_permissions_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            reqPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
            return res
        }
    }

    private fun checkPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> startCamera()

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showRationaleDialog(
                    "Camera Access",
                    "Allow camera access in order to use camera",
                    Manifest.permission.CAMERA,
                    REQUEST_CODE_PERMISSIONS
                )
            }

            else -> {
                Log.d(T, "asking for permission")
                val reqPermission =
                    registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                        if (it) {
                            startCamera()
                        } else {
                            Log.d(T, "No permission!")
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.camera_permissions_error),
                                Toast.LENGTH_SHORT
                            ).show()
                            //TODO: don't finish but show activity with error message, or previous activity
                            parentFragmentManager.commit {
                                remove(this@CameraFragment)
                            }
                        }
                    }
                reqPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10 //??
        private val REQUIRED_PERMISSIONS = listOf(Manifest.permission.CAMERA).toTypedArray()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                bindCameraPreview(cameraProvider)
            } catch (e: Exception) {
                //TODO: notify user about error
                Log.e(T, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraPreview(cameraProvider: ProcessCameraProvider) {
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

        //may be there is a bug
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(requireContext()), //TODO: should this be the main executor?
            QRCodeImageAnalyzer(listener)
        )
        cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, imageAnalysis, preview)
    }

    private val listener = object : QRCodeImageAnalyzer.QRCodeListener {
        override fun onQRCodeFound(string: String) {
            // looking at the same qr code we sent previously
            //if error with previous attempt happened, attempt again
            if (string == viewModel.lastSent && viewModel.intnetErrorMessageVisibility.value == View.GONE) {
                return
            }

            if (!viewModel.nameSent) {
                // trying to send anything camera sees, sending only if it matches expected pattern
                if (string.matches(englishQRRegex)) {
                    if (viewModel.isUsingGeodata) {
                        val subStr = string.substringAfterLast(":")
                        if (subStr != "null") {
                            val lonLat = subStr.split("--")
                            val lon = lonLat[0].toDouble()
                            val lat = lonLat[1].toDouble()
                            locatonOfCurrentAttendee = GeoLocation(lon, lat)
                        }
                    }
                    runBlocking {
                        Log.e(T, "going to send : ${string.substringBeforeLast(":")}")
                        sendScan(string.substringBeforeLast(":"))
                    }
                } else {
                    val currentThread = Thread.currentThread()
                    viewModel.status.value = "QR Code doesn't look like student's name"
                }
            } else {
                // looking at the token
                sendVerify(string)
            }

            //  Do not account string if an error has occurred
            if (viewModel.intnetErrorMessageVisibility.value == View.GONE) {
                viewModel.lastSent = string
            }
        }

        override fun onQRCodeNotFound() {}
    }

    private fun sendScan(data: String) {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()
            val json = "{\"email\":\"$email\", \"data\":\"$data\"}"
            val body = json.toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("$API_URL/scan")
                .post(body)
                .build()
            tryCatchNetworkError {
                client.newCall(request).execute().use { res ->
                    when (res.code) {
                        201 -> {
                            requireActivity().runOnUiThread {
                                viewModel.nameSent = true
                                viewModel.status.value = "student name sent"
                            }
                        }

                        202 -> {
                            requireActivity().runOnUiThread {
                                viewModel.status.value = "This phone has already been scanned!"
                            }
                        }

                        406 -> {
                            requireActivity().runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    "wrong QR Code format or wrong request body",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        401 -> {
                            requireActivity().runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    "The session has not been found on the server",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        else -> {}
                    }
                }
            }
            Log.i(T, "call is made in coroutine!")
        }
        Log.i(T, "after calling coroutine in mainThread")
    }

    @JsonClass(generateAdapter = true)
    data class Student(val firstName: String, val secondName: String)

    private fun sendVerify(data: String) {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()
//            val moshi = Moshi.Builder().build()
//            val adapter = moshi.adapter(Student::class.java)


            val json = "{\"email\":\"$email\", \"data\":\"$data\"}"
            val body = json.toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("$API_URL/verify")
                .post(body)
                .build()
            tryCatchNetworkError {
                client.newCall(request).execute().use { res ->
                    when (res.code) {
                        200 -> {
                            val reader = JsonReader(InputStreamReader(res.body!!.byteStream()))
                            reader.beginObject()
                            reader.nextName()
                            val firstName = reader.nextString()
                            reader.nextName()
                            val secondName = reader.nextString()

                            viewModel.attendeesList += getAttendee(firstName, secondName, locatonOfCurrentAttendee)
                            viewModel.nameSent = false
                            requireActivity().runOnUiThread {
                                viewModel.status.value = "Student accounted"
                            }
                        }

                        401 -> {
                            requireActivity().runOnUiThread {
                                viewModel.status.value = "wrong token!"
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * calculates distance and sets status
     */
    private fun getAttendee(firstName: String, secondName: String, attendeeLocation: GeoLocation?): Attendee {
        var status = Status.OK
        if (viewModel.isUsingGeodata) {
            if (attendeeLocation == null) {
                status = Status.NO_DATA
            } else {
                val results = FloatArray(3)
                Location.distanceBetween(
                    attendeeLocation.latitude,
                    attendeeLocation.longitude,
                    viewModel.ownLocation?.latitude ?: 0.0,
                    viewModel.ownLocation?.longitude ?: 0.0,
                    results
                )
                Log.d(T, "$firstName distance = ${results[0]}")
                if (results[0] > allowedDistance) {
                    status = Status.OUT_OF_RANGE
                }
            }
        }
        return Attendee(firstName, secondName, status)
    }

    /**
     * Utility function to run try catch on a network request
     * Makes request and handles error display
     */
    private fun tryCatchNetworkError(block: () -> Unit) {
        try {
            Log.d(T, "tryCatchNetworkError")
            block()
            Handler(Looper.getMainLooper()).post {
                viewModel.intnetErrorMessageVisibility.value = View.GONE
            }
        } catch (e: IOException) {
            Handler(Looper.getMainLooper()).post {
//                Toast.makeText(
//                    requireContext(), //TODO: here crash may happen when no context!!!
//                    "Internet error",
//                    Toast.LENGTH_LONG
//                ).show()
                viewModel.intnetErrorMessageVisibility.value = View.VISIBLE
            }
        }
    }


    //TODO don't request permission here
    private fun showRationaleDialog(
        title: String,
        message: String,
        permission: String,
        requestCode: Int
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Ok") { _, _ ->
                requestPermissions(arrayOf(permission), requestCode)
            }
        builder.create().show()
    }

    //TODO: delete this
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.camera_permissions_error),
                    Toast.LENGTH_SHORT
                ).show()
                //TODO: don't finish but show activity with error message, or previous activity
                parentFragmentManager.commit {
                    Log.d(T, "REMOVING FRAGMENT!")
                    remove(this@CameraFragment)
                }
            }
        }
    }
}