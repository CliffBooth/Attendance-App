package com.vysotsky.attendance.professor

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.api.RetrofitInstance
import com.vysotsky.attendance.models.Session
import com.vysotsky.attendance.util.Resource
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class ProfessorHomeViewModel : ViewModel() {
    val sessions = MutableLiveData<Resource<List<Session>>>()

    val tvNoItemsVisibility = MutableLiveData(false)
    val btnRetryRequestVisibility = MutableLiveData(false)

    //to avoid making request with every screen rotation
    private var searchedOnStartUp = false

    fun getSessions(email: String, force: Boolean = false) {
        if (!force && searchedOnStartUp)
            return

        Log.d(TAG, "ProfessorHomeViewModel: getSessions() email = $email")
        sessions.postValue(Resource.Loading())

        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getProfessorSessions(email)
                if (response.body() != null)
                    sessions.postValue(Resource.Success(response.body()!!))
                else
                    sessions.postValue(Resource.Error("empty response body!"))
            } catch (e: Throwable) {
                sessions.postValue(Resource.Error("couldn't make network request!"))
                when (e) {
                    is HttpException -> {
                        Log.e(TAG, "ProfessorHomeViewModel: ", e)
                    }

                    is IOException -> {
                        Log.e(TAG, "getSessions: ", e)
                    }

                    else -> Log.d(TAG, "getSessions: ", e)
                }
            }
        }
        searchedOnStartUp = true
    }
}