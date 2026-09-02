package com.example.airwave.util

import android.content.Context
import android.content.SharedPreferences

object PreferencesHelper {
    private const val PREF_NAME = "airwave_prefs"

    private const val KEY_NICKNAME = "nickname"
    private const val KEY_STATUS = "status"
    private const val KEY_PROFILE_PICTURE = "profile_picture"
    private const val KEY_IS_GUEST = "is_guest"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_ACCENT_COLOR = "accent_color"
    private const val KEY_CONTRAST_MODE = "contrast_mode"
    private const val KEY_TEXT_SIZE = "text_size"
    private const val KEY_REDUCED_MOTION = "reduced_motion"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_MESSAGE_NOTIFICATIONS = "message_notifications"
    private const val KEY_CONNECTION_NOTIFICATIONS = "connection_notifications"
    private const val KEY_DISCOVERABLE = "discoverable"
    private const val KEY_PROFILE_VISIBLE = "profile_visible"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    var nickname: String
        get() = prefs.getString(KEY_NICKNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NICKNAME, value).apply()

    var status: String
        get() = prefs.getString(KEY_STATUS, "Available nearby") ?: "Available nearby"
        set(value) = prefs.edit().putString(KEY_STATUS, value).apply()

    var profilePicture: String
        get() = prefs.getString(KEY_PROFILE_PICTURE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PROFILE_PICTURE, value).apply()

    var isGuest: Boolean
        get() = prefs.getBoolean(KEY_IS_GUEST, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_GUEST, value).apply()

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, 0)
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    var accentColor: Int
        get() = prefs.getInt(KEY_ACCENT_COLOR, 0)
        set(value) = prefs.edit().putInt(KEY_ACCENT_COLOR, value).apply()

    var contrastMode: Int
        get() = prefs.getInt(KEY_CONTRAST_MODE, 0)
        set(value) = prefs.edit().putInt(KEY_CONTRAST_MODE, value).apply()

    var textSize: Int
        get() = prefs.getInt(KEY_TEXT_SIZE, 1)
        set(value) = prefs.edit().putInt(KEY_TEXT_SIZE, value).apply()

    var reducedMotion: Boolean
        get() = prefs.getBoolean(KEY_REDUCED_MOTION, false)
        set(value) = prefs.edit().putBoolean(KEY_REDUCED_MOTION, value).apply()

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

    var profileVisible: Boolean
        get() = prefs.getBoolean(KEY_PROFILE_VISIBLE, true)
        set(value) = prefs.edit().putBoolean(KEY_PROFILE_VISIBLE, value).apply()

    fun clearProfile() {
        prefs.edit()
            .remove(KEY_NICKNAME)
            .remove(KEY_STATUS)
            .remove(KEY_PROFILE_PICTURE)
            .remove(KEY_IS_GUEST)
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
