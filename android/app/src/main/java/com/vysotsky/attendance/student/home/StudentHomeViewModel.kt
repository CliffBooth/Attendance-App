package com.vysotsky.attendance.student.home

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.api.RetrofitInstance
import com.vysotsky.attendance.api.StudentClass
import com.vysotsky.attendance.util.Resource
import kotlinx.coroutines.launch

class StudentHomeViewModel: ViewModel() {

    val requestStatus = MutableLiveData<Resource<List<StudentClass>>>()
    val tvNoItemsVisibility = MutableLiveData(false)
    val btnRetryRequestVisibility = MutableLiveData(false)
    private var searchedOnStartUp = false

    fun getClasses(id: String, force: Boolean = false) {
        if (!force && searchedOnStartUp)
            return

        Log.d(TAG, "StudentHomeViewModel getClasses() id = $id")
        requestStatus.postValue(Resource.Loading())
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getStudentSessions(id)
                if (response.body() != null) {
                    requestStatus.postValue(Resource.Success(response.body()!!))
                } else {
                    requestStatus.postValue(Resource.Error("empty response body!"))
                }
            } catch (e: Throwable) {
                requestStatus.postValue(Resource.Error("couldn't make network request!"))
                Log.e(TAG, "StudentHomeViewModel: getClasses() ", e)
            }
        }
        searchedOnStartUp = true
    }

}