package com.vysotsky.attendance

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Point
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.JsonReader
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import com.vysotsky.attendance.databinding.ActivityQrcodeBinding
import androidmads.library.qrgenearator.QRGEncoder
import androidx.lifecycle.lifecycleScope
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL

//add image dynamically or use preset?
//maybe save image to the memory?
//when return is pressed, quit the application (toast: click again to quit)
class QRCodeActivity : MenuActivity() {
    private lateinit var binding: ActivityQrcodeBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var dimen = 0
    private var token: String? = null
    private lateinit var stringToQR: String

    //TODO: save image as state to not recreate when screen flipped
    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrcodeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        //if file is empty do something (maybe send back?)
        //...
        val firstName = sharedPreferences.getString(getString(R.string.saved_first_name), null) //TODO think about this null
        val secondName = sharedPreferences.getString(getString(R.string.saved_second_name), null)
        Log.d(T, "qrcodeActivity: first name = $firstName second name = $secondName")

        val androidId = intent.extras?.getString("id") ?:
            Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)

        stringToQR = "$firstName:$secondName:$androidId"
        val manager = getSystemService(WINDOW_SERVICE) as WindowManager
        val display = manager.defaultDisplay
        val point = Point()
        display.getSize(point)
        dimen = if (point.x < point.y) point.x * 3 / 4 else point.y * 3 / 4

        val imageView = binding.qrCodeImage
        val bitmap = getBitMap(stringToQR)
        if (bitmap == null) {
            //TODO: HANDLE ERROR
            //...
            Log.d(T, "QR Code image is null!")
            return
        }
        imageView.setImageBitmap(getBitMap(stringToQR))

        binding.checkButton.setOnClickListener {
            //TODO: API call
            //TODO: HANDLE IF RESPONSE IS NOT A TOKEN (TOAST: YOU CAN'T CHECK RIGHT NOW or something)
            Log.d(T, "api call")
//            val tokenMock = "12345"
            sendStudent()
        }

    }

    private fun sendStudent() {
        lifecycleScope.launch(Dispatchers.IO) {
            Log.i(T, "QRCodeActivity: enter coroutine")
            val client = OkHttpClient()
            //val moshi = Moshi.Builder().build()
            //val adapter = moshi.adapter(Data::class.java)
            val json = "{\"data\":\"$stringToQR\"}"
            val body = json.toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("$API_URL/student")
                .post(body)
                .build()
            Log.i(T, "QRCodeActivity: request built")
            client.newCall(request).execute().use { res ->
                when (res.code) {
                    200 -> {
                        //token = adapter.fromJson(res.body!!.source())!!.token
                        Log.i(T, "QRCodeActivity: inside 200")
                        val reader = JsonReader(InputStreamReader(res.body!!.byteStream()))
                        //reader.isLenient = true
                        Log.i(T, "1")
                        try {
//                            reader.beginObject()
                            reader.beginObject()
                            reader.nextName()
                            token = reader.nextString()
                            Log.d(T, "read token: $token")
                        } catch (e: Exception) {
                            Log.d(T, "exception: $e")
                        }
                        Log.i(T, "2")

                        Log.i(T, "parsed token: ${token}")
                        runOnUiThread { setImage() }
                    }
                    401 -> {
                        runOnUiThread {
                            Toast.makeText(applicationContext, "can't get token", Toast.LENGTH_SHORT).show()
                        }
                        Log.d(T, "the phone hasn't been scanned, doing nothing")
                    }
                    406 -> {
                        runOnUiThread {
                            Toast.makeText(applicationContext, "ERROR SENDING REQUEST", Toast.LENGTH_LONG).show()
                        }
                        Log.d(T, "QRCodeActivity: error sending request")
                    }
                }
            }
        }
    }

    private fun setImage() {
        val tokenBitmap = getBitMap(token!!)
        if (tokenBitmap == null) {
            //TODO: HANDLE ERROR
            //...
            Log.d(T, "QR Code image is null!")
        } else {
            binding.qrCodeImage.setImageBitmap(tokenBitmap)
            binding.statusText.text = "TOKEN"
            //binding.root.setBackgroundColor(resources.getColor(R.color.green))
        }
    }

    private fun getBitMap(string: String): Bitmap? =
        QRGEncoder(string, null, QRGContents.Type.TEXT, dimen).bitmap
}

//@JsonClass(generateAdapter = true)
//data class Data(val token: String)