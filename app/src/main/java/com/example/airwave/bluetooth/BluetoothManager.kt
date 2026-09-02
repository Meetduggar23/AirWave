package com.example.airwave.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.UUID

class BluetoothManager(private val context: Context) {

    private val androidBtManager: android.bluetooth.BluetoothManager? by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        androidBtManager?.adapter
    }

    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())
    private val discoveredDevices = mutableMapOf<String, BluetoothDevice>()
    private val airwaveDevices = mutableMapOf<String, AirWaveDevice>()

    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var connectedSocket: BluetoothSocket? = null

    var onDeviceFound: ((AirWaveDevice) -> Unit)? = null
    var onDiscoveryFinished: (() -> Unit)? = null
    var onDiscoveryStarted: (() -> Unit)? = null
    var onConnectionStateChanged: ((ConnectionState) -> Unit)? = null
    var onMessageReceived: ((String) -> Unit)? = null
    var onConnectionRequest: ((String, String) -> Unit)? = null
    var onSocketReady: ((BluetoothSocket) -> Unit)? = null

    private var messageThread: MessageThread? = null

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    val isBluetoothAvailable: Boolean
        get() = bluetoothAdapter != null

    val deviceName: String
        @SuppressLint("MissingPermission")
        get() = bluetoothAdapter?.name ?: "Unknown Device"

    fun getState(): ConnectionState {
        return when {
            connectedSocket?.isConnected == true -> ConnectionState.CONNECTED
            clientSocket != null || serverSocket != null -> ConnectionState.CONNECTING
            else -> ConnectionState.DISCONNECTED
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (!isBluetoothEnabled || isScanning) return

        discoveredDevices.clear()
        airwaveDevices.clear()
        isScanning = true
        onDiscoveryStarted?.invoke()

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        }

        context.registerReceiver(discoveryReceiver, filter)
        bluetoothAdapter?.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        if (isScanning) {
            try {
                bluetoothAdapter?.cancelDiscovery()
            } catch (e: Exception) {
                // Ignore
            }
            try {
                context.unregisterReceiver(discoveryReceiver)
            } catch (e: Exception) {
                // Ignore
            }
            isScanning = false
        }
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let {
                        val address = it.address
                        if (!discoveredDevices.containsKey(address)) {
                            discoveredDevices[address] = it
                            val name = try { it.name } catch (e: SecurityException) { null }
                                ?: address.takeLast(5)
                            val airwaveDevice = AirWaveDevice(
                                name = name,
                                address = address,
                                device = it,
                                isAirWave = name.startsWith("AirWave") || true
                            )
                            airwaveDevices[address] = airwaveDevice
                            onDeviceFound?.invoke(airwaveDevice)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    isScanning = false
                    onDiscoveryFinished?.invoke()
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    isScanning = true
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startServer() {
        Thread {
            try {
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                    "AirWave",
                    AIRWAVE_UUID
                )
                onConnectionStateChanged?.invoke(ConnectionState.LISTENING)

                while (true) {
                    val socket = serverSocket?.accept() ?: break
                    connectedSocket = socket
                    onSocketReady?.invoke(socket)
                    onConnectionStateChanged?.invoke(ConnectionState.CONNECTED)
                    startListeningForMessages(socket)
                }
            } catch (e: Exception) {
                onConnectionStateChanged?.invoke(ConnectionState.FAILED)
            }
        }.start()
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        onConnectionStateChanged?.invoke(ConnectionState.CONNECTING)
        Thread {
            try {
                clientSocket = device.createRfcommSocketToServiceRecord(AIRWAVE_UUID)
                bluetoothAdapter?.cancelDiscovery()
                clientSocket?.connect()
                connectedSocket = clientSocket
                onConnectionStateChanged?.invoke(ConnectionState.CONNECTED)
                onSocketReady?.invoke(clientSocket!!)
                startListeningForMessages(clientSocket!!)
            } catch (e: Exception) {
                onConnectionStateChanged?.invoke(ConnectionState.FAILED)
                try { clientSocket?.close() } catch (e2: Exception) { }
                clientSocket = null
            }
        }.start()
    }

    private fun startListeningForMessages(socket: BluetoothSocket) {
        messageThread = MessageThread(socket, { message ->
            onMessageReceived?.invoke(message)
        }, {
            onConnectionStateChanged?.invoke(ConnectionState.DISCONNECTED)
        })
        messageThread?.start()
    }

    fun sendMessage(text: String): Boolean {
        val thread = messageThread ?: return false
        if (!thread.isAlive) return false
        thread.sendMessage(text)
        return true
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        messageThread?.cancel()
        messageThread = null
        try { connectedSocket?.close() } catch (e: Exception) { }
        try { clientSocket?.close() } catch (e: Exception) { }
        try { serverSocket?.close() } catch (e: Exception) { }
        connectedSocket = null
        clientSocket = null
        serverSocket = null
        onConnectionStateChanged?.invoke(ConnectionState.DISCONNECTED)
    }

    fun destroy() {
        disconnect()
        stopDiscovery()
    }

    data class AirWaveDevice(
        val name: String,
        val address: String,
        val device: BluetoothDevice,
        val isAirWave: Boolean = true
    )

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, FAILED, LISTENING
    }

    companion object {
        val AIRWAVE_UUID: UUID = UUID.fromString("fa87bc08-64ce-45e7-9c49-259b4791342f")
    }
}
