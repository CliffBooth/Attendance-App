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
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.api.Student
import java.io.IOException
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    Log.d(TAG, "asking for permissions: ${permissions.contentDeepToString()}")
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

fun formatDate(unixTime: Long): String {
    val format = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
    val date = Date(unixTime)
    return format.format(date)
}

fun compareByName(s1: Student, s2: Student): Boolean {
    return s1.firstName.lowercase() == s2.firstName.lowercase() && s1.secondName.lowercase() == s2.secondName.lowercase()
}

fun listOfDistinct(l: List<Student>): List<Student> {
    val res = mutableListOf<Student>()
//    Log.d(TAG, "listOfDistinct() = $l")
    for (st in l) {
        if (res.find { s -> compareByName(s, st)} == null) {
            res += Student(null, st.firstName, st.secondName)
        }
    }
//    Log.d(TAG, "listOfDistinct: $res")
    return res
}

//class for connections api
data class Endpoint(val id: String, val name: String)