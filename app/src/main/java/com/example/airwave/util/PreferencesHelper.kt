package com.example.airwave.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Local convenience preferences for AirWave.
 *
 * The only identity data stored is the session nickname (remembered locally so
 * the user does not have to retype it). There is no account, no password, no
 * remote profile, and no chat history anywhere on this device.
 */
object PreferencesHelper {
    private const val PREF_NAME = "airwave_prefs"

    /**
     * Nickname validation limits - the single source of truth for how long the
     * AirWave identity may be. Every screen that validates the name (Welcome,
     * Profile) and the QR verification payload all read these constants.
     */
    const val MIN_NICKNAME_LENGTH = 2
    const val MAX_NICKNAME_LENGTH = 20

    private const val KEY_NICKNAME = "nickname"
    private const val KEY_BLOCKED_USERS = "blocked_users"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_MESSAGE_NOTIFICATIONS = "message_notifications"
    private const val KEY_CONNECTION_NOTIFICATIONS = "connection_notifications"
    private const val KEY_DISCOVERABLE = "discoverable"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /** The session nickname. Empty means the user has not entered a name yet. */
    var nickname: String
        get() = prefs.getString(KEY_NICKNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NICKNAME, value.trim()).apply()

    /** Users blocked during incoming connection requests (session privacy). */
    fun isBlocked(name: String): Boolean = name in getBlocked()

    fun addBlocked(name: String) {
        val updated = getBlocked() + name
        prefs.edit().putStringSet(KEY_BLOCKED_USERS, updated).apply()
    }

    private fun getBlocked(): Set<String> =
        prefs.getStringSet(KEY_BLOCKED_USERS, emptySet()) ?: emptySet()

    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, 2) // 0 light, 1 dark, 2 system
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var messageNotifications: Boolean
        get() = prefs.getBoolean(KEY_MESSAGE_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_MESSAGE_NOTIFICATIONS, value).apply()

    var connectionNotifications: Boolean
        get() = prefs.getBoolean(KEY_CONNECTION_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_CONNECTION_NOTIFICATIONS, value).apply()

    var discoverable: Boolean
        get() = prefs.getBoolean(KEY_DISCOVERABLE, true)
        set(value) = prefs.edit().putBoolean(KEY_DISCOVERABLE, value).apply()

    /** Forgets the locally remembered nickname (it is not an account). */
    fun clearNickname() {
        prefs.edit().remove(KEY_NICKNAME).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
