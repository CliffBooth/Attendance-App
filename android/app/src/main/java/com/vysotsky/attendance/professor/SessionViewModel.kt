package com.vysotsky.attendance.professor

import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.api.RetrofitInstance
import com.vysotsky.attendance.models.ProfessorData
import com.vysotsky.attendance.models.Session
import com.vysotsky.attendance.models.Student
import com.vysotsky.attendance.professor.attendeeList.AdapterList
import com.vysotsky.attendance.professor.attendeeList.Attendee
import com.vysotsky.attendance.professor.attendeeList.GeoLocation
import com.vysotsky.attendance.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SessionViewModel: ViewModel() {
    var intnetErrorMessageVisibility = MutableLiveData(View.GONE)
    var sessionStarted = false
    var isUsingGeodata = false
    var runningPolling = true
        set(value) {
            Log.d(TAG, "SessionViewModel: setting runningPolling = $value")
            field = value
        }
    var subjectName = "error"
    var ownLocation: GeoLocation? = null
    val attendeesList =
        AdapterList<Attendee>() //TODO: AdapterList holds adapter reference, adapter holds context reference which is not allowed in ViewModel (view AttendeesListFragment class)
    val isSessionTerminated = MutableLiveData(false)


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

    val postSessionStatus = MutableLiveData<Resource<Unit>>()
    val endSessionStatus = MutableLiveData<Resource<Unit>>()
    val isStopButtonEnabled = MutableLiveData(true)
    val isPBVisible = MutableLiveData(false)

    /**
     * save current session on the backend
     */
    fun postSession(email: String, token: String) {
        Log.d(TAG, "SessionViewModel: postSession() email = $email")

        postSessionStatus.postValue(Resource.Loading())
        //students should be already registered, otherwise result will be: registered session but zero students
        val students = attendeesList.map { a -> Student(a.id, a.firstName, a.secondName) }
        val session = Session("", students, subjectName)
        if (students.isEmpty())
            return
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.addSession(email, session, token)
                if (response.isSuccessful)
                    postSessionStatus.postValue(Resource.Success(Unit))
                else
                    postSessionStatus.postValue(Resource.Error(response.message()))
                Log.d(TAG, "SessionViewModel: postSession(): response = ${response}")
            } catch (e: Throwable) {
                postSessionStatus.postValue(Resource.Error("couldn't make network request!"))
                Log.e(TAG, "SessionViewModel: postSession() ", e)
            }
        }
    }

    fun endSession(email: String) {
        Log.d(TAG, "SessionViewModel, endSession() email = $email ")
        endSessionStatus.postValue(Resource.Loading())
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.endSession(ProfessorData(email))
                if (response.code() == 406) { //no email in request
                    endSessionStatus.postValue(Resource.Error("weird stuff"))
                } else {
                    endSessionStatus
                }
            } catch (e: Throwable) {
                Log.e(TAG, "SessionViewModel, endSession() ", e)
                endSessionStatus.postValue(Resource.Error("error ending session"))
            }
        }
    }

    //polling, constantly updating attendeeList
    fun runPolling(email: String) {
        var i = 1
        viewModelScope.launch {
            while (runningPolling) {
                Log.d(TAG, "SessionViewModel: runPolling() ${i++}")
                try {
                    val response = RetrofitInstance.api.getCurrentStudentsList(ProfessorData(email))
                    if (response.isSuccessful) {
                        val list = response.body()!!.students.map { s -> Attendee(s.firstName, s.secondName, s.phoneId) }
                        list.forEach { a ->
                            if (attendeesList.none { it.id == a.id }) {
                                addAttendeeToList(a)
                            }
                        }
                    } else {
                        //if session has been terminated, terminate it on the android
                        if (response.code() == 401) {
                            isSessionTerminated.postValue(true)
                        }
                        Log.e(TAG, "SessionViewModel: runPolling() status code: ${response.code()}")
                        runningPolling = false
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "SessionViewModel: runPolling() ", e)
                }
                delay(1000)
            }
        }
    }
}