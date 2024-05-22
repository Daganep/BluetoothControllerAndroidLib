package com.example.bt.bluetooth

import android.bluetooth.BluetoothAdapter

class Controller(private val adapter: BluetoothAdapter) {

    private var connectThread: ConnectThread? = null

    fun connect(mac: String, listener: Listener) {
        if (adapter.isEnabled && mac.isNotEmpty()) {
            val device = adapter.getRemoteDevice(mac)
            connectThread = ConnectThread(device, listener)
            connectThread?.start()
        }
    }

    fun sendMessage(message: String) {
        connectThread?.sendMessage(message)
    }

    interface Listener {
        fun onConnected()
        fun onDisconnected()
        fun onMessageReceived(message: String)
        fun onError(error: String)
    }
}