package com.vysotsky.attendance.util

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader

class QRCodeImageAnalyzer(private val callBack: QRCodeListener) : ImageAnalysis.Analyzer {

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
            callBack.onQRCodeFound(result.text)
        } catch (e: ReaderException) {
            callBack.onQRCodeNotFound()
        }
        image.close()
    }

}