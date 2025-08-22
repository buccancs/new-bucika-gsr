package com.multisensor.recording.controllers

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.widget.Toast
import com.multisensor.recording.managers.UsbDeviceManager
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbController @Inject constructor(
    private val usbDeviceManager: UsbDeviceManager
) {

    private val performanceAnalytics = UsbPerformanceAnalytics()

    private val devicePrioritizer = UsbDevicePrioritizer(performanceAnalytics)

    companion object {
        private const val USB_PREFS_NAME = "usb_device_prefs"
        private const val PREF_LAST_CONNECTED_DEVICE = "last_connected_device"
        private const val PREF_LAST_CONNECTION_TIME = "last_connection_time"
        private const val PREF_CONNECTION_COUNT = "connection_count"
        private const val PREF_DEVICE_VENDOR_ID = "device_vendor_id"
        private const val PREF_DEVICE_PRODUCT_ID = "device_product_id"
        private const val SCANNING_INTERVAL_MS = 5000L

        private const val PREF_DEVICE_PROFILES = "device_profiles"
        private const val PREF_DEVICE_FILTERS = "device_filters"
        private const val PREF_CALIBRATION_STATES = "calibration_states"
        private const val PREF_PRIORITY_CONFIG = "priority_config"
    }

    data class DeviceProfile(
        val deviceId: String,
        val vendorId: Int,
        val productId: Int,
        val deviceName: String,
        val settings: Map<String, Any> = emptyMap(),
        val calibrationData: Map<String, Any> = emptyMap(),
        val lastUsed: Long = System.currentTimeMillis(),
        val priority: Int = 0
    )

    data class DeviceFilter(
        val vendorIds: Set<Int> = emptySet(),
        val productIds: Set<Int> = emptySet(),
        val deviceNames: Set<String> = emptySet(),
        val requireCalibration: Boolean = false,
        val minPriority: Int = 0
    )

    data class NetworkDeviceStatus(
        val deviceId: String,
        val status: String,
        val lastUpdate: Long,
        val metrics: Map<String, Any> = emptyMap()
    )

    interface UsbCallback {
        fun onSupportedDeviceAttached(device: UsbDevice)
        fun onUnsupportedDeviceAttached(device: UsbDevice)
        fun onDeviceDetached(device: UsbDevice)
        fun onUsbError(message: String)
        fun updateStatusText(text: String)
        fun showToast(message: String, duration: Int)
        fun getContext(): Context
        fun initializeRecordingSystem()
        fun areAllPermissionsGranted(): Boolean
    }

    private var callback: UsbCallback? = null

    private val deviceProfiles = mutableMapOf<String, DeviceProfile>()
    private val calibrationStates = mutableMapOf<String, Map<String, Any>>()
    private var deviceFilter: DeviceFilter? = null
    private val networkStatusReporter = mutableMapOf<String, NetworkDeviceStatus>()
    private var hotSwapDetectionEnabled = true

    private val connectedSupportedDevices = mutableMapOf<String, UsbDevice>()
    private val deviceConnectionTimes = mutableMapOf<String, Long>()
    private val deviceConnectionCounts = mutableMapOf<String, Int>()

    private var isScanning = false
    private var lastKnownDevices = mutableSetOf<String>()
    private val scanningHandler = android.os.Handler(android.os.Looper.getMainLooper())

    fun setCallback(callback: UsbCallback) {
        this.callback = callback
    }

    fun handleUsbDeviceIntent(context: Context, intent: Intent) {
        val startTime = System.currentTimeMillis()
        android.util.Log.d("UsbController", "[DEBUG_LOG] Handling USB device intent: ${intent.action}")

        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                handleUsbDeviceAttached(context, intent)
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                handleUsbDeviceDetached(context, intent)
            }

            else -> {
                android.util.Log.d(
                    "UsbController",
                    "[DEBUG_LOG] Intent action: ${intent.action} (not USB device related)"
                )
            }
        }

        val duration = System.currentTimeMillis() - startTime
        performanceAnalytics.recordEvent(
            UsbPerformanceAnalytics.PerformanceEventType.CALLBACK_NOTIFICATION,
            duration
        )
    }

    private fun handleUsbDeviceAttached(context: Context, intent: Intent) {
        val device: UsbDevice? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }

        device?.let { usbDevice ->
            android.util.Log.d("UsbController", "[DEBUG_LOG] USB device attached:")
            android.util.Log.d("UsbController", "[DEBUG_LOG] - Device name: ${usbDevice.deviceName}")
            android.util.Log.d(
                "UsbController",
                "[DEBUG_LOG] - Vendor ID: 0x${String.format("%04X", usbDevice.vendorId)}"
            )
            android.util.Log.d(
                "UsbController",
                "[DEBUG_LOG] - Product ID: 0x${String.format("%04X", usbDevice.productId)}"
            )
            android.util.Log.d("UsbController", "[DEBUG_LOG] - Device class: ${usbDevice.deviceClass}")

            if (usbDeviceManager.isSupportedTopdonDevice(usbDevice)) {
                handleSupportedDeviceAttached(context, usbDevice)
            } else {
                handleUnsupportedDeviceAttached(context, usbDevice)
            }
        } ?: run {
            android.util.Log.w("UsbController", "[DEBUG_LOG] USB device attachment intent received but no device found")
            callback?.onUsbError("USB device attachment detected but no device information available")
        }
    }

    private fun handleUsbDeviceDetached(context: Context, intent: Intent) {
        val device: UsbDevice? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }

        device?.let { usbDevice ->
            val deviceKey = getDeviceKey(usbDevice)
            android.util.Log.d("UsbController", "[DEBUG_LOG] USB device detached:")
            android.util.Log.d("UsbController", "[DEBUG_LOG] - Device name: ${usbDevice.deviceName}")
            android.util.Log.d("UsbController", "[DEBUG_LOG] - Device key: $deviceKey")
            android.util.Log.d(
                "UsbController",
                "[DEBUG_LOG] - Vendor ID: 0x${String.format("%04X", usbDevice.vendorId)}"
            )
            android.util.Log.d(
                "UsbController",
                "[DEBUG_LOG] - Product ID: 0x${String.format("%04X", usbDevice.productId)}"
            )

            callback?.onDeviceDetached(usbDevice)

            if (usbDeviceManager.isSupportedTopdonDevice(usbDevice)) {
                connectedSupportedDevices.remove(deviceKey)

                val remainingDevices = connectedSupportedDevices.size
                val message = if (remainingDevices == 0) {
                    "Topdon Thermal Camera Disconnected"
                } else {
                    "Topdon Camera Disconnected\nRemaining devices: $remainingDevices"
                }

                callback?.updateStatusText(getMultiDeviceStatusText())
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                saveMultiDeviceState(context)
            }
        } ?: run {
            android.util.Log.w("UsbController", "[DEBUG_LOG] USB device detachment intent received but no device found")
        }
    }

    private fun handleSupportedDeviceAttached(context: Context, usbDevice: UsbDevice) {
        val startTime = System.currentTimeMillis()
        val deviceKey = getDeviceKey(usbDevice)
        android.util.Log.d("UsbController", "[DEBUG_LOG] ✓ Supported Topdon thermal camera detected!")
        android.util.Log.d("UsbController", "[DEBUG_LOG] Device key: $deviceKey")

        connectedSupportedDevices[deviceKey] = usbDevice
        deviceConnectionTimes[deviceKey] = System.currentTimeMillis()
        deviceConnectionCounts[deviceKey] = (deviceConnectionCounts[deviceKey] ?: 0) + 1

        val deviceCount = connectedSupportedDevices.size
        val deviceAssessments = getDevicePriorityAssessments()
        val selectionResult = devicePrioritizer.optimizeDeviceSelection(deviceAssessments)

        val message = if (deviceCount == 1) {
            "Topdon Thermal Camera Connected!\nDevice: ${usbDevice.deviceName}"
        } else {
            val primaryDevice = selectionResult.primaryDevice?.deviceKey?.substringAfterLast("/") ?: "Unknown"
            "Topdon Camera #$deviceCount Connected!\nDevice: ${usbDevice.deviceName}\nPrimary: $primaryDevice"
        }

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()

        val statusText = when (deviceCount) {
            1 -> "1 Topdon thermal camera connected - Ready for recording"
            else -> {
                val primaryInfo = selectionResult.primaryDevice?.let {
                    it.deviceKey.substringAfterLast("/") + " (${it.priorityLevel.name})"
                } ?: "Auto-selecting"
                "$deviceCount Topdon thermal cameras connected - Primary: $primaryInfo"
            }
        }
        callback?.updateStatusText(statusText)

        saveDeviceConnectionState(context, usbDevice)
        saveMultiDeviceState(context)

        if (callback?.areAllPermissionsGranted() == true) {
            android.util.Log.d("UsbController", "[DEBUG_LOG] Permissions available, initializing thermal recorder")
            callback?.initializeRecordingSystem()
        } else {
            android.util.Log.d("UsbController", "[DEBUG_LOG] Permissions not available, requesting permissions first")
            callback?.updateStatusText("Thermal camera detected - Please grant permissions to continue")
        }

        callback?.onSupportedDeviceAttached(usbDevice)

        val duration = System.currentTimeMillis() - startTime
        performanceAnalytics.recordEvent(
            UsbPerformanceAnalytics.PerformanceEventType.DEVICE_ATTACHMENT,
            duration,
            deviceKey
        )
    }

    private fun handleUnsupportedDeviceAttached(context: Context, usbDevice: UsbDevice) {
        android.util.Log.d("UsbController", "[DEBUG_LOG] ⚠ USB device is not a supported Topdon thermal camera")
        android.util.Log.d(
            "UsbController",
            "[DEBUG_LOG] Supported devices: VID=0x0BDA, PID=0x3901/0x5840/0x5830/0x5838"
        )

        callback?.onUnsupportedDeviceAttached(usbDevice)

        android.util.Log.i("UsbController", "Unsupported USB device connected: ${usbDevice.deviceName}")
    }

    fun getConnectedUsbDevices(context: Context): List<UsbDevice> {
        return usbDeviceManager.getConnectedUsbDevices(context)
    }

    fun getConnectedSupportedDevices(context: Context): List<UsbDevice> {
        return usbDeviceManager.getConnectedSupportedDevices(context)
    }

    fun hasSupportedDeviceConnected(context: Context): Boolean {
        return usbDeviceManager.hasSupportedDeviceConnected(context)
    }

    fun isSupportedTopdonDevice(device: UsbDevice): Boolean {
        return usbDeviceManager.isSupportedTopdonDevice(device)
    }

    fun getDeviceInfoString(device: UsbDevice): String {
        return usbDeviceManager.getDeviceInfoString(device)
    }

    fun initializeUsbMonitoring(context: Context) {
        android.util.Log.d("UsbController", "[DEBUG_LOG] Initializing USB monitoring...")

        restoreMultiDeviceState(context)

        scanForDevices(context)

        startPeriodicScanning(context)
    }

    private fun startPeriodicScanning(context: Context) {
        if (isScanning) {
            android.util.Log.d("UsbController", "[DEBUG_LOG] USB scanning already active")
            return
        }

        isScanning = true
        android.util.Log.d(
            "UsbController",
            "[DEBUG_LOG] Starting periodic USB device scanning (${SCANNING_INTERVAL_MS}ms interval)"
        )

        val scanningRunnable = object : Runnable {
            override fun run() {
                if (isScanning) {
                    scanForDevices(context)
                    scanningHandler.postDelayed(this, SCANNING_INTERVAL_MS)
                }
            }
        }

        scanningHandler.postDelayed(scanningRunnable, SCANNING_INTERVAL_MS)
    }

    fun stopPeriodicScanning() {
        if (isScanning) {
            isScanning = false
            scanningHandler.removeCallbacksAndMessages(null)
            android.util.Log.d("UsbController", "[DEBUG_LOG] USB periodic scanning stopped")
        }
    }

    private fun scanForDevices(context: Context) {
        try {
            val connectedDevices = getConnectedUsbDevices(context)
            val currentDeviceNames = connectedDevices.map { it.deviceName }.toSet()

            val newDevices = currentDeviceNames - lastKnownDevices
            val removedDevices = lastKnownDevices - currentDeviceNames

            newDevices.forEach { deviceName ->
                val device = connectedDevices.find { it.deviceName == deviceName }
                device?.let {
                    android.util.Log.d("UsbController", "[DEBUG_LOG] Detected new USB device: $deviceName")
                    if (usbDeviceManager.isSupportedTopdonDevice(it)) {
                        handleSupportedDeviceAttached(context, it)
                    }
                }
            }

            removedDevices.forEach { deviceName ->
                android.util.Log.d("UsbController", "[DEBUG_LOG] USB device disconnected: $deviceName")

                val keysToRemove = connectedSupportedDevices.filter { (_, device) ->
                    device.deviceName == deviceName
                }.keys

                keysToRemove.forEach { key ->
                    connectedSupportedDevices.remove(key)
                }

                if (keysToRemove.isNotEmpty()) {
                    callback?.updateStatusText(getMultiDeviceStatusText())
                    saveMultiDeviceState(context)
                }
            }

            lastKnownDevices = currentDeviceNames.toMutableSet()

        } catch (e: Exception) {
            android.util.Log.e("UsbController", "[DEBUG_LOG] Error during USB device scanning: ${e.message}")
        }
    }

    fun getUsbStatusSummary(context: Context): String {
        val connectedDevices = getConnectedUsbDevices(context)
        val supportedDevices = getConnectedSupportedDevices(context)
        val trackedSupportedDevices = connectedSupportedDevices.size
        val lastDeviceInfo = getLastConnectedDeviceInfo(context)

        return buildString {
            append("Enhanced USB Status Summary:\n")
            append("- Total connected devices: ${connectedDevices.size}\n")
            append("- Supported TOPDON devices (system): ${supportedDevices.size}\n")
            append("- Tracked supported devices (controller): $trackedSupportedDevices\n")
            append("- Last connected device: $lastDeviceInfo\n")
            append("- Total connections: ${getConnectionCount(context)}\n")

            if (connectedSupportedDevices.isNotEmpty()) {
                append("- Tracked supported devices:\n")
                connectedSupportedDevices.forEach { (key, device) ->
                    val connectionTime = deviceConnectionTimes[key]
                    val connectionCount = deviceConnectionCounts[key] ?: 0
                    val timeStr = if (connectionTime != null) {
                        val format = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        format.format(java.util.Date(connectionTime))
                    } else "Unknown"

                    append("  • ${device.deviceName} (Key: $key)\n")
                    append(
                        "    VID: 0x${String.format("%04X", device.vendorId)}, PID: 0x${
                            String.format(
                                "%04X",
                                device.productId
                            )
                        }\n"
                    )
                    append("    Connected: $timeStr, Count: $connectionCount\n")
                }
            }

            if (supportedDevices.isNotEmpty() && supportedDevices.size != trackedSupportedDevices) {
                append("- System-detected but not tracked:\n")
                supportedDevices.forEach { device ->
                    val key = getDeviceKey(device)
                    if (!connectedSupportedDevices.containsKey(key)) {
                        append(
                            "  • ${device.deviceName} (VID: 0x${
                                String.format(
                                    "%04X",
                                    device.vendorId
                                )
                            }, PID: 0x${String.format("%04X", device.productId)})\n"
                        )
                    }
                }
            }
        }
    }

    private fun saveDeviceConnectionState(context: Context, device: UsbDevice) {
        try {
            val prefs = context.getSharedPreferences(USB_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString(PREF_LAST_CONNECTED_DEVICE, device.deviceName)
                putLong(PREF_LAST_CONNECTION_TIME, System.currentTimeMillis())
                putInt(PREF_DEVICE_VENDOR_ID, device.vendorId)
                putInt(PREF_DEVICE_PRODUCT_ID, device.productId)
                putInt(PREF_CONNECTION_COUNT, getConnectionCount(context) + 1)
                apply()
            }

            android.util.Log.d("UsbController", "[DEBUG_LOG] Device state saved: ${device.deviceName}")
        } catch (e: Exception) {
            android.util.Log.e("UsbController", "[DEBUG_LOG] Failed to save device state: ${e.message}")
        }
    }

    private fun getLastConnectedDeviceInfo(context: Context): String {
        return try {
            val prefs = context.getSharedPreferences(USB_PREFS_NAME, Context.MODE_PRIVATE)
            val deviceName = prefs.getString(PREF_LAST_CONNECTED_DEVICE, null)
            val lastConnectionTime = prefs.getLong(PREF_LAST_CONNECTION_TIME, 0L)

            if (deviceName != null && lastConnectionTime > 0) {
                val timeFormat = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                "$deviceName (${timeFormat.format(java.util.Date(lastConnectionTime))})"
            } else {
                "None"
            }
        } catch (e: Exception) {
            android.util.Log.e("UsbController", "[DEBUG_LOG] Failed to get last device info: ${e.message}")
            "Error retrieving info"
        }
    }

    private fun getConnectionCount(context: Context): Int {
        return try {
            val prefs = context.getSharedPreferences(USB_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getInt(PREF_CONNECTION_COUNT, 0)
        } catch (e: Exception) {
            android.util.Log.e("UsbController", "[DEBUG_LOG] Failed to get connection count: ${e.message}")
            0
        }
    }

    fun hasPreviouslyConnectedDevice(context: Context): Boolean {
        return try {
            val prefs = context.getSharedPreferences(USB_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString(PREF_LAST_CONNECTED_DEVICE, null) != null
        } catch (e: Exception) {
            android.util.Log.e("UsbController", "[DEBUG_LOG] Failed to check previous device: ${e.message}")
            false
        }
    }

    private fun getDeviceKey(device: UsbDevice): String {
        return "${device.vendorId}_${device.productId}_${device.deviceName}"
    }

    fun getConnectedSupportedDevicesList(): List<UsbDevice> {
        return connectedSupportedDevices.values.toList()
    }

    fun getConnectedSupportedDeviceCount(): Int {
        return connectedSupportedDevices.size
    }

    fun getDeviceInfoByKey(deviceKey: String): UsbDevice? {
        return connectedSupportedDevices[deviceKey]
    }

    fun isDeviceConnected(deviceKey: String): Boolean {
        return connectedSupportedDevices.containsKey(deviceKey)
    }

    fun getDeviceConnectionTime(deviceKey: String): Long? {
        return deviceConnectionTimes[deviceKey]
    }

    fun getDeviceConnectionCount(deviceKey: String): Int {
        return deviceConnectionCounts[deviceKey] ?: 0
    }

    private fun getMultiDeviceStatusText(): String {
        val deviceCount = connectedSupportedDevices.size
        return when (deviceCount) {
            0 -> "No thermal cameras connected"
            1 -> "1 Topdon thermal camera connected - Ready for recording"
            else -> "$deviceCount Topdon thermal cameras connected - Ready for multi-device recording"
        }
    }

    private fun saveMultiDeviceState(context: Context) {
        try {
            val prefs = context.getSharedPreferences(USB_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putInt("connected_device_count", connectedSupportedDevices.size)
                putStringSet("connected_device_keys", connectedSupportedDevices.keys)

                connectedSupportedDevices.forEach { (key, device) ->
                    putString("device_${key}_name", device.deviceName)
                    putInt("device_${key}_vendor", device.vendorId)
                    putInt("device_${key}_product", device.productId)
                    putLong("device_${key}_connected_time", deviceConnectionTimes[key] ?: 0L)
                    putInt("device_${key}_connection_count", deviceConnectionCounts[key] ?: 0)
                }

                apply()
            }

            android.util.Log.d(
                "UsbController",
                "[DEBUG_LOG] Multi-device state saved: ${connectedSupportedDevices.size} devices"
            )
        } catch (e: Exception) {
            android.util.Log.e("UsbController", "[DEBUG_LOG] Failed to save multi-device state: ${e.message}")
        }
    }

    fun restoreMultiDeviceState(context: Context) {
        try {
            val prefs = context.getSharedPreferences(USB_PREFS_NAME, Context.MODE_PRIVATE)
            val deviceKeys = prefs.getStringSet("connected_device_keys", emptySet()) ?: emptySet()

            android.util.Log.d(
                "UsbController",
                "[DEBUG_LOG] Restoring multi-device state for ${deviceKeys.size} devices"
            )

            connectedSupportedDevices.clear()
            deviceConnectionTimes.clear()

            deviceKeys.forEach { key ->
                val connectionCount = prefs.getInt("device_${key}_connection_count", 0)
                val lastConnectionTime = prefs.getLong("device_${key}_connected_time", 0L)

                if (connectionCount > 0) {
                    deviceConnectionCounts[key] = connectionCount
                }
                if (lastConnectionTime > 0L) {
                    deviceConnectionTimes[key] = lastConnectionTime
                }
            }

        } catch (e: Exception) {
            android.util.Log.e("UsbController", "[DEBUG_LOG] Failed to restore multi-device state: ${e.message}")
        }
    }

    fun getMultiDeviceStatusSummary(context: Context): String {
        val currentDevices = connectedSupportedDevices.size
        val lastDeviceInfo = getLastConnectedDeviceInfo(context)
        val totalConnections = getConnectionCount(context)

        return buildString {
            append("Multi-Device USB Status Summary:\n")
            append("- Currently connected TOPDON devices: $currentDevices\n")
            append("- Last connected device: $lastDeviceInfo\n")
            append("- Total historical connections: $totalConnections\n")

            if (connectedSupportedDevices.isNotEmpty()) {
                append("- Currently connected devices:\n")
                connectedSupportedDevices.forEach { (key, device) ->
                    val connectionTime = deviceConnectionTimes[key]
                    val connectionCount = deviceConnectionCounts[key] ?: 0
                    val timeStr = if (connectionTime != null) {
                        val format = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        format.format(java.util.Date(connectionTime))
                    } else "Unknown"

                    append("  • ${device.deviceName} (Key: $key)\n")
                    append(
                        "    VID: 0x${String.format("%04X", device.vendorId)}, PID: 0x${
                            String.format(
                                "%04X",
                                device.productId
                            )
                        }\n"
                    )
                    append("    Connected at: $timeStr, Total connections: $connectionCount\n")
                }
            }

            if (deviceConnectionCounts.isNotEmpty()) {
                append("- Historical device connections:\n")
                deviceConnectionCounts.forEach { (key, count) ->
                    if (!connectedSupportedDevices.containsKey(key)) {
                        append("  • Device $key: $count connections\n")
                    }
                }
            }
        }
    }

    fun getDevicePriorityAssessments(): List<UsbDevicePrioritizer.DevicePriorityAssessment> {
        return connectedSupportedDevices.map { (deviceKey, device) ->
            val connectionTime = deviceConnectionTimes[deviceKey]
            val connectionCount = deviceConnectionCounts[deviceKey] ?: 0
            devicePrioritizer.assessDevicePriority(deviceKey, device, connectionTime, connectionCount)
        }
    }

    fun getOptimizedDeviceSelection(maxDevices: Int = 3): UsbDevicePrioritizer.DeviceSelectionResult {
        val assessments = getDevicePriorityAssessments()
        return devicePrioritizer.optimizeDeviceSelection(assessments, maxDevices)
    }

    fun getPerformanceAnalyticsReport(context: Context): UsbPerformanceAnalytics.PerformanceReport {
        return performanceAnalytics.generatePerformanceReport(context)
    }

    fun monitorDeviceConnectionQuality(deviceKey: String): String {
        return performanceAnalytics.monitorConnectionQuality(deviceKey)
    }

    fun generateDevicePriorityReport(): String {
        val assessments = getDevicePriorityAssessments()
        return devicePrioritizer.generatePriorityAnalysisReport(assessments)
    }

    fun updateDevicePerformanceFeedback(
        deviceKey: String,
        performanceScore: Double,
        actualReliability: Double,
        resourceUsage: Double
    ) {
        devicePrioritizer.updateDevicePriorityFeedback(
            deviceKey, performanceScore, actualReliability, resourceUsage
        )
    }

    fun getResourceUtilizationMetrics(): Map<String, Double> {
        return performanceAnalytics.getResourceUtilization()
    }

    fun resetPerformanceAnalytics() {
        performanceAnalytics.resetAnalytics()
    }

    fun getComprehensiveSystemStatus(context: Context): String {
        val basicStatus = getMultiDeviceStatusSummary(context)
        val performanceReport = getPerformanceAnalyticsReport(context)
        val priorityReport = generateDevicePriorityReport()
        val selectionResult = getOptimizedDeviceSelection()

        return buildString {
            append("complete USB Controller System Analysis\n")
            append("═══════════════════════════════════════════════\n\n")

            append("MULTI-DEVICE STATUS:\n")
            append(basicStatus)
            append("\n\n")

            append("PERFORMANCE ANALYTICS:\n")
            append("• Total Events Processed: ${performanceReport.totalEvents}\n")
            append("• Average Response Time: ${"%.2f".format(performanceReport.averageResponseTime)}ms\n")
            append("• 95th Percentile Response: ${performanceReport.percentile95ResponseTime}ms\n")
            append("• CPU Efficiency Score: ${"%.3f".format(performanceReport.cpuEfficiencyScore)}\n")
            append("• Memory Utilisation: ${performanceReport.memoryUtilization / 1024}KB\n")
            append("• Event Throughput: ${"%.2f".format(performanceReport.eventThroughput)} events/sec\n\n")

            append("DEVICE PRIORITIZATION:\n")
            append("• Primary Device: ${selectionResult.primaryDevice?.deviceKey ?: "None"}\n")
            append("• Secondary Devices: ${selectionResult.secondaryDevices.size}\n")
            append("• Selection Quality Score: ${"%.3f".format(selectionResult.optimizationMetrics.totalQualityScore)}\n")
            append("• Expected Reliability: ${"%.3f".format(selectionResult.optimizationMetrics.expectedReliability)}\n")
            append("• Resource Efficiency: ${"%.3f".format(selectionResult.optimizationMetrics.resourceEfficiency)}\n\n")

            append("SYSTEM RECOMMENDATIONS:\n")
            performanceReport.systemRecommendations.forEach { recommendation ->
                append("• $recommendation\n")
            }
        }
    }

    private fun saveDeviceProfiles(context: Context) {
        try {
            val prefs = context.getSharedPreferences(USB_PREFS_NAME, Context.MODE_PRIVATE)
            val profilesJson = JSONArray()

            deviceProfiles.values.forEach { profile ->
                val profileJson = JSONObject().apply {
                    put("deviceId", profile.deviceId)
                    put("vendorId", profile.vendorId)
                    put("productId", profile.productId)
                    put("deviceName", profile.deviceName)
                    put("settings", JSONObject(profile.settings))
                    put("calibrationData", JSONObject(profile.calibrationData))
                    put("lastUsed", profile.lastUsed)
                    put("priority", profile.priority)
                }
                profilesJson.put(profileJson)
            }

            prefs.edit().putString(PREF_DEVICE_PROFILES, profilesJson.toString()).apply()
            android.util.Log.d("UsbController", "[DEBUG_LOG] Device profiles saved: ${deviceProfiles.size}")
        } catch (e: Exception) {
            android.util.Log.e("UsbController", "[DEBUG_LOG] Failed to save device profiles: ${e.message}")
        }
    }

    private fun restoreDeviceProfiles(context: Context) {
        try {
            val prefs = context.getSharedPreferences(USB_PREFS_NAME, Context.MODE_PRIVATE)
            val profilesJsonString = prefs.getString(PREF_DEVICE_PROFILES, null) ?: return

            val profilesJson = JSONArray(profilesJsonString)
            deviceProfiles.clear()

            for (i in 0 until profilesJson.length()) {
                val profileJson = profilesJson.getJSONObject(i)

                val settings = mutableMapOf<String, Any>()
                val settingsJson = profileJson.getJSONObject("settings")
                settingsJson.keys().forEach { key ->
                    settings[key] = settingsJson.get(key)
                }

                val calibrationData = mutableMapOf<String, Any>()
                val calibrationJson = profileJson.getJSONObject("calibrationData")
                calibrationJson.keys().forEach { key ->
                    calibrationData[key] = calibrationJson.get(key)
                }

                val profile = DeviceProfile(
                    deviceId = profileJson.getString("deviceId"),
                    vendorId = profileJson.getInt("vendorId"),
                    productId = profileJson.getInt("productId"),
                    deviceName = profileJson.getString("deviceName"),
                    settings = settings,
                    calibrationData = calibrationData,
                    lastUsed = profileJson.getLong("lastUsed"),
                    priority = profileJson.getInt("priority")
                )

                deviceProfiles[profile.deviceId] = profile
            }

            android.util.Log.d("UsbController", "[DEBUG_LOG] Device profiles restored: ${deviceProfiles.size}")
        } catch (e: Exception) {
            android.util.Log.e("UsbController", "[DEBUG_LOG] Failed to restore device profiles: ${e.message}")
        }
    }

    fun setDevicePriority(deviceId: String, priority: Int) {
        deviceProfiles[deviceId]?.let { profile ->
            deviceProfiles[deviceId] = profile.copy(priority = priority, lastUsed = System.currentTimeMillis())
            callback?.getContext()?.let { context -> saveDeviceProfiles(context) }

            android.util.Log.d("UsbController", "[DEBUG_LOG] Device priority set: $deviceId -> $priority")
        }
    }

    fun getPriorityDeviceForRecording(): UsbDevice? {
        val prioritizedDevices = connectedSupportedDevices.values
            .sortedByDescending { device ->
                val deviceId = "${device.vendorId}-${device.productId}"
                deviceProfiles[deviceId]?.priority ?: 0
            }

        return prioritizedDevices.firstOrNull()?.also { device ->
            val deviceId = "${device.vendorId}-${device.productId}"
            android.util.Log.d("UsbController", "[DEBUG_LOG] Priority device selected: $deviceId")
        }
    }

    fun setHotSwapDetectionEnabled(enabled: Boolean) {
        hotSwapDetectionEnabled = enabled
        android.util.Log.d("UsbController", "[DEBUG_LOG] Hot-swap detection: $enabled")
    }

    private fun handleDeviceHotSwap(removedDevice: UsbDevice, newDevice: UsbDevice?) {
        if (!hotSwapDetectionEnabled) return

        val removedDeviceId = "${removedDevice.vendorId}-${removedDevice.productId}"
        android.util.Log.d("UsbController", "[DEBUG_LOG] Hot-swap detected: $removedDeviceId removed")

        if (newDevice != null) {
            val newDeviceId = "${newDevice.vendorId}-${newDevice.productId}"
            android.util.Log.d("UsbController", "[DEBUG_LOG] Hot-swap replacement: $newDeviceId connected")

            if (removedDevice.vendorId == newDevice.vendorId && removedDevice.productId == newDevice.productId) {
                calibrationStates[removedDeviceId]?.let { calibration ->
                    calibrationStates[newDeviceId] = calibration
                    android.util.Log.d(
                        "UsbController",
                        "[DEBUG_LOG] Calibration state transferred to replacement device"
                    )
                }
            }

            callback?.showToast("Device replaced: ${newDevice.deviceName}", android.widget.Toast.LENGTH_SHORT)
        } else {
            callback?.showToast("Device removed: ${removedDevice.deviceName}", android.widget.Toast.LENGTH_SHORT)
        }
    }

    fun setDeviceFilter(filter: DeviceFilter) {
        deviceFilter = filter
        android.util.Log.d(
            "UsbController",
            "[DEBUG_LOG] Device filter updated: ${filter.vendorIds.size} vendors, ${filter.productIds.size} products"
        )

        val filteredDevices = connectedSupportedDevices.filter { (deviceId, device) ->
            applyDeviceFilter(device)
        }

        android.util.Log.d(
            "UsbController",
            "[DEBUG_LOG] Filtered devices: ${filteredDevices.size}/${connectedSupportedDevices.size}"
        )
    }

    private fun applyDeviceFilter(device: UsbDevice): Boolean {
        val filter = deviceFilter ?: return true

        if (filter.vendorIds.isNotEmpty() && device.vendorId !in filter.vendorIds) {
            return false
        }

        if (filter.productIds.isNotEmpty() && device.productId !in filter.productIds) {
            return false
        }

        if (filter.deviceNames.isNotEmpty() && device.deviceName !in filter.deviceNames) {
            return false
        }

        if (filter.requireCalibration) {
            val deviceId = "${device.vendorId}-${device.productId}"
            if (calibrationStates[deviceId] == null) {
                return false
            }
        }

        if (filter.minPriority > 0) {
            val deviceId = "${device.vendorId}-${device.productId}"
            val priority = deviceProfiles[deviceId]?.priority ?: 0
            if (priority < filter.minPriority) {
                return false
            }
        }

        return true
    }

    fun saveDeviceCalibrationState(deviceId: String, calibrationData: Map<String, Any>) {
        calibrationStates[deviceId] = calibrationData

        deviceProfiles[deviceId]?.let { profile ->
            deviceProfiles[deviceId] = profile.copy(
                calibrationData = calibrationData,
                lastUsed = System.currentTimeMillis()
            )
        }

        callback?.getContext()?.let { context -> saveDeviceProfiles(context) }
        android.util.Log.d("UsbController", "[DEBUG_LOG] Calibration state saved for device: $deviceId")
    }

    fun getDeviceCalibrationState(deviceId: String): Map<String, Any>? {
        return calibrationStates[deviceId]
    }

    fun reportDeviceStatusToNetwork(deviceId: String, status: String, metrics: Map<String, Any> = emptyMap()) {
        val networkStatus = NetworkDeviceStatus(
            deviceId = deviceId,
            status = status,
            lastUpdate = System.currentTimeMillis(),
            metrics = metrics
        )

        networkStatusReporter[deviceId] = networkStatus

        android.util.Log.d("UsbController", "[DEBUG_LOG] Device status reported: $deviceId -> $status")
    }

    fun getNetworkDeviceStatusReports(): Map<String, NetworkDeviceStatus> {
        return networkStatusReporter.toMap()
    }

    fun initializeEnhancedDeviceManagement(context: Context) {
        restoreDeviceProfiles(context)
        setHotSwapDetectionEnabled(true)

        android.util.Log.d("UsbController", "[DEBUG_LOG] Enhanced device management initialized")
    }
}
