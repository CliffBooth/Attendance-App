package com.vysotsky.attendance.util

import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.vysotsky.attendance.T
import java.io.IOException

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

/**
 * Thread, used by both professor and student when sending and receiving data over bluetooth
 * @param callback - will be called when message received
 */
class ConnectedThread(
    private val socket: BluetoothSocket,
    private val callback: ((String) -> Unit)
): Thread() {
    private val buffer: ByteArray = ByteArray(1024)
    @Volatile private var running = true

    override fun run() {
        while (running) {
            try {
                val bytesRead = socket.inputStream.read(buffer)
                if (bytesRead > 0) {
                    callback(String(buffer))
                }
            } catch (e: IOException) {
                Log.d(T, "ConnectedThread: error reading data", e)
            }
        }
    }

    fun write(bytes: ByteArray) {
        try {
            socket.outputStream.write(bytes)
        } catch (e: IOException) {
            Log.d(T, "ConnectedThread: error writing data", e)
        }

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