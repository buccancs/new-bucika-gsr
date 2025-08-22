package com.multisensor.recording.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Process
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object AppLogger {

    private const val TAG_PREFIX = "MSR"

    private var isDebugEnabled = true
    private var isVerboseEnabled = false

    private val startTimes = ConcurrentHashMap<String, Long>()
    private val performanceStats = ConcurrentHashMap<String, PerformanceStats>()

    private var memoryMonitoringEnabled = true
    private var lastMemoryCheck = System.currentTimeMillis()
    private val memorySnapshots = mutableListOf<MemorySnapshot>()

    private var crashHandler: Thread.UncaughtExceptionHandler? = null
    private var originalHandler: Thread.UncaughtExceptionHandler? = null

    private var appContext: Context? = null
    private val logCounter = AtomicLong(0)

    private val dateFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val fullDateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    data class PerformanceStats(
        val operationName: String,
        val totalCalls: Long,
        val totalTimeMs: Long,
        val minTimeMs: Long,
        val maxTimeMs: Long,
        val avgTimeMs: Long = if (totalCalls > 0) totalTimeMs / totalCalls else 0L
    )

    data class MemorySnapshot(
        val timestamp: Long,
        val context: String,
        val usedMemoryMB: Long,
        val freeMemoryMB: Long,
        val maxMemoryMB: Long,
        val nativeHeapSizeMB: Long,
        val threadCount: Int
    )

    fun initialize(context: Context) {
        appContext = context
        setupCrashReporting()
        logSystemInfo()
        i("AppLogger", "Enhanced logging system initialized")
    }

    private fun setupCrashReporting() {
        originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        crashHandler = Thread.UncaughtExceptionHandler { thread, throwable ->
            logCrash(thread, throwable)
            originalHandler?.uncaughtException(thread, throwable)
        }
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)
    }

    private fun logSystemInfo() {
        i("SystemInfo", "=== SYSTEM INFORMATION ===")
        i("SystemInfo", "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        i("SystemInfo", "Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        i("SystemInfo", "App Process ID: ${Process.myPid()}")
        i("SystemInfo", "Available Processors: ${Runtime.getRuntime().availableProcessors()}")

        appContext?.let { context ->
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            i("SystemInfo", "Total RAM: ${memoryInfo.totalMem / (1024 * 1024)} MB")
            i("SystemInfo", "Available RAM: ${memoryInfo.availMem / (1024 * 1024)} MB")
            i("SystemInfo", "Low Memory Threshold: ${memoryInfo.threshold / (1024 * 1024)} MB")
            i("SystemInfo", "Low Memory: ${memoryInfo.lowMemory}")
        }
        i("SystemInfo", "=== END SYSTEM INFORMATION ===")
    }

    fun setDebugEnabled(enabled: Boolean) {
        isDebugEnabled = enabled
        i("AppLogger", "Debug logging ${if (enabled) "enabled" else "disabled"}")
    }

    fun setVerboseEnabled(enabled: Boolean) {
        isVerboseEnabled = enabled
        i("AppLogger", "Verbose logging ${if (enabled) "enabled" else "disabled"}")
    }

    fun setMemoryMonitoringEnabled(enabled: Boolean) {
        memoryMonitoringEnabled = enabled
        i("AppLogger", "Memory monitoring ${if (enabled) "enabled" else "disabled"}")
    }

    fun getLoggingStats(): String {
        val totalLogs = logCounter.get()
        val performanceOps = performanceStats.size
        val memorySnapshots = memorySnapshots.size

        return "Logs: $totalLogs, Performance Ops: $performanceOps, Memory Snapshots: $memorySnapshots"
    }

    fun v(tag: String, message: String, throwable: Throwable? = null, context: Map<String, Any>? = null) {
        if (isVerboseEnabled) {
            val structuredMessage = formatMessage(message, context)
            logToAndroid(Log.VERBOSE, tag, structuredMessage, throwable)
        }
    }

    fun d(tag: String, message: String, throwable: Throwable? = null, context: Map<String, Any>? = null) {
        if (isDebugEnabled) {
            val structuredMessage = formatMessage(message, context)
            logToAndroid(Log.DEBUG, tag, structuredMessage, throwable)
        }
    }

    fun i(tag: String, message: String, throwable: Throwable? = null, context: Map<String, Any>? = null) {
        val structuredMessage = formatMessage(message, context)
        logToAndroid(Log.INFO, tag, structuredMessage, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null, context: Map<String, Any>? = null) {
        val structuredMessage = formatMessage(message, context)
        logToAndroid(Log.WARN, tag, structuredMessage, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null, context: Map<String, Any>? = null) {
        val structuredMessage = formatMessage(message, context)
        logToAndroid(Log.ERROR, tag, structuredMessage, throwable)
    }

    private fun logToAndroid(priority: Int, tag: String, message: String, throwable: Throwable?) {
        val fullTag = "${TAG_PREFIX}_$tag"
        val timestamp = dateFormatter.format(Date())
        val threadName = Thread.currentThread().name
        val logId = logCounter.incrementAndGet()

        val fullMessage = "[$timestamp][#$logId][$threadName] $message"

        if (memoryMonitoringEnabled && priority >= Log.WARN) {
            checkMemoryUsage("Log Entry")
        }

        if (throwable != null) {
            Log.println(priority, fullTag, "$fullMessage\n${getStackTraceString(throwable)}")
        } else {
            Log.println(priority, fullTag, fullMessage)
        }
    }

    private fun formatMessage(message: String, context: Map<String, Any>?): String {
        return if (context.isNullOrEmpty()) {
            message
        } else {
            val contextStr = context.entries.joinToString(", ") { "${it.key}=${it.value}" }
            "$message [$contextStr]"
        }
    }

    private fun getStackTraceString(throwable: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        return stringWriter.toString()
    }

    fun logMethodEntry(tag: String, methodName: String, vararg params: Any?) {
        if (isDebugEnabled) {
            val paramString = if (params.isNotEmpty()) {
                " with params: ${params.joinToString(", ") { it?.toString() ?: "null" }}"
            } else {
                ""
            }
            d(tag, "→ Entering $methodName$paramString")
        }
    }

    fun logMethodExit(tag: String, methodName: String, returnValue: Any? = null) {
        if (isDebugEnabled) {
            val returnString = returnValue?.let { " returning: $it" } ?: ""
            d(tag, "← Exiting $methodName$returnString")
        }
    }

    fun logLifecycle(tag: String, lifecycleEvent: String, componentName: String? = null) {
        val component = componentName ?: tag
        i(tag, "🔄 Lifecycle: $component.$lifecycleEvent")
    }

    fun logNetwork(tag: String, operation: String, endpoint: String? = null, status: String? = null) {
        val endpointInfo = endpoint?.let { " to $it" } ?: ""
        val statusInfo = status?.let { " - $it" } ?: ""
        i(tag, "🌐 Network: $operation$endpointInfo$statusInfo")
    }

    fun logRecording(tag: String, operation: String, deviceInfo: String? = null) {
        val deviceString = deviceInfo?.let { " ($it)" } ?: ""
        i(tag, "📹 Recording: $operation$deviceString")
    }

    fun logSensor(tag: String, operation: String, sensorType: String? = null, value: String? = null) {
        val sensorInfo = sensorType?.let { " $it" } ?: ""
        val valueInfo = value?.let { " = $it" } ?: ""
        i(tag, "📊 Sensor$sensorInfo: $operation$valueInfo")
    }

    fun logFile(tag: String, operation: String, fileName: String? = null, size: Long? = null) {
        val fileInfo = fileName?.let { " $it" } ?: ""
        val sizeInfo = size?.let { " (${formatFileSize(it)})" } ?: ""
        i(tag, "📁 File: $operation$fileInfo$sizeInfo")
    }

    fun startTiming(tag: String, operationName: String, context: String? = null) {
        val key = "${tag}_$operationName"
        val fullKey = context?.let { "${key}_$it" } ?: key
        startTimes[fullKey] = System.nanoTime()
        d(tag, "⏱️ Started timing: $operationName${context?.let { " ($it)" } ?: ""}")
    }

    fun endTiming(tag: String, operationName: String, context: String? = null): Long {
        val key = "${tag}_$operationName"
        val fullKey = context?.let { "${key}_$it" } ?: key
        val startTime = startTimes.remove(fullKey)

        return if (startTime != null) {
            val durationNanos = System.nanoTime() - startTime
            val durationMs = durationNanos / 1_000_000

            updatePerformanceStats(operationName, durationMs)

            val contextStr = context?.let { " ($it)" } ?: ""
            i(
                tag, "⏱️ Completed $operationName$contextStr in ${durationMs}ms",
                context = mapOf("duration_ms" to durationMs, "operation" to operationName)
            )

            durationMs
        } else {
            w(tag, "⏱️ No start time found for operation: $operationName${context?.let { " ($it)" } ?: ""}")
            -1L
        }
    }

    inline fun <T> measureTime(tag: String, operationName: String, block: () -> T): T {
        val startTime = System.nanoTime()
        return try {
            val result = block()
            val durationMs = (System.nanoTime() - startTime) / 1_000_000
            updatePerformanceStats(operationName, durationMs)

            i(
                tag, "⏱️ Measured $operationName in ${durationMs}ms",
                context = mapOf("duration_ms" to durationMs, "operation" to operationName)
            )
            result
        } catch (e: Exception) {
            val durationMs = (System.nanoTime() - startTime) / 1_000_000
            e(
                tag, "⏱️ Failed $operationName after ${durationMs}ms", e,
                context = mapOf("duration_ms" to durationMs, "operation" to operationName)
            )
            throw e
        }
    }

    fun updatePerformanceStats(operationName: String, durationMs: Long) {
        performanceStats.compute(operationName) { _, existing ->
            if (existing == null) {
                PerformanceStats(
                    operationName = operationName,
                    totalCalls = 1,
                    totalTimeMs = durationMs,
                    minTimeMs = durationMs,
                    maxTimeMs = durationMs
                )
            } else {
                existing.copy(
                    totalCalls = existing.totalCalls + 1,
                    totalTimeMs = existing.totalTimeMs + durationMs,
                    minTimeMs = minOf(existing.minTimeMs, durationMs),
                    maxTimeMs = maxOf(existing.maxTimeMs, durationMs)
                )
            }
        }
    }

    fun getPerformanceStats(): Map<String, PerformanceStats> = performanceStats.toMap()

    fun logPerformanceStats(tag: String) {
        if (performanceStats.isEmpty()) {
            i(tag, "📊 No performance statistics available")
            return
        }

        i(tag, "📊 Performance Statistics:")
        performanceStats.forEach { (operation, stats) ->
            i(
                tag, "  $operation: ${stats.totalCalls} calls, avg=${stats.avgTimeMs}ms, " +
                        "min=${stats.minTimeMs}ms, max=${stats.maxTimeMs}ms, total=${stats.totalTimeMs}ms"
            )
        }
    }

    fun clearPerformanceStats() {
        performanceStats.clear()
        i("AppLogger", "📊 Performance statistics cleared")
    }

    fun logMemoryUsage(tag: String, context: String = "Memory Check") {
        if (!memoryMonitoringEnabled) return

        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory

        val nativeHeapSize = Debug.getNativeHeapSize()
        val nativeHeapFree = Debug.getNativeHeapFreeSize()
        val nativeHeapUsed = nativeHeapSize - nativeHeapFree

        val threadCount = Thread.activeCount()

        val snapshot = MemorySnapshot(
            timestamp = System.currentTimeMillis(),
            context = context,
            usedMemoryMB = usedMemory / (1024 * 1024),
            freeMemoryMB = freeMemory / (1024 * 1024),
            maxMemoryMB = maxMemory / (1024 * 1024),
            nativeHeapSizeMB = nativeHeapSize / (1024 * 1024),
            threadCount = threadCount
        )

        synchronized(memorySnapshots) {
            memorySnapshots.add(snapshot)
            if (memorySnapshots.size > 50) {
                memorySnapshots.removeAt(0)
            }
        }

        val memoryInfo = mapOf(
            "used_mb" to snapshot.usedMemoryMB,
            "free_mb" to snapshot.freeMemoryMB,
            "max_mb" to snapshot.maxMemoryMB,
            "native_heap_mb" to snapshot.nativeHeapSizeMB,
            "thread_count" to threadCount,
            "usage_percent" to ((usedMemory * 100) / maxMemory)
        )

        val level = when {
            (usedMemory * 100 / maxMemory) > 85 -> Log.WARN
            (usedMemory * 100 / maxMemory) > 70 -> Log.INFO
            else -> Log.DEBUG
        }

        val priority = if (level == Log.WARN) "WARNING" else "INFO"
        val logMessage = "💾 $context - Used: ${formatFileSize(usedMemory)}, " +
                "Free: ${formatFileSize(freeMemory)}, " +
                "Max: ${formatFileSize(maxMemory)}, " +
                "Native: ${formatFileSize(nativeHeapUsed)}, " +
                "Threads: $threadCount"

        when (level) {
            Log.WARN -> w(tag, logMessage, context = memoryInfo)
            Log.INFO -> i(tag, logMessage, context = memoryInfo)
            else -> d(tag, logMessage, context = memoryInfo)
        }

        lastMemoryCheck = System.currentTimeMillis()
    }

    private fun checkMemoryUsage(context: String) {
        val now = System.currentTimeMillis()
        if (now - lastMemoryCheck > 30000) {
            logMemoryUsage("MemoryMonitor", context)
        }
    }

    fun getMemorySnapshots(): List<MemorySnapshot> = synchronized(memorySnapshots) {
        memorySnapshots.toList()
    }

    fun forceGarbageCollection(tag: String, context: String = "Manual GC") {
        val beforeMemory = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }

        measureTime(tag, "garbage_collection") {
            System.gc()
            System.runFinalization()
            System.gc()
        }

        val afterMemory = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }
        val freed = beforeMemory - afterMemory

        i(
            tag,
            "🗑️ $context - Freed ${formatFileSize(freed)} (${formatFileSize(beforeMemory)} → ${
                formatFileSize(afterMemory)
            })",
            context = mapOf(
                "freed_bytes" to freed,
                "before_bytes" to beforeMemory,
                "after_bytes" to afterMemory
            )
        )
    }

    fun logThreadInfo(tag: String, context: String = "Thread Info") {
        val thread = Thread.currentThread()
        d(
            tag, "🧵 $context - Thread: ${thread.name}, ID: ${thread.id}, " +
                    "State: ${thread.state}"
        )
    }

    fun logError(tag: String, operation: String, error: Throwable) {
        e(tag, "❌ Failed $operation: ${error.message}", error)
    }

    fun logStateChange(tag: String, component: String, fromState: String, toState: String) {
        i(tag, "🔄 State Change: $component from '$fromState' to '$toState'")
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    private fun logCrash(thread: Thread, throwable: Throwable) {
        try {
            e("CrashReport", "=== UNCAUGHT EXCEPTION ===")
            e("CrashReport", "Thread: ${thread.name} (ID: ${thread.id})")
            e("CrashReport", "Exception: ${throwable::class.java.simpleName}")
            e("CrashReport", "Message: ${throwable.message}")
            e("CrashReport", "Stack trace:", throwable)

            logSystemStateAtCrash()

            e("CrashReport", "=== END CRASH REPORT ===")
        } catch (e: Exception) {
            Log.e("${TAG_PREFIX}_CrashReport", "Failed to log crash details", e)
        }
    }

    private fun logSystemStateAtCrash() {
        try {
            val runtime = Runtime.getRuntime()
            e(
                "CrashReport", "Memory - Used: ${formatFileSize(runtime.totalMemory() - runtime.freeMemory())}, " +
                        "Max: ${formatFileSize(runtime.maxMemory())}"
            )

            e("CrashReport", "Active Threads: ${Thread.activeCount()}")

            if (startTimes.isNotEmpty()) {
                e("CrashReport", "Active Performance Timers: ${startTimes.keys.joinToString(", ")}")
            }

            synchronized(memorySnapshots) {
                if (memorySnapshots.isNotEmpty()) {
                    val recent = memorySnapshots.takeLast(3)
                    e("CrashReport", "Recent Memory Snapshots:")
                    recent.forEach { snapshot ->
                        e(
                            "CrashReport",
                            "  ${snapshot.context}: ${snapshot.usedMemoryMB}MB used, ${snapshot.threadCount} threads"
                        )
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("${TAG_PREFIX}_CrashReport", "Failed to log system state", e)
        }
    }

    fun logError(tag: String, operation: String, error: Throwable, context: Map<String, Any>? = null) {
        val errorContext = mutableMapOf<String, Any>(
            "operation" to operation,
            "exception_type" to error::class.java.simpleName,
            "exception_message" to (error.message ?: "No message")
        )
        context?.let { errorContext.putAll(it) }

        e(tag, "❌ Failed $operation: ${error.message}", error, errorContext)

        if (memoryMonitoringEnabled) {
            logMemoryUsage(tag, "Error Context: $operation")
        }
    }

    fun logStateChange(
        tag: String,
        component: String,
        fromState: String,
        toState: String,
        context: Map<String, Any>? = null
    ) {
        val stateContext = mutableMapOf<String, Any>(
            "component" to component,
            "from_state" to fromState,
            "to_state" to toState,
            "transition_time" to System.currentTimeMillis()
        )
        context?.let { stateContext.putAll(it) }

        i(tag, "🔄 State Change: $component from '$fromState' to '$toState'", context = stateContext)
    }

    fun logLifecycle(
        tag: String,
        lifecycleEvent: String,
        componentName: String? = null,
        context: Map<String, Any>? = null
    ) {
        val component = componentName ?: tag
        val lifecycleContext = mutableMapOf<String, Any>(
            "component" to component,
            "lifecycle_event" to lifecycleEvent,
            "timestamp" to System.currentTimeMillis()
        )
        context?.let { lifecycleContext.putAll(it) }

        i(tag, "🔄 Lifecycle: $component.$lifecycleEvent", context = lifecycleContext)
    }

    fun logNetwork(
        tag: String, operation: String, endpoint: String? = null, status: String? = null,
        responseTime: Long? = null, context: Map<String, Any>? = null
    ) {
        val endpointInfo = endpoint?.let { " to $it" } ?: ""
        val statusInfo = status?.let { " - $it" } ?: ""
        val timeInfo = responseTime?.let { " (${it}ms)" } ?: ""

        val networkContext = mutableMapOf<String, Any>(
            "operation" to operation
        )
        endpoint?.let { networkContext["endpoint"] = it }
        status?.let { networkContext["status"] = it }
        responseTime?.let { networkContext["response_time_ms"] = it }
        context?.let { networkContext.putAll(it) }

        i(tag, "🌐 Network: $operation$endpointInfo$statusInfo$timeInfo", context = networkContext)
    }

    fun logRecording(
        tag: String, operation: String, deviceInfo: String? = null, duration: Long? = null,
        fileSize: Long? = null, context: Map<String, Any>? = null
    ) {
        val deviceString = deviceInfo?.let { " ($it)" } ?: ""
        val durationString = duration?.let { " ${it}ms" } ?: ""
        val sizeString = fileSize?.let { " ${formatFileSize(it)}" } ?: ""

        val recordingContext = mutableMapOf<String, Any>(
            "operation" to operation
        )
        deviceInfo?.let { recordingContext["device"] = it }
        duration?.let { recordingContext["duration_ms"] = it }
        fileSize?.let { recordingContext["file_size_bytes"] = it }
        context?.let { recordingContext.putAll(it) }

        i(tag, "📹 Recording: $operation$deviceString$durationString$sizeString", context = recordingContext)
    }

    fun logSensor(
        tag: String, operation: String, sensorType: String? = null, value: String? = null,
        accuracy: Int? = null, timestamp: Long? = null, context: Map<String, Any>? = null
    ) {
        val sensorInfo = sensorType?.let { " $it" } ?: ""
        val valueInfo = value?.let { " = $it" } ?: ""
        val accuracyInfo = accuracy?.let { " (accuracy: $it)" } ?: ""

        val sensorContext = mutableMapOf<String, Any>(
            "operation" to operation
        )
        sensorType?.let { sensorContext["sensor_type"] = it }
        value?.let { sensorContext["value"] = it }
        accuracy?.let { sensorContext["accuracy"] = it }
        timestamp?.let { sensorContext["sensor_timestamp"] = it }
        context?.let { sensorContext.putAll(it) }

        i(tag, "📊 Sensor$sensorInfo: $operation$valueInfo$accuracyInfo", context = sensorContext)
    }

    fun logFile(
        tag: String, operation: String, fileName: String? = null, size: Long? = null,
        duration: Long? = null, success: Boolean = true, context: Map<String, Any>? = null
    ) {
        val fileInfo = fileName?.let { " $it" } ?: ""
        val sizeInfo = size?.let { " (${formatFileSize(it)})" } ?: ""
        val durationInfo = duration?.let { " in ${it}ms" } ?: ""
        val statusIcon = if (success) "📁" else "❌"

        val fileContext = mutableMapOf<String, Any>(
            "operation" to operation,
            "success" to success
        )
        fileName?.let { fileContext["file_name"] = it }
        size?.let { fileContext["file_size_bytes"] = it }
        duration?.let { fileContext["duration_ms"] = it }
        context?.let { fileContext.putAll(it) }

        val logLevel = if (success) Log.INFO else Log.WARN
        val message = "$statusIcon File: $operation$fileInfo$sizeInfo$durationInfo"

        when (logLevel) {
            Log.WARN -> w(tag, message, context = fileContext)
            else -> i(tag, message, context = fileContext)
        }
    }

}

fun Any.getLogTag(): String = this::class.java.simpleName

fun Any.logV(message: String, throwable: Throwable? = null, context: Map<String, Any>? = null) =
    AppLogger.v(getLogTag(), message, throwable, context)

fun Any.logD(message: String, throwable: Throwable? = null, context: Map<String, Any>? = null) =
    AppLogger.d(getLogTag(), message, throwable, context)

fun Any.logI(message: String, throwable: Throwable? = null, context: Map<String, Any>? = null) =
    AppLogger.i(getLogTag(), message, throwable, context)

fun Any.logW(message: String, throwable: Throwable? = null, context: Map<String, Any>? = null) =
    AppLogger.w(getLogTag(), message, throwable, context)

fun Any.logE(message: String, throwable: Throwable? = null, context: Map<String, Any>? = null) =
    AppLogger.e(getLogTag(), message, throwable, context)

fun Any.startTiming(operationName: String, context: String? = null) =
    AppLogger.startTiming(getLogTag(), operationName, context)

fun Any.endTiming(operationName: String, context: String? = null) =
    AppLogger.endTiming(getLogTag(), operationName, context)

inline fun <T> Any.measureTime(operationName: String, block: () -> T): T =
    AppLogger.measureTime(getLogTag(), operationName, block)

fun Any.logMemory(context: String = "Memory Check") =
    AppLogger.logMemoryUsage(getLogTag(), context)

fun Any.logError(operation: String, error: Throwable, context: Map<String, Any>? = null) =
    AppLogger.logError(getLogTag(), operation, error, context)
