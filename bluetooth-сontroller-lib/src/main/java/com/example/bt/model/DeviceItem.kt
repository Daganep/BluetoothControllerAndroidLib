package com.example.bt.model

import android.bluetooth.BluetoothDevice

data class DeviceItem(
    val bluetoothDevice: BluetoothDevice,
    var isChecked: Boolean = false,
    val deviceName: String
)
