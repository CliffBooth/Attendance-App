package com.vysotsky.attendance

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vysotsky.attendance.api.RetrofitInstance
import com.vysotsky.attendance.models.ProfessorData
import com.vysotsky.attendance.util.Resource
import kotlinx.coroutines.launch

class ProfessorLoginViewModel : ViewModel() {

    val loginRequestStatus = MutableLiveData<Resource<Int>>() //response status
    val signupRequestStatus = MutableLiveData<Resource<Unit>>() //response    status
    val isPBVisible = MutableLiveData(false)

    lateinit var enteredEmail: String

    fun login(email: String) {
        Log.d(TAG, "ProfessorLoginViewModel login($email)")
        loginRequestStatus.postValue(Resource.Loading())
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.login(ProfessorData(email))
                if (response.code() == 200) {
                    loginRequestStatus.postValue(Resource.Success(200))
                } else {
                    loginRequestStatus.postValue(Resource.Success(401))
                }
            } catch (e: Throwable) {
                loginRequestStatus.postValue(Resource.Error("couldn't make network request"))
                Log.e(TAG, "ProfessorLoginViewModel login()", e)
            }
        }
    }

    fun signup(email: String) {
        Log.d(TAG, "ProfessorLoginViewModel signup($email)")
        loginRequestStatus.postValue(Resource.Loading())
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.signup(ProfessorData(email))
                if (response.code() == 200) {
                    signupRequestStatus.postValue(Resource.Success(Unit))
                } else {
                    signupRequestStatus.postValue(Resource.Error(response.message()))
                }
            } catch (e: Throwable) {
                loginRequestStatus.postValue(Resource.Error("couldn't make network request"))
                Log.e(TAG, "ProfessorLoginViewModel login()", e)
            }
        }
    }

}