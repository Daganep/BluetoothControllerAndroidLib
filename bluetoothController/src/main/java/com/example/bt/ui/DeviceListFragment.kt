package com.example.bt.ui

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bt.R
import com.example.bt.databinding.FragmentListBinding
import com.example.bt.model.Constants.BLUETOOTH_PREFERENCES
import com.example.bt.model.Constants.CONNECTED_DEVICE_MAC_ADDRESS
import com.example.bt.model.DeviceItem
import com.google.android.material.snackbar.Snackbar

class DeviceListFragment : Fragment(), DeviceAdapter.PairedDeviceListener {

    private lateinit var binding: FragmentListBinding
    private lateinit var adapter: DeviceAdapter
    private lateinit var adapterSearch: DeviceAdapter
    private lateinit var bluetoothLauncher: ActivityResultLauncher<Intent>
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isBluetoothOn: Boolean = false
    private var sharedPrefs: SharedPreferences? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermissions()
        registerIntentFilters()
        initBluetoothLauncher()
        initBluetoothAdapter()
        initSharedPrefs()
        initUi()
    }

    override fun onResume() {
        super.onResume()
        updateBluetoothState()
        getPairedDevices()
    }

    override fun onPairedDeviceClick(device: DeviceItem) {
        try {
            adapter.currentList.forEach {
                it.isChecked = it.bluetoothDevice.name == device.bluetoothDevice.name
            }
            saveConnectedDeviceMacAddress(device.bluetoothDevice.address)
        } catch (e:SecurityException) {
            Snackbar.make(binding.root, "ERROR НЕТ РАЗРЕШЕНИЯ", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun initUi() {
        initRecyclerView()
        setBluetoothButtonClickListener()
        setSearchDevicesButtonClickListener()
        setBackButtonClickListener()
        isPairedListEmpty(true)
    }

    private fun updateBluetoothState() {
        isBluetoothOn = bluetoothAdapter?.isEnabled == true
        if (isBluetoothOn) {
            binding.blueToothSwitchButton.setImageResource(R.drawable.icon_bluetooth_on_24px)
        } else {
            binding.blueToothSwitchButton.setImageResource(R.drawable.icon_bluetooth_off_24px)
        }

    }

    private fun initRecyclerView() {
        adapter = DeviceAdapter(listener = this, isDevicesPaired = true)
        adapterSearch = DeviceAdapter(listener = this, isDevicesPaired = false)
        val pairedRecyclerView = binding.pairedDevicesList
        val searchRecyclerView = binding.searchDevicesList
        pairedRecyclerView.layoutManager = LinearLayoutManager(activity)
        searchRecyclerView.layoutManager = LinearLayoutManager(activity)
        pairedRecyclerView.adapter = adapter
        searchRecyclerView.adapter = adapterSearch

        //TODO убрать когда будет добавлен поиск обнаруженных устройств на старте
        binding.searchDevicesList.isVisible = adapterSearch.currentList.isNotEmpty()
        binding.emptySearchDevices.isVisible = adapterSearch.currentList.isEmpty()
    }

    private fun initBluetoothAdapter() {
        val bluetoothService = activity?.getSystemService(Context.BLUETOOTH_SERVICE)
        bluetoothAdapter = (bluetoothService as BluetoothManager).adapter
    }

    private fun initBluetoothLauncher() {
        bluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode != Activity.RESULT_OK) {
                Snackbar.make(binding.root, "Блютуз выключен!", Snackbar.LENGTH_LONG).show()
            }
            updateBluetoothState()
        }
    }

    private fun initSharedPrefs() {
        sharedPrefs = activity?.getSharedPreferences(
            BLUETOOTH_PREFERENCES,
            Context.MODE_PRIVATE
        )
    }

    private fun setBluetoothButtonClickListener() {
        binding.blueToothSwitchButton.setOnClickListener {
            if (bluetoothAdapter?.isEnabled != true) {
                bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }
        updateBluetoothState()
    }

    private fun setSearchDevicesButtonClickListener() {
        binding.searchDevicesButton.setOnClickListener {
            it.isVisible = false
            binding.searchProgressBar.isVisible = true
            searchDevices()
        }
        binding.searchDevicesLabel.setOnClickListener {
            binding.searchDevicesButton.isVisible = false
            binding.searchProgressBar.isVisible = true
            searchDevices()
        }
    }

    private fun searchDevices() {
        try {
            if (bluetoothAdapter?.isEnabled == true) {
                bluetoothAdapter?.startDiscovery()
            }
        } catch (e: SecurityException) {
            Log.d("MyFilter", "ERROR! CATCH SecurityException")
        }
    }

    private fun setBackButtonClickListener() {
        binding.backButton.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun getPairedDevices() {
        try {
            val bluetoothDevices = bluetoothAdapter?.bondedDevices as Set<BluetoothDevice>
            val deviceList = mutableListOf<DeviceItem>()
            bluetoothDevices.forEach {
                val isChecked = it.address == sharedPrefs?.getString(CONNECTED_DEVICE_MAC_ADDRESS, "")
                deviceList.add(DeviceItem(it, isChecked, getDeviceName(it.name)))
                Log.d("MyFilter", "device. name: ${it.name} address: ${it.address} bondState: ${it.bondState}")
            }
            if (isBluetoothOn && deviceList.isNotEmpty()) {
                isPairedListEmpty(false)
                adapter.submitList(deviceList)
            } else {
                isPairedListEmpty(true)
            }
        } catch (e: SecurityException) {
            Snackbar.make(
                binding.root,
                getString(R.string.permission_not_granted_paired_list),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun isPairedListEmpty(isEmpty: Boolean) {
        binding.pairedDevicesList.isVisible = !isEmpty
        binding.emptyPairedDevices.isVisible = isEmpty
    }

    private fun saveConnectedDeviceMacAddress(address: String) {
        val editor = sharedPrefs?.edit()
        editor?.putString(CONNECTED_DEVICE_MAC_ADDRESS, address)
        editor?.apply()
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    try {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        val foundDevicesList = mutableSetOf<DeviceItem>()
                        foundDevicesList.addAll(adapterSearch.currentList)
                        Log.d("MyFilter", "getDeviceName: ${getDeviceName( device?.name)}")
                        device?.let { foundDevicesList.add(DeviceItem(it, false, getDeviceName(it.name))) }
                        adapterSearch.submitList(foundDevicesList.toList())
                        binding.searchDevicesList.isVisible = foundDevicesList.isNotEmpty()
                        binding.emptySearchDevices.isVisible = foundDevicesList.isEmpty()
                        Log.d("MyFilter", "device. name: ${device?.name} address: ${device?.address}")
                    } catch (e: SecurityException) {
                        Log.d("MyFilter", "ERROR! CATCH SecurityException")
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    getPairedDevices()
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    binding.searchDevicesButton.isVisible = true
                    binding.searchProgressBar.isVisible = false
                }
            }
        }
    }

    private fun checkPermissions() {
        val activity = activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activity != null) {
            val permission = ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_SCAN
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, permissionsLocation.toTypedArray(), 1)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private val permissionsLocation = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_PRIVILEGED
    )

    private fun registerIntentFilters() {
        val filters = listOf(
            IntentFilter(BluetoothDevice.ACTION_FOUND),
            IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
            IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        )
        filters.forEach { activity?.registerReceiver(bluetoothReceiver, it) }
    }

    private fun getDeviceName(deviceName: String?): String {
        return if (deviceName.isNullOrEmpty()) {
            getString(R.string.name_undefined)
        } else {
            deviceName
        }
    }

    companion object {
        @JvmStatic
        fun createFragment(extras: Bundle? = null) = DeviceListFragment().apply {
            arguments = extras ?: Bundle()
        }
    }
}