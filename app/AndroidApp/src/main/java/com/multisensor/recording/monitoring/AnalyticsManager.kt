package com.multisensor.recording.monitoring

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) : LifecycleObserver {

    companion object {
        private const val TAG = "AnalyticsManager"
        private const val ANALYTICS_FILE = "analytics_data.json"
        private const val MAX_EVENTS_PER_SESSION = 1000
        private const val FLUSH_INTERVAL_MS = 30_000L
        private const val MAX_FILE_SIZE_MB = 5

        private const val APP_VERSION = "1.0.0"
        private const val PROTOCOL_VERSION = 1
        private const val HEARTBEAT_INTERVAL = 5
        private const val BUFFER_SIZE = 8192
    }

    private val analyticsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val eventQueue = ConcurrentHashMap<String, MutableList<AnalyticsEvent>>()

    private val _sessionMetrics = MutableStateFlow(SessionMetrics())
    val sessionMetrics: StateFlow<SessionMetrics> = _sessionMetrics.asStateFlow()

    private val _systemHealth = MutableStateFlow(SystemHealth())
    val systemHealth: StateFlow<SystemHealth> = _systemHealth.asStateFlow()

    private var currentSessionId: String? = null
    private var sessionStartTime: Long = 0

    private val analyticsFile: File by lazy {
        File(context.filesDir, ANALYTICS_FILE)
    }

    init {
        startPeriodicFlush()
        startHealthMonitoring()
    }

    fun startSession(sessionId: String) {
        currentSessionId = sessionId
        sessionStartTime = System.currentTimeMillis()

        trackEvent(
            SessionEvent.SESSION_STARTED, mapOf(
                "session_id" to sessionId,
                "app_version" to APP_VERSION,
                "protocol_version" to PROTOCOL_VERSION
            )
        )

        Log.i(TAG, "Analytics session started: $sessionId")
    }

    fun endSession() {
        currentSessionId?.let { sessionId ->
            val duration = System.currentTimeMillis() - sessionStartTime

            trackEvent(
                SessionEvent.SESSION_ENDED, mapOf(
                    "session_id" to sessionId,
                    "duration_ms" to duration
                )
            )

            updateSessionMetrics { metrics ->
                metrics.copy(
                    totalSessions = metrics.totalSessions + 1,
                    totalSessionDuration = metrics.totalSessionDuration + duration,
                    averageSessionDuration = (metrics.totalSessionDuration + duration) / (metrics.totalSessions + 1)
                )
            }
        }

        currentSessionId = null
        sessionStartTime = 0

        Log.i(TAG, "Analytics session ended")
    }

    fun trackSessionMetrics(metrics: SessionMetrics) {
        _sessionMetrics.value = metrics

        trackEvent(
            MetricEvent.SESSION_METRICS, mapOf(
                "recording_count" to metrics.recordingCount,
                "total_data_size_mb" to metrics.totalDataSizeMB,
                "average_frame_rate" to metrics.averageFrameRate,
                "error_count" to metrics.errorCount
            )
        )
    }

    fun reportErrorEvent(error: ErrorEvent) {
        trackEvent(
            ErrorEventType.ERROR_OCCURRED, mapOf(
                "error_type" to error.type,
                "error_message" to error.message,
                "error_code" to error.code,
                "stack_trace" to error.stackTrace,
                "session_id" to (currentSessionId ?: "unknown"),
                "timestamp" to System.currentTimeMillis()
            )
        )

        updateSessionMetrics { metrics ->
            metrics.copy(errorCount = metrics.errorCount + 1)
        }

        Log.e(TAG, "Error reported: ${error.type} - ${error.message}")
    }

    fun monitorPerformanceMetrics() {
        analyticsScope.launch {
            while (true) {
                try {
                    val performance = collectPerformanceMetrics()

                    trackEvent(
                        MetricEvent.PERFORMANCE_METRICS, mapOf(
                            "memory_usage_mb" to performance.memoryUsageMB,
                            "cpu_usage_percent" to performance.cpuUsagePercent,
                            "battery_level" to performance.batteryLevel,
                            "storage_available_mb" to performance.storageAvailableMB,
                            "network_speed_mbps" to performance.networkSpeedMbps
                        )
                    )

                    updateSystemHealth(performance)

                } catch (e: Exception) {
                    Log.e(TAG, "Error collecting performance metrics", e)
                }

                delay(HEARTBEAT_INTERVAL * 1000L)
            }
        }
    }

    fun trackUserInteraction(interaction: UserInteraction) {
        trackEvent(
            UserEvent.USER_INTERACTION, mapOf(
                "action" to interaction.action,
                "screen" to interaction.screen,
                "element" to interaction.element,
                "duration_ms" to interaction.durationMs
            )
        )
    }

    fun trackNetworkEvent(event: NetworkEvent) {
        trackEvent(
            NetworkEventType.NETWORK_EVENT, mapOf(
                "event_type" to event.type,
                "connection_type" to event.connectionType,
                "bandwidth_mbps" to event.bandwidthMbps,
                "latency_ms" to event.latencyMs,
                "success" to event.success
            )
        )
    }

    private fun trackEvent(eventType: AnalyticsEventType, parameters: Map<String, Any>) {
        val event = AnalyticsEvent(
            type = eventType.eventName,
            parameters = parameters,
            timestamp = System.currentTimeMillis(),
            sessionId = currentSessionId
        )

        val sessionEvents = eventQueue.getOrPut(currentSessionId ?: "global") { mutableListOf() }

        synchronized(sessionEvents) {
            if (sessionEvents.size < MAX_EVENTS_PER_SESSION) {
                sessionEvents.add(event)
            } else {
                sessionEvents.removeAt(0)
                sessionEvents.add(event)
            }
        }
    }

    private fun startPeriodicFlush() {
        analyticsScope.launch {
            while (true) {
                delay(FLUSH_INTERVAL_MS)
                flushToStorage()
            }
        }
    }

    private fun startHealthMonitoring() {
        analyticsScope.launch {
            monitorPerformanceMetrics()
        }
    }

    private suspend fun flushToStorage() {
        withContext(Dispatchers.IO) {
            try {
                val allEvents = mutableListOf<AnalyticsEvent>()

                eventQueue.forEach { (_, events) ->
                    synchronized(events) {
                        allEvents.addAll(events)
                        events.clear()
                    }
                }

                if (allEvents.isNotEmpty()) {
                    appendEventsToFile(allEvents)
                }

                rotateFileIfNeeded()

            } catch (e: Exception) {
                Log.e(TAG, "Error flushing analytics data", e)
            }
        }
    }

    private fun appendEventsToFile(events: List<AnalyticsEvent>) {
        try {
            val jsonArray = JSONObject().apply {
                put("events", events.map { it.toJson() })
                put("flush_timestamp", System.currentTimeMillis())
                put("app_version", APP_VERSION)
            }

            analyticsFile.appendText(jsonArray.toString() + "\n")

        } catch (e: Exception) {
            Log.e(TAG, "Error writing analytics events to file", e)
        }
    }

    private fun rotateFileIfNeeded() {
        if (analyticsFile.exists() && analyticsFile.length() > MAX_FILE_SIZE_MB * 1024 * 1024) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(context.filesDir, "analytics_data_$timestamp.json")

            analyticsFile.renameTo(backupFile)
            Log.i(TAG, "Analytics file rotated to: ${backupFile.name}")
        }
    }

    private fun collectPerformanceMetrics(): PerformanceMetrics {
        val runtime = Runtime.getRuntime()
        val memoryUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

        return PerformanceMetrics(
            memoryUsageMB = memoryUsed.toDouble(),
            cpuUsagePercent = getCpuUsage(),
            batteryLevel = getBatteryLevel(),
            storageAvailableMB = getAvailableStorage(),
            networkSpeedMbps = getNetworkSpeed()
        )
    }

    private fun getCpuUsage(): Double {
        return Runtime.getRuntime().availableProcessors().toDouble() * 10.0
    }

    private fun getBatteryLevel(): Int {
        return 85
    }

    private fun getAvailableStorage(): Double {
        return context.filesDir.freeSpace / (1024.0 * 1024.0)
    }

    private fun getNetworkSpeed(): Double {
        return 50.0
    }

    private fun updateSessionMetrics(update: (SessionMetrics) -> SessionMetrics) {
        _sessionMetrics.value = update(_sessionMetrics.value)
    }

    private fun updateSystemHealth(performance: PerformanceMetrics) {
        val health = SystemHealth(
            isHealthy = performance.memoryUsageMB < BUFFER_SIZE &&
                    performance.batteryLevel > 20,
            memoryPressure = performance.memoryUsageMB > 1000,
            lowBattery = performance.batteryLevel < 30,
            storageWarning = performance.storageAvailableMB < 100,
            lastUpdateTime = System.currentTimeMillis()
        )

        _systemHealth.value = health
    }

    fun getAnalyticsSummary(): String {
        val summary = JSONObject().apply {
            put("session_metrics", _sessionMetrics.value.toJson())
            put("system_health", _systemHealth.value.toJson())
            put("current_session", currentSessionId)
            put("export_timestamp", System.currentTimeMillis())
        }

        return summary.toString(2)
    }

    fun clearAnalyticsData() {
        eventQueue.clear()
        if (analyticsFile.exists()) {
            analyticsFile.delete()
        }

        _sessionMetrics.value = SessionMetrics()
        _systemHealth.value = SystemHealth()

        Log.i(TAG, "Analytics data cleared")
    }
}

data class SessionMetrics(
    val recordingCount: Int = 0,
    val totalDataSizeMB: Double = 0.0,
    val averageFrameRate: Double = 0.0,
    val errorCount: Int = 0,
    val totalSessions: Int = 0,
    val totalSessionDuration: Long = 0,
    val averageSessionDuration: Long = 0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("recording_count", recordingCount)
        put("total_data_size_mb", totalDataSizeMB)
        put("average_frame_rate", averageFrameRate)
        put("error_count", errorCount)
        put("total_sessions", totalSessions)
        put("total_session_duration", totalSessionDuration)
        put("average_session_duration", averageSessionDuration)
    }
}

data class SystemHealth(
    val isHealthy: Boolean = true,
    val memoryPressure: Boolean = false,
    val lowBattery: Boolean = false,
    val storageWarning: Boolean = false,
    val lastUpdateTime: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("is_healthy", isHealthy)
        put("memory_pressure", memoryPressure)
        put("low_battery", lowBattery)
        put("storage_warning", storageWarning)
        put("last_update_time", lastUpdateTime)
    }
}

data class ErrorEvent(
    val type: String,
    val message: String,
    val code: Int = 0,
    val stackTrace: String = ""
)

data class PerformanceMetrics(
    val memoryUsageMB: Double,
    val cpuUsagePercent: Double,
    val batteryLevel: Int,
    val storageAvailableMB: Double,
    val networkSpeedMbps: Double
)

data class UserInteraction(
    val action: String,
    val screen: String,
    val element: String,
    val durationMs: Long
)

data class NetworkEvent(
    val type: String,
    val connectionType: String,
    val bandwidthMbps: Double,
    val latencyMs: Long,
    val success: Boolean
)

data class AnalyticsEvent(
    val type: String,
    val parameters: Map<String, Any>,
    val timestamp: Long,
    val sessionId: String?
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type)
        put("parameters", JSONObject(parameters))
        put("timestamp", timestamp)
        put("session_id", sessionId)
    }
}

interface AnalyticsEventType {
    val eventName: String
}

enum class SessionEvent(override val eventName: String) : AnalyticsEventType {
    SESSION_STARTED("session_started"),
    SESSION_ENDED("session_ended"),
    RECORDING_STARTED("recording_started"),
    RECORDING_STOPPED("recording_stopped")
}

enum class MetricEvent(override val eventName: String) : AnalyticsEventType {
    SESSION_METRICS("session_metrics"),
    PERFORMANCE_METRICS("performance_metrics")
}

enum class ErrorEventType(override val eventName: String) : AnalyticsEventType {
    ERROR_OCCURRED("error_occurred"),
    CRASH_DETECTED("crash_detected")
}

enum class UserEvent(override val eventName: String) : AnalyticsEventType {
    USER_INTERACTION("user_interaction"),
    SCREEN_VIEW("screen_view")
}

enum class NetworkEventType(override val eventName: String) : AnalyticsEventType {
    NETWORK_EVENT("network_event"),
    CONNECTION_CHANGE("connection_change")
}
