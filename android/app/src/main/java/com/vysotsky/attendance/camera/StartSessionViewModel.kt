package com.vysotsky.attendance.camera

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StartSessionViewModel: ViewModel() {
    val spinnerVisibility = MutableLiveData(View.GONE)
}