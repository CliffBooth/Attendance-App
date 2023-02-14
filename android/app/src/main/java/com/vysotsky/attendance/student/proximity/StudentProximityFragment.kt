package com.vysotsky.attendance.student.proximity

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.vysotsky.attendance.SERVICE_ID
import com.vysotsky.attendance.T
import com.vysotsky.attendance.databinding.FragmentStudentWifiBinding
import com.vysotsky.attendance.getName
import com.vysotsky.attendance.student.StudentViewModel
import com.vysotsky.attendance.util.Endpoint

class StudentProximityFragment : Fragment() {
    private var _binding: FragmentStudentWifiBinding? = null
    private val binding
        get() = _binding!!
    val viewModel: StudentProximityViewModel by viewModels()
    private val activityViewModel: StudentViewModel by activityViewModels()

    lateinit var adapter: ArrayAdapter<String>

    private lateinit var connectionsClient: ConnectionsClient
    private val devicesList = mutableListOf<String>()
    private lateinit var lastDiscoveredEndpoint: Endpoint
    private val pendingConnections = mutableMapOf<String, Endpoint>()
    private val establishedConnections = mutableMapOf<String, Endpoint>()
    private val userName = getName()
    private lateinit var stringToSend: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stringToSend = "${activityViewModel.firstName}:${activityViewModel.secondName}:${activityViewModel.deviceID}"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentWifiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, devicesList)
        binding.devicesList.adapter = adapter
        binding.scanButton.setOnClickListener {
            startDiscovering()
        }
        binding.devicesList.setOnItemClickListener { parent, view, position, id ->
            connectToEndpoint(lastDiscoveredEndpoint)
        }
        subscribe()
        connectionsClient = Nearby.getConnectionsClient(requireActivity())
    }

    override fun onStop() {
        super.onStop()
        connectionsClient.run {
            stopDiscovery()
        }
    }

    // connections api
    private fun handleReceive(endpoint: Endpoint, data: String) {
        when (data) {
            "200" -> {
                viewModel.accountedStatus.value = StudentProximityViewModel.AccountedStatus.OK
            }

            "202" -> {
                viewModel.accountedStatus.value =
                    StudentProximityViewModel.AccountedStatus.ALREADY_SCANNED
            }

            "406" -> {
                viewModel.accountedStatus.value = StudentProximityViewModel.AccountedStatus.ERROR
            }
        }
            //DISCONNECT!!! (read the docs)
    }

    private fun startDiscovering() {
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()
        connectionsClient.startDiscovery(SERVICE_ID, discoveryCallback, discoveryOptions)
            .addOnSuccessListener {
                Log.d(T, "StudentWifiFragment discovery success")
            }
            .addOnFailureListener { e ->
                Log.d(T, "StudentWifiFragment discovery failure ", e)
            }

    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(T, "StudentWifiFragment: onEndpointFound: id = $endpointId, info.name = ${info.endpointName}")
            if (SERVICE_ID == info.serviceId) {
                //display in the list
                val str = "${info.endpointName} ${info.serviceId}"
                if (!devicesList.contains(str)) {
                    devicesList += str
                    lastDiscoveredEndpoint = Endpoint(endpointId, info.endpointName)
                    adapter.notifyDataSetChanged()
                }
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(T, "StudentWifiFragment: onEndpointLost: id = $endpointId")
        }
    }

    private fun connectToEndpoint(endpoint: Endpoint) {
        Log.d(T, "sending a connection request to endpoint = $endpoint")
        connectionsClient.requestConnection(userName, endpoint.id, connectionLifeCycleCallback)
            .addOnFailureListener { e: Exception ->
                Log.d(T, "requestConnection() failed: ", e)
            }
    }

    private fun onConnectionInitiated(endpoint: Endpoint) {
        connectionsClient.acceptConnection(endpoint.id, object : PayloadCallback() {
            override fun onPayloadReceived(id: String, payload: Payload) {
                val data = payload.asBytes()?.let {String(it)} ?: ""
                Log.d(T, "StudentWifiFragment: onPayloadReceived() payload = " +
                        "${data}")
                handleReceive(endpoint, data)
            }

            override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                /**
                 * 1 - success, 3 - in_progress, 2 - failure, 4 - cancelled
                 */
                if (update.status == 2 || update.status == 4) {
                    val strStatus = when (update.status) {
                        2 -> "status = ${update.status} (failure)"
                        4 -> "status = ${update.status} (cancelled)"
                        else -> ""
                    }
                    Log.d(T, "PorfessorWifiFragment: onPayloadUpdate() id = $endpointId $strStatus")
                }
            }
        })
    }

    private fun send(endpoint: Endpoint, data: String) {
        connectionsClient.sendPayload(endpoint.id, Payload.fromBytes(data.toByteArray()))
            .addOnFailureListener { Log.d(T, "error sending data: ", it) }
    }

    private val connectionLifeCycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            val endpoint = Endpoint(endpointId, connectionInfo.endpointName)
            Log.d(T, "ProfessorWifiFragment: onConnectionInitiated(): $endpoint (saving to pending...)")
            pendingConnections[endpointId] = endpoint
            //nConnectionInitiated(endpoint, connectionInfo) //update ui or something
            Log.d(T, "sending data endpoint.id = ${endpoint.id}")
            onConnectionInitiated(endpoint)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Log.d(T, "StudentWifiFragment onConnectionResult() id = $endpointId")
            if (!result.status.isSuccess) {
                Log.d(T, "Connection failed. Status = ${result.status}")
                //onConnectionFailed() //start discovering again
                return
            }
            if (pendingConnections.containsKey(endpointId))
                connectedToEndpoint(pendingConnections.remove(endpointId)!!)
        }

        private fun connectedToEndpoint(endpoint: Endpoint) {
            Log.d(T, "connectedToEndpoint $endpoint, (saving in established...)")
            establishedConnections[endpoint.id] = endpoint
            send(endpoint, stringToSend)
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(T, "StudentWifiFragment onDisconnected()")
        }
    }

    private fun subscribe() {
        viewModel.accountedStatus.observe(viewLifecycleOwner) {
            when (it) {
                StudentProximityViewModel.AccountedStatus.NONE -> {
                    binding.accountedStatus.text = ""
                }
                StudentProximityViewModel.AccountedStatus.OK -> {
                    binding.accountedStatus.text = "Accounted!"
                    binding.accountedStatus.setTextColor(Color.GREEN)
                }
                StudentProximityViewModel.AccountedStatus.ALREADY_SCANNED -> {
                    binding.accountedStatus.setTextColor(Color.YELLOW)
                    binding.accountedStatus.text = "You have already been scanned!"
                }
                StudentProximityViewModel.AccountedStatus.ERROR -> {
                    binding.accountedStatus.setTextColor(Color.RED)
                    binding.accountedStatus.text = "Error occurred, try again"
                }
                else -> Unit //to get rid of warning
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}