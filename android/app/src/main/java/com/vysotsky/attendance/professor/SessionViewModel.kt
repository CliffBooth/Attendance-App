package com.vysotsky.attendance.professor

import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.api.RetrofitInstance
import com.vysotsky.attendance.models.Session
import com.vysotsky.attendance.models.Student
import com.vysotsky.attendance.professor.attendeeList.AdapterList
import com.vysotsky.attendance.professor.attendeeList.Attendee
import com.vysotsky.attendance.professor.attendeeList.GeoLocation
import com.vysotsky.attendance.util.Resource
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class SessionViewModel: ViewModel() {
    var intnetErrorMessageVisibility = MutableLiveData(View.GONE)
    var sessionStarted = false
    var isUsingGeodata = false
    var subjectName = "error"
    var ownLocation: GeoLocation? = null
    val attendeesList =
        AdapterList<Attendee>() //TODO: AdapterList holds adapter reference, adapter holds context reference which is not allowed in ViewModel


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


    val requestStatus = MutableLiveData<Resource<Unit>>()
    val isStopButtonEnabled = MutableLiveData(true)
    val isPBVisible = MutableLiveData(false)

    /**
     * save current session on the backend
     */
    fun postSession(email: String) {
        Log.d(TAG, "SessionViewModel: postSession() email = $email")

        requestStatus.postValue(Resource.Loading())
        //students should be already registered, otherwise result will be: registered session but zero students
        val students = attendeesList.map { a -> Student(a.id, a.firstName, a.secondName) }
        val session = Session("", students, subjectName)
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.addSession(email, session)
                if (response.isSuccessful)
                    requestStatus.postValue(Resource.Success(Unit))
                else
                    requestStatus.postValue(Resource.Error(response.message()))
                Log.d(TAG, "SessionViewModel: postSession(): response = ${response}")
            } catch (e: Throwable) {
                requestStatus.postValue(Resource.Error("couldn't make network request!"))
                Log.e(TAG, "SessionViewModel: postSession() ", e)
            }
        }
    }
}