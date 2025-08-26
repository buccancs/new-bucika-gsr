package com.topdon.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.util.SparseArray
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.topdon.ble.callback.ScanListener
import com.topdon.ble.util.Logger
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

abstract class AbstractScanner(
    easyBle: EasyBLE,
    protected val bluetoothAdapter: BluetoothAdapter
) : Scanner {
    
    protected val configuration: ScanConfiguration = easyBle.scanConfiguration
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isScanning = false
    private val scanListeners: MutableList<ScanListener> = CopyOnWriteArrayList()
    private val proxyBluetoothProfiles = SparseArray<BluetoothProfile>()
    protected val logger: Logger = easyBle.getLogger()
    private val deviceCreator: DeviceCreator = easyBle.getDeviceCreator()

    override fun addScanListener(listener: ScanListener) {
        if (!scanListeners.contains(listener)) {
            scanListeners.add(listener)
        }
    }

    override fun removeScanListener(listener: ScanListener) {
        scanListeners.remove(listener)
    }

    private fun isLocationEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            locationManager?.isLocationEnabled == true
        } else {
            try {
                val locationMode = Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.LOCATION_MODE
                )
                locationMode != Settings.Secure.LOCATION_MODE_OFF
            } catch (e: Settings.SettingNotFoundException) {
                false
            }
        }
    }

    private fun noLocationPermission(context: Context): Boolean {
        val sdkVersion = context.applicationInfo.targetSdkVersion
        return if (sdkVersion >= 29) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        }
    }

    protected fun handleScanCallback(
        start: Boolean,
        device: Device?,
        isConnectedBySys: Boolean,
        errorCode: Int,
        errorMsg: String
    ) {
        mainHandler.post {
            for (listener in scanListeners) {
                when {
                    device != null -> listener.onScanResult(device, isConnectedBySys)
                    start -> listener.onScanStart()
                    errorCode >= 0 -> listener.onScanError(errorCode, errorMsg)
                    else -> listener.onScanStop()
                }
            }
        }
    }

    @Suppress("all")
    private fun getSystemConnectedDevices(context: Context) {
        try {
            val method = bluetoothAdapter.javaClass.getDeclaredMethod("getConnectionState")
            method.isAccessible = true
            val state = method.invoke(bluetoothAdapter) as Int
            if (state == BluetoothAdapter.STATE_CONNECTED) {
                val devices = bluetoothAdapter.bondedDevices
                for (device in devices) {
                    val isConnectedMethod = device.javaClass.getDeclaredMethod("isConnected")
                    isConnectedMethod.isAccessible = true
                    val isConnected = isConnectedMethod.invoke(device) as Boolean
                    if (isConnected) {
                        parseScanResult(device, true)
                    }
                }
            }
        } catch (ignore: Exception) {
        }

        for (i in 1..21) {
            try {
                getSystemConnectedDevices(context, i)
            } catch (ignore: Exception) {
            }
        }
    }

    private fun getSystemConnectedDevices(context: Context, profile: Int) {
        bluetoothAdapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (proxy == null) return
                proxyBluetoothProfiles.put(profile, proxy)
                synchronized(this@AbstractScanner) {
                    if (!isScanning) return
                }
                try {
                    val devices = proxy.connectedDevices
                    for (device in devices) {
                        parseScanResult(device, true)
                    }
                } catch (ignore: Exception) {
                }
            }

            override fun onServiceDisconnected(profile: Int) {}
        }, profile)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    protected fun parseScanResult(device: BluetoothDevice, result: ScanResult?) {
        if (result == null) {
            parseScanResult(device, false)
        } else {
            val record = result.scanRecord
            parseScanResult(device, false, result, result.rssi, record?.bytes)
        }
    }

    private fun parseScanResult(device: BluetoothDevice, isConnectedBySys: Boolean) {
        parseScanResult(device, isConnectedBySys, null, -120, null)
    }

    protected fun parseScanResult(
        device: BluetoothDevice,
        isConnectedBySys: Boolean,
        result: ScanResult?,
        rssi: Int,
        scanRecord: ByteArray?
    ) {
        if ((configuration.onlyAcceptBleDevice && device.type != BluetoothDevice.DEVICE_TYPE_LE) ||
            !device.address.matches("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$".toRegex())
        ) {
            return
        }
        
        val name = device.name ?: ""
        if (configuration.rssiLowLimit <= rssi) {
            val dev = deviceCreator.create(device, result)
            if (dev != null) {
                dev.name = if (TextUtils.isEmpty(dev.name)) name else dev.name
                dev.rssi = rssi
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    dev.scanResult = result
                }
                dev.scanRecord = scanRecord
                handleScanCallback(false, dev, isConnectedBySys, -1, "")
            }
        }
        val msg = String.format(
            Locale.US,
            "found device! [name: %s, addr: %s]",
            if (TextUtils.isEmpty(name)) "N/A" else name,
            device.address
        )
        logger.log(Log.DEBUG, Logger.TYPE_SCAN_STATE, msg)
    }

    @CallSuper
    override fun startScan(context: Context) {
        synchronized(this) {
            if (!isBtEnabled() || (getType() != ScannerType.CLASSIC && isScanning) || !isReady()) {
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!isLocationEnabled(context)) {
                    val errorMsg = "Unable to scan for Bluetooth devices, the phone's location service is not turned on."
                    handleScanCallback(false, null, false, ScanListener.ERROR_LOCATION_SERVICE_CLOSED, errorMsg)
                    logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, errorMsg)
                    return
                } else if (noLocationPermission(context)) {
                    val errorMsg = "Unable to scan for Bluetooth devices, lack location permission."
                    handleScanCallback(false, null, false, ScanListener.ERROR_LACK_LOCATION_PERMISSION, errorMsg)
                    logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, errorMsg)
                    return
                }
            }
            if (getType() != ScannerType.CLASSIC) {
                isScanning = true
            }
        }
        if (getType() != ScannerType.CLASSIC) {
            handleScanCallback(true, null, false, -1, "")
        }
        if (configuration.acceptSysConnectedDevice) {
            getSystemConnectedDevices(context)
        }
        performStartScan()
        if (getType() != ScannerType.CLASSIC) {
            mainHandler.postDelayed(stopScanRunnable, configuration.scanPeriodMillis.toLong())
        }
    }

    override fun isScanning(): Boolean = isScanning

    @CallSuper
    protected open fun setScanning(scanning: Boolean) {
        synchronized(this) {
            isScanning = scanning
        }
    }

    @CallSuper
    override fun stopScan(quietly: Boolean) {
        mainHandler.removeCallbacks(stopScanRunnable)
        val size = proxyBluetoothProfiles.size()
        for (i in 0 until size) {
            try {
                bluetoothAdapter.closeProfileProxy(
                    proxyBluetoothProfiles.keyAt(i),
                    proxyBluetoothProfiles.valueAt(i)
                )
            } catch (ignore: Exception) {
            }
        }
        proxyBluetoothProfiles.clear()
        if (isBtEnabled()) {
            performStopScan()
        }
        if (getType() != ScannerType.CLASSIC) {
            synchronized(this) {
                if (isScanning) {
                    isScanning = false
                    if (!quietly) {
                        handleScanCallback(false, null, false, -1, "")
                    }
                }
            }
        }
    }

    private val stopScanRunnable = Runnable { stopScan(false) }

    private fun isBtEnabled(): Boolean {
        if (bluetoothAdapter.isEnabled) {
            try {
                val method = bluetoothAdapter.javaClass.getDeclaredMethod("isLeEnabled")
                method.isAccessible = true
                return method.invoke(bluetoothAdapter) as Boolean
            } catch (e: Exception) {
                val state = bluetoothAdapter.state
                return state == BluetoothAdapter.STATE_ON || state == 15
            }
        }
        return false
    }

    override fun onBluetoothOff() {
        synchronized(this) {
            isScanning = false
        }
        handleScanCallback(false, null, false, -1, "")
    }

    override fun release() {
        stopScan(false)
        scanListeners.clear()
    }

    protected abstract fun isReady(): Boolean
    protected abstract fun performStartScan()
    protected abstract fun performStopScan()
}
