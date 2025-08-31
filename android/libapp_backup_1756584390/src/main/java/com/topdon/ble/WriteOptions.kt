package com.topdon.ble

import android.bluetooth.BluetoothGattCharacteristic

class WriteOptions private constructor(builder: Builder) {
    val packageWriteDelayMillis: Int = builder.packageWriteDelayMillis
    val requestWriteDelayMillis: Int = builder.requestWriteDelayMillis
    var packageSize: Int = builder.packageSize
    val isWaitWriteResult: Boolean = builder.isWaitWriteResult
    val writeType: Int = builder.writeType
    val useMtuAsPackageSize: Boolean = builder.useMtuAsPackageSize
    
    class Builder {
        internal var packageWriteDelayMillis = 0
        internal var requestWriteDelayMillis = -1
        internal var packageSize = 20
        internal var isWaitWriteResult = true
        internal var writeType = -1
        internal var useMtuAsPackageSize = false
        
        fun setPackageWriteDelayMillis(packageWriteDelayMillis: Int): Builder = apply {
            this.packageWriteDelayMillis = packageWriteDelayMillis
        }
        
        fun setRequestWriteDelayMillis(requestWriteDelayMillis: Int): Builder = apply {
            this.requestWriteDelayMillis = requestWriteDelayMillis
        }
        
        fun setPackageSize(packageSize: Int): Builder = apply {
            if (packageSize > 0) {
                this.packageSize = packageSize
            }
        }
        
        fun setWaitWriteResult(waitWriteResult: Boolean): Builder = apply {
            isWaitWriteResult = waitWriteResult
        }
        
        fun setWriteType(writeType: Int): Builder = apply {
            if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT ||
                writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE ||
                writeType == BluetoothGattCharacteristic.WRITE_TYPE_SIGNED
            ) {
                this.writeType = writeType
            }
        }
        
        fun setMtuAsPackageSize(): Builder = apply {
            useMtuAsPackageSize = true
        }
        
        fun build(): WriteOptions {
            return WriteOptions(this)
        }
    }
}