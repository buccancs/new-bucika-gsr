package com.multisensor.recording.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid

@Entity(tableName = "shimmer_device_state")
@TypeConverters(ShimmerStateConverters::class)
data class ShimmerDeviceState(
    @PrimaryKey
    val deviceAddress: String,
    val deviceName: String,
    val connectionType: ShimmerBluetoothManagerAndroid.BT_TYPE,
    val isConnected: Boolean = false,
    val lastConnectedTimestamp: Long = 0L,
    val connectionAttempts: Int = 0,
    val lastConnectionError: String? = null,

    val enabledSensors: Set<String> = emptySet(),
    val samplingRate: Double = 512.0,
    val gsrRange: Int = 0,
    val sensorConfiguration: String? = null,

    val isStreaming: Boolean = false,
    val isSDLogging: Boolean = false,
    val lastStreamingTimestamp: Long = 0L,
    val lastSDLoggingTimestamp: Long = 0L,

    val batteryLevel: Int = -1,
    val signalStrength: Int = -1,
    val firmwareVersion: String = "",
    val deviceType: String = "Shimmer3",

    val autoReconnectEnabled: Boolean = true,
    val preferredConnectionOrder: Int = 0,

    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "shimmer_connection_history")
data class ShimmerConnectionHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceAddress: String,
    val deviceName: String,
    val connectionType: ShimmerBluetoothManagerAndroid.BT_TYPE,
    val action: ConnectionAction,
    val success: Boolean,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long = 0
)

enum class ConnectionAction {
    CONNECT_ATTEMPT,
    CONNECT_SUCCESS,
    CONNECT_FAILED,
    DISCONNECT,
    AUTO_RECONNECT,
    CONFIGURATION_APPLIED,
    STREAMING_STARTED,
    STREAMING_STOPPED,
    SD_LOGGING_STARTED,
    SD_LOGGING_STOPPED
}

class ShimmerStateConverters {

    @TypeConverter
    fun fromBtType(btType: ShimmerBluetoothManagerAndroid.BT_TYPE): String {
        return btType.name
    }

    @TypeConverter
    fun toBtType(btTypeName: String): ShimmerBluetoothManagerAndroid.BT_TYPE {
        return try {
            ShimmerBluetoothManagerAndroid.BT_TYPE.valueOf(btTypeName)
        } catch (e: IllegalArgumentException) {
            ShimmerBluetoothManagerAndroid.BT_TYPE.BT_CLASSIC
        }
    }

    @TypeConverter
    fun fromStringSet(stringSet: Set<String>): String {
        return stringSet.joinToString(",")
    }

    @TypeConverter
    fun toStringSet(stringValue: String): Set<String> {
        return if (stringValue.isEmpty()) {
            emptySet()
        } else {
            stringValue.split(",").toSet()
        }
    }

    @TypeConverter
    fun fromConnectionAction(action: ConnectionAction): String {
        return action.name
    }

    @TypeConverter
    fun toConnectionAction(actionName: String): ConnectionAction {
        return try {
            ConnectionAction.valueOf(actionName)
        } catch (e: IllegalArgumentException) {
            ConnectionAction.CONNECT_ATTEMPT
        }
    }
}
