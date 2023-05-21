package com.vysotsky.attendance.professor

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.api.RetrofitInstance
import com.vysotsky.attendance.api.ProfessorData
import com.vysotsky.attendance.api.RegisterSessoinBody
import com.vysotsky.attendance.util.Resource
import kotlinx.coroutines.launch

class StartSessionVIewModel : ViewModel() {
    val spinnerVisibility = MutableLiveData(false)
    val startButtonEnabled = MutableLiveData(true)
    val startButtonDisplayed = MutableLiveData(true)
    var subjectName = "error"

    val offlineSession = MutableLiveData(true)

    val requestStatus = MutableLiveData<Resource<Int>>()
    val startSessionStatus = MutableLiveData<Resource<Int>>()

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "StartSessionViewModel onCleared() ")
    }

    //check if session has already been started by result code: 200 if yes, 401 if no.
    //if can't make network request, then don't register session pressing on a button. (specify that starting offline)
    fun checkSession(email: String) {
        requestStatus.postValue(Resource.Loading())

        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getCurrentStudentsList(ProfessorData(email))
                if (response.isSuccessful) {
                    subjectName = response.body()!!.subjectName
                }
                requestStatus.postValue(Resource.Success(response.code()))
                offlineSession.postValue(false)
            } catch (e: Throwable) {
                offlineSession.postValue(true)
                requestStatus.postValue(Resource.Error("couldn't make network request!"))
                Log.e(TAG, "StartSessionViewModel: checkSession()", e)
            }
        }
    }

    fun registerSession(email: String, subjectName: String) {
        startSessionStatus.postValue(Resource.Loading())

        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.registerSession(RegisterSessoinBody(email, subjectName))

            } catch (e: Throwable) {
                startSessionStatus.postValue(Resource.Error("network error"))
                Log.e(TAG, "StartSessionViewModel: registerSession()", e)
            }
        }
    }

}