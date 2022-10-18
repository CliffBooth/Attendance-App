package com.vysotsky.attendance

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import com.vysotsky.attendance.databinding.FragmentCameraBinding

class CameraFragment : Fragment() {
    private var _binding: FragmentCameraBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(T, "CameraFragment onCreate()")
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> startCamera()
//            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
//                Log.d(T, "shouldShowRequestPermissionRationale")
//            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showRationaleDialog(
                    "TITLE",
                    "MESSAGE",
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
                            //TODO
                            Log.d(T, "No permission!")
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.permissions_error),
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

    override fun onDestroy() {
        super.onDestroy()
        Log.d(T, "CameraFragment onDestroy()")
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
                    getString(R.string.permissions_error),
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

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10 //??
        private val REQUIRED_PERMISSIONS = listOf(Manifest.permission.CAMERA).toTypedArray()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
//        if (allPermissionsGranted()) {
//            startCamera()
//        } else {
////            requestPermissions(
////                REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
//            val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
//                if (it) {
//                    startCamera()
//                } else {
//                    //TODO
//                    Log.d(T, "No permission!")
//                }
//            }
//            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
//        }

        return binding.root
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

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()),
            QRCodeImageAnalyzer(
                object : QRCodeImageAnalyzer.QRCodeListener {
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

    private class QRCodeImageAnalyzer(val cb: QRCodeListener) : ImageAnalysis.Analyzer {

        interface QRCodeListener {
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

/*
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
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), QRCodeImageAnalyzer(
            object: QRCodeImageAnalyzer.QRCodeListener {
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
 */