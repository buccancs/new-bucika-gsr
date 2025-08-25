package com.topdon.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.util.Pair
import com.topdon.ble.callback.RequestCallback
import com.topdon.ble.callback.ScanListener
import com.topdon.ble.util.HexUtil
import com.topdon.ble.util.Logger
import com.topdon.commons.observer.Observable
import com.topdon.commons.poster.MethodInfo
import com.topdon.commons.poster.PosterDispatcher
import com.topdon.commons.util.MathUtils
import com.topdon.commons.util.StringUtils
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

@SuppressLint("MissingPermission")
internal class ConnectionImpl(
    private val easyBle: EasyBLE,
    private val bluetoothAdapter: BluetoothAdapter,
    private val device: Device,
    configuration: ConnectionConfiguration?,
    connectDelay: Int,
    private val observer: EventObserver?
) : Connection, ScanListener {

    companion object {
        private const val MSG_REQUEST_TIMEOUT = 0
        private const val MSG_CONNECT = 1
        private const val MSG_DISCONNECT = 2
        private const val MSG_REFRESH = 3
        private const val MSG_TIMER = 4
        private const val MSG_DISCOVER_SERVICES = 6
        private const val MSG_ON_CONNECTION_STATE_CHANGE = 7
        private const val MSG_ON_SERVICES_DISCOVERED = 8

        private const val MSG_ARG_NONE = 0
        private const val MSG_ARG_RECONNECT = 1
    }

    private val configuration: ConnectionConfiguration = configuration ?: ConnectionConfiguration()
    private var bluetoothGatt: BluetoothGatt? = null
    private val requestQueue = mutableListOf<GenericRequest>()
    private var currentRequest: GenericRequest? = null
    private var isReleased = false
    private val connHandler: Handler
    private var connStartTime: Long = 0
    private var refreshCount = 0
    private var tryReconnectCount = 0
    private var lastConnectionState: ConnectionState? = null
    private var reconnectImmediatelyCount = 0
    private var refreshing = false
    private var isActiveDisconnect = false
    private var lastScanStopTime: Long = 0
    private val logger: Logger = easyBle.getLogger()
    private val observable: Observable = easyBle.getObservable()
    private val posterDispatcher: PosterDispatcher = easyBle.getPosterDispatcher()
    private val gattCallback = BleGattCallback()
    private var mtu = 23
    private var originCallback: BluetoothGattCallback? = null

    init {
        connHandler = ConnHandler(this)
        connStartTime = System.currentTimeMillis()
        connHandler.sendEmptyMessageDelayed(MSG_CONNECT, connectDelay.toLong())
        connHandler.sendEmptyMessageDelayed(MSG_TIMER, connectDelay.toLong())
        easyBle.addScanListener(this)
    }

    override fun onScanStart() {}

    override fun onScanStop() {
        synchronized(this) {
            lastScanStopTime = System.currentTimeMillis()
        }
    }

    override fun onScanResult(device: Device, isConnectedBySys: Boolean) {
        synchronized(this) {
            if (!isReleased && this.device == device && 
                this.device.connectionState == ConnectionState.SCANNING_FOR_RECONNECTION) {
                connHandler.sendEmptyMessage(MSG_CONNECT)
            }
        }
    }

    override fun onScanError(errorCode: Int, errorMsg: String) {}

    override fun setBluetoothGattCallback(callback: BluetoothGattCallback?) {
        originCallback = callback
    }

    override fun hasProperty(service: UUID, characteristic: UUID, property: Int): Boolean {
        val charac = getCharacteristic(service, characteristic) ?: return false
        return (charac.properties and property) != 0
    }

    private inner class BleGattCallback : BluetoothGattCallback() {
        
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            originCallback?.let { callback ->
                easyBle.getExecutorService().execute { callback.onConnectionStateChange(gatt, status, newState) }
            }
            if (!isReleased) {
                Message.obtain(connHandler, MSG_ON_CONNECTION_STATE_CHANGE, status, newState).sendToTarget()
            } else {
                closeGatt(gatt)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            originCallback?.let { callback ->
                easyBle.getExecutorService().execute { callback.onServicesDiscovered(gatt, status) }
            }
            if (!isReleased) {
                Message.obtain(connHandler, MSG_ON_SERVICES_DISCOVERED, status, 0).sendToTarget()
            } else {
                closeGatt(gatt)
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Log.e("bcf", "onCharacteristicRead  status: $status  value: ${HexUtil.bytesToHexString(characteristic.value)}")
            originCallback?.let { callback ->
                easyBle.getExecutorService().execute { callback.onCharacteristicRead(gatt, characteristic, status) }
            }
            currentRequest?.let { request ->
                if (request.type == RequestType.READ_CHARACTERISTIC) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        notifyCharacteristicRead(request, characteristic.value)
                    } else {
                        handleGattStatusFailed()
                    }
                    executeNextRequest()
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            originCallback?.let { callback ->
                easyBle.getExecutorService().execute { callback.onCharacteristicWrite(gatt, characteristic, status) }
            }
            
            currentRequest?.let { request ->
                if (request.type == RequestType.WRITE_CHARACTERISTIC && request.writeOptions.isWaitWriteResult) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        handleSuccessfulWrite(request, characteristic)
                    } else {
                        handleFailedCallback(request, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, true)
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            originCallback?.let { callback ->
                easyBle.getExecutorService().execute { callback.onCharacteristicChanged(gatt, characteristic) }
            }
            notifyCharacteristicChanged(characteristic)
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            device.setRssi(rssi)
            originCallback?.let { callback ->
                easyBle.getExecutorService().execute { callback.onReadRemoteRssi(gatt, rssi, status) }
            }
            currentRequest?.let { request ->
                if (request.type == RequestType.READ_RSSI) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        notifyRssiRead(request, rssi)
                    } else {
                        handleGattStatusFailed()
                    }
                    executeNextRequest()
                }
            }
        }

        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            originCallback?.let { callback ->
                easyBle.getExecutorService().execute { callback.onDescriptorRead(gatt, descriptor, status) }
            }
            currentRequest?.let { request ->
                if (request.type == RequestType.READ_DESCRIPTOR) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        notifyDescriptorRead(request, descriptor.value)
                    } else {
                        handleGattStatusFailed()
                    }
                    executeNextRequest()
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            originCallback?.let { callback ->
                easyBle.getExecutorService().execute { callback.onDescriptorWrite(gatt, descriptor, status) }
            }
            currentRequest?.let { request ->
                if (request.type == RequestType.SET_NOTIFICATION || request.type == RequestType.SET_INDICATION) {
                    val localDescriptor = getDescriptor(
                        descriptor.characteristic.service.uuid,
                        descriptor.characteristic.uuid,
                        clientCharacteristicConfig
                    )
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        handleGattStatusFailed()
                        localDescriptor?.value = request.descriptorTemp
                    } else {
                        notifyNotificationChanged(request, (request.value as Int) == 1)
                    }
                    executeNextRequest()
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            originCallback?.let { callback ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    easyBle.getExecutorService().execute { callback.onMtuChanged(gatt, mtu, status) }
                }
            }
            currentRequest?.let { request ->
                if (request.type == RequestType.CHANGE_MTU) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        this@ConnectionImpl.mtu = mtu
                        notifyMtuChanged(request, mtu)
                    } else {
                        handleGattStatusFailed()
                    }
                    executeNextRequest()
                }
            }
        }

        override fun onPhyRead(gatt: BluetoothGatt, txPhy: Int, rxPhy: Int, status: Int) {
            originCallback?.let { callback ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    easyBle.getExecutorService().execute { callback.onPhyRead(gatt, txPhy, rxPhy, status) }
                }
            }
            handlePhyChange(true, txPhy, rxPhy, status)
        }

        override fun onPhyUpdate(gatt: BluetoothGatt, txPhy: Int, rxPhy: Int, status: Int) {
            originCallback?.let { callback ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    easyBle.getExecutorService().execute { callback.onPhyRead(gatt, txPhy, rxPhy, status) }
                }
            }
            handlePhyChange(false, txPhy, rxPhy, status)
        }
    }

    private fun handleSuccessfulWrite(request: GenericRequest, characteristic: BluetoothGattCharacteristic) {
        if (logger.isEnabled()) {
            val data = request.value as ByteArray
            val packageSize = request.writeOptions.packageSize
            val total = data.size / packageSize + if (data.size % packageSize == 0) 0 else 1
            val progress = when {
                request.remainQueue == null || request.remainQueue!!.isEmpty() -> total
                else -> data.size / packageSize - request.remainQueue!!.size + 1
            }
            printWriteLog(request, progress, total, characteristic.value)
        }
        
        if (request.remainQueue == null || request.remainQueue!!.isEmpty()) {
            notifyCharacteristicWrite(request, request.value as ByteArray)
            executeNextRequest()
        } else {
            connHandler.removeMessages(MSG_REQUEST_TIMEOUT)
            connHandler.sendMessageDelayed(
                Message.obtain(connHandler, MSG_REQUEST_TIMEOUT, request),
                configuration.requestTimeoutMillis.toLong()
            )
            val delay = request.writeOptions.packageWriteDelayMillis
            if (delay > 0) {
                try {
                    Thread.sleep(delay.toLong())
                } catch (ignore: InterruptedException) {}
                if (request != currentRequest) return
            }
            request.sendingBytes = request.remainQueue!!.remove()
            write(request, characteristic, request.sendingBytes)
        }
    }

    private fun doOnConnectionStateChange(status: Int, newState: Int) {
        bluetoothGatt?.let { gatt ->
            when {
                status == BluetoothGatt.GATT_SUCCESS -> {
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            logD(Logger.TYPE_CONNECTION_STATE, "connected! [name: %s, addr: %s]", device.name, device.address)
                            device.connectionState = ConnectionState.CONNECTED
                            sendConnectionCallback()
                            connHandler.sendEmptyMessageDelayed(MSG_DISCOVER_SERVICES, configuration.discoverServicesDelayMillis.toLong())
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            logD(Logger.TYPE_CONNECTION_STATE, "disconnected! [name: %s, addr: %s, autoReconnEnable: %s]",
                                device.name, device.address, configuration.isAutoReconnect)
                            clearRequestQueueAndNotify()
                            notifyDisconnected()
                        }
                    }
                }
                else -> {
                    logE(Logger.TYPE_CONNECTION_STATE, "GATT error! [status: %d, name: %s, addr: %s]",
                        status, device.name, device.address)
                    if (status == 133) {
                        doClearTaskAndRefresh()
                    } else {
                        clearRequestQueueAndNotify()
                        notifyDisconnected()
                    }
                }
            }
        }
    }

    private fun doOnServicesDiscovered(status: Int) {
        bluetoothGatt?.let { gatt ->
            val services = gatt.services
            if (status == BluetoothGatt.GATT_SUCCESS) {
                logD(Logger.TYPE_CONNECTION_STATE, "services discovered! [name: %s, addr: %s, size: %d]", 
                    device.name, device.address, services.size)
                if (services.isEmpty()) {
                    doClearTaskAndRefresh()
                } else {
                    refreshCount = 0
                    tryReconnectCount = 0
                    reconnectImmediatelyCount = 0
                    device.connectionState = ConnectionState.SERVICE_DISCOVERED
                    sendConnectionCallback()
                }
            } else {
                doClearTaskAndRefresh()
                logE(Logger.TYPE_CONNECTION_STATE, "GATT error! [status: %d, name: %s, addr: %s]",
                    status, device.name, device.address)
            }
        }
    }

    private fun doDiscoverServices() {
        bluetoothGatt?.let { gatt ->
            gatt.discoverServices()
            device.connectionState = ConnectionState.SERVICE_DISCOVERING
            sendConnectionCallback()
        } ?: notifyDisconnected()
    }

    private fun doTimer() {
        if (isReleased) return

        if (device.connectionState != ConnectionState.SERVICE_DISCOVERED && !refreshing && !isActiveDisconnect) {
            if (device.connectionState != ConnectionState.DISCONNECTED) {
                if (System.currentTimeMillis() - connStartTime > configuration.connectTimeoutMillis) {
                    connStartTime = System.currentTimeMillis()
                    logE(Logger.TYPE_CONNECTION_STATE, "connect timeout! [name: %s, addr: %s]", device.name, device.address)
                    
                    val timeoutType = when (device.connectionState) {
                        ConnectionState.SCANNING_FOR_RECONNECTION -> TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE
                        ConnectionState.CONNECTING -> TIMEOUT_TYPE_CANNOT_CONNECT
                        else -> TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES
                    }
                    
                    val methodInfo = MethodInfoGenerator.onConnectTimeout(device, timeoutType)
                    observable.notifyObservers(methodInfo)
                    observer?.let { posterDispatcher.post(it, methodInfo) }
                    
                    val infinite = configuration.tryReconnectMaxTimes == ConnectionConfiguration.TRY_RECONNECT_TIMES_INFINITE
                    if (configuration.isAutoReconnect && (infinite || tryReconnectCount < configuration.connectTimeoutMillis)) {
                        doDisconnect(true)
                    } else {
                        doDisconnect(false)
                        val failMethodInfo = MethodInfoGenerator.onConnectFailed(device, CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION)
                        observer?.let { posterDispatcher.post(it, failMethodInfo) }
                        observable.notifyObservers(failMethodInfo)
                        logE(Logger.TYPE_CONNECTION_STATE, "connect failed! [type: maximun reconnection, name: %s, addr: %s]",
                            device.name, device.address)
                    }
                }
            } else if (configuration.isAutoReconnect) {
                doDisconnect(true)
            }
        }
        connHandler.sendEmptyMessageDelayed(MSG_TIMER, 500)
    }

    private val connectRunnable = Runnable {
        if (!isReleased) {
            easyBle.stopScan()
            bluetoothGatt = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    device.getOriginDevice().connectGatt(easyBle.getContext(), false, gattCallback,
                        configuration.transport, configuration.phy)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    device.getOriginDevice().connectGatt(easyBle.getContext(), false, gattCallback,
                        configuration.transport)
                }
                else -> {
                    device.getOriginDevice().connectGatt(easyBle.getContext(), false, gattCallback)
                }
            }
        }
    }

    private fun doConnect() {
        cancelRefreshState()
        device.connectionState = ConnectionState.CONNECTING
        sendConnectionCallback()
        logD(Logger.TYPE_CONNECTION_STATE, "connecting [name: %s, addr: %s]", device.name, device.address)
        connHandler.postDelayed(connectRunnable, 500)
    }

    private fun doDisconnect(reconnect: Boolean) {
        clearRequestQueueAndNotify()
        connHandler.removeCallbacks(connectRunnable)
        connHandler.removeMessages(MSG_DISCOVER_SERVICES)
        
        bluetoothGatt?.let { gatt ->
            closeGatt(gatt)
            bluetoothGatt = null
        }
        
        device.connectionState = ConnectionState.DISCONNECTED
        
        if (bluetoothAdapter.isEnabled && reconnect && !isReleased) {
            when {
                reconnectImmediatelyCount < configuration.reconnectImmediatelyMaxTimes -> {
                    tryReconnectCount++
                    reconnectImmediatelyCount++
                    connStartTime = System.currentTimeMillis()
                    doConnect()
                    return
                }
                canScanReconnect() -> {
                    tryScanReconnect()
                    return
                }
            }
        }
        sendConnectionCallback()
    }

    private fun doClearTaskAndRefresh() {
        clearRequestQueueAndNotify()
        doRefresh(true)
    }

    private fun doRefresh(isAuto: Boolean) {
        logD(Logger.TYPE_CONNECTION_STATE, "refresh GATT! [name: %s, addr: %s]", device.name, device.address)
        connStartTime = System.currentTimeMillis()
        
        bluetoothGatt?.let { gatt ->
            try {
                gatt.disconnect()
            } catch (ignore: Exception) {}

            if (isAuto) {
                if (refreshCount <= 5) {
                    refreshing = doRefresh()
                }
                refreshCount++
            } else {
                refreshing = doRefresh()
            }
            
            if (refreshing) {
                connHandler.postDelayed(::cancelRefreshState, 2000)
            } else {
                closeGatt(gatt)
                bluetoothGatt = null
            }
        }
        notifyDisconnected()
    }

    private fun cancelRefreshState() {
        if (refreshing) {
            refreshing = false
            bluetoothGatt?.let { gatt ->
                closeGatt(gatt)
                bluetoothGatt = null
            }
        }
    }

    private fun tryScanReconnect() {
        if (!isReleased) {
            connStartTime = System.currentTimeMillis()
            easyBle.stopScan()
            device.connectionState = ConnectionState.SCANNING_FOR_RECONNECTION
            logD(Logger.TYPE_CONNECTION_STATE, "scanning for reconnection [name: %s, addr: %s]", device.name, device.address)
            easyBle.startScan()
        }
    }

    private fun canScanReconnect(): Boolean {
        val duration = System.currentTimeMillis() - lastScanStopTime
        val parameters = configuration.scanIntervalPairsInAutoReconnection
        val sortedParameters = parameters.sortedWith { o1, o2 ->
            when {
                o1?.first == null -> 1
                o2?.first == null -> -1
                else -> o2.first!!.compareTo(o1.first!!)
            }
        }
        return sortedParameters.any { pair ->
            pair.first != null && pair.second != null && 
            tryReconnectCount >= pair.first!! && duration >= pair.second!!
        }
    }

    private fun closeGatt(gatt: BluetoothGatt) {
        try {
            gatt.disconnect()
        } catch (ignore: Exception) {}
        try {
            gatt.close()
        } catch (ignore: Exception) {}
    }

    private fun notifyDisconnected() {
        device.connectionState = ConnectionState.DISCONNECTED
        sendConnectionCallback()
    }

    private fun sendConnectionCallback() {
        if (lastConnectionState != device.connectionState) {
            lastConnectionState = device.connectionState
            val methodInfo = MethodInfoGenerator.onConnectionStateChanged(device)
            observer?.let { posterDispatcher.post(it, methodInfo) }
            observable.notifyObservers(methodInfo)
        }
    }

    private fun write(request: GenericRequest, characteristic: BluetoothGattCharacteristic, value: ByteArray): Boolean {
        characteristic.value = value
        val writeType = request.writeOptions.writeType
        if (writeType in listOf(
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
            BluetoothGattCharacteristic.WRITE_TYPE_SIGNED,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)) {
            characteristic.writeType = writeType
        }
        
        bluetoothGatt?.let { gatt ->
            return if (gatt.writeCharacteristic(characteristic)) {
                true
            } else {
                handleWriteFailed(request)
                false
            }
        } ?: run {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_GATT_IS_NULL, true)
            return false
        }
    }

    private fun handleWriteFailed(request: GenericRequest) {
        connHandler.removeMessages(MSG_REQUEST_TIMEOUT)
        request.remainQueue = null
        handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
    }

    private fun enableNotificationOrIndicationFail(
        enable: Boolean, 
        notification: Boolean, 
        characteristic: BluetoothGattCharacteristic
    ): Boolean {
        if (!bluetoothAdapter.isEnabled || bluetoothGatt?.setCharacteristicNotification(characteristic, enable) != true) {
            return true
        }
        
        val descriptor = characteristic.getDescriptor(clientCharacteristicConfig) ?: return true
        val originValue = descriptor.value
        
        currentRequest?.let { request ->
            if (request.type == RequestType.SET_NOTIFICATION || request.type == RequestType.SET_INDICATION) {
                request.descriptorTemp = originValue
            }
        }
        
        when {
            enable -> {
                descriptor.value = if (notification) {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                }
            }
            else -> {
                descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            }
        }
        
        val writeType = characteristic.writeType
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val result = bluetoothGatt?.writeDescriptor(descriptor) == true
        if (!enable) {
            descriptor.value = originValue
        }
        characteristic.writeType = writeType
        return !result
    }

    private class ConnHandler(connection: ConnectionImpl) : Handler(Looper.getMainLooper()) {
        private val weakRef = WeakReference(connection)

        override fun handleMessage(msg: Message) {
            val connection = weakRef.get() ?: return
            if (connection.isReleased) return

            when (msg.what) {
                MSG_REQUEST_TIMEOUT -> {
                    val request = msg.obj as GenericRequest
                    if (connection.currentRequest == request) {
                        connection.handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_TIMEOUT, false)
                        connection.executeNextRequest()
                    }
                }
                MSG_CONNECT -> {
                    if (connection.bluetoothAdapter.isEnabled) {
                        connection.doConnect()
                    }
                }
                MSG_DISCONNECT -> {
                    val reconnect = msg.arg1 == MSG_ARG_RECONNECT && connection.bluetoothAdapter.isEnabled
                    connection.doDisconnect(reconnect)
                }
                MSG_REFRESH -> connection.doRefresh(false)
                MSG_TIMER -> connection.doTimer()
                MSG_DISCOVER_SERVICES -> {
                    if (connection.bluetoothAdapter.isEnabled) {
                        connection.doDiscoverServices()
                    }
                }
                MSG_ON_SERVICES_DISCOVERED -> {
                    if (connection.bluetoothAdapter.isEnabled) {
                        connection.doOnServicesDiscovered(msg.arg1)
                    }
                }
                MSG_ON_CONNECTION_STATE_CHANGE -> {
                    if (connection.bluetoothAdapter.isEnabled) {
                        connection.doOnConnectionStateChange(msg.arg1, msg.arg2)
                    }
                }
            }
        }
    }

    // Additional methods continue here - this file is getting very long
    // Let me continue with the remaining methods in the next part...

    private fun enqueue(request: GenericRequest) {
        if (isReleased) {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_CONNECTION_RELEASED, false)
        } else {
            synchronized(this) {
                if (currentRequest == null) {
                    executeRequest(request)
                } else {
                    var index = -1
                    for (i in requestQueue.indices) {
                        val req = requestQueue[i]
                        if (req.priority >= request.priority) {
                            if (i < requestQueue.size - 1) {
                                if (requestQueue[i + 1].priority < request.priority) {
                                    index = i + 1
                                    break
                                }
                            } else {
                                index = i + 1
                            }
                        }
                    }
                    when {
                        index == -1 -> requestQueue.add(0, request)
                        index >= requestQueue.size -> requestQueue.add(request)
                        else -> requestQueue.add(index, request)
                    }
                }
            }
        }
    }

    private fun executeNextRequest() {
        synchronized(this) {
            connHandler.removeMessages(MSG_REQUEST_TIMEOUT)
            if (requestQueue.isEmpty()) {
                currentRequest = null
            } else {
                executeRequest(requestQueue.removeAt(0))
            }
        }
    }

    private fun executeRequest(request: GenericRequest) {
        currentRequest = request
        connHandler.sendMessageDelayed(
            Message.obtain(connHandler, MSG_REQUEST_TIMEOUT, request),
            configuration.requestTimeoutMillis.toLong()
        )
        
        if (!bluetoothAdapter.isEnabled) {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED, true)
            return
        }
        
        bluetoothGatt?.let { gatt ->
            when (request.type) {
                RequestType.READ_RSSI -> {
                    if (!gatt.readRemoteRssi()) {
                        handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
                    }
                }
                RequestType.CHANGE_MTU -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        if (!gatt.requestMtu(request.value as Int)) {
                            handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
                        }
                    }
                }
                RequestType.READ_PHY -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        gatt.readPhy()
                    }
                }
                RequestType.SET_PREFERRED_PHY -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val options = request.value as IntArray
                        gatt.setPreferredPhy(options[0], options[1], options[2])
                    }
                }
                else -> {
                    val gattService = gatt.getService(request.service)
                    if (gattService != null) {
                        val characteristic = gattService.getCharacteristic(request.characteristic)
                        if (characteristic != null) {
                            when (request.type) {
                                RequestType.SET_NOTIFICATION, RequestType.SET_INDICATION -> 
                                    executeIndicationOrNotification(request, characteristic)
                                RequestType.READ_CHARACTERISTIC -> 
                                    executeReadCharacteristic(request, characteristic)
                                RequestType.READ_DESCRIPTOR -> 
                                    executeReadDescriptor(request, characteristic)
                                RequestType.WRITE_CHARACTERISTIC -> 
                                    executeWriteCharacteristic(request, characteristic)
                                else -> {}
                            }
                        } else {
                            handleFailedCallback(request, REQUEST_FAIL_TYPE_CHARACTERISTIC_NOT_EXIST, true)
                        }
                    } else {
                        handleFailedCallback(request, REQUEST_FAIL_TYPE_SERVICE_NOT_EXIST, true)
                    }
                }
            }
        } ?: handleFailedCallback(request, REQUEST_FAIL_TYPE_GATT_IS_NULL, true)
    }

    private fun printWriteLog(request: GenericRequest, progress: Int, total: Int, value: ByteArray) {
        if (logger.isEnabled()) {
            val t = total.toString()
            val sb = StringBuilder(progress.toString())
            while (sb.length < t.length) {
                sb.insert(0, "0")
            }
            logD(Logger.TYPE_CHARACTERISTIC_WRITE, "package [%s/%s] write success! [UUID: %s, addr: %s, value: %s]",
                sb.toString(), t, substringUuid(request.characteristic), device.address, toHex(value))
        }
    }

    private fun executeWriteCharacteristic(request: GenericRequest, characteristic: BluetoothGattCharacteristic) {
        try {
            val value = request.value as ByteArray
            val options = request.writeOptions
            val reqDelay = if (options.requestWriteDelayMillis > 0) options.requestWriteDelayMillis else options.packageWriteDelayMillis
            
            if (reqDelay > 0) {
                try {
                    Thread.sleep(reqDelay.toLong())
                } catch (ignore: InterruptedException) {}
                if (request != currentRequest) return
            }
            
            if (options.useMtuAsPackageSize) {
                options.packageSize = mtu - 3
            }
            
            if (value.size > options.packageSize) {
                val list = MathUtils.splitPackage(value, options.packageSize)
                if (!options.isWaitWriteResult) {
                    val delay = options.packageWriteDelayMillis
                    for (i in list.indices) {
                        val bytes = list[i]
                        if (i > 0 && delay > 0) {
                            try {
                                Thread.sleep(delay.toLong())
                            } catch (ignore: InterruptedException) {}
                            if (request != currentRequest) return
                        }
                        if (!write(request, characteristic, bytes)) {
                            return
                        } else {
                            printWriteLog(request, i + 1, list.size, bytes)
                        }
                    }
                    printWriteLog(request, list.size, list.size, list.last())
                } else {
                    request.remainQueue = ConcurrentLinkedQueue(list)
                    request.sendingBytes = request.remainQueue!!.remove()
                    write(request, characteristic, request.sendingBytes)
                }
            } else {
                request.sendingBytes = value
                if (write(request, characteristic, value)) {
                    if (!options.isWaitWriteResult) {
                        notifyCharacteristicWrite(request, value)
                        printWriteLog(request, 1, 1, value)
                        executeNextRequest()
                    }
                }
            }
        } catch (e: Exception) {
            handleWriteFailed(request)
        }
    }

    private fun executeReadDescriptor(request: GenericRequest, characteristic: BluetoothGattCharacteristic) {
        val gattDescriptor = characteristic.getDescriptor(request.descriptor)
        if (gattDescriptor != null) {
            if (bluetoothGatt?.readDescriptor(gattDescriptor) != true) {
                handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
            }
        } else {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_DESCRIPTOR_NOT_EXIST, true)
        }
    }

    private fun executeReadCharacteristic(request: GenericRequest, characteristic: BluetoothGattCharacteristic) {
        if (bluetoothGatt?.readCharacteristic(characteristic) != true) {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
        }
    }

    private fun executeIndicationOrNotification(request: GenericRequest, characteristic: BluetoothGattCharacteristic) {
        if (enableNotificationOrIndicationFail(
            (request.value as Int) == 1,
            request.type == RequestType.SET_NOTIFICATION,
            characteristic)) {
            handleGattStatusFailed()
        }
    }

    private fun handlePhyChange(read: Boolean, txPhy: Int, rxPhy: Int, status: Int) {
        currentRequest?.let { request ->
            val isCorrectType = (read && request.type == RequestType.READ_PHY) || 
                              (!read && request.type == RequestType.SET_PREFERRED_PHY)
            if (isCorrectType) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    notifyPhyChange(request, txPhy, rxPhy)
                } else {
                    handleGattStatusFailed()
                }
                executeNextRequest()
            }
        }
    }

    private fun handleGattStatusFailed() {
        currentRequest?.let { request ->
            handleFailedCallback(request, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, false)
        }
    }

    private fun handleFailedCallback(request: GenericRequest, failType: Int, executeNext: Boolean) {
        notifyRequestFailed(request, failType)
        if (executeNext) {
            executeNextRequest()
        }
    }

    private fun toHex(bytes: ByteArray): String = StringUtils.toHex(bytes)

    private fun substringUuid(uuid: UUID?): String = 
        uuid?.toString()?.substring(0, 8) ?: "null"

    private fun handleCallbacks(callback: RequestCallback?, info: MethodInfo) {
        observer?.let { posterDispatcher.post(it, info) }
        callback?.let { posterDispatcher.post(it, info) } ?: observable.notifyObservers(info)
    }

    private fun log(priority: Int, type: Int, format: String, vararg args: Any?) {
        logger.log(priority, type, String.format(Locale.US, format, *args))
    }

    private fun logE(type: Int, format: String, vararg args: Any?) {
        log(Log.ERROR, type, format, *args)
    }

    private fun logD(type: Int, format: String, vararg args: Any?) {
        log(Log.DEBUG, type, format, *args)
    }

    private fun notifyRequestFailed(request: GenericRequest, failType: Int) {
        val info = MethodInfoGenerator.onRequestFailed(request, failType, request.value)
        handleCallbacks(request.callback, info)
        logE(Logger.TYPE_REQUEST_FAILED, "request failed! [requestType: %s, addr: %s, failType: %d",
            request.type, device.address, failType)
    }

    private fun notifyCharacteristicRead(request: GenericRequest, value: ByteArray) {
        val info = MethodInfoGenerator.onCharacteristicRead(request, value)
        handleCallbacks(request.callback, info)
        logD(Logger.TYPE_CHARACTERISTIC_READ, "characteristic read! [UUID: %s, addr: %s, value: %s]",
            substringUuid(request.characteristic), device.address, toHex(value))
    }

    private fun notifyCharacteristicChanged(characteristic: BluetoothGattCharacteristic) {
        val info = MethodInfoGenerator.onCharacteristicChanged(
            device, characteristic.service.uuid,
            characteristic.uuid, characteristic.value
        )
        observable.notifyObservers(info)
        observer?.let { posterDispatcher.post(it, info) }
        logD(Logger.TYPE_CHARACTERISTIC_CHANGED, "characteristic change! [UUID: %s, addr: %s, value: %s]",
            substringUuid(characteristic.uuid), device.address, toHex(characteristic.value))
    }

    private fun notifyRssiRead(request: GenericRequest, rssi: Int) {
        val info = MethodInfoGenerator.onRssiRead(request, rssi)
        handleCallbacks(request.callback, info)
        logD(Logger.TYPE_READ_REMOTE_RSSI, "rssi read! [addr: %s, rssi: %d]", device.address, rssi)
    }

    private fun notifyMtuChanged(request: GenericRequest, mtu: Int) {
        val info = MethodInfoGenerator.onMtuChanged(request, mtu)
        handleCallbacks(request.callback, info)
        logD(Logger.TYPE_MTU_CHANGED, "mtu change! [addr: %s, mtu: %d]", device.address, mtu)
    }

    private fun notifyDescriptorRead(request: GenericRequest, value: ByteArray) {
        val info = MethodInfoGenerator.onDescriptorRead(request, value)
        handleCallbacks(request.callback, info)
        logD(Logger.TYPE_DESCRIPTOR_READ, "descriptor read! [UUID: %s, addr: %s, value: %s]",
            substringUuid(request.characteristic), device.address, toHex(value))
    }

    private fun notifyNotificationChanged(request: GenericRequest, isEnabled: Boolean) {
        val info = MethodInfoGenerator.onNotificationChanged(request, isEnabled)
        handleCallbacks(request.callback, info)
        val eventType = if (request.type == RequestType.SET_NOTIFICATION) {
            Logger.TYPE_NOTIFICATION_CHANGED
        } else {
            Logger.TYPE_INDICATION_CHANGED
        }
        val message = if (isEnabled) {
            if (request.type == RequestType.SET_NOTIFICATION) "notification enabled!" else "indication enabled!"
        } else {
            if (request.type == RequestType.SET_NOTIFICATION) "notification disabled!" else "indication disabled!"
        }
        logD(eventType, "%s [UUID: %s, addr: %s]", message, substringUuid(request.characteristic), device.address)
    }

    private fun notifyCharacteristicWrite(request: GenericRequest, value: ByteArray) {
        val info = MethodInfoGenerator.onCharacteristicWrite(request, value)
        handleCallbacks(request.callback, info)
    }

    private fun notifyPhyChange(request: GenericRequest, txPhy: Int, rxPhy: Int) {
        val info = MethodInfoGenerator.onPhyChange(request, txPhy, rxPhy)
        handleCallbacks(request.callback, info)
        val event = if (request.type == RequestType.READ_PHY) "phy read!" else "phy update!"
        logD(Logger.TYPE_PHY_CHANGE, "%s [addr: %s, tvPhy: %s, rxPhy: %s]", event, device.address, txPhy, rxPhy)
    }

    override fun getMtu(): Int = mtu

    @NonNull
    override fun getDevice(): Device = device

    override fun reconnect() {
        if (!isReleased) {
            isActiveDisconnect = false
            tryReconnectCount = 0
            reconnectImmediatelyCount = 0
            Message.obtain(connHandler, MSG_DISCONNECT, MSG_ARG_RECONNECT, 0).sendToTarget()
        }
    }

    override fun disconnect() {
        if (!isReleased) {
            isActiveDisconnect = true
            Message.obtain(connHandler, MSG_DISCONNECT, MSG_ARG_NONE, 0).sendToTarget()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun doRefresh(): Boolean {
        return try {
            val localMethod = bluetoothGatt?.javaClass?.getMethod("refresh")
            localMethod?.invoke(bluetoothGatt) as? Boolean ?: false
        } catch (ignore: Exception) {
            false
        }
    }

    override fun refresh() {
        connHandler.sendEmptyMessage(MSG_REFRESH)
    }

    private fun release(noEvent: Boolean) {
        if (!isReleased) {
            isReleased = true
            configuration.setAutoReconnect(false)
            connHandler.removeCallbacksAndMessages(null)
            easyBle.removeScanListener(this)
            clearRequestQueueAndNotify()
            bluetoothGatt?.let { gatt ->
                closeGatt(gatt)
                bluetoothGatt = null
            }
            device.connectionState = ConnectionState.RELEASED
            logD(Logger.TYPE_CONNECTION_STATE, "connection released! [name: %s, addr: %s]", device.name, device.address)
            if (!noEvent) {
                sendConnectionCallback()
            }
            easyBle.releaseConnection(device)
        }
    }

    override fun release() {
        release(false)
    }

    override fun releaseNoEvent() {
        release(true)
    }

    @NonNull
    override fun getConnectionState(): ConnectionState = device.connectionState

    override fun isAutoReconnectEnabled(): Boolean = configuration.isAutoReconnect

    @Nullable
    override fun getGatt(): BluetoothGatt? = bluetoothGatt

    override fun clearRequestQueue() {
        synchronized(this) {
            requestQueue.clear()
            currentRequest = null
        }
    }

    override fun clearRequestQueueByType(type: RequestType) {
        synchronized(this) {
            val iterator = requestQueue.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().type == type) {
                    iterator.remove()
                }
            }
            if (currentRequest?.type == type) {
                currentRequest = null
            }
        }
    }

    private fun clearRequestQueueAndNotify() {
        synchronized(this) {
            requestQueue.forEach { request ->
                handleFailedCallback(request, REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED, false)
            }
            currentRequest?.let { request ->
                handleFailedCallback(request, REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED, false)
            }
        }
        clearRequestQueue()
    }

    @NonNull
    override fun getConnectionConfiguration(): ConnectionConfiguration = configuration

    @Nullable
    override fun getService(service: UUID?): BluetoothGattService? {
        return if (service != null && bluetoothGatt != null) {
            bluetoothGatt?.getService(service)
        } else null
    }

    @Nullable
    override fun getCharacteristic(service: UUID?, characteristic: UUID?): BluetoothGattCharacteristic? {
        if (service != null && characteristic != null && bluetoothGatt != null) {
            val gattService = bluetoothGatt?.getService(service)
            return gattService?.getCharacteristic(characteristic)
        }
        return null
    }

    @Nullable
    override fun getDescriptor(service: UUID?, characteristic: UUID?, descriptor: UUID?): BluetoothGattDescriptor? {
        if (service != null && characteristic != null && descriptor != null && bluetoothGatt != null) {
            val gattService = bluetoothGatt?.getService(service)
            val gattCharacteristic = gattService?.getCharacteristic(characteristic)
            return gattCharacteristic?.getDescriptor(descriptor)
        }
        return null
    }

    private fun checkUuidExistsAndEnqueue(request: GenericRequest, uuidNum: Int) {
        val exists = when {
            uuidNum > 2 -> checkDescriptorExists(request, request.service, request.characteristic, request.descriptor)
            uuidNum > 1 -> checkCharacteristicExists(request, request.service, request.characteristic)
            uuidNum == 1 -> checkServiceExists(request, request.service)
            else -> false
        }
        if (exists) {
            enqueue(request)
        }
    }

    private fun checkServiceExists(request: GenericRequest, uuid: UUID?): Boolean {
        if (getService(uuid) == null) {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_SERVICE_NOT_EXIST, false)
            return false
        }
        return true
    }

    private fun checkCharacteristicExists(request: GenericRequest, service: UUID?, characteristic: UUID?): Boolean {
        if (checkServiceExists(request, service)) {
            if (getCharacteristic(service, characteristic) == null) {
                handleFailedCallback(request, REQUEST_FAIL_TYPE_CHARACTERISTIC_NOT_EXIST, false)
                return false
            }
            return true
        }
        return false
    }

    private fun checkDescriptorExists(request: GenericRequest, service: UUID?, characteristic: UUID?, descriptor: UUID?): Boolean {
        if (checkServiceExists(request, service) && checkCharacteristicExists(request, service, characteristic)) {
            if (getDescriptor(service, characteristic, descriptor) == null) {
                handleFailedCallback(request, REQUEST_FAIL_TYPE_DESCRIPTOR_NOT_EXIST, false)
                return false
            }
            return true
        }
        return false
    }

    override fun execute(request: Request) {
        if (request is GenericRequest) {
            request.device = device
            when (request.type) {
                RequestType.SET_NOTIFICATION,
                RequestType.SET_INDICATION,
                RequestType.READ_CHARACTERISTIC,
                RequestType.WRITE_CHARACTERISTIC -> {
                    if (request.type == RequestType.WRITE_CHARACTERISTIC && request.writeOptions == null) {
                        request.writeOptions = configuration.getDefaultWriteOptions(request.service, request.characteristic)
                            ?: WriteOptions.Builder().build()
                    }
                    checkUuidExistsAndEnqueue(request, 2)
                }
                RequestType.READ_DESCRIPTOR -> checkUuidExistsAndEnqueue(request, 3)
                else -> enqueue(request)
            }
        }
    }

    override fun isNotificationOrIndicationEnabled(characteristic: BluetoothGattCharacteristic): Boolean {
        Inspector.requireNonNull(characteristic, "characteristic can't be null")
        val descriptor = characteristic.getDescriptor(clientCharacteristicConfig)
        return descriptor != null && (
            Arrays.equals(descriptor.value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ||
            Arrays.equals(descriptor.value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
        )
    }

    override fun isNotificationOrIndicationEnabled(service: UUID, characteristic: UUID): Boolean {
        val c = getCharacteristic(service, characteristic)
        return c?.let { isNotificationOrIndicationEnabled(it) } ?: false
    }
}