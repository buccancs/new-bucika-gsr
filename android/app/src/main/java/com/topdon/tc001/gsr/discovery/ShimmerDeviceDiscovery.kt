package com.topdon.tc001.gsr.discovery

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.elvishew.xlog.XLog

class ShimmerDeviceDiscovery(private val context: Context) {
    
    companion object {
        private const val TAG = "ShimmerDeviceDiscovery"
        
        private val SHIMMER_NAME_PATTERNS = arrayOf(
            "Shimmer",
            "Shimmer3",
            "shimmer",
            "RN42",
            "GSR"
        )
        
        private val SHIMMER_MAC_PREFIXES = arrayOf(
            "00:06:66",
            "00:0C:F6"
        )
    }
    
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val discoveredDevices = mutableSetOf<ShimmerDeviceInfo>()
    private var discoveryListener: ShimmerDiscoveryListener? = null
    private var isScanning = false
    
    interface ShimmerDiscoveryListener {
        fun onShimmerDeviceFound(device: ShimmerDeviceInfo)
        fun onDiscoveryStarted()
        fun onDiscoveryFinished(devicesFound: List<ShimmerDeviceInfo>)
        fun onDiscoveryFailed(error: String)
    }
    
    data class ShimmerDeviceInfo(
        val name: String,
        val address: String,
        val rssi: Short = 0,
        val deviceType: ShimmerDeviceType = ShimmerDeviceType.UNKNOWN,
        val isPaired: Boolean = false,
        val isConnectable: Boolean = true
    )
    
    enum class ShimmerDeviceType {
        SHIMMER3_GSR_PLUS,
        SHIMMER3_IMU,
        SHIMMER3_ECG,
        SHIMMER3_EMG,
        SHIMMER2R,
        UNKNOWN
    }
    
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, 0)
                    
                    device?.let { handleDeviceFound(it, rssi) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    XLog.i(TAG, "Bluetooth discovery started")
                    discoveryListener?.onDiscoveryStarted()
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    XLog.i(TAG, "Bluetooth discovery finished. Found ${discoveredDevices.size} Shimmer devices")
                    isScanning = false
                    discoveryListener?.onDiscoveryFinished(discoveredDevices.toList())
                }
            }
        }
    }
    
    fun setDiscoveryListener(listener: ShimmerDiscoveryListener) {
        this.discoveryListener = listener
    }
    
    fun startDiscovery(): Boolean {
        if (bluetoothAdapter == null) {
            discoveryListener?.onDiscoveryFailed("Bluetooth not available")
            return false
        }
        
        if (!bluetoothAdapter.isEnabled) {
            discoveryListener?.onDiscoveryFailed("Bluetooth not enabled")
            return false
        }
        
        if (!hasBluetoothPermissions()) {
            discoveryListener?.onDiscoveryFailed("Missing Bluetooth permissions")
            return false
        }
        
        if (isScanning) {
            XLog.w(TAG, "Discovery already in progress")
            return false
        }
        
        try {

            discoveredDevices.clear()
            
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            context.registerReceiver(discoveryReceiver, filter)
            
            addPairedShimmerDevices()
            
            isScanning = true
            val started = bluetoothAdapter.startDiscovery()
            
            if (!started) {
                context.unregisterReceiver(discoveryReceiver)
                isScanning = false
                discoveryListener?.onDiscoveryFailed("Failed to start discovery")
            }
            
            return started
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error starting discovery: ${e.message}", e)
            isScanning = false
            discoveryListener?.onDiscoveryFailed("Discovery error: ${e.message}")
            return false
        }
    }
    
    fun stopDiscovery() {
        try {
            if (isScanning && bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            
            try {
                context.unregisterReceiver(discoveryReceiver)
            } catch (e: IllegalArgumentException) {

            }
            
            isScanning = false
            XLog.i(TAG, "Discovery stopped")
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error stopping discovery: ${e.message}", e)
        }
    }
    
    private fun handleDeviceFound(device: BluetoothDevice, rssi: Short) {
        try {
            val deviceName = device.name ?: "Unknown"
            val deviceAddress = device.address
            
            if (isShimmerDevice(deviceName, deviceAddress)) {
                val shimmerDevice = ShimmerDeviceInfo(
                    name = deviceName,
                    address = deviceAddress,
                    rssi = rssi,
                    deviceType = determineShimmerDeviceType(deviceName),
                    isPaired = device.bondState == BluetoothDevice.BOND_BONDED,
                    isConnectable = true
                )
                
                val existingDevice = discoveredDevices.find { it.address == deviceAddress }
                if (existingDevice == null) {
                    discoveredDevices.add(shimmerDevice)
                    discoveryListener?.onShimmerDeviceFound(shimmerDevice)
                    XLog.i(TAG, "Found Shimmer device: $deviceName ($deviceAddress)")
                } else {

                    discoveredDevices.remove(existingDevice)
                    discoveredDevices.add(shimmerDevice)
                }
            }
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error handling discovered device: ${e.message}", e)
        }
    }
    
    private fun addPairedShimmerDevices() {
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            
            bluetoothAdapter?.bondedDevices?.forEach { device ->
                val deviceName = device.name ?: "Unknown"
                val deviceAddress = device.address
                
                if (isShimmerDevice(deviceName, deviceAddress)) {
                    val shimmerDevice = ShimmerDeviceInfo(
                        name = deviceName,
                        address = deviceAddress,
                        rssi = 0,
                        deviceType = determineShimmerDeviceType(deviceName),
                        isPaired = true,
                        isConnectable = true
                    )
                    
                    discoveredDevices.add(shimmerDevice)
                    discoveryListener?.onShimmerDeviceFound(shimmerDevice)
                    XLog.i(TAG, "Found paired Shimmer device: $deviceName ($deviceAddress)")
                }
            }
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error adding paired devices: ${e.message}", e)
        }
    }
    
    private fun isShimmerDevice(name: String, address: String): Boolean {

        val nameMatch = SHIMMER_NAME_PATTERNS.any { pattern ->
            name.contains(pattern, ignoreCase = true)
        }
        
        val macMatch = SHIMMER_MAC_PREFIXES.any { prefix ->
            address.startsWith(prefix, ignoreCase = true)
        }
        
        return nameMatch || macMatch
    }
    
    private fun determineShimmerDeviceType(name: String): ShimmerDeviceType {
        val nameLower = name.lowercase()
        
        return when {
            nameLower.contains("gsr") || nameLower.contains("eda") -> ShimmerDeviceType.SHIMMER3_GSR_PLUS
            nameLower.contains("imu") || nameLower.contains("accel") || nameLower.contains("gyro") -> ShimmerDeviceType.SHIMMER3_IMU
            nameLower.contains("ecg") || nameLower.contains("ekg") -> ShimmerDeviceType.SHIMMER3_ECG
            nameLower.contains("emg") -> ShimmerDeviceType.SHIMMER3_EMG
            nameLower.contains("shimmer2") -> ShimmerDeviceType.SHIMMER2R
            nameLower.contains("shimmer3") || nameLower.contains("shimmer") -> ShimmerDeviceType.SHIMMER3_GSR_PLUS
            else -> ShimmerDeviceType.UNKNOWN
        }
    }
    
    private fun hasBluetoothPermissions(): Boolean {
        val requiredPermissions = mutableListOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        
        return requiredPermissions.all { permission ->
            ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun getDiscoveredDevices(): List<ShimmerDeviceInfo> = discoveredDevices.toList()
    
    fun isScanning(): Boolean = isScanning
    
    fun getDeviceTypeDescription(type: ShimmerDeviceType): String {
        return when (type) {
            ShimmerDeviceType.SHIMMER3_GSR_PLUS -> "Shimmer3 GSR+"
            ShimmerDeviceType.SHIMMER3_IMU -> "Shimmer3 IMU"
            ShimmerDeviceType.SHIMMER3_ECG -> "Shimmer3 ECG"
            ShimmerDeviceType.SHIMMER3_EMG -> "Shimmer3 EMG"
            ShimmerDeviceType.SHIMMER2R -> "Shimmer2R"
            ShimmerDeviceType.UNKNOWN -> "Unknown Shimmer"
        }
    }
    
    fun clearResults() {
        discoveredDevices.clear()
    }
    
    fun cleanup() {
        stopDiscovery()
        clearResults()
        discoveryListener = null
    }
