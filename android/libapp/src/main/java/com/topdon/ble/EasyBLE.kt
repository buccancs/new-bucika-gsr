package com.topdon.ble

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.topdon.ble.callback.ScanListener
import com.topdon.ble.util.DefaultLogger
import com.topdon.ble.util.Logger
import com.topdon.commons.observer.Observable
import com.topdon.commons.poster.MethodInfo
import com.topdon.commons.poster.PosterDispatcher
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService

class EasyBLE internal constructor(builder: EasyBLEBuilder) {
    
    companion object {
        @Volatile
        var instance: EasyBLE? = null
            internal set
        
        private val DEFAULT_BUILDER = EasyBLEBuilder()

        @JvmStatic
        fun getInstance(): EasyBLE {
            return instance ?: synchronized(EasyBLE::class.java) {
                instance ?: EasyBLE(DEFAULT_BUILDER).also { instance = it }
            }
        }

        @JvmStatic
        fun getBuilder(): EasyBLEBuilder = EasyBLEBuilder()
    }

    private val executorService: ExecutorService
    private val posterDispatcher: PosterDispatcher
    private val bondController: BondController?
    private val deviceCreator: DeviceCreator
    val observable: Observable
    private val logger: Logger
    private val scannerType: ScannerType?
    val scanConfiguration: ScanConfiguration
    private var scanner: Scanner? = null
    private var application: Application? = null
    private var isInitialized = false
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private val connectionMap = ConcurrentHashMap<String, Connection>()
    private val addressList = CopyOnWriteArrayList<String>()
    private val internalObservable: Boolean

    init {
        tryGetApplication()
        bondController = builder.bondController
        scannerType = builder.scannerType
        deviceCreator = builder.deviceCreator ?: DefaultDeviceCreator()
        scanConfiguration = builder.scanConfiguration ?: ScanConfiguration()
        logger = builder.logger ?: DefaultLogger("EasyBLE")
        
        if (builder.observable != null) {
            internalObservable = false
            observable = builder.observable!!
            posterDispatcher = observable.getPosterDispatcher()
            executorService = posterDispatcher.getExecutorService()
        } else {
            internalObservable = true
            executorService = builder.executorService
            posterDispatcher = PosterDispatcher(executorService, builder.methodDefaultThreadMode)
            observable = Observable(posterDispatcher, builder.isObserveAnnotationRequired)
        }
    }

    @Nullable
    internal fun getContext(): Context? {
        if (application == null) {
            tryAutoInit()
        }
        return application
    }

    @SuppressLint("PrivateApi")
    private fun tryGetApplication() {
        try {
            val cls = Class.forName("android.app.ActivityThread")
            val method = cls.getMethod("currentActivityThread")
            method.isAccessible = true
            val acThread = method.invoke(null)
            val appMethod = acThread.javaClass.getMethod("getApplication")
            application = appMethod.invoke(acThread) as Application
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Nullable
    fun getBluetoothAdapter(): BluetoothAdapter? = bluetoothAdapter

    internal fun getExecutorService(): ExecutorService = executorService

    internal fun getPosterDispatcher(): PosterDispatcher = posterDispatcher

    internal fun getDeviceCreator(): DeviceCreator = deviceCreator

    internal fun getObservable(): Observable = observable

    internal fun getLogger(): Logger = logger

    fun getScannerType(): ScannerType? = scannerType

    fun isInitialized(): Boolean = isInitialized && application != null && instance != null

    fun isBluetoothOn(): Boolean = bluetoothAdapter?.isEnabled == true

    private inner class InnerBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    bluetoothAdapter?.let { adapter ->
                        observable.notifyObservers(MethodInfoGenerator.onBluetoothAdapterStateChanged(adapter.state))
                        when (adapter.state) {
                            BluetoothAdapter.STATE_OFF -> {
                                logger.log(Log.DEBUG, Logger.TYPE_GENERAL, "蓝牙关闭了")
                                scanner?.onBluetoothOff()
                                disconnectAllConnections()
                            }
                            BluetoothAdapter.STATE_ON -> {
                                logger.log(Log.DEBUG, Logger.TYPE_GENERAL, "蓝牙开启了")
                                connectionMap.values.forEach { connection ->
                                    if (connection.isAutoReconnectEnabled()) {
                                        connection.reconnect()
                                    }
                                }
                            }
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    (scanner as? ClassicScanner)?.setScanning(true)
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    (scanner as? ClassicScanner)?.setScanning(false)
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null && scanner is ClassicScanner) {
                        val rssi = intent.extras?.getShort(BluetoothDevice.EXTRA_RSSI) ?: -120
                        (scanner as ClassicScanner).parseScanResult(device, false, null, rssi.toInt(), null)
                    }
                }
            }
        }
    }

    @Synchronized
    fun initialize(application: Application) {
        if (isInitialized()) return
        
        Inspector.requireNonNull(application, "application can't be null")
        this.application = application

        if (!application.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return
        }

        val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter ?: return

        if (broadcastReceiver == null) {
            broadcastReceiver = InnerBroadcastReceiver()
            val filter = IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothDevice.ACTION_FOUND)
            }
            application.registerReceiver(broadcastReceiver, filter)
        }
        isInitialized = true
    }

    @Synchronized
    private fun checkStatus(): Boolean {
        Inspector.requireNonNull(instance, "EasyBLE instance has been destroyed!")
        return when {
            !isInitialized -> tryAutoInit()
            application == null -> tryAutoInit()
            else -> true
        }
    }

    private fun tryAutoInit(): Boolean {
        tryGetApplication()
        application?.let { initialize(it) }
        return isInitialized()
    }

    fun setLogEnabled(isEnabled: Boolean) {
        logger.setEnabled(isEnabled)
    }

    @Synchronized
    fun release() {
        broadcastReceiver?.let { receiver ->
            application?.unregisterReceiver(receiver)
            broadcastReceiver = null
        }
        isInitialized = false
        scanner?.release()
        releaseAllConnections()
        if (internalObservable) {
            observable.unregisterAll()
            posterDispatcher.clearTasks()
        }
    }

    fun destroy() {
        release()
        synchronized(EasyBLE::class.java) {
            instance = null
        }
    }

    fun registerObserver(observer: EventObserver) {
        if (checkStatus()) {
            observable.registerObserver(observer)
        }
    }

    fun isObserverRegistered(observer: EventObserver): Boolean = observable.isRegistered(observer)

    fun unregisterObserver(observer: EventObserver) {
        observable.unregisterObserver(observer)
    }

    fun notifyObservers(info: MethodInfo) {
        if (checkStatus()) {
            observable.notifyObservers(info)
        }
    }

    private fun checkAndInstanceScanner() {
        if (scanner == null) {
            synchronized(this) {
                if (bluetoothAdapter != null && scanner == null) {
                    scanner = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                            when (scannerType) {
                                ScannerType.LEGACY -> LegacyScanner(this, bluetoothAdapter!!)
                                ScannerType.CLASSIC -> ClassicScanner(this, bluetoothAdapter!!)
                                else -> LeScanner(this, bluetoothAdapter!!)
                            }
                        }
                        scannerType == ScannerType.CLASSIC -> ClassicScanner(this, bluetoothAdapter!!)
                        else -> LegacyScanner(this, bluetoothAdapter!!)
                    }
                }
            }
        }
    }

    fun addScanListener(listener: ScanListener) {
        checkAndInstanceScanner()
        if (checkStatus()) {
            scanner?.addScanListener(listener)
        }
    }

    fun removeScanListener(listener: ScanListener) {
        scanner?.removeScanListener(listener)
    }

    fun isScanning(): Boolean = scanner?.isScanning() == true

    fun startScan() {
        checkAndInstanceScanner()
        if (checkStatus() && application != null) {
            scanner?.startScan(application!!)
        }
    }

    fun stopScan() {
        if (checkStatus()) {
            scanner?.stopScan(false)
        }
    }

    fun stopScanQuietly() {
        if (checkStatus()) {
            scanner?.stopScan(true)
        }
    }

    @Nullable
    fun connect(address: String): Connection? = connect(address, null, null)

    @Nullable
    fun connect(address: String, configuration: ConnectionConfiguration?): Connection? = 
        connect(address, configuration, null)

    @Nullable
    fun connect(address: String, observer: EventObserver?): Connection? = 
        connect(address, null, observer)

    @Nullable
    fun connect(
        address: String,
        configuration: ConnectionConfiguration?,
        observer: EventObserver?
    ): Connection? {
        if (checkStatus()) {
            Inspector.requireNonNull(address, "address can't be null")
            val remoteDevice = bluetoothAdapter?.getRemoteDevice(address)
            if (remoteDevice != null) {
                return connect(Device(remoteDevice), configuration, observer)
            }
        }
        return null
    }

    @Nullable
    fun connect(device: Device): Connection? = connect(device, null, null)

    @Nullable
    fun connect(device: Device, configuration: ConnectionConfiguration?): Connection? = 
        connect(device, configuration, null)

    @Nullable
    fun connect(device: Device, observer: EventObserver?): Connection? = 
        connect(device, null, observer)

    @Nullable
    @Synchronized
    fun connect(
        device: Device,
        configuration: ConnectionConfiguration?,
        observer: EventObserver?
    ): Connection? {
        if (!checkStatus()) return null
        
        Inspector.requireNonNull(device, "device can't be null")
        connectionMap.remove(device.address)?.releaseNoEvent()

        val isConnectable = device.isConnectable()
        if (isConnectable != false) {
            var connectDelay = 0
            if (bondController?.accept(device) == true) {
                val remoteDevice = bluetoothAdapter?.getRemoteDevice(device.address)
                if (remoteDevice?.bondState != BluetoothDevice.BOND_BONDED) {
                    connectDelay = if (createBond(device.address)) 1500 else 0
                }
            }
            val connection = ConnectionImpl(this, bluetoothAdapter!!, device, configuration, connectDelay, observer)
            connectionMap[device.address] = connection
            addressList.add(device.address)
            return connection
        } else {
            val message = String.format(Locale.US, 
                "connect failed! [type: unconnectable, name: %s, addr: %s]", 
                device.name, device.address)
            logger.log(Log.ERROR, Logger.TYPE_CONNECTION_STATE, message)
            observer?.let { 
                posterDispatcher.post(it, MethodInfoGenerator.onConnectFailed(device, Connection.CONNECT_FAIL_TYPE_CONNECTION_IS_UNSUPPORTED))
            }
            observable.notifyObservers(MethodInfoGenerator.onConnectFailed(device, Connection.CONNECT_FAIL_TYPE_CONNECTION_IS_UNSUPPORTED))
        }
        return null
    }

    @NonNull
    fun getConnections(): Collection<Connection> = connectionMap.values

    @NonNull
    fun getOrderedConnections(): List<Connection> {
        return addressList.mapNotNull { connectionMap[it] }
    }

    @Nullable
    fun getFirstConnection(): Connection? = 
        addressList.firstOrNull()?.let { connectionMap[it] }

    @Nullable
    fun getLastConnection(): Connection? = 
        addressList.lastOrNull()?.let { connectionMap[it] }

    @Nullable
    fun getConnection(device: Device?): Connection? = 
        device?.address?.let { connectionMap[it] }

    @Nullable
    fun getConnection(address: String?): Connection? = 
        address?.let { connectionMap[it] }

    fun disconnectConnection(device: Device?) {
        if (checkStatus()) {
            device?.address?.let { connectionMap[it]?.disconnect() }
        }
    }

    fun disconnectConnection(address: String?) {
        if (checkStatus()) {
            address?.let { connectionMap[it]?.disconnect() }
        }
    }

    fun disconnectAllConnections() {
        if (checkStatus()) {
            connectionMap.values.forEach { it.disconnect() }
        }
    }

    fun releaseAllConnections() {
        if (checkStatus()) {
            connectionMap.values.forEach { it.release() }
            connectionMap.clear()
            addressList.clear()
        }
    }

    fun releaseConnection(address: String?) {
        if (checkStatus() && address != null) {
            addressList.remove(address)
            connectionMap.remove(address)?.release()
        }
    }

    fun releaseConnection(device: Device?) {
        if (checkStatus() && device != null) {
            addressList.remove(device.address)
            connectionMap.remove(device.address)?.release()
        }
    }

    fun reconnectAll() {
        if (checkStatus()) {
            connectionMap.values.forEach { connection ->
                if (connection.connectionState != ConnectionState.SERVICE_DISCOVERED) {
                    connection.reconnect()
                }
            }
        }
    }

    fun reconnect(device: Device?) {
        if (checkStatus() && device != null) {
            val connection = connectionMap[device.address]
            if (connection?.connectionState != ConnectionState.SERVICE_DISCOVERED) {
                connection?.reconnect()
            }
        }
    }

    fun getBondState(address: String): Int {
        checkStatus()
        return try {
            bluetoothAdapter?.getRemoteDevice(address)?.bondState ?: BluetoothDevice.BOND_NONE
        } catch (e: Exception) {
            BluetoothDevice.BOND_NONE
        }
    }

    fun createBond(address: String): Boolean {
        checkStatus()
        return try {
            val remoteDevice = bluetoothAdapter?.getRemoteDevice(address)
            remoteDevice?.bondState != BluetoothDevice.BOND_NONE || remoteDevice?.createBond() == true
        } catch (ignore: Exception) {
            false
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun clearBondDevices(filter: RemoveBondFilter?) {
        checkStatus()
        bluetoothAdapter?.bondedDevices?.forEach { device ->
            if (filter?.accept(device) != false) {
                try {
                    device.javaClass.getMethod("removeBond").invoke(device)
                } catch (ignore: Exception) {
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun removeBond(address: String) {
        checkStatus()
        try {
            val remoteDevice = bluetoothAdapter?.getRemoteDevice(address)
            if (remoteDevice?.bondState != BluetoothDevice.BOND_NONE) {
                remoteDevice?.javaClass?.getMethod("removeBond")?.invoke(remoteDevice)
            }
        } catch (ignore: Exception) {
        }
    }
}