package com.vysotsky.attendance.QRCode

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class QRCodeViewModel : ViewModel() {
    val spinnerVisibility = MutableLiveData(View.GONE)
    val token = MutableLiveData<String>()
    var pollingEnabled = false
    var tryAgainButtonVisibility = MutableLiveData(View.GONE)

    /**
     * this is needed for polling not be run every time screen is rotated (Activity recreated)
     * when created first time, QRCodeActivity runs polling and sets this to true.
     */
    var isRunningPolling = false
}