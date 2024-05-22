package com.example.bt.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bt.R
import com.example.bt.databinding.ItemDeviceBinding
import com.example.bt.model.DeviceItem
import com.google.android.material.snackbar.Snackbar

class DeviceAdapter(
    private val listener: PairedDeviceListener,
    private val isDevicesPaired: Boolean
) : ListAdapter<DeviceItem, DeviceAdapter.DeviceHolder>(DeviceDiffUtil()) {

    private var oldCheckBox: CheckBox? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceHolder, position: Int) {
        holder.bind(currentList[position])
    }

    inner class DeviceHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val binding = ItemDeviceBinding.bind(view)

        fun bind(item: DeviceItem) {
            with(binding) {
                try {
                    deviceName.text = item.deviceName
                    deviceMacAddress.text = item.bluetoothDevice.address
                } catch (e: SecurityException) {
                    Log.d("MyFilter", "ERROR IN ADAPTER")
                }
                root.setOnClickListener {
                    if (isDevicesPaired) {
                        selectDevice(checkbox, item)
                    } else {
                        try {
                            item.bluetoothDevice.createBond()
                        } catch (e: SecurityException) {
                            Log.d("MyFilter", "ERROR IN ADAPTER WITH CREATE BOND")
                        }
                    }
                }
                checkbox.isVisible = isDevicesPaired
                if (isDevicesPaired) {
                    checkbox.setOnClickListener {
                        selectDevice(checkbox, item)
                    }
                    if (item.isChecked) {
                        selectDevice(checkbox, item)
                    }
                }

            }
        }

        private fun selectDevice(checkBox: CheckBox, item: DeviceItem) {
            if (isDevicesPaired) {
                item.isChecked = true
                oldCheckBox?.isChecked = false
                oldCheckBox = checkBox
                oldCheckBox?.isChecked = true
            }
            listener.onPairedDeviceClick(item)
        }
    }

    class DeviceDiffUtil : DiffUtil.ItemCallback<DeviceItem>() {

        override fun areItemsTheSame(oldItem: DeviceItem, newItem: DeviceItem) =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: DeviceItem, newItem: DeviceItem) =
            oldItem.isChecked == newItem.isChecked
    }

    interface PairedDeviceListener {
        fun onPairedDeviceClick(device: DeviceItem)
    }
}