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
import com.topdon.tc001.gsr.quality.GSRDataQualityMonitor
import com.topdon.tc001.gsr.quality.QualityAssessment
import com.topdon.tc001.performance.PerformanceMonitor
import java.util.concurrent.atomic.AtomicBoolean

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
    
    private var shimmerDevice: ShimmerBluetooth? = null
    private var bluetoothAddress: String? = null
    private var connectedDeviceName: String? = null
    
    private val dataProcessor = ShimmerDataProcessor()
    
    private val dataWriter: GSRDataWriter = GSRDataWriter.getInstance(context)
    
    private val qualityMonitor = GSRDataQualityMonitor()
    
    private val performanceMonitor = PerformanceMonitor(context)
    
    private val isConnected = AtomicBoolean(false)
    private val isRecording = AtomicBoolean(false)
    
    private var gsrDataListener: GSRDataListener? = null
    private var advancedDataListener: AdvancedGSRDataListener? = null
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    interface GSRDataListener {
        fun onGSRDataReceived(timestamp: Long, gsrValue: Double, skinTemperature: Double)
        fun onConnectionStatusChanged(isConnected: Boolean, deviceName: String?)
    }
    
    interface AdvancedGSRDataListener {
        fun onProcessedGSRDataReceived(processedData: ProcessedGSRData)
        fun onSignalQualityChanged(signalQuality: Double, validDataRatio: Double)
        fun onConnectionStatusChanged(isConnected: Boolean, deviceName: String?)
        fun onQualityAssessmentUpdated(assessment: QualityAssessment)
    }
    
    fun setGSRDataListener(listener: GSRDataListener) {
        this.gsrDataListener = listener
        XLog.i(TAG, "Basic GSR data listener set")
    }
    
    fun setAdvancedGSRDataListener(listener: AdvancedGSRDataListener) {
        this.advancedDataListener = listener
        XLog.i(TAG, "Advanced GSR data listener set")
    }
    
    fun initializeShimmer() {
        XLog.i(TAG, "Enhanced Shimmer GSR Manager initialized with data processing")
    }
    
    fun connectToShimmer(bluetoothAddress: String) {
        if (isConnected.get()) {
            XLog.w(TAG, "Already connected to Shimmer device")
            return
        }
        
        try {
            this.bluetoothAddress = bluetoothAddress
            XLog.i(TAG, "Connecting to Shimmer device: $bluetoothAddress")
            
            dataProcessor.reset()
            
            shimmerDevice = ShimmerBluetooth(
                context = context,
                handler = createShimmerMessageHandler(),
                bluetoothAddress = bluetoothAddress,
                continousSync = false,
                shimmerUserAssignedName = false
            )
            
            configureShimmerForGSR()
            
            shimmerDevice?.connect()
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to connect to Shimmer device: ${e.message}", e)
            handleConnectionError()
        }
    }
    
    private fun configureShimmerForGSR() {
        shimmerDevice?.let { device ->
            device.setSamplingRateShimmer(DEFAULT_SAMPLING_RATE)
            device.setEnabledSensors(Configuration.Shimmer3.SensorMap.GSR.mValue or Configuration.Shimmer3.SensorMap.TEMPERATURE.mValue)
            XLog.i(TAG, "Shimmer configured for GSR and temperature sampling at $DEFAULT_SAMPLING_RATE Hz")
        }
    }
    
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
    
    private fun processAndDeliverGSRData(objectCluster: ObjectCluster) {
        try {

            val processedData = dataProcessor.processGSRData(objectCluster, DEFAULT_SAMPLING_RATE)
            
            performanceMonitor.recordGSRSampleProcessed()
            
            if (isRecording.get()) {
                val validationResult = qualityMonitor.processDataSample(processedData)
                
                if (validationResult.isValid) {
                    dataWriter.addGSRData(processedData)
                    performanceMonitor.recordFileOperation()
                } else {
                    XLog.w(TAG, "Low quality sample detected (${validationResult.qualityScore.toInt()}%): ${validationResult.issues}")
                }
            }
            
            advancedDataListener?.let { listener ->
                mainHandler.post {
                    listener.onProcessedGSRDataReceived(processedData)
                    
                    if (processedData.sampleIndex % 64 == 0L) {
                        val stats = dataProcessor.getStatistics()
                        listener.onSignalQualityChanged(stats.currentSignalQuality, stats.validDataRatio)
                        
                        if (processedData.sampleIndex % 256 == 0L && isRecording.get()) {
                            val qualityAssessment = qualityMonitor.getCurrentQualityAssessment()
                            listener.onQualityAssessmentUpdated(qualityAssessment)
                        }
                    }
                }
            }
            
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
    
    fun disconnectShimmer() {
        try {

            if (isRecording.get()) {
                stopRecording()
            }
            
            shimmerDevice?.disconnect()
            shimmerDevice = null
            
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
    
    fun startRecording(): Boolean {
        return if (isConnected.get() && !isRecording.get()) {
            try {

                val recordingStarted = dataWriter.startRecording()
                if (!recordingStarted) {
                    XLog.e(TAG, "Failed to start local data recording")
                    return false
                }
                
                qualityMonitor.reset()
                
                performanceMonitor.startMonitoring()
                
                shimmerDevice?.startStreaming()
                isRecording.set(true)
                XLog.i(TAG, "GSR recording started with advanced processing, local storage, quality monitoring, and performance tracking")
                true
            } catch (e: Exception) {
                XLog.e(TAG, "Failed to start GSR recording: ${e.message}", e)

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
    
    fun stopRecording(): Boolean {
        return if (isRecording.get()) {
            try {

                shimmerDevice?.stopStreaming()
                isRecording.set(false)
                
                val recordingStopped = dataWriter.stopRecording()
                
                performanceMonitor.stopMonitoring()
                
                val stats = dataProcessor.getStatistics()
                val recordingInfo = if (recordingStopped) dataWriter.getCurrentRecordingInfo() else null
                val finalQualityAssessment = qualityMonitor.getCurrentQualityAssessment()
                val performanceSummary = performanceMonitor.getPerformanceSummary()
                
                XLog.i(TAG, "GSR recording stopped. Stats: ${stats.totalSamples} total, ${stats.validSamples} valid, quality: ${stats.currentSignalQuality}%")
                if (recordingInfo != null) {
                    XLog.i(TAG, "Local file saved: ${recordingInfo.fileName} with ${recordingInfo.samplesWritten} samples")
                }
                XLog.i(TAG, "Final quality assessment: ${finalQualityAssessment.grade} (${finalQualityAssessment.qualityScore.toInt()}%)")
                XLog.i(TAG, "Performance summary: Overall score ${performanceSummary.overallScore.toInt()}%, Memory ${performanceSummary.memoryUsageMB}MB, Battery ${performanceSummary.batteryLevel}%")
                
                advancedDataListener?.let { listener ->
                    mainHandler.post {
                        listener.onQualityAssessmentUpdated(finalQualityAssessment)
                    }
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
    
    fun isConnected(): Boolean = isConnected.get()
    
    fun isRecording(): Boolean = isRecording.get()
    
    fun getConnectedDeviceName(): String? = if (isConnected.get()) connectedDeviceName else null
    
    fun getProcessingStatistics() = dataProcessor.getStatistics()
    
    private fun handleConnectionError() {
        isConnected.set(false)
        mainHandler.post {
            gsrDataListener?.onConnectionStatusChanged(false, null)
            advancedDataListener?.onConnectionStatusChanged(false, null)
        }
    }
    
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
    
    fun cleanup() {
        try {
            if (isRecording.get()) {
                stopRecording()
            }
            
            disconnectShimmer()
            dataProcessor.reset()
            dataWriter.cleanup()
            performanceMonitor.cleanup()
            gsrDataListener = null
            advancedDataListener = null
            
            XLog.i(TAG, "Enhanced GSR Manager cleaned up")
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error during cleanup: ${e.message}", e)
        }
    }
    
    fun getRecordedFiles() = dataWriter.getRecordedFiles()
    
    fun getCurrentRecordingInfo() = dataWriter.getCurrentRecordingInfo()
    
    fun getCurrentQualityAssessment() = qualityMonitor.getCurrentQualityAssessment()
    
    fun getPerformanceSummary() = performanceMonitor.getPerformanceSummary()
    
    fun getQualityMetricsFlow() = qualityMonitor.qualityMetrics
    
    suspend fun exportGSRDataToFile(
        gsrData: List<ProcessedGSRData>,
        fileName: String? = null,
        includeAnalysis: Boolean = true
    ) = dataWriter.exportGSRDataToFile(gsrData, fileName, includeAnalysis)
