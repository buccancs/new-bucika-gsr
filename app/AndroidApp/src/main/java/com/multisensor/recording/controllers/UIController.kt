package com.multisensor.recording.controllers

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import android.widget.TextView
import com.multisensor.recording.ui.MainUiState
import com.multisensor.recording.ui.SessionDisplayInfo
import com.multisensor.recording.ui.components.ActionButtonPair
import com.multisensor.recording.ui.components.StatusIndicatorView
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UIController @Inject constructor() {

    companion object {
        private const val UI_PREFS_NAME = "ui_controller_prefs"
        private const val PREFS_NAME = UI_PREFS_NAME
        private const val PREF_THEME_MODE = "theme_mode"
        private const val PREF_ACCESSIBILITY_ENABLED = "accessibility_enabled"
        private const val PREF_COMPONENT_VALIDATION = "component_validation"
        private const val PREF_UI_STATE = "ui_state"
        private const val KEY_LAST_BATTERY_LEVEL = "last_battery_level"
        private const val KEY_PC_CONNECTION_STATUS = "pc_connection_status"
        private const val KEY_SHIMMER_CONNECTION_STATUS = "shimmer_connection_status"
        private const val KEY_THERMAL_CONNECTION_STATUS = "thermal_connection_status"
        private const val KEY_RECORDING_STATE = "recording_state"
        private const val KEY_STREAMING_STATE = "streaming_state"
        private const val KEY_UI_THEME_MODE = "ui_theme_mode"
        private const val KEY_ACCESSIBILITY_MODE = "accessibility_mode"
        private const val KEY_HIGH_CONTRAST_MODE = "high_contrast_mode"
    }

    enum class ThemeMode(val displayName: String) {
        LIGHT("Light"),
        DARK("Dark"),
        AUTO("Auto (System)")
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
    )

    data class AccessibilityConfig(
        val isEnabled: Boolean,
        val increasedTouchTargets: Boolean = false,
        val highContrastMode: Boolean = false,
        val audioFeedback: Boolean = false,
        val hapticFeedback: Boolean = false
    )

    interface UICallback {
        fun onUIComponentsInitialized()
        fun onUIStateUpdated(state: MainUiState)
        fun onUIError(message: String)
        fun updateStatusText(text: String)
        fun showToast(message: String, duration: Int = android.widget.Toast.LENGTH_SHORT)
        fun runOnUiThread(action: () -> Unit)
        fun getContext(): Context
        fun getStatusText(): TextView?
        fun getStartRecordingButton(): View?
        fun getStopRecordingButton(): View?
        fun getCalibrationButton(): View?
        fun getPcConnectionIndicator(): View?
        fun getShimmerConnectionIndicator(): View?
        fun getThermalConnectionIndicator(): View?
        fun getPcConnectionStatus(): TextView?
        fun getShimmerConnectionStatus(): TextView?
        fun getThermalConnectionStatus(): TextView?
        fun getBatteryLevelText(): TextView?
        fun getRecordingIndicator(): View?
        fun getStreamingIndicator(): View?
        fun getStreamingLabel(): View?
        fun getStreamingDebugOverlay(): TextView?
        fun getRequestPermissionsButton(): View?
        fun getShimmerStatusText(): TextView?
    }

    private var callback: UICallback? = null

    private var currentThemeMode = ThemeMode.AUTO
    private var accessibilityConfig = AccessibilityConfig(false)
    private var componentValidationEnabled = true
    private val validationErrors = mutableListOf<String>()

    private lateinit var pcStatusIndicator: StatusIndicatorView
    private lateinit var shimmerStatusIndicator: StatusIndicatorView
    private lateinit var thermalStatusIndicator: StatusIndicatorView
    private lateinit var recordingButtonPair: ActionButtonPair

    private var sharedPreferences: SharedPreferences? = null

    fun setCallback(callback: UICallback) {
        this.callback = callback

        val context = callback.getContext()
        initializeUIStatePersistence(context)
    }

    private fun initializeUIStatePersistence(context: Context) {
        try {
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            android.util.Log.d("UIController", "[DEBUG_LOG] UI state persistence initialized")
        } catch (e: Exception) {
            android.util.Log.e("UIController", "[DEBUG_LOG] Failed to initialize UI state persistence: ${e.message}")
        }
    }

    private fun saveUIState(state: MainUiState) {
        sharedPreferences?.edit()?.apply {
            putInt(KEY_LAST_BATTERY_LEVEL, state.batteryLevel)
            putBoolean(KEY_PC_CONNECTION_STATUS, state.isPcConnected)
            putBoolean(KEY_SHIMMER_CONNECTION_STATUS, state.isShimmerConnected)
            putBoolean(KEY_THERMAL_CONNECTION_STATUS, state.isThermalConnected)
            putBoolean(KEY_RECORDING_STATE, state.isRecording)
            putBoolean(KEY_STREAMING_STATE, state.isStreaming)
            apply()
        }
    }

    fun getSavedUIState(): SavedUIState {
        return sharedPreferences?.let { prefs ->
            SavedUIState(
                lastBatteryLevel = prefs.getInt(KEY_LAST_BATTERY_LEVEL, -1),
                isPcConnected = prefs.getBoolean(KEY_PC_CONNECTION_STATUS, false),
                isShimmerConnected = prefs.getBoolean(KEY_SHIMMER_CONNECTION_STATUS, false),
                isThermalConnected = prefs.getBoolean(KEY_THERMAL_CONNECTION_STATUS, false),
                wasRecording = prefs.getBoolean(KEY_RECORDING_STATE, false),
                wasStreaming = prefs.getBoolean(KEY_STREAMING_STATE, false),
                themeMode = prefs.getString(KEY_UI_THEME_MODE, "default") ?: "default",
                accessibilityMode = prefs.getBoolean(KEY_ACCESSIBILITY_MODE, false),
                highContrastMode = prefs.getBoolean(KEY_HIGH_CONTRAST_MODE, false)
            )
        } ?: SavedUIState()
    }

    fun initializeUIComponents() {
        android.util.Log.d("UIController", "[DEBUG_LOG] Initializing consolidated UI components")

        val context = callback?.getContext() ?: run {
            android.util.Log.e("UIController", "[DEBUG_LOG] Context not available")
            callback?.onUIError("Context not available")
            return
        }

        pcStatusIndicator = StatusIndicatorView(context).apply {
            setStatus(StatusIndicatorView.StatusType.DISCONNECTED, "PC: Waiting for PC...")
            setTextColor(android.R.color.white)
        }

        shimmerStatusIndicator = StatusIndicatorView(context).apply {
            setStatus(StatusIndicatorView.StatusType.DISCONNECTED, "Shimmer: Disconnected")
            setTextColor(android.R.color.white)
        }

        thermalStatusIndicator = StatusIndicatorView(context).apply {
            setStatus(StatusIndicatorView.StatusType.DISCONNECTED, "Thermal: Disconnected")
            setTextColor(android.R.color.white)
        }

        recordingButtonPair = ActionButtonPair(context).apply {
            setButtons("Start Recording", "Stop Recording")
            setButtonsEnabled(true, false)
        }

        callback?.onUIComponentsInitialized()
        android.util.Log.d("UIController", "[DEBUG_LOG] Consolidated UI components initialized successfully")
    }

    fun updateUIFromState(state: MainUiState) {
        android.util.Log.d("UIController", "[DEBUG_LOG] Updating UI from centralized state")

        callback?.runOnUiThread {
            saveUIState(state)

            callback?.getStatusText()?.text = state.statusText

            if (::recordingButtonPair.isInitialized) {
                recordingButtonPair.setButtonsEnabled(state.canStartRecording, state.canStopRecording)
            }

            callback?.getStartRecordingButton()?.isEnabled = state.canStartRecording
            callback?.getStopRecordingButton()?.isEnabled = state.canStopRecording

            callback?.getCalibrationButton()?.isEnabled = state.canRunCalibration

            updateStatusIndicatorsWithAccessibility(state)

            updateConnectionIndicator(callback?.getPcConnectionIndicator(), state.isPcConnected)
            updateConnectionIndicator(callback?.getShimmerConnectionIndicator(), state.isShimmerConnected)
            updateConnectionIndicator(callback?.getThermalConnectionIndicator(), state.isThermalConnected)

            callback?.getPcConnectionStatus()?.text =
                "PC: ${if (state.isPcConnected) "Connected" else "Waiting for PC..."}"
            callback?.getShimmerConnectionStatus()?.text =
                "Shimmer: ${if (state.isShimmerConnected) "Connected" else "Disconnected"}"
            callback?.getThermalConnectionStatus()?.text =
                "Thermal: ${if (state.isThermalConnected) "Connected" else "Disconnected"}"

            updateBatteryLevelDisplay(state.batteryLevel)

            updateRecordingIndicator(state.isRecording)

            updateStreamingIndicator(
                state.isStreaming,
                state.streamingFrameRate.toInt(),
                state.streamingDataSize.toString()
            )

            callback?.getRequestPermissionsButton()?.visibility =
                if (state.showPermissionsButton) View.VISIBLE else View.GONE

            state.errorMessage?.let { errorMsg ->
                if (state.showErrorDialog) {
                    callback?.showToast(errorMsg, android.widget.Toast.LENGTH_LONG)
                    android.util.Log.d("UIController", "[DEBUG_LOG] Error displayed, clearing from state")
                }
            }

            state.currentSessionInfo?.let { sessionInfo ->
                updateSessionInfoDisplay(sessionInfo)
            }

            updateShimmerStatusWithAccessibility(state)

            callback?.onUIStateUpdated(state)
        }
    }

    private fun updateStatusIndicatorsWithAccessibility(state: MainUiState) {
        val savedState = getSavedUIState()
        val isHighContrast = savedState.highContrastMode
        val isAccessibilityMode = savedState.accessibilityMode

        if (::pcStatusIndicator.isInitialized) {
            pcStatusIndicator.setStatus(
                if (state.isPcConnected) StatusIndicatorView.StatusType.CONNECTED else StatusIndicatorView.StatusType.DISCONNECTED,
                "PC: ${if (state.isPcConnected) "Connected" else "Waiting for PC..."}"
            )

            if (isAccessibilityMode) {
                pcStatusIndicator.contentDescription =
                    "PC connection status: ${if (state.isPcConnected) "Connected" else "Disconnected"}"
            }
        }

        if (::shimmerStatusIndicator.isInitialized) {
            shimmerStatusIndicator.setStatus(
                if (state.isShimmerConnected) StatusIndicatorView.StatusType.CONNECTED else StatusIndicatorView.StatusType.DISCONNECTED,
                "Shimmer: ${if (state.isShimmerConnected) "Connected" else "Disconnected"}"
            )

            if (isAccessibilityMode) {
                shimmerStatusIndicator.contentDescription =
                    "Shimmer sensor status: ${if (state.isShimmerConnected) "Connected" else "Disconnected"}"
            }
        }

        if (::thermalStatusIndicator.isInitialized) {
            thermalStatusIndicator.setStatus(
                if (state.isThermalConnected) StatusIndicatorView.StatusType.CONNECTED else StatusIndicatorView.StatusType.DISCONNECTED,
                "Thermal: ${if (state.isThermalConnected) "Connected" else "Disconnected"}"
            )

            if (isAccessibilityMode) {
                thermalStatusIndicator.contentDescription =
                    "Thermal camera status: ${if (state.isThermalConnected) "Connected" else "Disconnected"}"
            }
        }
    }

    private fun updateBatteryLevelDisplay(batteryLevel: Int) {
        val savedState = getSavedUIState()
        val isHighContrast = savedState.highContrastMode

        val batteryText = if (batteryLevel >= 0) {
            "Battery: $batteryLevel%"
        } else {
            "Battery: ---%"
        }

        val textColor = when {
            batteryLevel < 0 -> Color.WHITE
            isHighContrast -> {
                when {
                    batteryLevel > 50 -> Color.GREEN
                    batteryLevel > 20 -> Color.YELLOW
                    else -> Color.RED
                }
            }

            else -> {
                when {
                    batteryLevel > 50 -> Color.parseColor("#4CAF50")
                    batteryLevel > 20 -> Color.parseColor("#FF9800")
                    else -> Color.parseColor("#F44336")
                }
            }
        }

        callback?.getBatteryLevelText()?.let { textView ->
            textView.text = batteryText
            textView.setTextColor(textColor)

            if (savedState.accessibilityMode) {
                textView.contentDescription = "Battery level: $batteryLevel percent"
            }
        }
    }

    private fun updateShimmerStatusWithAccessibility(state: MainUiState) {
        val savedState = getSavedUIState()
        val isHighContrast = savedState.highContrastMode

        val shimmerStatusText = when {
            state.shimmerDeviceInfo != null -> {
                "Shimmer GSR: ${state.shimmerDeviceInfo.deviceId} - Connected"
            }

            state.isShimmerConnected -> "Shimmer GSR: Connected"
            else -> "Shimmer GSR: Disconnected"
        }

        val textColor = if (isHighContrast) {
            if (state.isShimmerConnected) Color.GREEN else Color.RED
        } else {
            if (state.isShimmerConnected) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
        }

        callback?.getShimmerStatusText()?.let { textView ->
            textView.text = shimmerStatusText
            textView.setTextColor(textColor)

            if (savedState.accessibilityMode) {
                textView.contentDescription =
                    "Shimmer GSR sensor: ${if (state.isShimmerConnected) "Connected" else "Disconnected"}"
            }
        }
    }

    private fun updateConnectionIndicator(indicator: View?, isConnected: Boolean) {
        val savedState = getSavedUIState()
        val isHighContrast = savedState.highContrastMode

        val color = if (isHighContrast) {
            if (isConnected) Color.GREEN else Color.RED
        } else {
            if (isConnected) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
        }

        indicator?.setBackgroundColor(color)
    }

    private fun updateRecordingIndicator(isRecording: Boolean) {
        val savedState = getSavedUIState()
        val isHighContrast = savedState.highContrastMode

        val color = if (isHighContrast) {
            if (isRecording) Color.RED else Color.GRAY
        } else {
            if (isRecording) Color.parseColor("#F44336") else Color.parseColor("#9E9E9E")
        }

        callback?.getRecordingIndicator()?.setBackgroundColor(color)
    }

    private fun updateStreamingIndicator(isStreaming: Boolean, frameRate: Int, dataSize: String) {
        val savedState = getSavedUIState()
        val isHighContrast = savedState.highContrastMode

        val color = if (isHighContrast) {
            if (isStreaming) Color.GREEN else Color.GRAY
        } else {
            if (isStreaming) Color.parseColor("#4CAF50") else Color.parseColor("#9E9E9E")
        }

        callback?.getStreamingIndicator()?.setBackgroundColor(color)

        if (isStreaming && frameRate > 0) {
            callback?.getStreamingDebugOverlay()?.let { overlay ->
                overlay.text = "Streaming: ${frameRate}fps ($dataSize)"
                overlay.visibility = View.VISIBLE

                if (savedState.accessibilityMode) {
                    overlay.contentDescription =
                        "Currently streaming at $frameRate frames per second, data size $dataSize"
                }
            }
            callback?.getStreamingLabel()?.visibility = View.VISIBLE
        } else {
            callback?.getStreamingDebugOverlay()?.visibility = View.GONE
            callback?.getStreamingLabel()?.visibility = View.GONE
        }
    }

    private fun updateSessionInfoDisplay(sessionInfo: SessionDisplayInfo?) {
        if (sessionInfo != null) {
            val sessionSummary = buildString {
                append("Session ${sessionInfo.sessionId}")
                append(" - ${sessionInfo.status}")

                if (sessionInfo.status == "Active") {
                    append(" [Recording in progress]")
                } else {
                    append(" [Session completed]")
                }
            }

            callback?.updateStatusText(sessionSummary)

            android.util.Log.d("UIController", "SessionInfo updated: $sessionSummary")
        } else {
            android.util.Log.d("UIController", "No active session info to display")
        }
    }

    fun getConsolidatedComponents(): ConsolidatedUIComponents {
        return ConsolidatedUIComponents(
            pcStatusIndicator = if (::pcStatusIndicator.isInitialized) pcStatusIndicator else null,
            shimmerStatusIndicator = if (::shimmerStatusIndicator.isInitialized) shimmerStatusIndicator else null,
            thermalStatusIndicator = if (::thermalStatusIndicator.isInitialized) thermalStatusIndicator else null,
            recordingButtonPair = if (::recordingButtonPair.isInitialized) recordingButtonPair else null
        )
    }

    fun setThemeMode(themeMode: String) {
        sharedPreferences?.edit()?.apply {
            putString(KEY_UI_THEME_MODE, themeMode)
            apply()
        }
        android.util.Log.d("UIController", "[DEBUG_LOG] UI theme mode set to: $themeMode")
    }

    fun setAccessibilityMode(enabled: Boolean) {
        sharedPreferences?.edit()?.apply {
            putBoolean(KEY_ACCESSIBILITY_MODE, enabled)
            apply()
        }
        android.util.Log.d("UIController", "[DEBUG_LOG] Accessibility mode set to: $enabled")
    }

    fun setHighContrastMode(enabled: Boolean) {
        sharedPreferences?.edit()?.apply {
            putBoolean(KEY_HIGH_CONTRAST_MODE, enabled)
            apply()
        }
        android.util.Log.d("UIController", "[DEBUG_LOG] High contrast mode set to: $enabled")
    }

    data class ConsolidatedUIComponents(
        val pcStatusIndicator: StatusIndicatorView?,
        val shimmerStatusIndicator: StatusIndicatorView?,
        val thermalStatusIndicator: StatusIndicatorView?,
        val recordingButtonPair: ActionButtonPair?
    )

    data class SavedUIState(
        val lastBatteryLevel: Int = -1,
        val isPcConnected: Boolean = false,
        val isShimmerConnected: Boolean = false,
        val isThermalConnected: Boolean = false,
        val wasRecording: Boolean = false,
        val wasStreaming: Boolean = false,
        val themeMode: String = "default",
        val accessibilityMode: Boolean = false,
        val highContrastMode: Boolean = false
    )

    fun getUIStatus(): String {
        val savedState = getSavedUIState()
        return buildString {
            append("UI Controller Status:\n")
            append("- PC Status Indicator: ${if (::pcStatusIndicator.isInitialized) "Initialized" else "Not initialized"}\n")
            append("- Shimmer Status Indicator: ${if (::shimmerStatusIndicator.isInitialized) "Initialized" else "Not initialized"}\n")
            append("- Thermal Status Indicator: ${if (::thermalStatusIndicator.isInitialized) "Initialized" else "Not initialized"}\n")
            append("- Recording Button Pair: ${if (::recordingButtonPair.isInitialized) "Initialized" else "Not initialized"}\n")
            append("- State Persistence: ${if (sharedPreferences != null) "Enabled" else "Disabled"}\n")
            append("- Theme Mode: ${savedState.themeMode}\n")
            append("- Accessibility Mode: ${savedState.accessibilityMode}\n")
            append("- High Contrast Mode: ${savedState.highContrastMode}\n")
            append("- Last Battery Level: ${savedState.lastBatteryLevel}%\n")
            append("- Callback Set: ${callback != null}")
        }
    }

    fun resetState() {
        sharedPreferences?.edit()?.clear()?.apply()
        android.util.Log.d("UIController", "[DEBUG_LOG] UI controller state reset and persisted state cleared")
    }

    fun cleanup() {
        try {
            callback = null
            sharedPreferences = null
            android.util.Log.d("UIController", "[DEBUG_LOG] UI controller resources cleaned up")
        } catch (e: Exception) {
            android.util.Log.w("UIController", "[DEBUG_LOG] Error during UI cleanup: ${e.message}")
        }
    }

    fun setRecordingButtonListeners(startAction: (View) -> Unit, stopAction: (View) -> Unit) {
        if (::recordingButtonPair.isInitialized) {
            recordingButtonPair.setOnClickListeners(startAction, stopAction)
            android.util.Log.d("UIController", "[DEBUG_LOG] Recording button listeners set")
        } else {
            android.util.Log.w(
                "UIController",
                "[DEBUG_LOG] Cannot set listeners - recording button pair not initialized"
            )
        }
    }

    fun applyThemeFromPreferences() {
        val savedState = getSavedUIState()
        android.util.Log.d(
            "UIController",
            "[DEBUG_LOG] Applying UI theme: ${savedState.themeMode}, accessibility: ${savedState.accessibilityMode}, high contrast: ${savedState.highContrastMode}"
        )

        callback?.let { cb ->
            if (cb is Context) {
                android.util.Log.d("UIController", "[DEBUG_LOG] Theme preferences ready for application")
            }
        }
    }

    fun validateUIComponents(): UIValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        try {
            if (callback == null) {
                errors.add("UI callback is null - UI operations will fail")
            } else {
                try {
                    callback?.getContext()
                } catch (e: Exception) {
                    errors.add("Context not available: ${e.message}")
                }

                if (callback?.getStatusText() == null) {
                    warnings.add("Status text view is null - status updates may not display")
                }

                if (callback?.getStartRecordingButton() == null) {
                    errors.add("Start recording button is null - recording cannot be initiated")
                }

                if (callback?.getStopRecordingButton() == null) {
                    errors.add("Stop recording button is null - recording cannot be stopped")
                }

                if (callback?.getBatteryLevelText() == null) {
                    warnings.add("Battery level text is null - battery status will not display")
                }

                if (callback?.getPcConnectionIndicator() == null) {
                    warnings.add("PC connection indicator is null - PC status will not display")
                }

                if (callback?.getShimmerConnectionIndicator() == null) {
                    warnings.add("Shimmer connection indicator is null - Shimmer status will not display")
                }

                if (callback?.getThermalConnectionIndicator() == null) {
                    warnings.add("Thermal connection indicator is null - thermal camera status will not display")
                }
            }

            if (::pcStatusIndicator.isInitialized) {
                try {
                    pcStatusIndicator.contentDescription
                } catch (e: Exception) {
                    warnings.add("PC status indicator may be corrupted: ${e.message}")
                }
            } else {
                warnings.add("PC status indicator not initialized - using legacy indicator")
            }

            if (::shimmerStatusIndicator.isInitialized) {
                try {
                    shimmerStatusIndicator.contentDescription
                } catch (e: Exception) {
                    warnings.add("Shimmer status indicator may be corrupted: ${e.message}")
                }
            } else {
                warnings.add("Shimmer status indicator not initialized - using legacy indicator")
            }

            if (::thermalStatusIndicator.isInitialized) {
                try {
                    thermalStatusIndicator.contentDescription
                } catch (e: Exception) {
                    warnings.add("Thermal status indicator may be corrupted: ${e.message}")
                }
            } else {
                warnings.add("Thermal status indicator not initialized - using legacy indicator")
            }

            if (::recordingButtonPair.isInitialized) {
                try {
                    recordingButtonPair.contentDescription
                } catch (e: Exception) {
                    warnings.add("Recording button pair may be corrupted: ${e.message}")
                }
            } else {
                warnings.add("Recording button pair not initialized - using legacy buttons")
            }

            if (sharedPreferences == null) {
                warnings.add("SharedPreferences not available - UI state will not persist")
            }

        } catch (e: Exception) {
            errors.add("Critical error during UI validation: ${e.message}")
        }

        return UIValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            componentCount = getComponentCount(),
            validationTimestamp = System.currentTimeMillis()
        )
    }

    fun recoverFromUIErrors(): UIRecoveryResult {
        val recoveryActions = mutableListOf<String>()
        var success = true

        try {
            android.util.Log.d("UIController", "[DEBUG_LOG] Attempting UI error recovery")

            if (callback != null) {
                try {
                    initializeUIComponents()
                    recoveryActions.add("Re-initialized UI components")
                } catch (e: Exception) {
                    success = false
                    recoveryActions.add("Failed to re-initialize UI components: ${e.message}")
                }

                try {
                    val savedState = getSavedUIState()
                    if (savedState.lastBatteryLevel >= 0) {
                        recoveryActions.add("Restored UI state from preferences")
                    }
                } catch (e: Exception) {
                    recoveryActions.add("Could not restore UI state: ${e.message}")
                }

                try {
                    applyThemeFromPreferences()
                    recoveryActions.add("Applied theme preferences")
                } catch (e: Exception) {
                    recoveryActions.add("Could not apply theme preferences: ${e.message}")
                }
            } else {
                success = false
                recoveryActions.add("Cannot recover - UI callback is null")
            }

        } catch (e: Exception) {
            success = false
            recoveryActions.add("Critical recovery error: ${e.message}")
        }

        return UIRecoveryResult(
            success = success,
            recoveryActions = recoveryActions,
            recoveryTimestamp = System.currentTimeMillis()
        )
    }

    fun validateUIState(state: MainUiState): UIStateValidationResult {
        val issues = mutableListOf<String>()
        val suggestions = mutableListOf<String>()

        try {
            if (state.isRecording && state.canStartRecording) {
                issues.add("Inconsistent recording state: recording is active but start button is enabled")
            }

            if (!state.isRecording && state.canStopRecording) {
                issues.add("Inconsistent recording state: recording is not active but stop button is enabled")
            }

            if (state.isRecording && state.canRunCalibration) {
                issues.add("Inconsistent state: recording is active but calibration is allowed")
            }

            if (state.batteryLevel < 0 && state.batteryLevel != -1) {
                issues.add("Invalid battery level: ${state.batteryLevel} (should be -1 for unknown or 0-100)")
            }

            if (state.batteryLevel > 100) {
                issues.add("Invalid battery level: ${state.batteryLevel} (should not exceed 100)")
            }

            if (state.batteryLevel in 1..20) {
                suggestions.add("Low battery level detected: ${state.batteryLevel}% - consider showing warning")
            }

            if (state.isStreaming && state.streamingFrameRate <= 0) {
                issues.add("Inconsistent streaming state: streaming is active but frame rate is ${state.streamingFrameRate}")
            }

            if (!state.isStreaming && state.streamingFrameRate > 0) {
                suggestions.add("Streaming not active but frame rate is ${state.streamingFrameRate} - may indicate stopped streaming")
            }

            if (state.isRecording && !state.isPcConnected && !state.isShimmerConnected && !state.isThermalConnected) {
                issues.add("Recording is active but no devices are connected - this may indicate a problem")
            }

            if (state.canStartRecording && !state.isPcConnected && !state.isShimmerConnected && !state.isThermalConnected) {
                suggestions.add("Recording is enabled but no devices connected - user may need to connect devices first")
            }

            if (state.errorMessage != null && !state.showErrorDialog) {
                suggestions.add("Error message present but dialog not shown - error may not be visible to user")
            }

            if (state.showErrorDialog && state.errorMessage.isNullOrBlank()) {
                issues.add("Error dialog should be shown but no error message provided")
            }

            if (state.currentSessionInfo != null && state.currentSessionInfo.sessionId.isBlank()) {
                issues.add("Session info provided but session ID is blank")
            }

            if (state.statusText.isBlank()) {
                suggestions.add("Status text is blank - consider providing status information")
            }

        } catch (e: Exception) {
            issues.add("Critical error during state validation: ${e.message}")
        }

        return UIStateValidationResult(
            isValid = issues.isEmpty(),
            issues = issues,
            suggestions = suggestions,
            validationTimestamp = System.currentTimeMillis()
        )
    }

    private fun getComponentCount(): Int {
        var count = 0
        callback?.let { cb ->
            if (cb.getStatusText() != null) count++
            if (cb.getStartRecordingButton() != null) count++
            if (cb.getStopRecordingButton() != null) count++
            if (cb.getCalibrationButton() != null) count++
            if (cb.getPcConnectionIndicator() != null) count++
            if (cb.getShimmerConnectionIndicator() != null) count++
            if (cb.getThermalConnectionIndicator() != null) count++
            if (cb.getBatteryLevelText() != null) count++
            if (cb.getRecordingIndicator() != null) count++
            if (cb.getStreamingIndicator() != null) count++
            if (cb.getStreamingLabel() != null) count++
            if (cb.getStreamingDebugOverlay() != null) count++
            if (cb.getRequestPermissionsButton() != null) count++
            if (cb.getShimmerStatusText() != null) count++
        }
        return count
    }

    fun enableAccessibilityFeatures() {
        try {
            setAccessibilityMode(true)

            val savedState = getSavedUIState()
            if (savedState.accessibilityMode) {
                if (::pcStatusIndicator.isInitialized) {
                    pcStatusIndicator.contentDescription = "PC connection status indicator"
                }
                if (::shimmerStatusIndicator.isInitialized) {
                    shimmerStatusIndicator.contentDescription = "Shimmer sensor status indicator"
                }
                if (::thermalStatusIndicator.isInitialized) {
                    thermalStatusIndicator.contentDescription = "Thermal camera status indicator"
                }
                if (::recordingButtonPair.isInitialized) {
                    recordingButtonPair.contentDescription = "Recording control buttons"
                }
            }

            android.util.Log.d("UIController", "[DEBUG_LOG] Accessibility features enabled")
        } catch (e: Exception) {
            android.util.Log.e("UIController", "[DEBUG_LOG] Failed to enable accessibility features: ${e.message}")
        }
    }

    fun applyDynamicTheme(themeMode: String, highContrast: Boolean = false): Boolean {
        return try {
            val validThemes = listOf("light", "dark", "auto", "default")
            if (themeMode !in validThemes) {
                android.util.Log.w("UIController", "[DEBUG_LOG] Invalid theme mode: $themeMode, using default")
                setThemeMode("default")
            } else {
                setThemeMode(themeMode)
            }

            setHighContrastMode(highContrast)

            if (::pcStatusIndicator.isInitialized || ::shimmerStatusIndicator.isInitialized ||
                ::thermalStatusIndicator.isInitialized
            ) {
                android.util.Log.d("UIController", "[DEBUG_LOG] Updating component themes")
                val currentState = MainUiState(statusText = "Theme updated")
                updateUIFromState(currentState)
            }

            android.util.Log.d(
                "UIController",
                "[DEBUG_LOG] Dynamic theme applied: $themeMode, high contrast: $highContrast"
            )
            true
        } catch (e: Exception) {
            android.util.Log.e("UIController", "[DEBUG_LOG] Failed to apply dynamic theme: ${e.message}")
            false
        }
    }

    data class UIValidationResult(
        val isValid: Boolean,
        val errors: List<String>,
        val warnings: List<String>,
        val componentCount: Int,
        val validationTimestamp: Long
    )

    data class UIRecoveryResult(
        val success: Boolean,
        val recoveryActions: List<String>,
        val recoveryTimestamp: Long
    )

    data class UIStateValidationResult(
        val isValid: Boolean,
        val issues: List<String>,
        val suggestions: List<String>,
        val validationTimestamp: Long
    )

    fun setThemeMode(themeMode: ThemeMode) {
        currentThemeMode = themeMode

        callback?.getContext()?.let { context ->
            val prefs = context.getSharedPreferences(UI_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(PREF_THEME_MODE, themeMode.name).apply()

            applyThemeMode(context, themeMode)
        }

        android.util.Log.d("UIController", "[DEBUG_LOG] Theme mode set to: ${themeMode.displayName}")
    }

    private fun applyThemeMode(context: Context, themeMode: ThemeMode) {
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        when (themeMode) {
            ThemeMode.LIGHT -> {
                updateComponentColors(false)
            }

            ThemeMode.DARK -> {
                updateComponentColors(true)
            }

            ThemeMode.AUTO -> {
                val isDarkMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES
                updateComponentColors(isDarkMode)
            }
        }

        callback?.updateStatusText("Theme applied: ${themeMode.displayName}")
    }

    private fun updateComponentColors(isDarkMode: Boolean) {
        val backgroundColor = if (isDarkMode) Color.parseColor("#1E1E1E") else Color.parseColor("#FFFFFF")
        val textColor = if (isDarkMode) Color.parseColor("#FFFFFF") else Color.parseColor("#000000")
        val accentColor = if (isDarkMode) Color.parseColor("#BB86FC") else Color.parseColor("#6200EE")

        callback?.getStatusText()?.setTextColor(textColor)

        android.util.Log.d(
            "UIController",
            "[DEBUG_LOG] Component colors updated for ${if (isDarkMode) "dark" else "light"} mode"
        )
    }

    fun configureAccessibility(config: AccessibilityConfig) {
        accessibilityConfig = config

        callback?.getContext()?.let { context ->
            val prefs = context.getSharedPreferences(UI_PREFS_NAME, Context.MODE_PRIVATE)
            val configJson = JSONObject().apply {
                put("isEnabled", config.isEnabled)
                put("increasedTouchTargets", config.increasedTouchTargets)
                put("highContrastMode", config.highContrastMode)
                put("audioFeedback", config.audioFeedback)
                put("hapticFeedback", config.hapticFeedback)
            }
            prefs.edit().putString(PREF_ACCESSIBILITY_ENABLED, configJson.toString()).apply()

            applyAccessibilitySettings(context, config)
        }

        android.util.Log.d("UIController", "[DEBUG_LOG] Accessibility configured: enabled=${config.isEnabled}")
    }

    private fun applyAccessibilitySettings(context: Context, config: AccessibilityConfig) {
        if (!config.isEnabled) return

        if (config.increasedTouchTargets) {
            val buttons = listOf(
                callback?.getStartRecordingButton(),
                callback?.getStopRecordingButton(),
                callback?.getCalibrationButton()
            )

            buttons.filterNotNull().forEach { button ->
                val layoutParams = button.layoutParams
                if (layoutParams != null) {
                    val minSize = (48 * context.resources.displayMetrics.density).toInt()
                    layoutParams.width = maxOf(layoutParams.width, minSize)
                    layoutParams.height = maxOf(layoutParams.height, minSize)
                    button.layoutParams = layoutParams
                }
            }
        }

        if (config.highContrastMode) {
            updateComponentColors(true)
        }

        android.util.Log.d("UIController", "[DEBUG_LOG] Accessibility settings applied")
    }

    fun setComponentValidationEnabled(enabled: Boolean) {
        componentValidationEnabled = enabled

        callback?.getContext()?.let { context ->
            val prefs = context.getSharedPreferences(UI_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(PREF_COMPONENT_VALIDATION, enabled).apply()
        }

        android.util.Log.d("UIController", "[DEBUG_LOG] Component validation: $enabled")
    }

    fun handleUIError(error: String, exception: Exception? = null) {
        validationErrors.add(error)

        android.util.Log.e("UIController", "[DEBUG_LOG] UI Error: $error", exception)

        try {
            when {
                error.contains("component not found", ignoreCase = true) -> {
                    callback?.showToast("UI component missing - attempting recovery")
                    callback?.onUIComponentsInitialized()
                }

                error.contains("theme", ignoreCase = true) -> {
                    setThemeMode(ThemeMode.AUTO)
                }

                error.contains("accessibility", ignoreCase = true) -> {
                    configureAccessibility(AccessibilityConfig(false))
                }
            }
        } catch (recoveryException: Exception) {
            android.util.Log.e("UIController", "[DEBUG_LOG] UI error recovery failed", recoveryException)
        }

        callback?.onUIError(error)
    }

    fun getCurrentThemeMode(): ThemeMode = currentThemeMode

    fun getAccessibilityConfig(): AccessibilityConfig = accessibilityConfig

    fun getValidationErrors(): List<String> = validationErrors.toList()

    fun initializeEnhancedUI(context: Context) {
        val prefs = context.getSharedPreferences(UI_PREFS_NAME, Context.MODE_PRIVATE)

        val themeModeString = prefs.getString(PREF_THEME_MODE, ThemeMode.AUTO.name)
        try {
            currentThemeMode = ThemeMode.valueOf(themeModeString ?: ThemeMode.AUTO.name)
            applyThemeMode(context, currentThemeMode)
        } catch (e: IllegalArgumentException) {
            currentThemeMode = ThemeMode.AUTO
        }

        val accessibilityJson = prefs.getString(PREF_ACCESSIBILITY_ENABLED, null)
        if (accessibilityJson != null) {
            try {
                val jsonObject = JSONObject(accessibilityJson)
                accessibilityConfig = AccessibilityConfig(
                    isEnabled = jsonObject.getBoolean("isEnabled"),
                    increasedTouchTargets = jsonObject.getBoolean("increasedTouchTargets"),
                    highContrastMode = jsonObject.getBoolean("highContrastMode"),
                    audioFeedback = jsonObject.getBoolean("audioFeedback"),
                    hapticFeedback = jsonObject.getBoolean("hapticFeedback")
                )
                applyAccessibilitySettings(context, accessibilityConfig)
            } catch (e: Exception) {
                android.util.Log.e("UIController", "[DEBUG_LOG] Failed to restore accessibility config", e)
            }
        }

        componentValidationEnabled = prefs.getBoolean(PREF_COMPONENT_VALIDATION, true)

        android.util.Log.d("UIController", "[DEBUG_LOG] Enhanced UI features initialized")
    }
}
