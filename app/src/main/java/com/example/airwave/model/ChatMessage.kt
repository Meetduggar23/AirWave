package com.example.airwave.model

/**
 * A single chat message that exists only in memory for the duration of the
 * active Bluetooth session. Nothing about messages is ever persisted.
 */
data class ChatMessage(
    val senderName: String,
    val text: String,
    val isSent: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
