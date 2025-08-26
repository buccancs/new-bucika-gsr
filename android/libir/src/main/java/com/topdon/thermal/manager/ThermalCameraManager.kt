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

class ThermalCameraManager(
    activity: Activity,
    private val lifecycleOwner: LifecycleOwner
) {
    private val activityRef = WeakReference(activity)
    private val context: Context get() = activityRef.get() ?: throw IllegalStateException("Activity reference lost")
    
    private var isConnected: Boolean = false
    private var currentCameraMode: CameraMode = CameraMode.TEMPERATURE
    private var currentGainMode: GainMode = GainMode.HIGH
    
    private var iruvctc: IRUVCTC? = null
    private var uvcCamera: UVCCamera? = null
    
    enum class CameraMode {
        TEMPERATURE,
        OBSERVATION
    }
    
    enum class GainMode {
        HIGH,
        LOW
    }
    
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
    
    private fun setupEventHandlers() {

    }
    
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
    
    private fun handleCameraConnect(cameraInfo: UVCCamera.CameraInfo) {
        isConnected = true
        lifecycleOwner.lifecycleScope.launch {

            initializeCameraSettings()
            notifyConnectionStatusChanged(true)
            Log.d(TAG, "Camera connected: ${cameraInfo.productName}")
        }
    }
    
    private fun handleCameraDisconnect() {
        isConnected = false
        lifecycleOwner.lifecycleScope.launch {
            cleanupCameraResources()
            notifyConnectionStatusChanged(false)
            Log.d(TAG, "Camera disconnected")
        }
    }
    
    private fun handleCameraCancel() {
        Log.d(TAG, "Camera connection cancelled")

    }
    
    private fun handleSensorSwitch(sensorSwitch: Boolean) {
        Log.d(TAG, "Sensor switch event: $sensorSwitch")

    }
    
    private fun switchToTemperatureMode() {
        IRCMD.getInstance()?.setParam(IRCMDType.MEASUREMENT_MODE, 1)

    }
    
    private fun switchToObservationMode() {
        IRCMD.getInstance()?.setParam(IRCMDType.MEASUREMENT_MODE, 0)  

    }
    
    private fun initializeCameraSettings() {

    }
    
    private fun cleanupCameraResources() {
        iruvctc?.release()
        uvcCamera?.destroy()
    }
    
    private fun handleInitializationError(error: Exception) {
        EventBus.getDefault().post(IRMsgEvent(MsgCode.CAMERA_INIT_ERROR, error.message))
    }
    
    private fun handleModeChangeError(error: Exception) {
        EventBus.getDefault().post(IRMsgEvent(MsgCode.MODE_CHANGE_ERROR, error.message))
    }
    
    private fun handleGainModeError(error: Exception) {
        EventBus.getDefault().post(IRMsgEvent(MsgCode.GAIN_MODE_ERROR, error.message))
    }
    
    private fun showLoadingIndicator(show: Boolean) {
        activityRef.get()?.runOnUiThread {

        }
    }
    
    private fun notifyModeChanged(mode: CameraMode) {
        EventBus.getDefault().post(CameraModeChangeEvent(mode))
    }
    
    private fun notifyGainModeChanged(gainMode: GainMode) {
        EventBus.getDefault().post(GainModeChangeEvent(gainMode))
    }
    
    private fun notifyConnectionStatusChanged(connected: Boolean) {
        EventBus.getDefault().post(CameraConnectionEvent(connected))
    }
    
    fun release() {
        cleanupCameraResources()
        iruvctc = null
        uvcCamera = null
    }
    
    fun isConnected(): Boolean = isConnected
    fun getCurrentMode(): CameraMode = currentCameraMode
    fun getCurrentGainMode(): GainMode = currentGainMode
    
    companion object {
        private const val TAG = "ThermalCameraManager"
    }
}

data class CameraModeChangeEvent(val mode: ThermalCameraManager.CameraMode)

data class GainModeChangeEvent(val gainMode: ThermalCameraManager.GainMode)
