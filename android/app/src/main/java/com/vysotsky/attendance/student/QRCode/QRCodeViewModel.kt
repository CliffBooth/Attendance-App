package com.vysotsky.attendance.student.QRCode

import android.content.Context
import android.location.LocationRequest
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.debug
import com.vysotsky.attendance.randomLocation
import java.util.Random

class QRCodeViewModel : ViewModel() {
    val spinnerVisibility = MutableLiveData(false)
    val token = MutableLiveData<String>()
//    var pollingEnabled = false
    var tryAgainButtonVisibility = MutableLiveData(false)
//    var isUsingLocation = false

    // var stringToQR = MutableLiveData<String>()
    private val _locationString = MutableLiveData<String>("null")
    val locationString: LiveData<String>
        get() = _locationString
    val isCheckBoxEnabled = MutableLiveData<Boolean>()

    /**
     * this is needed for polling not be run every time screen is rotated (Activity recreated)
     * when created first time, QRCodeActivity runs polling and sets this to true.
     */
    var isRunningPolling = false

    var isPollingActive = false

    /**
     * because there is some logic after update, you can't simply assign value to locationString,
     * you always need to call updateLocation
     * @param getLocatoin - true if make location request, false if use null instead
     */
    fun updateLocation(context: Context, getLocation: Boolean) {
        Log.d(TAG, "inside updateLocation()")
        if (!getLocation) {
            _locationString.value = "null"
            isCheckBoxEnabled.value = true
            return
        }

        if (debug && randomLocation) {
            val random = Random()
            val lon = random.nextDouble() * 1000
            val lat = random.nextDouble() * 1000
            _locationString.value = "$lon--$lat"
            isCheckBoxEnabled.value = true
            return
        }

        try {
            val priority = if (android.os.Build.VERSION.SDK_INT >= 31)
                LocationRequest.QUALITY_HIGH_ACCURACY
            else
                100

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.getCurrentLocation(
                priority,
                object : CancellationToken() {
                    override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                        CancellationTokenSource().token

                    override fun isCancellationRequested() = false
                }).addOnSuccessListener { location ->
                //TODO: bug: if no gps then location == 0. Notify user and cancel this operation
                if (location == null) {
                    //gps was turned off
                    Log.d(TAG, "QRCodeViewModel displaying toast...")
                    Toast.makeText(context, "can't retrieve location data!", Toast.LENGTH_LONG)
                        .show()
                    //_locationString.value = "null"
                } else {
                    val lon = location.longitude
                    val lat = location.latitude
                    Log.d(TAG, "inside callback: longitude = $lon, latitude = $lat")
                    _locationString.value = "$lon--$lat"
                }
                isCheckBoxEnabled.value = true
            }
        } catch (e: SecurityException) {
            Log.d(TAG, e.toString())
        }
    }
}