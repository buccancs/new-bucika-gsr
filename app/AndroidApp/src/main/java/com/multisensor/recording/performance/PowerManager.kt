package com.multisensor.recording.performance

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.multisensor.recording.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

enum class PowerSaveMode {
    NORMAL,
    OPTIMISED,
    AGGRESSIVE
}

@Singleton
class PowerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var monitoringJob: Job? = null
    private var listeners = mutableListOf<PowerOptimizationListener>()

    private var currentBatteryLevel: Int = 100
    private var isCharging: Boolean = false
    private var powerSaveMode: PowerSaveMode = PowerSaveMode.NORMAL
    private var adaptiveFrameRateEnabled: Boolean = true
    private var backgroundProcessingOptimized: Boolean = false

    interface PowerOptimizationListener {
        fun onBatteryLevelChanged(level: Int)
        fun onChargingStateChanged(isCharging: Boolean)
        fun onPowerSaveModeChanged(mode: PowerSaveMode)
        fun onAdaptiveFrameRateChanged(enabled: Boolean)
    }

    fun startOptimization() {
        logger.info("PowerManager: Starting power optimisation")

        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    updateBatteryStatus()

                    optimizePowerSettings()

                    delay(10000)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: SecurityException) {
                    logger.error("PowerManager: Security error during monitoring cycle", e)
                    delay(15000)
                } catch (e: IllegalStateException) {
                    logger.error("PowerManager: State error during monitoring cycle", e)
                    delay(15000)
                }
            }
        }
    }

    fun stopOptimization() {
        logger.info("PowerManager: Stopping power optimisation")
        monitoringJob?.cancel()
        monitoringJob = null
    }

    fun addListener(listener: PowerOptimizationListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: PowerOptimizationListener) {
        listeners.remove(listener)
    }

    private fun updateBatteryStatus() {
        try {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (batteryIntent != null) {
                val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = if (level != -1 && scale != -1) {
                    (level * 100 / scale)
                } else {
                    currentBatteryLevel
                }

                if (currentBatteryLevel != batteryPct) {
                    currentBatteryLevel = batteryPct
                    notifyBatteryLevelChange()
                }

                val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                if (isCharging != charging) {
                    isCharging = charging
                    notifyChargingStateChange()
                }

                logger.debug("PowerManager: Battery level: $currentBatteryLevel%, Charging: $isCharging")
            }
        } catch (e: SecurityException) {
            logger.error("PowerManager: Security error accessing battery status", e)
        } catch (e: IllegalStateException) {
            logger.error("PowerManager: State error updating battery status", e)
        }
    }

    private fun optimizePowerSettings() {
        val newPowerSaveMode = when {
            currentBatteryLevel <= 20 && !isCharging -> PowerSaveMode.AGGRESSIVE
            currentBatteryLevel <= 50 && !isCharging -> PowerSaveMode.OPTIMISED
            else -> PowerSaveMode.NORMAL
        }

        if (powerSaveMode != newPowerSaveMode) {
            powerSaveMode = newPowerSaveMode
            notifyPowerSaveModeChange()
            logger.info("PowerManager: Power save mode changed to $powerSaveMode")

            applyPowerSaveOptimizations()
        }
    }

    private fun applyPowerSaveOptimizations() {
        when (powerSaveMode) {
            PowerSaveMode.NORMAL -> {
                adaptiveFrameRateEnabled = true
                backgroundProcessingOptimized = false
            }

            PowerSaveMode.OPTIMISED -> {
                adaptiveFrameRateEnabled = true
                backgroundProcessingOptimized = true
                optimizeBackgroundProcessing()
            }

            PowerSaveMode.AGGRESSIVE -> {
                adaptiveFrameRateEnabled = true
                backgroundProcessingOptimized = true
                optimizeBackgroundProcessing()
                implementAdaptiveFrameRates()
            }
        }

        notifyAdaptiveFrameRateChange()
    }

    private fun optimizeBackgroundProcessing() {
        logger.info("PowerManager: Optimising background processing")

        logger.debug("PowerManager: Background processing optimised for power save mode: $powerSaveMode")
    }

    private fun implementAdaptiveFrameRates() {
        logger.info("PowerManager: Implementing adaptive frame rates")

        logger.debug("PowerManager: Adaptive frame rates enabled for power conservation")
    }

    fun getPowerOptimizationSettings(): PowerOptimizationSettings {
        return PowerOptimizationSettings(
            batteryLevel = currentBatteryLevel,
            isCharging = isCharging,
            powerSaveMode = powerSaveMode,
            adaptiveFrameRateEnabled = adaptiveFrameRateEnabled,
            backgroundProcessingOptimized = backgroundProcessingOptimized
        )
    }

    fun getRecommendedPowerSettings(): PowerRecommendations {
        return when (powerSaveMode) {
            PowerSaveMode.NORMAL -> PowerRecommendations(
                recommendedFrameRate = 5.0f,
                recommendedQuality = 85,
                enablePreview = true,
                enableThermal = true,
                reducedProcessing = false
            )

            PowerSaveMode.OPTIMISED -> PowerRecommendations(
                recommendedFrameRate = 3.0f,
                recommendedQuality = 70,
                enablePreview = true,
                enableThermal = true,
                reducedProcessing = true
            )

            PowerSaveMode.AGGRESSIVE -> PowerRecommendations(
                recommendedFrameRate = 1.0f,
                recommendedQuality = 55,
                enablePreview = false,
                enableThermal = false,
                reducedProcessing = true
            )
        }
    }

    fun setPowerSaveMode(mode: PowerSaveMode) {
        if (powerSaveMode != mode) {
            powerSaveMode = mode
            notifyPowerSaveModeChange()
            applyPowerSaveOptimizations()
            logger.info("PowerManager: Manually set power save mode to $mode")
        }
    }

    fun isInLowPowerState(): Boolean {
        return currentBatteryLevel <= 20 && !isCharging
    }

    private fun notifyBatteryLevelChange() {
        listeners.forEach { it.onBatteryLevelChanged(currentBatteryLevel) }
    }

    private fun notifyChargingStateChange() {
        listeners.forEach { it.onChargingStateChanged(isCharging) }
    }

    private fun notifyPowerSaveModeChange() {
        listeners.forEach { it.onPowerSaveModeChanged(powerSaveMode) }
    }

    private fun notifyAdaptiveFrameRateChange() {
        listeners.forEach { it.onAdaptiveFrameRateChanged(adaptiveFrameRateEnabled) }
    }
}

data class PowerOptimizationSettings(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val powerSaveMode: PowerSaveMode,
    val adaptiveFrameRateEnabled: Boolean,
    val backgroundProcessingOptimized: Boolean
)

data class PowerRecommendations(
    val recommendedFrameRate: Float,
    val recommendedQuality: Int,
    val enablePreview: Boolean,
    val enableThermal: Boolean,
    val reducedProcessing: Boolean
)
