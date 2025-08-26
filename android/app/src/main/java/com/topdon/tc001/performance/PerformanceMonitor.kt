package com.topdon.tc001.performance

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Debug
import android.os.Process
import com.elvishew.xlog.XLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

class PerformanceMonitor(private val context: Context) {
    
    companion object {
        private const val TAG = "PerformanceMonitor"
        
        private const val MEMORY_WARNING_THRESHOLD_MB = 400
        private const val MEMORY_CRITICAL_THRESHOLD_MB = 500
        private const val CPU_WARNING_THRESHOLD = 80.0
        private const val BATTERY_WARNING_LEVEL = 20
        private const val UPDATE_INTERVAL_MS = 5000L
        
        private const val BYTES_IN_MEGABYTE = 1024 * 1024
    }
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager?
    
    private val gsrSamplesProcessed = AtomicLong(0)
    private val networkBytesTransferred = AtomicLong(0)
    private val fileOperationsCount = AtomicLong(0)
    
    private val memoryHistory = mutableListOf<Long>()
    private val cpuHistory = mutableListOf<Double>()
    private val batteryHistory = mutableListOf<Int>()
    private var lastCpuTime = 0L
    private var lastCpuIdleTime = 0L
    
    private var monitoringJob: Job? = null
    private var isMonitoring = false
    
    fun startMonitoring() {
        if (isMonitoring) {
            XLog.w(TAG, "Performance monitoring already running")
            return
        }
        
        isMonitoring = true
        monitoringJob = coroutineScope.launch {
            XLog.i(TAG, "Started performance monitoring")
            
            while (isMonitoring) {
                try {
                    updatePerformanceMetrics()
                    delay(UPDATE_INTERVAL_MS)
                } catch (e: Exception) {
                    XLog.e(TAG, "Error in performance monitoring: ${e.message}", e)
                    delay(UPDATE_INTERVAL_MS)
                }
            }
        }
    }
    
    fun stopMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
        XLog.i(TAG, "Stopped performance monitoring")
    }
    
    private suspend fun updatePerformanceMetrics() = withContext(Dispatchers.IO) {
        val memoryInfo = getMemoryInfo()
        val cpuUsage = getCpuUsage()
        val batteryInfo = getBatteryInfo()
        val storageInfo = getStorageInfo()
        val networkInfo = getNetworkInfo()
        
        updateHistoricalData(memoryInfo.usedMemoryMB, cpuUsage, batteryInfo.level)
        
        val alerts = generatePerformanceAlerts(memoryInfo, cpuUsage, batteryInfo)
        val recommendations = generateOptimizationRecommendations(memoryInfo, cpuUsage, batteryInfo)
        
        val memoryScore = calculateMemoryScore(memoryInfo)
        val cpuScore = calculateCpuScore(cpuUsage)
        val batteryScore = calculateBatteryScore(batteryInfo)
        val overallScore = (memoryScore + cpuScore + batteryScore) / 3.0
        
        _performanceMetrics.value = PerformanceMetrics(
            memoryInfo = memoryInfo,
            cpuUsage = cpuUsage,
            batteryInfo = batteryInfo,
            storageInfo = storageInfo,
            networkInfo = networkInfo,
            gsrProcessingMetrics = GSRProcessingMetrics(
                samplesProcessed = gsrSamplesProcessed.get(),
                processingRate = calculateGSRProcessingRate(),
                memoryPerSample = if (gsrSamplesProcessed.get() > 0) {
                    memoryInfo.usedMemoryMB * BYTES_IN_MEGABYTE / gsrSamplesProcessed.get()
                } else 0L
            ),
            performanceScores = PerformanceScores(
                memoryScore = memoryScore,
                cpuScore = cpuScore,
                batteryScore = batteryScore,
                overallScore = overallScore
            ),
            alerts = alerts,
            recommendations = recommendations,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    private fun getMemoryInfo(): MemoryInfo {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val runtime = Runtime.getRuntime()
        val nativeHeapSize = Debug.getNativeHeapSize()
        val nativeHeapUsed = Debug.getNativeHeapAllocatedSize()
        
        return MemoryInfo(
            totalMemoryMB = memInfo.totalMem / BYTES_IN_MEGABYTE,
            availableMemoryMB = memInfo.availMem / BYTES_IN_MEGABYTE,
            usedMemoryMB = (memInfo.totalMem - memInfo.availMem) / BYTES_IN_MEGABYTE,
            appHeapMaxMB = runtime.maxMemory() / BYTES_IN_MEGABYTE,
            appHeapUsedMB = (runtime.totalMemory() - runtime.freeMemory()) / BYTES_IN_MEGABYTE,
            appHeapFreeMB = runtime.freeMemory() / BYTES_IN_MEGABYTE,
            nativeHeapSizeMB = nativeHeapSize / BYTES_IN_MEGABYTE,
            nativeHeapUsedMB = nativeHeapUsed / BYTES_IN_MEGABYTE,
            lowMemoryThreshold = memInfo.threshold / BYTES_IN_MEGABYTE,
            isLowMemory = memInfo.lowMemory
        )
    }
    
    private fun getCpuUsage(): Double {
        try {
            val cpuInfo = readCpuInfo()
            val totalCpuTime = cpuInfo.user + cpuInfo.nice + cpuInfo.system + cpuInfo.idle + cpuInfo.iowait + cpuInfo.irq + cpuInfo.softirq
            val idleCpuTime = cpuInfo.idle + cpuInfo.iowait
            
            return if (lastCpuTime > 0) {
                val totalDelta = totalCpuTime - lastCpuTime
                val idleDelta = idleCpuTime - lastCpuIdleTime
                
                if (totalDelta > 0) {
                    val usage = ((totalDelta - idleDelta) * 100.0) / totalDelta
                    lastCpuTime = totalCpuTime
                    lastCpuIdleTime = idleCpuTime
                    usage.coerceIn(0.0, 100.0)
                } else {
                    0.0
                }
            } else {
                lastCpuTime = totalCpuTime
                lastCpuIdleTime = idleCpuTime
                0.0
            }
        } catch (e: Exception) {
            XLog.w(TAG, "Failed to read CPU usage: ${e.message}")
            return 0.0
        }
    }
    
    private fun readCpuInfo(): CpuInfo {
        return try {
            val cpuLine = File("/proc/stat").readLines().first { it.startsWith("cpu ") }
            val values = cpuLine.split("\\s+".toRegex()).drop(1).map { it.toLong() }
            
            CpuInfo(
                user = values.getOrElse(0) { 0 },
                nice = values.getOrElse(1) { 0 },
                system = values.getOrElse(2) { 0 },
                idle = values.getOrElse(3) { 0 },
                iowait = values.getOrElse(4) { 0 },
                irq = values.getOrElse(5) { 0 },
                softirq = values.getOrElse(6) { 0 }
            )
        } catch (e: Exception) {
            XLog.w(TAG, "Failed to read /proc/stat: ${e.message}")
            CpuInfo()
        }
    }
    
    private fun getBatteryInfo(): BatteryInfo {
        return try {
            val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            val temperature = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: -1
            val voltage = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE) ?: -1
            
            BatteryInfo(
                level = level,
                temperature = temperature / 10.0,
                voltage = voltage / 1000.0,
                isCharging = batteryManager?.isCharging ?: false,
                chargingTimeRemaining = batteryManager?.computeChargeTimeRemaining() ?: -1L
            )
        } catch (e: Exception) {
            XLog.w(TAG, "Failed to read battery info: ${e.message}")
            BatteryInfo()
        }
    }
    
    private fun getStorageInfo(): StorageInfo {
        return try {
            val dataDir = context.filesDir
            val totalSpace = dataDir.totalSpace
            val usableSpace = dataDir.usableSpace
            val usedSpace = totalSpace - usableSpace
            
            StorageInfo(
                totalSpaceMB = totalSpace / BYTES_IN_MEGABYTE,
                usedSpaceMB = usedSpace / BYTES_IN_MEGABYTE,
                availableSpaceMB = usableSpace / BYTES_IN_MEGABYTE,
                fileOperationsCount = fileOperationsCount.get()
            )
        } catch (e: Exception) {
            XLog.w(TAG, "Failed to read storage info: ${e.message}")
            StorageInfo()
        }
    }
    
    private fun getNetworkInfo(): NetworkInfo {
        return try {

            NetworkInfo(
                bytesTransferred = networkBytesTransferred.get(),
                connectionCount = 1,
                transferRate = calculateNetworkTransferRate()
            )
        } catch (e: Exception) {
            XLog.w(TAG, "Failed to read network info: ${e.message}")
            NetworkInfo()
        }
    }
    
    private fun calculateGSRProcessingRate(): Double {
        val currentTime = System.currentTimeMillis()
        val timeElapsed = currentTime - (_performanceMetrics.value.lastUpdateTime.takeIf { it > 0 } ?: currentTime)
        
        return if (timeElapsed > 0) {
            gsrSamplesProcessed.get() * 1000.0 / timeElapsed
        } else {
            0.0
        }
    }
    
    private fun calculateNetworkTransferRate(): Double {
        val currentTime = System.currentTimeMillis()
        val timeElapsed = currentTime - (_performanceMetrics.value.lastUpdateTime.takeIf { it > 0 } ?: currentTime)
        
        return if (timeElapsed > 0) {
            networkBytesTransferred.get() * 1000.0 / timeElapsed
        } else {
            0.0
        }
    }
    
    private fun updateHistoricalData(memoryUsage: Long, cpuUsage: Double, batteryLevel: Int) {

        memoryHistory.add(memoryUsage)
        if (memoryHistory.size > 24) memoryHistory.removeAt(0)
        
        cpuHistory.add(cpuUsage)
        if (cpuHistory.size > 24) cpuHistory.removeAt(0)
        
        batteryHistory.add(batteryLevel)
        if (batteryHistory.size > 60) batteryHistory.removeAt(0)
    }
    
    private fun generatePerformanceAlerts(
        memoryInfo: MemoryInfo,
        cpuUsage: Double,
        batteryInfo: BatteryInfo
    ): List<PerformanceAlert> {
        val alerts = mutableListOf<PerformanceAlert>()
        
        when {
            memoryInfo.usedMemoryMB > MEMORY_CRITICAL_THRESHOLD_MB -> {
                alerts.add(PerformanceAlert(
                    type = AlertType.MEMORY_CRITICAL,
                    severity = AlertSeverity.CRITICAL,
                    message = "Critical memory usage: ${memoryInfo.usedMemoryMB}MB",
                    recommendation = "Stop non-essential processes immediately"
                ))
            }
            memoryInfo.usedMemoryMB > MEMORY_WARNING_THRESHOLD_MB -> {
                alerts.add(PerformanceAlert(
                    type = AlertType.MEMORY_WARNING,
                    severity = AlertSeverity.WARNING,
                    message = "High memory usage: ${memoryInfo.usedMemoryMB}MB",
                    recommendation = "Consider reducing data buffer sizes"
                ))
            }
        }
        
        if (cpuUsage > CPU_WARNING_THRESHOLD) {
            alerts.add(PerformanceAlert(
                type = AlertType.CPU_HIGH,
                severity = AlertSeverity.WARNING,
                message = "High CPU usage: ${cpuUsage.toInt()}%",
                recommendation = "Reduce GSR sampling rate or processing complexity"
            ))
        }
        
        if (batteryInfo.level > 0 && batteryInfo.level < BATTERY_WARNING_LEVEL) {
            alerts.add(PerformanceAlert(
                type = AlertType.BATTERY_LOW,
                severity = AlertSeverity.WARNING,
                message = "Low battery: ${batteryInfo.level}%",
                recommendation = "Connect charger or enable power saving mode"
            ))
        }
        
        if (memoryHistory.size >= 10) {
            val recentTrend = memoryHistory.takeLast(10)
            val isIncreasing = recentTrend.zipWithNext { a, b -> b > a }.all { it }
            if (isIncreasing && recentTrend.last() - recentTrend.first() > 50) {
                alerts.add(PerformanceAlert(
                    type = AlertType.MEMORY_LEAK,
                    severity = AlertSeverity.HIGH,
                    message = "Potential memory leak detected",
                    recommendation = "Restart application to free memory"
                ))
            }
        }
        
        return alerts
    }
    
    private fun generateOptimizationRecommendations(
        memoryInfo: MemoryInfo,
        cpuUsage: Double,
        batteryInfo: BatteryInfo
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (memoryInfo.usedMemoryMB > 300) {
            recommendations.add("Reduce GSR data buffer size to minimize memory usage")
            recommendations.add("Enable automatic old file cleanup")
        }
        
        if (cpuUsage > 60) {
            recommendations.add("Lower GSR sampling rate from 128Hz to 64Hz if acceptable")
            recommendations.add("Disable advanced signal processing during recording")
        }
        
        if (batteryInfo.level < 30 && batteryInfo.level > 0) {
            recommendations.add("Enable battery optimization mode")
            recommendations.add("Reduce screen brightness and disable unnecessary features")
            recommendations.add("Consider shorter recording sessions to preserve battery")
        }
        
        if (cpuHistory.isNotEmpty() && cpuHistory.average() > 70) {
            recommendations.add("CPU usage consistently high - consider background processing limits")
        }
        
        return recommendations
    }
    
    private fun calculateMemoryScore(memoryInfo: MemoryInfo): Double {
        val usageRatio = memoryInfo.usedMemoryMB.toDouble() / memoryInfo.totalMemoryMB.toDouble()
        return max(0.0, 100.0 - (usageRatio * 100.0))
    }
    
    private fun calculateCpuScore(cpuUsage: Double): Double {
        return max(0.0, 100.0 - cpuUsage)
    }
    
    private fun calculateBatteryScore(batteryInfo: BatteryInfo): Double {
        return if (batteryInfo.level > 0) {
            batteryInfo.level.toDouble()
        } else {
            50.0
        }
    }
    
    fun recordGSRSampleProcessed() {
        gsrSamplesProcessed.incrementAndGet()
    }
    
    fun recordNetworkTransfer(bytes: Long) {
        networkBytesTransferred.addAndGet(bytes)
    }
    
    fun recordFileOperation() {
        fileOperationsCount.incrementAndGet()
    }
    
    fun getPerformanceSummary(): PerformanceSummary {
        val current = _performanceMetrics.value
        
        return PerformanceSummary(
            overallScore = current.performanceScores.overallScore,
            memoryUsageMB = current.memoryInfo.usedMemoryMB,
            cpuUsage = current.cpuUsage,
            batteryLevel = current.batteryInfo.level,
            activeAlerts = current.alerts.size,
            gsrProcessingRate = current.gsrProcessingMetrics.processingRate,
            recommendationCount = current.recommendations.size
        )
    }
    
    fun cleanup() {
        stopMonitoring()
        coroutineScope.cancel()
        memoryHistory.clear()
        cpuHistory.clear()
        batteryHistory.clear()
        XLog.i(TAG, "Performance monitor cleaned up")
    }
}

data class PerformanceMetrics(
    val memoryInfo: MemoryInfo = MemoryInfo(),
    val cpuUsage: Double = 0.0,
    val batteryInfo: BatteryInfo = BatteryInfo(),
    val storageInfo: StorageInfo = StorageInfo(),
    val networkInfo: NetworkInfo = NetworkInfo(),
    val gsrProcessingMetrics: GSRProcessingMetrics = GSRProcessingMetrics(),
    val performanceScores: PerformanceScores = PerformanceScores(),
    val alerts: List<PerformanceAlert> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val lastUpdateTime: Long = 0
)

data class MemoryInfo(
    val totalMemoryMB: Long = 0,
    val availableMemoryMB: Long = 0,
    val usedMemoryMB: Long = 0,
    val appHeapMaxMB: Long = 0,
    val appHeapUsedMB: Long = 0,
    val appHeapFreeMB: Long = 0,
    val nativeHeapSizeMB: Long = 0,
    val nativeHeapUsedMB: Long = 0,
    val lowMemoryThreshold: Long = 0,
    val isLowMemory: Boolean = false
)

data class BatteryInfo(
    val level: Int = -1,
    val temperature: Double = 0.0,
    val voltage: Double = 0.0,
    val isCharging: Boolean = false,
    val chargingTimeRemaining: Long = -1L
)

data class StorageInfo(
    val totalSpaceMB: Long = 0,
    val usedSpaceMB: Long = 0,
    val availableSpaceMB: Long = 0,
    val fileOperationsCount: Long = 0
)

data class NetworkInfo(
    val bytesTransferred: Long = 0,
    val connectionCount: Int = 0,
    val transferRate: Double = 0.0
)

data class GSRProcessingMetrics(
    val samplesProcessed: Long = 0,
    val processingRate: Double = 0.0,
    val memoryPerSample: Long = 0
)

data class PerformanceScores(
    val memoryScore: Double = 100.0,
    val cpuScore: Double = 100.0,
    val batteryScore: Double = 100.0,
    val overallScore: Double = 100.0
)

data class PerformanceAlert(
    val type: AlertType,
    val severity: AlertSeverity,
    val message: String,
    val recommendation: String
)

data class CpuInfo(
    val user: Long = 0,
    val nice: Long = 0,
    val system: Long = 0,
    val idle: Long = 0,
    val iowait: Long = 0,
    val irq: Long = 0,
    val softirq: Long = 0
)

data class PerformanceSummary(
    val overallScore: Double,
    val memoryUsageMB: Long,
    val cpuUsage: Double,
    val batteryLevel: Int,
    val activeAlerts: Int,
    val gsrProcessingRate: Double,
    val recommendationCount: Int
)

enum class AlertType {
    MEMORY_WARNING,
    MEMORY_CRITICAL,
    MEMORY_LEAK,
    CPU_HIGH,
    BATTERY_LOW,
    STORAGE_LOW,
    NETWORK_SLOW
}

enum class AlertSeverity {
    LOW,
    WARNING,
    HIGH,
    CRITICAL
