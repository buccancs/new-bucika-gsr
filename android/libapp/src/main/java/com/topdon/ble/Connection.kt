package com.topdon.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import java.util.UUID

interface Connection {
    companion object {
        val clientCharacteristicConfig: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        const val REQUEST_FAIL_TYPE_REQUEST_FAILED = 0
        const val REQUEST_FAIL_TYPE_CHARACTERISTIC_NOT_EXIST = 1
        const val REQUEST_FAIL_TYPE_DESCRIPTOR_NOT_EXIST = 2
        const val REQUEST_FAIL_TYPE_SERVICE_NOT_EXIST = 3
        const val REQUEST_FAIL_TYPE_GATT_STATUS_FAILED = 4
        const val REQUEST_FAIL_TYPE_GATT_IS_NULL = 5
        const val REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED = 6
        const val REQUEST_FAIL_TYPE_REQUEST_TIMEOUT = 7
        const val REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED = 8
        const val REQUEST_FAIL_TYPE_CONNECTION_RELEASED = 9

        const val TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE = 0
        const val TIMEOUT_TYPE_CANNOT_CONNECT = 1
        const val TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES = 2

        const val CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION = 1
        const val CONNECT_FAIL_TYPE_CONNECTION_IS_UNSUPPORTED = 2
    }

    @get:NonNull
    val device: Device

    val mtu: Int

    fun reconnect()
    fun disconnect()
    fun refresh()
    fun release()
    fun releaseNoEvent()

    @get:NonNull
    val connectionState: ConnectionState

    fun isAutoReconnectEnabled(): Boolean

    @get:Nullable
    val gatt: BluetoothGatt?

    fun clearRequestQueue()
    fun clearRequestQueueByType(type: RequestType)

    @get:NonNull
    val connectionConfiguration: ConnectionConfiguration

    @Nullable
    fun getService(service: UUID): BluetoothGattService?

    @Nullable
    fun getCharacteristic(service: UUID, characteristic: UUID): BluetoothGattCharacteristic?

    @Nullable
    fun getDescriptor(service: UUID, characteristic: UUID, descriptor: UUID): BluetoothGattDescriptor?

    fun execute(request: Request)

    fun isNotificationOrIndicationEnabled(characteristic: BluetoothGattCharacteristic): Boolean
    fun isNotificationOrIndicationEnabled(service: UUID, characteristic: UUID): Boolean

    fun setBluetoothGattCallback(callback: BluetoothGattCallback)

    fun hasProperty(service: UUID, characteristic: UUID, property: Int): Boolean
}
