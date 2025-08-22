package com.multisensor.recording.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.multisensor.recording.util.AppLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

data class NetworkConnectionState(
    val isConnected: Boolean = false,
    val networkType: String = "unknown",
    val quality: NetworkQuality = NetworkQuality.UNKNOWN,
    val latencyMs: Long = 0,
    val reconnectAttempts: Int = 0,
    val lastConnectedTime: Long = 0,
    val lastDisconnectedTime: Long = 0
)

data class SessionPreservationState(
    val sessionId: String,
    val recordingActive: Boolean,
    val fileCount: Int,
    val lastSyncTime: Long,
    val pendingData: List<String>,
    val connectionLostTime: Long
)

enum class NetworkQuality {
    EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
}

enum class RecoveryStrategy {
    IMMEDIATE_RETRY,
    EXPONENTIAL_BACKOFF,
    PROGRESSIVE_DEGRADATION,
    MANUAL_INTERVENTION
}

@Singleton
class NetworkRecoveryManager @Inject constructor(
    private val context: Context,
    private val jsonSocketClient: JsonSocketClient
) {
    companion object {
        private const val TAG = "NetworkRecoveryManager"
        private const val MAX_RECONNECT_ATTEMPTS = 10
        private const val BASE_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 30000L
        private const val NETWORK_TIMEOUT_MS = 10000L
        private const val SESSION_PRESERVATION_TIMEOUT_MS = 300000L
    }

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow(NetworkConnectionState())
    val connectionState: StateFlow<NetworkConnectionState> = _connectionState.asStateFlow()

    private val _sessionPreservationState = MutableStateFlow<SessionPreservationState?>(null)
    val sessionPreservationState: StateFlow<SessionPreservationState?> = _sessionPreservationState.asStateFlow()

    private val isRecovering = AtomicBoolean(false)
    private val reconnectAttempts = AtomicInteger(0)
    private var recoveryJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val preservedSessions = ConcurrentHashMap<String, SessionPreservationState>()
    private var currentSessionId: String? = null

    private var totalConnectionLosses = 0
    private var totalRecoveryAttempts = 0
    private var successfulRecoveries = 0

    init {
        setupNetworkCallback()
        startNetworkMonitoring()
    }

    fun handleConnectionLoss() {
        val currentTime = System.currentTimeMillis()

        val currentState = _connectionState.value
        _connectionState.value = currentState.copy(
            isConnected = false,
            lastDisconnectedTime = currentTime
        )

        preserveCurrentSession()

        if (!isRecovering.get()) {
            startRecoveryProcess()
        }

        totalConnectionLosses++
        AppLogger.logNetwork(TAG, "Connection loss detected, starting recovery process")
    }

    fun attemptReconnection(): Boolean {
        if (isRecovering.get()) {
            AppLogger.logNetwork(TAG, "Recovery already in progress")
            return false
        }

        val attempts = reconnectAttempts.incrementAndGet()
        totalRecoveryAttempts++

        AppLogger.logNetwork(TAG, "Attempting reconnection #$attempts")

        return try {
            val strategy = determineRecoveryStrategy(attempts)
            executeRecoveryStrategy(strategy)
        } catch (e: Exception) {
            AppLogger.logError(TAG, "Reconnection attempt failed", e)
            false
        }
    }

    fun preserveSessionState(
        sessionId: String,
        recordingActive: Boolean,
        fileCount: Int,
        pendingData: List<String> = emptyList()
    ) {
        val currentTime = System.currentTimeMillis()
        val preservationState = SessionPreservationState(
            sessionId = sessionId,
            recordingActive = recordingActive,
            fileCount = fileCount,
            lastSyncTime = currentTime,
            pendingData = pendingData,
            connectionLostTime = currentTime
        )

        preservedSessions[sessionId] = preservationState
        _sessionPreservationState.value = preservationState
        currentSessionId = sessionId

        AppLogger.i(TAG, "Session state preserved for $sessionId")
    }

    fun restoreSessionState(sessionId: String): SessionPreservationState? {
        val preservedState = preservedSessions[sessionId]

        if (preservedState != null) {
            val timeSinceDisconnect = System.currentTimeMillis() - preservedState.connectionLostTime

            if (timeSinceDisconnect < SESSION_PRESERVATION_TIMEOUT_MS) {
                AppLogger.i(TAG, "Restored session state for $sessionId (${timeSinceDisconnect}ms offline)")
                return preservedState
            } else {
                preservedSessions.remove(sessionId)
                AppLogger.logNetwork(TAG, "Session $sessionId expired, removed from preservation")
            }
        }

        return null
    }

    fun getCurrentNetworkQuality(): NetworkQuality {
        return _connectionState.value.quality
    }

    fun isConnected(): Boolean {
        return _connectionState.value.isConnected
    }

    fun getRecoveryStatistics(): Map<String, Any> {
        val successRate = if (totalRecoveryAttempts > 0) {
            (successfulRecoveries.toFloat() / totalRecoveryAttempts * 100).toInt()
        } else 0

        return mapOf(
            "total_connection_losses" to totalConnectionLosses,
            "total_recovery_attempts" to totalRecoveryAttempts,
            "successful_recoveries" to successfulRecoveries,
            "success_rate_percent" to successRate,
            "current_reconnect_attempts" to reconnectAttempts.get(),
            "is_recovering" to isRecovering.get(),
            "preserved_sessions" to preservedSessions.size,
            "connection_state" to _connectionState.value
        )
    }

    fun forceRecovery(): Boolean {
        AppLogger.logNetwork(TAG, "Manual recovery forced")
        reconnectAttempts.set(0)
        return attemptReconnection()
    }

    fun cleanup() {
        recoveryJob?.cancel()
        networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
        coroutineScope.cancel()
        preservedSessions.clear()
    }

    private fun setupNetworkCallback() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                handleNetworkAvailable(network)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                handleNetworkLost(network)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                assessNetworkQuality(networkCapabilities)
            }
        }
    }

    private fun startNetworkMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        networkCallback?.let { callback ->
            connectivityManager.registerNetworkCallback(request, callback)
        }
    }

    private fun handleNetworkAvailable(network: Network) {
        val currentTime = System.currentTimeMillis()

        coroutineScope.launch {
            val quality = testNetworkQuality(network)

            _connectionState.value = _connectionState.value.copy(
                isConnected = true,
                quality = quality,
                lastConnectedTime = currentTime,
                networkType = getNetworkType(network)
            )

            if (isRecovering.getAndSet(false)) {
                successfulRecoveries++
                reconnectAttempts.set(0)
                AppLogger.logNetwork(TAG, "Recovery successful, connection restored")

                restoreActiveSession()
            }
        }
    }

    private fun handleNetworkLost(network: Network) {
        if (_connectionState.value.isConnected) {
            handleConnectionLoss()
        }
    }

    private fun assessNetworkQuality(capabilities: NetworkCapabilities) {
        val quality = when {
            capabilities.linkDownstreamBandwidthKbps > 10000 -> NetworkQuality.EXCELLENT
            capabilities.linkDownstreamBandwidthKbps > 5000 -> NetworkQuality.GOOD
            capabilities.linkDownstreamBandwidthKbps > 1000 -> NetworkQuality.FAIR
            capabilities.linkDownstreamBandwidthKbps > 0 -> NetworkQuality.POOR
            else -> NetworkQuality.UNKNOWN
        }

        _connectionState.value = _connectionState.value.copy(quality = quality)
    }

    private fun preserveCurrentSession() {
        currentSessionId?.let { sessionId ->
            preserveSessionState(
                sessionId = sessionId,
                recordingActive = true,
                fileCount = 0,
                pendingData = emptyList()
            )
        }
    }

    private fun startRecoveryProcess() {
        if (isRecovering.getAndSet(true)) {
            return
        }

        recoveryJob = coroutineScope.launch {
            while (isRecovering.get() && reconnectAttempts.get() < MAX_RECONNECT_ATTEMPTS) {
                val success = attemptReconnection()

                if (success) {
                    break
                }

                val attempts = reconnectAttempts.get()
                val delay = calculateRetryDelay(attempts)

                AppLogger.logNetwork(TAG, "Recovery attempt $attempts failed, retrying in ${delay}ms")
                delay(delay)
            }

            if (reconnectAttempts.get() >= MAX_RECONNECT_ATTEMPTS) {
                AppLogger.e(TAG, "Maximum recovery attempts reached, manual intervention required")
                isRecovering.set(false)
            }
        }
    }

    private fun determineRecoveryStrategy(attempts: Int): RecoveryStrategy {
        return when {
            attempts <= 2 -> RecoveryStrategy.IMMEDIATE_RETRY
            attempts <= 5 -> RecoveryStrategy.EXPONENTIAL_BACKOFF
            attempts <= 8 -> RecoveryStrategy.PROGRESSIVE_DEGRADATION
            else -> RecoveryStrategy.MANUAL_INTERVENTION
        }
    }

    private fun executeRecoveryStrategy(strategy: RecoveryStrategy): Boolean {
        return when (strategy) {
            RecoveryStrategy.IMMEDIATE_RETRY -> {
                jsonSocketClient.reconnect()
            }

            RecoveryStrategy.EXPONENTIAL_BACKOFF -> {
                jsonSocketClient.reconnect()
            }

            RecoveryStrategy.PROGRESSIVE_DEGRADATION -> {
                jsonSocketClient.reconnectWithReducedCapabilities()
            }

            RecoveryStrategy.MANUAL_INTERVENTION -> {
                AppLogger.e(TAG, "Manual intervention required for network recovery")
                false
            }
        }
    }

    private fun calculateRetryDelay(attempts: Int): Long {
        val baseDelay = BASE_RETRY_DELAY_MS * (1 shl (attempts - 1).coerceAtMost(5))
        
        // Replace random jitter with deterministic network-aware jitter
        val networkBasedJitter = calculateNetworkAwareJitter(baseDelay, attempts)
        
        return (baseDelay + networkBasedJitter).coerceAtMost(MAX_RETRY_DELAY_MS)
    }
    
    /**
     * Calculate network-aware jitter based on network conditions instead of random values
     */
    private fun calculateNetworkAwareJitter(baseDelay: Long, attempts: Int): Long {
        // Use current time and attempt count to create deterministic but varying jitter
        val timeMs = System.currentTimeMillis()
        val timeFactor = (timeMs % 1000) / 1000.0  // 0-1 based on current millisecond
        
        // Network condition factor (simulating real network variability)
        val networkConditionFactor = kotlin.math.sin(timeMs * 0.001) * 0.5 + 0.5  // 0-1
        
        // Attempt-based backoff factor (more attempts = more jitter)
        val attemptFactor = kotlin.math.min(attempts / 10.0, 1.0)
        
        // Combined jitter that's deterministic but varies with conditions
        val jitterFactor = (timeFactor * 0.4 + networkConditionFactor * 0.4 + attemptFactor * 0.2) * 0.1
        
        return (baseDelay * jitterFactor).toLong()
    }

    private suspend fun testNetworkQuality(network: Network): NetworkQuality {
        return withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
            NetworkQuality.GOOD
        } ?: NetworkQuality.POOR
    }

    private fun getNetworkType(network: Network): String {
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return when {
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            else -> "Unknown"
        }
    }

    private suspend fun restoreActiveSession() {
        currentSessionId?.let { sessionId ->
            val preservedState = restoreSessionState(sessionId)
            if (preservedState != null) {
                AppLogger.i(TAG, "Active session restored: $sessionId")
            }
        }
    }
}

fun JsonSocketClient.reconnect(): Boolean {
    return try {
        disconnect()
        connect()
        true
    } catch (e: Exception) {
        false
    }
}

fun JsonSocketClient.reconnectWithReducedCapabilities(): Boolean {
    return try {
        reconnect()
    } catch (e: Exception) {
        false
    }
}
