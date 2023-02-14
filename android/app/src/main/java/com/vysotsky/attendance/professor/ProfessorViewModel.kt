package com.vysotsky.attendance.professor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vysotsky.attendance.T
import com.vysotsky.attendance.professor.attendeeList.AdapterList
import com.vysotsky.attendance.professor.attendeeList.Attendee
import com.vysotsky.attendance.professor.attendeeList.GeoLocation
import com.vysotsky.attendance.professor.bluetooth.ProfessorBluetoothFragment
import com.vysotsky.attendance.util.ConnectedThread

class ProfessorViewModel: ViewModel() {
    var intnetErrorMessageVisibility = MutableLiveData(View.GONE)
    var sessionStarted = false
    var isUsingGeodata = false
    var ownLocation: GeoLocation? = null
    val attendeesList = AdapterList<Attendee>() //TODO: AdapterList holds adapter reference, adapter holds context reference which is not allowed in ViewModel

//    val accountedStudents: List<Student> = mutableListOf()
//    fun notInTheList(s: Student): Boolean {
//        return accountedStudents.find { student -> student.id == s.id } == null
//    }

    fun notInTheList(a: Attendee): Boolean {
        return attendeesList.find { student -> (student.id != null && student.id == a.id) } == null
    }

    fun addAttendeeToList(attendee: Attendee) {
        attendeesList += attendee
        attendeesList.notifyDataSetChanged()
    }

//    val attendeesList = mutableListOf<Attendee>()
    @Volatile
    var lastSent: String? = null
    @Volatile
    var nameSent = false
    val status = MutableLiveData("Nothing")



    //move to bluetooth! (or delete)
    val scanMode = MutableLiveData(BluetoothAdapter.SCAN_MODE_NONE)
    var bluetoothPermission = false
    private var acceptThread: ProfessorBluetoothFragment.AcceptThread? = null
    private var connectedThreads = mutableListOf<ConnectedThread>() //TODO list of threads is never cleared!
    val studentsNumber = MutableLiveData(0)
    val message = MutableLiveData("")
    var previousName: String? = null
        set(value) {
            Log.i(T, "ProfessorViewModel: previousName = ${value}")
            field = value
        }
    //TODO don't need bluetoothPermission variable, can just check if bluetoothAdapter is null or not
    var bluetoothAdapter: BluetoothAdapter? = null

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

    fun stopConnection(t: ConnectedThread) {

    }

    fun stopServer() {
        this.acceptThread?.cancel()
        connectedThreads.forEach {
            it.cancel()
        }
//        this.connectedThreads = null
        this.acceptThread = null
    }

    @SuppressLint("MissingPermission")
    override fun onCleared() {
        super.onCleared()
        Log.d(T, "ProfessorViewModel: onClear()")
        if (bluetoothPermission) {
            if (previousName != null)
                bluetoothAdapter?.name = previousName
            previousName = null
        }
        stopServer()
    }
}