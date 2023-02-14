package com.vysotsky.attendance.professor.proximity

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.vysotsky.attendance.SERVICE_ID
import com.vysotsky.attendance.T
import com.vysotsky.attendance.databinding.FragmentProfessorWifiBinding
import com.vysotsky.attendance.englishQRRegex
import com.vysotsky.attendance.getName
import com.vysotsky.attendance.professor.ProfessorViewModel
import com.vysotsky.attendance.professor.attendeeList.Attendee
import com.vysotsky.attendance.util.Endpoint
import com.vysotsky.attendance.util.checkPermissions

/**
 * connect -> connectionLifecycleCallback.onConnectionInitiated
 * connectionLifecycleCallback.onConnectionInitiated -> accept
 */

class ProfessorProximityFragment : Fragment() {
    private var _binding: FragmentProfessorWifiBinding? = null
    private val binding: FragmentProfessorWifiBinding
        get() = _binding!!

    private val activityViewModel: ProfessorViewModel by activityViewModels()

    private val pendingConnections = mutableMapOf<String, Endpoint>()
    private val establishedConnections = mutableMapOf<String, Endpoint>()
    lateinit var connectionsClient: ConnectionsClient
    val userName = getName()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        val permissionsGranted = checkPermissions(requireActivity(), this, permissions)
        if (!permissionsGranted) {
            Toast.makeText(
                requireContext(),
                "Can't use wifi because no location permission!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfessorWifiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        registerService()
        connectionsClient = Nearby.getConnectionsClient(requireActivity())
        startAdvertising()
        subscribe()
    }

    override fun onStop() {
        super.onStop()
        connectionsClient.run {
            stopAdvertising()
        }
    }

    /** 2 main functions: to send to to handle received data */
    private fun send(endpoint: Endpoint, data: String) {
        connectionsClient.sendPayload(endpoint.id, Payload.fromBytes(data.toByteArray()))
            .addOnFailureListener { Log.d(T, "ProfessorWifiFragment, error sending data:", it) }
    }

    private fun handleReceive(endpoint: Endpoint, data: String) {
        if ("$data:null".matches(englishQRRegex)) {
            val (firstName, secondName, id) = data.split(":")
            val attendee = Attendee(firstName, secondName, id)
            if (activityViewModel.notInTheList(attendee)) {
                activityViewModel.addAttendeeToList(attendee)
                activityViewModel.studentsNumber.value = activityViewModel.studentsNumber.value!! + 1
                send(endpoint, "200")
            } else {
                send(endpoint, "402")
            }
        } else {
            Log.d(T, "ProfessorWifiFragment: ERROR! data doesn't match regex: ${data}")
            send(endpoint, "406")
        }
        //todo: disconnect!
    }


    private fun startAdvertising() {
        val advertisingOptions: AdvertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()
        Nearby.getConnectionsClient(requireContext())
            .startAdvertising(
                userName, SERVICE_ID, connectionLifecycleCallback, advertisingOptions
            )
            .addOnSuccessListener { Log.d(T, "now advertising: $userName")}
            .addOnFailureListener {
                Log.d(T, "startAdvertising() failed", it)
                //onAdvertisingFailed() //UI
            }
    }

    //on connection initiated needs to call accept connection
    private fun acceptConnection(endpoint: Endpoint) {
        Log.d(T, "AcceptConnection is called endpoint.id = ${endpoint.id}")
        connectionsClient.acceptConnection(endpoint.id, object: PayloadCallback() {
            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                Log.d(T, "onPayloadReceived(): payload = ${payload.asBytes()?.let { String(it) }}")
                handleReceive(endpoint, payload.asBytes()?.let { String(it) } ?: "")
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
        }).addOnFailureListener {
            Log.e(T, "acceptConnection() failed: ", it)
        }
    }

    private fun disconnectFromEndpoint(endpointId: String) {
        Log.d(T, "ProfessorWifiFragment: disconnectFromEndpoint() id = $endpointId")
        establishedConnections.remove(endpointId)
        //onEndpointDisconnected() //change UI
    }

    private fun connectedToEndpoint(endpoint: Endpoint) {
        Log.d(T, "connectedToEndpoint $endpoint, (saving in established...)")
        establishedConnections[endpoint.id] = endpoint
    }

    private val connectionLifecycleCallback = object :ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            val endpoint = Endpoint(endpointId, connectionInfo.endpointName)
            Log.d(T, "ProfessorWifiFragment: onConnectionInitiated(): $endpoint (saving to pending...)")
            pendingConnections[endpointId] = endpoint

            acceptConnection(endpoint)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Log.d(T, "onConnectionResult: id = $endpointId")
            if (!result.status.isSuccess) {
                Log.d(T, "Connection failed. Status = ${result.status}")
                //onConnectionFailed() //start discovering again
                return
            }
            if (pendingConnections.containsKey(endpointId))
                connectedToEndpoint(pendingConnections.remove(endpointId)!!)
        }

        override fun onDisconnected(endpointId: String) {
            //check if connection to this endpoint is already established
            if (establishedConnections.containsKey(endpointId)) {
                Log.d(T, "ProfessorWifiFragment Unexpected disconnection from endpoint = $endpointId")
            }
            disconnectFromEndpoint(endpointId)
        }
    }

    private fun subscribe() {
        activityViewModel.studentsNumber.observe(viewLifecycleOwner) {
            binding.studentCounter.text = it.toString()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}