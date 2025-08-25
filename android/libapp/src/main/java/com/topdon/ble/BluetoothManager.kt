package com.topdon.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.topdon.ble.callback.MtuChangeCallback
import com.topdon.ble.callback.NotificationChangeCallback
import com.topdon.ble.callback.ReadCharacteristicCallback
import com.topdon.commons.UUIDManager
import com.topdon.commons.observer.Observable
import com.topdon.commons.observer.Observe
import com.topdon.commons.poster.RunOn
import com.topdon.commons.poster.Tag
import com.topdon.commons.poster.ThreadMode
import com.topdon.commons.util.LLog
import com.topdon.commons.util.StringUtils
import org.greenrobot.eventbus.EventBus
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothManager private constructor() : EventObserver {
    
    private var mDevice: Device? = null
    private var connection: Connection? = null
    private var writeCharact: BluetoothGattCharacteristic? = null

    companion object {
        @JvmStatic
        var iSReset = false
        
        @JvmStatic
        var isSending = false
        
        @JvmStatic
        var isClickStopCharging = false
        
        @JvmStatic
        var isReceiveBleData = false
        
        @Volatile
        private var instance: BluetoothManager? = null

        @JvmStatic
        fun getInstance(): BluetoothManager {
            return instance ?: synchronized(this) {
                instance ?: BluetoothManager().also { instance = it }
            }
        }

        @JvmStatic
        fun setBleData(message: String) {
            // Implementation for setBleData if needed
        }
    }

    fun getDevice(): Device? = mDevice

    private fun setMTUValue() {
        mDevice?.let { device ->
            if (device.isConnected) {
                Log.e("bcf_ble", "连接设备名称：${device.name}")
                
                val mtuValue = when {
                    device.name?.contains("T-darts") == true || 
                    device.name?.contains("TD") == true -> 240
                    else -> 503
                }
                
                val builder = RequestBuilderFactory().getChangeMtuBuilder(mtuValue)
                val request = builder.setCallback(object : MtuChangeCallback {
                    override fun onMtuChanged(request: Request, mtu: Int) {
                        Log.d("wangchen", "MTU修改成功，新值：$mtu")
                        setReadCallback()
                    }

                    override fun onRequestFailed(request: Request, failType: Int, value: Any?) {
                        Log.d("bcf", "MTU修改失败")
                    }
                }).build()
                
                connection?.execute(request)
            }
        }
    }

    private fun setReadCallback() {
        mDevice?.let { device ->
            if (device.isConnected) {
                isSending = false

                val serviceUUID = UUID.fromString(UUIDManager.SERVICE_UUID)
                val notifyUUID = UUID.fromString(UUIDManager.NOTIFY_UUID)
                val readUUID = UUID.fromString(UUIDManager.READ_UUID)

                val isEnabled = connection?.isNotificationOrIndicationEnabled(serviceUUID, notifyUUID) ?: false
                LLog.w("bcf_ble", "是否打开了Notifycation: $isEnabled")

                val notificationBuilder = RequestBuilderFactory()
                    .getSetNotificationBuilder(serviceUUID, notifyUUID, true)
                val readBuilder = RequestBuilderFactory()
                    .getReadCharacteristicBuilder(serviceUUID, readUUID)

                connection?.let { conn ->
                    notificationBuilder.build().execute(conn)
                    readBuilder.build().execute(conn)
                }
            }
        }
    }

    fun setCancelListening() {
        val observable = EasyBLE.getInstance().observable
        observable?.let {
            EasyBLE.getInstance().unregisterObserver(this)
        }
    }

    fun connect(device: Device): Connection? {
        mDevice = device
        val config = ConnectionConfiguration().apply {
            connectTimeoutMillis = 10000
            requestTimeoutMillis = 7000
            isAutoReconnect = false
            reconnectImmediatelyMaxTimes = 3
        }
        
        connection = EasyBLE.getInstance().connect(device, config, this)
        connection?.setBluetoothGattCallback(object : BluetoothGattCallback() {
            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                val message = "原始写入数据状态：status: $status  内容：${StringUtils.toHex(characteristic.value)}"
                Log.d("ble_bcf_data", message)
                setBleData(message)
            }
        })
        return connection
    }

    fun connect(mac: String, name: String): Connection? {
        val configuration = ConnectionConfiguration().apply {
            connectTimeoutMillis = 10000
            requestTimeoutMillis = 7000
            isAutoReconnect = false
            reconnectImmediatelyMaxTimes = 3
        }
        
        connection = EasyBLE.getInstance().connect(mac, configuration, this)
        mDevice = connection?.device?.apply { 
            this.name = name 
        }
        return connection
    }

    fun release() {
        Log.d("bcf", "释放所有BLE连接")
        mDevice?.let { device ->
            EasyBLE.getInstance().apply {
                disconnectConnection(device)
                release()
                releaseConnection(device)
            }
        }
    }

    fun isConnected(): Boolean = mDevice?.isConnected == true

    @Tag("onConnectionStateChanged")
    @Observe
    @RunOn(ThreadMode.MAIN)
    override fun onConnectionStateChanged(device: Device) {
        val state = device.connectionState
        if (state != ConnectionState.SERVICE_DISCOVERED && state != ConnectionState.DISCONNECTED) {
            EventBus.getDefault().post(state)
            Log.e("wangchen", "发送广播--$state")
        }
        
        Log.d("ywq", "MyObserver 连接状态：$state 是否已连接：${device.isConnected}-----名称：${device.name}-------mac: ${device.address}")
        
        when (state) {
            ConnectionState.SCANNING_FOR_RECONNECTION -> {}
            ConnectionState.CONNECTING -> {}
            ConnectionState.CONNECTED -> {}
            ConnectionState.DISCONNECTED -> {
                EventBus.getDefault().post(ConnectionState.DISCONNECTED.name)
            }
            ConnectionState.RELEASED -> {
                EventBus.getDefault().post(ConnectionState.RELEASED.name)
            }
            ConnectionState.SERVICE_DISCOVERED -> {
                setMTUValue()
                if (device.isConnected) {
                    EventBus.getDefault().post(ConnectionState.SERVICE_DISCOVERED.name)
                }
            }
        }
    }

    override fun onConnectFailed(device: Device, failType: Int) {
        Log.e("bcf_ble", "连接失败${device.name}")
        EventBus.getDefault().post(device.connectionState)
    }

    override fun onConnectTimeout(device: Device, type: Int) {
        Log.e("bcf_ble", "连接超时")
    }

    @Observe
    override fun onNotificationChanged(request: Request, isEnabled: Boolean) {
        val typeTag = if (request.type == RequestType.SET_NOTIFICATION) {
            EventBus.getDefault().post(ConnectionState.MTU_SUCCESS)
            "通知"
        } else {
            "Indication"
        }
        Log.d("bcf_ble", "onNotificationChanged ：$typeTag：${if (isEnabled) "开启" else "关闭"}")
    }

    fun writeBuletoothData(data: ByteArray): Boolean {
        val device = mDevice
        val conn = connection
        
        if (device == null || !device.isConnected || conn == null) {
            return false
        }

        val serviceUUID = UUID.fromString(UUIDManager.SERVICE_UUID)
        val writeUUID = UUID.fromString(UUIDManager.WRITE_UUID)
        
        writeCharact = conn.getCharacteristic(serviceUUID, writeUUID)?.apply {
            writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            value = data
        }

        writeCharact?.let { characteristic ->
            conn.gatt?.setCharacteristicNotification(characteristic, true)
            return conn.gatt?.writeCharacteristic(characteristic) ?: false
        }
        
        return false
    }

    @Observe
    override fun onCharacteristicRead(request: Request, value: ByteArray) {
        val data = StringUtils.toHex(value)
        // Handle characteristic read if needed
    }

    @Observe
    override fun onCharacteristicChanged(device: Device, service: UUID, characteristic: UUID, value: ByteArray) {
        Log.e("ble_bcf_data", "接收蓝牙数据：${StringUtils.toHex(value)}")
        EventBus.getDefault().post(value)
    }
}