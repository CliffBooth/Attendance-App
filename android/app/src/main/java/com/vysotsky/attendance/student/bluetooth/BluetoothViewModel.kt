package com.vysotsky.attendance.student.bluetooth

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BluetoothViewModel : ViewModel() {
    //TODO maybe these statuses should be in activityViewModel, so when you switch between fragments they are still there
    val connectionStatus = MutableLiveData(ConnectionStatus.NONE)
    val accountedStatus = MutableLiveData(AccountedStatus.NONE)
    val devicesList = mutableListOf<BluetoothDevice>()
}

enum class AccountedStatus {
    NONE, OK, ERROR, ALREADY_SCANNED
}

enum class ConnectionStatus {
    NONE, SEARCHING, SEARCH_DONE, CONNECTING, ALREADY_CONNECTED, MESSAGE_SENT, GOT_RESPONSE, DISCONNECTED

}