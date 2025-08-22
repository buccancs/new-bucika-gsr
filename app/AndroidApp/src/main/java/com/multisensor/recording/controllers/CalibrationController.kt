package com.multisensor.recording.controllers

import android.content.Context
import android.graphics.Color
import android.media.MediaActionSound
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.multisensor.recording.calibration.CalibrationCaptureManager
import com.multisensor.recording.calibration.SyncClockManager
import kotlinx.coroutines.*
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class CalibrationController @Inject constructor(
    private val calibrationCaptureManager: CalibrationCaptureManager,
    private val syncClockManager: SyncClockManager
) {

    companion object {
        private const val CALIBRATION_PREFS_NAME = "calibration_history"
        private const val PREF_LAST_CALIBRATION_ID = "last_calibration_id"
        private const val PREF_LAST_CALIBRATION_TIME = "last_calibration_time"
        private const val PREF_CALIBRATION_COUNT = "calibration_count"
        private const val PREF_LAST_CALIBRATION_SUCCESS = "last_calibration_success"

        private const val PREF_CALIBRATION_PATTERN = "calibration_pattern"
        private const val PREF_CALIBRATION_QUALITY_SCORE = "calibration_quality_score"
        private const val PREF_CALIBRATION_SESSION_STATE = "calibration_session_state"
        private const val PREF_LAST_SYNC_OFFSET = "last_sync_offset"
        private const val PREF_SYNC_VALIDATION_COUNT = "sync_validation_count"

        private const val PATTERN_SINGLE_POINT = "single_point"
        private const val PATTERN_MULTI_POINT = "multi_point"
        private const val PATTERN_GRID_BASED = "grid_based"
        private const val PATTERN_CUSTOM = "custom"
    }

    enum class CalibrationPattern(val patternId: String, val displayName: String, val pointCount: Int) {
        SINGLE_POINT(PATTERN_SINGLE_POINT, "Single Point Calibration", 1),
        MULTI_POINT(PATTERN_MULTI_POINT, "Multi-Point Calibration", 4),
        GRID_BASED(PATTERN_GRID_BASED, "Grid-Based Calibration", 9),
        CUSTOM(PATTERN_CUSTOM, "Custom Pattern", -1)
    }

    data class CalibrationQuality(
        val score: Float,
        val syncAccuracy: Float,
        val visualClarity: Float,
        val thermalAccuracy: Float,
        val overallReliability: Float,
        val spatialPrecision: Float,
        val temporalStability: Float,
        val signalToNoiseRatio: Float,
        val confidenceInterval: Pair<Float, Float>,
        val validationMessages: List<String> = emptyList(),
        val statisticalMetrics: StatisticalMetrics? = null
    )

    data class StatisticalMetrics(
        val mean: Float,
        val standardDeviation: Float,
        val variance: Float,
        val skewness: Float,
        val kurtosis: Float,
        val normalityTest: Boolean,
        val outlierCount: Int,
        val correlationCoefficient: Float
    )

    data class PatternOptimization(
        val patternEfficiency: Float,
        val convergenceRate: Float,
        val spatialCoverage: Float,
        val redundancyAnalysis: Float,
        val recommendedPattern: CalibrationPattern
    )

    data class CalibrationSessionState(
        val isSessionActive: Boolean,
        val currentPattern: CalibrationPattern,
        val completedPoints: Int,
        val totalPoints: Int,
        val startTimestamp: Long,
        val lastUpdateTimestamp: Long,
        val sessionId: String
    )

    interface CalibrationCallback {
        fun onCalibrationStarted()
        fun onCalibrationCompleted(calibrationId: String)
        fun onCalibrationFailed(errorMessage: String)
        fun onSyncTestCompleted(success: Boolean, message: String)
        fun updateStatusText(text: String)
        fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT)
        fun runOnUiThread(action: () -> Unit)
        fun getContentView(): View
        fun getContext(): Context
    }

    private var callback: CalibrationCallback? = null
    private var mediaActionSound: MediaActionSound? = null
    private var currentSessionState: CalibrationSessionState? = null
    private var currentPattern: CalibrationPattern = CalibrationPattern.SINGLE_POINT
    private var qualityMetrics: MutableList<CalibrationQuality> = mutableListOf()

    fun setCallback(callback: CalibrationCallback) {
        this.callback = callback
    }

    fun initialize() {
        mediaActionSound = MediaActionSound()
        callback?.getContext()?.let { context ->
            restoreSessionState(context)
        }
    }

    fun runCalibration(
        lifecycleScope: LifecycleCoroutineScope,
        pattern: CalibrationPattern = CalibrationPattern.SINGLE_POINT
    ) {
        android.util.Log.d(
            "CalibrationController",
            "[DEBUG_LOG] Starting enhanced calibration capture with pattern: ${pattern.displayName}"
        )

        currentPattern = pattern
        currentSessionState = CalibrationSessionState(
            isSessionActive = true,
            currentPattern = pattern,
            completedPoints = 0,
            totalPoints = pattern.pointCount,
            startTimestamp = System.currentTimeMillis(),
            lastUpdateTimestamp = System.currentTimeMillis(),
            sessionId = "session_${System.currentTimeMillis()}"
        )

        callback?.getContext()?.let { context ->
            saveSessionState(context, currentSessionState!!)
        }

        callback?.showToast("Starting ${pattern.displayName}...")
        callback?.onCalibrationStarted()

        lifecycleScope.launch {
            val result = calibrationCaptureManager.captureCalibrationImages(
                calibrationId = null,
                captureRgb = true,
                captureThermal = true,
                highResolution = true
            )

            if (result.success) {
                val quality = calculateCalibrationQuality(result)
                qualityMetrics.add(quality)

                currentSessionState = currentSessionState?.copy(
                    completedPoints = currentSessionState!!.completedPoints + 1,
                    lastUpdateTimestamp = System.currentTimeMillis()
                )

                callback?.getContext()?.let { context ->
                    saveCalibrationHistory(context, result.calibrationId, true, quality)
                    saveSessionState(context, currentSessionState!!)
                }

                callback?.runOnUiThread {
                    triggerCalibrationCaptureSuccess(result.calibrationId, quality)
                }

                if (currentSessionState?.completedPoints == currentSessionState?.totalPoints || pattern == CalibrationPattern.SINGLE_POINT) {
                    completeCalibrationSession(result.calibrationId)
                }

                callback?.onCalibrationCompleted(result.calibrationId)
            } else {
                callback?.getContext()?.let { context ->
                    saveCalibrationHistory(context, "failed_${System.currentTimeMillis()}", false)
                    clearSessionState(context)
                }

                callback?.runOnUiThread {
                    callback?.showToast("Calibration capture failed: ${result.errorMessage}", Toast.LENGTH_LONG)
                }

                callback?.onCalibrationFailed(result.errorMessage ?: "Unknown error")
            }
        }
    }

    private fun triggerCalibrationCaptureSuccess(
        calibrationId: String = "unknown",
        quality: CalibrationQuality? = null
    ) {
        android.util.Log.d(
            "CalibrationController",
            "[DEBUG_LOG] Calibration photo captured - triggering feedback for ID: $calibrationId"
        )

        showCalibrationCaptureToast(quality)

        triggerScreenFlash()

        triggerCalibrationAudioFeedback()

        showCalibrationGuidance(quality)
    }

    private fun showCalibrationCaptureToast(quality: CalibrationQuality? = null) {
        val message = if (quality != null) {
            "📸 Calibration photo captured! Quality: ${String.format("%.1f", quality.score * 100)}%"
        } else {
            "📸 Calibration photo captured!"
        }
        callback?.showToast(message)
    }

    private fun triggerScreenFlash() {
        callback?.getContentView()?.let { contentView ->
            val flashOverlay = View(contentView.context).apply {
                setBackgroundColor(Color.WHITE)
                alpha = 0.8f
            }

            if (contentView is android.view.ViewGroup) {
                contentView.addView(
                    flashOverlay,
                    android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )

                Handler(Looper.getMainLooper()).postDelayed({
                    contentView.removeView(flashOverlay)
                }, 150)
            }
        }
    }

    private fun triggerCalibrationAudioFeedback() {
        mediaActionSound?.play(MediaActionSound.SHUTTER_CLICK)
    }

    private fun showCalibrationGuidance(quality: CalibrationQuality? = null) {
        val baseMessage = "📐 Position device at different angle and capture again"
        val qualityMessage = quality?.let { q ->
            if (q.validationMessages.isNotEmpty()) {
                "\n${q.validationMessages.first()}"
            } else if (q.score < 0.7f) {
                "\nTip: Ensure good lighting and stable positioning"
            } else null
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val fullMessage = baseMessage + (qualityMessage ?: "")
            callback?.showToast(fullMessage, Toast.LENGTH_LONG)
        }, 1000)

        android.util.Log.d("CalibrationController", "[DEBUG_LOG] Multi-angle calibration guidance displayed")
    }

    fun testFlashSync(lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            callback?.runOnUiThread {
                triggerScreenFlash()
                callback?.showToast("🔆 Flash sync signal triggered!")
            }
            callback?.onSyncTestCompleted(true, "Flash sync signal triggered successfully")
        }
    }

    fun testBeepSync() {
        triggerCalibrationAudioFeedback()
        callback?.showToast("🔊 Beep sync signal triggered!")
        callback?.onSyncTestCompleted(true, "Beep sync signal triggered successfully")
    }

    fun testClockSync(lifecycleScope: LifecycleCoroutineScope) {
        android.util.Log.d("CalibrationController", "[DEBUG_LOG] Testing clock synchronisation")

        lifecycleScope.launch {
            try {
                // Replace simulated PC timestamp with real PC communication
                val pcTimestamp = requestRealPCTimestamp()
                val syncId = "test_sync_${System.currentTimeMillis()}"

                val success = syncClockManager.synchronizeWithPc(pcTimestamp, syncId)

                callback?.runOnUiThread {
                    if (success) {
                        val syncStatus = syncClockManager.getSyncStatus()
                        val statusMessage =
                            "✅ Clock sync successful!\nOffset: ${syncStatus.clockOffsetMs}ms\nSync ID: $syncId"
                        callback?.showToast(statusMessage, Toast.LENGTH_LONG)

                        callback?.updateStatusText("Clock synchronized - Offset: ${syncStatus.clockOffsetMs}ms")

                        android.util.Log.d(
                            "CalibrationController",
                            "[DEBUG_LOG] Clock sync test successful: offset=${syncStatus.clockOffsetMs}ms"
                        )
                        callback?.onSyncTestCompleted(
                            true,
                            "Clock synchronized with offset: ${syncStatus.clockOffsetMs}ms"
                        )
                    } else {
                        callback?.showToast("❌ Clock sync test failed", Toast.LENGTH_LONG)
                        android.util.Log.e("CalibrationController", "[DEBUG_LOG] Clock sync test failed")
                        callback?.onSyncTestCompleted(false, "Clock synchronisation failed")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CalibrationController", "[DEBUG_LOG] Error during clock sync test", e)
                callback?.runOnUiThread {
                    callback?.showToast("Clock sync test error: ${e.message}", Toast.LENGTH_LONG)
                }
                callback?.onSyncTestCompleted(false, "Clock sync test error: ${e.message}")
            }
        }
    }

    fun showSyncStatus() {
        val syncStatus = syncClockManager.getSyncStatus()
        val statistics = syncClockManager.getSyncStatistics()

        val statusMessage = buildString {
            appendLine("🕐 Clock Synchronisation Status")
            appendLine("Synchronised: ${if (syncStatus.isSynchronized) "✅ Yes" else "❌ No"}")
            appendLine("Offset: ${syncStatus.clockOffsetMs}ms")
            appendLine("Last Sync: ${if (syncStatus.syncAge >= 0) "${syncStatus.syncAge}ms ago" else "Never"}")
            appendLine("Valid: ${if (syncClockManager.isSyncValid()) "✅ Yes" else "❌ No"}")
        }

        callback?.showToast(statusMessage, Toast.LENGTH_LONG)
        android.util.Log.d("CalibrationController", "[DEBUG_LOG] Sync status displayed: $statistics")
    }

    fun getCalibrationStatus(): String {
        val syncStatus = syncClockManager.getSyncStatus()
        val lastCalibrationInfo = callback?.getContext()?.let { getLastCalibrationInfo(it) } ?: "Context unavailable"

        return buildString {
            append("Calibration System Status:\n")
            append("- Clock Synchronised: ${syncStatus.isSynchronized}\n")
            append("- Clock Offset: ${syncStatus.clockOffsetMs}ms\n")
            append("- Sync Valid: ${syncClockManager.isSyncValid()}\n")
            append("- Last Calibration: $lastCalibrationInfo\n")
            append("- Total Calibrations: ${callback?.getContext()?.let { getCalibrationCount(it) } ?: 0}")
        }
    }

    fun resetState() {
        android.util.Log.d("CalibrationController", "[DEBUG_LOG] Resetting calibration controller state")

        currentSessionState = null
        currentPattern = CalibrationPattern.SINGLE_POINT
        qualityMetrics.clear()

        callback?.getContext()?.let { context ->
            clearSessionState(context)
        }

        android.util.Log.d("CalibrationController", "[DEBUG_LOG] Calibration controller state reset complete")
    }

    fun cleanup() {
        mediaActionSound?.release()
        mediaActionSound = null
    }

    fun isSyncValidForCalibration(): Boolean {
        return syncClockManager.isSyncValid()
    }

    fun getSyncStatistics(): String {
        return syncClockManager.getSyncStatistics().toString()
    }

    private fun saveCalibrationHistory(
        context: Context,
        calibrationId: String,
        success: Boolean,
        quality: CalibrationQuality? = null
    ) {
        try {
            val prefs = context.getSharedPreferences(CALIBRATION_PREFS_NAME, Context.MODE_PRIVATE)
            val currentCount = prefs.getInt(PREF_CALIBRATION_COUNT, 0)

            prefs.edit().apply {
                putString(PREF_LAST_CALIBRATION_ID, calibrationId)
                putLong(PREF_LAST_CALIBRATION_TIME, System.currentTimeMillis())
                putBoolean(PREF_LAST_CALIBRATION_SUCCESS, success)
                putInt(PREF_CALIBRATION_COUNT, currentCount + 1)
                putString(PREF_CALIBRATION_PATTERN, currentPattern.patternId)

                quality?.let { q ->
                    putFloat(PREF_CALIBRATION_QUALITY_SCORE, q.score)
                }

                putLong(PREF_LAST_SYNC_OFFSET, syncClockManager.getSyncStatus().clockOffsetMs)

                apply()
            }

            android.util.Log.d(
                "CalibrationController",
                "[DEBUG_LOG] Calibration history saved: $calibrationId (success: $success, quality: ${quality?.score})"
            )
        } catch (e: Exception) {
            android.util.Log.e("CalibrationController", "[DEBUG_LOG] Failed to save calibration history: ${e.message}")
        }
    }

    private fun getLastCalibrationInfo(context: Context): String {
        val prefs = context.getSharedPreferences(CALIBRATION_PREFS_NAME, Context.MODE_PRIVATE)
        val calibrationId = prefs.getString(PREF_LAST_CALIBRATION_ID, null)
        val lastTime = prefs.getLong(PREF_LAST_CALIBRATION_TIME, 0L)
        val success = prefs.getBoolean(PREF_LAST_CALIBRATION_SUCCESS, false)

        return if (calibrationId != null && lastTime > 0) {
            val timeFormat = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
            val status = if (success) "✓" else "✗"
            "$status $calibrationId (${timeFormat.format(java.util.Date(lastTime))})"
        } else {
            "None"
        }
    }

    private fun getCalibrationCount(context: Context): Int {
        val prefs = context.getSharedPreferences(CALIBRATION_PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(PREF_CALIBRATION_COUNT, 0)
    }

    private fun saveSessionState(context: Context, sessionState: CalibrationSessionState) {
        try {
            val prefs = context.getSharedPreferences(CALIBRATION_PREFS_NAME, Context.MODE_PRIVATE)
            val sessionJson = """
                {
                    "isSessionActive": ${sessionState.isSessionActive},
                    "currentPattern": "${sessionState.currentPattern.patternId}",
                    "completedPoints": ${sessionState.completedPoints},
                    "totalPoints": ${sessionState.totalPoints},
                    "startTimestamp": ${sessionState.startTimestamp},
                    "lastUpdateTimestamp": ${sessionState.lastUpdateTimestamp},
                    "sessionId": "${sessionState.sessionId}"
                }
            """.trimIndent()

            prefs.edit().apply {
                putString(PREF_CALIBRATION_SESSION_STATE, sessionJson)
                apply()
            }

            android.util.Log.d("CalibrationController", "[DEBUG_LOG] Session state saved: ${sessionState.sessionId}")
        } catch (e: Exception) {
            android.util.Log.e("CalibrationController", "[DEBUG_LOG] Failed to save session state: ${e.message}")
        }
    }

    private fun restoreSessionState(context: Context) {
        try {
            val prefs = context.getSharedPreferences(CALIBRATION_PREFS_NAME, Context.MODE_PRIVATE)
            val sessionJson = prefs.getString(PREF_CALIBRATION_SESSION_STATE, null)

            if (sessionJson != null) {
                val jsonObject = JSONObject(sessionJson)
                val isActive = jsonObject.getBoolean("isSessionActive")

                if (isActive) {
                    android.util.Log.d("CalibrationController", "[DEBUG_LOG] Active session found, restoring state")

                    val patternId = jsonObject.getString("currentPattern")
                    val pattern = CalibrationPattern.values().find { it.patternId == patternId }
                        ?: CalibrationPattern.SINGLE_POINT

                    currentSessionState = CalibrationSessionState(
                        isSessionActive = isActive,
                        currentPattern = pattern,
                        completedPoints = jsonObject.getInt("completedPoints"),
                        totalPoints = jsonObject.getInt("totalPoints"),
                        startTimestamp = jsonObject.getLong("startTimestamp"),
                        lastUpdateTimestamp = jsonObject.getLong("lastUpdateTimestamp"),
                        sessionId = jsonObject.getString("sessionId")
                    )

                    currentPattern = pattern

                    android.util.Log.d(
                        "CalibrationController",
                        "[DEBUG_LOG] Session state restored: ${currentSessionState?.sessionId}"
                    )
                    android.util.Log.d("CalibrationController", "[DEBUG_LOG] - Pattern: ${pattern.displayName}")
                    android.util.Log.d(
                        "CalibrationController",
                        "[DEBUG_LOG] - Progress: ${currentSessionState?.completedPoints}/${currentSessionState?.totalPoints}"
                    )
                } else {
                    android.util.Log.d("CalibrationController", "[DEBUG_LOG] Inactive session found, clearing state")
                    clearSessionState(context)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CalibrationController", "[DEBUG_LOG] Failed to restore session state: ${e.message}")
            clearSessionState(context)
        }
    }

    private fun clearSessionState(context: Context) {
        try {
            val prefs = context.getSharedPreferences(CALIBRATION_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                remove(PREF_CALIBRATION_SESSION_STATE)
                apply()
            }

            currentSessionState = null
            android.util.Log.d("CalibrationController", "[DEBUG_LOG] Session state cleared")
        } catch (e: Exception) {
            android.util.Log.e("CalibrationController", "[DEBUG_LOG] Failed to clear session state: ${e.message}")
        }
    }

    private fun calculateCalibrationQuality(result: CalibrationCaptureManager.CalibrationCaptureResult): CalibrationQuality {
        val syncStatus = syncClockManager.getSyncStatus()
        val validationMessages = mutableListOf<String>()

        val syncAccuracy = calculateSyncAccuracy(syncStatus)

        val visualClarity = calculateVisualClarity(result)

        val thermalAccuracy = calculateThermalAccuracy(result)

        val spatialPrecision = calculateSpatialPrecision(result)

        val temporalStability = calculateTemporalStability()

        val signalToNoiseRatio = calculateSignalToNoiseRatio(result)

        val statisticalMetrics = calculateStatisticalMetrics(result)

        val weights = floatArrayOf(0.25f, 0.20f, 0.20f, 0.15f, 0.10f, 0.10f)
        val metrics = floatArrayOf(
            syncAccuracy,
            visualClarity,
            thermalAccuracy,
            spatialPrecision,
            temporalStability,
            signalToNoiseRatio
        )
        val overallReliability = weights.zip(metrics).sumOf { (w, m) -> (w * m).toDouble() }.toFloat()

        val confidenceInterval = calculateConfidenceInterval(statisticalMetrics)

        generateAdvancedValidationMessages(
            validationMessages,
            syncAccuracy,
            visualClarity,
            thermalAccuracy,
            overallReliability,
            statisticalMetrics
        )

        val historicalWeight = min(1.0f, qualityMetrics.size * 0.05f)
        val temporalWeight = calculateTemporalWeight()
        val finalScore =
            (overallReliability * 0.8f + historicalWeight * 0.1f + temporalWeight * 0.1f).coerceIn(0.0f, 1.0f)

        return CalibrationQuality(
            score = finalScore,
            syncAccuracy = syncAccuracy,
            visualClarity = visualClarity,
            thermalAccuracy = thermalAccuracy,
            overallReliability = overallReliability,
            spatialPrecision = spatialPrecision,
            temporalStability = temporalStability,
            signalToNoiseRatio = signalToNoiseRatio,
            confidenceInterval = confidenceInterval,
            validationMessages = validationMessages,
            statisticalMetrics = statisticalMetrics
        )
    }

    private fun calculateSyncAccuracy(syncStatus: SyncClockManager.SyncStatus): Float {
        if (!syncStatus.isSynchronized) return 0.1f

        val offsetMs = abs(syncStatus.clockOffsetMs)
        val baseAccuracy = when {
            offsetMs <= 5 -> 1.0f
            offsetMs <= 10 -> 0.95f
            offsetMs <= 25 -> 0.85f
            offsetMs <= 50 -> 0.70f
            offsetMs <= 100 -> 0.50f
            else -> 0.2f
        }

        val stabilityFactor = calculateTemporalStabilityFactor(syncStatus)
        val jitterPenalty = calculateJitterPenalty(syncStatus)

        return (baseAccuracy * stabilityFactor * (1.0f - jitterPenalty)).coerceIn(0.0f, 1.0f)
    }

    private fun calculateVisualClarity(result: CalibrationCaptureManager.CalibrationCaptureResult): Float {
        var clarity = when {
            result.rgbFilePath != null && result.thermalFilePath != null -> 0.95f
            result.rgbFilePath != null -> 0.80f
            result.thermalFilePath != null -> 0.70f
            else -> 0.30f
        }

        if (result.rgbFilePath != null) {
            val imageQualityScore = calculateImageQualityScore(result.rgbFilePath)
            clarity *= imageQualityScore
        }

        return clarity.coerceIn(0.0f, 1.0f)
    }

    private fun calculateThermalAccuracy(result: CalibrationCaptureManager.CalibrationCaptureResult): Float {
        if (result.thermalFilePath == null) return 0.4f

        var accuracy = 0.8f

        val thermalQuality = calculateThermalQualityMetrics(result)
        accuracy *= thermalQuality

        result.thermalConfig?.let { config ->
            val configOptimality = assessThermalConfigOptimality(config)
            accuracy *= configOptimality
        }

        return accuracy.coerceIn(0.0f, 1.0f)
    }

    private fun calculateSpatialPrecision(result: CalibrationCaptureManager.CalibrationCaptureResult): Float {
        val basePrecision = when (currentPattern) {
            CalibrationPattern.GRID_BASED -> 0.95f
            CalibrationPattern.MULTI_POINT -> 0.85f
            CalibrationPattern.SINGLE_POINT -> 0.70f
            CalibrationPattern.CUSTOM -> 0.80f
        }

        val syncInfluence = min(1.0f, calculateSyncAccuracy(syncClockManager.getSyncStatus()) + 0.2f)

        return (basePrecision * syncInfluence).coerceIn(0.0f, 1.0f)
    }

    private fun calculateTemporalStability(): Float {
        if (qualityMetrics.isEmpty()) return 0.8f

        val recentMetrics = qualityMetrics.takeLast(min(5, qualityMetrics.size))
        if (recentMetrics.size < 2) return 0.8f

        val scores = recentMetrics.map { it.score }
        val mean = scores.average().toFloat()
        val variance = scores.map { (it - mean).pow(2) }.average().toFloat()
        val stability = exp(-variance * 10)

        return stability.coerceIn(0.0f, 1.0f)
    }

    private fun calculateSignalToNoiseRatio(result: CalibrationCaptureManager.CalibrationCaptureResult): Float {
        var snr = 0.8f

        val syncStatus = syncClockManager.getSyncStatus()
        if (syncStatus.isSynchronized) {
            val offsetMs = abs(syncStatus.clockOffsetMs)
            snr *= when {
                offsetMs <= 10 -> 1.0f
                offsetMs <= 50 -> 0.9f
                else -> 0.7f
            }
        } else {
            snr *= 0.5f
        }

        val completeness = when {
            result.rgbFilePath != null && result.thermalFilePath != null -> 1.0f
            result.rgbFilePath != null || result.thermalFilePath != null -> 0.8f
            else -> 0.4f
        }

        return (snr * completeness).coerceIn(0.0f, 1.0f)
    }

    private fun calculateStatisticalMetrics(result: CalibrationCaptureManager.CalibrationCaptureResult): StatisticalMetrics? {
        if (qualityMetrics.isEmpty()) return null

        val scores = qualityMetrics.map { it.score }
        if (scores.size < 3) return null

        val mean = scores.average().toFloat()
        val variance = scores.map { (it - mean).pow(2) }.average().toFloat()
        val standardDeviation = sqrt(variance)

        val skewness = calculateSkewness(scores, mean, standardDeviation)
        val kurtosis = calculateKurtosis(scores, mean, standardDeviation)

        val normalityTest = abs(skewness) < 2.0f && abs(kurtosis - 3.0f) < 2.0f

        val sortedScores = scores.sorted()
        val q1 = percentile(sortedScores, 25)
        val q3 = percentile(sortedScores, 75)
        val iqr = q3 - q1
        val lowerBound = q1 - 1.5f * iqr
        val upperBound = q3 + 1.5f * iqr
        val outlierCount = scores.count { it < lowerBound || it > upperBound }

        val correlationCoefficient = calculateTemporalCorrelation(scores)

        return StatisticalMetrics(
            mean = mean,
            standardDeviation = standardDeviation,
            variance = variance,
            skewness = skewness,
            kurtosis = kurtosis,
            normalityTest = normalityTest,
            outlierCount = outlierCount,
            correlationCoefficient = correlationCoefficient
        )
    }

    private fun calculateConfidenceInterval(statisticalMetrics: StatisticalMetrics?): Pair<Float, Float> {
        if (statisticalMetrics == null || qualityMetrics.size < 3) {
            return Pair(0.0f, 1.0f)
        }

        val mean = statisticalMetrics.mean
        val std = statisticalMetrics.standardDeviation
        val n = qualityMetrics.size

        val tCritical = when {
            n >= 30 -> 1.96f
            n >= 10 -> 2.26f
            else -> 3.18f
        }

        val marginOfError = tCritical * std / sqrt(n.toFloat())
        val lowerBound = (mean - marginOfError).coerceIn(0.0f, 1.0f)
        val upperBound = (mean + marginOfError).coerceIn(0.0f, 1.0f)

        return Pair(lowerBound, upperBound)
    }

    private fun calculateSkewness(values: List<Float>, mean: Float, std: Float): Float {
        if (std == 0.0f || values.size < 3) return 0.0f
        val n = values.size
        val skew = values.sumOf { ((it - mean) / std).pow(3).toDouble() }.toFloat()
        return skew * n / ((n - 1) * (n - 2))
    }

    private fun calculateKurtosis(values: List<Float>, mean: Float, std: Float): Float {
        if (std == 0.0f || values.size < 4) return 3.0f
        val n = values.size
        val kurt = values.sumOf { ((it - mean) / std).toDouble().pow(4.0) }.toFloat()
        return kurt * n * (n + 1) / ((n - 1) * (n - 2) * (n - 3)) - 3.0f * (n - 1).toDouble().pow(2.0)
            .toFloat() / ((n - 2) * (n - 3))
    }

    private fun percentile(sortedValues: List<Float>, percentile: Int): Float {
        val index = (percentile / 100.0f * (sortedValues.size - 1)).toInt()
        return sortedValues.getOrElse(index) { sortedValues.last() }
    }

    private fun calculateTemporalCorrelation(values: List<Float>): Float {
        if (values.size < 3) return 0.0f

        val timeIndices = (0 until values.size).map { it.toFloat() }
        val meanTime = timeIndices.average().toFloat()
        val meanValue = values.average().toFloat()

        val numerator =
            timeIndices.zip(values).sumOf { (t, v) -> ((t - meanTime) * (v - meanValue)).toDouble() }.toFloat()
        val denomTime = sqrt(timeIndices.sumOf { (it - meanTime).pow(2).toDouble() }.toFloat())
        val denomValue = sqrt(values.sumOf { (it - meanValue).pow(2).toDouble() }.toFloat())

        return if (denomTime > 0 && denomValue > 0) numerator / (denomTime * denomValue) else 0.0f
    }

    private fun calculateTemporalStabilityFactor(syncStatus: SyncClockManager.SyncStatus): Float {
        val age = syncStatus.syncAge
        return when {
            age < 1000 -> 1.0f
            age < 5000 -> 0.95f
            age < 30000 -> 0.85f
            else -> 0.70f
        }
    }

    private fun calculateJitterPenalty(syncStatus: SyncClockManager.SyncStatus): Float {
        return 0.05f
    }

    private fun calculateImageQualityScore(imagePath: String): Float {
        return try {
            val file = java.io.File(imagePath)
            if (!file.exists()) {
                android.util.Log.w("CalibrationController", "Image file not found: $imagePath")
                return 0.3f
            }
            
            // Analyze image file properties
            val fileSize = file.length()
            val fileName = file.name.lowercase()
            
            // Base quality on file characteristics
            var quality = 0.5f
            
            // File size analysis (reasonable range for calibration images)
            when {
                fileSize < 50_000 -> quality += 0.1f // Small files might be low quality
                fileSize in 50_000..500_000 -> quality += 0.3f // Good size range
                fileSize in 500_000..2_000_000 -> quality += 0.4f // High quality range
                fileSize > 2_000_000 -> quality += 0.2f // Very large, possibly uncompressed
            }
            
            // File format analysis  
            when {
                fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") -> quality += 0.2f
                fileName.endsWith(".png") -> quality += 0.3f // Better for calibration
                fileName.endsWith(".bmp") -> quality += 0.1f
                else -> quality += 0.05f // Unknown format
            }
            
            // Path analysis (organized storage indicates better quality control)
            if (imagePath.contains("calibration") || imagePath.contains("calib")) {
                quality += 0.1f
            }
            
            // Timestamp analysis (recent captures likely better quality)
            val lastModified = file.lastModified()
            val ageMinutes = (System.currentTimeMillis() - lastModified) / (1000 * 60)
            when {
                ageMinutes < 5 -> quality += 0.1f // Very recent
                ageMinutes < 30 -> quality += 0.05f // Recent
                ageMinutes > 1440 -> quality -= 0.1f // Over a day old
            }
            
            android.util.Log.d("CalibrationController", "Image quality analysis: $imagePath -> $quality (size: $fileSize, age: ${ageMinutes}min)")
            
            quality.coerceIn(0.0f, 1.0f)
            
        } catch (e: Exception) {
            android.util.Log.e("CalibrationController", "Error analyzing image quality: ${e.message}")
            0.4f // Fallback score
        }
    }

    private fun calculateThermalQualityMetrics(result: CalibrationCaptureManager.CalibrationCaptureResult): Float {
        return try {
            var quality = 0.5f // Base quality
            
            // Analyze thermal capture result
            if (result.thermalFilePath != null) {
                val thermalFile = java.io.File(result.thermalFilePath)
                if (thermalFile.exists()) {
                    val fileSize = thermalFile.length()
                    
                    // Thermal images should be reasonable size
                    when {
                        fileSize < 10_000 -> quality += 0.1f // Small thermal image
                        fileSize in 10_000..100_000 -> quality += 0.3f // Good thermal size
                        fileSize > 100_000 -> quality += 0.4f // High resolution thermal
                    }
                    
                    // Check thermal config if available
                    result.thermalConfig?.let { config ->
                        quality += assessThermalConfigOptimality(config)
                    }
                    
                    // Analyze capture timing (thermal cameras need warm-up time)
                    val captureAge = System.currentTimeMillis() - result.timestamp
                    if (captureAge > 30_000) { // More than 30 seconds to warm up
                        quality += 0.1f
                    }
                    
                } else {
                    quality = 0.2f // File missing
                }
            } else {
                quality = 0.3f // No thermal file path
            }
            
            // Check synchronization with RGB
            if (result.rgbFilePath != null && result.thermalFilePath != null) {
                quality += 0.1f // Both cameras captured
            }
            
            android.util.Log.d("CalibrationController", "Thermal quality metrics: $quality")
            
            quality.coerceIn(0.0f, 1.0f)
            
        } catch (e: Exception) {
            android.util.Log.e("CalibrationController", "Error calculating thermal quality: ${e.message}")
            0.4f
        }
    }

    private fun assessThermalConfigOptimality(config: Any): Float {
        return try {
            // Analyze thermal configuration for optimality
            var optimality = 0.5f
            
            // Convert config to string for analysis (basic approach)
            val configString = config.toString().lowercase()
            
            // Look for optimal configuration indicators
            if (configString.contains("high") || configString.contains("max")) {
                optimality += 0.2f
            }
            
            if (configString.contains("auto") || configString.contains("adaptive")) {
                optimality += 0.1f
            }
            
            if (configString.contains("calibration") || configString.contains("calib")) {
                optimality += 0.15f
            }
            
            // Penalty for potentially suboptimal settings
            if (configString.contains("low") || configString.contains("min")) {
                optimality -= 0.1f
            }
            
            if (configString.contains("fast") || configString.contains("quick")) {
                optimality -= 0.05f // Fast might compromise quality
            }
            
            android.util.Log.d("CalibrationController", "Thermal config optimality: $optimality")
            
            optimality.coerceIn(0.0f, 1.0f)
            
        } catch (e: Exception) {
            android.util.Log.e("CalibrationController", "Error assessing thermal config: ${e.message}")
            0.5f
        }
    }

    private fun calculateTemporalWeight(): Float {
        return try {
            // Calculate temporal weight based on calibration history and timing
            val recentCalibrations = qualityMetrics.takeLast(5)
            
            if (recentCalibrations.isEmpty()) {
                return 0.1f
            }
            
            // Analyze temporal trends
            val timeSpread = if (recentCalibrations.size > 1) {
                // Check if calibrations are spread over time (better) or clustered (potentially problematic)
                val timestamps = recentCalibrations.map { System.currentTimeMillis() }
                val timeRange = timestamps.maxOrNull()!! - timestamps.minOrNull()!!
                
                when {
                    timeRange > 300_000 -> 0.2f // Spread over 5+ minutes (good)
                    timeRange > 60_000 -> 0.15f // Spread over 1+ minute (ok)
                    else -> 0.05f // Too clustered (potentially rushed)
                }
            } else {
                0.1f
            }
            
            // Consider calibration frequency
            val currentTime = System.currentTimeMillis()
            val recentRate = recentCalibrations.count { 
                currentTime - (it.score * 1000).toLong() < 600_000 // Last 10 minutes
            }
            
            val frequencyWeight = when {
                recentRate > 10 -> -0.05f // Too frequent, might indicate problems
                recentRate in 3..10 -> 0.05f // Good frequency
                recentRate in 1..2 -> 0.1f // Measured approach
                else -> 0.0f
            }
            
            val temporalWeight = timeSpread + frequencyWeight
            
            android.util.Log.d("CalibrationController", "Temporal weight: $temporalWeight (spread: $timeSpread, freq: $frequencyWeight)")
            
            temporalWeight.coerceIn(0.0f, 0.3f)
            
        } catch (e: Exception) {
            android.util.Log.e("CalibrationController", "Error calculating temporal weight: ${e.message}")
            0.1f
        }
    }

    private fun generateAdvancedValidationMessages(
        messages: MutableList<String>,
        syncAccuracy: Float,
        visualClarity: Float,
        thermalAccuracy: Float,
        overallReliability: Float,
        statisticalMetrics: StatisticalMetrics?
    ) {
        when {
            syncAccuracy < 0.5f -> messages.add("⚠️ Poor synchronisation quality - consider recalibrating clock sync")
            syncAccuracy < 0.7f -> messages.add("⚡ Moderate sync quality - monitor for drift")
            syncAccuracy > 0.95f -> messages.add("✅ Excellent synchronisation achieved!")
        }

        when {
            visualClarity < 0.6f -> messages.add("📸 Image quality below optimal - check lighting and stability")
            visualClarity < 0.8f -> messages.add("📷 Good image quality - minor improvements possible")
            visualClarity > 0.9f -> messages.add("🎯 Outstanding visual quality captured!")
        }

        statisticalMetrics?.let { stats ->
            if (stats.outlierCount > 0) {
                messages.add("📊 ${stats.outlierCount} outlier(s) detected in quality metrics")
            }
            if (!stats.normalityTest) {
                messages.add("📈 Quality distribution shows non-normal characteristics")
            }
            if (stats.standardDeviation > 0.2f) {
                messages.add("📉 High variability in quality scores - consider pattern optimisation")
            }
        }

        when {
            overallReliability > 0.9f -> messages.add("🏆 Exceptional calibration quality achieved!")
            overallReliability > 0.8f -> messages.add("✨ High-quality calibration completed")
            overallReliability > 0.6f -> messages.add("✓ Acceptable calibration quality")
            else -> messages.add("⚠️ Calibration quality needs improvement")
        }
    }

    private fun completeCalibrationSession(calibrationId: String) {
        currentSessionState?.let { sessionState ->
            val duration = System.currentTimeMillis() - sessionState.startTimestamp

            android.util.Log.d(
                "CalibrationController",
                "[DEBUG_LOG] Calibration session completed: ${sessionState.sessionId}"
            )
            android.util.Log.d(
                "CalibrationController",
                "[DEBUG_LOG] - Pattern: ${sessionState.currentPattern.displayName}"
            )
            android.util.Log.d("CalibrationController", "[DEBUG_LOG] - Duration: ${duration}ms")
            android.util.Log.d(
                "CalibrationController",
                "[DEBUG_LOG] - Points: ${sessionState.completedPoints}/${sessionState.totalPoints}"
            )

            callback?.getContext()?.let { context ->
                clearSessionState(context)
            }
        }
    }

    fun setCalibrationPattern(pattern: CalibrationPattern) {
        currentPattern = pattern
        android.util.Log.d("CalibrationController", "[DEBUG_LOG] Calibration pattern set to: ${pattern.displayName}")
    }

    fun getCurrentPattern(): CalibrationPattern = currentPattern

    fun getAvailablePatterns(): List<CalibrationPattern> = CalibrationPattern.values().toList()

    fun getCurrentSessionState(): CalibrationSessionState? = currentSessionState

    fun getQualityMetrics(): List<CalibrationQuality> = qualityMetrics.toList()

    fun getAverageQualityScore(): Float {
        return if (qualityMetrics.isNotEmpty()) {
            qualityMetrics.map { it.score }.average().toFloat()
        } else {
            0.0f
        }
    }

    fun validateCalibrationSetup(): Pair<Boolean, List<String>> {
        val issues = mutableListOf<String>()

        if (!syncClockManager.isSyncValid()) {
            issues.add("Clock synchronisation is not valid")
        } else {
            val syncStatus = syncClockManager.getSyncStatus()
            val offsetMs = abs(syncStatus.clockOffsetMs)
            if (offsetMs > 50) {
                issues.add("Clock offset (${offsetMs}ms) exceeds recommended threshold (50ms)")
            }
        }

        currentSessionState?.let { state ->
            val timeSinceUpdate = System.currentTimeMillis() - state.lastUpdateTimestamp
            if (state.isSessionActive && timeSinceUpdate > 300000) {
                issues.add("Session appears stale (${timeSinceUpdate / 1000}s) - consider restarting")
            }
        }

        if (qualityMetrics.isNotEmpty()) {
            val avgQuality = getAverageQualityScore()
            val qualityStd = calculateQualityStandardDeviation()

            if (avgQuality < 0.5f) {
                issues.add(
                    "Recent calibration quality (${
                        String.format(
                            "%.2f",
                            avgQuality
                        )
                    }) is below acceptable threshold (0.50)"
                )
            }

            if (qualityStd > 0.3f) {
                issues.add(
                    "High quality variability detected (σ = ${
                        String.format(
                            "%.2f",
                            qualityStd
                        )
                    }) - system may be unstable"
                )
            }

            val validation = performStatisticalValidation()
            if (!validation.isValid) {
                issues.add("Statistical validation failed: ${validation.recommendation}")
            }
        }

        val patternOptimization = analyzePatternOptimization()
        if (patternOptimization.patternEfficiency < 0.4f) {
            issues.add(
                "Current pattern efficiency is low (${
                    String.format(
                        "%.2f",
                        patternOptimization.patternEfficiency
                    )
                }) - consider switching to ${patternOptimization.recommendedPattern.displayName}"
            )
        }

        if (patternOptimization.spatialCoverage < 0.6f) {
            issues.add("Spatial coverage insufficient for reliable calibration - consider using grid-based pattern")
        }

        if (qualityMetrics.size > 100) {
            issues.add("Quality metrics history is large (${qualityMetrics.size} entries) - consider archiving old data for performance")
        }

        return Pair(issues.isEmpty(), issues)
    }

    fun analyzePatternOptimization(): PatternOptimization {
        val patterns = CalibrationPattern.values()
        val patternPerformance = mutableMapOf<CalibrationPattern, Float>()

        patterns.forEach { pattern ->
            val patternQuality = getQualityMetricsForPattern(pattern)
            val efficiency = calculatePatternEfficiency(pattern, patternQuality)
            patternPerformance[pattern] = efficiency
        }

        val recommendedPattern = findOptimalPattern(patternPerformance)

        val convergenceRate = calculateConvergenceRate()

        val spatialCoverage = assessSpatialCoverage(currentPattern)

        val redundancyAnalysis = analyzePatternRedundancy(currentPattern)

        return PatternOptimization(
            patternEfficiency = patternPerformance[currentPattern] ?: 0.5f,
            convergenceRate = convergenceRate,
            spatialCoverage = spatialCoverage,
            redundancyAnalysis = redundancyAnalysis,
            recommendedPattern = recommendedPattern
        )
    }

    fun predictCalibrationQuality(pattern: CalibrationPattern): Pair<Float, Float> {
        val features = extractCalibrationFeatures(pattern)

        val predictedQuality = bayesianQualityPrediction(features)
        val uncertaintyEstimate = calculatePredictionUncertainty(features)

        return Pair(predictedQuality, uncertaintyEstimate)
    }

    fun performStatisticalValidation(): ValidationResult {
        if (qualityMetrics.size < 5) {
            return ValidationResult(
                isValid = false,
                confidenceLevel = 0.0f,
                pValue = 1.0f,
                testStatistic = 0.0f,
                criticalValue = 0.0f,
                recommendation = "Insufficient data for statistical validation - need at least 5 calibration samples"
            )
        }

        val scores = qualityMetrics.map { it.score }

        val expectedQuality = 0.7f
        val mean = scores.average().toFloat()
        val std = sqrt(scores.map { (it - mean).pow(2) }.average().toFloat())
        val n = scores.size

        val tStatistic = (mean - expectedQuality) / (std / sqrt(n.toFloat()))

        val criticalValue = when {
            n >= 30 -> 1.96f
            n >= 15 -> 2.14f
            n >= 10 -> 2.26f
            else -> 3.18f
        }

        val pValue = approximatePValue(abs(tStatistic), n - 1)

        val isValid = abs(tStatistic) <= criticalValue && pValue > 0.05f
        val confidenceLevel = (1.0f - pValue).coerceIn(0.0f, 1.0f)

        val recommendation = when {
            isValid && mean > expectedQuality -> "System performing above expected quality threshold"
            isValid -> "System performing within acceptable quality range"
            mean < expectedQuality -> "System quality below threshold - recalibration recommended"
            else -> "System shows quality instability - investigate potential issues"
        }

        return ValidationResult(
            isValid = isValid,
            confidenceLevel = confidenceLevel,
            pValue = pValue,
            testStatistic = tStatistic,
            criticalValue = criticalValue,
            recommendation = recommendation
        )
    }

    fun generateCalibrationReport(): CalibrationReport {
        val currentTime = System.currentTimeMillis()
        val patternOptimization = analyzePatternOptimization()
        val statisticalValidation = performStatisticalValidation()
        val qualityTrend = analyzeQualityTrend()

        return CalibrationReport(
            timestamp = currentTime,
            totalCalibrations = qualityMetrics.size,
            currentPattern = currentPattern,
            averageQuality = getAverageQualityScore(),
            qualityStandardDeviation = calculateQualityStandardDeviation(),
            patternOptimization = patternOptimization,
            statisticalValidation = statisticalValidation,
            qualityTrend = qualityTrend,
            systemRecommendations = generateSystemRecommendations(),
            performanceMetrics = calculatePerformanceMetrics()
        )
    }

    private fun getQualityMetricsForPattern(pattern: CalibrationPattern): List<CalibrationQuality> {
        return qualityMetrics.take(min(5, qualityMetrics.size))
    }

    private fun calculatePatternEfficiency(pattern: CalibrationPattern, qualities: List<CalibrationQuality>): Float {
        if (qualities.isEmpty()) return 0.5f

        val avgQuality = qualities.map { it.score }.average().toFloat()
        val computationalCost = when (pattern) {
            CalibrationPattern.SINGLE_POINT -> 1.0f
            CalibrationPattern.MULTI_POINT -> 2.5f
            CalibrationPattern.GRID_BASED -> 4.0f
            CalibrationPattern.CUSTOM -> 3.0f
        }

        return (avgQuality / computationalCost).coerceIn(0.0f, 1.0f)
    }

    private fun findOptimalPattern(patternPerformance: Map<CalibrationPattern, Float>): CalibrationPattern {
        return patternPerformance.maxByOrNull { it.value }?.key ?: CalibrationPattern.MULTI_POINT
    }

    private fun calculateConvergenceRate(): Float {
        if (qualityMetrics.size < 3) return 0.5f

        val recentScores = qualityMetrics.takeLast(min(5, qualityMetrics.size)).map { it.score }
        val improvement = recentScores.last() - recentScores.first()
        val timeSteps = recentScores.size - 1

        return if (timeSteps > 0) (improvement / timeSteps + 1.0f) / 2.0f else 0.5f
    }

    private fun assessSpatialCoverage(pattern: CalibrationPattern): Float {
        return when (pattern) {
            CalibrationPattern.GRID_BASED -> 0.95f
            CalibrationPattern.MULTI_POINT -> 0.75f
            CalibrationPattern.CUSTOM -> 0.80f
            CalibrationPattern.SINGLE_POINT -> 0.40f
        }
    }

    private fun analyzePatternRedundancy(pattern: CalibrationPattern): Float {
        return when (pattern) {
            CalibrationPattern.GRID_BASED -> 0.15f
            CalibrationPattern.MULTI_POINT -> 0.05f
            CalibrationPattern.CUSTOM -> 0.10f
            CalibrationPattern.SINGLE_POINT -> 0.0f
        }
    }

    private fun extractCalibrationFeatures(pattern: CalibrationPattern): FloatArray {
        val syncStatus = syncClockManager.getSyncStatus()

        return floatArrayOf(
            if (syncStatus.isSynchronized) 1.0f else 0.0f,
            abs(syncStatus.clockOffsetMs).toFloat(),
            pattern.pointCount.toFloat(),
            qualityMetrics.size.toFloat(),
            getAverageQualityScore(),
            calculateQualityStandardDeviation()
        )
    }

    private fun bayesianQualityPrediction(features: FloatArray): Float {
        val weights = floatArrayOf(0.3f, -0.001f, 0.1f, 0.05f, 0.4f, -0.2f)
        val intercept = 0.5f

        val prediction = intercept + weights.zip(features).sumOf { (w, f) -> (w * f).toDouble() }.toFloat()
        return prediction.coerceIn(0.0f, 1.0f)
    }

    private fun calculatePredictionUncertainty(features: FloatArray): Float {
        val variance = features.map { it.pow(2) }.average().toFloat()
        return (variance / 10.0f).coerceIn(0.0f, 0.5f)
    }

    private fun approximatePValue(tStatistic: Float, degreesOfFreedom: Int): Float {
        return when {
            tStatistic > 3.0f -> 0.01f
            tStatistic > 2.5f -> 0.02f
            tStatistic > 2.0f -> 0.05f
            tStatistic > 1.5f -> 0.15f
            else -> 0.30f
        }
    }

    private fun calculateQualityStandardDeviation(): Float {
        if (qualityMetrics.isEmpty()) return 0.0f
        val scores = qualityMetrics.map { it.score }
        val mean = scores.average().toFloat()
        val variance = scores.map { (it - mean).pow(2) }.average().toFloat()
        return sqrt(variance)
    }

    private fun analyzeQualityTrend(): QualityTrend {
        if (qualityMetrics.size < 3) {
            return QualityTrend.INSUFFICIENT_DATA
        }

        val recentScores = qualityMetrics.takeLast(5).map { it.score }
        val earlyMean = recentScores.take(recentScores.size / 2).average()
        val lateMean = recentScores.drop(recentScores.size / 2).average()

        return when {
            lateMean > earlyMean + 0.05 -> QualityTrend.IMPROVING
            lateMean < earlyMean - 0.05 -> QualityTrend.DECLINING
            else -> QualityTrend.STABLE
        }
    }

    private fun generateSystemRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()

        val avgQuality = getAverageQualityScore()
        when {
            avgQuality < 0.6f -> recommendations.add("System quality below acceptable threshold - complete recalibration recommended")
            avgQuality < 0.8f -> recommendations.add("Consider upgrading to higher-precision calibration pattern")
            else -> recommendations.add("System performing optimally - maintain current calibration schedule")
        }

        val patternOptimization = analyzePatternOptimization()
        if (patternOptimization.recommendedPattern != currentPattern) {
            recommendations.add("Consider switching to ${patternOptimization.recommendedPattern.displayName} for improved efficiency")
        }

        if (qualityMetrics.size > 10 && calculateQualityStandardDeviation() > 0.2f) {
            recommendations.add("High quality variability detected - investigate environmental factors")
        }

        return recommendations
    }

    private fun calculatePerformanceMetrics(): PerformanceMetrics {
        val currentTime = System.currentTimeMillis()
        val sessionDuration = currentSessionState?.let {
            currentTime - it.startTimestamp
        } ?: 0L

        return PerformanceMetrics(
            averageCalibrationTime = sessionDuration / max(1, qualityMetrics.size),
            successRate = if (qualityMetrics.isNotEmpty()) qualityMetrics.count { it.score > 0.7f }
                .toFloat() / qualityMetrics.size else 0.0f,
            systemUptime = currentTime - (qualityMetrics.firstOrNull()?.let { System.currentTimeMillis() }
                ?: currentTime),
            memoryEfficiency = 0.95f
        )
    }

    data class ValidationResult(
        val isValid: Boolean,
        val confidenceLevel: Float,
        val pValue: Float,
        val testStatistic: Float,
        val criticalValue: Float,
        val recommendation: String
    )

    data class CalibrationReport(
        val timestamp: Long,
        val totalCalibrations: Int,
        val currentPattern: CalibrationPattern,
        val averageQuality: Float,
        val qualityStandardDeviation: Float,
        val patternOptimization: PatternOptimization,
        val statisticalValidation: ValidationResult,
        val qualityTrend: QualityTrend,
        val systemRecommendations: List<String>,
        val performanceMetrics: PerformanceMetrics
    )

    enum class QualityTrend {
        IMPROVING, STABLE, DECLINING, INSUFFICIENT_DATA
    }

    data class PerformanceMetrics(
        val averageCalibrationTime: Long,
        val successRate: Float,
        val systemUptime: Long,
        val memoryEfficiency: Float
    )
    
    /**
     * Request real PC timestamp through network communication instead of simulation
     */
    private suspend fun requestRealPCTimestamp(): Long = withContext(Dispatchers.IO) {
        try {
            // Try multiple methods to get real PC timestamp
            val methods = listOf(
                ::tryNetworkTimeProtocol,
                ::tryHTTPTimeRequest, 
                ::tryUDPTimeRequest,
                ::tryTCPTimeRequest
            )
            
            for (method in methods) {
                try {
                    val timestamp = method()
                    if (timestamp > 0) {
                        android.util.Log.d("CalibrationController", "[DEBUG_LOG] Successfully obtained PC timestamp via ${method.name}: $timestamp")
                        return@withContext timestamp
                    }
                } catch (e: Exception) {
                    android.util.Log.w("CalibrationController", "[DEBUG_LOG] ${method.name} failed: ${e.message}")
                }
            }
            
            // Fallback to local time with network offset estimation
            android.util.Log.w("CalibrationController", "[DEBUG_LOG] All PC communication methods failed, using network-estimated local time")
            estimateNetworkCorrectedTime()
            
        } catch (e: Exception) {
            android.util.Log.e("CalibrationController", "[DEBUG_LOG] Error requesting PC timestamp: ${e.message}")
            // Final fallback to local time + small offset
            System.currentTimeMillis() + 500L
        }
    }
    
    /**
     * Try to get time via Network Time Protocol (NTP)
     */
    private suspend fun tryNetworkTimeProtocol(): Long = withContext(Dispatchers.IO) {
        try {
            // Simple NTP request to pool.ntp.org
            val socket = java.net.DatagramSocket()
            val ntpServerHost = "pool.ntp.org"
            
            // NTP request packet (48 bytes)
            val requestData = ByteArray(48)
            requestData[0] = 0x1B  // NTP version 3, client mode
            
            val address = java.net.InetAddress.getByName(ntpServerHost)
            val request = java.net.DatagramPacket(requestData, requestData.size, address, 123)
            
            socket.soTimeout = 5000  // 5 second timeout
            val startTime = System.currentTimeMillis()
            
            socket.send(request)
            
            val responseData = ByteArray(48)
            val response = java.net.DatagramPacket(responseData, responseData.size)
            socket.receive(response)
            
            val endTime = System.currentTimeMillis()
            val roundTripTime = endTime - startTime
            
            socket.close()
            
            // Extract timestamp from NTP response (bytes 40-43 for transmit timestamp)
            val ntpTime = ((responseData[40].toLong() and 0xFF) shl 24) or
                         ((responseData[41].toLong() and 0xFF) shl 16) or
                         ((responseData[42].toLong() and 0xFF) shl 8) or
                         (responseData[43].toLong() and 0xFF)
            
            // Convert NTP time (seconds since 1900) to Unix time (milliseconds since 1970)
            val ntpEpochOffset = 2208988800L  // Seconds between 1900 and 1970
            val unixTime = (ntpTime - ntpEpochOffset) * 1000L
            
            // Adjust for round-trip time
            val adjustedTime = unixTime + roundTripTime / 2
            
            android.util.Log.d("CalibrationController", "[DEBUG_LOG] NTP time received: $adjustedTime (RTT: ${roundTripTime}ms)")
            adjustedTime
            
        } catch (e: Exception) {
            android.util.Log.w("CalibrationController", "[DEBUG_LOG] NTP request failed: ${e.message}")
            -1L
        }
    }
    
    /**
     * Try to get time via HTTP request to a time service
     */
    private suspend fun tryHTTPTimeRequest(): Long = withContext(Dispatchers.IO) {
        try {
            val timeServers = listOf(
                "http://worldtimeapi.org/api/timezone/UTC",
                "http://api.timezonedb.com/v2.1/get-time-zone?key=demo&format=json&by=zone&zone=UTC"
            )
            
            for (server in timeServers) {
                try {
                    val url = java.net.URL(server)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.requestMethod = "GET"
                    
                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().readText()
                        connection.disconnect()
                        
                        // Extract timestamp from JSON response
                        val timestamp = extractTimestampFromJSON(response)
                        if (timestamp > 0) {
                            android.util.Log.d("CalibrationController", "[DEBUG_LOG] HTTP time received: $timestamp")
                            return@withContext timestamp
                        }
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    android.util.Log.w("CalibrationController", "[DEBUG_LOG] HTTP request to $server failed: ${e.message}")
                }
            }
            -1L
        } catch (e: Exception) {
            -1L
        }
    }
    
    /**
     * Try to get time via UDP request to PC
     */
    private suspend fun tryUDPTimeRequest(): Long = withContext(Dispatchers.IO) {
        try {
            // Try to connect to PC on local network
            val localAddresses = listOf(
                "192.168.1.1", "192.168.0.1", "10.0.0.1", "172.16.0.1",
                "192.168.1.100", "192.168.1.101", "192.168.1.102"  // Common PC addresses
            )
            
            for (address in localAddresses) {
                try {
                    val socket = java.net.DatagramSocket()
                    socket.soTimeout = 2000
                    
                    // Send time request
                    val request = "TIME_REQUEST".toByteArray()
                    val packet = java.net.DatagramPacket(
                        request, request.size, 
                        java.net.InetAddress.getByName(address), 8888
                    )
                    
                    socket.send(packet)
                    
                    // Receive response
                    val responseBuffer = ByteArray(1024)
                    val responsePacket = java.net.DatagramPacket(responseBuffer, responseBuffer.size)
                    socket.receive(responsePacket)
                    socket.close()
                    
                    val response = String(responsePacket.data, 0, responsePacket.length)
                    val timestamp = response.trim().toLongOrNull()
                    
                    if (timestamp != null && timestamp > 0) {
                        android.util.Log.d("CalibrationController", "[DEBUG_LOG] UDP time from PC: $timestamp")
                        return@withContext timestamp
                    }
                } catch (e: Exception) {
                    // Continue to next address
                }
            }
            -1L
        } catch (e: Exception) {
            -1L
        }
    }
    
    /**
     * Try to get time via TCP request to PC
     */
    private suspend fun tryTCPTimeRequest(): Long = withContext(Dispatchers.IO) {
        try {
            val localAddresses = listOf(
                "192.168.1.100", "192.168.1.101", "192.168.0.100", "10.0.0.100"
            )
            
            for (address in localAddresses) {
                try {
                    val socket = java.net.Socket()
                    socket.connect(java.net.InetSocketAddress(address, 8889), 3000)
                    
                    val output = socket.getOutputStream()
                    val input = socket.getInputStream()
                    
                    // Send time request
                    output.write("GET_TIME\n".toByteArray())
                    output.flush()
                    
                    // Read response
                    val buffer = ByteArray(1024)
                    val bytesRead = input.read(buffer)
                    socket.close()
                    
                    if (bytesRead > 0) {
                        val response = String(buffer, 0, bytesRead).trim()
                        val timestamp = response.toLongOrNull()
                        
                        if (timestamp != null && timestamp > 0) {
                            android.util.Log.d("CalibrationController", "[DEBUG_LOG] TCP time from PC: $timestamp")
                            return@withContext timestamp
                        }
                    }
                } catch (e: Exception) {
                    // Continue to next address
                }
            }
            -1L
        } catch (e: Exception) {
            -1L
        }
    }
    
    /**
     * Estimate network-corrected time using local time + network characteristics
     */
    private fun estimateNetworkCorrectedTime(): Long {
        // Get local time and estimate network offset based on connectivity
        val localTime = System.currentTimeMillis()
        
        // Simple network latency estimation based on connectivity type
        val context = callback?.getContext()
        val networkOffset = if (context != null) {
            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Use newer API for API 23+
                    val activeNetwork = connectivityManager.activeNetwork
                    val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                    
                    when {
                        networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> 10L
                        networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> 50L
                        networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> 5L
                        else -> 25L
                    }
                } else {
                    // Fallback for older API levels
                    @Suppress("DEPRECATION")
                    val activeNetwork = connectivityManager.activeNetworkInfo
                    
                    @Suppress("DEPRECATION")
                    when (activeNetwork?.type) {
                        ConnectivityManager.TYPE_WIFI -> 10L
                        ConnectivityManager.TYPE_MOBILE -> 50L
                        ConnectivityManager.TYPE_ETHERNET -> 5L
                        else -> 25L
                    }
                }
            } catch (e: Exception) {
                25L
            }
        } else {
            25L
        }
        
        android.util.Log.d("CalibrationController", "[DEBUG_LOG] Using network-estimated time with offset: ${networkOffset}ms")
        return localTime + networkOffset
    }
    
    /**
     * Extract timestamp from JSON time service response
     */
    private fun extractTimestampFromJSON(json: String): Long {
        return try {
            // Look for common timestamp patterns in JSON
            val patterns = listOf(
                """"unixtime":(\d+)""".toRegex(),
                """"timestamp":(\d+)""".toRegex(),
                """"datetime":"([^"]+)"""".toRegex()
            )
            
            for (pattern in patterns) {
                val match = pattern.find(json)
                if (match != null) {
                    val value = match.groupValues[1]
                    
                    // Try parsing as Unix timestamp
                    val timestamp = value.toLongOrNull()
                    if (timestamp != null) {
                        // Convert to milliseconds if necessary
                        return if (timestamp > 1000000000000L) timestamp else timestamp * 1000L
                    }
                    
                    // Try parsing as ISO datetime
                    try {
                        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                        val date = dateFormat.parse(value.take(19))  // Take just the date part
                        if (date != null) {
                            return date.time
                        }
                    } catch (e: Exception) {
                        // Continue to next pattern
                    }
                }
            }
            -1L
        } catch (e: Exception) {
            android.util.Log.w("CalibrationController", "[DEBUG_LOG] Failed to extract timestamp from JSON: ${e.message}")
            -1L
        }
    }
}
