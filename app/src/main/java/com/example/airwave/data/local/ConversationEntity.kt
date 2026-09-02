package com.example.airwave.data.local

data class ConversationEntity(
    val chatId: String,
    val deviceAddress: String,
    val deviceName: String,
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isFavorite: Boolean = false
)
