package com.vysotsky.attendance.professor

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vysotsky.attendance.professor.Attendee
import com.vysotsky.attendance.professor.GeoLocation

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
}