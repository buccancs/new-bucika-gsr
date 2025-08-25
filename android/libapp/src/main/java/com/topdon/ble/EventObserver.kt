package com.topdon.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import com.topdon.commons.observer.Observer
import java.util.UUID

interface EventObserver : Observer {
    
    fun onBluetoothAdapterStateChanged(state: Int) {}
    
    fun onCharacteristicRead(request: Request, value: ByteArray) {}
    
    fun onCharacteristicChanged(device: Device, service: UUID, characteristic: UUID, value: ByteArray) {}
    
    fun onCharacteristicWrite(request: Request, value: ByteArray) {}
    
    fun onRssiRead(request: Request, rssi: Int) {}
    
    fun onDescriptorRead(request: Request, value: ByteArray) {}
    
    fun onNotificationChanged(request: Request, isEnabled: Boolean) {}
    
    fun onMtuChanged(request: Request, mtu: Int) {}
    
    fun onPhyChange(request: Request, txPhy: Int, rxPhy: Int) {}
    
    fun onRequestFailed(request: Request, failType: Int, value: Any?) {}
    
    fun onConnectionStateChanged(device: Device) {}
    
    fun onConnectFailed(device: Device, failType: Int) {}
    
    fun onConnectTimeout(device: Device, type: Int) {}
}
