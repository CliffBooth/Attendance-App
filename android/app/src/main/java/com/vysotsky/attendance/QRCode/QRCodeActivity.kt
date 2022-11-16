package com.vysotsky.attendance.QRCode

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Point
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
import androidx.activity.viewModels
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
    private var dimen = 0

    //private var token: String? = null
    private lateinit var stringToQR: String
    private val viewModel: QRCodeViewModel by viewModels()

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrcodeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        );
        sharedPreferences =
            getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        //if file is empty do something (maybe send back?)
        //...
        val firstName = sharedPreferences.getString(
            getString(R.string.saved_first_name),
            null
        ) //TODO think about this null (send to login Activity)
        val secondName = sharedPreferences.getString(getString(R.string.saved_second_name), null)
        Log.d(T, "qrcodeActivity: first name = $firstName second name = $secondName")

        val androidId = intent.extras?.getString("id") ?: Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )

        stringToQR = "$firstName:$secondName:$androidId"
        val manager = getSystemService(WINDOW_SERVICE) as WindowManager
        val display = manager.defaultDisplay
        val point = Point()
        display.getSize(point)
        dimen = if (point.x < point.y) point.x * 3 / 4 else point.y * 3 / 4

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
                binding.statusText.text = "TOKEN"

            }
        }
    }

    private fun runPolling() {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()
            val json = "{\"data\":\"$stringToQR\"}"
            val body = json.toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("$API_URL/student")
                .post(body)
                .build()

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
                            else -> {delay(1000)}
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

    override fun onResume() {
        super.onResume()
        Log.d(T, "inside on resume")
        if (viewModel.token.value != null) {
            Log.d(T, "inside value != null")
            setImage(viewModel.token.value!!) //TODO: handle null
            binding.statusText.text = "TOKEN"
        } else {
            setImage(stringToQR)
        }
        if (viewModel.pollingEnabled) {
            binding.checkButton.visibility = View.GONE
        }
    }

    private fun sendStudent() {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            Log.i(T, "QRCodeActivity: enter coroutine")
            val client = OkHttpClient()
            val json = "{\"data\":\"$stringToQR\"}"
            val body = json.toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("$API_URL/student")
                .post(body)
                .build()

            Log.i(T, "QRCodeActivity: request built")

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
                        Toast.LENGTH_SHORT
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
            //...
            Log.d(T, "QR Code image is null!")
        } else {
            binding.qrCodeImage.setImageBitmap(tokenBitmap)
            //binding.root.setBackgroundColor(resources.getColor(R.color.green))
        }
    }

    private fun getBitMap(string: String): Bitmap? =
        QRGEncoder(string, null, QRGContents.Type.TEXT, dimen).bitmap

    companion object {
        const val TOKEN_KEY = "token_key"
        const val SPINNER_KEY = "spinner_visibility"
    }
}

//@JsonClass(generateAdapter = true)
//data class Data(val token: String)