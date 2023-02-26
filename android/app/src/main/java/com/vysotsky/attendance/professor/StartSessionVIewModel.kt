package com.vysotsky.attendance.professor

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vysotsky.attendance.TAG

class StartSessionVIewModel : ViewModel() {
    val spinnerVisibility = MutableLiveData(false)

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "StartSessionViewModel onCleared() ")
    }
}