package com.vysotsky.attendance.professor.bluetooth

import android.Manifest
import android.annotation.SuppressLint
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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.vysotsky.attendance.BLUETOOTH_UUID
import com.vysotsky.attendance.R
import com.vysotsky.attendance.T
import com.vysotsky.attendance.databinding.FragmentProfessorBluetoothBinding
import com.vysotsky.attendance.professor.ProfessorViewModel
import com.vysotsky.attendance.util.ConnectedThread
import com.vysotsky.attendance.util.checkPermissions
import java.io.IOException

class ProfessorBluetoothFragment : Fragment() {
    private var _binding: FragmentProfessorBluetoothBinding? = null
    private val binding: FragmentProfessorBluetoothBinding
        get() = _binding!!

    private var bluetoothAdapter: BluetoothAdapter? = null
    private val viewModel: ProfessorViewModel by activityViewModels()
    private lateinit var previousName: String
    private lateinit var acceptThread: AcceptThread
    private val connectedThreads = mutableListOf<ConnectedThread>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bluetoothManager: BluetoothManager =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
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
        if (bluetoothAdapter == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.bluetooth_not_supported),
                Toast.LENGTH_LONG
            ).show()
        } else {
            if (viewModel.bluetoothPermission)
                previousName = bluetoothAdapter!!.name
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
        }

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfessorBluetoothBinding.inflate(layoutInflater, container, false)

        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3000)
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
                Log.d(T, "ProfessorBluetoothFragment previous name = ${previousName}")
                bluetoothAdapter!!.name = getString(R.string.bluetooth_name)
                launcher.launch(discoverableIntent)
            }
        }
        subscribe()
        return binding.root
    }

    //TODO attach thread to viewModel
    override fun onStart() {
        super.onStart()
        acceptThread = AcceptThread()
        acceptThread.start()
    }

    override fun onStop() {
        super.onStop()
        acceptThread.cancel()
        connectedThreads.forEach {
            it.cancel()
        }
    }

    //make object?
    @SuppressLint("MissingPermission")
    private inner class AcceptThread : Thread() {
        private val NAME = "Attendance app"
        @Volatile
        private var shouldAccept = true
        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(NAME, BLUETOOTH_UUID)
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
                    val connectedThread = ConnectedThread(socket) { message ->
                        Log.d(T, "ProfessorBluetoothFragment: received message: $message")
                        requireActivity().runOnUiThread {
                            binding.messageTextView.text = message
                        }
                    }
                    connectedThreads += connectedThread
                    connectedThread.start()
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
                    if (viewModel.bluetoothPermission)
                        bluetoothAdapter?.name = previousName
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        if (viewModel.bluetoothPermission) {
            Log.d(T, "ProfessorBluetoothFragment: onDestroy renaming to $previousName")
            bluetoothAdapter!!.name = previousName
        }
    }

}