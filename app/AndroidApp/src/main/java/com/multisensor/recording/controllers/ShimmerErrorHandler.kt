package com.multisensor.recording.controllers

import com.multisensor.recording.persistence.ShimmerDeviceStateRepository
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShimmerErrorHandler @Inject constructor(
    private val shimmerDeviceStateRepository: ShimmerDeviceStateRepository
) {

    private val errorHandlingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    enum class ShimmerErrorType {
        CONNECTION_TIMEOUT,
        BLUETOOTH_DISABLED,
        BLUETOOTH_PERMISSION_DENIED,
        DEVICE_NOT_FOUND,
        DEVICE_ALREADY_CONNECTED,
        CONFIGURATION_FAILED,
        STREAMING_ERROR,
        SD_LOGGING_ERROR,
        BATTERY_LOW,
        SIGNAL_WEAK,
        FIRMWARE_INCOMPATIBLE,
        UNKNOWN_ERROR
    }

    data class ErrorHandlingStrategy(
        val shouldRetry: Boolean,
        val retryDelay: Long,
        val maxRetries: Int,
        val userActionRequired: Boolean,
        val userMessage: String,
        val technicalMessage: String
    )

    data class RetryConfiguration(
        val maxAttempts: Int = 3,
        val initialDelay: Long = 1000L,
        val maxDelay: Long = 30000L,
        val backoffMultiplier: Double = 2.0,
        val retryOnBluetoothDisabled: Boolean = false,
        val retryOnPermissionDenied: Boolean = false
    )

    private val activeRetries = mutableMapOf<String, RetryConfiguration>()

    fun classifyError(exception: Throwable?, errorMessage: String?): ShimmerErrorType {
        val message = errorMessage?.lowercase() ?: exception?.message?.lowercase() ?: ""

        return when {
            message.contains("timeout") || message.contains("connection timed out") -> ShimmerErrorType.CONNECTION_TIMEOUT
            message.contains("bluetooth") && message.contains("disabled") -> ShimmerErrorType.BLUETOOTH_DISABLED
            message.contains("permission") && message.contains("denied") -> ShimmerErrorType.BLUETOOTH_PERMISSION_DENIED
            message.contains("device not found") || message.contains("no device") -> ShimmerErrorType.DEVICE_NOT_FOUND
            message.contains("already connected") || message.contains("device busy") -> ShimmerErrorType.DEVICE_ALREADY_CONNECTED
            message.contains("configuration") && message.contains("failed") -> ShimmerErrorType.CONFIGURATION_FAILED
            message.contains("streaming") -> ShimmerErrorType.STREAMING_ERROR
            message.contains("sd logging") || message.contains("logging") -> ShimmerErrorType.SD_LOGGING_ERROR
            message.contains("battery") && message.contains("low") -> ShimmerErrorType.BATTERY_LOW
            message.contains("signal") && (message.contains("weak") || message.contains("low")) -> ShimmerErrorType.SIGNAL_WEAK
            message.contains("firmware") -> ShimmerErrorType.FIRMWARE_INCOMPATIBLE
            else -> ShimmerErrorType.UNKNOWN_ERROR
        }
    }

    fun getErrorHandlingStrategy(errorType: ShimmerErrorType, attemptNumber: Int = 1): ErrorHandlingStrategy {
        return when (errorType) {
            ShimmerErrorType.CONNECTION_TIMEOUT -> ErrorHandlingStrategy(
                shouldRetry = attemptNumber <= 3,
                retryDelay = (1000L * attemptNumber * 2),
                maxRetries = 3,
                userActionRequired = false,
                userMessage = "Connection timeout. Retrying in ${1 * attemptNumber * 2} seconds...",
                technicalMessage = "Bluetooth connection timeout after ${attemptNumber} attempts"
            )

            ShimmerErrorType.BLUETOOTH_DISABLED -> ErrorHandlingStrategy(
                shouldRetry = false,
                retryDelay = 0L,
                maxRetries = 0,
                userActionRequired = true,
                userMessage = "Please enable Bluetooth and try again",
                technicalMessage = "Bluetooth is disabled on device"
            )

            ShimmerErrorType.BLUETOOTH_PERMISSION_DENIED -> ErrorHandlingStrategy(
                shouldRetry = false,
                retryDelay = 0L,
                maxRetries = 0,
                userActionRequired = true,
                userMessage = "Bluetooth permission required. Please grant permission in settings",
                technicalMessage = "Bluetooth permission denied by user"
            )

            ShimmerErrorType.DEVICE_NOT_FOUND -> ErrorHandlingStrategy(
                shouldRetry = attemptNumber <= 2,
                retryDelay = 5000L,
                maxRetries = 2,
                userActionRequired = false,
                userMessage = "Device not found. Make sure it's powered on and in range",
                technicalMessage = "Shimmer device not discoverable via Bluetooth"
            )

            ShimmerErrorType.DEVICE_ALREADY_CONNECTED -> ErrorHandlingStrategy(
                shouldRetry = false,
                retryDelay = 0L,
                maxRetries = 0,
                userActionRequired = false,
                userMessage = "Device is already connected",
                technicalMessage = "Device connection already established"
            )

            ShimmerErrorType.CONFIGURATION_FAILED -> ErrorHandlingStrategy(
                shouldRetry = attemptNumber <= 2,
                retryDelay = 3000L,
                maxRetries = 2,
                userActionRequired = false,
                userMessage = "Configuration failed. Retrying...",
                technicalMessage = "Sensor configuration command failed"
            )

            ShimmerErrorType.STREAMING_ERROR -> ErrorHandlingStrategy(
                shouldRetry = attemptNumber <= 1,
                retryDelay = 2000L,
                maxRetries = 1,
                userActionRequired = false,
                userMessage = "Streaming error occurred. Attempting to restart...",
                technicalMessage = "Data streaming interrupted"
            )

            ShimmerErrorType.SD_LOGGING_ERROR -> ErrorHandlingStrategy(
                shouldRetry = attemptNumber <= 1,
                retryDelay = 2000L,
                maxRetries = 1,
                userActionRequired = false,
                userMessage = "SD logging error. Check SD card and retry...",
                technicalMessage = "SD card logging operation failed"
            )

            ShimmerErrorType.BATTERY_LOW -> ErrorHandlingStrategy(
                shouldRetry = false,
                retryDelay = 0L,
                maxRetries = 0,
                userActionRequired = true,
                userMessage = "Device battery is low. Please charge before continuing",
                technicalMessage = "Device battery level below operational threshold"
            )

            ShimmerErrorType.SIGNAL_WEAK -> ErrorHandlingStrategy(
                shouldRetry = false,
                retryDelay = 0L,
                maxRetries = 0,
                userActionRequired = true,
                userMessage = "Weak signal. Move closer to device or check interference",
                technicalMessage = "Bluetooth signal strength below reliable threshold"
            )

            ShimmerErrorType.FIRMWARE_INCOMPATIBLE -> ErrorHandlingStrategy(
                shouldRetry = false,
                retryDelay = 0L,
                maxRetries = 0,
                userActionRequired = true,
                userMessage = "Device firmware is incompatible. Please update firmware",
                technicalMessage = "Firmware version not supported by application"
            )

            ShimmerErrorType.UNKNOWN_ERROR -> ErrorHandlingStrategy(
                shouldRetry = attemptNumber <= 1,
                retryDelay = 5000L,
                maxRetries = 1,
                userActionRequired = false,
                userMessage = "Unknown error occurred. Retrying once...",
                technicalMessage = "Unclassified error in Shimmer operation"
            )
        }
    }

    suspend fun handleError(
        deviceAddress: String,
        deviceName: String,
        exception: Throwable?,
        errorMessage: String?,
        attemptNumber: Int,
        connectionType: ShimmerBluetoothManagerAndroid.BT_TYPE,
        onRetry: suspend () -> Boolean,
        onFinalFailure: (ErrorHandlingStrategy) -> Unit
    ): Boolean {
        val errorType = classifyError(exception, errorMessage)
        val strategy = getErrorHandlingStrategy(errorType, attemptNumber)

        android.util.Log.e("ShimmerErrorHandler", "[ERROR] Device: $deviceName ($deviceAddress)")
        android.util.Log.e("ShimmerErrorHandler", "[ERROR] Type: $errorType, Attempt: $attemptNumber")
        android.util.Log.e("ShimmerErrorHandler", "[ERROR] Message: ${errorMessage ?: exception?.message}")

        shimmerDeviceStateRepository.logConnectionAttempt(
            deviceAddress,
            false,
            strategy.technicalMessage,
            deviceName,
            connectionType
        )

        if (strategy.shouldRetry && attemptNumber <= strategy.maxRetries) {
            android.util.Log.d("ShimmerErrorHandler", "[RETRY] Scheduling retry in ${strategy.retryDelay}ms")

            delay(strategy.retryDelay)

            return try {
                android.util.Log.d("ShimmerErrorHandler", "[RETRY] Attempting retry $attemptNumber for $deviceName")
                onRetry()
            } catch (retryException: Exception) {
                android.util.Log.e("ShimmerErrorHandler", "[RETRY] Retry failed: ${retryException.message}")

                handleError(
                    deviceAddress,
                    deviceName,
                    retryException,
                    null,
                    attemptNumber + 1,
                    connectionType,
                    onRetry,
                    onFinalFailure
                )
            }
        } else {
            android.util.Log.e("ShimmerErrorHandler", "[FINAL] Maximum retries exceeded or retry not appropriate")
            onFinalFailure(strategy)
            return false
        }
    }

    suspend fun checkDeviceHealth(deviceAddress: String): List<String> {
        val recommendations = mutableListOf<String>()

        try {
            val deviceState = shimmerDeviceStateRepository.getDeviceState(deviceAddress)
            if (deviceState != null) {
                if (deviceState.batteryLevel in 1..20) {
                    recommendations.add("Battery level is low (${deviceState.batteryLevel}%). Consider charging.")
                }

                if (deviceState.signalStrength < -80) {
                    recommendations.add("Signal strength is weak. Move closer to device or reduce interference.")
                }

                val recentHistory = shimmerDeviceStateRepository.getConnectionHistory(deviceAddress, 10)
                val recentFailures = recentHistory.count { !it.success }
                if (recentFailures > 3) {
                    recommendations.add("Multiple recent connection failures detected. Check device status.")
                }

                val timeSinceLastConnection = System.currentTimeMillis() - deviceState.lastConnectedTimestamp
                if (timeSinceLastConnection > 24 * 60 * 60 * 1000) {
                    recommendations.add("Device hasn't been connected recently. Verify it's powered on.")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ShimmerErrorHandler", "Failed to check device health: ${e.message}")
            recommendations.add("Unable to check device health. Check connection.")
        }

        return recommendations
    }

    suspend fun generateDiagnosticReport(deviceAddress: String): String {
        return try {
            val deviceState = shimmerDeviceStateRepository.getDeviceState(deviceAddress)
            val connectionHistory = shimmerDeviceStateRepository.getConnectionHistory(deviceAddress, 20)
            val healthRecommendations = checkDeviceHealth(deviceAddress)

            buildString {
                append("=== Shimmer Device Diagnostic Report ===\n")
                append("Device: ${deviceState?.deviceName ?: "Unknown"}\n")
                append("Address: $deviceAddress\n")
                append("Generated: ${java.util.Date()}\n\n")

                append("=== Device State ===\n")
                if (deviceState != null) {
                    append("Connected: ${deviceState.isConnected}\n")
                    append("Connection Type: ${deviceState.connectionType}\n")
                    append("Battery Level: ${if (deviceState.batteryLevel >= 0) "${deviceState.batteryLevel}%" else "Unknown"}\n")
                    append("Signal Strength: ${if (deviceState.signalStrength != -1) "${deviceState.signalStrength} dBm" else "Unknown"}\n")
                    append("Last Connected: ${if (deviceState.lastConnectedTimestamp > 0) java.util.Date(deviceState.lastConnectedTimestamp) else "Never"}\n")
                    append("Auto-reconnect: ${deviceState.autoReconnectEnabled}\n")
                    append("Connection Attempts: ${deviceState.connectionAttempts}\n")
                    append("Last Error: ${deviceState.lastConnectionError ?: "None"}\n\n")
                } else {
                    append("No device state found\n\n")
                }

                append("=== Recent Connection History ===\n")
                if (connectionHistory.isNotEmpty()) {
                    connectionHistory.take(10).forEach { history ->
                        append("${java.util.Date(history.timestamp)}: ${history.action} - ${if (history.success) "SUCCESS" else "FAILED"}")
                        if (!history.success && history.errorMessage != null) {
                            append(" (${history.errorMessage})")
                        }
                        append("\n")
                    }
                } else {
                    append("No connection history found\n")
                }

                append("\n=== Health Recommendations ===\n")
                if (healthRecommendations.isNotEmpty()) {
                    healthRecommendations.forEach { recommendation ->
                        append("• $recommendation\n")
                    }
                } else {
                    append("No specific recommendations\n")
                }
            }
        } catch (e: Exception) {
            "Failed to generate diagnostic report: ${e.message}"
        }
    }

    suspend fun resetErrorState(deviceAddress: String) {
        activeRetries.remove(deviceAddress)
        try {
            val deviceState = shimmerDeviceStateRepository.getDeviceState(deviceAddress)
            if (deviceState != null) {
                val updatedState = deviceState.copy(
                    connectionAttempts = 0,
                    lastConnectionError = null,
                    lastUpdated = System.currentTimeMillis()
                )
                shimmerDeviceStateRepository.saveDeviceState(updatedState)
            }
        } catch (e: Exception) {
            android.util.Log.e("ShimmerErrorHandler", "Failed to reset error state: ${e.message}")
        }
        android.util.Log.d("ShimmerErrorHandler", "[RESET] Error state reset for $deviceAddress")
    }
}
