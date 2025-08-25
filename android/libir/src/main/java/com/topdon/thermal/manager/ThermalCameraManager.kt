package com.topdon.thermal.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.ircmd.IRCMDType
import com.energy.iruvc.uvc.ConnectCallback
import com.energy.iruvc.uvc.UVCCamera
import com.infisense.usbir.camera.IRUVCTC
import com.infisense.usbir.config.MsgCode
import com.infisense.usbir.event.IRMsgEvent
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.lang.ref.WeakReference

/**
 * Thermal Camera Management Component
 * 
 * Extracted from IRThermalNightActivity to handle all camera-related operations
 * with improved separation of concerns and reduced complexity.
 * 
 * Responsibilities:
 * - Camera initialization and configuration
 * - USB connection management  
 * - Thermal sensor communication
 * - Camera state management
 * - Error handling and recovery
 */
class ThermalCameraManager(
    activity: Activity,
    private val lifecycleOwner: LifecycleOwner
) {
    private val activityRef = WeakReference(activity)
    private val context: Context get() = activityRef.get() ?: throw IllegalStateException("Activity reference lost")
    
    // Camera state management
    private var isConnected: Boolean = false
    private var currentCameraMode: CameraMode = CameraMode.TEMPERATURE
    private var currentGainMode: GainMode = GainMode.HIGH
    
    // Camera instances
    private var iruvctc: IRUVCTC? = null
    private var uvcCamera: UVCCamera? = null
    
    enum class CameraMode {
        TEMPERATURE,    // Temperature measurement mode
        OBSERVATION     // Observation mode
    }
    
    enum class GainMode {
        HIGH,          // High gain mode
        LOW            // Low gain mode  
    }
    
    /**
     * Initialize thermal camera system
     */
    fun initializeCamera() {
        lifecycleOwner.lifecycleScope.launch {
            try {
                setupIRUVCTC()
                setupEventHandlers()
                Log.d(TAG, "Thermal camera initialization completed")
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                handleInitializationError(e)
            }
        }
    }
    
    /**
     * Set up IRUVCTC camera instance
     */
    private fun setupIRUVCTC() {
        activityRef.get()?.let { activity ->
            iruvctc = IRUVCTC()
            iruvctc?.let { camera ->
                camera.setContext(activity)
                camera.setSensorSwitchListener { sensorSwitch ->
                    handleSensorSwitch(sensorSwitch)
                }
                setupCameraCallbacks(camera)
            }
        }
    }
    
    /**
     * Set up camera event callbacks
     */
    private fun setupCameraCallbacks(camera: IRUVCTC) {
        camera.setConnectCallback(object : ConnectCallback {
            override fun onConnect(cameraInfo: UVCCamera.CameraInfo) {
                handleCameraConnect(cameraInfo)
            }
            
            override fun onDisconnect() {
                handleCameraDisconnect()
            }
            
            override fun onCancel() {
                handleCameraCancel()
            }
        })
    }
    
    /**
     * Set up event bus handlers
     */
    private fun setupEventHandlers() {
        // Event handling for IR messages will be managed here
        // This reduces the event handling burden on the main activity
    }
    
    /**
     * Switch camera between temperature and observation modes
     */
    fun switchCameraMode(mode: CameraMode, showLoading: Boolean = true) {
        if (currentCameraMode == mode) return
        
        lifecycleOwner.lifecycleScope.launch {
            try {
                if (showLoading) {
                    showLoadingIndicator(true)
                }
                
                when (mode) {
                    CameraMode.TEMPERATURE -> {
                        switchToTemperatureMode()
                    }
                    CameraMode.OBSERVATION -> {
                        switchToObservationMode()
                    }
                }
                
                currentCameraMode = mode
                notifyModeChanged(mode)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to switch camera mode", e)
                handleModeChangeError(e)
            } finally {
                if (showLoading) {
                    showLoadingIndicator(false)
                }
            }
        }
    }
    
    /**
     * Switch gain mode (high/low)
     */
    fun switchGainMode(gainMode: GainMode, showLoading: Boolean = true) {
        if (currentGainMode == gainMode) return
        
        lifecycleOwner.lifecycleScope.launch {
            try {
                if (showLoading) {
                    showLoadingIndicator(true)
                }
                
                val isLowGain = gainMode == GainMode.LOW
                IRCMD.getInstance()?.setParam(IRCMDType.GAIN_MODE, if (isLowGain) 1 else 0)
                
                currentGainMode = gainMode
                notifyGainModeChanged(gainMode)
                
                Log.d(TAG, "Gain mode switched to: $gainMode")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to switch gain mode", e)
                handleGainModeError(e)
            } finally {
                if (showLoading) {
                    showLoadingIndicator(false)
                }
            }
        }
    }
    
    /**
     * Handle camera connection
     */
    private fun handleCameraConnect(cameraInfo: UVCCamera.CameraInfo) {
        isConnected = true
        lifecycleOwner.lifecycleScope.launch {
            // Initialize camera settings after connection
            initializeCameraSettings()
            notifyConnectionStatusChanged(true)
            Log.d(TAG, "Camera connected: ${cameraInfo.productName}")
        }
    }
    
    /**
     * Handle camera disconnection
     */
    private fun handleCameraDisconnect() {
        isConnected = false
        lifecycleOwner.lifecycleScope.launch {
            cleanupCameraResources()
            notifyConnectionStatusChanged(false)
            Log.d(TAG, "Camera disconnected")
        }
    }
    
    /**
     * Handle camera connection cancellation
     */
    private fun handleCameraCancel() {
        Log.d(TAG, "Camera connection cancelled")
        // Handle cancellation logic
    }
    
    /**
     * Handle sensor switch events
     */
    private fun handleSensorSwitch(sensorSwitch: Boolean) {
        Log.d(TAG, "Sensor switch event: $sensorSwitch")
        // Handle sensor switch logic
    }
    
    /**
     * Switch to temperature measurement mode
     */
    private fun switchToTemperatureMode() {
        IRCMD.getInstance()?.setParam(IRCMDType.MEASUREMENT_MODE, 1)
        // Additional temperature mode setup
    }
    
    /**
     * Switch to observation mode
     */
    private fun switchToObservationMode() {
        IRCMD.getInstance()?.setParam(IRCMDType.MEASUREMENT_MODE, 0)  
        // Additional observation mode setup
    }
    
    /**
     * Initialize camera settings after connection
     */
    private fun initializeCameraSettings() {
        // Set default camera parameters
        // This consolidates camera initialization logic
    }
    
    /**
     * Clean up camera resources
     */
    private fun cleanupCameraResources() {
        iruvctc?.release()
        uvcCamera?.destroy()
    }
    
    /**
     * Handle initialization errors
     */
    private fun handleInitializationError(error: Exception) {
        EventBus.getDefault().post(IRMsgEvent(MsgCode.CAMERA_INIT_ERROR, error.message))
    }
    
    /**
     * Handle mode change errors
     */
    private fun handleModeChangeError(error: Exception) {
        EventBus.getDefault().post(IRMsgEvent(MsgCode.MODE_CHANGE_ERROR, error.message))
    }
    
    /**
     * Handle gain mode errors
     */
    private fun handleGainModeError(error: Exception) {
        EventBus.getDefault().post(IRMsgEvent(MsgCode.GAIN_MODE_ERROR, error.message))
    }
    
    /**
     * Show/hide loading indicator
     */
    private fun showLoadingIndicator(show: Boolean) {
        activityRef.get()?.runOnUiThread {
            // Update UI loading state
            // This will be connected to the activity's loading UI
        }
    }
    
    /**
     * Notify mode change to listeners
     */
    private fun notifyModeChanged(mode: CameraMode) {
        EventBus.getDefault().post(CameraModeChangeEvent(mode))
    }
    
    /**
     * Notify gain mode change to listeners
     */
    private fun notifyGainModeChanged(gainMode: GainMode) {
        EventBus.getDefault().post(GainModeChangeEvent(gainMode))
    }
    
    /**
     * Notify connection status change
     */
    private fun notifyConnectionStatusChanged(connected: Boolean) {
        EventBus.getDefault().post(CameraConnectionEvent(connected))
    }
    
    /**
     * Release resources
     */
    fun release() {
        cleanupCameraResources()
        iruvctc = null
        uvcCamera = null
    }
    
    // Getters for current state
    fun isConnected(): Boolean = isConnected
    fun getCurrentMode(): CameraMode = currentCameraMode
    fun getCurrentGainMode(): GainMode = currentGainMode
    
    companion object {
        private const val TAG = "ThermalCameraManager"
    }
}

/**
 * Camera mode change event
 */
data class CameraModeChangeEvent(val mode: ThermalCameraManager.CameraMode)

/**
 * Gain mode change event  
 */
data class GainModeChangeEvent(val gainMode: ThermalCameraManager.GainMode)

/**
 * Camera connection event
 */
data class CameraConnectionEvent(val connected: Boolean)