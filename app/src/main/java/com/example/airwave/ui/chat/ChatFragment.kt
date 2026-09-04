package com.example.airwave.ui.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.airwave.R
import com.example.airwave.bluetooth.BluetoothManager
import com.example.airwave.model.ChatMessage
import com.example.airwave.ui.adapter.MessageListAdapter
import java.util.Locale

/**
 * The chat screen belongs to the active Bluetooth session. Messages live in
 * memory only and are cleared whenever the session ends.
 */
class ChatFragment : Fragment() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnVoice: ImageButton
    private lateinit var btnMore: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var tvAvatar: TextView

    private lateinit var messageAdapter: MessageListAdapter

    private val bluetoothManager: BluetoothManager
        get() = BluetoothManager.getInstance(requireContext())

    private var deviceAddress: String = ""
    private var requestedName: String = ""
    private var endedDialogShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, backCallback)
    }

    private val stateObserver = Observer<BluetoothManager.ConnectionState?> { state ->
        if (!isAdded) return@Observer
        renderConnectionState()
        if (state == BluetoothManager.ConnectionState.DISCONNECTED) {
            handleUnexpectedDisconnectIfAny()
        }
    }

    private val peerObserver = Observer<String?> { peer ->
        if (!isAdded) return@Observer
        renderConnectionState()
    }

    private val messagesObserver = Observer<List<ChatMessage>> { messages ->
        if (!isAdded) return@Observer
        messageAdapter.updateData(messages)
        if (messages.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
            rvMessages.scrollToPosition(messages.size - 1)
        }
    }

    private val unexpectedEndObserver = Observer<BluetoothManager.UnexpectedEnd?> { end ->
        if (!isAdded || end == null || endedDialogShowing) return@Observer
        bluetoothManager.consumeUnexpectedEnd()
        showUnexpectedEndDialog(end)
    }

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            confirmDisconnectAndLeave()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deviceAddress = arguments?.getString("deviceAddress") ?: ""
        requestedName = arguments?.getString("deviceName") ?: deviceAddress

        rvMessages = view.findViewById(R.id.rvMessages)
        etMessage = view.findViewById(R.id.etMessage)
        btnSend = view.findViewById(R.id.btnSend)
        btnVoice = view.findViewById(R.id.btnVoice)
        btnMore = view.findViewById(R.id.btnMore)
        tvTitle = view.findViewById(R.id.tvTitle)
        tvStatus = view.findViewById(R.id.tvStatus)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        tvAvatar = view.findViewById(R.id.tvAvatar)

        messageAdapter = MessageListAdapter(bluetoothManager.messages.value.orEmpty())
        rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = messageAdapter

        btnSend.setOnClickListener { sendMessage() }
        btnVoice.setOnClickListener { onVoiceClicked() }
        btnMore.setOnClickListener { showMenu(it) }

        view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            confirmDisconnectAndLeave()
        }

        attachObservers()

        // Attach to an already established session (accepted incoming request)
        // or start dialing when arriving from the nearby list.
        val state = bluetoothManager.connectionState.value
        if (state == BluetoothManager.ConnectionState.CONNECTED ||
            state == BluetoothManager.ConnectionState.CONNECTING
        ) {
            renderConnectionState()
        } else if (deviceAddress.isNotEmpty()) {
            renderConnectionState()
            bluetoothManager.connectToAddress(deviceAddress)
        } else {
            // No session and nothing to dial - leave.
            Toast.makeText(context, R.string.error_connection_failed, Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rvMessages.adapter = null
    }

    override fun onDestroy() {
        super.onDestroy()
        backCallback.remove()
    }

    private fun attachObservers() {
        bluetoothManager.connectionState.observe(viewLifecycleOwner, stateObserver)
        bluetoothManager.peerName.observe(viewLifecycleOwner, peerObserver)
        bluetoothManager.messages.observe(viewLifecycleOwner, messagesObserver)
        bluetoothManager.unexpectedEnd.observe(viewLifecycleOwner, unexpectedEndObserver)
    }

    private fun renderConnectionState() {
        val peer = bluetoothManager.peerName.value
        val displayName = peer ?: requestedName.ifBlank { deviceAddress }
        tvTitle.text = displayName
        tvAvatar.text = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        when (bluetoothManager.connectionState.value) {
            BluetoothManager.ConnectionState.CONNECTED -> {
                tvStatus.text = getString(R.string.bluetooth_connected)
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_connected))
                etMessage.isEnabled = true
                btnSend.isEnabled = true
            }
            BluetoothManager.ConnectionState.CONNECTING,
            BluetoothManager.ConnectionState.INCOMING -> {
                tvStatus.text = getString(R.string.bluetooth_connecting)
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_connecting))
                etMessage.isEnabled = false
                btnSend.isEnabled = false
            }
            else -> {
                tvStatus.text = getString(R.string.bluetooth_disconnected)
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_disconnected))
                etMessage.isEnabled = false
                btnSend.isEnabled = false
            }
        }
    }

    // ---------------- Messaging ----------------

    private fun sendMessage() {
        val text = etMessage.text?.toString()?.trim() ?: ""
        if (text.isEmpty()) return

        if (bluetoothManager.connectionState.value != BluetoothManager.ConnectionState.CONNECTED) {
            Toast.makeText(context, R.string.bluetooth_disconnected, Toast.LENGTH_SHORT).show()
            return
        }
        bluetoothManager.sendMessage(text)
        etMessage.text?.clear()
    }

    private fun onVoiceClicked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        startVoiceInput()
    }

    private val recordAudioLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceInput()
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.chat_voice))
        }
        try {
            voiceInputLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, R.string.error_speech_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private val voiceInputLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                // Recognized speech is placed in the box for review - never auto-sent.
                etMessage.setText(matches[0])
                etMessage.setSelection(matches[0].length)
            }
        }
    }

    // ---------------- Disconnect ----------------

    private fun showMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.chat_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_disconnect -> {
                    confirmDisconnectAndLeave()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun confirmDisconnectAndLeave() {
        val peer = bluetoothManager.peerName.value
        val connected = bluetoothManager.connectionState.value == BluetoothManager.ConnectionState.CONNECTED

        val finish: () -> Unit = {
            bluetoothManager.disconnect()
            endedDialogShowing = false
            findNavController().popBackStack()
        }

        if (!connected) {
            finish()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.chat_disconnect)
            .setMessage(getString(R.string.disconnect_confirm_message, peer ?: ""))
            .setPositiveButton(R.string.chat_disconnect) { _, _ -> finish() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun handleUnexpectedDisconnectIfAny() {
        val end = bluetoothManager.unexpectedEnd.value ?: return
        if (endedDialogShowing) return
        endedDialogShowing = true
        bluetoothManager.consumeUnexpectedEnd()
        showUnexpectedEndDialog(end)
    }

    private fun showUnexpectedEndDialog(end: BluetoothManager.UnexpectedEnd) {
        endedDialogShowing = true
        val messageRes = when (end) {
            BluetoothManager.UnexpectedEnd.DECLINED -> R.string.chat_request_declined
            BluetoothManager.UnexpectedEnd.FAILED -> R.string.nearby_connection_failed
            else -> R.string.chat_connection_lost
        }
        val canRetry = deviceAddress.isNotEmpty()
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.connection_lost_title)
            .setMessage(messageRes)
            .setCancelable(false)
            .setPositiveButton(R.string.connection_lost_back) { _, _ ->
                endedDialogShowing = false
                findNavController().popBackStack()
            }
        if (canRetry) {
            dialog.setNegativeButton(R.string.connection_lost_retry) { _, _ ->
                endedDialogShowing = false
                bluetoothManager.connectToAddress(deviceAddress)
            }
        }
        dialog.show()
    }
}
