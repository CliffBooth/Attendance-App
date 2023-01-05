package com.vysotsky.attendance.util

import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.vysotsky.attendance.T
import java.io.IOException
import java.nio.charset.Charset

/**
 * check if granted and require if not granted.
 * @param context - context to checkSelfPermission
 * @param resultCaller - Activity or Fragment, to call registerForActivityResult() on
 * @param callback - will be called if permissions are not granted (like display a toast)
 */
fun checkPermissions(
    context: Context,
    resultCaller: ActivityResultCaller,
    permissions: Array<String>,
    callback: (() -> Unit)? = null
): Boolean {
    var check = true
    permissions.forEach { permission ->
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            check = false
            return@forEach
        }
    }
    if (check) return true

    var res = false
    Log.d(T, "asking for permissions: ${permissions.contentDeepToString()}")
    val permissionsLauncher =
        resultCaller.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it.any { entry -> !entry.value }) {
                if (callback != null) {
                    callback()
                }
            } else {
                res = true
            }
        }
    permissionsLauncher.launch(permissions)
    return res
}

const val MESSAGE_READ: Int = 0
const val MESSAGE_WRITE: Int = 1
const val MESSAGE_TOAST: Int = 2
const val MESSAGE_CLOSE: Int = 3

/**
 * Thread, used by both professor and student when sending and receiving data over bluetooth
 */
//add handler to send messages back to the fragment
class ConnectedThread(
    private val socket: BluetoothSocket,
    private val handler: ThreadHandler
) : Thread() {
    private val buffer: ByteArray = ByteArray(1024)
    @Volatile
    private var running = true

    init {
        handler.thisThread = this
    }

    override fun run() {
        var bytesRead = 0
        while (running) {
            try {
                bytesRead = socket.inputStream.read(buffer)
                val readMsg = handler.obtainMessage(
                    MESSAGE_READ, bytesRead, -1, buffer
                )
                readMsg.sendToTarget()
            } catch (e: IOException) {
                running = false
                val closeMsg = handler.obtainMessage(MESSAGE_CLOSE)
                closeMsg.sendToTarget()
                Log.d(T, "ConnectedThread: error reading data; bytesRead = ${bytesRead}", e)
            }
        }
    }

    fun write(bytes: ByteArray) {
        try {
            socket.outputStream.write(bytes)
        } catch (e: IOException) {
            Log.d(T, "ConnectedThread: error writing data", e)
            //TODO make toast in activity or something
            /*
            Log.e(TAG, "Error occurred when sending data", e)

               // Send a failure message back to the activity.
               val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
               val bundle = Bundle().apply {
                   putString("toast", "Couldn't send data to the other device")
               }
               writeErrorMsg.data = bundle
               handler.sendMessage(writeErrorMsg)
               return
             */
        }
        val writtenMsg = handler.obtainMessage(
            MESSAGE_WRITE, -1, -1, buffer
        )
        writtenMsg.sendToTarget()
    }

    fun cancel() {
        try {
            running = false
            socket.close()
        } catch (e: IOException) {
            Log.d(T, "ConnectedThread: error closing socket", e)
        }
    }
}

/**
 * Activity or Fragment that uses ConnectedThread should create a handler class
 * inherited from ThreadHandler
 */
open class ThreadHandler : Handler(Looper.getMainLooper()) {
    lateinit var thisThread: ConnectedThread
}

fun String.Companion.fromByteArray(byteArray: ByteArray, n: Int): String {
    return String(byteArray.copyOfRange(0, n), Charset.defaultCharset())
}