package com.example.bt.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.util.*

class ConnectThread(device: BluetoothDevice, private val listener: BluetoothController.Listener) : Thread() {
    private val uid = "00001101-0000-1000-8000-00805F9B34FB"
    private var socket: BluetoothSocket? = null

    init {
        try {
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString(uid))
        } catch (exception: IOException) {
            sendError(
                exception = exception.message,
                errorMessage = "Create socket ERROR! CATCH IOException"
            )
        } catch (exception: SecurityException) {
            sendError(
                exception = exception.message,
                errorMessage = "Create socket ERROR! CATCH SecurityException"
            )
        }
    }

    override fun run() {
        try {
            socket?.connect()
            listener.onConnected()
            readMessage()
        } catch (exception: IOException) {
            sendError(
                exception = exception.message,
                errorMessage = "Connecting ERROR! CATCH IOException"
            )
        } catch (exception: SecurityException) {
            sendError(
                exception = exception.message,
                errorMessage = "Connecting ERROR! CATCH SecurityException"
            )
        }
    }

    private fun readMessage() {
        val buffer = ByteArray(BYTE_ARRAY_SIZE)
        var isReadyToRead = true
        while (isReadyToRead) {
            try {
                val messageLength = socket?.inputStream?.read(buffer)
                val message = String(buffer, 0, messageLength ?: 0)
                listener.onMessageReceived(message)
            } catch (exception: IOException) {
                isReadyToRead = false
                sendError(
                    exception = exception.message,
                    errorMessage = "Read message ERROR! CATCH IOException"
                )
            }
        }
    }

    fun sendMessage(message: String) {
        try {
            socket?.outputStream?.write(message.toByteArray())
        } catch (exception: IOException) {
            sendError(
                exception = exception.message,
                errorMessage = "Send message ERROR! CATCH IOException"
            )
        }
    }

    fun closeConnection() {
        try {
            socket?.close()
            listener.onDisconnected()
        } catch (exception: IOException) {
            sendError(
                exception = exception.message,
                errorMessage = "Socket close ERROR! CATCH IOException"
            )
        }
    }

    private fun sendError(exception: String?, errorMessage: String) {
        if (exception.isNullOrEmpty()) {
            listener.onError(errorMessage)
        } else {
            listener.onError(exception)
        }
    }

    companion object {
        private const val BYTE_ARRAY_SIZE: Int = 256
    }
}
