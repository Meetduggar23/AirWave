package com.example.airwave.data.local

data class MessageEntity(
    val id: Long = 0,
    val chatId: String,
    val senderAddress: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSent: Boolean,
    val status: Int = STATUS_SENT
) {
    companion object {
        const val STATUS_SENT = 0
        const val STATUS_DELIVERED = 1
        const val STATUS_FAILED = 2
    }
}
