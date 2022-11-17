package com.vysotsky.attendance.camera

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraViewModel: ViewModel() {
    val spinnerVisibility = MutableLiveData(View.GONE)
    var intnetErrorMessageVisibility = MutableLiveData(View.GONE)
    var sessionStarted = false
    var usingGeolocation = false
    var ownLocation: GeoLocation? = null
    val attendeesList = mutableListOf<Attendee>()
    @Volatile
    var lastSent: String? = null
    @Volatile
    var nameSent = false
    val status = MutableLiveData("Nothing")
    var isUsingGeodata = false
}