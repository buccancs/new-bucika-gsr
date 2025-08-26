package com.topdon.ble

import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.core.util.Pair
import java.util.*

class ConnectionConfiguration {
    
    companion object {
        const val TRY_RECONNECT_TIMES_INFINITE = -1
    }
    
    var discoverServicesDelayMillis = 600
        private set
        
    var connectTimeoutMillis = 10000
        
    var requestTimeoutMillis = 3000
        
    var tryReconnectMaxTimes = TRY_RECONNECT_TIMES_INFINITE
        
    var reconnectImmediatelyMaxTimes = 3
        
    var isAutoReconnect = true
        
    @RequiresApi(Build.VERSION_CODES.M)
    var transport = BluetoothDevice.TRANSPORT_LE
        
    @RequiresApi(Build.VERSION_CODES.O)
    var phy = BluetoothDevice.PHY_LE_1M_MASK
        
    @NonNull
    val scanIntervalPairsInAutoReconnection: MutableList<Pair<Int, Int>>
    
    private val defaultWriteOptionsMap = HashMap<String, WriteOptions>()

    init {
        scanIntervalPairsInAutoReconnection = mutableListOf<Pair<Int, Int>>().apply {
            add(Pair.create(0, 2000))
            add(Pair.create(1, 5000))
            add(Pair.create(3, 10000))
            add(Pair.create(5, 30000))
            add(Pair.create(10, 60000))
        }
    }

    fun setDiscoverServicesDelayMillis(discoverServicesDelayMillis: Int): ConnectionConfiguration {
        this.discoverServicesDelayMillis = discoverServicesDelayMillis
        return this
    }

    fun setConnectTimeoutMillis(connectTimeoutMillis: Int): ConnectionConfiguration {
        if (connectTimeoutMillis >= 1000) {
            this.connectTimeoutMillis = connectTimeoutMillis
        }
        return this
    }

    fun setRequestTimeoutMillis(requestTimeoutMillis: Int): ConnectionConfiguration {
        if (requestTimeoutMillis >= 1000) {
            this.requestTimeoutMillis = requestTimeoutMillis
        }
        return this
    }

    fun setTryReconnectMaxTimes(tryReconnectMaxTimes: Int): ConnectionConfiguration {
        this.tryReconnectMaxTimes = tryReconnectMaxTimes
        return this
    }

    fun setReconnectImmediatelyMaxTimes(reconnectImmediatelyMaxTimes: Int): ConnectionConfiguration {
        this.reconnectImmediatelyMaxTimes = reconnectImmediatelyMaxTimes
        return this
    }

    fun setAutoReconnect(autoReconnect: Boolean): ConnectionConfiguration {
        isAutoReconnect = autoReconnect
        return this
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun setTransport(transport: Int): ConnectionConfiguration {
        this.transport = transport
        return this
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setPhy(phy: Int): ConnectionConfiguration {
        this.phy = phy
        return this
    }

    fun setScanIntervalPairsInAutoReconnection(parameters: List<Pair<Int, Int>>): ConnectionConfiguration {
        Inspector.requireNonNull(parameters, "parameters can't be null")
        scanIntervalPairsInAutoReconnection.clear()
        scanIntervalPairsInAutoReconnection.addAll(parameters)
        return this
    }

    fun setDefaultWriteOptions(service: UUID, characteristic: UUID, options: WriteOptions): ConnectionConfiguration {
        Inspector.requireNonNull(service, "service can't be null")
        Inspector.requireNonNull(characteristic, "characteristic can't be null")
        Inspector.requireNonNull(options, "options can't be null")
        defaultWriteOptionsMap["$service:$characteristic"] = options
        return this
    }
    
    @Nullable
    fun getDefaultWriteOptions(service: UUID, characteristic: UUID): WriteOptions? {
        return defaultWriteOptionsMap["$service:$characteristic"]
    }
}