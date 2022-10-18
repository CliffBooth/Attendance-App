package com.vysotsky.attendance

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Point
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidmads.library.qrgenearator.QRGContents
import com.vysotsky.attendance.databinding.ActivityQrcodeBinding
import androidmads.library.qrgenearator.QRGEncoder

//add image dynamically or use preset?
//maybe save image to the memory?
//when return is pressed, quit the application (toast: click again to quit)
class QRCodeActivity : MenuActivity() {
    private lateinit var binding: ActivityQrcodeBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var dimen = 0

    //TODO: save image as state to not recreate when screen flipped
    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrcodeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        //if file is empty do something (maybe send back?)
        //...
        val firstName = sharedPreferences.getString(getString(R.string.saved_first_name), null) //TODO think about this null
        val secondName = sharedPreferences.getString(getString(R.string.saved_second_name), null)
        Log.d(T, "qrcodeActivity: first name = $firstName second name = $secondName")

        val androidId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        val stringToQR = "$firstName:$secondName:$androidId"
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
            Log.d(T, "api call mock...")
            val tokenMock = "12345"

            val tokenBitmap = getBitMap(tokenMock)
            if (tokenBitmap == null) {
                //TODO: HANDLE ERROR
                //...
                Log.d(T, "QR Code image is null!")
            } else {
                imageView.setImageBitmap(tokenBitmap)
                binding.root.setBackgroundColor(resources.getColor(R.color.green))
            }
        }

    }

    private fun getBitMap(string: String): Bitmap? =
        QRGEncoder(string, null, QRGContents.Type.TEXT, dimen).bitmap
}