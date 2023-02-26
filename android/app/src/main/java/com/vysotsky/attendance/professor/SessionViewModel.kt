package com.vysotsky.attendance.professor

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vysotsky.attendance.professor.attendeeList.AdapterList
import com.vysotsky.attendance.professor.attendeeList.Attendee
import com.vysotsky.attendance.professor.attendeeList.GeoLocation

class SessionViewModel: ViewModel() {
    var intnetErrorMessageVisibility = MutableLiveData(View.GONE)
    var sessionStarted = false
    var isUsingGeodata = false
    var ownLocation: GeoLocation? = null
    val attendeesList = AdapterList<Attendee>() //TODO: AdapterList holds adapter reference, adapter holds context reference which is not allowed in ViewModel

    fun notInTheList(a: Attendee): Boolean {
        return attendeesList.find { student -> (student.id != null && student.id == a.id) } == null
    }

    fun addAttendeeToList(attendee: Attendee) {
        attendeesList += attendee
        attendeesList.notifyDataSetChanged()
    }

//    val attendeesList = mutableListOf<Attendee>()
    @Volatile
    var lastSent: String? = null
    @Volatile
    var nameSent = false
    val status = MutableLiveData("Nothing")

    //move to bluetooth! (or delete)
    val studentsNumber = MutableLiveData(0)
}