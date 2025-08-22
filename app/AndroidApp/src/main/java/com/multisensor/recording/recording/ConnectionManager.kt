package com.multisensor.recording.recording

import com.multisensor.recording.util.Logger
import kotlinx.coroutines.*
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class ConnectionManager(
    private val logger: Logger
) {

    data class ConnectionPolicy(
        val maxRetryAttempts: Int = 5,
        val initialRetryDelay: Long = 2000L,
        val maxRetryDelay: Long = 30000L,
        val exponentialBackoff: Boolean = true,
        val enableAutoReconnect: Boolean = true,
        val healthCheckInterval: Long = 10000L,
        val connectionTimeout: Long = 30000L,
        val enableConnectionPersistence: Boolean = true
    )

    data class ConnectionAttempt(
        val deviceId: String,
        val attemptNumber: Int,
        val timestamp: Long,
        val success: Boolean,
        val errorMessage: String? = null,
        val duration: Long = 0L
    )

    data class ConnectionHealth(
        val deviceId: String,
        val isHealthy: Boolean,
        val lastSuccessfulConnection: Long,
        val consecutiveFailures: Int,
        val averageConnectionTime: Long,
        val packetLossRate: Double,
        val signalStrength: Int
    )

    private val policy = ConnectionPolicy()
    private val connectionAttempts = ConcurrentHashMap<String, MutableList<ConnectionAttempt>>()
    private val connectionHealth = ConcurrentHashMap<String, ConnectionHealth>()
    private val reconnectionJobs = ConcurrentHashMap<String, Job>()
    private val healthMonitoringJobs = ConcurrentHashMap<String, Job>()

    private val connectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isManaging = AtomicBoolean(false)

    private val totalConnectionAttempts = AtomicLong(0L)
    private val successfulConnections = AtomicLong(0L)
    private val failedConnections = AtomicLong(0L)

    fun startManagement() {
        if (isManaging.compareAndSet(false, true)) {
            logger.info("Starting enhanced connection management")

            connectionScope.launch {
                monitorOverallHealth()
            }
        }
    }

    fun stopManagement() {
        if (isManaging.compareAndSet(true, false)) {
            logger.info("Stopping connection management")

            reconnectionJobs.values.forEach { it.cancel() }
            healthMonitoringJobs.values.forEach { it.cancel() }
            connectionScope.cancel()

            reconnectionJobs.clear()
            healthMonitoringJobs.clear()
        }
    }

    suspend fun connectWithRetry(
        deviceId: String,
        connectionFunction: suspend () -> Boolean
    ): Boolean = withContext(Dispatchers.IO) {

        logger.info("Starting connection attempt for device: $deviceId")
        var attempt = 1
        var delay = policy.initialRetryDelay

        while (attempt <= policy.maxRetryAttempts && isManaging.get()) {
            val startTime = System.currentTimeMillis()

            try {
                logger.debug("Connection attempt $attempt/${policy.maxRetryAttempts} for device: $deviceId")

                val success = connectionFunction()
                val duration = System.currentTimeMillis() - startTime

                recordConnectionAttempt(deviceId, attempt, true, null, duration)

                if (success) {
                    logger.info("Successfully connected to device: $deviceId (attempt $attempt)")
                    updateConnectionHealth(deviceId, true)
                    successfulConnections.incrementAndGet()
                    return@withContext true
                } else {
                    recordConnectionAttempt(deviceId, attempt, false, "Connection function returned false", duration)
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                val duration = System.currentTimeMillis() - startTime
                val errorMessage = e.message ?: "Connection IO error"

                logger.warning("Connection attempt $attempt failed for device $deviceId: $errorMessage")
                recordConnectionAttempt(deviceId, attempt, false, errorMessage, duration)
                updateConnectionHealth(deviceId, false)
            } catch (e: SecurityException) {
                val duration = System.currentTimeMillis() - startTime
                val errorMessage = e.message ?: "Connection security error"

                logger.warning("Connection attempt $attempt failed for device $deviceId: $errorMessage")
                recordConnectionAttempt(deviceId, attempt, false, errorMessage, duration)
                updateConnectionHealth(deviceId, false)
            }

            if (attempt < policy.maxRetryAttempts) {
                logger.debug("Waiting ${delay}ms before next attempt for device: $deviceId")
                delay(delay)

                if (policy.exponentialBackoff) {
                    delay = (delay * 1.5).toLong().coerceAtMost(policy.maxRetryDelay)
                }
            }

            attempt++
        }

        logger.error("Failed to connect to device $deviceId after ${policy.maxRetryAttempts} attempts")
        failedConnections.incrementAndGet()
        false
    }

    fun startAutoReconnection(
        deviceId: String,
        connectionFunction: suspend () -> Boolean
    ) {
        if (!policy.enableAutoReconnect) return

        reconnectionJobs[deviceId]?.cancel()

        val job = connectionScope.launch {
            while (isManaging.get()) {
                try {
                    delay(policy.healthCheckInterval)

                    val health = connectionHealth[deviceId]
                    if (health?.isHealthy == false) {
                        logger.info("Attempting automatic reconnection for device: $deviceId")
                        connectWithRetry(deviceId, connectionFunction)
                    }
                } catch (e: Exception) {
                    logger.error("Error in auto-reconnection for device $deviceId", e)
                }
            }
        }

        reconnectionJobs[deviceId] = job
    }

    fun stopAutoReconnection(deviceId: String) {
        reconnectionJobs[deviceId]?.cancel()
        reconnectionJobs.remove(deviceId)
        logger.debug("Stopped auto-reconnection for device: $deviceId")
    }

    fun startHealthMonitoring(deviceId: String) {
        healthMonitoringJobs[deviceId]?.cancel()

        val job = connectionScope.launch {
            while (isManaging.get()) {
                try {
                    delay(policy.healthCheckInterval)
                    checkDeviceHealth(deviceId)
                } catch (e: Exception) {
                    logger.error("Error in health monitoring for device $deviceId", e)
                }
            }
        }

        healthMonitoringJobs[deviceId] = job
    }

    fun stopHealthMonitoring(deviceId: String) {
        healthMonitoringJobs[deviceId]?.cancel()
        healthMonitoringJobs.remove(deviceId)
        logger.debug("Stopped health monitoring for device: $deviceId")
    }

    private fun recordConnectionAttempt(
        deviceId: String,
        attemptNumber: Int,
        success: Boolean,
        errorMessage: String?,
        duration: Long
    ) {
        val attempt = ConnectionAttempt(
            deviceId = deviceId,
            attemptNumber = attemptNumber,
            timestamp = System.currentTimeMillis(),
            success = success,
            errorMessage = errorMessage,
            duration = duration
        )

        connectionAttempts.computeIfAbsent(deviceId) { mutableListOf() }.add(attempt)
        totalConnectionAttempts.incrementAndGet()

        logger.debug("Recorded connection attempt for $deviceId: success=$success, duration=${duration}ms")
    }

    private fun updateConnectionHealth(deviceId: String, isHealthy: Boolean) {
        val currentHealth = connectionHealth[deviceId]
        val currentTime = System.currentTimeMillis()

        val attempts = connectionAttempts[deviceId] ?: emptyList()
        val recentAttempts = attempts.filter { currentTime - it.timestamp < 300000 }

        val consecutiveFailures = if (isHealthy) 0 else (currentHealth?.consecutiveFailures ?: 0) + 1
        val avgConnectionTime = if (recentAttempts.isNotEmpty()) {
            recentAttempts.map { it.duration }.average().toLong()
        } else {
            currentHealth?.averageConnectionTime ?: 0L
        }

        val packetLossRate = calculatePacketLossRate(recentAttempts)

        val updatedHealth = ConnectionHealth(
            deviceId = deviceId,
            isHealthy = isHealthy,
            lastSuccessfulConnection = if (isHealthy) currentTime else (currentHealth?.lastSuccessfulConnection ?: 0L),
            consecutiveFailures = consecutiveFailures,
            averageConnectionTime = avgConnectionTime,
            packetLossRate = packetLossRate,
            signalStrength = currentHealth?.signalStrength ?: 0
        )

        connectionHealth[deviceId] = updatedHealth
        logger.debug("Updated health for device $deviceId: healthy=$isHealthy, failures=$consecutiveFailures")
    }

    private suspend fun checkDeviceHealth(deviceId: String) {

        val health = connectionHealth[deviceId]
        if (health != null) {
            val isHealthy = health.consecutiveFailures < 3

            if (!isHealthy && health.isHealthy) {
                logger.warning("Device $deviceId health degraded - consecutive failures: ${health.consecutiveFailures}")
            }
        }
    }

    private suspend fun monitorOverallHealth() {
        while (isManaging.get()) {
            try {
                val unhealthyDevices = connectionHealth.values.filter { !it.isHealthy }

                if (unhealthyDevices.isNotEmpty()) {
                    logger.warning("${unhealthyDevices.size} devices have connection health issues")
                    unhealthyDevices.forEach { health ->
                        logger.debug("Unhealthy device: ${health.deviceId} - failures: ${health.consecutiveFailures}")
                    }
                }

                delay(30000)
            } catch (e: Exception) {
                logger.error("Error in overall health monitoring", e)
                delay(30000)
            }
        }
    }

    private fun calculatePacketLossRate(attempts: List<ConnectionAttempt>): Double {
        if (attempts.isEmpty()) return 0.0

        val totalAttempts = attempts.size
        val successfulAttempts = attempts.count { it.success }

        return ((totalAttempts - successfulAttempts).toDouble() / totalAttempts) * 100.0
    }

    fun getConnectionStatistics(deviceId: String): Map<String, Any> {
        val attempts = connectionAttempts[deviceId] ?: emptyList()
        val health = connectionHealth[deviceId]

        return mapOf(
            "deviceId" to deviceId,
            "totalAttempts" to attempts.size,
            "successfulAttempts" to attempts.count { it.success },
            "failedAttempts" to attempts.count { !it.success },
            "averageConnectionTime" to (health?.averageConnectionTime ?: 0L),
            "consecutiveFailures" to (health?.consecutiveFailures ?: 0),
            "packetLossRate" to (health?.packetLossRate ?: 0.0),
            "isHealthy" to (health?.isHealthy ?: false),
            "lastSuccessfulConnection" to (health?.lastSuccessfulConnection ?: 0L)
        )
    }

    fun getOverallStatistics(): Map<String, Any> = mapOf(
        "totalDevices" to connectionHealth.size,
        "healthyDevices" to connectionHealth.values.count { it.isHealthy },
        "unhealthyDevices" to connectionHealth.values.count { !it.isHealthy },
        "totalConnectionAttempts" to totalConnectionAttempts.get(),
        "successfulConnections" to successfulConnections.get(),
        "failedConnections" to failedConnections.get(),
        "successRate" to if (totalConnectionAttempts.get() > 0) {
            (successfulConnections.get().toDouble() / totalConnectionAttempts.get()) * 100.0
        } else {
            0.0
        },
        "isManaging" to isManaging.get()
    )

    fun getConnectionHealth(deviceId: String): ConnectionHealth? = connectionHealth[deviceId]

    fun getAllConnectionHealth(): Map<String, ConnectionHealth> = connectionHealth.toMap()

    fun resetDeviceStatistics(deviceId: String) {
        connectionAttempts.remove(deviceId)
        connectionHealth.remove(deviceId)
        logger.info("Reset statistics for device: $deviceId")
    }

    fun resetAllStatistics() {
        connectionAttempts.clear()
        connectionHealth.clear()
        totalConnectionAttempts.set(0L)
        successfulConnections.set(0L)
        failedConnections.set(0L)
        logger.info("Reset all connection statistics")
    }

    fun cleanup() {
        stopManagement()
        connectionAttempts.clear()
        connectionHealth.clear()
        logger.info("ConnectionManager cleanup completed")
    }
}
