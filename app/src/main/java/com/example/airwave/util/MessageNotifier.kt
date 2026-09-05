package com.example.airwave.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.airwave.AirWaveApp
import com.example.airwave.MainActivity
import com.example.airwave.R
import com.example.airwave.model.ChatMessage

/**
 * Posts local "new message" notifications for the active Bluetooth session.
 *
 * All notifications are grouped into a single, updated notification per chat
 * session (same notification id), so a burst of messages never turns into
 * dozens of separate notifications. Only the peer nickname and a message
 * preview are shown - no Bluetooth/device identifiers.
 */
class MessageNotifier(private val context: Context) {

    private var currentPeer: String? = null
    private var messageCount = 0

    /** Recent incoming (text, timestamp) pairs, used for the messaging-style stack. */
    private val recentMessages = ArrayDeque<Pair<String, Long>>()

    /** Called for each new incoming message that should produce a notification. */
    fun onIncomingMessage(peer: String, text: String, timestamp: Long) {
        if (peer.isBlank() || text.isBlank()) return
        if (peer != currentPeer) {
            // New chat session with a different peer - start a fresh stack.
            currentPeer = peer
            messageCount = 0
            recentMessages.clear()
        }
        messageCount++
        recentMessages.addLast(text to timestamp)
        while (recentMessages.size > MAX_STACK_SIZE) {
            recentMessages.removeFirst()
        }
        postNotification()
    }

    /** Clears per-session state when the chat session ends. */
    fun resetSession() {
        currentPeer = null
        messageCount = 0
        recentMessages.clear()
    }

    private fun postNotification() {
        val peer = currentPeer ?: return
        val latestText = recentMessages.lastOrNull()?.first ?: return

        val style = NotificationCompat.MessagingStyle(peer)
        for ((text, timestamp) in recentMessages) {
            style.addMessage(NotificationCompat.MessagingStyle.Message(text, timestamp, peer))
        }

        val notification = NotificationCompat.Builder(context, AirWaveApp.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setContentTitle(peer)
            .setContentText(latestText)
            .setStyle(style)
            .setNumber(messageCount)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(buildContentIntent(peer))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Permission denied, channel disabled or notifications off -
            // messaging itself is unaffected.
        }
    }

    private fun buildContentIntent(peer: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_CHAT, true)
            putExtra("deviceName", peer)
        }
        return PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val NOTIFICATION_ID = 2002

        /** Extra on the tap intent telling MainActivity to open the chat screen. */
        const val EXTRA_OPEN_CHAT = "airwave.open_chat"

        private const val MAX_STACK_SIZE = 5

        /**
         * Returns the messages that arrived since [lastSeenCount], keeping only
         * real incoming messages (not sent by us, non-empty, with a sender).
         * The list is append-only, so this is how duplicate notifications are
         * avoided - a message is only reported once.
         */
        fun newIncomingMessages(lastSeenCount: Int, messages: List<ChatMessage>): List<ChatMessage> {
            if (messages.isEmpty() || messages.size <= lastSeenCount) return emptyList()
            return messages.subList(lastSeenCount, messages.size)
                .filter { !it.isSent && it.text.isNotBlank() && it.senderName.isNotBlank() }
        }
    }
}