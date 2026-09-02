package com.example.airwave.ui.chat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.airwave.R
import com.example.airwave.bluetooth.BluetoothManager
import com.example.airwave.data.local.ConversationEntity
import com.example.airwave.data.local.DatabaseHelper
import com.example.airwave.data.local.MessageEntity
import com.example.airwave.ui.adapter.MessageListAdapter
import com.example.airwave.util.PreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ChatFragment : Fragment() {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnVoice: ImageButton
    private lateinit var btnMore: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvEmpty: TextView

    private val messages = mutableListOf<MessageEntity>()
    private lateinit var messageAdapter: MessageListAdapter
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var db: DatabaseHelper

    private var deviceAddress: String = ""
    private var deviceName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deviceAddress = arguments?.getString("deviceAddress") ?: ""
        deviceName = arguments?.getString("deviceName") ?: deviceAddress

        db = DatabaseHelper.getInstance(requireContext())
        bluetoothManager = BluetoothManager(requireContext().applicationContext)

        rvMessages = view.findViewById(R.id.rvMessages)
        etMessage = view.findViewById(R.id.etMessage)
        btnSend = view.findViewById(R.id.btnSend)
        btnVoice = view.findViewById(R.id.btnVoice)
        btnMore = view.findViewById(R.id.btnMore)
        tvTitle = view.findViewById(R.id.tvTitle)
        tvStatus = view.findViewById(R.id.tvStatus)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        tvTitle.text = deviceName
        tvStatus.text = getString(R.string.bluetooth_connecting)

        messageAdapter = MessageListAdapter(messages)
        rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = messageAdapter

        loadMessages()
        connectToDevice()

        btnSend.setOnClickListener { sendMessage() }
        btnVoice.setOnClickListener { startVoiceInput() }
        btnMore.setOnClickListener { showMenu(it) }

        bluetoothManager.onMessageReceived = { message ->
            handler.post {
                val msg = MessageEntity(
                    chatId = deviceAddress,
                    senderAddress = deviceAddress,
                    senderName = deviceName,
                    text = message,
                    isSent = false
                )
                addMessage(msg)
                saveMessage(msg)
            }
        }

        bluetoothManager.onConnectionStateChanged = { state ->
            handler.post {
                when (state) {
                    BluetoothManager.ConnectionState.CONNECTED -> {
                        tvStatus.text = getString(R.string.bluetooth_connected)
                    }
                    BluetoothManager.ConnectionState.DISCONNECTED -> {
                        tvStatus.text = getString(R.string.bluetooth_disconnected)
                        Toast.makeText(context, R.string.chat_connection_lost, Toast.LENGTH_SHORT).show()
                    }
                    BluetoothManager.ConnectionState.CONNECTING -> {
                        tvStatus.text = getString(R.string.bluetooth_connecting)
                    }
                    BluetoothManager.ConnectionState.FAILED -> {
                        tvStatus.text = getString(R.string.nearby_connection_failed)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun loadMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            val savedMessages = withContext(Dispatchers.IO) {
                db.getMessagesForChat(deviceAddress)
            }
            messages.clear()
            messages.addAll(savedMessages)
            messageAdapter.notifyDataSetChanged()
            if (messages.isNotEmpty()) {
                rvMessages.scrollToPosition(messages.size - 1)
                tvEmpty.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun connectToDevice() {
        val btManager = requireContext().getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        val btAdapter = btManager?.adapter
        val device = btAdapter?.getRemoteDevice(deviceAddress)
        if (device != null) {
            bluetoothManager.connectToDevice(device)
        }
    }

    private fun sendMessage() {
        val text = etMessage.text?.toString()?.trim() ?: ""
        if (text.isEmpty()) return

        val sent = bluetoothManager.sendMessage(text)
        val msg = MessageEntity(
            chatId = deviceAddress,
            senderAddress = "local",
            senderName = PreferencesHelper.nickname,
            text = text,
            isSent = true,
            status = if (sent) MessageEntity.STATUS_SENT else MessageEntity.STATUS_FAILED
        )
        addMessage(msg)
        saveMessage(msg)
        updateConversation(text)
        etMessage.text?.clear()
    }

    private fun addMessage(message: MessageEntity) {
        messages.add(message)
        messageAdapter.notifyItemInserted(messages.size - 1)
        rvMessages.scrollToPosition(messages.size - 1)
        tvEmpty.visibility = View.GONE
    }

    private fun saveMessage(message: MessageEntity) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.insertMessage(message)
        }
    }

    private fun updateConversation(lastMessage: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val conversation = ConversationEntity(
                chatId = deviceAddress,
                deviceAddress = deviceAddress,
                deviceName = deviceName,
                lastMessage = lastMessage,
                lastMessageTime = System.currentTimeMillis(),
                unreadCount = 0
            )
            db.insertOrUpdateConversation(conversation)
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
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
                etMessage.setText(matches[0])
            }
        }
    }

    private fun showMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.chat_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_clear -> {
                    AlertDialog.Builder(requireContext())
                        .setMessage(R.string.chat_clear)
                        .setPositiveButton(R.string.yes) { _, _ -> clearChat() }
                        .setNegativeButton(R.string.no, null)
                        .show()
                    true
                }
                R.id.action_disconnect -> {
                    bluetoothManager.disconnect()
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun clearChat() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.deleteMessagesForChat(deviceAddress)
        }
        messages.clear()
        messageAdapter.notifyDataSetChanged()
        tvEmpty.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }
}
