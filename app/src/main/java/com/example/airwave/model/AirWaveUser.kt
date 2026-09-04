package com.example.airwave.model

import android.bluetooth.BluetoothDevice

/**
 * A nearby device shown in discovery results. Only devices that look like
 * phones/computers (or advertise an "AirWave" name) are surfaced, and true
 * AirWave compatibility is confirmed by the handshake when connecting.
 */
data class AirWaveUser(
    val name: String,
    val address: String,
    val device: BluetoothDevice
) {
    val initial: Char
        get() = name.trim().firstOrNull()?.uppercaseChar() ?: '?'
}
