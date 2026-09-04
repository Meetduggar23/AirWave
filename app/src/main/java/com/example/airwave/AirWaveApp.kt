package com.example.airwave

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.example.airwave.util.PreferencesHelper

class AirWaveApp : Application() {

    override fun onCreate() {
        super.onCreate()
        PreferencesHelper.init(this)
        applyStoredTheme()
        createNotificationChannels()
    }

    private fun applyStoredTheme() {
        // 0 = Light, 1 = Dark, 2 = System default
        when (PreferencesHelper.themeMode) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val messageChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New message notifications"
                enableVibration(true)
            }

            val connectionChannel = NotificationChannel(
                CHANNEL_CONNECTIONS,
                "Connections",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Connection status notifications"
            }

            manager.createNotificationChannel(messageChannel)
            manager.createNotificationChannel(connectionChannel)
        }
    }

    companion object {
        const val CHANNEL_MESSAGES = "airwave_messages"
        const val CHANNEL_CONNECTIONS = "airwave_connections"
    }
}
