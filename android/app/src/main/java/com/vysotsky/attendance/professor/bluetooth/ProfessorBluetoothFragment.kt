package com.vysotsky.attendance.professor.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import com.vysotsky.attendance.API_URL
import com.vysotsky.attendance.BLUETOOTH_UUID
import com.vysotsky.attendance.R
import com.vysotsky.attendance.T
import com.vysotsky.attendance.databinding.FragmentProfessorBluetoothBinding
import com.vysotsky.attendance.debug
import com.vysotsky.attendance.englishQRRegex
import com.vysotsky.attendance.httpClient
import com.vysotsky.attendance.professor.Attendee
import com.vysotsky.attendance.professor.ProfessorViewModel
import com.vysotsky.attendance.professor.Status
import com.vysotsky.attendance.util.ConnectedThread
import com.vysotsky.attendance.util.MESSAGE_CLOSE
import com.vysotsky.attendance.util.MESSAGE_READ
import com.vysotsky.attendance.util.MESSAGE_TOAST
import com.vysotsky.attendance.util.MESSAGE_WRITE
import com.vysotsky.attendance.util.ThreadHandler
import com.vysotsky.attendance.util.checkPermissions
import com.vysotsky.attendance.util.fromByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ProfessorBluetoothFragment : Fragment() {
    private var _binding: FragmentProfessorBluetoothBinding? = null
    private val binding: FragmentProfessorBluetoothBinding
        get() = _binding!!

//    private var bluetoothAdapter: BluetoothAdapter? = null
    private val viewModel: ProfessorViewModel by activityViewModels()
//    private lateinit var previousName: String
    private lateinit var email: String

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val c = requireContext()
        val sharedPreferences = c.getSharedPreferences(
            getString(R.string.preference_file_key),
            AppCompatActivity.MODE_PRIVATE
        )
        email = sharedPreferences.getString(c.getString(R.string.saved_email), "error") ?: "error"
        val bluetoothManager: BluetoothManager =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        viewModel.bluetoothAdapter = bluetoothManager.adapter
        viewModel.bluetoothPermission = if (Build.VERSION.SDK_INT >= 31) {
            checkPermissions(
                requireActivity(),
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
            ) {
                //maybe add some refresh button or something
                Toast.makeText(
                    requireContext(),
                    "Bluetooth permission is not granted!",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            true
        }
        if (viewModel.bluetoothAdapter == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.bluetooth_not_supported),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action.toString()) {
                BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> {
                    viewModel.scanMode.value = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_SCAN_MODE,
                        BluetoothAdapter.SCAN_MODE_NONE
                    )
                }
            }
        }
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(T, "ProfessorBluetoothFragment: launcher result: ${result.resultCode}")
            if (result.resultCode != Activity.RESULT_CANCELED) {
                viewModel.runServer(AcceptThread())
            }
        }

    inner class ProfessorHandler : ThreadHandler() {

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_READ -> {
                    val message = String.fromByteArray(msg.obj as ByteArray, msg.arg1)
                    if (debug) {
                        viewModel.message.value = message
                    }
//                    thisThread.write("Echo hello!".toByteArray())
                    handle(message, thisThread)
                }

                MESSAGE_WRITE -> {
                    Log.d(T, "Handler: MESSAGE_WRITE")
                }

                MESSAGE_TOAST -> {

                }
                MESSAGE_CLOSE -> {

                }
            }
        }
    }

    private fun handle(message: String, thread: ConnectedThread) {
        //send to the server
        if ("$message:null".matches(englishQRRegex)) {
            viewModel.viewModelScope.launch(Dispatchers.IO) {
//                val client = OkHttpClient()
                val json = "{\"email\":\"$email\", \"data\":\"$message\"}"
                val (firstName, secondName, _) = message.split(":")
                val body = json.toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url("$API_URL/bluetooth")
                    .post(body)
                    .build()
                try {
                    httpClient.newCall(request).execute().use { res ->
                        when (res.code) {
                            200 -> {
                                thread.write("200".toByteArray())
                                viewModel.attendeesList += Attendee(
                                    firstName,
                                    secondName,
                                    Status.OK
                                )
                                Handler(Looper.getMainLooper()).post {
                                    viewModel.attendeesList.notifyDataSetChanged()
                                    viewModel.studentsNumber.value =
                                        viewModel.studentsNumber.value!! + 1
                                }
                            }

                            202 -> {
                                thread.write("202".toByteArray())
                            }

                            406 -> {
                                thread.write("406".toByteArray())
                            }

                            else -> Unit
                        }
                    }
                } catch (e: IOException) {
                    //network error
                    //TODO handle network error, something like "send again button"
                    Handler(Looper.getMainLooper()).post {
                        viewModel.intnetErrorMessageVisibility.value = View.VISIBLE
                    }
                }
            }
        } else {
            Log.d(T, "ProfessorBluetoothFragment: ERROR! data doesn't match regex: ${message}")
            Log.d(T, "ProfessorBluetoothFragment: message.length = ${message.length}")
            thread.write("406".toByteArray())
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfessorBluetoothBinding.inflate(layoutInflater, container, false)

        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600)
        }

        requireActivity().registerReceiver(
            receiver,
            IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        )
        binding.makeDiscoverableButton.setOnClickListener {
            if (!viewModel.bluetoothPermission) {
                Toast.makeText(
                    requireContext(),
                    "Bluetooth permission is not granted!",
                    Toast.LENGTH_LONG
                ).show()
            }
            if (viewModel.bluetoothPermission) {
//                previousName = bluetoothAdapter!!.name
                if (viewModel.previousName == null) {
                    viewModel.previousName = viewModel.bluetoothAdapter!!.name
                    viewModel.bluetoothAdapter!!.name = getString(R.string.bluetooth_name)
                }
                Log.d(T, "ProfessorBluetoothFragment previous name = ${viewModel.previousName}")
                launcher.launch(discoverableIntent)
            }
        }
        subscribe()
        return binding.root
    }

    //make object?
    @SuppressLint("MissingPermission")
    inner class AcceptThread : Thread() {
        private val NAME = "Attendance app"

        @Volatile
        private var shouldAccept = true
        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            viewModel.bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(NAME, BLUETOOTH_UUID)
        }

        override fun run() {
            Log.d(T, "ProfessorBluetoothFragment: AcceptedThread: run()")
            while (shouldAccept) {
                var socket: BluetoothSocket? = null
                try {
                    socket = mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(T, "ProfessorBluetoothFragment: accept() method failed", e)
                    shouldAccept = false
                }
                if (socket != null) {
                    val connectedThread = ConnectedThread(socket, ProfessorHandler())
                    viewModel.runConnectedThread(connectedThread)
                }
            }
        }

        fun cancel() {
            try {
                shouldAccept = false
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(T, "ProfessorBluetoothFragment: error closing socket", e)
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun subscribe() {
        viewModel.scanMode.observe(viewLifecycleOwner) {
            Log.d(T, "ProfessorBluetoothFragment: observe: bluetoothAdapter.name=${viewModel.bluetoothAdapter?.name}")
            when (it) {
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> {
                    binding.modeText.text = "Device is visible =)"
                    binding.modeText.setTextColor(Color.GREEN)
                    binding.makeDiscoverableButton.isVisible = false
                }

                else -> {
                    binding.modeText.text = "Device is not visible =("
                    binding.modeText.setTextColor(Color.RED)
                    binding.makeDiscoverableButton.isVisible = true
                    if (viewModel.bluetoothPermission && viewModel.bluetoothAdapter != null) {
                        if (viewModel.previousName != null)
                            viewModel.bluetoothAdapter?.name = viewModel.previousName
                        //should we null previousName ?
                        //if yes, then need to set it every time you click "Discover"
                        //
                    }
                    viewModel.stopServer()
                }
            }
        }
        viewModel.intnetErrorMessageVisibility.observe(viewLifecycleOwner) {
            when (it) {
                View.VISIBLE -> {
                    Toast.makeText(requireContext(), "Internet error!", Toast.LENGTH_LONG).show()
                }
            }
        }
        viewModel.studentsNumber.observe(viewLifecycleOwner) {
            //TODO use pattern string
            binding.studentNumber.text = it.toString()
        }
        viewModel.message.observe(viewLifecycleOwner) {
            binding.messageTextView.text = it
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
    }

}