package com.vysotsky.attendance

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vysotsky.attendance.api.RetrofitInstance
import com.vysotsky.attendance.models.Student
//import com.vysotsky.attendance.models.StudentData
import com.vysotsky.attendance.models.StudentLoginData
import com.vysotsky.attendance.util.Resource
import kotlinx.coroutines.launch

class StudentLoginViewModel : ViewModel() {

    data class LoginResponse(val status: Int, val firstName: String?, val secondName: String?)

    val loginRequestStatus = MutableLiveData<Resource<LoginResponse>>() //response status
    val signupRequestStatus = MutableLiveData<Resource<Int>>() //response status
    val isLoginTextEditVisible = MutableLiveData(false)
    val isPBVisible = MutableLiveData(false)

    lateinit var signupFirstName: String
    lateinit var signupSecondName: String

    //email == id
    fun login(email: String) {
        Log.d(TAG, "StudentLoginViewModel: login()")

        loginRequestStatus.postValue(Resource.Loading())
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.login(StudentLoginData(phoneId = email))
                if (response.code() == 200) {
                    loginRequestStatus.postValue(
                        Resource.Success(
                            LoginResponse(
                                status = 200,
                                firstName = response.body()!!.firstName,
                                secondName = response.body()!!.secondName
                            )
                        )
                    )
                } else {
                    loginRequestStatus.postValue(
                        Resource.Success(LoginResponse(status = response.code(), null, null))
                    )
                }
            } catch (e: Throwable) {
                loginRequestStatus.postValue(Resource.Error("couldn't make network request!"))
                Log.e(TAG, "StudentLoginViewModel: login() ", e)
            }
        }
    }

    fun signup(email: String, firstName: String, secondName: String) {
        signupFirstName = firstName
        signupSecondName = secondName

        signupRequestStatus.postValue(Resource.Loading())
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.signup(Student(email, firstName, secondName))
                if (response.isSuccessful) {
                    signupRequestStatus.postValue(Resource.Success(200))
                }
            } catch (e: Throwable) {
                loginRequestStatus.postValue(Resource.Error("couldn't make network request!"))
                Log.e(TAG, "StudentLoginViewModel: signup() ", e)
            }
        }
    }
}