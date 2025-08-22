package com.multisensor.recording.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "session_state")
@TypeConverters(DeviceStateListConverter::class)
data class SessionState(
    @PrimaryKey
    val sessionId: String,
    val recordingState: RecordingState,
    val deviceStates: List<DeviceState>,
    val timestamp: Long,
    val videoEnabled: Boolean = false,
    val rawEnabled: Boolean = false,
    val thermalEnabled: Boolean = false,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val errorOccurred: Boolean = false,
    val errorMessage: String? = null
)

enum class RecordingState {
    IDLE,
    STARTING,
    RECORDING,
    STOPPING,
    COMPLETED,
    FAILED
}

data class DeviceState(
    val deviceId: String,
    val deviceType: String,
    val connected: Boolean,
    val batteryLevel: Int = -1,
    val status: String = "unknown"
)

class DeviceStateListConverter {
    @TypeConverter
    fun fromDeviceStateList(value: List<DeviceState>): String {
        return value.joinToString("|") { "${it.deviceId},${it.deviceType},${it.connected},${it.batteryLevel},${it.status}" }
    }

    @TypeConverter
    fun toDeviceStateList(value: String): List<DeviceState> {
        if (value.isEmpty()) return emptyList()
        return value.split("|").map { deviceString ->
            val parts = deviceString.split(",")
            DeviceState(
                deviceId = parts[0],
                deviceType = parts[1],
                connected = parts[2].toBoolean(),
                batteryLevel = parts[3].toInt(),
                status = parts[4]
            )
        }
    }
}
