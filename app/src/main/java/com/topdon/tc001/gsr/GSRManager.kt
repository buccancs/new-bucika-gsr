package com.topdon.tc001.gsr

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.topdon.module.thermal.ir.capture.sync.EnhancedSynchronizedCaptureSystem
import com.elvishew.xlog.XLog
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Enhanced GSR Manager with synchronized timestamping
 * Integrated with global master clock for nanosecond-precision synchronization
 * Optimized for concurrent operation with video and DNG capture
 */
class EnhancedGSRManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "EnhancedGSRManager"
        private const val GSR_SAMPLING_RATE_HZ = 128
        private const val SAMPLING_INTERVAL_NS = (1_000_000_000.0 / GSR_SAMPLING_RATE_HZ).toLong()
        
        @Volatile
        private var INSTANCE: EnhancedGSRManager? = null
        
        fun getInstance(context: Context): EnhancedGSRManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EnhancedGSRManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Thread-safe state management
    private val isSimulating = AtomicBoolean(false)
    private val isRecording = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)
    private val samplesCollected = AtomicLong(0)
    private val lastSampleTimestamp = AtomicLong(0)
    
    private var simulatedDeviceName = "Enhanced Shimmer GSR Device"
    private var recordingStartTime: Long = 0
    
    // Multi-threaded components for zero-drop sampling
    private var gsrSamplingExecutor: ScheduledExecutorService? = null
    private val handler = Handler(Looper.getMainLooper())
    private var syncSystem: EnhancedSynchronizedCaptureSystem? = null
    
    // Performance monitoring
    private val samplingPerformanceMetrics = mutableMapOf<String, Double>()
    
    interface EnhancedGSRDataListener {
        fun onGSRDataReceived(
            timestamp: Long,
            synchronizedTimestamp: Long,
            gsrValue: Double,
            skinTemperature: Double,
            sampleIndex: Long
        )
        fun onConnectionStatusChanged(isConnected: Boolean, deviceName: String?)
        fun onSyncQualityChanged(syncAccuracy: Double, temporalDrift: Double)
    }
    
    private var dataListener: EnhancedGSRDataListener? = null
    
    /**
     * Set GSR data listener for synchronized data reception
     */
    fun setGSRDataListener(listener: EnhancedGSRDataListener) {
        this.dataListener = listener
        XLog.i(TAG, "Enhanced GSR data listener registered")
    }
    
    /**
     * Set synchronization system for temporal alignment
     */
    fun setSynchronizationSystem(syncSystem: EnhancedSynchronizedCaptureSystem) {
        this.syncSystem = syncSystem
        XLog.i(TAG, "Synchronization system integrated with GSR manager")
    }
    
    /**
     * Initialize Shimmer with enhanced configuration
     */
    fun initializeShimmer() {
        try {
            // Initialize global master clock if not already done
            EnhancedSynchronizedCaptureSystem.initializeGlobalMasterClock()
            
            // TODO: Initialize ShimmerAndroidAPI when dependency is available
            // Set sampling rate to 128 Hz as per requirements
            // Configure GSR range (auto-ranging)
            // Enable hardware timestamping if available
            
            XLog.i(TAG, "Enhanced Shimmer initialization completed")
            XLog.i(TAG, "Sampling rate: ${GSR_SAMPLING_RATE_HZ} Hz")
            XLog.i(TAG, "Sampling interval: ${SAMPLING_INTERVAL_NS / 1_000_000.0} ms")
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to initialize enhanced Shimmer: ${e.message}", e)
        }
    }
    
    /**
     * Start synchronized GSR recording
     */
    fun startRecording(): Boolean {
        return if (isConnected.get() && !isRecording.get()) {
            try {
                recordingStartTime = EnhancedSynchronizedCaptureSystem.getGlobalMasterClock()
                isRecording.set(true)
                samplesCollected.set(0)
                lastSampleTimestamp.set(0)
                
                startEnhancedDataStream()
                
                XLog.i(TAG, "Enhanced synchronized GSR recording started")
                XLog.i(TAG, "Recording start time: ${recordingStartTime}ns")
                true
            } catch (e: Exception) {
                XLog.e(TAG, "Failed to start enhanced GSR recording: ${e.message}", e)
                false
            }
        } else {
            XLog.w(TAG, "Cannot start recording - not connected or already recording")
            false
        }
    }
    
    /**
     * Stop synchronized GSR recording
     */
    fun stopRecording(): Boolean {
        return if (isRecording.get()) {
            try {
                isRecording.set(false)
                stopEnhancedDataStream()
                
                val recordingDuration = (EnhancedSynchronizedCaptureSystem.getGlobalMasterClock() - recordingStartTime) / 1_000_000
                val totalSamples = samplesCollected.get()
                val actualSamplingRate = if (recordingDuration > 0) {
                    (totalSamples * 1000.0) / recordingDuration
                } else 0.0
                
                XLog.i(TAG, "Enhanced GSR recording stopped")
                XLog.i(TAG, "Duration: ${recordingDuration}ms, Samples: $totalSamples")
                XLog.i(TAG, "Actual sampling rate: %.2f Hz".format(actualSamplingRate))
                
                true
            } catch (e: Exception) {
                XLog.e(TAG, "Failed to stop enhanced GSR recording: ${e.message}", e)
                false
            }
        } else {
            XLog.w(TAG, "Cannot stop recording - not currently recording")
            false
        }
    }
    
    /**
     * Connect to Shimmer with enhanced connection management
     */
    fun connectToShimmer(bluetoothAddress: String) {
        try {
            // Simulate connection for development with realistic timing
            XLog.i(TAG, "Connecting to enhanced Shimmer at $bluetoothAddress")
            
            // Simulate connection delay
            handler.postDelayed({
                isSimulating.set(true)
                isConnected.set(true)
                XLog.i(TAG, "Enhanced connection established")
                dataListener?.onConnectionStatusChanged(true, simulatedDeviceName)
            }, 1500) // More realistic connection time
            
            // TODO: Replace with actual ShimmerAndroidAPI connection
            // shimmerManager?.connectShimmerDevice(bluetoothAddress, true)
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to connect to enhanced Shimmer: ${e.message}", e)
            dataListener?.onConnectionStatusChanged(false, null)
        }
    }
    
    /**
     * Disconnect from Shimmer
     */
    fun disconnectShimmer() {
        try {
            stopRecording()
            isSimulating.set(false)
            isConnected.set(false)
            
            dataListener?.onConnectionStatusChanged(false, null)
            XLog.i(TAG, "Enhanced Shimmer disconnected")
            
            // TODO: Replace with actual Shimmer disconnection
            // shimmerManager?.disconnectShimmerDevice(bluetoothAddress)
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error disconnecting enhanced Shimmer: ${e.message}", e)
        }
    }
    
    /**
     * Check connection status
     */
    fun isConnected(): Boolean = isConnected.get()
    
    /**
     * Check recording status
     */
    fun isRecording(): Boolean = isRecording.get()
    
    /**
     * Get connected device name
     */
    fun getConnectedDeviceName(): String? = if (isConnected.get()) simulatedDeviceName else null
    
    /**
     * Get current sampling statistics
     */
    fun getSamplingStatistics(): GSRSamplingStatistics {
        val currentTime = EnhancedSynchronizedCaptureSystem.getGlobalMasterClock()
        val recordingDuration = if (isRecording.get() && recordingStartTime > 0) {
            (currentTime - recordingStartTime) / 1_000_000 // Convert to milliseconds
        } else 0L
        
        val totalSamples = samplesCollected.get()
        val actualSamplingRate = if (recordingDuration > 0) {
            (totalSamples * 1000.0) / recordingDuration
        } else 0.0
        
        return GSRSamplingStatistics(
            isRecording = isRecording.get(),
            totalSamples = totalSamples,
            recordingDurationMs = recordingDuration,
            actualSamplingRateHz = actualSamplingRate,
            targetSamplingRateHz = GSR_SAMPLING_RATE_HZ.toDouble(),
            samplingAccuracy = if (GSR_SAMPLING_RATE_HZ > 0) {
                (actualSamplingRate / GSR_SAMPLING_RATE_HZ) * 100.0
            } else 0.0,
            lastSampleTimestamp = lastSampleTimestamp.get()
        )
    }
    
    /**
     * Start enhanced data stream with precise timing and synchronization
     */
    private fun startEnhancedDataStream() {
        // Create high-priority thread for GSR sampling
        gsrSamplingExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "EnhancedGSRSampling").apply {
                priority = Thread.MAX_PRIORITY
            }
        }
        
        // Schedule precise sampling at 128 Hz
        gsrSamplingExecutor?.scheduleAtFixedRate({
            if (isRecording.get()) {
                collectSynchronizedGSRSample()
            }
        }, 0, SAMPLING_INTERVAL_NS, TimeUnit.NANOSECONDS)
        
        XLog.i(TAG, "Enhanced GSR data stream started at ${GSR_SAMPLING_RATE_HZ} Hz")
    }
    
    /**
     * Stop enhanced data stream
     */
    private fun stopEnhancedDataStream() {
        gsrSamplingExecutor?.shutdown()
        try {
            if (!gsrSamplingExecutor?.awaitTermination(1000, TimeUnit.MILLISECONDS) == true) {
                gsrSamplingExecutor?.shutdownNow()
            }
        } catch (e: InterruptedException) {
            gsrSamplingExecutor?.shutdownNow()
        }
        gsrSamplingExecutor = null
        
        XLog.i(TAG, "Enhanced GSR data stream stopped")
    }
    
    /**
     * Collect synchronized GSR sample with precise timing
     */
    private fun collectSynchronizedGSRSample() {
        try {
            val startTime = System.nanoTime()
            
            // Get synchronized timestamp from global master clock
            val globalTimestamp = EnhancedSynchronizedCaptureSystem.getGlobalMasterClock()
            val originalTimestamp = System.currentTimeMillis()
            
            // Generate realistic GSR data
            val gsrValue = generateRealisticGSRValue()
            val skinTemp = generateRealisticSkinTemperature()
            
            val sampleIndex = samplesCollected.incrementAndGet()
            lastSampleTimestamp.set(globalTimestamp)
            
            // Register with synchronization system if available
            var synchronizedTimestamp = globalTimestamp
            syncSystem?.let { sync ->
                synchronizedTimestamp = sync.registerGSRSample(gsrValue, skinTemp, originalTimestamp)
            }
            
            // Deliver data on main thread for UI updates
            handler.post {
                dataListener?.onGSRDataReceived(
                    timestamp = originalTimestamp,
                    synchronizedTimestamp = synchronizedTimestamp,
                    gsrValue = gsrValue,
                    skinTemperature = skinTemp,
                    sampleIndex = sampleIndex
                )
            }
            
            // Monitor performance
            val processingTime = (System.nanoTime() - startTime) / 1_000_000.0 // Convert to ms
            updateSamplingPerformance("sample_processing", processingTime)
            
            // Report sync quality periodically
            if (sampleIndex % (GSR_SAMPLING_RATE_HZ / 2) == 0L) { // Every 0.5 seconds
                reportSyncQuality()
            }
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error collecting synchronized GSR sample: ${e.message}", e)
        }
    }
    
    /**
     * Generate realistic GSR values with physiological patterns
     */
    private fun generateRealisticGSRValue(): Double {
        val baseGSR = 5.0
        val timeMs = (EnhancedSynchronizedCaptureSystem.getGlobalMasterClock() - recordingStartTime) / 1_000_000
        
        // Add physiological patterns (breathing, heart rate, etc.)
        val breathingPattern = 0.5 * kotlin.math.sin(timeMs * 0.001 * 2 * kotlin.math.PI / 4.0) // 4-second breathing
        val heartRatePattern = 0.2 * kotlin.math.sin(timeMs * 0.001 * 2 * kotlin.math.PI / 0.8) // ~75 BPM
        val noiseComponent = Random.nextDouble(-0.3, 0.3)
        val trendComponent = 0.001 * timeMs // Slow upward trend
        
        return kotlin.math.max(0.1, baseGSR + breathingPattern + heartRatePattern + noiseComponent + trendComponent)
    }
    
    /**
     * Generate realistic skin temperature values
     */
    private fun generateRealisticSkinTemperature(): Double {
        val baseTemp = 32.5
        val timeMs = (EnhancedSynchronizedCaptureSystem.getGlobalMasterClock() - recordingStartTime) / 1_000_000
        
        // Gradual temperature changes
        val thermalDrift = 0.0001 * timeMs // Very slow temperature drift
        val noiseComponent = Random.nextDouble(-0.1, 0.1)
        
        return baseTemp + thermalDrift + noiseComponent
    }
    
    /**
     * Update sampling performance metrics
     */
    private fun updateSamplingPerformance(metric: String, value: Double) {
        samplingPerformanceMetrics[metric] = value
        
        // Warn if sampling is too slow
        if (metric == "sample_processing" && value > 1.0) { // > 1ms processing time
            XLog.w(TAG, "Slow GSR sample processing: %.2f ms".format(value))
        }
    }
    
    /**
     * Report synchronization quality to listener
     */
    private fun reportSyncQuality() {
        syncSystem?.let { sync ->
            val metrics = sync.getSynchronizationMetrics()
            val syncAccuracy = if (metrics.gsrSamplesCaptured > 0) {
                (metrics.totalGSRSamplesSynced.toDouble() / metrics.gsrSamplesCaptured) * 100.0
            } else 0.0
            
            val temporalDrift = metrics.averageTemporalDriftNs / 1_000_000.0 // Convert to ms
            
            handler.post {
                dataListener?.onSyncQualityChanged(syncAccuracy, temporalDrift)
            }
        }
    }
    
    /**
     * Cleanup enhanced GSR manager
     */
    fun cleanup() {
        try {
            stopRecording()
            disconnectShimmer()
            
            gsrSamplingExecutor?.shutdown()
            samplingPerformanceMetrics.clear()
            
            XLog.i(TAG, "Enhanced GSR Manager cleaned up")
        } catch (e: Exception) {
            XLog.e(TAG, "Error during enhanced GSR cleanup: ${e.message}", e)
        }
    }
}

/**
 * GSR sampling statistics
 */
data class GSRSamplingStatistics(
    val isRecording: Boolean,
    val totalSamples: Long,
    val recordingDurationMs: Long,
    val actualSamplingRateHz: Double,
    val targetSamplingRateHz: Double,
    val samplingAccuracy: Double,
    val lastSampleTimestamp: Long
)
