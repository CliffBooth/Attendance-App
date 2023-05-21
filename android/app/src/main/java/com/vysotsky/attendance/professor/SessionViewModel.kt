package com.vysotsky.attendance.professor

import android.content.Context
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.api.*
import com.vysotsky.attendance.database.Class
import com.vysotsky.attendance.database.getDatabase
import com.vysotsky.attendance.professor.attendeeList.AdapterList
import com.vysotsky.attendance.professor.attendeeList.Attendee
import com.vysotsky.attendance.professor.attendeeList.GeoLocation
import com.vysotsky.attendance.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SessionViewModel : ViewModel() {
    var intnetErrorMessageVisibility = MutableLiveData(View.GONE)
    var sessionStarted = false
    var isOffline = false
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

    fun addAttendeeToList(attendee: Attendee) { //maybe put a uniqueness check here?
        attendeesList += attendee
        attendeesList.notifyDataSetChanged()
    }

    /**
     * Attempt to save manually added attendee on the backend
     */
    fun saveAttendee(email: String, attendee: Attendee) {
        if (isOffline)
            return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = RetrofitInstance.api.addAttendeeToCurrentSession(
                    ManualStudentData(
                        email = email,
                        student = Student(phoneId = null, attendee.firstName, attendee.secondName)
                    )
                )
                if (!resp.isSuccessful) {
                    Log.d(TAG, "SessionViewModel: saveAttendee() can't saveAttendee, message = ${resp.message()}")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "SessionViewModel: saveAttendee()", e)
            }
        }
    }

    fun deleteAttendee(email: String, attendee: Attendee) {
        if (isOffline)
            return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = RetrofitInstance.api.deleteAttendeeFromCurrentSession(
                    ManualStudentData(
                        email = email,
                        student = Student(phoneId = null, attendee.firstName, attendee.secondName)
                    )
                )
                if (!resp.isSuccessful) {
                    Log.d(TAG, "SessionViewModel: delete() can't deleteAttendee, message = ${resp.message()}")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "SessionViewModel: deleteAttendee()", e)
            }
        }
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

    val databaseStatus = MutableLiveData<Resource<Unit>>()

    /**
     * save current session on the backend
     */
    fun postSession(email: String, token: String) {
        Log.d(TAG, "SessionViewModel: postSession() email = $email")

        postSessionStatus.postValue(Resource.Loading())
        //students should be already registered, otherwise result will be: registered session but zero students
        val students = attendeesList.map { a -> Student(a.id, a.firstName, a.secondName) }
        val session = Session(System.currentTimeMillis(), students, subjectName)
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

    fun saveSessionToDatabase(context: Context) {
        Log.d(TAG, "SessionViewModel saveSessionToDatabase()")
        val students = attendeesList.map { a -> Student(a.id, a.firstName, a.secondName) }
        if (students.isEmpty())
            return
        viewModelScope.launch {
            try {
                getDatabase(context).classDao.insertClass(Class(
                    date = System.currentTimeMillis(),
                    subjectName = subjectName,
                    ArrayList(students)
                ))
                databaseStatus.postValue(Resource.Success(data=Unit))
            } catch (e: Throwable) {
                databaseStatus.postValue(Resource.Error("cannot save the session!"))
            }
        }
    }

    fun endSession(email: String) {
        if (isOffline) {
            endSessionStatus.postValue(Resource.Success(Unit))
            return
        }
        Log.d(TAG, "SessionViewModel, endSession() email = $email ")
        endSessionStatus.postValue(Resource.Loading())
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.endSession(ProfessorData(email))
                if (response.code() == 406) { //no email in request
                    endSessionStatus.postValue(Resource.Error("weird stuff"))
                } else {
                    Log.d(TAG, "SessionViewModel else branch")
                    endSessionStatus.postValue(Resource.Success(Unit))
                }
            } catch (e: Throwable) {
                Log.e(TAG, "SessionViewModel, endSession() ", e)
                endSessionStatus.postValue(Resource.Error("error ending session"))
            }
        }
    }

    //polling, constantly updating attendeeList
    fun runPolling(email: String) {
        if (isOffline)
            return
        var i = 1
        viewModelScope.launch {
            while (runningPolling) {
                Log.d(TAG, "SessionViewModel: runPolling() ${i++}")
                try {
                    val response = RetrofitInstance.api.getCurrentStudentsList(ProfessorData(email))
                    if (response.isSuccessful) {
                        val list = response.body()!!.students.map { s ->
                            Attendee(
                                s.firstName,
                                s.secondName,
                                s.phoneId
                            )
                        }
                        Log.d(TAG, "runPolling() attendeesList = $attendeesList")
                        list.forEach { a ->
                            // if has id, comparing by id, if doesn't comparing by name, which should be unique (repeating names won't be saved in the database)
                            if (a.id != null && attendeesList.none { it.id == a.id }
                                || a.id == null && attendeesList.none { it.firstName == a.firstName && it.secondName == a.secondName } ) {
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
                delay(3000)
            }
        }
    }
}