package com.topdon.ble

import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import com.topdon.ble.callback.MtuChangeCallback
import com.topdon.ble.callback.NotificationChangeCallback
import com.topdon.ble.callback.PhyChangeCallback
import com.topdon.ble.callback.ReadCharacteristicCallback
import com.topdon.ble.callback.ReadRssiCallback
import java.util.UUID

object RequestBuilderFactory {
    
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun getChangeMtuBuilder(@IntRange(from = 23, to = 517) mtu: Int): RequestBuilder<MtuChangeCallback> {
        val validMtu = when {
            mtu < 23 -> 23
            mtu > 517 -> 517
            else -> mtu
        }
        return RequestBuilder<MtuChangeCallback>(RequestType.CHANGE_MTU).apply {
            value = validMtu
        }
    }

    fun getReadCharacteristicBuilder(service: UUID, characteristic: UUID): RequestBuilder<ReadCharacteristicCallback> {
        return RequestBuilder<ReadCharacteristicCallback>(RequestType.READ_CHARACTERISTIC).apply {
            this.service = service
            this.characteristic = characteristic
        }
    }

    fun getSetNotificationBuilder(
        service: UUID, 
        characteristic: UUID,
        enable: Boolean
    ): RequestBuilder<NotificationChangeCallback> {
        return RequestBuilder<NotificationChangeCallback>(RequestType.SET_NOTIFICATION).apply {
            this.service = service
            this.characteristic = characteristic
            this.value = if (enable) 1 else 0
        }
    }

    fun getSetIndicationBuilder(
        service: UUID,
        characteristic: UUID,
        enable: Boolean
    ): RequestBuilder<NotificationChangeCallback> {
        return RequestBuilder<NotificationChangeCallback>(RequestType.SET_INDICATION).apply {
            this.service = service
            this.characteristic = characteristic
            this.value = if (enable) 1 else 0
        }
    }

    fun getReadDescriptorBuilder(
        service: UUID,
        characteristic: UUID,
        descriptor: UUID
    ): RequestBuilder<NotificationChangeCallback> {
        return RequestBuilder<NotificationChangeCallback>(RequestType.READ_DESCRIPTOR).apply {
            this.service = service
            this.characteristic = characteristic
            this.descriptor = descriptor
        }
    }

    fun getWriteCharacteristicBuilder(
        service: UUID,
        characteristic: UUID,
        value: ByteArray
    ): WriteCharacteristicBuilder {
        Inspector.requireNonNull(value, "value can't be null")
        return WriteCharacteristicBuilder().apply {
            this.service = service
            this.characteristic = characteristic
            this.value = value
        }
    }

    fun getReadRssiBuilder(): RequestBuilder<ReadRssiCallback> {
        return RequestBuilder(RequestType.READ_RSSI)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getReadPhyBuilder(): RequestBuilder<PhyChangeCallback> {
        return RequestBuilder(RequestType.READ_PHY)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getSetPreferredPhyBuilder(txPhy: Int, rxPhy: Int, phyOptions: Int): RequestBuilder<PhyChangeCallback> {
        return RequestBuilder<PhyChangeCallback>(RequestType.SET_PREFERRED_PHY).apply {
            value = intArrayOf(txPhy, rxPhy, phyOptions)
        }
    }
}