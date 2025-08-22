package com.multisensor.recording.recording

import com.multisensor.recording.util.Logger
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class DeviceStatusTracker(
    private val logger: Logger
) {

    data class DeviceStatus(
        val deviceId: String,
        val macAddress: String,
        val deviceName: String,
        val connectionState: ConnectionState,
        val operatingMode: OperatingMode,
        val communicationHealth: CommunicationHealth,
        val batteryLevel: Int,
        val signalStrength: Int,
        val lastSeen: Long,
        val dataSchema: DataSchema,
        val isPairedWithPC: Boolean,
        val firmwareVersion: String,
        val hardwareVersion: String,
        val totalSamples: Long,
        val samplingRate: Double,
        val enabledSensors: Set<DeviceConfiguration.SensorChannel>,
        val connectionType: String,
        val reconnectionAttempts: Int,
        val errorCount: Int,
        val lastError: String?
    ) {
        fun isFullyOperational(): Boolean =
            connectionState == ConnectionState.CONNECTED &&
                    communicationHealth == CommunicationHealth.EXCELLENT &&
                    batteryLevel > 20

        fun getDisplaySummary(): String = buildString {
            append("Device: $deviceName ($deviceId)\n")
            append("Status: $connectionState | Mode: $operatingMode\n")
            append("Health: $communicationHealth | Battery: $batteryLevel%\n")
            append("Sensors: ${enabledSensors.size} | Rate: ${samplingRate}Hz\n")
            append("Samples: $totalSamples | Type: $connectionType\n")
            if (isPairedWithPC) append("PC Paired: Yes\n")
            if (errorCount > 0) append("Errors: $errorCount")
        }
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        ERROR,
        PAIRING
    }

    enum class OperatingMode {
        IDLE,
        STREAMING,
        SD_LOGGING,
        BOTH,
        CONFIGURING,
        UNKNOWN
    }

    enum class CommunicationHealth {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR,
        CRITICAL,
        UNKNOWN
    }

    data class DataSchema(
        val version: String,
        val sensorChannels: Map<String, String>,
        val samplingRateRange: Pair<Double, Double>,
        val supportedModes: Set<OperatingMode>,
        val dataFormat: String,
        val isValidated: Boolean
    ) {
        companion object {
            fun createShimmer3GSRPlusSchema(): DataSchema = DataSchema(
                version = "3.2.3",
                sensorChannels = mapOf(
                    "GSR" to "Double",
                    "PPG" to "Double",
                    "Accel_X" to "Double",
                    "Accel_Y" to "Double",
                    "Accel_Z" to "Double",
                    "Gyro_X" to "Double",
                    "Gyro_Y" to "Double",
                    "Gyro_Z" to "Double",
                    "Mag_X" to "Double",
                    "Mag_Y" to "Double",
                    "Mag_Z" to "Double",
                    "ECG" to "Double",
                    "EMG" to "Double",
                    "Battery" to "Double",
                    "Timestamp" to "Long"
                ),
                samplingRateRange = 1.0 to 1000.0,
                supportedModes = setOf(
                    OperatingMode.IDLE,
                    OperatingMode.STREAMING,
                    OperatingMode.SD_LOGGING,
                    OperatingMode.BOTH
                ),
                dataFormat = "ObjectCluster",
                isValidated = true
            )
        }
    }

    interface StatusListener {
        fun onDeviceStatusChanged(deviceId: String, status: DeviceStatus)
        fun onConnectionStateChanged(deviceId: String, state: ConnectionState)
        fun onOperatingModeChanged(deviceId: String, mode: OperatingMode)
        fun onCommunicationHealthChanged(deviceId: String, health: CommunicationHealth)
        fun onDeviceError(deviceId: String, error: String)
    }

    private val deviceStatuses = ConcurrentHashMap<String, DeviceStatus>()
    private val statusListeners = mutableSetOf<StatusListener>()
    private val lastPacketTimes = ConcurrentHashMap<String, AtomicLong>()
    private val packetCounts = ConcurrentHashMap<String, AtomicLong>()
    private val errorCounts = ConcurrentHashMap<String, AtomicLong>()

    private val monitoringScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isMonitoring = AtomicBoolean(false)

    fun startMonitoring() {
        if (isMonitoring.compareAndSet(false, true)) {
            logger.info("Starting complete device status monitoring")

            monitoringScope.launch {
                while (isMonitoring.get()) {
                    updateAllDeviceStatuses()
                    delay(1000)
                }
            }

            monitoringScope.launch {
                while (isMonitoring.get()) {
                    monitorCommunicationHealth()
                    delay(5000)
                }
            }
        }
    }

    fun stopMonitoring() {
        if (isMonitoring.compareAndSet(true, false)) {
            logger.info("Stopping device status monitoring")
            monitoringScope.cancel()
        }
    }

    fun registerDevice(
        deviceId: String,
        macAddress: String,
        deviceName: String,
        connectionType: String = "Classic"
    ) {
        logger.info("Registering device for status tracking: $deviceName ($deviceId)")

        val initialStatus = DeviceStatus(
            deviceId = deviceId,
            macAddress = macAddress,
            deviceName = deviceName,
            connectionState = ConnectionState.DISCONNECTED,
            operatingMode = OperatingMode.UNKNOWN,
            communicationHealth = CommunicationHealth.UNKNOWN,
            batteryLevel = 0,
            signalStrength = 0,
            lastSeen = System.currentTimeMillis(),
            dataSchema = DataSchema.createShimmer3GSRPlusSchema(),
            isPairedWithPC = false,
            firmwareVersion = "",
            hardwareVersion = "",
            totalSamples = 0L,
            samplingRate = 0.0,
            enabledSensors = emptySet(),
            connectionType = connectionType,
            reconnectionAttempts = 0,
            errorCount = 0,
            lastError = null
        )

        deviceStatuses[deviceId] = initialStatus
        lastPacketTimes[deviceId] = AtomicLong(System.currentTimeMillis())
        packetCounts[deviceId] = AtomicLong(0L)
        errorCounts[deviceId] = AtomicLong(0L)

        notifyStatusChange(deviceId, initialStatus)
    }

    fun updateConnectionState(deviceId: String, state: ConnectionState) {
        deviceStatuses[deviceId]?.let { current ->
            val updated = current.copy(
                connectionState = state,
                lastSeen = System.currentTimeMillis()
            )
            deviceStatuses[deviceId] = updated

            logger.debug("Device $deviceId connection state: $state")
            notifyConnectionStateChange(deviceId, state)
            notifyStatusChange(deviceId, updated)
        }
    }

    fun updateOperatingMode(deviceId: String, mode: OperatingMode) {
        deviceStatuses[deviceId]?.let { current ->
            val updated = current.copy(
                operatingMode = mode,
                lastSeen = System.currentTimeMillis()
            )
            deviceStatuses[deviceId] = updated

            logger.debug("Device $deviceId operating mode: $mode")
            notifyModeChange(deviceId, mode)
            notifyStatusChange(deviceId, updated)
        }
    }

    fun updateDeviceInfo(
        deviceId: String,
        batteryLevel: Int? = null,
        firmwareVersion: String? = null,
        hardwareVersion: String? = null,
        samplingRate: Double? = null,
        enabledSensors: Set<DeviceConfiguration.SensorChannel>? = null,
        isPairedWithPC: Boolean? = null
    ) {
        deviceStatuses[deviceId]?.let { current ->
            val updated = current.copy(
                batteryLevel = batteryLevel ?: current.batteryLevel,
                firmwareVersion = firmwareVersion ?: current.firmwareVersion,
                hardwareVersion = hardwareVersion ?: current.hardwareVersion,
                samplingRate = samplingRate ?: current.samplingRate,
                enabledSensors = enabledSensors ?: current.enabledSensors,
                isPairedWithPC = isPairedWithPC ?: current.isPairedWithPC,
                lastSeen = System.currentTimeMillis()
            )
            deviceStatuses[deviceId] = updated
            notifyStatusChange(deviceId, updated)
        }
    }

    fun recordPacketReceived(deviceId: String) {
        lastPacketTimes[deviceId]?.set(System.currentTimeMillis())
        packetCounts[deviceId]?.incrementAndGet()

        deviceStatuses[deviceId]?.let { current ->
            val updated = current.copy(
                totalSamples = packetCounts[deviceId]?.get() ?: 0L,
                lastSeen = System.currentTimeMillis()
            )
            deviceStatuses[deviceId] = updated
        }
    }

    fun recordError(deviceId: String, error: String) {
        errorCounts[deviceId]?.incrementAndGet()

        deviceStatuses[deviceId]?.let { current ->
            val updated = current.copy(
                errorCount = errorCounts[deviceId]?.get()?.toInt() ?: 0,
                lastError = error,
                lastSeen = System.currentTimeMillis()
            )
            deviceStatuses[deviceId] = updated

            logger.error("Device $deviceId error: $error")
            notifyError(deviceId, error)
            notifyStatusChange(deviceId, updated)
        }
    }

    fun getDeviceStatus(deviceId: String): DeviceStatus? = deviceStatuses[deviceId]

    fun getAllDeviceStatuses(): Map<String, DeviceStatus> = deviceStatuses.toMap()

    fun isDeviceOperational(deviceId: String): Boolean =
        getDeviceStatus(deviceId)?.isFullyOperational() ?: false

    fun getDevicesByState(state: ConnectionState): List<DeviceStatus> =
        deviceStatuses.values.filter { it.connectionState == state }

    fun getDevicesByMode(mode: OperatingMode): List<DeviceStatus> =
        deviceStatuses.values.filter { it.operatingMode == mode }

    fun addStatusListener(listener: StatusListener) {
        statusListeners.add(listener)
    }

    fun removeStatusListener(listener: StatusListener) {
        statusListeners.remove(listener)
    }

    private suspend fun monitorCommunicationHealth() {
        deviceStatuses.keys.forEach { deviceId ->
            val currentTime = System.currentTimeMillis()
            val lastPacketTime = lastPacketTimes[deviceId]?.get() ?: 0L
            val timeSinceLastPacket = currentTime - lastPacketTime

            val health = when {
                timeSinceLastPacket > 30000 -> CommunicationHealth.CRITICAL
                timeSinceLastPacket > 10000 -> CommunicationHealth.POOR
                timeSinceLastPacket > 5000 -> CommunicationHealth.FAIR
                timeSinceLastPacket > 2000 -> CommunicationHealth.GOOD
                else -> CommunicationHealth.EXCELLENT
            }

            deviceStatuses[deviceId]?.let { current ->
                if (current.communicationHealth != health) {
                    val updated = current.copy(communicationHealth = health)
                    deviceStatuses[deviceId] = updated

                    logger.debug("Device $deviceId communication health: $health")
                    notifyHealthChange(deviceId, health)
                    notifyStatusChange(deviceId, updated)
                }
            }
        }
    }

    private suspend fun updateAllDeviceStatuses() {
        deviceStatuses.keys.forEach { deviceId ->
            deviceStatuses[deviceId]?.let { current ->
                if (current.connectionState == ConnectionState.CONNECTED) {
                    val updated = current.copy(lastSeen = System.currentTimeMillis())
                    deviceStatuses[deviceId] = updated
                }
            }
        }
    }

    private fun notifyStatusChange(deviceId: String, status: DeviceStatus) {
        statusListeners.forEach { listener ->
            try {
                listener.onDeviceStatusChanged(deviceId, status)
            } catch (e: RuntimeException) {
                logger.error("Runtime error notifying status listener", e)
            }
        }
    }

    private fun notifyConnectionStateChange(deviceId: String, state: ConnectionState) {
        statusListeners.forEach { listener ->
            try {
                listener.onConnectionStateChanged(deviceId, state)
            } catch (e: RuntimeException) {
                logger.error("Runtime error notifying connection state listener", e)
            }
        }
    }

    private fun notifyModeChange(deviceId: String, mode: OperatingMode) {
        statusListeners.forEach { listener ->
            try {
                listener.onOperatingModeChanged(deviceId, mode)
            } catch (e: Exception) {
                logger.error("Error notifying mode change listener", e)
            }
        }
    }

    private fun notifyHealthChange(deviceId: String, health: CommunicationHealth) {
        statusListeners.forEach { listener ->
            try {
                listener.onCommunicationHealthChanged(deviceId, health)
            } catch (e: Exception) {
                logger.error("Error notifying health change listener", e)
            }
        }
    }

    private fun notifyError(deviceId: String, error: String) {
        statusListeners.forEach { listener ->
            try {
                listener.onDeviceError(deviceId, error)
            } catch (e: Exception) {
                logger.error("Error notifying error listener", e)
            }
        }
    }

    fun cleanup() {
        stopMonitoring()
        deviceStatuses.clear()
        statusListeners.clear()
        lastPacketTimes.clear()
        packetCounts.clear()
        errorCounts.clear()
        logger.info("DeviceStatusTracker cleanup completed")
    }
}
