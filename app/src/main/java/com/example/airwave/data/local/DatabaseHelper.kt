package com.example.airwave.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_MESSAGES_TABLE)
        db.execSQL(CREATE_CONVERSATIONS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS messages")
        db.execSQL("DROP TABLE IF EXISTS conversations")
        onCreate(db)
    }

    fun insertMessage(message: MessageEntity): Long {
        val values = ContentValues().apply {
            put(COL_CHAT_ID, message.chatId)
            put(COL_SENDER_ADDRESS, message.senderAddress)
            put(COL_SENDER_NAME, message.senderName)
            put(COL_TEXT, message.text)
            put(COL_TIMESTAMP, message.timestamp)
            put(COL_IS_SENT, if (message.isSent) 1 else 0)
            put(COL_STATUS, message.status)
        }
        return writableDatabase.insert(TABLE_MESSAGES, null, values)
    }

    fun getMessagesForChat(chatId: String): List<MessageEntity> {
        val messages = mutableListOf<MessageEntity>()
        val cursor = readableDatabase.query(
            TABLE_MESSAGES, null, "$COL_CHAT_ID = ?", arrayOf(chatId),
            null, null, "$COL_TIMESTAMP ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                messages.add(cursorToMessage(it))
            }
        }
        return messages
    }

    fun deleteMessagesForChat(chatId: String) {
        writableDatabase.delete(TABLE_MESSAGES, "$COL_CHAT_ID = ?", arrayOf(chatId))
    }

    fun deleteAllMessages() {
        writableDatabase.delete(TABLE_MESSAGES, null, null)
    }

    fun getAllConversations(): List<ConversationEntity> {
        val conversations = mutableListOf<ConversationEntity>()
        val cursor = readableDatabase.query(
            TABLE_CONVERSATIONS, null, null, null,
            null, null, "$COL_LAST_MESSAGE_TIME DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                conversations.add(cursorToConversation(it))
            }
        }
        return conversations
    }

    fun getConversation(chatId: String): ConversationEntity? {
        val cursor = readableDatabase.query(
            TABLE_CONVERSATIONS, null, "$COL_CHAT_ID = ?", arrayOf(chatId),
            null, null, null
        )
        cursor.use {
            return if (it.moveToFirst()) cursorToConversation(it) else null
        }
    }

    fun insertOrUpdateConversation(conversation: ConversationEntity) {
        val existing = getConversation(conversation.chatId)
        val values = ContentValues().apply {
            put(COL_CHAT_ID, conversation.chatId)
            put(COL_DEVICE_ADDRESS, conversation.deviceAddress)
            put(COL_DEVICE_NAME, conversation.deviceName)
            put(COL_LAST_MESSAGE, conversation.lastMessage)
            put(COL_LAST_MESSAGE_TIME, conversation.lastMessageTime)
            put(COL_UNREAD_COUNT, conversation.unreadCount)
        }
        if (existing != null) {
            writableDatabase.update(TABLE_CONVERSATIONS, values, "$COL_CHAT_ID = ?", arrayOf(conversation.chatId))
        } else {
            writableDatabase.insert(TABLE_CONVERSATIONS, null, values)
        }
    }

    fun deleteConversation(chatId: String) {
        writableDatabase.delete(TABLE_CONVERSATIONS, "$COL_CHAT_ID = ?", arrayOf(chatId))
    }

    fun deleteAllConversations() {
        writableDatabase.delete(TABLE_CONVERSATIONS, null, null)
    }

    private fun cursorToMessage(cursor: Cursor): MessageEntity {
        return MessageEntity(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            chatId = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHAT_ID)),
            senderAddress = cursor.getString(cursor.getColumnIndexOrThrow(COL_SENDER_ADDRESS)),
            senderName = cursor.getString(cursor.getColumnIndexOrThrow(COL_SENDER_NAME)),
            text = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEXT)),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP)),
            isSent = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_SENT)) == 1,
            status = cursor.getInt(cursor.getColumnIndexOrThrow(COL_STATUS))
        )
    }

    private fun cursorToConversation(cursor: Cursor): ConversationEntity {
        return ConversationEntity(
            chatId = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHAT_ID)),
            deviceAddress = cursor.getString(cursor.getColumnIndexOrThrow(COL_DEVICE_ADDRESS)),
            deviceName = cursor.getString(cursor.getColumnIndexOrThrow(COL_DEVICE_NAME)),
            lastMessage = cursor.getString(cursor.getColumnIndexOrThrow(COL_LAST_MESSAGE)),
            lastMessageTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_LAST_MESSAGE_TIME)),
            unreadCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_UNREAD_COUNT))
        )
    }

    companion object {
        private const val DATABASE_NAME = "airwave.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_MESSAGES = "messages"
        private const val TABLE_CONVERSATIONS = "conversations"

        private const val COL_ID = "id"
        private const val COL_CHAT_ID = "chatId"
        private const val COL_SENDER_ADDRESS = "senderAddress"
        private const val COL_SENDER_NAME = "senderName"
        private const val COL_TEXT = "text"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_IS_SENT = "isSent"
        private const val COL_STATUS = "status"

        private const val COL_DEVICE_ADDRESS = "deviceAddress"
        private const val COL_DEVICE_NAME = "deviceName"
        private const val COL_LAST_MESSAGE = "lastMessage"
        private const val COL_LAST_MESSAGE_TIME = "lastMessageTime"
        private const val COL_UNREAD_COUNT = "unreadCount"

        private const val CREATE_MESSAGES_TABLE = """
            CREATE TABLE $TABLE_MESSAGES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CHAT_ID TEXT,
                $COL_SENDER_ADDRESS TEXT,
                $COL_SENDER_NAME TEXT,
                $COL_TEXT TEXT,
                $COL_TIMESTAMP INTEGER,
                $COL_IS_SENT INTEGER,
                $COL_STATUS INTEGER
            )
        """

        private const val CREATE_CONVERSATIONS_TABLE = """
            CREATE TABLE $TABLE_CONVERSATIONS (
                $COL_CHAT_ID TEXT PRIMARY KEY,
                $COL_DEVICE_ADDRESS TEXT,
                $COL_DEVICE_NAME TEXT,
                $COL_LAST_MESSAGE TEXT,
                $COL_LAST_MESSAGE_TIME INTEGER,
                $COL_UNREAD_COUNT INTEGER
            )
        """

        @Volatile
        private var INSTANCE: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
