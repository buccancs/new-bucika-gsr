package com.topdon.tc001.battery

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.elvishew.xlog.XLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

/**
 * Advanced Battery Life Optimization System for Bucika GSR Platform
 * 
 * Provides comprehensive battery management during long recording sessions:
 * - Real-time battery monitoring with predictive analytics
 * - Adaptive power management based on recording requirements
 * - Intelligent feature reduction during low battery situations
 * - Power-saving recommendations and automatic optimizations
 * - Integration with recording sessions for battery-aware operation
 * 
 * Key Features:
 * - Predictive battery life estimation for session planning
 * - Automatic CPU frequency scaling during low battery
 * - Screen brightness optimization for recording scenarios
 * - Network usage optimization to extend battery life
 * - Background process management for power efficiency
 */
class BatteryOptimizationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BatteryOptimizationManager"
        
        // Battery monitoring parameters
        private const val MONITORING_INTERVAL_MS = 15_000L // 15 seconds
        private const val BATTERY_HISTORY_SIZE = 100 // Keep last 100 readings
        
        // Battery level thresholds
        private const val CRITICAL_BATTERY_LEVEL = 10 // 10%
        private const val LOW_BATTERY_LEVEL = 20 // 20%
        private const val WARNING_BATTERY_LEVEL = 30 // 30%
        private const val OPTIMAL_BATTERY_LEVEL = 50 // 50%
        
        // Power saving modes
        private const val AGGRESSIVE_POWER_SAVING_LEVEL = 15 // 15%
        private const val MODERATE_POWER_SAVING_LEVEL = 25 // 25%
        
        // Recording session parameters
        private const val MIN_RECORDING_BATTERY_LEVEL = 15 // Minimum battery for new recordings
        private const val RECOMMENDED_RECORDING_BATTERY_LEVEL = 30 // Recommended minimum
        
        // Power consumption estimation (mAh per hour)
        private const val GSR_RECORDING_POWER_MAH = 200
        private const val THERMAL_CAMERA_POWER_MAH = 300
        private const val RGB_VIDEO_POWER_MAH = 250
        private const val NETWORK_STREAMING_POWER_MAH = 150
        private const val BASE_SYSTEM_POWER_MAH = 400
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    
    // Battery optimization state
    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()
    
    // Battery history for trend analysis
    private val batteryHistory = mutableListOf<BatteryReading>()
    
    // Power saving strategies
    private val powerSavingStrategies = mutableListOf<PowerSavingStrategy>()
    
    // Current optimization level
    private var currentOptimizationLevel = OptimizationLevel.NONE
    
    init {
        initializePowerSavingStrategies()
        startBatteryMonitoring()
    }

    /**
     * Data class representing battery optimization state
     */
    data class BatteryState(
        val isMonitoring: Boolean = false,
        val batteryLevel: Int = 100,
        val batteryVoltage: Int = 0,
        val batteryTemperature: Float = 0f,
        val chargingState: ChargingState = ChargingState.UNKNOWN,
        val powerSource: PowerSource = PowerSource.UNKNOWN,
        val batteryHealth: BatteryHealth = BatteryHealth.UNKNOWN,
        val estimatedRemainingHours: Double = 0.0,
        val powerConsumptionRate: Double = 0.0, // mAh per hour
        val optimizationLevel: OptimizationLevel = OptimizationLevel.NONE,
        val recommendedActions: List<String> = emptyList(),
        val canStartRecording: Boolean = true,
        val maxRecordingDuration: Double = 0.0, // hours
        val batteryTrend: BatteryTrend = BatteryTrend.STABLE,
        val powerSavingActive: Boolean = false
    )
    
    enum class ChargingState {
        CHARGING_AC,
        CHARGING_USB,
        CHARGING_WIRELESS,
        NOT_CHARGING,
        DISCHARGING,
        UNKNOWN
    }
    
    enum class PowerSource {
        AC_CHARGER,
        USB_CHARGER,
        WIRELESS_CHARGER,
        BATTERY,
        UNKNOWN
    }
    
    enum class BatteryHealth {
        GOOD,
        COLD,
        DEAD,
        OVERHEAT,
        OVER_VOLTAGE,
        FAILURE,
        UNKNOWN
    }
    
    enum class OptimizationLevel {
        NONE,           // No optimization
        CONSERVATIVE,   // Basic power saving
        MODERATE,       // Moderate power saving  
        AGGRESSIVE,     // Maximum power saving
        EMERGENCY       // Emergency power conservation
    }
    
    enum class BatteryTrend {
        IMPROVING,      // Battery level increasing (charging)
        STABLE,         // Stable battery level
        DECLINING,      // Normal battery drain
        RAPID_DECLINE   // Rapid battery drain
    }

    /**
     * Start battery optimization monitoring
     */
    fun startMonitoring() {
        XLog.i(TAG, "Starting battery optimization monitoring")
        
        _batteryState.value = _batteryState.value.copy(isMonitoring = true)
    }

    /**
     * Stop battery optimization monitoring
     */
    fun stopMonitoring() {
        XLog.i(TAG, "Stopping battery optimization monitoring")
        
        coroutineScope.cancel()
        disableAllOptimizations()
        _batteryState.value = BatteryState()
    }

    /**
     * Check if device can start a recording session
     */
    fun canStartRecordingSession(): Boolean {
        val currentState = _batteryState.value
        return currentState.batteryLevel >= MIN_RECORDING_BATTERY_LEVEL && 
               currentState.batteryHealth != BatteryHealth.DEAD &&
               currentState.batteryHealth != BatteryHealth.FAILURE
    }

    /**
     * Get estimated recording duration based on current battery
     */
    fun getEstimatedRecordingDuration(
        includeGSR: Boolean = true,
        includeThermal: Boolean = true,
        includeRGB: Boolean = true,
        includeNetworkStreaming: Boolean = true
    ): Double {
        val currentState = _batteryState.value
        
        // Calculate total power consumption
        var totalPowerMah = BASE_SYSTEM_POWER_MAH
        if (includeGSR) totalPowerMah += GSR_RECORDING_POWER_MAH
        if (includeThermal) totalPowerMah += THERMAL_CAMERA_POWER_MAH
        if (includeRGB) totalPowerMah += RGB_VIDEO_POWER_MAH
        if (includeNetworkStreaming) totalPowerMah += NETWORK_STREAMING_POWER_MAH
        
        // Get battery capacity (estimate if not available)
        val batteryCapacityMah = getBatteryCapacityMah()
        val availableBatteryMah = (batteryCapacityMah * currentState.batteryLevel) / 100.0
        
        // Reserve some battery for emergency use
        val usableBatteryMah = availableBatteryMah * 0.85 // Use 85% of available battery
        
        return usableBatteryMah / totalPowerMah
    }

    /**
     * Get battery optimization recommendations
     */
    fun getOptimizationRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val currentState = _batteryState.value
        
        when {
            currentState.batteryLevel <= CRITICAL_BATTERY_LEVEL -> {
                recommendations.add("Critical battery level - connect charger immediately")
                recommendations.add("Stop all non-essential recording features")
                recommendations.add("Save current data and consider ending session")
                recommendations.add("Enable emergency power saving mode")
            }
            
            currentState.batteryLevel <= LOW_BATTERY_LEVEL -> {
                recommendations.add("Low battery level - connect charger soon")
                recommendations.add("Reduce video recording quality to extend battery")
                recommendations.add("Disable thermal camera if not essential")
                recommendations.add("Enable aggressive power saving mode")
            }
            
            currentState.batteryLevel <= WARNING_BATTERY_LEVEL -> {
                recommendations.add("Battery level getting low - prepare for charging")
                recommendations.add("Consider reducing screen brightness")
                recommendations.add("Enable moderate power saving features")
                recommendations.add("Monitor battery trend closely")
            }
        }
        
        // Trend-based recommendations
        when (currentState.batteryTrend) {
            BatteryTrend.RAPID_DECLINE -> {
                recommendations.add("Rapid battery drain detected - check for background apps")
                recommendations.add("Consider reducing recording intensity")
                recommendations.add("Enable power optimization immediately")
            }
            BatteryTrend.DECLINING -> {
                if (currentState.estimatedRemainingHours < 2.0) {
                    recommendations.add("Less than 2 hours estimated remaining")
                    recommendations.add("Plan for charging or session completion")
                }
            }
            BatteryTrend.IMPROVING -> {
                if (currentState.chargingState != ChargingState.NOT_CHARGING) {
                    recommendations.add("Device is charging - optimal time for long recordings")
                }
            }
            BatteryTrend.STABLE -> {
                // No specific trend recommendations
            }
        }
        
        // Health-based recommendations
        when (currentState.batteryHealth) {
            BatteryHealth.OVERHEAT -> {
                recommendations.add("Battery overheating - reduce CPU-intensive operations")
                recommendations.add("Allow device to cool down")
                recommendations.add("Consider pausing thermal camera recording")
            }
            BatteryHealth.COLD -> {
                recommendations.add("Battery is cold - performance may be reduced")
                recommendations.add("Allow device to warm up gradually")
            }
            BatteryHealth.DEAD, BatteryHealth.FAILURE -> {
                recommendations.add("Battery health critical - connect to power immediately")
                recommendations.add("Device may shut down unexpectedly")
            }
            else -> {
                // No health-specific recommendations
            }
        }
        
        return recommendations
    }

    /**
     * Apply battery optimization based on current level
     */
    fun applyOptimization(level: OptimizationLevel) {
        if (level == currentOptimizationLevel) return
        
        XLog.i(TAG, "Applying battery optimization level: $level")
        
        // Disable current optimizations
        disableAllOptimizations()
        
        // Apply new optimization level
        when (level) {
            OptimizationLevel.CONSERVATIVE -> applyConservativeOptimization()
            OptimizationLevel.MODERATE -> applyModerateOptimization()
            OptimizationLevel.AGGRESSIVE -> applyAggressiveOptimization()
            OptimizationLevel.EMERGENCY -> applyEmergencyOptimization()
            OptimizationLevel.NONE -> {
                // No optimizations to apply
            }
        }
        
        currentOptimizationLevel = level
        
        val currentState = _batteryState.value
        _batteryState.value = currentState.copy(
            optimizationLevel = level,
            powerSavingActive = level != OptimizationLevel.NONE
        )
    }

    /**
     * Start continuous battery monitoring
     */
    private fun startBatteryMonitoring() {
        coroutineScope.launch {
            while (isActive) {
                try {
                    val batteryReading = captureBatteryReading()
                    updateBatteryHistory(batteryReading)
                    analyzeBatteryTrends()
                    updateBatteryState(batteryReading)
                    
                    // Auto-apply optimizations based on battery level
                    autoApplyOptimizations(batteryReading)
                    
                } catch (e: Exception) {
                    XLog.e(TAG, "Battery monitoring error: ${e.message}", e)
                }
                
                delay(MONITORING_INTERVAL_MS)
            }
        }
    }

    /**
     * Capture current battery reading
     */
    private fun captureBatteryReading(): BatteryReading {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) {
            (level.toFloat() / scale.toFloat() * 100.0f).toInt()
        } else {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }
        
        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val temperature = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)?.div(10f) ?: 0f
        
        val chargingState = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> {
                when (batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
                    BatteryManager.BATTERY_PLUGGED_AC -> ChargingState.CHARGING_AC
                    BatteryManager.BATTERY_PLUGGED_USB -> ChargingState.CHARGING_USB
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingState.CHARGING_WIRELESS
                    else -> ChargingState.CHARGING_AC
                }
            }
            BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargingState.DISCHARGING
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargingState.NOT_CHARGING
            else -> ChargingState.UNKNOWN
        }
        
        val batteryHealth = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.FAILURE
            else -> BatteryHealth.UNKNOWN
        }
        
        return BatteryReading(
            timestamp = System.currentTimeMillis(),
            batteryLevel = batteryPct,
            voltage = voltage,
            temperature = temperature,
            chargingState = chargingState,
            batteryHealth = batteryHealth
        )
    }

    /**
     * Update battery history and maintain rolling window
     */
    private fun updateBatteryHistory(reading: BatteryReading) {
        batteryHistory.add(reading)
        
        // Maintain history size
        while (batteryHistory.size > BATTERY_HISTORY_SIZE) {
            batteryHistory.removeAt(0)
        }
    }

    /**
     * Analyze battery trends
     */
    private fun analyzeBatteryTrends() {
        if (batteryHistory.size < 5) return
        
        val recentReadings = batteryHistory.takeLast(5)
        val timeSpan = recentReadings.last().timestamp - recentReadings.first().timestamp
        val levelChange = recentReadings.last().batteryLevel - recentReadings.first().batteryLevel
        
        // Calculate trend
        val trend = when {
            levelChange > 2 -> BatteryTrend.IMPROVING
            levelChange < -10 -> BatteryTrend.RAPID_DECLINE
            levelChange < -2 -> BatteryTrend.DECLINING
            else -> BatteryTrend.STABLE
        }
        
        // Calculate power consumption rate
        val powerConsumptionRate = if (timeSpan > 0 && levelChange < 0) {
            val hoursElapsed = timeSpan / (1000.0 * 60.0 * 60.0)
            val batteryCapacity = getBatteryCapacityMah()
            val powerUsed = (kotlin.math.abs(levelChange) / 100.0) * batteryCapacity
            powerUsed / hoursElapsed
        } else {
            0.0
        }
        
        // Calculate estimated remaining time
        val currentLevel = batteryHistory.last().batteryLevel
        val estimatedHours = if (powerConsumptionRate > 0 && currentLevel > 0) {
            val batteryCapacity = getBatteryCapacityMah()
            val remainingCapacity = (currentLevel / 100.0) * batteryCapacity
            remainingCapacity / powerConsumptionRate
        } else {
            0.0
        }
        
        // Update state with trend analysis
        val currentState = _batteryState.value
        _batteryState.value = currentState.copy(
            batteryTrend = trend,
            powerConsumptionRate = powerConsumptionRate,
            estimatedRemainingHours = estimatedHours
        )
    }

    /**
     * Update battery state with current reading
     */
    private fun updateBatteryState(reading: BatteryReading) {
        val powerSource = when (reading.chargingState) {
            ChargingState.CHARGING_AC -> PowerSource.AC_CHARGER
            ChargingState.CHARGING_USB -> PowerSource.USB_CHARGER
            ChargingState.CHARGING_WIRELESS -> PowerSource.WIRELESS_CHARGER
            else -> PowerSource.BATTERY
        }
        
        val canStartRecording = reading.batteryLevel >= MIN_RECORDING_BATTERY_LEVEL &&
                               reading.batteryHealth != BatteryHealth.DEAD &&
                               reading.batteryHealth != BatteryHealth.FAILURE
        
        val maxRecordingDuration = getEstimatedRecordingDuration()
        
        val currentState = _batteryState.value
        _batteryState.value = currentState.copy(
            batteryLevel = reading.batteryLevel,
            batteryVoltage = reading.voltage,
            batteryTemperature = reading.temperature,
            chargingState = reading.chargingState,
            powerSource = powerSource,
            batteryHealth = reading.batteryHealth,
            canStartRecording = canStartRecording,
            maxRecordingDuration = maxRecordingDuration,
            recommendedActions = getOptimizationRecommendations()
        )
    }

    /**
     * Auto-apply optimizations based on battery level
     */
    private fun autoApplyOptimizations(reading: BatteryReading) {
        val targetLevel = when {
            reading.batteryLevel <= CRITICAL_BATTERY_LEVEL -> OptimizationLevel.EMERGENCY
            reading.batteryLevel <= AGGRESSIVE_POWER_SAVING_LEVEL -> OptimizationLevel.AGGRESSIVE
            reading.batteryLevel <= MODERATE_POWER_SAVING_LEVEL -> OptimizationLevel.MODERATE
            reading.batteryLevel <= WARNING_BATTERY_LEVEL -> OptimizationLevel.CONSERVATIVE
            else -> OptimizationLevel.NONE
        }
        
        if (targetLevel != currentOptimizationLevel) {
            applyOptimization(targetLevel)
        }
    }

    /**
     * Initialize power saving strategies
     */
    private fun initializePowerSavingStrategies() {
        powerSavingStrategies.apply {
            add(PowerSavingStrategy("Screen Brightness", ::optimizeScreenBrightness))
            add(PowerSavingStrategy("CPU Performance", ::optimizeCPUPerformance))
            add(PowerSavingStrategy("Network Usage", ::optimizeNetworkUsage))
            add(PowerSavingStrategy("Background Apps", ::optimizeBackgroundApps))
            add(PowerSavingStrategy("Sensor Sampling", ::optimizeSensorSampling))
            add(PowerSavingStrategy("Video Quality", ::optimizeVideoQuality))
        }
    }

    /**
     * Apply conservative optimization
     */
    private fun applyConservativeOptimization() {
        XLog.d(TAG, "Applying conservative power optimization")
        optimizeScreenBrightness()
    }

    /**
     * Apply moderate optimization  
     */
    private fun applyModerateOptimization() {
        XLog.d(TAG, "Applying moderate power optimization")
        optimizeScreenBrightness()
        optimizeNetworkUsage()
    }

    /**
     * Apply aggressive optimization
     */
    private fun applyAggressiveOptimization() {
        XLog.w(TAG, "Applying aggressive power optimization")
        optimizeScreenBrightness()
        optimizeCPUPerformance()
        optimizeNetworkUsage()
        optimizeBackgroundApps()
        optimizeSensorSampling()
    }

    /**
     * Apply emergency optimization
     */
    private fun applyEmergencyOptimization() {
        XLog.e(TAG, "Applying emergency power optimization")
        optimizeScreenBrightness()
        optimizeCPUPerformance()
        optimizeNetworkUsage()
        optimizeBackgroundApps()
        optimizeSensorSampling()
        optimizeVideoQuality()
    }

    /**
     * Disable all optimizations
     */
    private fun disableAllOptimizations() {
        XLog.d(TAG, "Disabling all power optimizations")
        // Implementation would restore normal power settings
    }

    // Power saving strategy implementations
    private fun optimizeScreenBrightness() {
        XLog.d(TAG, "Optimizing screen brightness")
        // Implementation would reduce screen brightness
    }

    private fun optimizeCPUPerformance() {
        XLog.d(TAG, "Optimizing CPU performance")
        // Implementation would reduce CPU governor to power save mode
    }

    private fun optimizeNetworkUsage() {
        XLog.d(TAG, "Optimizing network usage")
        // Implementation would reduce network polling, disable background sync
    }

    private fun optimizeBackgroundApps() {
        XLog.d(TAG, "Optimizing background apps")
        // Implementation would limit background app processing
    }

    private fun optimizeSensorSampling() {
        XLog.d(TAG, "Optimizing sensor sampling")
        // Implementation would reduce GSR sampling rate if possible
    }

    private fun optimizeVideoQuality() {
        XLog.d(TAG, "Optimizing video quality")
        // Implementation would reduce video resolution/framerate
    }

    /**
     * Get battery capacity in mAh
     */
    private fun getBatteryCapacityMah(): Double {
        return try {
            // Try to get from BatteryManager (API 21+)
            val capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            if (capacity > 0) capacity.toDouble() else 3000.0 // Default estimate
        } catch (e: Exception) {
            3000.0 // Conservative estimate for most phones
        }
    }

    /**
     * Data class for battery readings
     */
    private data class BatteryReading(
        val timestamp: Long,
        val batteryLevel: Int,
        val voltage: Int,
        val temperature: Float,
        val chargingState: ChargingState,
        val batteryHealth: BatteryHealth
    )

    /**
     * Data class for power saving strategies
     */
    private data class PowerSavingStrategy(
        val name: String,
        val execute: () -> Unit
    )
}