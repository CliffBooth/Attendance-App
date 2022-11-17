package com.vysotsky.attendance.QRCode

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Point
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.JsonReader
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import com.vysotsky.attendance.databinding.ActivityQrcodeBinding
import androidmads.library.qrgenearator.QRGEncoder
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.vysotsky.attendance.API_URL
import com.vysotsky.attendance.MenuActivity
import com.vysotsky.attendance.R
import com.vysotsky.attendance.T
import com.vysotsky.attendance.polling
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.io.InputStreamReader

//add image dynamically or use preset?
//maybe save image to the memory?
//when return is pressed, quit the application (toast: click again to quit)
class QRCodeActivity : MenuActivity() {
    private lateinit var binding: ActivityQrcodeBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firstName: String
    private lateinit var secondName: String
    private lateinit var androidID: String
    private var dimen = 0
    //request that will be made with and without polling
    private lateinit var request: Request

    //private var token: String? = null
    private lateinit var stringToQR: String
    private val viewModel: QRCodeViewModel by viewModels()

    /**
     * this is needed, because callback from before recreate() may try to repaint the qr code.
     */
    private var isDisplayingQRCode = false

    //TODO: maybe move request, stringToQR to ViewModel (init{}) since there is no need to recreate them on every rotation?
    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermission()
        binding = ActivityQrcodeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        );

        val manager = getSystemService(WINDOW_SERVICE) as WindowManager
        val display = manager.defaultDisplay
        val point = Point()
        display.getSize(point)
        dimen = if (point.x < point.y) point.x * 3 / 4 else point.y * 3 / 4
        sharedPreferences =
            getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        //if file is empty do something (maybe send back?)
        //...
        firstName = sharedPreferences.getString(
            getString(R.string.saved_first_name),
            null
        ).toString() //TODO think about this null (send to login Activity)
        secondName = sharedPreferences.getString(getString(R.string.saved_second_name), null).toString()
        Log.d(T, "qrcodeActivity: first name = $firstName second name = $secondName")
        androidID = intent.extras?.getString("id") ?: Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        stringToQR = "$firstName:$secondName:$androidID:${viewModel.locationString.value}"
        val stringToSend = "$firstName:$secondName:$androidID"
        val json = "{\"data\":\"$stringToSend\"}"
        val body = json.toRequestBody("application/json".toMediaTypeOrNull())
        request = Request.Builder()
            .url("$API_URL/student")
            .post(body)
            .build()

        if (polling) {
            binding.checkButton.visibility = View.GONE
            binding.tryAgainButton.setOnClickListener {
                runPolling()
                viewModel.tryAgainButtonVisibility.value = View.GONE
            }
            //to only run polling once
            if (!viewModel.isRunningPolling) {
                viewModel.isRunningPolling = true
                runPolling()
            }
        } else {
            binding.checkButton.setOnClickListener {
                Log.d(T, "api call")
                sendStudent()
            }
        }

        binding.locationCheckbox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed)
                return@setOnCheckedChangeListener
            viewModel.isCheckBoxEnabled.value = false //this should be on top
            Log.d(T, "clickable disabled: ${binding.locationCheckbox.isEnabled}")
            if (!isChecked) {
                viewModel.updateLocation(this, false);
            } else {
                val permitted =
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (permitted)
                    viewModel.updateLocation(this, true)
                else {
                    Log.d(T, "No location permission!")
                    Toast.makeText(
                        this,
                        getString(R.string.permissions_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        viewModel.spinnerVisibility.observe(this) {
            binding.spinner.visibility = it
        }

        viewModel.tryAgainButtonVisibility.observe(this) {
            binding.tryAgainButton.visibility = it
        }

        viewModel.token.observe(this) {
            if (it == null) {
                setImage(stringToQR)
                binding.statusText.text = "QR CODE"
            } else {
                setImage(it)
                binding.locationCheckbox.visibility = View.GONE
                binding.statusText.text = "TOKEN"
            }
        }

        viewModel.locationString.observe(this) { location ->
            Log.d(T, "inside observer!")
            stringToQR = "$firstName:$secondName:$androidID:$location"
            setImage(stringToQR)
            Log.d(T, "clickable enabled: ${binding.locationCheckbox.isEnabled}")
        }

        viewModel.isCheckBoxEnabled.observe(this) {
            if (it == null)
                return@observe
            Log.i(T, "inside isCheckBoxEnabled.observe(), ${it}")
            binding.locationCheckbox.isEnabled = it
            Log.i(T,"locationCheckBox.isEnabled = ${binding.locationCheckbox.isEnabled}")
        }
    }

    //TODO make a a global function to enable permissions
    private fun checkPermission(): Boolean {
        val check =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (check == PackageManager.PERMISSION_GRANTED) {
            return true
        } else {
            var res = false
            Log.d(T, "QRCodeActivity: asking for location permission")
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
                                this,
                                getString(R.string.permissions_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            Log.d(T, "No location permission!")
                            Toast.makeText(
                                this,
                                getString(R.string.permissions_error),
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

    private fun runPolling() {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()
            try {
                var i = 0;
                var gotResult = false
                while (!gotResult) {
                    Log.d(T, "making polling request... ${++i}")
                    client.newCall(request).execute().use { res ->
                        when (res.code) {
                            200 -> {
                                //token = adapter.fromJson(res.body!!.source())!!.token
                                Log.i(T, "QRCodeActivity: inside 200")
                                val reader = JsonReader(InputStreamReader(res.body!!.byteStream()))
                                //reader.isLenient = true
                                Log.i(T, "1")
                                try {
                                    reader.beginObject()
                                    reader.nextName()
                                    val token = reader.nextString()
                                    Log.d(T, "read token: $token")
                                    runOnUiThread {
                                        viewModel.token.value = token
                                    }
                                } catch (e: Exception) {
                                    Log.e(T, "exception: $e")
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
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "Internet error",
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.tryAgainButtonVisibility.value = View.VISIBLE
                }
            }
        }
    }

    private fun sendStudent() {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            Log.i(T, "QRCodeActivity: enter coroutine")
            val client = OkHttpClient()
            try {
                runOnUiThread {
                    viewModel.spinnerVisibility.value = View.VISIBLE
                }
                client.newCall(request).execute().use { res ->
                    requestResultHandler(res)
                }
            } catch (e: IOException) {
                runOnUiThread {
                    viewModel.spinnerVisibility.value = View.GONE
                    Toast.makeText(
                        applicationContext,
                        "Internet error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun requestResultHandler(res: Response) {
        runOnUiThread {
            viewModel.spinnerVisibility.value = View.GONE
        }
        when (res.code) {
            200 -> {
                //token = adapter.fromJson(res.body!!.source())!!.token
                Log.i(T, "QRCodeActivity: inside 200")
                val reader = JsonReader(InputStreamReader(res.body!!.byteStream()))
                //reader.isLenient = true
                Log.i(T, "1")
                try {
                    reader.beginObject()
                    reader.nextName()
                    val token = reader.nextString()
                    Log.d(T, "read token: $token")
                    runOnUiThread {
                        viewModel.token.value = token
                    }
                } catch (e: Exception) {
                    Log.e(T, "exception: $e")
                }
            }

            401 -> {
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "can't get token",
                        Toast.LENGTH_LONG
                    ).show()
                }
                Log.d(T, "the phone hasn't been scanned, doing nothing")
            }

            406 -> {
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "ERROR SENDING REQUEST",
                        Toast.LENGTH_LONG
                    ).show()
                }
                Log.d(T, "QRCodeActivity: error sending request")
            }
        }
    }

    private fun setImage(str: String) {
        val tokenBitmap = getBitMap(str)
        if (tokenBitmap == null) {
            //TODO: HANDLE ERROR
            Log.d(T, "QR Code image is null!")
        } else {
            binding.qrCodeImage.setImageBitmap(tokenBitmap)
            binding.qrCodeText.text = str
            //binding.root.setBackgroundColor(resources.getColor(R.color.green))
        }
    }

    private fun getBitMap(string: String): Bitmap? =
        QRGEncoder(string, null, QRGContents.Type.TEXT, dimen).bitmap

    companion object {
        const val MENU_ITEM_ID = 1
        const val GROUP_ITEM_ID = 1
    }
}

//@JsonClass(generateAdapter = true)
//data class Data(val token: String)