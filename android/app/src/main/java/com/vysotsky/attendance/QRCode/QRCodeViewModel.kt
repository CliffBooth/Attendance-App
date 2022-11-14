package com.vysotsky.attendance.QRCode

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class QRCodeViewModel : ViewModel() {
    val spinnerVisibility = MutableLiveData(View.GONE)
    val token = MutableLiveData<String>()
}