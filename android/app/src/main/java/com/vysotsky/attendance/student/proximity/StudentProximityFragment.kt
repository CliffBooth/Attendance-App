package com.vysotsky.attendance.student.proximity

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
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
import com.vysotsky.attendance.R
import com.vysotsky.attendance.SERVICE_ID
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.databinding.FragmentStudentWifiBinding
import com.vysotsky.attendance.getName
import com.vysotsky.attendance.student.StudentViewModel
import com.vysotsky.attendance.util.Endpoint
import com.vysotsky.attendance.util.checkPermissions

//TODO: doesn't work if android.permission.NEARBY_WIFI_DEVICES not allowed, and there is no question for taht!!
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
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        val permissions31 = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
        val permissions33 = arrayOf(
            Manifest.permission.NEARBY_WIFI_DEVICES,
        )

        var permissionsGranted = checkPermissions(requireActivity(), this, permissions) {
            Log.d(
                TAG,
                "ProfessorProximityFragment permissions are ot granted!"
            )}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsGranted = permissionsGranted || checkPermissions(requireActivity(), this, permissions31) {
                Log.d(
                    TAG,
                    "ProfessorProximityFragment permissions31 are ot granted!"
                )}
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsGranted =
                permissionsGranted || checkPermissions(requireActivity(), this, permissions33) {
                    Log.d(
                        TAG,
                        "ProfessorProximityFragment permissions33 are ot granted!"
                    )}
        }
        Log.d(TAG, "StudentProximityFragment onCreate()  permissionsGranted = $permissionsGranted")
        if (!permissionsGranted) {
            Toast.makeText(
                requireContext(),
                getString(R.string.no_permissions),
                Toast.LENGTH_LONG
            ).show()
        }
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
        connectionsClient.disconnectFromEndpoint(endpoint.id)
        Log.d(TAG, "StudentProximityFragment handleReceive() called disconnect")
        establishedConnections.remove(endpoint.id)
    }

    private fun startDiscovering() {
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()
        viewModel.pbVisibility.value = true
        connectionsClient.startDiscovery(SERVICE_ID, discoveryCallback, discoveryOptions)
            .addOnSuccessListener {
                Log.d(TAG, "StudentWifiFragment started discovering...")
            }
            .addOnFailureListener { e ->
                if (!checkIfGpsEnabled() && context != null)
                    Toast.makeText(context, getString(R.string.please_enable_gps), Toast.LENGTH_LONG).show()
                Log.d(TAG, "StudentWifiFragment start discovery failure ", e)
                viewModel.pbVisibility.value = false
            }

    }

    private fun checkIfGpsEnabled(): Boolean {
        if (context == null)
            return false
        val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return try {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            false
        }

    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "StudentWifiFragment: onEndpointFound: id = $endpointId, info.name = ${info.endpointName}")
            if (SERVICE_ID == info.serviceId) {
                viewModel.pbVisibility.value = false
                //display in the list
                val str = "${info.serviceId}"
                if (!devicesList.contains(str)) {
                    devicesList += str
                    lastDiscoveredEndpoint = Endpoint(endpointId, info.endpointName)
                    adapter.notifyDataSetChanged()
                }
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "StudentWifiFragment: onEndpointLost: id = $endpointId")
            //TODO: delete from list
        }
    }

    private fun connectToEndpoint(endpoint: Endpoint) {
        viewModel.pbVisibility.value = true
        Log.d(TAG, "sending a connection request to endpoint = $endpoint")
        connectionsClient.requestConnection(userName, endpoint.id, connectionLifeCycleCallback)
            .addOnFailureListener { e: Exception ->
            viewModel.pbVisibility.value = false
            Log.d(TAG, "requestConnection() failed: ", e)
            }
    }

    private fun onConnectionInitiated(endpoint: Endpoint) {
        connectionsClient.acceptConnection(endpoint.id, object : PayloadCallback() {
            override fun onPayloadReceived(id: String, payload: Payload) {
                val data = payload.asBytes()?.let {String(it)} ?: ""
                Log.d(TAG, "StudentWifiFragment: onPayloadReceived() payload = " +
                        "${data}")
                handleReceive(endpoint, data)
            }

            override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                /**
                 * 1 - success, 3 - in_progress, 2 - failure, 4 - cancelled
                 */
                if (update.status == 2 || update.status == 4) {
                    viewModel.pbVisibility.value = false
                    val strStatus = when (update.status) {
                        2 -> "status = ${update.status} (failure)"
                        4 -> "status = ${update.status} (cancelled)"
                        else -> ""
                    }
                    Log.d(TAG, "StudentProximityFragment: onPayloadUpdate() id = $endpointId $strStatus")
                }
            }
        })
    }

    private fun send(endpoint: Endpoint, data: String) {
        connectionsClient.sendPayload(endpoint.id, Payload.fromBytes(data.toByteArray()))
            .addOnFailureListener { Log.d(TAG, "error sending data: ", it) }
    }

    private val connectionLifeCycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            val endpoint = Endpoint(endpointId, connectionInfo.endpointName)
            Log.d(TAG, "StudentProximityFragment: onConnectionInitiated(): $endpoint (saving to pending...)")
            pendingConnections[endpointId] = endpoint
            //nConnectionInitiated(endpoint, connectionInfo) //update ui or something
            Log.d(TAG, "sending data endpoint.id = ${endpoint.id}")
            onConnectionInitiated(endpoint)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Log.d(TAG, "StudentWifiFragment onConnectionResult() id = $endpointId")
            if (!result.status.isSuccess) {
                Log.d(TAG, "Connection failed. Status = ${result.status}")
                //onConnectionFailed() //start discovering again
                return
            }
            if (pendingConnections.containsKey(endpointId))
                connectedToEndpoint(pendingConnections.remove(endpointId)!!)
        }

        private fun connectedToEndpoint(endpoint: Endpoint) {
            Log.d(TAG, "connectedToEndpoint $endpoint, (saving in established...)")
            establishedConnections[endpoint.id] = endpoint
            send(endpoint, stringToSend)
        }

        override fun onDisconnected(endpointId: String) {
            viewModel.pbVisibility.value = false
            Log.d(TAG, "StudentWifiFragment onDisconnected()")
        }
    }

    private fun subscribe() {
        viewModel.accountedStatus.observe(viewLifecycleOwner) {
            when (it) {
                StudentProximityViewModel.AccountedStatus.NONE -> {
                    binding.accountedStatus.text = ""
                }
                StudentProximityViewModel.AccountedStatus.OK -> {
                    binding.accountedStatus.text = getString(R.string.accounted)
                    binding.accountedStatus.setTextColor(Color.GREEN)
                }
                StudentProximityViewModel.AccountedStatus.ALREADY_SCANNED -> {
                    binding.accountedStatus.setTextColor(Color.BLACK)
                    binding.accountedStatus.text = getString(R.string.you_have_already_been_scanned)
                }
                StudentProximityViewModel.AccountedStatus.ERROR -> {
                    binding.accountedStatus.setTextColor(Color.RED)
                    binding.accountedStatus.text = getString(R.string.error_occurred_try_again)
                }
//                else -> Unit
            }
        }

        viewModel.pbVisibility.observe(viewLifecycleOwner) {
            binding.pb.isVisible = it
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}