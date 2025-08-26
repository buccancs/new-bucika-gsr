package com.topdon.ble

import com.topdon.commons.poster.MethodInfo
import java.util.UUID

internal object MethodInfoGenerator {
    
    @JvmStatic
    fun onBluetoothAdapterStateChanged(state: Int): MethodInfo {
        return MethodInfo("onBluetoothAdapterStateChanged", MethodInfo.Parameter(Int::class.java, state))
    }
    
    @JvmStatic
    fun onConnectionStateChanged(device: Device): MethodInfo {
        return MethodInfo("onConnectionStateChanged", MethodInfo.Parameter(Device::class.java, device))
    }
    
    @JvmStatic
    fun onConnectFailed(device: Device, failType: Int): MethodInfo {
        return MethodInfo(
            "onConnectFailed",
            MethodInfo.Parameter(Device::class.java, device),
            MethodInfo.Parameter(Int::class.java, failType)
        )
    }
    
    @JvmStatic
    fun onConnectTimeout(device: Device, type: Int): MethodInfo {
        return MethodInfo(
            "onConnectTimeout",
            MethodInfo.Parameter(Device::class.java, device),
            MethodInfo.Parameter(Int::class.java, type)
        )
    }
    
    @JvmStatic
    fun onCharacteristicChanged(device: Device, service: UUID, characteristic: UUID, value: ByteArray): MethodInfo {
        return MethodInfo(
            "onCharacteristicChanged",
            MethodInfo.Parameter(Device::class.java, device),
            MethodInfo.Parameter(UUID::class.java, service),
            MethodInfo.Parameter(UUID::class.java, characteristic),
            MethodInfo.Parameter(ByteArray::class.java, value)
        )
    }
    
    @JvmStatic
    fun onCharacteristicRead(request: Request, value: ByteArray): MethodInfo {
        return MethodInfo(
            "onCharacteristicRead",
            MethodInfo.Parameter(Request::class.java, request),
            MethodInfo.Parameter(ByteArray::class.java, value)
        )
    }
    
    @JvmStatic
    fun onCharacteristicWrite(request: Request, value: ByteArray): MethodInfo {
        return MethodInfo(
            "onCharacteristicWrite",
            MethodInfo.Parameter(Request::class.java, request),
            MethodInfo.Parameter(ByteArray::class.java, value)
        )
    }
    
    @JvmStatic
    fun onRssiRead(request: Request, rssi: Int): MethodInfo {
        return MethodInfo(
            "onRssiRead",
            MethodInfo.Parameter(Request::class.java, request),
            MethodInfo.Parameter(Int::class.java, rssi)
        )
    }
    
    @JvmStatic
    fun onDescriptorRead(request: Request, value: ByteArray): MethodInfo {
        return MethodInfo(
            "onDescriptorRead",
            MethodInfo.Parameter(Request::class.java, request),
            MethodInfo.Parameter(ByteArray::class.java, value)
        )
    }
    
    @JvmStatic
    fun onNotificationChanged(request: Request, isEnabled: Boolean): MethodInfo {
        return MethodInfo(
            "onNotificationChanged",
            MethodInfo.Parameter(Request::class.java, request),
            MethodInfo.Parameter(Boolean::class.java, isEnabled)
        )
    }
    
    @JvmStatic
    fun onMtuChanged(request: Request, mtu: Int): MethodInfo {
        return MethodInfo(
            "onMtuChanged",
            MethodInfo.Parameter(Request::class.java, request),
            MethodInfo.Parameter(Int::class.java, mtu)
        )
    }
    
    @JvmStatic
    fun onPhyChange(request: Request, txPhy: Int, rxPhy: Int): MethodInfo {
        return MethodInfo(
            "onPhyChange",
            MethodInfo.Parameter(Request::class.java, request),
            MethodInfo.Parameter(Int::class.java, txPhy),
            MethodInfo.Parameter(Int::class.java, rxPhy)
        )
    }
    
    @JvmStatic
    fun onRequestFailed(request: Request, failType: Int, value: Any?): MethodInfo {
        return MethodInfo(
            "onRequestFailed",
            MethodInfo.Parameter(Request::class.java, request),
            MethodInfo.Parameter(Int::class.java, failType),
            MethodInfo.Parameter(Any::class.java, value)
        )
    }
}