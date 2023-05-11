package com.vysotsky.attendance

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vysotsky.attendance.api.RetrofitInstance
import com.vysotsky.attendance.models.AuthResult
import com.vysotsky.attendance.models.ProfessorAuthData
import com.vysotsky.attendance.models.ProfessorData
import com.vysotsky.attendance.util.Resource
import kotlinx.coroutines.launch

data class LoginResponse(
    val status: Int,
    val data: AuthResult?
)

class ProfessorLoginViewModel : ViewModel() {

    val loginRequestStatus = MutableLiveData<Resource<LoginResponse>>()
    val signupRequestStatus = MutableLiveData<Resource<AuthResult>>()
    val isPBVisible = MutableLiveData(false)

    lateinit var enteredEmail: String
    lateinit var enteredPassword: String

    fun login(email: String, password: String) {
        Log.d(TAG, "ProfessorLoginViewModel login($email)")
        loginRequestStatus.postValue(Resource.Loading())
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.login(ProfessorAuthData(email, password))
                val body = response.body()
                if (response.code() == 200 && body != null) {
                    loginRequestStatus.postValue(Resource.Success(LoginResponse(200, body)))
                } else {
                    when (response.code()) {
                        401 -> loginRequestStatus.postValue(Resource.Success(LoginResponse(401, null)))
                        409 -> loginRequestStatus.postValue(Resource.Success(LoginResponse(409, null)))
                        else -> loginRequestStatus.postValue(Resource.Error("couldn't make network request"))
                    }
                }
            } catch (e: Throwable) {
                loginRequestStatus.postValue(Resource.Error("couldn't make network request"))
                Log.e(TAG, "ProfessorLoginViewModel login()", e)
            }
        }
    }

    fun signup(email: String, password: String) {
        Log.d(TAG, "ProfessorLoginViewModel signup($email)")
        loginRequestStatus.postValue(Resource.Loading())
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.signup(ProfessorAuthData(email, password))
                val body = response.body()
                if (response.code() == 200 && body != null) {
                    signupRequestStatus.postValue(Resource.Success(body))
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