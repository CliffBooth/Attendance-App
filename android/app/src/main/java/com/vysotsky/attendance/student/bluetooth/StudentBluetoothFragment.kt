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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
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
import com.vysotsky.attendance.student.StudentViewModel
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

class StudentBluetoothFragment : Fragment() {
    private var _binding: FragmentStudentBluetoothBinding? = null
    private val binding: FragmentStudentBluetoothBinding
        get() = _binding!!

    private var bluetoothAdapter: BluetoothAdapter? = null
//    private val devicesList = mutableListOf<BluetoothDevice>()
    private lateinit var listViewAdapter: DevicesListAdapter
    private val fragmentViewModel: BluetoothViewModel by viewModels()
    private val activityViewModel: StudentViewModel by activityViewModels()

    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null

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
                            if (fragmentViewModel.devicesList.all { d -> d.address != device.address }) {
                                fragmentViewModel.devicesList += device
                                listViewAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d(T, "StudentBluetoothFragment: ACTION_DISCOVERY_STARTED")
                    fragmentViewModel.connectionStatus.value = ConnectionStatus.SEARCHING
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(T, "StudentBluetoothFragment: ACTION_DISCOVERY_FINISHED")
                    //when you click on a device, status should say "Connecting", not "search is done"
                    if (fragmentViewModel.connectionStatus.value == ConnectionStatus.SEARCHING)
                        fragmentViewModel.connectionStatus.value = ConnectionStatus.SEARCH_DONE
                }
            }
        }
    }

    inner class StudentHandler : ThreadHandler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_READ -> {
                    val message = String.fromByteArray(msg.obj as ByteArray, msg.arg1)
                    //binding.statusText1.text = "response: $message"
                    fragmentViewModel.connectionStatus.value = ConnectionStatus.GOT_RESPONSE
                    Log.d(T, "Response: $message")
                    when (message) {
                        "200" -> {
                            fragmentViewModel.accountedStatus.value = AccountedStatus.OK
                        }
                        "202" -> {
                            fragmentViewModel.accountedStatus.value = AccountedStatus.ALREADY_SCANNED
                        }
                        "406" -> {
                            fragmentViewModel.accountedStatus.value = AccountedStatus.ERROR
                        }
                    }
                    //all is done, cancel connection
                    disconnect()
                }

                MESSAGE_WRITE -> {
                    fragmentViewModel.connectionStatus.value = ConnectionStatus.MESSAGE_SENT
                    Log.d(T, "Handler: MESSAGE_WRITE")
                }

                MESSAGE_TOAST -> {

                }

                MESSAGE_CLOSE -> {
                    disconnect()

                    //this may happen when closing the fragment,
                    //received message when binding is already null
                    try {
                        fragmentViewModel.connectionStatus.value = ConnectionStatus.DISCONNECTED
                    } catch (_: NullPointerException) {
                    }

                    fragmentViewModel.devicesList.clear()
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
        stringToSend = "${activityViewModel.firstName}:${activityViewModel.secondName}:${activityViewModel.deviceID}"

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
        listViewAdapter = DevicesListAdapter(
            requireContext(),
            fragmentViewModel.devicesList
        )
//        val testList = mutableListOf<String>()
//        val testAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, testList)
//        repeat(20) {
//            testList += "test"
//        }
        binding.list.adapter = listViewAdapter
        listViewAdapter.notifyDataSetChanged()
        binding.list.setOnItemClickListener { parent, view, position, id ->
            fragmentViewModel.connectionStatus.value = ConnectionStatus.CONNECTING
            Log.d(T, "StudentBluetoothFragment: position = $position")
            connect(fragmentViewModel.devicesList[position])
        }

        binding.discoverButton.setOnClickListener {
            enableGPS()
            doDiscovery()
        }
        subscribe()
        return binding.root
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        if (connectThread != null) {
            //TODO implement state in viewModel and set the status according to state
            fragmentViewModel.connectionStatus.value = ConnectionStatus.ALREADY_CONNECTED
            return //??? should i do this?
        }
        val adapter = bluetoothAdapter!!
        listViewAdapter.notifyDataSetChanged()
        adapter.cancelDiscovery()
        connectThread?.cancel()
        connectThread = ConnectThread(device).apply {
            start()
        }
    }

    //TODO move the thread to viewModel
    override fun onStop() {
        super.onStop()
        connectThread?.cancel()
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(private val professorDevice: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
//            professorDevice.createRfcommSocketToServiceRecord(uuid)
            professorDevice.createInsecureRfcommSocketToServiceRecord(BLUETOOTH_UUID)
        }

        override fun run() {
            bluetoothAdapter?.cancelDiscovery()
            Log.d(T, "StudentBluetoothFragment: background thread: ${currentThread().name}")
            Log.d(T, "StudentBluetoothFragment: address = ${professorDevice.address}")
            if (mmSocket != null) {
                try {
                    mmSocket!!.connect()
                    sendData(mmSocket!!)
                } catch (e: IOException) {
                    Log.d(T, "ConnectThread: error trying to connect", e)
                    disconnect()
                    Handler(Looper.getMainLooper()).post {
                        fragmentViewModel.connectionStatus.value = ConnectionStatus.DISCONNECTED
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
                        1
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
        fragmentViewModel.devicesList.clear()
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

    private fun subscribe() {
        fragmentViewModel.connectionStatus.observe(viewLifecycleOwner) {
            when(it) {
                ConnectionStatus.NONE -> {
                    binding.statusText1.text = ""
                }
                ConnectionStatus.SEARCHING -> {
                    binding.statusText1.text = "Searching for ATTENDANCE APP..."
                }
                ConnectionStatus.SEARCH_DONE -> {
                    binding.statusText1.text = "Search is done"
                }
                ConnectionStatus.CONNECTING -> {
                    binding.statusText1.text = "Connecting..."
                }
                ConnectionStatus.ALREADY_CONNECTED -> {
                    binding.statusText1.text = "Already connected"
                }
                ConnectionStatus.GOT_RESPONSE -> {
                    binding.statusText1.text = "Received response..."
                }
                ConnectionStatus.DISCONNECTED -> {
                    binding.statusText1.text = "Disconnected"
                }
                ConnectionStatus.MESSAGE_SENT -> {
                    binding.statusText1.text = "message sent"
                }
            }
        }

        fragmentViewModel.accountedStatus.observe(viewLifecycleOwner) {
            when (it) {
                AccountedStatus.NONE -> {
                    binding.accountedStatus.text = ""
                }
                AccountedStatus.OK -> {
                    binding.accountedStatus.text = "Accounted!"
                    binding.accountedStatus.setTextColor(Color.GREEN)
                }
                AccountedStatus.ALREADY_SCANNED -> {
                    binding.accountedStatus.setTextColor(Color.YELLOW)
                    binding.accountedStatus.text = "You have already been scanned!"
                }
                AccountedStatus.ERROR -> {
                    binding.accountedStatus.setTextColor(Color.RED)
                    binding.accountedStatus.text = "Error occurred, try again"
                }
                else -> Unit //to get rid of warning
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(receiver)
        Log.d(T, "StudentBluetoothFragment: receiver UNregistered")
    }


}