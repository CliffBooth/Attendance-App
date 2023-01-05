package com.vysotsky.attendance.student.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import com.vysotsky.attendance.BLUETOOTH_UUID
import com.vysotsky.attendance.R
import com.vysotsky.attendance.T
import com.vysotsky.attendance.databinding.FragmentStudentBluetoothBinding
import com.vysotsky.attendance.util.ConnectedThread
import com.vysotsky.attendance.util.MESSAGE_CLOSE
import com.vysotsky.attendance.util.MESSAGE_READ
import com.vysotsky.attendance.util.MESSAGE_TOAST
import com.vysotsky.attendance.util.MESSAGE_WRITE
import com.vysotsky.attendance.util.ThreadHandler
import com.vysotsky.attendance.util.checkPermissions
import com.vysotsky.attendance.util.fromByteArray
import java.io.IOException
import java.lang.NullPointerException

//shouldn't perform discovery while connected
class StudentBluetoothFragment : Fragment() {
    private var _binding: FragmentStudentBluetoothBinding? = null
    private val binding: FragmentStudentBluetoothBinding
        get() = _binding!!

    private var bluetoothAdapter: BluetoothAdapter? = null
    private val devicesList = mutableListOf<String>()
    private lateinit var listViewAdapter: ArrayAdapter<String>

    //TODO won't work if multiple devices named ATTENDANCE APP, better implement adapter
    //or better yet simply use ArrayAdapter of Pair<> and pass list.map { pair -> "${pair.first} -- ${pair.second}" }
    private lateinit var professorDevice: BluetoothDevice
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null

    private lateinit var firstName: String
    private lateinit var secondName: String
    private lateinit var deviceID: String
    private lateinit var stringToSend: String

    //put devicesList and status text in the viewModel

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action.toString()) {
                BluetoothDevice.ACTION_FOUND -> {
                    Log.d(T, "StudentBluetoothFragment: ACTION_FOUND")
                    val extras = intent.extras
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) {
                        extras?.getParcelable(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    } else {
                        extras?.getParcelable(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (device != null) {
                        Log.d(T, "device = $device")
                        if (device.name == getString(R.string.bluetooth_name)) {
                            devicesList += "${device.name} --- ${device.address}"
                            professorDevice = device
                        }
                        listViewAdapter.notifyDataSetChanged()
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d(T, "StudentBluetoothFragment: ACTION_DISCOVERY_STARTED")
                    binding.statusText1.text = "Searching for ATTENDANCE APP..."
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(T, "StudentBluetoothFragment: ACTION_DISCOVERY_FINISHED")
                    if (binding.statusText1.text == "Searching for ATTENDANCE APP...")
                        binding.statusText1.text = "search is done"
                }
            }
        }
    }

    inner class StudentHandler : ThreadHandler() {

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_READ -> {
                    val message = String.fromByteArray(msg.obj as ByteArray, msg.arg1)
                    binding.statusText1.text = "response: $message"
                    Log.d(T, "Response: $message")
                    when (message) {
                        "200" -> {
                            binding.accountedStatus.text = "Accounted!"
                            binding.accountedStatus.setTextColor(Color.GREEN)
                        }
                        "202" -> {
                            binding.accountedStatus.setTextColor(Color.RED)
                            binding.accountedStatus.text = "You have already been scanned!"
                        }
                        "406" -> {
                            binding.accountedStatus.setTextColor(Color.RED)
                            binding.accountedStatus.text = "Some kind of an error has occurred..."
                        }
                    }
                    //all is done, cancel connection
                    disconnect()
                }

                MESSAGE_WRITE -> {
                    binding.statusText1.text = "message sent"
                    Log.d(T, "Handler: MESSAGE_WRITE")
                }

                MESSAGE_TOAST -> {

                }

                MESSAGE_CLOSE -> {
                    disconnect()

                    //this may happen when closing the fragment,
                    //received message when binding is already null
                    try {
                        binding.statusText1.text = "Disconnected"
                    } catch (_: NullPointerException) {
                    }

                    devicesList.clear()
                    listViewAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bluetoothManager: BluetoothManager =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.bluetooth_not_supported),
                Toast.LENGTH_LONG
            ).show()
        }

        //TODO move out to viewModel to share across fragments
        val sharedPreferences = requireActivity().getSharedPreferences(
            getString(R.string.preference_file_key),
            AppCompatActivity.MODE_PRIVATE
        )
        firstName = sharedPreferences.getString(getString(R.string.saved_first_name), null).toString()
        secondName = sharedPreferences.getString(getString(R.string.saved_second_name), null).toString()
        deviceID = requireActivity().intent.extras?.getString("id") ?: Settings.Secure.getString(
            requireActivity().applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        stringToSend = "$firstName:$secondName:$deviceID"

    }

    //TODO asks for permission every time when the screen is rotated
    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentBluetoothBinding.inflate(inflater, container, false)

        //NOT CHECKING ALREADY PAIRED DEVICES, BECAUSE WHY
        //discover
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        }
        requireActivity().registerReceiver(receiver, filter)
        Log.d(T, "StudentBluetoothFragment: receiver registered")
        val api31 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        Log.d(
            T,
            "StudentBluetoothFragment: Build.VERSION.SDK_INT >= Build.VERSION_CODES.S = $api31"
        )
        val granted = if (api31) {
            checkPermissions(
                requireActivity(),
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            ) {
                //maybe add some refresh button or something
                binding.errorText.text = "Bluetooth permission in not granted!"
                binding.errorText.setTextColor(Color.RED)
            }
        } else {
            checkPermissions(
                requireActivity(),
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            ) {
                //maybe add some refresh button or something
                binding.errorText.text = "Location permission in not granted!"
                binding.errorText.setTextColor(Color.RED)
            }
        }
        if (granted) {
            var enabled = bluetoothAdapter!!.isEnabled
            if (!enabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                val resultLauncher =
                    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                        if (result.resultCode != Activity.RESULT_OK) {
                            Toast.makeText(
                                requireContext(),
                                "Please, turn on bluetooth!",
                                Toast.LENGTH_LONG
                            ).show()
                            //TODO maybe quit the fragment?
                        } else {
                            doDiscovery()
                        }
                    }
                resultLauncher.launch(enableBtIntent)
            }
        }
        enableGPS()
        //setup list
        listViewAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            devicesList
        ) //* can use pair list
        binding.list.adapter = listViewAdapter
        //TODO list length
//        repeat(20) {
//            devicesList += "test"
//        }
        listViewAdapter.notifyDataSetChanged()
        binding.list.setOnItemClickListener { parent, view, position, id ->
            binding.statusText1.text = "Connecting..."
            connect()
        }

        binding.discoverButton.setOnClickListener {
            enableGPS()
            doDiscovery()
        }
        return binding.root
    }

    @SuppressLint("MissingPermission")
    private fun connect() {
        if (connectThread != null) {
            //TODO implement state in viewModel and set the status according to state
            binding.statusText1.text = "Already connected"
            return //??? should i do this?
        }
        val adapter = bluetoothAdapter!!
        listViewAdapter.notifyDataSetChanged()
        adapter.cancelDiscovery()
        connectThread?.cancel()
        connectThread = ConnectThread().apply {
            start()
        }
    }

    //TODO move the thread to viewModel
    override fun onStop() {
        super.onStop()
        connectThread?.cancel()
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
//            professorDevice.createRfcommSocketToServiceRecord(uuid)
            professorDevice.createInsecureRfcommSocketToServiceRecord(BLUETOOTH_UUID)
        }

        override fun run() {
            bluetoothAdapter?.cancelDiscovery()
            Log.d(T, "StudentBluetoothFragment: background thread: ${currentThread().name}")
            Log.d(T, "StudentBluetoothFragment: socket = ${mmSocket}")
            Log.d(T, "StudentBluetoothFragment: address = ${professorDevice.address}")
            if (mmSocket != null) {
                try {
                    mmSocket!!.connect()
                    sendData(mmSocket!!)
                } catch (e: IOException) {
                    Log.d(T, "ConnectThread: error trying to connect", e)
                    disconnect()
                    Handler(Looper.getMainLooper()).post {
                        binding.statusText1.text = "Disconnected"
                    }
                }
            }
        }

        fun cancel() {
            Log.d(T, "StudentBluetoothFragment: ConnectedThread cancel()")
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.d(T, "StudentBluetoothFragment: Could not close the client socket", e)
            }
        }
    }

    private fun sendData(socket: BluetoothSocket) {
        Log.d(T, "StudentBluetoothFragment: send data from thread: ${Thread.currentThread().name}")
        connectedThread = ConnectedThread(socket, StudentHandler())
        connectedThread!!.start()
        connectedThread!!.write(stringToSend.toByteArray())
        //        socket.outputStream.write("Hello".toByteArray())
//        socket.close()
    }

    private fun disconnect() {
        connectThread?.cancel()
        connectThread = null
        connectedThread?.cancel()
        connectedThread = null
    }


    private fun enableGPS() {
        val REQUEST_CHECK_SETTINGS = 1
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
            .setMinUpdateIntervalMillis(5000)
            .build()
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)
            .build()

        val client: SettingsClient = LocationServices.getSettingsClient(requireActivity())
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(settingsRequest)

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun doDiscovery() {
        val adapter = bluetoothAdapter!!
        devicesList.clear()
        listViewAdapter.notifyDataSetChanged()
        if (adapter.isDiscovering) {
            adapter.cancelDiscovery()
        }
        val result = adapter.startDiscovery()
        Log.d(T, "StudentBluetoothFragment: startDiscovery() = $result; state = ${adapter.state}")
//        binding.statusText1.text = "Searching for devices..."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(receiver)
        Log.d(T, "StudentBluetoothFragment: receiver UNregistered")
    }


}