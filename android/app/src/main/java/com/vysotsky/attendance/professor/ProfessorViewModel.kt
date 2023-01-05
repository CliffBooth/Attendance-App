package com.vysotsky.attendance.professor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vysotsky.attendance.BLUETOOTH_UUID
import com.vysotsky.attendance.T
import com.vysotsky.attendance.professor.bluetooth.ProfessorBluetoothFragment
import com.vysotsky.attendance.util.ConnectedThread
import java.io.IOException

class ProfessorViewModel: ViewModel() {
    var intnetErrorMessageVisibility = MutableLiveData(View.GONE)
    var sessionStarted = false
    var isUsingGeodata = false
    var ownLocation: GeoLocation? = null
    val attendeesList = AdapterList<Attendee>()
//    val attendeesList = mutableListOf<Attendee>()
    @Volatile
    var lastSent: String? = null
    @Volatile
    var nameSent = false
    val status = MutableLiveData("Nothing")

    //bluetooth
    val scanMode = MutableLiveData(BluetoothAdapter.SCAN_MODE_NONE)
    var bluetoothPermission = false
    private var acceptThread: ProfessorBluetoothFragment.AcceptThread? = null
    private var connectedThreads = mutableListOf<ConnectedThread>()
    val studentsNumber = MutableLiveData(0)
    val message = MutableLiveData("")

    fun runServer(acceptThread: ProfessorBluetoothFragment.AcceptThread) {
        if (this.acceptThread != null) {
            return
//            this.acceptThread!!.cancel()
        }
        this.acceptThread = acceptThread
        Log.d(T, "ProfessorViewModel: new acceptThread is set")
        acceptThread.start()
    }

    fun runConnectedThread(t: ConnectedThread) {
        connectedThreads += t
        t.start()
    }

    fun stopServer() {
        this.acceptThread?.cancel()
        connectedThreads.forEach {
            it.cancel()
        }
//        this.connectedThreads = null
        this.acceptThread = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(T, "ProfessorViewModel: onClear()")
        stopServer()
    }
}