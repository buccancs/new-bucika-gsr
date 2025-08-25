package com.topdon.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RequiresApi
import java.util.*

data class Device(
    val originDevice: BluetoothDevice
) : Comparable<Device>, Cloneable, Parcelable {

    var connectionState: ConnectionState = ConnectionState.DISCONNECTED

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    var scanResult: ScanResult? = null

    var scanRecord: ByteArray? = null
    var name: String = originDevice.name ?: ""
    var address: String = originDevice.address
    var rssi: Int = -120

    fun setRssi(rssi: Int) {
        this.rssi = rssi
    }

    fun setName(name: String) {
        this.name = name
    }

    fun getConnectionState(): ConnectionState {
        val connection = EasyBLE.getInstance().getConnection(this)
        return connection?.getConnectionState() ?: connectionState
    }

    fun isConnectable(): Boolean? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanResult?.let { scanResult ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return scanResult.isConnectable
                }
            }
        }
        return null
    }

    fun isConnected(): Boolean = getConnectionState() == ConnectionState.SERVICE_DISCOVERED

    fun isDisconnected(): Boolean {
        val state = getConnectionState()
        return state == ConnectionState.DISCONNECTED || state == ConnectionState.RELEASED
    }

    fun isConnecting(): Boolean {
        val state = getConnectionState()
        return state != ConnectionState.DISCONNECTED && 
               state != ConnectionState.SERVICE_DISCOVERED &&
               state != ConnectionState.RELEASED
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Device) return false
        return address == other.address
    }

    override fun hashCode(): Int = address.hashCode()

    override fun compareTo(other: Device): Int {
        return when {
            rssi == 0 -> -1
            other.rssi == 0 -> 1
            else -> {
                val result = other.rssi.compareTo(rssi)
                if (result == 0) name.compareTo(other.name) else result
            }
        }
    }

    override fun toString(): String {
        return "Device(name='$name', address='$address')"
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(originDevice, flags)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dest.writeParcelable(scanResult, flags)
        }
        scanRecord?.let { record ->
            dest.writeInt(record.size)
            dest.writeByteArray(record)
        } ?: dest.writeInt(-1)
        
        dest.writeString(name)
        dest.writeString(address)
        dest.writeInt(rssi)
        dest.writeString(connectionState.name)
    }

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(BluetoothDevice::class.java.classLoader)!!
    ) {
        readFromParcel(parcel)
    }

    fun readFromParcel(parcel: Parcel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanResult = parcel.readParcelable(ScanResult::class.java.classLoader)
        }
        val scanRecordLen = parcel.readInt()
        if (scanRecordLen > 0) {
            scanRecord = ByteArray(scanRecordLen)
            parcel.readByteArray(scanRecord!!)
        }
        name = parcel.readString() ?: ""
        address = Objects.requireNonNull(parcel.readString())
        rssi = parcel.readInt()
        connectionState = ConnectionState.valueOf(parcel.readString()!!)
    }

    companion object CREATOR : Parcelable.Creator<Device> {
        override fun createFromParcel(parcel: Parcel): Device = Device(parcel)
        override fun newArray(size: Int): Array<Device?> = arrayOfNulls(size)
    }
}