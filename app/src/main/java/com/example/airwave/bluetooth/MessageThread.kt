package com.example.airwave.bluetooth

import android.bluetooth.BluetoothSocket
import java.io.*
import java.nio.charset.StandardCharsets

class MessageThread(
    private val socket: BluetoothSocket,
    private val onMessageReceived: (String) -> Unit,
    private val onDisconnected: () -> Unit
) : Thread() {

    private val inputStream: InputStream? = socket.inputStream
    private val outputStream: OutputStream? = socket.outputStream
    private var isRunning = true

    override fun run() {
        val buffer = ByteArray(1024)
        val messageBuffer = StringBuilder()

        while (isRunning) {
            try {
                val bytesRead = inputStream?.read(buffer) ?: -1
                if (bytesRead == -1) {
                    isRunning = false
                    onDisconnected.invoke()
                    break
                }

                val chunk = String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
                messageBuffer.append(chunk)

                // Process complete messages (delimited by newline)
                while (true) {
                    val newlineIndex = messageBuffer.indexOf("\n")
                    if (newlineIndex == -1) break

                    val completeMessage = messageBuffer.substring(0, newlineIndex).trim()
                    messageBuffer.delete(0, newlineIndex + 1)

                    if (completeMessage.isNotEmpty()) {
                        onMessageReceived.invoke(completeMessage)
                    }
                }
            } catch (e: IOException) {
                if (isRunning) {
                    isRunning = false
                    onDisconnected.invoke()
                }
                break
            }
        }
    }

    fun sendMessage(text: String) {
        try {
            val message = "$text\n"
            outputStream?.write(message.toByteArray(StandardCharsets.UTF_8))
            outputStream?.flush()
        } catch (e: IOException) {
            // Message failed to send
        }
    }

    fun cancel() {
        isRunning = false
        try {
            inputStream?.close()
            outputStream?.close()
            socket.close()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
