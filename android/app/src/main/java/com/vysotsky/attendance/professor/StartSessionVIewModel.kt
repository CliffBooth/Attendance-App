package com.vysotsky.attendance.professor

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.api.RetrofitInstance
import com.vysotsky.attendance.api.ProfessorData
import com.vysotsky.attendance.util.Resource
import kotlinx.coroutines.launch

class StartSessionVIewModel : ViewModel() {
    val spinnerVisibility = MutableLiveData(false)
    val startButtonEnabled = MutableLiveData(false)
    val startButtonDisplayed = MutableLiveData(true)
    var subjectName = "error"

    val requestStatus = MutableLiveData<Resource<Int>>()

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "StartSessionViewModel onCleared() ")
    }

    //check if session has already been started by result code: 200 if yes, 401 if no.
    //also, while session is going, keep checking on any additional students
    //if session is already going, change button name to "go to ongoing session" and remove class name input
    fun checkSession(email: String) {
        requestStatus.postValue(Resource.Loading())

        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getCurrentStudentsList(ProfessorData(email))
                if (response.isSuccessful) {
                    subjectName = response.body()!!.subjectName
                }
                requestStatus.postValue(Resource.Success(response.code()))
            } catch (e: Throwable) {
                requestStatus.postValue(Resource.Error("couldn't make network request!"))
                Log.e(TAG, "StartSessionViewModel: checkSession()", e)
            }
        }
    }

}