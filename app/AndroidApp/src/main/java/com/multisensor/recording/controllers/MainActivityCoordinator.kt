package com.multisensor.recording.controllers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.os.Build
import android.view.TextureView
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleCoroutineScope
import com.multisensor.recording.ui.MainViewModel
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainActivityCoordinator @Inject constructor(
    private val permissionController: PermissionController,
    private val usbController: UsbController,
    private val shimmerController: ShimmerController,
    private val recordingController: RecordingController,
    private val calibrationController: CalibrationController,
    private val networkController: NetworkController,
    private val statusDisplayController: StatusDisplayController,
    private val uiController: UIController
) {

    companion object {
        private const val COORDINATOR_PREFS_NAME = "coordinator_state"
        private const val PREF_COORDINATOR_STATE = "coordinator_state_json"
        private const val PREF_FEATURE_DEPENDENCIES = "feature_dependencies"
        private const val PREF_ERROR_RECOVERY_STATE = "error_recovery_state"
    }

    data class CoordinatorState(
        val isInitialized: Boolean,
        val activeFeatures: Set<String>,
        val lastInitTimestamp: Long,
        val errorCount: Int = 0,
        val lastErrorTimestamp: Long = 0
    )

    data class DependencyValidationResult(
        val isValid: Boolean,
        val missingDependencies: List<String>,
        val conflictingFeatures: List<String>
    )

    interface CoordinatorCallback {
        fun updateStatusText(text: String)
        fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT)
        fun runOnUiThread(action: () -> Unit)
        fun getContentView(): View
        fun getStreamingIndicator(): View?
        fun getStreamingLabel(): View?
        fun getStreamingDebugOverlay(): TextView?
        fun showPermissionButton(show: Boolean)

        fun getContext(): android.content.Context
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
        fun getRequestPermissionsButton(): View?
        fun getShimmerStatusText(): TextView?
    }

    private var callback: CoordinatorCallback? = null
    private var isInitialized = false
    private var currentState: CoordinatorState? = null
    private val activeFeatures = mutableSetOf<String>()
    private var errorRecoveryAttempts = 0

    fun initialize(callback: CoordinatorCallback) {
        android.util.Log.d(
            "MainActivityCoordinator",
            "[DEBUG_LOG] Initializing coordinator with all feature controllers"
        )

        this.callback = callback

        try {
            currentState = restoreCoordinatorState(callback.getContext())
            if (currentState != null) {
                android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Restored previous coordinator state")
                activeFeatures.addAll(currentState!!.activeFeatures)
            }

            setupPermissionController()
            setupUsbController()
            setupShimmerController()
            setupRecordingController()
            setupCalibrationController()
            setupNetworkController()
            setupStatusDisplayController()
            setupUIController()
            setupMenuController()

            currentState = CoordinatorState(
                isInitialized = true,
                activeFeatures = activeFeatures.toSet(),
                lastInitTimestamp = System.currentTimeMillis(),
                errorCount = currentState?.errorCount ?: 0,
                lastErrorTimestamp = currentState?.lastErrorTimestamp ?: 0
            )

            saveCoordinatorState(callback.getContext(), currentState!!)

            isInitialized = true
            android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Coordinator initialization complete")
        } catch (e: Exception) {
            android.util.Log.e("MainActivityCoordinator", "[DEBUG_LOG] Coordinator initialization failed: ${e.message}")
            handleCoordinatorError("initialization", e)
        }
    }

    private fun setupPermissionController() {
        permissionController.setCallback(object : PermissionController.PermissionCallback {
            override fun onAllPermissionsGranted() {
                android.util.Log.d(
                    "MainActivityCoordinator",
                    "[DEBUG_LOG] All permissions granted - initializing recording system"
                )
            }

            override fun onPermissionsTemporarilyDenied(
                deniedPermissions: List<String>,
                grantedCount: Int,
                totalCount: Int
            ) {
                callback?.updateStatusText("Permissions: $grantedCount/$totalCount granted - Some permissions denied")
            }

            override fun onPermissionsPermanentlyDenied(deniedPermissions: List<String>) {
                callback?.updateStatusText("Permissions required - Please enable in Settings")
            }

            override fun onPermissionCheckStarted() {
                callback?.updateStatusText("Checking permissions...")
            }

            override fun onPermissionRequestCompleted() {
            }

            override fun updateStatusText(text: String) {
                callback?.updateStatusText(text)
            }

            override fun showPermissionButton(show: Boolean) {
                callback?.showPermissionButton(show)
            }
        })
    }

    private fun setupUsbController() {
        usbController.setCallback(object : UsbController.UsbCallback {
            override fun onSupportedDeviceAttached(device: UsbDevice) {
                android.util.Log.d(
                    "MainActivityCoordinator",
                    "[DEBUG_LOG] Supported USB device attached - coordinating with other controllers"
                )
                callback?.updateStatusText("Topdon thermal camera connected - Ready for recording")
            }

            override fun onUnsupportedDeviceAttached(device: UsbDevice) {
                android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Unsupported USB device attached")
            }

            override fun onDeviceDetached(device: UsbDevice) {
                callback?.updateStatusText("USB device disconnected")
            }

            override fun onUsbError(message: String) {
                callback?.showToast("USB Error: $message", Toast.LENGTH_LONG)
            }

            override fun updateStatusText(text: String) {
                callback?.updateStatusText(text)
            }

            override fun initializeRecordingSystem() {
            }

            override fun areAllPermissionsGranted(): Boolean {
                return callback?.getContext()?.let { context ->
                    permissionController.areAllPermissionsGranted(context)
                } ?: false
            }

            override fun getContext(): android.content.Context {
                return callback?.getContext() ?: throw IllegalStateException("Context not available")
            }

            override fun showToast(message: String, duration: Int) {
                callback?.showToast(message, duration)
            }
        })
    }

    private fun setupShimmerController() {
        shimmerController.setCallback(object : ShimmerController.ShimmerCallback {
            override fun onDeviceSelected(address: String, name: String) {
                callback?.updateStatusText("Shimmer device selected: $name")
            }

            override fun onDeviceSelectionCancelled() {
                callback?.updateStatusText("Shimmer device selection cancelled")
            }

            override fun onConnectionStatusChanged(connected: Boolean) {
                val status = if (connected) "connected" else "disconnected"
                callback?.updateStatusText("Shimmer device $status")
            }

            override fun onConfigurationComplete() {
                callback?.showToast("Shimmer configuration completed")
            }

            override fun onShimmerError(message: String) {
                callback?.showToast("Shimmer Error: $message", Toast.LENGTH_LONG)
            }

            override fun updateStatusText(text: String) {
                callback?.updateStatusText(text)
            }

            override fun showToast(message: String, duration: Int) {
                callback?.showToast(message, duration)
            }

            override fun runOnUiThread(action: () -> Unit) {
                callback?.runOnUiThread(action)
            }
        })
    }

    private fun setupRecordingController() {
        recordingController.setCallback(object : RecordingController.RecordingCallback {
            override fun onRecordingInitialized() {
                callback?.updateStatusText("Recording system initialized - Ready to record")
            }

            override fun onRecordingStarted() {
                callback?.updateStatusText("Recording in progress...")
                callback?.getContext()?.let { context ->
                    networkController.updateStreamingUI(context, true)
                }
            }

            override fun onRecordingStopped() {
                callback?.updateStatusText("Recording stopped - Processing data...")
                callback?.getContext()?.let { context ->
                    networkController.updateStreamingUI(context, false)
                }
            }

            override fun onRecordingError(message: String) {
                callback?.showToast("Recording Error: $message", Toast.LENGTH_LONG)
            }

            override fun updateStatusText(text: String) {
                callback?.updateStatusText(text)
            }

            override fun showToast(message: String, duration: Int) {
                callback?.showToast(message, duration)
            }
        })
    }

    private fun setupCalibrationController() {
        calibrationController.setCallback(object : CalibrationController.CalibrationCallback {
            override fun onCalibrationStarted() {
                callback?.updateStatusText("Calibration in progress...")
            }

            override fun onCalibrationCompleted(calibrationId: String) {
                callback?.updateStatusText("Calibration completed - ID: $calibrationId")
            }

            override fun onCalibrationFailed(errorMessage: String) {
                callback?.showToast("Calibration failed: $errorMessage", Toast.LENGTH_LONG)
            }

            override fun onSyncTestCompleted(success: Boolean, message: String) {
                val status = if (success) "✅" else "❌"
                callback?.showToast("$status $message")
            }

            override fun updateStatusText(text: String) {
                callback?.updateStatusText(text)
            }

            override fun showToast(message: String, duration: Int) {
                callback?.showToast(message, duration)
            }

            override fun runOnUiThread(action: () -> Unit) {
                callback?.runOnUiThread(action)
            }

            override fun getContentView(): View {
                return callback?.getContentView() ?: throw IllegalStateException("Content view not available")
            }

            override fun getContext(): Context {
                return callback?.getContext() ?: throw IllegalStateException("Context not available")
            }
        })
    }

    private fun setupNetworkController() {
        networkController.setCallback(object : NetworkController.NetworkCallback {
            override fun onStreamingStarted() {
                callback?.updateStatusText("Streaming started")
            }

            override fun onStreamingStopped() {
                callback?.updateStatusText("Streaming stopped")
            }

            override fun onNetworkStatusChanged(connected: Boolean) {
                val status = if (connected) "connected" else "disconnected"
                callback?.updateStatusText("Network $status")
            }

            override fun onStreamingError(message: String) {
                callback?.showToast("Streaming Error: $message", Toast.LENGTH_LONG)
            }

            override fun onStreamingQualityChanged(quality: NetworkController.StreamingQuality) {
                callback?.updateStatusText("Streaming quality: ${quality.displayName}")
            }

            override fun onNetworkRecovery(networkType: String) {
                callback?.updateStatusText("Network recovered: $networkType")
                callback?.showToast("Network recovered: $networkType", Toast.LENGTH_SHORT)
            }

            override fun updateStatusText(text: String) {
                callback?.updateStatusText(text)
            }

            override fun showToast(message: String, duration: Int) {
                callback?.showToast(message, duration)
            }

            override fun getStreamingIndicator(): View? {
                return callback?.getStreamingIndicator()
            }

            override fun getStreamingLabel(): View? {
                return callback?.getStreamingLabel()
            }

            override fun getStreamingDebugOverlay(): TextView? {
                return callback?.getStreamingDebugOverlay()
            }

            override fun onProtocolChanged(protocol: NetworkController.StreamingProtocol) {
                callback?.updateStatusText("Protocol: ${protocol.displayName}")
            }

            override fun onBandwidthEstimated(bandwidth: Long, method: NetworkController.BandwidthEstimationMethod) {
                val bandwidthMbps = bandwidth / 1_000_000.0
                callback?.updateStatusText("Bandwidth: ${String.format("%.1f", bandwidthMbps)}Mbps")
            }

            override fun onFrameDropped(reason: String) {
                callback?.updateStatusText("Frame dropped: $reason")
            }

            override fun onEncryptionStatusChanged(enabled: Boolean) {
                val status = if (enabled) "Enabled" else "Disabled"
                callback?.updateStatusText("Encryption: $status")
            }

            override fun getContext(): android.content.Context {
                return callback?.getContext() ?: throw IllegalStateException("Context not available")
            }
        })
    }

    private fun setupStatusDisplayController() {
        statusDisplayController.setCallback(object : StatusDisplayController.StatusDisplayCallback {
            override fun onBatteryLevelChanged(level: Int, colour: Int) {
            }

            override fun onConnectionStatusChanged(type: StatusDisplayController.ConnectionType, connected: Boolean) {
                val statusText = when (type) {
                    StatusDisplayController.ConnectionType.PC -> if (connected) "PC connected" else "PC disconnected"
                    StatusDisplayController.ConnectionType.SHIMMER -> if (connected) "Shimmer connected" else "Shimmer disconnected"
                    StatusDisplayController.ConnectionType.THERMAL -> if (connected) "Thermal connected" else "Thermal disconnected"
                }
                callback?.updateStatusText(statusText)
            }

            override fun onStatusMonitoringInitialized() {
                callback?.updateStatusText("Status monitoring initialized")
            }

            override fun onStatusMonitoringError(message: String) {
                callback?.showToast("Status Error: $message", Toast.LENGTH_LONG)
            }

            override fun updateStatusText(text: String) {
                callback?.updateStatusText(text)
            }

            override fun runOnUiThread(action: () -> Unit) {
                callback?.runOnUiThread(action)
            }

            override fun registerBroadcastReceiver(
                receiver: android.content.BroadcastReceiver,
                filter: android.content.IntentFilter
            ): android.content.Intent? {
                return try {
                    val context = callback?.getContext()
                    if (context != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            // For Android 13+, use RECEIVER_NOT_EXPORTED for app-specific broadcasts
                            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
                        } else {
                            context.registerReceiver(receiver, filter)
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivityCoordinator", "Failed to register broadcast receiver", e)
                    null
                }
            }

            override fun unregisterBroadcastReceiver(receiver: android.content.BroadcastReceiver) {
                try {
                    callback?.getContext()?.unregisterReceiver(receiver)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivityCoordinator", "Failed to unregister broadcast receiver", e)
                }
            }

            override fun getBatteryLevelText(): TextView? {
                return callback?.getBatteryLevelText()
            }

            override fun getPcConnectionStatus(): TextView? {
                return callback?.getPcConnectionStatus()
            }

            override fun getPcConnectionIndicator(): View? {
                return callback?.getPcConnectionIndicator()
            }

            override fun getShimmerConnectionStatus(): TextView? {
                return callback?.getShimmerConnectionStatus()
            }

            override fun getShimmerConnectionIndicator(): View? {
                return callback?.getShimmerConnectionIndicator()
            }

            override fun getThermalConnectionStatus(): TextView? {
                return callback?.getThermalConnectionStatus()
            }

            override fun getThermalConnectionIndicator(): View? {
                return callback?.getThermalConnectionIndicator()
            }
        })
    }

    private fun setupUIController() {
        uiController.setCallback(object : UIController.UICallback {
            override fun onUIComponentsInitialized() {
                android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] UI components initialized")
            }

            override fun onUIStateUpdated(state: com.multisensor.recording.ui.MainUiState) {
            }

            override fun onUIError(message: String) {
                callback?.showToast("UI Error: $message", Toast.LENGTH_LONG)
            }

            override fun updateStatusText(text: String) {
                callback?.updateStatusText(text)
            }

            override fun showToast(message: String, duration: Int) {
                callback?.showToast(message, duration)
            }

            override fun runOnUiThread(action: () -> Unit) {
                callback?.runOnUiThread(action)
            }

            override fun getContext(): android.content.Context {
                return callback?.getContext() ?: throw IllegalStateException("Context not available")
            }

            override fun getStatusText(): TextView? {
                return callback?.getStatusText()
            }

            override fun getStartRecordingButton(): View? {
                return callback?.getStartRecordingButton()
            }

            override fun getStopRecordingButton(): View? {
                return callback?.getStopRecordingButton()
            }

            override fun getCalibrationButton(): View? {
                return callback?.getCalibrationButton()
            }

            override fun getPcConnectionIndicator(): View? {
                return callback?.getPcConnectionIndicator()
            }

            override fun getShimmerConnectionIndicator(): View? {
                return callback?.getShimmerConnectionIndicator()
            }

            override fun getThermalConnectionIndicator(): View? {
                return callback?.getThermalConnectionIndicator()
            }

            override fun getPcConnectionStatus(): TextView? {
                return callback?.getPcConnectionStatus()
            }

            override fun getShimmerConnectionStatus(): TextView? {
                return callback?.getShimmerConnectionStatus()
            }

            override fun getThermalConnectionStatus(): TextView? {
                return callback?.getThermalConnectionStatus()
            }

            override fun getBatteryLevelText(): TextView? {
                return callback?.getBatteryLevelText()
            }

            override fun getRecordingIndicator(): View? {
                return callback?.getRecordingIndicator()
            }

            override fun getStreamingIndicator(): View? {
                return callback?.getStreamingIndicator()
            }

            override fun getStreamingLabel(): View? {
                return callback?.getStreamingLabel()
            }

            override fun getStreamingDebugOverlay(): TextView? {
                return callback?.getStreamingDebugOverlay()
            }

            override fun getRequestPermissionsButton(): View? {
                return callback?.getRequestPermissionsButton()
            }

            override fun getShimmerStatusText(): TextView? {
                return callback?.getShimmerStatusText()
            }
        })
    }

    private fun setupMenuController() {
        android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] MenuController not used in new UI")
    }

    fun checkPermissions(context: Context) {
        android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Coordinating permission check")
        permissionController.checkPermissions(context)
    }

    fun handleUsbDeviceIntent(context: Context, intent: Intent) {
        android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Coordinating USB device intent handling")
        usbController.handleUsbDeviceIntent(context, intent)
    }

    fun initializeRecordingSystem(context: Context, textureView: TextureView, viewModel: MainViewModel) {
        android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Coordinating recording system initialization")

        recordingController.initializeStatePersistence(context)

        recordingController.initializeRecordingSystem(context, textureView, viewModel)
    }

    fun startRecording(context: Context, viewModel: MainViewModel) {
        android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Coordinating recording start")

        if (!recordingController.validateRecordingPrerequisites(context)) {
            android.util.Log.w("MainActivityCoordinator", "[DEBUG_LOG] Recording prerequisites not met")
            return
        }

        val currentQuality = recordingController.getCurrentQuality()
        if (!recordingController.validateQualityForResources(context, currentQuality)) {
            val recommendedQuality = recordingController.getRecommendedQuality(context)
            android.util.Log.w(
                "MainActivityCoordinator",
                "[DEBUG_LOG] Current quality $currentQuality not suitable, switching to $recommendedQuality"
            )
            recordingController.setRecordingQuality(recommendedQuality)
        }

        recordingController.startRecording(context, viewModel)
        networkController.updateStreamingUI(context, true)
    }

    fun stopRecording(context: Context, viewModel: MainViewModel) {
        android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Coordinating recording stop")
        recordingController.stopRecording(context, viewModel)
        networkController.updateStreamingUI(context, false)
    }

    fun emergencyStopRecording(context: Context, viewModel: MainViewModel) {
        android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Coordinating emergency recording stop")
        recordingController.emergencyStopRecording(context, viewModel)
        networkController.updateStreamingUI(context, false)
    }

    fun setRecordingQuality(context: Context, quality: RecordingController.RecordingQuality) {
        android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Coordinating recording quality change to $quality")

        if (recordingController.validateQualityForResources(context, quality)) {
            recordingController.setRecordingQuality(quality)
        } else {
            android.util.Log.w(
                "MainActivityCoordinator",
                "[DEBUG_LOG] Quality $quality not suitable for current resources"
            )
            callback?.showToast(
                "Quality $quality not suitable for current storage/resources",
                android.widget.Toast.LENGTH_LONG
            )
        }
    }

    fun getRecordingStatus(): String {
        return recordingController.getRecordingStatus()
    }

    fun getCurrentRecordingQuality(): RecordingController.RecordingQuality {
        return recordingController.getCurrentQuality()
    }

    fun getAvailableRecordingQualities(): Array<RecordingController.RecordingQuality> {
        return recordingController.getAvailableQualities()
    }

    fun runCalibration(lifecycleScope: LifecycleCoroutineScope) {
        android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Coordinating calibration run")
        calibrationController.runCalibration(lifecycleScope)
    }

    fun testFlashSync(lifecycleScope: LifecycleCoroutineScope) {
        android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Coordinating flash sync test")
        calibrationController.testFlashSync(lifecycleScope)
    }

    fun testClockSync(lifecycleScope: LifecycleCoroutineScope) {
        android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Coordinating clock sync test")
        calibrationController.testClockSync(lifecycleScope)
    }

    fun launchShimmerDeviceDialog(activity: Activity, launcher: ActivityResultLauncher<Intent>) {
        android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Coordinating Shimmer device dialog launch")
        shimmerController.launchShimmerDeviceDialog(activity, launcher)
    }

    fun createOptionsMenu(menu: android.view.Menu, activity: Activity): Boolean {
        android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Options menu not used in new UI")
        return false
    }

    fun handleOptionsItemSelected(item: android.view.MenuItem): Boolean {
        android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Options menu not used in new UI")
        return false
    }

    fun getSystemStatusSummary(context: Context): String {
        return buildString {
            append("=== System Status Summary ===\n")
            append("Coordinator Initialized: $isInitialized\n\n")
            append(permissionController.getPermissionRetryCount().let { "Permission Retries: $it\n" })
            append(usbController.getUsbStatusSummary(context))
            append("\n")
            append(shimmerController.getConnectionStatus())
            append("\n")
            append(recordingController.getRecordingStatus())
            append("\n")
            append(calibrationController.getCalibrationStatus())
            append("\n")
            append(networkController.getStreamingStatus())
            append("\n")

            append("=== Recording Controller Enhanced Status ===\n")
            val currentState = recordingController.getCurrentState()
            append("Current Quality: ${recordingController.getCurrentQuality().displayName}\n")
            append("Service Health: ${if (recordingController.isServiceHealthy()) "✓ Healthy" else "✗ Unhealthy"}\n")
            append("State Persistence: ${if (currentState.isInitialized) "✓ Active" else "✗ Inactive"}\n")
            append("Session Count: ${currentState.sessionCount}\n")
            append("Total Recording Time: ${formatDuration(currentState.totalRecordingTime)}\n")

            val serviceState = recordingController.serviceConnectionState.value
            append("Service Connected: ${if (serviceState.isConnected) "✓ Yes" else "✗ No"}\n")
            if (serviceState.lastHeartbeat != null) {
                val timeSinceHeartbeat = System.currentTimeMillis() - serviceState.lastHeartbeat
                append("Last Heartbeat: ${timeSinceHeartbeat}ms ago\n")
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m ${remainingSeconds}s"
            minutes > 0 -> "${minutes}m ${remainingSeconds}s"
            else -> "${remainingSeconds}s"
        }
    }

    fun resetAllStates() {
        android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Resetting all controller states")

        permissionController.resetState()
        shimmerController.resetState()
        recordingController.resetState()
        calibrationController.resetState()
        networkController.resetState()

        isInitialized = false
    }

    private fun saveCoordinatorState(context: Context, state: CoordinatorState) {
        try {
            val prefs = context.getSharedPreferences(COORDINATOR_PREFS_NAME, Context.MODE_PRIVATE)
            val stateJson = JSONObject().apply {
                put("isInitialized", state.isInitialized)
                put("activeFeatures", state.activeFeatures.joinToString(","))
                put("lastInitTimestamp", state.lastInitTimestamp)
                put("errorCount", state.errorCount)
                put("lastErrorTimestamp", state.lastErrorTimestamp)
            }

            prefs.edit().apply {
                putString(PREF_COORDINATOR_STATE, stateJson.toString())
                apply()
            }

            android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Coordinator state saved")
        } catch (e: Exception) {
            android.util.Log.e("MainActivityCoordinator", "[DEBUG_LOG] Failed to save coordinator state: ${e.message}")
        }
    }

    private fun restoreCoordinatorState(context: Context): CoordinatorState? {
        return try {
            val prefs = context.getSharedPreferences(COORDINATOR_PREFS_NAME, Context.MODE_PRIVATE)
            val stateJson = prefs.getString(PREF_COORDINATOR_STATE, null) ?: return null

            val jsonObject = JSONObject(stateJson)
            val activeFeaturesList = jsonObject.getString("activeFeatures")
                .split(",")
                .filter { it.isNotBlank() }
                .toSet()

            CoordinatorState(
                isInitialized = jsonObject.getBoolean("isInitialized"),
                activeFeatures = activeFeaturesList,
                lastInitTimestamp = jsonObject.getLong("lastInitTimestamp"),
                errorCount = jsonObject.getInt("errorCount"),
                lastErrorTimestamp = jsonObject.getLong("lastErrorTimestamp")
            ).also {
                android.util.Log.d(
                    "MainActivityCoordinator",
                    "[DEBUG_LOG] Coordinator state restored: ${activeFeaturesList.size} features"
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(
                "MainActivityCoordinator",
                "[DEBUG_LOG] Failed to restore coordinator state: ${e.message}"
            )
            null
        }
    }

    private fun createFinalState(): CoordinatorState {
        return CoordinatorState(
            isInitialized = false,
            activeFeatures = emptySet(),
            lastInitTimestamp = System.currentTimeMillis(),
            errorCount = currentState?.errorCount ?: 0,
            lastErrorTimestamp = currentState?.lastErrorTimestamp ?: 0
        )
    }

    private fun handleCoordinatorError(operation: String, error: Exception) {
        errorRecoveryAttempts++
        val timestamp = System.currentTimeMillis()

        android.util.Log.e("MainActivityCoordinator", "[DEBUG_LOG] Coordinator error in $operation: ${error.message}")

        currentState = currentState?.copy(
            errorCount = (currentState?.errorCount ?: 0) + 1,
            lastErrorTimestamp = timestamp
        ) ?: CoordinatorState(
            isInitialized = false,
            activeFeatures = emptySet(),
            lastInitTimestamp = timestamp,
            errorCount = 1,
            lastErrorTimestamp = timestamp
        )

        if (errorRecoveryAttempts < 3) {
            android.util.Log.d(
                "MainActivityCoordinator",
                "[DEBUG_LOG] Attempting error recovery (attempt $errorRecoveryAttempts/3)"
            )

            callback?.getContext()?.let { context ->
                saveCoordinatorState(context, currentState!!)

                try {
                    when (operation) {
                        "initialization" -> {
                            resetAllStates()
                        }

                        "calibration" -> {
                            calibrationController.resetState()
                        }

                        "recording" -> {
                            callback?.getContext()?.let { context ->
                                android.util.Log.w(
                                    "MainActivityCoordinator",
                                    "Cannot stop recording without viewModel access"
                                )
                            }
                        }

                        "network" -> {
                            networkController.resetState()
                        }
                    }
                } catch (recoveryError: Exception) {
                    android.util.Log.e(
                        "MainActivityCoordinator",
                        "[DEBUG_LOG] Recovery failed: ${recoveryError.message}"
                    )
                }
            }
        } else {
            android.util.Log.e(
                "MainActivityCoordinator",
                "[DEBUG_LOG] Maximum recovery attempts reached, manual intervention required"
            )
            callback?.showToast("System error: Please restart the application", Toast.LENGTH_LONG)
        }
    }

    fun validateFeatureDependencies(featureToActivate: String): DependencyValidationResult {
        val missingDependencies = mutableListOf<String>()
        val conflictingFeatures = mutableListOf<String>()

        when (featureToActivate) {
            "recording" -> {
                if (!activeFeatures.contains("permissions")) {
                    missingDependencies.add("permissions")
                }
                if (!activeFeatures.contains("usb")) {
                    missingDependencies.add("usb")
                }
                if (activeFeatures.contains("calibration")) {
                    conflictingFeatures.add("calibration")
                }
            }

            "calibration" -> {
                if (!activeFeatures.contains("permissions")) {
                    missingDependencies.add("permissions")
                }
                if (activeFeatures.contains("recording")) {
                    conflictingFeatures.add("recording")
                }
            }

            "network" -> {
                if (!activeFeatures.contains("permissions")) {
                    missingDependencies.add("permissions")
                }
            }

            "shimmer" -> {
                if (!activeFeatures.contains("permissions")) {
                    missingDependencies.add("permissions")
                }
            }
        }

        val isValid = missingDependencies.isEmpty() && conflictingFeatures.isEmpty()

        if (!isValid) {
            android.util.Log.w(
                "MainActivityCoordinator",
                "[DEBUG_LOG] Feature dependency validation failed for $featureToActivate"
            )
            android.util.Log.w("MainActivityCoordinator", "[DEBUG_LOG] - Missing: $missingDependencies")
            android.util.Log.w("MainActivityCoordinator", "[DEBUG_LOG] - Conflicts: $conflictingFeatures")
        }

        return DependencyValidationResult(isValid, missingDependencies, conflictingFeatures)
    }

    fun activateFeature(featureName: String): Boolean {
        val validation = validateFeatureDependencies(featureName)

        if (validation.isValid) {
            activeFeatures.add(featureName)
            android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Feature activated: $featureName")

            currentState = currentState?.copy(
                activeFeatures = activeFeatures.toSet()
            )

            return true
        } else {
            android.util.Log.w("MainActivityCoordinator", "[DEBUG_LOG] Feature activation blocked: $featureName")
            callback?.showToast(
                "Cannot activate $featureName: ${validation.missingDependencies.joinToString()}",
                Toast.LENGTH_SHORT
            )
            return false
        }
    }

    fun deactivateFeature(featureName: String) {
        if (activeFeatures.remove(featureName)) {
            android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Feature deactivated: $featureName")

            currentState = currentState?.copy(
                activeFeatures = activeFeatures.toSet()
            )
        }
    }

    fun getActiveFeatures(): Set<String> = activeFeatures.toSet()

    fun cleanup() {
        android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] Cleaning up all controllers")

        try {
            calibrationController.cleanup()
            networkController.cleanup()
            statusDisplayController.cleanup()
            uiController.cleanup()

            permissionController.resetState()
            android.util.Log.w(
                "MainActivityCoordinator",
                "Cannot stop recording in resetAllStates without viewModel access"
            )

            callback?.getContext()?.let { context ->
                saveCoordinatorState(context, createFinalState())
            }

            activeFeatures.clear()
            currentState = null
            callback = null
            isInitialized = false

            android.util.Log.d("MainActivityCoordinator", "[DEBUG_LOG] All controllers cleaned up successfully")
        } catch (e: Exception) {
            android.util.Log.e("MainActivityCoordinator", "[DEBUG_LOG] Error during cleanup: ${e.message}")
            handleCoordinatorError("cleanup", e)
        }
    }
}
