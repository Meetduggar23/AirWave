package com.example.airwave.data.local

data class ConversationEntity(
    val chatId: String,
    val deviceAddress: String,
    val deviceName: String,
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val unreadCount: Int = 0
)
