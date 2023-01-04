package com.vysotsky.attendance.professor

import android.bluetooth.BluetoothAdapter
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProfessorViewModel: ViewModel() {
    var intnetErrorMessageVisibility = MutableLiveData(View.GONE)
    var sessionStarted = false
    var isUsingGeodata = false
    var ownLocation: GeoLocation? = null
    val attendeesList = mutableListOf<Attendee>()
    @Volatile
    var lastSent: String? = null
    @Volatile
    var nameSent = false
    val status = MutableLiveData("Nothing")

    //bluetooth
    val scanMode = MutableLiveData(BluetoothAdapter.SCAN_MODE_NONE)
    var bluetoothPermission = false
}