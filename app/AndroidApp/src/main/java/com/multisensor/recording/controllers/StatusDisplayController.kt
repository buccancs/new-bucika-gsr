package com.multisensor.recording.controllers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusDisplayController @Inject constructor() {

    companion object {
        private const val STATUS_PREFS_NAME = "status_display_prefs"
        private const val PREF_STATUS_STATE = "status_state"
        private const val PREF_CUSTOM_INDICATORS = "custom_indicators"
        private const val PREF_THEME_CONFIG = "theme_config"
        private const val PREF_UPDATE_INTERVALS = "update_intervals"
        private const val DEFAULT_UPDATE_INTERVAL = 5000L
    }

    data class StatusDisplayState(
        val isMonitoringActive: Boolean,
        val lastBatteryLevel: Int,
        val connectionStates: Map<String, Boolean>,
        val customMetrics: Map<String, Any>,
        val updateInterval: Long = DEFAULT_UPDATE_INTERVAL,
        val themeConfig: StatusThemeConfig
    )

    data class CustomStatusIndicator(
        val id: String,
        val displayName: String,
        val updateInterval: Long,
        val valueProvider: () -> Any,
        val colorProvider: (Any) -> Int = { Color.GRAY },
        val isEnabled: Boolean = true
    )

    data class StatusThemeConfig(
        val primaryColor: Int = Color.parseColor("#2196F3"),
        val errorColor: Int = Color.parseColor("#F44336"),
        val warningColor: Int = Color.parseColor("#FF9800"),
        val successColor: Int = Color.parseColor("#4CAF50"),
        val backgroundColor: Int = Color.parseColor("#FFFFFF"),
        val textColor: Int = Color.parseColor("#000000")
    )

    interface StatusDisplayCallback {
        fun onBatteryLevelChanged(level: Int, color: Int)
        fun onConnectionStatusChanged(type: ConnectionType, connected: Boolean)
        fun onStatusMonitoringInitialized()
        fun onStatusMonitoringError(message: String)
        fun updateStatusText(text: String)
        fun runOnUiThread(action: () -> Unit)
        fun registerBroadcastReceiver(receiver: BroadcastReceiver, filter: IntentFilter): Intent?
        fun unregisterBroadcastReceiver(receiver: BroadcastReceiver)
        fun getBatteryLevelText(): TextView?
        fun getPcConnectionStatus(): TextView?
        fun getPcConnectionIndicator(): View?
        fun getShimmerConnectionStatus(): TextView?
        fun getShimmerConnectionIndicator(): View?
        fun getThermalConnectionStatus(): TextView?
        fun getThermalConnectionIndicator(): View?
    }

    enum class ConnectionType {
        PC, SHIMMER, THERMAL
    }

    private var callback: StatusDisplayCallback? = null
    private var currentBatteryLevel = -1
    private var isPcConnected = false
    private var isShimmerConnected = false
    private var isThermalConnected = false
    private lateinit var statusUpdateHandler: Handler

    private var currentStatusState: StatusDisplayState? = null
    private val customIndicators = mutableMapOf<String, CustomStatusIndicator>()
    private var statusThemeConfig = StatusThemeConfig()
    private var statusUpdateInterval = DEFAULT_UPDATE_INTERVAL
    private val customMetrics = mutableMapOf<String, Any>()
    private var isMonitoringActive = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

                if (level != -1 && scale != -1) {
                    currentBatteryLevel = (level * 100) / scale
                    updateBatteryDisplay()
                }
            }
        }
    }

    fun setCallback(callback: StatusDisplayCallback) {
        this.callback = callback
    }

    fun initializeStatusMonitoring() {
        android.util.Log.d("StatusDisplayController", "[DEBUG_LOG] Initializing status monitoring system")

        try {
            statusUpdateHandler = Handler(Looper.getMainLooper())

            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryIntent = callback?.registerBroadcastReceiver(batteryReceiver, batteryFilter)

            batteryIntent?.let { intent ->
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    currentBatteryLevel = (level * 100) / scale
                    updateBatteryDisplay()
                }
            }

            updatePcConnectionStatus(false)
            updateSensorConnectionStatus(false, false)

            startPeriodicStatusUpdates()

            callback?.onStatusMonitoringInitialized()
            android.util.Log.d(
                "StatusDisplayController",
                "[DEBUG_LOG] Status monitoring system initialized successfully"
            )
        } catch (e: Exception) {
            android.util.Log.e(
                "StatusDisplayController",
                "[DEBUG_LOG] Failed to initialize status monitoring: ${e.message}"
            )
            callback?.onStatusMonitoringError("Failed to initialize status monitoring: ${e.message}")
        }
    }

    private fun updateBatteryDisplay() {
        callback?.runOnUiThread {
            if (currentBatteryLevel >= 0) {
                val batteryText = "Battery: $currentBatteryLevel%"

                val textColor = when {
                    currentBatteryLevel > 50 -> Color.GREEN
                    currentBatteryLevel > 20 -> Color.YELLOW
                    else -> Color.RED
                }

                callback?.getBatteryLevelText()?.let { textView ->
                    textView.text = batteryText
                    textView.setTextColor(textColor)
                }

                callback?.onBatteryLevelChanged(currentBatteryLevel, textColor)
            } else {
                callback?.getBatteryLevelText()?.let { textView ->
                    textView.text = "Battery: ---%"
                    textView.setTextColor(Color.WHITE)
                }

                callback?.onBatteryLevelChanged(-1, Color.WHITE)
            }
        }
    }

    fun updatePcConnectionStatus(connected: Boolean) {
        android.util.Log.d("StatusDisplayController", "[DEBUG_LOG] Updating PC connection status: $connected")

        isPcConnected = connected
        callback?.runOnUiThread {
            val statusText = if (connected) "PC: Connected" else "PC: Waiting for PC..."
            val indicatorColor = if (connected) Color.GREEN else Color.RED

            callback?.getPcConnectionStatus()?.text = statusText
            callback?.getPcConnectionIndicator()?.setBackgroundColor(indicatorColor)

            callback?.onConnectionStatusChanged(ConnectionType.PC, connected)
        }
    }

    fun updateSensorConnectionStatus(shimmerConnected: Boolean, thermalConnected: Boolean) {
        android.util.Log.d(
            "StatusDisplayController",
            "[DEBUG_LOG] Updating sensor connection status: Shimmer=$shimmerConnected, Thermal=$thermalConnected"
        )

        isShimmerConnected = shimmerConnected
        isThermalConnected = thermalConnected

        callback?.runOnUiThread {
            val shimmerStatusText = if (shimmerConnected) "Shimmer: Connected" else "Shimmer: Disconnected"
            val shimmerIndicatorColor = if (shimmerConnected) Color.GREEN else Color.RED

            callback?.getShimmerConnectionStatus()?.text = shimmerStatusText
            callback?.getShimmerConnectionIndicator()?.setBackgroundColor(shimmerIndicatorColor)

            val thermalStatusText = if (thermalConnected) "Thermal: Connected" else "Thermal: Disconnected"
            val thermalIndicatorColor = if (thermalConnected) Color.GREEN else Color.RED

            callback?.getThermalConnectionStatus()?.text = thermalStatusText
            callback?.getThermalConnectionIndicator()?.setBackgroundColor(thermalIndicatorColor)

            callback?.onConnectionStatusChanged(ConnectionType.SHIMMER, shimmerConnected)
            callback?.onConnectionStatusChanged(ConnectionType.THERMAL, thermalConnected)
        }
    }

    private fun startPeriodicStatusUpdates() {
        val updateRunnable = object : Runnable {
            override fun run() {

                statusUpdateHandler.postDelayed(this, 5000)
            }
        }
        statusUpdateHandler.post(updateRunnable)
        isMonitoringActive = true

        android.util.Log.d("StatusDisplayController", "[DEBUG_LOG] Periodic status updates started")
    }

    private fun stopPeriodicStatusUpdates() {
        if (::statusUpdateHandler.isInitialized) {
            statusUpdateHandler.removeCallbacksAndMessages(null)
        }
        isMonitoringActive = false

        android.util.Log.d("StatusDisplayController", "[DEBUG_LOG] Periodic status updates stopped")
    }

    fun updateShimmerConnectionStatus(connected: Boolean) {
        isShimmerConnected = connected
        updateSensorConnectionStatus(isShimmerConnected, isThermalConnected)
    }

    fun updateThermalConnectionStatus(connected: Boolean) {
        isThermalConnected = connected
        updateSensorConnectionStatus(isShimmerConnected, isThermalConnected)
    }

    fun getStatusSummary(): String {
        return buildString {
            append("Status Display System Summary:\n")
            append("- Battery Level: ${if (currentBatteryLevel >= 0) "$currentBatteryLevel%" else "Unknown"}\n")
            append("- PC Connected: $isPcConnected\n")
            append("- Shimmer Connected: $isShimmerConnected\n")
            append("- Thermal Connected: $isThermalConnected\n")
            append("- Status Monitoring: ${if (::statusUpdateHandler.isInitialized) "Active" else "Inactive"}")
        }
    }

    fun resetState() {
        currentBatteryLevel = -1
        isPcConnected = false
        isShimmerConnected = false
        isThermalConnected = false
        android.util.Log.d("StatusDisplayController", "[DEBUG_LOG] Status display controller state reset")
    }

    fun cleanup() {
        try {
            callback?.unregisterBroadcastReceiver(batteryReceiver)

            if (::statusUpdateHandler.isInitialized) {
                statusUpdateHandler.removeCallbacksAndMessages(null)
            }

            android.util.Log.d("StatusDisplayController", "[DEBUG_LOG] Status display controller resources cleaned up")
        } catch (e: Exception) {
            android.util.Log.w("StatusDisplayController", "[DEBUG_LOG] Error during cleanup: ${e.message}")
        }
    }

    fun getCurrentBatteryLevel(): Int = currentBatteryLevel

    fun getConnectionStatus(type: ConnectionType): Boolean {
        return when (type) {
            ConnectionType.PC -> isPcConnected
            ConnectionType.SHIMMER -> isShimmerConnected
            ConnectionType.THERMAL -> isThermalConnected
        }
    }

    private fun saveStatusState(context: Context) {
        try {
            val prefs = context.getSharedPreferences(STATUS_PREFS_NAME, Context.MODE_PRIVATE)

            val statusState = StatusDisplayState(
                isMonitoringActive = isMonitoringActive,
                lastBatteryLevel = currentBatteryLevel,
                connectionStates = mapOf(
                    "pc" to isPcConnected,
                    "shimmer" to isShimmerConnected,
                    "thermal" to isThermalConnected
                ),
                customMetrics = customMetrics.toMap(),
                updateInterval = statusUpdateInterval,
                themeConfig = statusThemeConfig
            )

            val stateJson = JSONObject().apply {
                put("isMonitoringActive", statusState.isMonitoringActive)
                put("lastBatteryLevel", statusState.lastBatteryLevel)
                put("connectionStates", JSONObject(statusState.connectionStates))
                put("customMetrics", JSONObject(statusState.customMetrics))
                put("updateInterval", statusState.updateInterval)
                put("themeConfig", JSONObject().apply {
                    put("primaryColor", statusState.themeConfig.primaryColor)
                    put("errorColor", statusState.themeConfig.errorColor)
                    put("warningColor", statusState.themeConfig.warningColor)
                    put("successColor", statusState.themeConfig.successColor)
                    put("backgroundColor", statusState.themeConfig.backgroundColor)
                    put("textColor", statusState.themeConfig.textColor)
                })
            }

            prefs.edit().putString(PREF_STATUS_STATE, stateJson.toString()).apply()
            currentStatusState = statusState

            android.util.Log.d("StatusDisplayController", "[DEBUG_LOG] Status state saved")
        } catch (e: Exception) {
            android.util.Log.e("StatusDisplayController", "[DEBUG_LOG] Failed to save status state: ${e.message}")
        }
    }

    private fun restoreStatusState(context: Context) {
        try {
            val prefs = context.getSharedPreferences(STATUS_PREFS_NAME, Context.MODE_PRIVATE)
            val stateJson = prefs.getString(PREF_STATUS_STATE, null) ?: return

            val jsonObject = JSONObject(stateJson)

            currentBatteryLevel = jsonObject.getInt("lastBatteryLevel")
            statusUpdateInterval = jsonObject.getLong("updateInterval")

            val connectionStates = jsonObject.getJSONObject("connectionStates")
            isPcConnected = connectionStates.getBoolean("pc")
            isShimmerConnected = connectionStates.getBoolean("shimmer")
            isThermalConnected = connectionStates.getBoolean("thermal")

            val customMetricsJson = jsonObject.getJSONObject("customMetrics")
            customMetrics.clear()
            customMetricsJson.keys().forEach { key ->
                customMetrics[key] = customMetricsJson.get(key)
            }

            val themeConfigJson = jsonObject.getJSONObject("themeConfig")
            statusThemeConfig = StatusThemeConfig(
                primaryColor = themeConfigJson.getInt("primaryColor"),
                errorColor = themeConfigJson.getInt("errorColor"),
                warningColor = themeConfigJson.getInt("warningColor"),
                successColor = themeConfigJson.getInt("successColor"),
                backgroundColor = themeConfigJson.getInt("backgroundColor"),
                textColor = themeConfigJson.getInt("textColor")
            )

            applyThemeConfig()

            android.util.Log.d("StatusDisplayController", "[DEBUG_LOG] Status state restored")
        } catch (e: Exception) {
            android.util.Log.e("StatusDisplayController", "[DEBUG_LOG] Failed to restore status state: ${e.message}")
        }
    }

    fun addCustomStatusIndicator(indicator: CustomStatusIndicator) {
        customIndicators[indicator.id] = indicator
        android.util.Log.d("StatusDisplayController", "[DEBUG_LOG] Custom indicator added: ${indicator.id}")
    }

    fun removeCustomStatusIndicator(indicatorId: String) {
        customIndicators.remove(indicatorId)?.let {
            android.util.Log.d("StatusDisplayController", "[DEBUG_LOG] Custom indicator removed: $indicatorId")
        }
    }

    fun updateCustomMetric(key: String, value: Any) {
        customMetrics[key] = value
        android.util.Log.d("StatusDisplayController", "[DEBUG_LOG] Custom metric updated: $key = $value")
    }

    fun setStatusThemeConfig(themeConfig: StatusThemeConfig) {
        statusThemeConfig = themeConfig
        applyThemeConfig()

        android.util.Log.d("StatusDisplayController", "[DEBUG_LOG] Status theme config updated")
    }

    private fun applyThemeConfig() {
        callback?.runOnUiThread {
            callback?.getBatteryLevelText()?.setTextColor(statusThemeConfig.textColor)

            callback?.getPcConnectionStatus()?.setTextColor(statusThemeConfig.textColor)
            callback?.getShimmerConnectionStatus()?.setTextColor(statusThemeConfig.textColor)

            updateConnectionIndicatorColor(ConnectionType.PC, isPcConnected)
            updateConnectionIndicatorColor(ConnectionType.SHIMMER, isShimmerConnected)
            updateConnectionIndicatorColor(ConnectionType.THERMAL, isThermalConnected)
        }
    }

    private fun updateConnectionIndicatorColor(type: ConnectionType, connected: Boolean) {
        val color = if (connected) statusThemeConfig.successColor else statusThemeConfig.errorColor

        when (type) {
            ConnectionType.PC -> callback?.getPcConnectionIndicator()?.setBackgroundColor(color)
            ConnectionType.SHIMMER -> callback?.getShimmerConnectionIndicator()?.setBackgroundColor(color)
            ConnectionType.THERMAL -> callback?.getThermalConnectionIndicator()?.setBackgroundColor(color)
        }
    }

    fun setStatusUpdateInterval(intervalMs: Long) {
        statusUpdateInterval = intervalMs.coerceAtLeast(1000L)

        if (isMonitoringActive) {
            stopPeriodicStatusUpdates()
            startPeriodicStatusUpdates()
        }

        android.util.Log.d(
            "StatusDisplayController",
            "[DEBUG_LOG] Status update interval set to: ${statusUpdateInterval}ms"
        )
    }

    fun getCustomIndicators(): Map<String, CustomStatusIndicator> = customIndicators.toMap()

    fun getCustomMetrics(): Map<String, Any> = customMetrics.toMap()

    fun getCurrentThemeConfig(): StatusThemeConfig = statusThemeConfig

    fun initializeEnhancedStatusDisplay(context: Context) {
        restoreStatusState(context)

        addCustomStatusIndicator(
            CustomStatusIndicator(
                id = "system_memory",
                displayName = "Memory Usage",
                updateInterval = 10000L,
                valueProvider = { Runtime.getRuntime().let { (it.totalMemory() - it.freeMemory()) / 1024 / 1024 } },
                colorProvider = { value ->
                    val memoryMB = value as Long
                    when {
                        memoryMB < 100 -> statusThemeConfig.successColor
                        memoryMB < 200 -> statusThemeConfig.warningColor
                        else -> statusThemeConfig.errorColor
                    }
                }
            ))

        addCustomStatusIndicator(
            CustomStatusIndicator(
                id = "system_uptime",
                displayName = "App Uptime",
                updateInterval = 60000L,
                valueProvider = { (System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()) / 1000 / 60 }
            ))

        android.util.Log.d("StatusDisplayController", "[DEBUG_LOG] Enhanced status display initialized")
    }
}
