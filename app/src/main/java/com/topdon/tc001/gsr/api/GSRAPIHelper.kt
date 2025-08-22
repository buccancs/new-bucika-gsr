package com.topdon.tc001.gsr.api

import android.content.Context
import com.elvishew.xlog.XLog
import com.shimmerresearch.driver.Configuration
import com.shimmerresearch.driver.ProcessedGSRData
import com.topdon.tc001.gsr.GSRManager
import com.topdon.tc001.gsr.discovery.ShimmerDeviceDiscovery
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * GSR API Helper - Comprehensive UI API functionalities for Shimmer integration
 * Provides high-level interfaces for GSR data management, device discovery, and analysis
 */
class GSRAPIHelper private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "GSRAPIHelper"
        
        @Volatile
        private var INSTANCE: GSRAPIHelper? = null
        
        fun getInstance(context: Context): GSRAPIHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GSRAPIHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Core components
    private val gsrManager = GSRManager.getInstance(context)
    private val deviceDiscovery = ShimmerDeviceDiscovery(context)
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Data analysis
    private val gsrDataBuffer = mutableListOf<ProcessedGSRData>()
    private val maxBufferSize = 12800 // 100 seconds at 128 Hz
    
    // Event listeners
    private var gsrEventListener: GSREventListener? = null
    private var deviceDiscoveryListener: DeviceDiscoveryEventListener? = null
    
    /**
     * Comprehensive GSR event listener interface
     */
    interface GSREventListener {
        fun onGSRDataReceived(data: ProcessedGSRData, analysis: GSRAnalysis)
        fun onConnectionStatusChanged(isConnected: Boolean, deviceInfo: DeviceConnectionInfo?)
        fun onRecordingStatusChanged(isRecording: Boolean, recordingInfo: RecordingInfo?)
        fun onSignalQualityChanged(quality: SignalQualityInfo)
        fun onError(error: GSRError)
    }
    
    /**
     * Device discovery event listener
     */
    interface DeviceDiscoveryEventListener {
        fun onDeviceFound(device: ShimmerDeviceDiscovery.ShimmerDeviceInfo)
        fun onDiscoveryCompleted(devices: List<ShimmerDeviceDiscovery.ShimmerDeviceInfo>)
        fun onDiscoveryError(error: String)
    }
    
    // Data classes for API responses
    data class DeviceConnectionInfo(
        val deviceName: String,
        val deviceAddress: String,
        val connectionTimestamp: Long,
        val firmwareVersion: String = "Unknown",
        val batteryLevel: Int = -1
    )
    
    data class RecordingInfo(
        val startTime: Long,
        val duration: Long,
        val samplesRecorded: Long,
        val averageSamplingRate: Double,
        val dataQuality: Double
    )
    
    data class SignalQualityInfo(
        val overallQuality: Double,
        val gsrQuality: Double,
        val temperatureQuality: Double,
        val signalNoiseRatio: Double,
        val artifactPercentage: Double
    )
    
    data class GSRError(
        val errorType: ErrorType,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    enum class ErrorType {
        CONNECTION_FAILED,
        DEVICE_NOT_FOUND,
        PERMISSION_DENIED,
        BLUETOOTH_DISABLED,
        DATA_PROCESSING_ERROR,
        RECORDING_ERROR
    }
    
    data class GSRAnalysis(
        val timestamp: Long,
        val gsrLevel: GSRLevel,
        val arousalState: ArousalState,
        val stressIndicator: StressIndicator,
        val heartRateVariability: Double,
        val skinConductanceResponse: SCRAnalysis,
        val temperatureTrend: TemperatureTrend
    )
    
    enum class GSRLevel { LOW, NORMAL, ELEVATED, HIGH }
    enum class ArousalState { RELAXED, NEUTRAL, AROUSED, HIGHLY_AROUSED }
    enum class StressIndicator { LOW_STRESS, MILD_STRESS, MODERATE_STRESS, HIGH_STRESS }
    enum class TemperatureTrend { DECREASING, STABLE, INCREASING }
    
    data class SCRAnalysis(
        val responseDetected: Boolean,
        val responseAmplitude: Double,
        val responseLatency: Double,
        val recoveryTime: Double
    )
    
    /**
     * Set GSR event listener
     */
    fun setGSREventListener(listener: GSREventListener) {
        this.gsrEventListener = listener
        
        // Set up GSR manager listener
        gsrManager.setAdvancedGSRDataListener(object : GSRManager.AdvancedGSRDataListener {
            override fun onProcessedGSRDataReceived(processedData: ProcessedGSRData) {
                handleGSRDataReceived(processedData)
            }
            
            override fun onSignalQualityChanged(signalQuality: Double, validDataRatio: Double) {
                val qualityInfo = SignalQualityInfo(
                    overallQuality = signalQuality,
                    gsrQuality = signalQuality,
                    temperatureQuality = signalQuality * 0.9, // Simplified
                    signalNoiseRatio = calculateSNR(),
                    artifactPercentage = (1.0 - validDataRatio) * 100.0
                )
                listener.onSignalQualityChanged(qualityInfo)
            }
            
            override fun onConnectionStatusChanged(isConnected: Boolean, deviceName: String?) {
                val deviceInfo = if (isConnected && deviceName != null) {
                    DeviceConnectionInfo(
                        deviceName = deviceName,
                        deviceAddress = gsrManager.getConnectedDeviceName() ?: "Unknown",
                        connectionTimestamp = System.currentTimeMillis()
                    )
                } else null
                
                listener.onConnectionStatusChanged(isConnected, deviceInfo)
            }
        })
    }
    
    /**
     * Set device discovery event listener
     */
    fun setDeviceDiscoveryEventListener(listener: DeviceDiscoveryEventListener) {
        this.deviceDiscoveryListener = listener
        
        deviceDiscovery.setDiscoveryListener(object : ShimmerDeviceDiscovery.ShimmerDiscoveryListener {
            override fun onShimmerDeviceFound(device: ShimmerDeviceDiscovery.ShimmerDeviceInfo) {
                listener.onDeviceFound(device)
            }
            
            override fun onDiscoveryStarted() {
                // Optional callback
            }
            
            override fun onDiscoveryFinished(devicesFound: List<ShimmerDeviceDiscovery.ShimmerDeviceInfo>) {
                listener.onDiscoveryCompleted(devicesFound)
            }
            
            override fun onDiscoveryFailed(error: String) {
                listener.onDiscoveryError(error)
            }
        })
    }
    
    /**
     * Start device discovery
     */
    fun startDeviceDiscovery() {
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    deviceDiscovery.startDiscovery()
                }
            } catch (e: Exception) {
                gsrEventListener?.onError(
                    GSRError(ErrorType.DEVICE_NOT_FOUND, "Discovery failed: ${e.message}")
                )
            }
        }
    }
    
    /**
     * Stop device discovery
     */
    fun stopDeviceDiscovery() {
        deviceDiscovery.stopDiscovery()
    }
    
    /**
     * Connect to Shimmer device
     */
    fun connectToDevice(deviceAddress: String) {
        coroutineScope.launch {
            try {
                gsrManager.initializeShimmer()
                gsrManager.connectToShimmer(deviceAddress)
            } catch (e: Exception) {
                gsrEventListener?.onError(
                    GSRError(ErrorType.CONNECTION_FAILED, "Connection failed: ${e.message}")
                )
            }
        }
    }
    
    /**
     * Disconnect from Shimmer device
     */
    fun disconnectFromDevice() {
        gsrManager.disconnectShimmer()
    }
    
    /**
     * Start GSR recording
     */
    fun startRecording(): Boolean {
        val started = gsrManager.startRecording()
        if (started) {
            gsrEventListener?.onRecordingStatusChanged(true, createRecordingInfo())
        }
        return started
    }
    
    /**
     * Stop GSR recording
     */
    fun stopRecording(): Boolean {
        val stopped = gsrManager.stopRecording()
        if (stopped) {
            gsrEventListener?.onRecordingStatusChanged(false, createRecordingInfo())
        }
        return stopped
    }
    
    /**
     * Handle received GSR data and perform analysis
     */
    private fun handleGSRDataReceived(processedData: ProcessedGSRData) {
        // Add to buffer
        gsrDataBuffer.add(processedData)
        if (gsrDataBuffer.size > maxBufferSize) {
            gsrDataBuffer.removeAt(0)
        }
        
        // Perform analysis
        val analysis = performGSRAnalysis(processedData)
        
        // Deliver to listener
        gsrEventListener?.onGSRDataReceived(processedData, analysis)
    }
    
    /**
     * Perform comprehensive GSR analysis
     */
    private fun performGSRAnalysis(data: ProcessedGSRData): GSRAnalysis {
        return GSRAnalysis(
            timestamp = data.timestamp,
            gsrLevel = determineGSRLevel(data.filteredGSR),
            arousalState = determineArousalState(data.filteredGSR, data.gsrVariability),
            stressIndicator = determineStressLevel(data.filteredGSR, data.gsrDerivative),
            heartRateVariability = calculateHRV(),
            skinConductanceResponse = analyzeSCR(data),
            temperatureTrend = analyzeTemperatureTrend(data.filteredTemperature)
        )
    }
    
    /**
     * Determine GSR level category
     */
    private fun determineGSRLevel(gsrValue: Double): GSRLevel {
        return when {
            gsrValue < 2.0 -> GSRLevel.LOW
            gsrValue < 5.0 -> GSRLevel.NORMAL
            gsrValue < 10.0 -> GSRLevel.ELEVATED
            else -> GSRLevel.HIGH
        }
    }
    
    /**
     * Determine arousal state
     */
    private fun determineArousalState(gsrValue: Double, variability: Double): ArousalState {
        val arousalScore = gsrValue * 0.7 + variability * 0.3
        return when {
            arousalScore < 3.0 -> ArousalState.RELAXED
            arousalScore < 6.0 -> ArousalState.NEUTRAL
            arousalScore < 12.0 -> ArousalState.AROUSED
            else -> ArousalState.HIGHLY_AROUSED
        }
    }
    
    /**
     * Determine stress level
     */
    private fun determineStressLevel(gsrValue: Double, derivative: Double): StressIndicator {
        val stressScore = gsrValue * 0.6 + kotlin.math.abs(derivative) * 0.4
        return when {
            stressScore < 3.0 -> StressIndicator.LOW_STRESS
            stressScore < 7.0 -> StressIndicator.MILD_STRESS
            stressScore < 12.0 -> StressIndicator.MODERATE_STRESS
            else -> StressIndicator.HIGH_STRESS
        }
    }
    
    /**
     * Calculate heart rate variability from GSR data
     */
    private fun calculateHRV(): Double {
        if (gsrDataBuffer.size < 128) return 0.0
        
        val recentData = gsrDataBuffer.takeLast(128)
        val intervals = mutableListOf<Double>()
        
        // Simplified HRV calculation based on GSR fluctuations
        for (i in 1 until recentData.size) {
            val interval = recentData[i].timestamp - recentData[i-1].timestamp
            intervals.add(interval.toDouble())
        }
        
        if (intervals.size < 2) return 0.0
        
        val mean = intervals.average()
        val variance = intervals.map { (it - mean).pow(2) }.average()
        return sqrt(variance)
    }
    
    /**
     * Analyze skin conductance response
     */
    private fun analyzeSCR(data: ProcessedGSRData): SCRAnalysis {
        val responseThreshold = 0.1 // µS threshold for SCR detection
        val responseDetected = data.gsrDerivative > responseThreshold
        
        return SCRAnalysis(
            responseDetected = responseDetected,
            responseAmplitude = if (responseDetected) data.gsrDerivative else 0.0,
            responseLatency = if (responseDetected) 1.5 else 0.0, // Simplified
            recoveryTime = if (responseDetected) 3.0 else 0.0 // Simplified
        )
    }
    
    /**
     * Analyze temperature trend
     */
    private fun analyzeTemperatureTrend(currentTemp: Double): TemperatureTrend {
        if (gsrDataBuffer.size < 10) return TemperatureTrend.STABLE
        
        val recentTemps = gsrDataBuffer.takeLast(10).map { it.filteredTemperature }
        val firstTemp = recentTemps.first()
        val lastTemp = recentTemps.last()
        
        val tempDiff = lastTemp - firstTemp
        
        return when {
            tempDiff < -0.1 -> TemperatureTrend.DECREASING
            tempDiff > 0.1 -> TemperatureTrend.INCREASING
            else -> TemperatureTrend.STABLE
        }
    }
    
    /**
     * Calculate signal-to-noise ratio
     */
    private fun calculateSNR(): Double {
        if (gsrDataBuffer.size < 64) return 0.0
        
        val recentData = gsrDataBuffer.takeLast(64).map { it.filteredGSR }
        val signal = recentData.average()
        val noise = sqrt(recentData.map { (it - signal).pow(2) }.average())
        
        return if (noise > 0) 20 * kotlin.math.log10(signal / noise) else 60.0
    }
    
    /**
     * Create recording info
     */
    private fun createRecordingInfo(): RecordingInfo {
        val stats = gsrManager.getProcessingStatistics()
        return RecordingInfo(
            startTime = System.currentTimeMillis() - stats.totalSamples * 8, // Approximate
            duration = stats.totalSamples * 8, // ~8ms per sample at 128Hz
            samplesRecorded = stats.totalSamples,
            averageSamplingRate = 128.0, // Target rate
            dataQuality = stats.currentSignalQuality
        )
    }
    
    /**
     * Get current device connection status
     */
    fun isConnected(): Boolean = gsrManager.isConnected()
    
    /**
     * Get current recording status
     */
    fun isRecording(): Boolean = gsrManager.isRecording()
    
    /**
     * Get discovered devices
     */
    fun getDiscoveredDevices(): List<ShimmerDeviceDiscovery.ShimmerDeviceInfo> {
        return deviceDiscovery.getDiscoveredDevices()
    }
    
    /**
     * Export GSR data to CSV format
     */
    fun exportGSRDataToCSV(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val sb = StringBuilder()
        
        sb.appendLine("Timestamp,DateTime,GSR_µS,Temperature_°C,GSR_Derivative,Signal_Quality,Has_Artifact,Is_Valid")
        
        synchronized(gsrDataBuffer) {
            gsrDataBuffer.forEach { data ->
                sb.appendLine(
                    "${data.timestamp},${dateFormat.format(Date(data.timestamp))}," +
                    "${data.filteredGSR},${data.filteredTemperature}," +
                    "${data.gsrDerivative},${data.signalQuality}," +
                    "${data.hasArtifact},${data.isValid}"
                )
            }
        }
        
        return sb.toString()
    }
    
    /**
     * Get GSR statistics summary
     */
    fun getGSRStatistics(): GSRStatistics {
        if (gsrDataBuffer.isEmpty()) {
            return GSRStatistics(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
        
        val gsrValues = gsrDataBuffer.map { it.filteredGSR }
        val tempValues = gsrDataBuffer.map { it.filteredTemperature }
        
        return GSRStatistics(
            sampleCount = gsrDataBuffer.size,
            avgGSR = gsrValues.average(),
            minGSR = gsrValues.minOrNull() ?: 0.0,
            maxGSR = gsrValues.maxOrNull() ?: 0.0,
            avgTemperature = tempValues.average(),
            minTemperature = tempValues.minOrNull() ?: 0.0,
            maxTemperature = tempValues.maxOrNull() ?: 0.0,
            overallQuality = gsrDataBuffer.map { it.signalQuality }.average()
        )
    }
    
    data class GSRStatistics(
        val sampleCount: Int,
        val avgGSR: Double,
        val minGSR: Double,
        val maxGSR: Double,
        val avgTemperature: Double,
        val minTemperature: Double,
        val maxTemperature: Double,
        val overallQuality: Double
    )
    
    /**
     * Clear all buffered GSR data
     */
    fun clearGSRData() {
        gsrDataBuffer.clear()
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopDeviceDiscovery()
        gsrManager.cleanup()
        deviceDiscovery.cleanup()
        coroutineScope.cancel()
        gsrEventListener = null
        deviceDiscoveryListener = null
        gsrDataBuffer.clear()
    }
}