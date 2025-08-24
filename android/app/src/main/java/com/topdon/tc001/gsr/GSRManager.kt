package com.topdon.tc001.gsr

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.elvishew.xlog.XLog
import com.shimmerresearch.android.ShimmerBluetooth
import com.shimmerresearch.driver.Configuration
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.driver.ShimmerDataProcessor
import com.shimmerresearch.driver.ProcessedGSRData
import com.topdon.tc001.gsr.data.GSRDataWriter
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Enhanced GSR Manager for comprehensive Shimmer3 GSR+ device integration
 * Provides advanced signal processing and data quality monitoring
 * Integrates ShimmerAndroidAPI with professional-grade data analysis
 */
class GSRManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "GSRManager"
        private const val DEFAULT_SAMPLING_RATE = 128.0
        
        @Volatile
        private var INSTANCE: GSRManager? = null
        
        fun getInstance(context: Context): GSRManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GSRManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Shimmer device components
    private var shimmerDevice: ShimmerBluetooth? = null
    private var bluetoothAddress: String? = null
    private var connectedDeviceName: String? = null
    
    // Advanced data processing
    private val dataProcessor = ShimmerDataProcessor()
    
    // Data writer for local storage
    private val dataWriter: GSRDataWriter = GSRDataWriter.getInstance(context)
    
    // State management
    private val isConnected = AtomicBoolean(false)
    private val isRecording = AtomicBoolean(false)
    
    // Data listeners
    private var gsrDataListener: GSRDataListener? = null
    private var advancedDataListener: AdvancedGSRDataListener? = null
    
    // Handler for UI updates
    private val mainHandler = Handler(Looper.getMainLooper())
    
    /**
     * Basic interface for GSR data callbacks
     */
    interface GSRDataListener {
        fun onGSRDataReceived(timestamp: Long, gsrValue: Double, skinTemperature: Double)
        fun onConnectionStatusChanged(isConnected: Boolean, deviceName: String?)
    }
    
    /**
     * Advanced interface for processed GSR data with quality metrics
     */
    interface AdvancedGSRDataListener {
        fun onProcessedGSRDataReceived(processedData: ProcessedGSRData)
        fun onSignalQualityChanged(signalQuality: Double, validDataRatio: Double)
        fun onConnectionStatusChanged(isConnected: Boolean, deviceName: String?)
    }
    
    /**
     * Set basic GSR data listener
     */
    fun setGSRDataListener(listener: GSRDataListener) {
        this.gsrDataListener = listener
        XLog.i(TAG, "Basic GSR data listener set")
    }
    
    /**
     * Set advanced GSR data listener with processing capabilities
     */
    fun setAdvancedGSRDataListener(listener: AdvancedGSRDataListener) {
        this.advancedDataListener = listener
        XLog.i(TAG, "Advanced GSR data listener set")
    }
    
    /**
     * Initialize Shimmer device
     */
    fun initializeShimmer() {
        XLog.i(TAG, "Enhanced Shimmer GSR Manager initialized with data processing")
    }
    
    /**
     * Connect to Shimmer device via Bluetooth
     */
    fun connectToShimmer(bluetoothAddress: String) {
        if (isConnected.get()) {
            XLog.w(TAG, "Already connected to Shimmer device")
            return
        }
        
        try {
            this.bluetoothAddress = bluetoothAddress
            XLog.i(TAG, "Connecting to Shimmer device: $bluetoothAddress")
            
            // Reset data processor
            dataProcessor.reset()
            
            // Create Shimmer device instance
            shimmerDevice = ShimmerBluetooth(
                context = context,
                handler = createShimmerMessageHandler(),
                bluetoothAddress = bluetoothAddress,
                continousSync = false,
                shimmerUserAssignedName = false
            )
            
            // Configure Shimmer for GSR
            configureShimmerForGSR()
            
            // Initiate connection
            shimmerDevice?.connect()
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to connect to Shimmer device: ${e.message}", e)
            handleConnectionError()
        }
    }
    
    /**
     * Configure Shimmer device for GSR data collection
     */
    private fun configureShimmerForGSR() {
        shimmerDevice?.let { device ->
            device.setSamplingRateShimmer(DEFAULT_SAMPLING_RATE)
            device.setEnabledSensors(Configuration.Shimmer3.SensorMap.GSR.mValue or Configuration.Shimmer3.SensorMap.TEMPERATURE.mValue)
            XLog.i(TAG, "Shimmer configured for GSR and temperature sampling at $DEFAULT_SAMPLING_RATE Hz")
        }
    }
    
    /**
     * Create message handler for Shimmer SDK callbacks
     */
    private fun createShimmerMessageHandler(): Handler {
        return object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: android.os.Message) {
                when (msg.what) {
                    ShimmerBluetooth.MSG_STATE_CHANGE -> {
                        handleConnectionStateChange(msg.arg1)
                    }
                    ShimmerBluetooth.MSG_READ -> {
                        if (isRecording.get()) {
                            val objectCluster = msg.obj as? ObjectCluster
                            objectCluster?.let { processAndDeliverGSRData(it) }
                        }
                    }
                    ShimmerBluetooth.MSG_DEVICE_NAME -> {
                        connectedDeviceName = msg.data.getString(ShimmerBluetooth.EXTRA_DEVICE_NAME)
                        XLog.i(TAG, "Connected to device: $connectedDeviceName")
                    }
                    ShimmerBluetooth.MSG_TOAST -> {
                        val toastMessage = msg.data.getString("toast")
                        XLog.i(TAG, "Shimmer message: $toastMessage")
                    }
                }
            }
        }
    }
    
    /**
     * Handle connection state changes
     */
    private fun handleConnectionStateChange(newState: Int) {
        when (newState) {
            ShimmerBluetooth.STATE_CONNECTED -> {
                isConnected.set(true)
                XLog.i(TAG, "Shimmer device connected successfully")
                mainHandler.post {
                    gsrDataListener?.onConnectionStatusChanged(true, connectedDeviceName)
                    advancedDataListener?.onConnectionStatusChanged(true, connectedDeviceName)
                }
            }
            ShimmerBluetooth.STATE_CONNECTING -> {
                XLog.d(TAG, "Connecting to Shimmer device...")
            }
            ShimmerBluetooth.STATE_NONE -> {
                if (isConnected.get()) {
                    handleDisconnection()
                }
            }
        }
    }
    
    /**
     * Process GSR data and deliver to listeners
     */
    private fun processAndDeliverGSRData(objectCluster: ObjectCluster) {
        try {
            // Process data with advanced algorithms
            val processedData = dataProcessor.processGSRData(objectCluster, DEFAULT_SAMPLING_RATE)
            
            // Save data to local file if recording
            if (isRecording.get()) {
                dataWriter.addGSRData(processedData)
            }
            
            // Deliver processed data to advanced listener
            advancedDataListener?.let { listener ->
                mainHandler.post {
                    listener.onProcessedGSRDataReceived(processedData)
                    
                    // Update signal quality periodically
                    if (processedData.sampleIndex % 64 == 0L) { // Every 0.5 seconds at 128 Hz
                        val stats = dataProcessor.getStatistics()
                        listener.onSignalQualityChanged(stats.currentSignalQuality, stats.validDataRatio)
                    }
                }
            }
            
            // Deliver basic data to simple listener
            gsrDataListener?.let { listener ->
                mainHandler.post {
                    listener.onGSRDataReceived(
                        processedData.timestamp,
                        processedData.filteredGSR,
                        processedData.filteredTemperature
                    )
                }
            }
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error processing GSR data: ${e.message}", e)
        }
    }
    
    /**
     * Disconnect from Shimmer device
     */
    fun disconnectShimmer() {
        try {
            // Stop recording if active
            if (isRecording.get()) {
                stopRecording()
            }
            
            // Disconnect device
            shimmerDevice?.disconnect()
            shimmerDevice = null
            
            // Reset state
            isConnected.set(false)
            bluetoothAddress = null
            connectedDeviceName = null
            
            XLog.i(TAG, "Shimmer device disconnected")
            
            mainHandler.post {
                gsrDataListener?.onConnectionStatusChanged(false, null)
                advancedDataListener?.onConnectionStatusChanged(false, null)
            }
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error disconnecting Shimmer device: ${e.message}", e)
        }
    }
    
    /**
     * Start GSR data recording
     */
    fun startRecording(): Boolean {
        return if (isConnected.get() && !isRecording.get()) {
            try {
                // Start local file recording
                val recordingStarted = dataWriter.startRecording()
                if (!recordingStarted) {
                    XLog.e(TAG, "Failed to start local data recording")
                    return false
                }
                
                // Start device streaming
                shimmerDevice?.startStreaming()
                isRecording.set(true)
                XLog.i(TAG, "GSR recording started with advanced processing and local storage")
                true
            } catch (e: Exception) {
                XLog.e(TAG, "Failed to start GSR recording: ${e.message}", e)
                // Cleanup if streaming started but recording failed
                if (dataWriter.isRecording()) {
                    dataWriter.stopRecording()
                }
                false
            }
        } else {
            XLog.w(TAG, "Cannot start recording - not connected or already recording")
            false
        }
    }
    
    /**
     * Stop GSR data recording
     */
    fun stopRecording(): Boolean {
        return if (isRecording.get()) {
            try {
                // Stop device streaming first
                shimmerDevice?.stopStreaming()
                isRecording.set(false)
                
                // Stop local file recording
                val recordingStopped = dataWriter.stopRecording()
                
                // Log final statistics
                val stats = dataProcessor.getStatistics()
                val recordingInfo = if (recordingStopped) dataWriter.getCurrentRecordingInfo() else null
                
                XLog.i(TAG, "GSR recording stopped. Stats: ${stats.totalSamples} total, ${stats.validSamples} valid, quality: ${stats.currentSignalQuality}%")
                if (recordingInfo != null) {
                    XLog.i(TAG, "Local file saved: ${recordingInfo.fileName} with ${recordingInfo.samplesWritten} samples")
                }
                
                true
            } catch (e: Exception) {
                XLog.e(TAG, "Failed to stop GSR recording: ${e.message}", e)
                false
            }
        } else {
            XLog.w(TAG, "Not currently recording")
            false
        }
    }
    
    /**
     * Check if device is connected
     */
    fun isConnected(): Boolean = isConnected.get()
    
    /**
     * Check if recording is active
     */
    fun isRecording(): Boolean = isRecording.get()
    
    /**
     * Get connected device name
     */
    fun getConnectedDeviceName(): String? = if (isConnected.get()) connectedDeviceName else null
    
    /**
     * Get current processing statistics
     */
    fun getProcessingStatistics() = dataProcessor.getStatistics()
    
    /**
     * Handle connection error
     */
    private fun handleConnectionError() {
        isConnected.set(false)
        mainHandler.post {
            gsrDataListener?.onConnectionStatusChanged(false, null)
            advancedDataListener?.onConnectionStatusChanged(false, null)
        }
    }
    
    /**
     * Handle disconnection
     */
    private fun handleDisconnection() {
        isConnected.set(false)
        if (isRecording.get()) {
            stopRecording()
        }
        
        mainHandler.post {
            gsrDataListener?.onConnectionStatusChanged(false, null)
            advancedDataListener?.onConnectionStatusChanged(false, null)
        }
        
        XLog.i(TAG, "Shimmer device disconnected")
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            if (isRecording.get()) {
                stopRecording()
            }
            
            disconnectShimmer()
            dataProcessor.reset()
            dataWriter.cleanup()
            gsrDataListener = null
            advancedDataListener = null
            
            XLog.i(TAG, "Enhanced GSR Manager cleaned up")
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error during cleanup: ${e.message}", e)
        }
    }
    
    /**
     * Get recorded GSR files
     */
    fun getRecordedFiles() = dataWriter.getRecordedFiles()
    
    /**
     * Get current recording information
     */
    fun getCurrentRecordingInfo() = dataWriter.getCurrentRecordingInfo()
    
    /**
     * Export GSR data to file
     */
    suspend fun exportGSRDataToFile(
        gsrData: List<ProcessedGSRData>,
        fileName: String? = null,
        includeAnalysis: Boolean = true
    ) = dataWriter.exportGSRDataToFile(gsrData, fileName, includeAnalysis)
}