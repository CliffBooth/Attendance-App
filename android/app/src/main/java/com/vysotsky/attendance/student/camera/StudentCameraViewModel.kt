package com.vysotsky.attendance.student.camera

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.api.RetrofitInstance
import com.vysotsky.attendance.api.SendQrCodeBody
import com.vysotsky.attendance.util.Resource
import kotlinx.coroutines.launch

class StudentCameraViewModel: ViewModel() {
    val status = MutableLiveData(Status.SEARCHING)
    val requestStatus = MutableLiveData<Resource<Int>>()

    fun send(qrCode: String, data: String) {
        requestStatus.postValue(Resource.Loading())

        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.sendQRCode(SendQrCodeBody(qrCode, data))
                requestStatus.postValue(Resource.Success(response.code()))
            } catch (e: Throwable) {
                requestStatus.postValue(Resource.Error("couldn't make network request!"))
                Log.e(TAG, "StudentCameraViewModel: send() ", e)
            }
        }
    }
}

enum class Status {
    SEARCHING, SENDING, SENT
}