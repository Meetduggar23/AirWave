package com.example.airwave.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.airwave.model.AirWaveUser
import com.example.airwave.model.ChatMessage
import com.example.airwave.util.PreferencesHelper
import java.util.UUID

/**
 * AirWave Bluetooth controller.
 *
 * This is the single owner of the active Bluetooth session:
 *  - it keeps one RFCOMM server socket listening so nearby AirWave users can
 *    find this device and request a chat,
 *  - it performs an AirWave handshake on every socket so only AirWave apps
 *    can talk to each other,
 *  - it holds chat messages **in memory only** for the duration of the
 *    session and clears them whenever the session ends.
 *
 * All observable state is exposed as LiveData on the main thread.
 */
class BluetoothManager private constructor(private val appContext: Context) {

    // ---------------- Observable state ----------------

    private val _connectionState = MutableLiveData(ConnectionState.DISCONNECTED)
    val connectionState: LiveData<ConnectionState> = _connectionState

    private val _peerName = MutableLiveData<String?>(null)
    val peerName: LiveData<String?> = _peerName

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _discoveredUsers = MutableLiveData<List<AirWaveUser>>(emptyList())
    val discoveredUsers: LiveData<List<AirWaveUser>> = _discoveredUsers

    private val _isScanning = MutableLiveData(false)
    val isScanning: LiveData<Boolean> = _isScanning

    /** Set when this device received a chat request that is waiting for a decision. */
    private val _incomingRequest = MutableLiveData<IncomingRequest?>(null)
    val incomingRequest: LiveData<IncomingRequest?> = _incomingRequest

    /**
     * Set once when a session that the user was part of ends unexpectedly
     * (peer out of range, peer declined, peer closed the app). Chat observes
     * this to decide how to phrase the "connection lost" UI.
     */
    private val _unexpectedEnd = MutableLiveData<UnexpectedEnd?>(null)
    val unexpectedEnd: LiveData<UnexpectedEnd?> = _unexpectedEnd

    // ---------------- Internal state ----------------

    private val androidBtManager: android.bluetooth.BluetoothManager? by lazy {
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy { androidBtManager?.adapter }

    private val handler = Handler(Looper.getMainLooper())

    /** Guards message list updates so concurrent appends can never be lost. */
    private val messagesLock = Any()

    private var serverSocket: BluetoothServerSocket? = null

    /** The accept loop thread. A fresh thread is created on every startListening(). */
    private var acceptThread: Thread? = null

    private var sessionSocket: BluetoothSocket? = null
    private var messageThread: MessageThread? = null

    @Volatile
    private var isServerRunning = false

    /** true when this side initiated the connection (client role). */
    @Volatile
    private var initiatedByUs = false

    /** true once both sides finished the handshake (session usable). */
    @Volatile
    private var sessionReady = false

    /** address we dialed (client role). Empty for the accepting side. */
    private var dialedAddress: String = ""

    @Volatile
    private var receiverRegistered = false

    /** The device's Bluetooth name before AirWave renamed it, so it can be restored. */
    @Volatile
    private var originalDeviceName: String? = null

    /** Name of the pending incoming peer (peerName and per-request state). */
    private var pendingPeerName: String = ""

    // ---------------- Public convenience ----------------

    val isBluetoothEnabled: Boolean get() = bluetoothAdapter?.isEnabled == true
    val isBluetoothAvailable: Boolean get() = bluetoothAdapter != null

    val connectedPeerName: String? get() = _peerName.value
    val isConnected: Boolean get() = _connectionState.value == ConnectionState.CONNECTED

    // ---------------- Permission helpers ----------------

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(appContext, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** True when the app may scan for other devices. */
    private fun canScan(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    /** True when the app may perform Bluetooth operations that read device identity. */
    private fun canConnect(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
    }

    // ---------------- Discovery ----------------

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (!isBluetoothEnabled || _isScanning.value == true) return
        if (!canScan()) return

        _discoveredUsers.postValue(emptyList())
        _isScanning.postValue(true)

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                appContext.registerReceiver(discoveryReceiver, filter)
            }
            receiverRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register discovery receiver", e)
        }
        try {
            bluetoothAdapter?.startDiscovery()
        } catch (e: SecurityException) {
            Log.w(TAG, "startDiscovery requires permission", e)
            unregisterDiscoveryReceiver()
            _isScanning.postValue(false)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        if (_isScanning.value != true) return
        try {
            bluetoothAdapter?.cancelDiscovery()
        } catch (e: Exception) {
            Log.w(TAG, "cancelDiscovery failed", e)
        }
        unregisterDiscoveryReceiver()
        _isScanning.postValue(false)
    }

    private fun unregisterDiscoveryReceiver() {
        if (receiverRegistered) {
            try {
                appContext.unregisterReceiver(discoveryReceiver)
            } catch (e: Exception) {
                Log.w(TAG, "unregisterReceiver failed", e)
            }
            receiverRegistered = false
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
                    device ?: return
                    if (device.address == bluetoothAdapter?.address) return
                    if (!looksLikeAirWavePhone(device)) return
                    val existing = _discoveredUsers.value.orEmpty()
                    if (existing.any { it.address == device.address }) return
                    val newList = existing + toUser(device)
                    _discoveredUsers.postValue(newList)
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    unregisterDiscoveryReceiver()
                    _isScanning.postValue(false)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun looksLikeAirWavePhone(device: BluetoothDevice): Boolean {
        val name = try { device.name } catch (e: SecurityException) { null } ?: return false
        if (name.startsWith(AIRWAVE_NAME_PREFIX)) return true
        val major = try { device.bluetoothClass?.majorDeviceClass } catch (e: SecurityException) { null }
        return major == BluetoothClass.Device.Major.PHONE || major == BluetoothClass.Device.Major.COMPUTER
    }

    @SuppressLint("MissingPermission")
    private fun toUser(device: BluetoothDevice): AirWaveUser {
        val raw = try { device.name } catch (e: SecurityException) { null } ?: device.address
        val display = if (raw.startsWith(AIRWAVE_NAME_PREFIX)) raw.removePrefix(AIRWAVE_NAME_PREFIX).trim() else raw
        return AirWaveUser(
            name = display.ifEmpty { raw },
            address = device.address,
            device = device
        )
    }

    // ---------------- Advertising our AirWave identity ----------------

    /**
     * Renames this device to "AirWave <nickname>" so other AirWave users can
     * spot it during discovery. The original device name is remembered the
     * first time and restored by [restoreOriginalDeviceName].
     */
    @SuppressLint("MissingPermission")
    fun ensureAirWaveDeviceName() {
        if (!canConnect()) return
        val nickname = PreferencesHelper.nickname.trim()
        if (nickname.isEmpty()) return
        val target = AIRWAVE_NAME_PREFIX + nickname
        try {
            val current = bluetoothAdapter?.name ?: return
            if (originalDeviceName == null && !current.startsWith(AIRWAVE_NAME_PREFIX)) {
                originalDeviceName = current
            }
            if (current != target) {
                bluetoothAdapter?.name = target
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set Bluetooth device name", e)
        }
    }

    /** Restores the Bluetooth name this device had before AirWave renamed it. */
    @SuppressLint("MissingPermission")
    fun restoreOriginalDeviceName() {
        if (!canConnect()) return
        val original = originalDeviceName ?: return
        originalDeviceName = null
        try {
            val current = bluetoothAdapter?.name ?: return
            // Only restore if the device still carries the AirWave name we set.
            if (current.startsWith(AIRWAVE_NAME_PREFIX)) {
                bluetoothAdapter?.name = original
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not restore Bluetooth device name", e)
        }
    }

    // ---------------- Listening (so others can find us) ----------------

    /**
     * Starts the RFCOMM accept loop. A brand-new thread is created on every
     * call so listening can restart after a session (a Thread can only be
     * started once).
     */
    @SuppressLint("MissingPermission")
    fun startListening() {
        if (!isBluetoothEnabled || isServerRunning) return
        if (!canConnect()) return

        isServerRunning = true
        val thread = Thread { runServerAcceptLoop() }.apply {
            isDaemon = true
            name = "airwave-server"
        }
        acceptThread = thread
        thread.start()
    }

    /** Stops accepting new connections. Safe to call repeatedly. */
    @SuppressLint("MissingPermission")
    fun stopListening() {
        isServerRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "close server socket failed", e)
        }
        serverSocket = null
        acceptThread = null
    }

    @SuppressLint("MissingPermission")
    private fun runServerAcceptLoop() {
        try {
            serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                "AirWave",
                AIRWAVE_UUID
            )
            while (isServerRunning) {
                val accepted = try {
                    serverSocket?.accept()
                } catch (e: Exception) {
                    null
                } ?: break
                handler.post { onSocketOpened(accepted, initiated = false) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Server accept loop ended", e)
        } finally {
            isServerRunning = false
            try {
                serverSocket?.close()
            } catch (e: Exception) {
                // Ignore
            }
            serverSocket = null
            acceptThread = null
        }
    }

    // ---------------- Outgoing connection ----------------

    fun connectToUser(user: AirWaveUser) {
        connectToAddress(user.address)
    }

    @SuppressLint("MissingPermission")
    fun connectToAddress(address: String) {
        if (!isBluetoothEnabled) return
        if (!canConnect()) return
        endSessionInternal(notifyEnd = false)
        handler.post {
            _unexpectedEnd.postValue(null)
            dialedAddress = address
            initiatedByUs = true
            sessionReady = false
            _peerName.postValue(null)
            _messages.postValue(emptyList())
            _connectionState.postValue(ConnectionState.CONNECTING)
        }
        Thread {
            try {
                val device = bluetoothAdapter?.getRemoteDevice(address) ?: return@Thread
                bluetoothAdapter?.cancelDiscovery()
                val socket = device.createRfcommSocketToServiceRecord(AIRWAVE_UUID)
                socket.connect()
                handler.post { onSocketOpened(socket, initiated = true) }
            } catch (e: Exception) {
                handler.post {
                    endSessionInternal(notifyEnd = true, unexpected = UnexpectedEnd.FAILED)
                }
            }
        }.start()
    }

    // ---------------- Socket handling & handshake ----------------

    private fun onSocketOpened(socket: BluetoothSocket, initiated: Boolean) {
        // Only one active session at a time.
        if (sessionSocket != null) {
            try { socket.close() } catch (e: Exception) {}
            return
        }
        sessionSocket = socket
        sessionReady = false
        _connectionState.postValue(ConnectionState.CONNECTING)
        _peerName.postValue(null)

        // Advertise who we are; the handshake line confirms AirWave compatibility.
        sendRawLine("$PROTO_HELLO${PreferencesHelper.nickname.trim()}")

        startListeningForMessages(socket)
    }

    private fun startListeningForMessages(socket: BluetoothSocket) {
        messageThread?.cancel()
        messageThread = MessageThread(
            socket,
            onMessageReceived = { line -> handler.post { handleIncomingLine(line) } },
            onDisconnected = { handler.post { onSocketClosed() } }
        )
        messageThread?.start()
    }

    private fun handleIncomingLine(line: String) {
        when {
            line.startsWith(PROTO_HELLO) -> handleHello(line.removePrefix(PROTO_HELLO).trim())
            line == PROTO_READY -> handleReady()
            line.startsWith(PROTO_PREFIX) -> {
                // Unknown control line - ignore.
            }
            else -> handleChatLine(line)
        }
    }

    private fun handleHello(peer: String) {
        if (peer.isEmpty()) {
            // Not a valid AirWave peer; refuse the socket.
            endSessionInternal(notifyEnd = true, unexpected = UnexpectedEnd.FAILED)
            return
        }
        if (PreferencesHelper.isBlocked(peer)) {
            // Blocked user - never surface the request, just drop the socket.
            endSessionInternal(notifyEnd = false)
            return
        }

        if (initiatedByUs) {
            // We dialed them; their HELLO is expected. Stay in CONNECTING until READY.
            _peerName.postValue(peer)
        } else {
            // Someone dialed us - ask the user whether to accept.
            _peerName.postValue(peer)
            pendingPeerName = peer
            _incomingRequest.postValue(IncomingRequest(peer))
            _connectionState.postValue(ConnectionState.INCOMING)
        }
    }

    private fun handleReady() {
        if (initiatedByUs && _incomingRequest.value == null) {
            _peerName.value?.let { markSessionConnected() }
        }
    }

    private fun handleChatLine(text: String) {
        // Chat only flows once a session is ready.
        if (!sessionReady && _incomingRequest.value == null) return
        val sender = _peerName.value ?: "Peer"
        appendMessage(ChatMessage(senderName = sender, text = text, isSent = false))
    }

    private fun markSessionConnected() {
        sessionReady = true
        _connectionState.postValue(ConnectionState.CONNECTED)
    }

    private fun sendRawLine(line: String) {
        val thread = messageThread ?: return
        if (thread.isRunning) thread.sendMessage(line)
    }

    // ---------------- Incoming request decisions ----------------

    fun acceptIncomingRequest() {
        val name = pendingPeerName
        if (name.isEmpty()) return
        _unexpectedEnd.postValue(null)
        _incomingRequest.postValue(null)
        pendingPeerName = ""
        markSessionConnected()
        sendRawLine(PROTO_READY)
    }

    fun rejectIncomingRequest() {
        _unexpectedEnd.postValue(null)
        endSessionInternal(notifyEnd = false)
        _incomingRequest.postValue(null)
        pendingPeerName = ""
    }

    fun blockIncomingRequest() {
        val name = pendingPeerName
        if (name.isNotEmpty()) PreferencesHelper.addBlocked(name)
        _unexpectedEnd.postValue(null)
        _incomingRequest.postValue(null)
        pendingPeerName = ""
        endSessionInternal(notifyEnd = false)
    }

    // ---------------- Session messaging ----------------

    fun sendMessage(text: String): Boolean {
        val safe = text.trim()
        if (safe.isEmpty()) return false
        if (!sessionReady || sessionSocket == null) return false
        val sender = PreferencesHelper.nickname.trim().ifEmpty { "Me" }
        val sent = sendRawText(safe)
        appendMessage(ChatMessage(senderName = sender, text = safe, isSent = true))
        return sent
    }

    private fun sendRawText(text: String): Boolean {
        val thread = messageThread ?: return false
        if (!thread.isRunning) return false
        thread.sendMessage(text)
        return true
    }

    private fun appendMessage(message: ChatMessage) {
        synchronized(messagesLock) {
            val current = _messages.value.orEmpty()
            _messages.postValue(current + message)
        }
    }

    // ---------------- Ending sessions ----------------

    private fun onSocketClosed() {
        if (sessionSocket == null) return
        if (sessionReady) {
            endSessionInternal(notifyEnd = true, unexpected = UnexpectedEnd.LOST)
        } else {
            // Peer closed during handshake: they declined, blocked, or failed.
            endSessionInternal(notifyEnd = true, unexpected = UnexpectedEnd.DECLINED)
        }
    }

    /** Ends the active session and always returns to listening for new requests. */
    fun disconnect() {
        _unexpectedEnd.postValue(null)
        endSessionInternal(notifyEnd = false)
    }

    private fun endSessionInternal(notifyEnd: Boolean, unexpected: UnexpectedEnd? = null) {
        messageThread?.cancel()
        messageThread = null
        try { sessionSocket?.close() } catch (e: Exception) {}
        sessionSocket = null
        sessionReady = false
        initiatedByUs = false
        dialedAddress = ""

        if (_incomingRequest.value != null) {
            _incomingRequest.postValue(null)
        }
        pendingPeerName = ""

        _peerName.postValue(null)
        _messages.postValue(emptyList())
        _connectionState.postValue(ConnectionState.DISCONNECTED)
        if (notifyEnd && unexpected != null) {
            _unexpectedEnd.postValue(unexpected)
        }
    }

    /** True if the session ended because we disconnected on purpose. */
    fun consumeUnexpectedEnd(): UnexpectedEnd? {
        val value = _unexpectedEnd.value
        _unexpectedEnd.postValue(null)
        return value
    }

    // ---------------- Models ----------------

    data class IncomingRequest(val peerName: String)

    enum class ConnectionState { DISCONNECTED, CONNECTING, INCOMING, CONNECTED }

    enum class UnexpectedEnd { DECLINED, LOST, FAILED }

    // ---------------- Lifecycle ----------------

    /**
     * Releases every resource the manager holds: stops scanning, stops the
     * accept loop, ends any active session and restores the original
     * Bluetooth device name.
     */
    fun destroy() {
        stopDiscovery()
        unregisterDiscoveryReceiver()
        stopListening()
        disconnect()
        restoreOriginalDeviceName()
    }

    companion object {
        private const val TAG = "BluetoothManager"

        const val AIRWAVE_NAME_PREFIX = "AirWave "
        const val PROTO_PREFIX = "AW/"
        const val PROTO_HELLO = "AW/HELLO|"
        const val PROTO_READY = "AW/READY"

        val AIRWAVE_UUID: UUID = UUID.fromString("fa87bc08-64ce-45e7-9c49-259b4791342f")

        @Volatile
        private var INSTANCE: BluetoothManager? = null

        fun getInstance(context: Context): BluetoothManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BluetoothManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
