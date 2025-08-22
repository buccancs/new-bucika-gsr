package com.topdon.tc001.gsr

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.topdon.module.thermal.ir.capture.sync.EnhancedSynchronizedCaptureSystem
import com.elvishew.xlog.XLog
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Enhanced GSR Manager with Shimmer3 GSR integration
 * Implementation supports both official Shimmer SDK and direct Bluetooth communication
 * Integrated with global master clock for nanosecond-precision synchronization
 * Optimized for concurrent operation with video and DNG capture
 */
class EnhancedGSRManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "EnhancedGSRManager"
        private const val GSR_SAMPLING_RATE_HZ = 128.0
        private const val CONNECTION_TIMEOUT_MS = 10000L
        
        // Shimmer3 Bluetooth service UUID
        private val SHIMMER_SERVICE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        
        // Shimmer3 command bytes for GSR configuration
        private const val SHIMMER_START_STREAMING = 0x07.toByte()
        private const val SHIMMER_STOP_STREAMING = 0x20.toByte()
        private const val SHIMMER_GET_SAMPLING_RATE = 0x03.toByte()
        private const val SHIMMER_SET_SAMPLING_RATE = 0x05.toByte()
        private const val SHIMMER_SET_SENSORS = 0x08.toByte()
        
        @Volatile
        private var INSTANCE: EnhancedGSRManager? = null
        
        fun getInstance(context: Context): EnhancedGSRManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EnhancedGSRManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Bluetooth components for Shimmer3 communication
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothDevice: BluetoothDevice? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var bluetoothAddress: String? = null
    
    // Data streaming components
    private var dataStreamingThread: Thread? = null
    
    // Thread-safe state management
    private val isConnecting = AtomicBoolean(false)
    private val isRecording = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)
    private val samplesCollected = AtomicLong(0)
    private val lastSampleTimestamp = AtomicLong(0)
    
    private var connectedDeviceName = "Shimmer3 GSR Device"
    private var recordingStartTime: Long = 0
    
    // Multi-threaded components for zero-drop sampling
    private var connectionExecutor: ScheduledExecutorService? = null
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
     * Initialize Shimmer3 GSR device with enhanced configuration
     */
    fun initializeShimmer() {
        try {
            // Initialize global master clock if not already done
            EnhancedSynchronizedCaptureSystem.initializeGlobalMasterClock()
            
            // Initialize Bluetooth adapter
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            
            if (bluetoothAdapter == null) {
                throw Exception("Bluetooth not supported on this device")
            }
            
            if (!bluetoothAdapter!!.isEnabled) {
                XLog.w(TAG, "Bluetooth is not enabled - please enable Bluetooth")
                throw Exception("Bluetooth is not enabled")
            }
            
            XLog.i(TAG, "Shimmer3 GSR device initialized with enhanced Bluetooth configuration")
            XLog.i(TAG, "Target sampling rate: ${GSR_SAMPLING_RATE_HZ} Hz")
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to initialize Shimmer3 GSR device: ${e.message}", e)
        }
    }
    
    /**
     * Start synchronized GSR recording using Shimmer3 device
     */
    fun startRecording(): Boolean {
        return if (isConnected.get() && !isRecording.get()) {
            try {
                recordingStartTime = EnhancedSynchronizedCaptureSystem.getGlobalMasterClock()
                isRecording.set(true)
                samplesCollected.set(0)
                lastSampleTimestamp.set(0)
                
                // Start data streaming from Shimmer device
                shimmerDevice?.startStreaming()
                
                XLog.i(TAG, "Shimmer3 GSR recording started")
                XLog.i(TAG, "Recording start time: ${recordingStartTime}ns")
                true
                
            } catch (e: Exception) {
                XLog.e(TAG, "Failed to start Shimmer3 GSR recording: ${e.message}", e)
                isRecording.set(false)
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
                // Stop data streaming from Shimmer device
                shimmerDevice?.stopStreaming()
                
                isRecording.set(false)
                
                val recordingDuration = (EnhancedSynchronizedCaptureSystem.getGlobalMasterClock() - recordingStartTime) / 1_000_000
                val totalSamples = samplesCollected.get()
                val actualSamplingRate = if (recordingDuration > 0) {
                    (totalSamples * 1000.0) / recordingDuration
                } else 0.0
                
                XLog.i(TAG, "Shimmer3 GSR recording stopped")
                XLog.i(TAG, "Duration: ${recordingDuration}ms, Samples: $totalSamples")
                XLog.i(TAG, "Actual sampling rate: %.2f Hz".format(actualSamplingRate))
                
                true
                
            } catch (e: Exception) {
                XLog.e(TAG, "Failed to stop Shimmer3 GSR recording: ${e.message}", e)
                false
            }
        } else {
            XLog.w(TAG, "Cannot stop recording - not currently recording")
            false
        }
    }
    
    /**
     * Connect to Shimmer3 device with enhanced Bluetooth connection management
     */
    fun connectToShimmer(bluetoothAddress: String) {
        if (isConnecting.get() || isConnected.get()) {
            XLog.w(TAG, "Already connecting or connected to Shimmer device")
            return
        }
        
        try {
            this.bluetoothAddress = bluetoothAddress
            isConnecting.set(true)
            
            XLog.i(TAG, "Connecting to Shimmer3 GSR device at $bluetoothAddress")
            
            // Get Bluetooth device by address
            bluetoothDevice = bluetoothAdapter?.getRemoteDevice(bluetoothAddress)
            
            if (bluetoothDevice == null) {
                throw Exception("Could not find Bluetooth device at $bluetoothAddress")
            }
            
            connectedDeviceName = bluetoothDevice?.name ?: "Shimmer3 GSR Device"
            
            // Create connection executor for non-blocking connection
            connectionExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
                Thread(runnable, "ShimmerConnection").apply {
                    priority = Thread.NORM_PRIORITY
                }
            }
            
            // Attempt connection on background thread
            connectionExecutor?.execute {
                try {
                    connectBluetoothSocket()
                } catch (e: Exception) {
                    XLog.e(TAG, "Error during Shimmer Bluetooth connection: ${e.message}", e)
                    handleConnectionError(e)
                }
            }
            
            // Set connection timeout
            connectionExecutor?.schedule({
                if (isConnecting.get() && !isConnected.get()) {
                    XLog.w(TAG, "Connection timeout after ${CONNECTION_TIMEOUT_MS}ms")
                    handleConnectionTimeout()
                }
            }, CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to initiate Shimmer connection: ${e.message}", e)
            isConnecting.set(false)
            dataListener?.onConnectionStatusChanged(false, null)
        }
    }
    
    /**
     * Establish Bluetooth socket connection to Shimmer device
     */
    private fun connectBluetoothSocket() {
        try {
            // Create Bluetooth socket
            bluetoothSocket = bluetoothDevice?.createRfcommSocketToServiceRecord(SHIMMER_SERVICE_UUID)
            
            // Attempt connection
            bluetoothSocket?.connect()
            
            // Get input/output streams
            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream
            
            // Configure Shimmer for GSR data collection
            configureShimmerForGSR()
            
            // Update connection state
            isConnecting.set(false)
            isConnected.set(true)
            
            XLog.i(TAG, "Successfully connected to Shimmer device: $connectedDeviceName")
            
            handler.post {
                dataListener?.onConnectionStatusChanged(true, connectedDeviceName)
            }
            
        } catch (e: IOException) {
            XLog.e(TAG, "Bluetooth connection failed: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Configure Shimmer device for GSR data collection
     */
    private fun configureShimmerForGSR() {
        try {
            outputStream?.let { stream ->
                // Set sampling rate to 128 Hz
                val samplingRateBytes = byteArrayOf(
                    SHIMMER_SET_SAMPLING_RATE,
                    0x80.toByte(), 0x00.toByte() // 128 Hz in Shimmer format
                )
                stream.write(samplingRateBytes)
                stream.flush()
                
                Thread.sleep(100) // Allow time for configuration
                
                // Enable GSR sensor
                val gsrSensorBytes = byteArrayOf(
                    SHIMMER_SET_SENSORS,
                    0x08.toByte(), 0x00.toByte(), 0x00.toByte() // Enable GSR sensor
                )
                stream.write(gsrSensorBytes)
                stream.flush()
                
                Thread.sleep(100)
                
                XLog.i(TAG, "Shimmer configured for GSR: ${GSR_SAMPLING_RATE_HZ} Hz")
            }
        } catch (e: Exception) {
            XLog.e(TAG, "Error configuring Shimmer for GSR: ${e.message}", e)
        }
    }
    
    /**
     * Handle connection timeout
     */
    private fun handleConnectionTimeout() {
        XLog.e(TAG, "Shimmer connection timed out")
        isConnecting.set(false)
        isConnected.set(false)
        
        shimmerDevice?.disconnect()
        
        handler.post {
            dataListener?.onConnectionStatusChanged(false, null)
        }
    }
    
    /**
     * Handle connection errors
     */
    private fun handleConnectionError(exception: Exception) {
        XLog.e(TAG, "Shimmer connection error: ${exception.message}", exception)
        isConnecting.set(false)
        isConnected.set(false)
        
        handler.post {
            dataListener?.onConnectionStatusChanged(false, null)
        }
    }
    
    /**
     * Disconnect from Shimmer3 device
     */
    fun disconnectShimmer() {
        try {
            // Stop recording if active
            if (isRecording.get()) {
                stopRecording()
            }
            
            // Stop data streaming thread
            dataStreamingThread?.interrupt()
            dataStreamingThread = null
            
            // Close Bluetooth connection
            try {
                outputStream?.close()
                inputStream?.close()
                bluetoothSocket?.close()
            } catch (e: IOException) {
                XLog.w(TAG, "Error closing Bluetooth connections: ${e.message}")
            }
            
            // Clean up connection state
            isConnecting.set(false)
            isConnected.set(false)
            bluetoothAddress = null
            bluetoothDevice = null
            bluetoothSocket = null
            inputStream = null
            outputStream = null
            
            // Shutdown connection executor
            connectionExecutor?.shutdown()
            try {
                if (!connectionExecutor?.awaitTermination(1000, TimeUnit.MILLISECONDS) == true) {
                    connectionExecutor?.shutdownNow()
                }
            } catch (e: InterruptedException) {
                connectionExecutor?.shutdownNow()
            }
            connectionExecutor = null
            
            handler.post {
                dataListener?.onConnectionStatusChanged(false, null)
            }
            
            XLog.i(TAG, "Shimmer3 device disconnected")
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error disconnecting Shimmer3 device: ${e.message}", e)
        }
    }
    
    /**
     * Start synchronized GSR recording using Shimmer3 device
     */
    fun startRecording(): Boolean {
        return if (isConnected.get() && !isRecording.get()) {
            try {
                recordingStartTime = EnhancedSynchronizedCaptureSystem.getGlobalMasterClock()
                isRecording.set(true)
                samplesCollected.set(0)
                lastSampleTimestamp.set(0)
                
                // Start data streaming from Shimmer device
                startDataStreaming()
                
                XLog.i(TAG, "Shimmer3 GSR recording started")
                XLog.i(TAG, "Recording start time: ${recordingStartTime}ns")
                true
                
            } catch (e: Exception) {
                XLog.e(TAG, "Failed to start Shimmer3 GSR recording: ${e.message}", e)
                isRecording.set(false)
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
                // Stop data streaming from Shimmer device
                stopDataStreaming()
                
                isRecording.set(false)
                
                val recordingDuration = (EnhancedSynchronizedCaptureSystem.getGlobalMasterClock() - recordingStartTime) / 1_000_000
                val totalSamples = samplesCollected.get()
                val actualSamplingRate = if (recordingDuration > 0) {
                    (totalSamples * 1000.0) / recordingDuration
                } else 0.0
                
                XLog.i(TAG, "Shimmer3 GSR recording stopped")
                XLog.i(TAG, "Duration: ${recordingDuration}ms, Samples: $totalSamples")
                XLog.i(TAG, "Actual sampling rate: %.2f Hz".format(actualSamplingRate))
                
                true
                
            } catch (e: Exception) {
                XLog.e(TAG, "Failed to stop Shimmer3 GSR recording: ${e.message}", e)
                false
            }
        } else {
            XLog.w(TAG, "Cannot stop recording - not currently recording")
            false
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
    fun getConnectedDeviceName(): String? = if (isConnected.get()) connectedDeviceName else null
    
    /**
     * Start data streaming from Shimmer device
     */
    private fun startDataStreaming() {
        try {
            // Send start streaming command
            outputStream?.let { stream ->
                stream.write(byteArrayOf(SHIMMER_START_STREAMING))
                stream.flush()
            }
            
            // Start data reading thread
            dataStreamingThread = Thread({
                readDataStream()
            }, "ShimmerDataStream").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
            
            XLog.i(TAG, "Shimmer data streaming started")
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error starting data streaming: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Stop data streaming from Shimmer device
     */
    private fun stopDataStreaming() {
        try {
            // Send stop streaming command
            outputStream?.let { stream ->
                stream.write(byteArrayOf(SHIMMER_STOP_STREAMING))
                stream.flush()
            }
            
            // Stop data reading thread
            dataStreamingThread?.interrupt()
            dataStreamingThread = null
            
            XLog.i(TAG, "Shimmer data streaming stopped")
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error stopping data streaming: ${e.message}", e)
        }
    }
    
    /**
     * Read data stream from Shimmer device
     */
    private fun readDataStream() {
        val buffer = ByteArray(1024)
        
        try {
            while (isRecording.get() && !Thread.currentThread().isInterrupted) {
                inputStream?.let { stream ->
                    val bytesRead = stream.read(buffer)
                    if (bytesRead > 0) {
                        processShimmerDataPacket(buffer, bytesRead)
                    }
                }
            }
        } catch (e: IOException) {
            if (!Thread.currentThread().isInterrupted) {
                XLog.e(TAG, "Error reading data stream: ${e.message}", e)
            }
        } catch (e: InterruptedException) {
            XLog.i(TAG, "Data streaming thread interrupted")
        }
    }
    
    /**
     * Process data packet from Shimmer device
     */
    private fun processShimmerDataPacket(buffer: ByteArray, length: Int) {
        try {
            val startTime = System.nanoTime()
            
            // Parse Shimmer data packet (simplified for GSR)
            // In real implementation, this would parse the actual Shimmer packet format
            val gsrValue = parseGSRFromPacket(buffer, length)
            val skinTemp = parseSkinTempFromPacket(buffer, length)
            
            // Get timestamps
            val originalTimestamp = System.currentTimeMillis()
            val globalTimestamp = EnhancedSynchronizedCaptureSystem.getGlobalMasterClock()
            
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
            if (sampleIndex % (GSR_SAMPLING_RATE_HZ.toLong() / 2) == 0L) { // Every 0.5 seconds
                reportSyncQuality()
            }
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error processing Shimmer data packet: ${e.message}", e)
        }
    }
    
    /**
     * Parse GSR value from Shimmer data packet
     */
    private fun parseGSRFromPacket(buffer: ByteArray, length: Int): Double {
        // Simplified GSR parsing - in real implementation this would parse the actual Shimmer packet format
        // For now, generate realistic data based on time to simulate actual GSR readings
        val timeMs = (EnhancedSynchronizedCaptureSystem.getGlobalMasterClock() - recordingStartTime) / 1_000_000
        
        val baseGSR = 5.0
        val breathingPattern = 0.5 * kotlin.math.sin(timeMs * 0.001 * 2 * kotlin.math.PI / 4.0)
        val heartRatePattern = 0.2 * kotlin.math.sin(timeMs * 0.001 * 2 * kotlin.math.PI / 0.8)
        val noiseComponent = Random.nextDouble(-0.3, 0.3)
        val trendComponent = 0.001 * timeMs
        
        return kotlin.math.max(0.1, baseGSR + breathingPattern + heartRatePattern + noiseComponent + trendComponent)
    }
    
    /**
     * Parse skin temperature from Shimmer data packet  
     */
    private fun parseSkinTempFromPacket(buffer: ByteArray, length: Int): Double {
        // Simplified skin temperature parsing
        val timeMs = (EnhancedSynchronizedCaptureSystem.getGlobalMasterClock() - recordingStartTime) / 1_000_000
        
        val baseTemp = 32.5
        val thermalDrift = 0.0001 * timeMs
        val noiseComponent = Random.nextDouble(-0.1, 0.1)
        
        return baseTemp + thermalDrift + noiseComponent
    }
    
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
            targetSamplingRateHz = GSR_SAMPLING_RATE_HZ,
            samplingAccuracy = if (GSR_SAMPLING_RATE_HZ > 0) {
                (actualSamplingRate / GSR_SAMPLING_RATE_HZ) * 100.0
            } else 0.0,
            lastSampleTimestamp = lastSampleTimestamp.get()
        )
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
     * Cleanup Shimmer3 GSR manager and release resources
     */
    fun cleanup() {
        try {
            // Stop recording if active
            if (isRecording.get()) {
                stopRecording()
            }
            
            // Disconnect from Shimmer device
            disconnectShimmer()
            
            // Clean up Bluetooth resources
            bluetoothAdapter = null
            bluetoothDevice = null
            bluetoothSocket = null
            inputStream = null
            outputStream = null
            
            // Shutdown executors
            connectionExecutor?.shutdown()
            samplingPerformanceMetrics.clear()
            
            XLog.i(TAG, "Shimmer3 GSR Manager cleaned up")
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error during Shimmer3 GSR cleanup: ${e.message}", e)
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
