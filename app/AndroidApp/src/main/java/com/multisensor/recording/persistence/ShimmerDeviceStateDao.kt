package com.multisensor.recording.persistence

import androidx.room.*
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import kotlinx.coroutines.flow.Flow

@Dao
interface ShimmerDeviceStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceState(deviceState: ShimmerDeviceState)

    @Update
    suspend fun updateDeviceState(deviceState: ShimmerDeviceState)

    @Delete
    suspend fun deleteDeviceState(deviceState: ShimmerDeviceState)

    @Query("SELECT * FROM shimmer_device_state WHERE deviceAddress = :address")
    suspend fun getDeviceState(address: String): ShimmerDeviceState?

    @Query("SELECT * FROM shimmer_device_state ORDER BY lastUpdated DESC")
    suspend fun getAllDeviceStates(): List<ShimmerDeviceState>

    @Query("SELECT * FROM shimmer_device_state WHERE isConnected = 1 ORDER BY preferredConnectionOrder ASC")
    suspend fun getConnectedDevices(): List<ShimmerDeviceState>

    @Query("SELECT * FROM shimmer_device_state WHERE autoReconnectEnabled = 1 AND isConnected = 0 ORDER BY preferredConnectionOrder ASC")
    suspend fun getAutoReconnectDevices(): List<ShimmerDeviceState>

    @Query("SELECT * FROM shimmer_device_state WHERE connectionType = :connectionType ORDER BY lastUpdated DESC")
    suspend fun getDevicesByConnectionType(connectionType: ShimmerBluetoothManagerAndroid.BT_TYPE): List<ShimmerDeviceState>

    @Query("SELECT * FROM shimmer_device_state WHERE isStreaming = 1")
    suspend fun getStreamingDevices(): List<ShimmerDeviceState>

    @Query("SELECT * FROM shimmer_device_state WHERE isSDLogging = 1")
    suspend fun getSDLoggingDevices(): List<ShimmerDeviceState>

    @Query("UPDATE shimmer_device_state SET isConnected = :connected, lastConnectedTimestamp = :timestamp, lastUpdated = :updateTime WHERE deviceAddress = :address")
    suspend fun updateConnectionStatus(
        address: String,
        connected: Boolean,
        timestamp: Long,
        updateTime: Long = System.currentTimeMillis()
    )

    @Query("UPDATE shimmer_device_state SET enabledSensors = :sensors, samplingRate = :samplingRate, gsrRange = :gsrRange, sensorConfiguration = :config, lastUpdated = :updateTime WHERE deviceAddress = :address")
    suspend fun updateSensorConfiguration(
        address: String,
        sensors: Set<String>,
        samplingRate: Double,
        gsrRange: Int,
        config: String?,
        updateTime: Long = System.currentTimeMillis()
    )

    @Query("UPDATE shimmer_device_state SET isStreaming = :streaming, lastStreamingTimestamp = :timestamp, lastUpdated = :updateTime WHERE deviceAddress = :address")
    suspend fun updateStreamingStatus(
        address: String,
        streaming: Boolean,
        timestamp: Long,
        updateTime: Long = System.currentTimeMillis()
    )

    @Query("UPDATE shimmer_device_state SET isSDLogging = :logging, lastSDLoggingTimestamp = :timestamp, lastUpdated = :updateTime WHERE deviceAddress = :address")
    suspend fun updateSDLoggingStatus(
        address: String,
        logging: Boolean,
        timestamp: Long,
        updateTime: Long = System.currentTimeMillis()
    )

    @Query("UPDATE shimmer_device_state SET batteryLevel = :battery, signalStrength = :signal, firmwareVersion = :firmware, lastUpdated = :updateTime WHERE deviceAddress = :address")
    suspend fun updateDeviceInfo(
        address: String,
        battery: Int,
        signal: Int,
        firmware: String,
        updateTime: Long = System.currentTimeMillis()
    )

    @Query("UPDATE shimmer_device_state SET connectionAttempts = connectionAttempts + 1, lastConnectionError = :error, lastUpdated = :updateTime WHERE deviceAddress = :address")
    suspend fun incrementConnectionAttempts(
        address: String,
        error: String?,
        updateTime: Long = System.currentTimeMillis()
    )

    @Query("UPDATE shimmer_device_state SET connectionAttempts = 0, lastConnectionError = NULL, lastUpdated = :updateTime WHERE deviceAddress = :address")
    suspend fun resetConnectionAttempts(address: String, updateTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM shimmer_device_state WHERE lastUpdated < :cutoffTime")
    suspend fun deleteOldDeviceStates(cutoffTime: Long)

    @Query("SELECT * FROM shimmer_device_state ORDER BY lastUpdated DESC")
    fun observeAllDeviceStates(): Flow<List<ShimmerDeviceState>>

    @Query("SELECT * FROM shimmer_device_state WHERE isConnected = 1 ORDER BY preferredConnectionOrder ASC")
    fun observeConnectedDevices(): Flow<List<ShimmerDeviceState>>

    @Query("SELECT * FROM shimmer_device_state WHERE deviceAddress = :address")
    fun observeDeviceState(address: String): Flow<ShimmerDeviceState?>

    @Insert
    suspend fun insertConnectionHistory(history: ShimmerConnectionHistory)

    @Query("SELECT * FROM shimmer_connection_history WHERE deviceAddress = :address ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getConnectionHistory(address: String, limit: Int = 50): List<ShimmerConnectionHistory>

    @Query("SELECT * FROM shimmer_connection_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentConnectionHistory(limit: Int = 100): List<ShimmerConnectionHistory>

    @Query("SELECT * FROM shimmer_connection_history WHERE success = 0 AND action = 'CONNECT_ATTEMPT' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getFailedConnectionAttempts(limit: Int = 50): List<ShimmerConnectionHistory>

    @Query("DELETE FROM shimmer_connection_history WHERE timestamp < :cutoffTime")
    suspend fun deleteOldConnectionHistory(cutoffTime: Long)

    @Query("SELECT COUNT(*) FROM shimmer_connection_history WHERE deviceAddress = :address AND action = 'CONNECT_ATTEMPT' AND timestamp > :since")
    suspend fun countConnectionAttempts(address: String, since: Long): Int
}
