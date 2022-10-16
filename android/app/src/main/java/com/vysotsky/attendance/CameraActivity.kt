package com.vysotsky.attendance

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import com.vysotsky.attendance.databinding.ActivityCameraBinding

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        val email = sharedPreferences.getString(getString(R.string.saved_email), "ERROR!");
        Log.d(T, "Camera activity: email: $email")

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                bindCameraPreview(cameraProvider)
            } catch(e: Exception) {
                //TODO: notify user about error
                Log.e(T, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraPreview(cameraProvider: ProcessCameraProvider) {
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        val preview = Preview.Builder()
            .build()
            .also {
                //getSurfaceProvider vs createSurfaceProvider ??
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
        /*
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
         */
        val imageAnalysis = ImageAnalysis.Builder()
//            .setTargetResolution(Size(1280, 720))
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), QRCodeImageAnalyzer(
            object: QRCodeImageAnalyzer.QRCodeFoundListener {
                var sent = false
                override fun onQRCodeFound(string: String) {
                    //TODO: make api request and stuff
                    if (!sent) {
                        sent = true
                        binding.debugText.text = "qr code found: ${string}"
                    }
                }
                var prevTime = 0L
                var counter = 1
                override fun onQRCodeNotFound() {
                    if (sent) return
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - prevTime >= 1000) {
                        prevTime = currentTime
                        binding.debugText.text = "qr code not found! ${counter++}"
                    }
                }

            }
        ))
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    getString(R.string.permissions_error),
                    Toast.LENGTH_SHORT).show()
                //TODO: don't finish but show activity with error message, or previous activity
                finish()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10 //??
        private val REQUIRED_PERMISSIONS = listOf(Manifest.permission.CAMERA).toTypedArray()
    }

    private class QRCodeImageAnalyzer(val cb: QRCodeFoundListener): ImageAnalysis.Analyzer {
        interface QRCodeFoundListener {
            fun onQRCodeFound(string: String)
            fun onQRCodeNotFound()
        }

        override fun analyze(image: ImageProxy) {
            val byteBuffer = image.planes[0].buffer
            val imageData = ByteArray(byteBuffer.capacity())
            byteBuffer.get(imageData)

            val source = PlanarYUVLuminanceSource(
                imageData,
                image.width,
                image.height,
                0,
                0,
                image.width,
                image.height,
                false
            )

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                val result = QRCodeMultiReader().decode(binaryBitmap)
                cb.onQRCodeFound(result.text)
            } catch (e: ReaderException) {
                cb.onQRCodeNotFound()
            }
            image.close()
        }

    }
}