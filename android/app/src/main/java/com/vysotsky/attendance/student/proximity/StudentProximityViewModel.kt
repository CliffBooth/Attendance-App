package com.vysotsky.attendance.student.proximity

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StudentProximityViewModel : ViewModel() {
    val accountedStatus = MutableLiveData(AccountedStatus.NONE)

    enum class AccountedStatus {
        NONE, OK, ERROR, ALREADY_SCANNED
    }
}