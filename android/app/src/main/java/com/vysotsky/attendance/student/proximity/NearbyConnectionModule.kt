package com.vysotsky.attendance.student.proximity

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Strategy
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.util.Endpoint

object NearbyConnectionModule {
    fun start(context: Context) {
        val connectionsClient = Nearby.getConnectionsClient(context)
    }

    fun stop() {
        val connection = Connection.Builder()
            .onStartAdvertising {
                Log.d("myTag", "stop: Hello world!!!")
            }
            .build()
    }
}

class Connection private constructor(
    val onStartAdvertising: (() -> Unit)?,
    val onAcceptConnection: (() -> Unit)?,

) {

    class Builder {
        private lateinit var onStartAdvertising: () -> Unit
        private lateinit var onAcceptConnection: (() -> Unit)

        fun build(): Connection {
            return Connection(
                onStartAdvertising,
                onAcceptConnection
            )
        }

        fun onStartAdvertising(f: () -> Unit): Builder {
            this.onStartAdvertising = f
            return this
        }

        fun onAcceptConnection(f: () -> Unit): Builder {
            this.onAcceptConnection = f
            return this
        }
    }

    /**
     * maybe it accepts things like onAdd callback
     */
    lateinit var connectionsClient: ConnectionsClient
    private val pendingConnections = mutableMapOf<String, Endpoint>()
    private val establishedConnections = mutableMapOf<String, Endpoint>()

    fun start(context: Context, userName: String, SERVICE_ID: String) {
        connectionsClient = Nearby.getConnectionsClient(context)
        val advertisingOptions: AdvertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()
//        connectionsClient.startAdvertising(
//            userName
//        )
    }

//    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
//        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
//            val endpoint = Endpoint(endpointId, connectionInfo.endpointName)
//            Log.d(TAG, "ProfessorWifiFragment: onConnectionInitiated(): $endpoint (saving to pending...)")
//            pendingConnections[endpointId] = endpoint
//
//            this@Connection.onAcceptConnection(endpoint)
//        }
//
//        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
//            Log.d(TAG, "onConnectionResult: id = $endpointId")
//            if (!result.status.isSuccess) {
//                Log.d(TAG, "Connection failed. Status = ${result.status}")
//                //onConnectionFailed() //start discovering again
//                return
//            }
//            if (pendingConnections.containsKey(endpointId))
//                connectedToEndpoint(pendingConnections.remove(endpointId)!!)
//        }
//
//        override fun onDisconnected(endpointId: String) {
//            //check if connection to this endpoint is already established
//            if (establishedConnections.containsKey(endpointId)) {
//                Log.d(TAG, "ProfessorWifiFragment Unexpected disconnection from endpoint = $endpointId")
//            }
//            disconnectFromEndpoint(endpointId)
//        }
//    }
}