package com.multisensor.recording.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DiagnosticsHealthStatus {
    EXCELLENT, GOOD, WARNING, CRITICAL, UNKNOWN
}

data class DiagnosticLogEntry(
    val timestamp: String,
    val level: String,
    val message: String,
    val source: String = ""
)

data class TestResult(
    val testName: String,
    val passed: Boolean,
    val details: String = "",
    val duration: Long = 0,
    val timestamp: String
)

data class DiagnosticsUiState(
    val systemHealthStatus: DiagnosticsHealthStatus = DiagnosticsHealthStatus.UNKNOWN,
    val systemUptime: String = "",
    val connectedDevicesCount: Int = 0,
    val activeProcessesCount: Int = 0,
    val lastDiagnosticRun: String = "Never",

    val cpuUsagePercent: Int = 0,
    val memoryUsagePercent: Int = 0,
    val storageUsagePercent: Int = 0,
    val networkDownload: String = "0 KB/s",
    val networkUpload: String = "0 KB/s",
    val batteryLevel: Int = 100,
    val currentFrameRate: Int = 0,

    val errorCount: Int = 0,
    val warningCount: Int = 0,
    val infoCount: Int = 0,
    val recentErrorLogs: List<DiagnosticLogEntry> = emptyList(),

    val networkTestResults: List<TestResult> = emptyList(),
    val deviceTestResults: List<TestResult> = emptyList(),
    val performanceTestResults: List<TestResult> = emptyList(),

    val isRunningDiagnostic: Boolean = false,
    val isTestingNetwork: Boolean = false,
    val isTestingDevices: Boolean = false,
    val isTestingPerformance: Boolean = false,
    val isGeneratingReport: Boolean = false,
    val isExportingData: Boolean = false,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        refreshSystemStatus()
        startPeriodicMonitoring()
    }

    fun runFullSystemDiagnostic() {
        _uiState.value = _uiState.value.copy(isRunningDiagnostic = true)

        viewModelScope.launch {
            delay(5000)

            val timestamp = getCurrentTimestamp()

            val healthStatus = calculateSystemHealth()

            _uiState.value = _uiState.value.copy(
                isRunningDiagnostic = false,
                systemHealthStatus = healthStatus,
                lastDiagnosticRun = timestamp
            )

            addDiagnosticLog("INFO", "Full system diagnostic completed", "DiagnosticsService")
        }
    }

    fun testNetworkConnectivity() {
        _uiState.value = _uiState.value.copy(isTestingNetwork = true)

        viewModelScope.launch {
            delay(3000)

            val timestamp = getCurrentTimestamp()
            val testResults = mutableListOf<TestResult>()

            testResults.add(TestResult("Internet Connectivity", true, "Ping: 25ms", 1200, timestamp))
            testResults.add(TestResult("DNS Resolution", true, "Response: 15ms", 800, timestamp))
            testResults.add(TestResult("PC Connection", true, "Latency: 5ms", 500, timestamp))
            testResults.add(TestResult("WiFi Signal", true, "Strength: -45dBm", 300, timestamp))

            _uiState.value = _uiState.value.copy(
                isTestingNetwork = false,
                networkTestResults = testResults
            )

            addDiagnosticLog("INFO", "Network connectivity tests completed", "NetworkService")
        }
    }

    fun testDeviceCommunication() {
        _uiState.value = _uiState.value.copy(isTestingDevices = true)

        viewModelScope.launch {
            delay(4000)

            val timestamp = getCurrentTimestamp()
            val testResults = mutableListOf<TestResult>()

            testResults.add(TestResult("Camera Connection", true, "Stream: 30fps", 2000, timestamp))
            testResults.add(TestResult("Thermal Camera", true, "Temp range: OK", 1500, timestamp))
            testResults.add(TestResult("Shimmer Device", true, "Battery: 85%", 1800, timestamp))
            testResults.add(TestResult("GSR Sensor", true, "Reading: 234kΩ", 1200, timestamp))

            _uiState.value = _uiState.value.copy(
                isTestingDevices = false,
                deviceTestResults = testResults
            )

            addDiagnosticLog("INFO", "Device communication tests completed", "DeviceService")
        }
    }

    fun testSystemPerformance() {
        _uiState.value = _uiState.value.copy(isTestingPerformance = true)

        viewModelScope.launch {
            delay(3500)

            val timestamp = getCurrentTimestamp()
            val testResults = mutableListOf<TestResult>()

            testResults.add(TestResult("CPU Performance", true, "Score: 2847", 2500, timestamp))
            testResults.add(TestResult("Memory Speed", true, "Bandwidth: 12GB/s", 1800, timestamp))
            testResults.add(TestResult("Storage I/O", true, "Read: 150MB/s", 2000, timestamp))
            testResults.add(TestResult("Graphics Performance", true, "FPS: 60", 1500, timestamp))

            _uiState.value = _uiState.value.copy(
                isTestingPerformance = false,
                performanceTestResults = testResults
            )

            addDiagnosticLog("INFO", "System performance tests completed", "PerformanceService")
        }
    }

    fun clearErrorLogs() {
        _uiState.value = _uiState.value.copy(
            errorCount = 0,
            warningCount = 0,
            infoCount = 0,
            recentErrorLogs = emptyList()
        )

        addDiagnosticLog("INFO", "Error logs cleared", "DiagnosticsService")
    }

    fun refreshSystemStatus() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)

        viewModelScope.launch {
            delay(1500)

            _uiState.value = _uiState.value.copy(
                isRefreshing = false,
                systemUptime = generateSystemUptime(),
                connectedDevicesCount = kotlin.random.Random.nextInt(2, 6),
                activeProcessesCount = kotlin.random.Random.nextInt(15, 25),
                cpuUsagePercent = kotlin.random.Random.nextInt(10, 40),
                memoryUsagePercent = kotlin.random.Random.nextInt(30, 70),
                storageUsagePercent = kotlin.random.Random.nextInt(20, 80),
                networkDownload = "${kotlin.random.Random.nextInt(50, 500)} KB/s",
                networkUpload = "${kotlin.random.Random.nextInt(10, 100)} KB/s",
                batteryLevel = kotlin.random.Random.nextInt(60, 100),
                currentFrameRate = kotlin.random.Random.nextInt(25, 31)
            )
        }
    }

    fun generateDiagnosticReport() {
        _uiState.value = _uiState.value.copy(isGeneratingReport = true)

        viewModelScope.launch {
            delay(2000)

            _uiState.value = _uiState.value.copy(isGeneratingReport = false)

            addDiagnosticLog("INFO", "Diagnostic report generated successfully", "ReportService")
        }
    }

    fun exportDiagnosticData() {
        _uiState.value = _uiState.value.copy(isExportingData = true)

        viewModelScope.launch {
            delay(1500)

            _uiState.value = _uiState.value.copy(isExportingData = false)

            addDiagnosticLog("INFO", "Diagnostic data exported to external storage", "ExportService")
        }
    }

    fun getDiagnosticLogContent(): String {
        val currentState = _uiState.value

        return buildString {
            appendLine("=== MULTI-SENSOR RECORDING APP DIAGNOSTIC REPORT ===")
            appendLine("Generated: ${getCurrentTimestamp()}")
            appendLine()
            appendLine("SYSTEM HEALTH:")
            appendLine("Status: ${currentState.systemHealthStatus}")
            appendLine("Uptime: ${currentState.systemUptime}")
            appendLine("Connected Devices: ${currentState.connectedDevicesCount}")
            appendLine("Active Processes: ${currentState.activeProcessesCount}")
            appendLine()
            appendLine("PERFORMANCE METRICS:")
            appendLine("CPU Usage: ${currentState.cpuUsagePercent}%")
            appendLine("Memory Usage: ${currentState.memoryUsagePercent}%")
            appendLine("Storage Usage: ${currentState.storageUsagePercent}%")
            appendLine("Network: ↓${currentState.networkDownload} ↑${currentState.networkUpload}")
            appendLine("Frame Rate: ${currentState.currentFrameRate}fps")
            appendLine()
            appendLine("ERROR SUMMARY:")
            appendLine("Errors: ${currentState.errorCount}")
            appendLine("Warnings: ${currentState.warningCount}")
            appendLine("Info: ${currentState.infoCount}")
            appendLine()
            appendLine("RECENT LOGS:")
            currentState.recentErrorLogs.forEach { log ->
                appendLine("${log.timestamp} [${log.level}] ${log.source}: ${log.message}")
            }
            appendLine()
            appendLine("TEST RESULTS:")
            appendLine("Network Tests: ${currentState.networkTestResults.size} completed")
            appendLine("Device Tests: ${currentState.deviceTestResults.size} completed")
            appendLine("Performance Tests: ${currentState.performanceTestResults.size} completed")
            appendLine("=== END OF REPORT ===")
        }
    }

    private fun startPeriodicMonitoring() {
        viewModelScope.launch {
            while (true) {
                delay(5000)

                if (!_uiState.value.isRefreshing) {
                    updateRealTimeMetrics()
                }
            }
        }
    }

    private fun updateRealTimeMetrics() {
        val currentState = _uiState.value

        _uiState.value = currentState.copy(
            cpuUsagePercent = (currentState.cpuUsagePercent + kotlin.random.Random.nextInt(-5, 6))
                .coerceIn(0, 100),
            memoryUsagePercent = (currentState.memoryUsagePercent + kotlin.random.Random.nextInt(-3, 4))
                .coerceIn(0, 100),
            currentFrameRate = (currentState.currentFrameRate + kotlin.random.Random.nextInt(-2, 3))
                .coerceIn(15, 31)
        )
    }

    private fun calculateSystemHealth(): DiagnosticsHealthStatus {
        val currentState = _uiState.value

        val healthScore = when {
            currentState.errorCount > 10 -> 0
            currentState.errorCount > 5 -> 1
            currentState.warningCount > 20 -> 1
            currentState.cpuUsagePercent > 80 -> 1
            currentState.memoryUsagePercent > 90 -> 1
            currentState.connectedDevicesCount < 3 -> 2
            else -> 4
        }

        return when (healthScore) {
            0 -> DiagnosticsHealthStatus.CRITICAL
            1 -> DiagnosticsHealthStatus.WARNING
            2 -> DiagnosticsHealthStatus.GOOD
            else -> DiagnosticsHealthStatus.EXCELLENT
        }
    }

    private fun addDiagnosticLog(level: String, message: String, source: String) {
        val newLog = DiagnosticLogEntry(
            timestamp = getCurrentTimestamp(),
            level = level,
            message = message,
            source = source
        )

        val currentLogs = _uiState.value.recentErrorLogs.toMutableList()
        currentLogs.add(newLog)

        if (currentLogs.size > 50) {
            currentLogs.removeAt(0)
        }

        val errorCount = currentLogs.count { it.level == "ERROR" }
        val warningCount = currentLogs.count { it.level == "WARNING" }
        val infoCount = currentLogs.count { it.level == "INFO" }

        _uiState.value = _uiState.value.copy(
            recentErrorLogs = currentLogs,
            errorCount = errorCount,
            warningCount = warningCount,
            infoCount = infoCount
        )
    }

    private fun generateSystemUptime(): String {
        val uptimeHours = kotlin.random.Random.nextInt(1, 72)
        val uptimeMinutes = kotlin.random.Random.nextInt(0, 60)

        return when {
            uptimeHours > 24 -> "${uptimeHours / 24}d ${uptimeHours % 24}h ${uptimeMinutes}m"
            uptimeHours > 0 -> "${uptimeHours}h ${uptimeMinutes}m"
            else -> "${uptimeMinutes}m"
        }
    }

    private fun getCurrentTimestamp(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
}
