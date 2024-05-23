package com.example.bt.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.util.*

class ConnectThread(device: BluetoothDevice, private val listener: BluetoothController.Listener) : Thread() {
    private val uid = "00001101-0000-1000-8000-00805F9B34FB"
    private var socket: BluetoothSocket? = null

    init {
        try {
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString(uid))
        } catch (e: IOException) {
            val errorMessage = "ERROR! CATCH IOException in ConnectThread init"
            listener.onError(errorMessage)
            Log.d("MyFilter", errorMessage)
        } catch (e: SecurityException) {
            val errorMessage = "ERROR! CATCH SecurityException in ConnectThread init"
            listener.onError(errorMessage)
            Log.d("MyFilter", errorMessage)
        }
    }

    override fun run() {
        try {
            socket?.connect()
            listener.onConnected()
            readMessage()
            Log.d("MyFilter", "Connected!")
        } catch (e: IOException) {
            val errorMessage = "ERROR! CATCH IOException in ConnectThread run"
            listener.onError(errorMessage)
            Log.d("MyFilter", errorMessage)
        } catch (e: SecurityException) {
            val errorMessage = "ERROR! CATCH SecurityException in ConnectThread run"
            listener.onError(errorMessage)
            Log.d("MyFilter", errorMessage)
        }
    }

    private fun readMessage() {
        val buffer = ByteArray(256)
        while (true) {
            try {
                val messageLength = socket?.inputStream?.read(buffer)
                val message = String(buffer, 0, messageLength ?: 0)
                listener.onMessageReceived(message)
            } catch (e: IOException) {
                val errorMessage = "ERROR! CATCH IOException in ConnectThread readMessage"
                listener.onError(errorMessage)
                Log.d("MyFilter", errorMessage)
                break
            }
        }
    }

    fun sendMessage(message: String) {
        try {
            socket?.outputStream?.write(message.toByteArray())
        } catch (e: IOException) {
            val errorMessage = "ERROR! CATCH IOException in ConnectThread sendMessage"
            listener.onError(errorMessage)
            Log.d("MyFilter", errorMessage)
        }
    }

    fun closeConnection() {
        try {
            socket?.close()
            listener.onDisconnected()
        } catch (e: IOException) {
            val errorMessage = "ERROR! CATCH IOException in ConnectThread closeConnection"
            listener.onError(errorMessage)
            Log.d("MyFilter", errorMessage)
        }
    }
}