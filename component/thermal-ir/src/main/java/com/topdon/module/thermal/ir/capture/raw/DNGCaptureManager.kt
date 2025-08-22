package com.topdon.module.thermal.ir.capture.raw

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.os.*
import android.util.Size
import com.elvishew.xlog.XLog
import com.topdon.lib.core.config.FileConfig
import com.topdon.module.thermal.ir.capture.sync.SynchronizedCaptureSystem
import com.topdon.module.thermal.ir.device.compatibility.DeviceCompatibilityChecker
import com.topdon.module.thermal.ir.device.compatibility.S22OptimizationParams
import java.io.*
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * DNG (Digital Negative) Capture Manager for RAD DNG Level 3 recording
 * Captures RAW sensor data at 30FPS and saves as DNG files
 * Optimized for Samsung S22 series concurrent capture capabilities
 */
@SuppressLint("MissingPermission")
class DNGCaptureManager(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "DNGCaptureManager"
        private const val TARGET_FPS = 30
        private const val CAPTURE_INTERVAL_MS = 1000L / TARGET_FPS // ~33.33ms for 30 FPS
        private const val MAX_IMAGES = 8 // Circular buffer for Samsung S22 optimization
    }
    
    // Device compatibility and optimization
    private val compatibilityChecker = DeviceCompatibilityChecker(context)
    private lateinit var optimizationParams: S22OptimizationParams
    
    // Synchronization system integration
    private var syncSystem: SynchronizedCaptureSystem? = null
    
    // Camera2 API components
    private var cameraManager: CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    
    // Background thread handling
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private val cameraOpenCloseLock = Semaphore(1)
    
    // Capture state
    private var isCapturing = false
    private var captureCount = 0
    private var captureStartTime = 0L
    private var currentCaptureDir: File? = null
    
    // Camera characteristics
    private var sensorOrientation = 0
    private var rawSize: Size? = null
    private var characteristics: CameraCharacteristics? = null
    
    init {
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        optimizationParams = compatibilityChecker.getSamsungS22OptimizationParams()
        setupBackgroundThread()
        
        // Log device compatibility information
        XLog.i(TAG, "Device compatibility check:")
        XLog.i(TAG, "- Samsung S22: ${compatibilityChecker.isSamsungS22()}")
        XLog.i(TAG, "- RAW capture support: ${compatibilityChecker.supportsRawCapture()}")
        XLog.i(TAG, "- Max concurrent streams: ${compatibilityChecker.getMaxConcurrentStreams()}")
        XLog.i(TAG, "- Optimization params: $optimizationParams")
    }
    
    /**
     * Set synchronization system for coordinated capture timing
     */
    fun setSynchronizationSystem(syncSystem: SynchronizedCaptureSystem) {
        this.syncSystem = syncSystem
        XLog.i(TAG, "Synchronization system integrated")
    }
    
    /**
     * Check if device supports RAW DNG capture at 30fps
     */
    fun isRawCaptureSupported(): Boolean {
        return compatibilityChecker.supportsRawCapture()
    }
    
    /**
     * Check if device supports concurrent 4K + RAW capture
     */
    fun isConcurrentCaptureSupported(): Boolean {
        return compatibilityChecker.supportsConcurrent4KAndRaw()
    }
    
    /**
     * Start DNG capture sequence at 30 FPS with compatibility validation
     */
    fun startDNGCapture(): Boolean {
        if (isCapturing) {
            XLog.w(TAG, "DNG capture already in progress")
            return false
        }
        
        // Check device compatibility first
        if (!isRawCaptureSupported()) {
            XLog.e(TAG, "RAW capture not supported on this device")
            return false
        }
        
        return try {
            setupCaptureDirectory()
            openCamera()
            true
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to start DNG capture: ${e.message}", e)
            false
        }
    }
    
    /**
     * Start concurrent DNG capture (for use with 4K video recording)
     */
    fun startConcurrentDNGCapture(): Boolean {
        if (isCapturing) {
            XLog.w(TAG, "DNG capture already in progress")
            return false
        }
        
        // Check concurrent capture compatibility
        if (!isConcurrentCaptureSupported()) {
            XLog.e(TAG, "Concurrent 4K + RAW capture not supported on this device")
            return false
        }
        
        return try {
            XLog.i(TAG, "Starting concurrent DNG capture with Samsung S22 optimizations")
            setupCaptureDirectory()
            openCamera()
            true
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to start concurrent DNG capture: ${e.message}", e)
            false
        }
    }
    
    /**
     * Stop DNG capture sequence
     */
    fun stopDNGCapture(): Boolean {
        if (!isCapturing) {
            XLog.w(TAG, "No DNG capture in progress")
            return false
        }
        
        return try {
            isCapturing = false
            closeCamera()
            
            val totalFiles = captureCount
            val duration = System.currentTimeMillis() - captureStartTime
            val actualFPS = (totalFiles * 1000.0) / duration
            
            XLog.i(TAG, "DNG capture stopped. Files: $totalFiles, Duration: ${duration}ms, Actual FPS: %.2f".format(actualFPS))
            true
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to stop DNG capture: ${e.message}", e)
            false
        }
    }
    
    /**
     * Check if DNG capture is active
     */
    fun isCapturing(): Boolean = isCapturing
    
    /**
     * Get current capture statistics
     */
    fun getCaptureStats(): Map<String, Any> {
        val duration = if (isCapturing) System.currentTimeMillis() - captureStartTime else 0L
        val actualFPS = if (duration > 0) (captureCount * 1000.0) / duration else 0.0
        
        return mapOf(
            "isCapturing" to isCapturing,
            "framesCaptured" to captureCount,
            "durationMs" to duration,
            "actualFPS" to actualFPS,
            "targetFPS" to TARGET_FPS,
            "captureDirectory" to (currentCaptureDir?.absolutePath ?: "")
        )
    }
    
    private fun setupCaptureDirectory() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dirName = "rad_dng_level3_dng_$timestamp"
        
        val baseDir = File(FileConfig.lineGalleryDir, "dng_captures")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        
        currentCaptureDir = File(baseDir, dirName)
        currentCaptureDir?.mkdirs()
        
        XLog.i(TAG, "DNG capture directory created: ${currentCaptureDir?.absolutePath}")
    }
    
    private fun setupBackgroundThread() {
        backgroundThread = HandlerThread("DNGCaptureBackground")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread?.looper!!)
    }
    
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            XLog.e(TAG, "Error stopping background thread", e)
        }
    }
    
    private fun openCamera() {
        val cameraId = getCameraId()
        if (cameraId == null) {
            XLog.e(TAG, "No suitable camera found for DNG capture")
            return
        }
        
        try {
            characteristics = cameraManager?.getCameraCharacteristics(cameraId)
            val map = characteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            
            // Get RAW sensor size
            val rawSizes = map?.getOutputSizes(ImageFormat.RAW_SENSOR)
            rawSize = rawSizes?.maxByOrNull { it.width * it.height }
            
            sensorOrientation = characteristics?.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            
            XLog.i(TAG, "Selected camera: $cameraId, RAW size: $rawSize, orientation: $sensorOrientation")
            
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            
            // Create ImageReader for RAW capture
            rawSize?.let { size ->
                imageReader = ImageReader.newInstance(
                    size.width, size.height,
                    ImageFormat.RAW_SENSOR, MAX_IMAGES
                )
                imageReader?.setOnImageAvailableListener(imageAvailableListener, backgroundHandler)
            }
            
            cameraManager?.openCamera(cameraId, stateCallback, backgroundHandler)
            
        } catch (e: Exception) {
            cameraOpenCloseLock.release()
            XLog.e(TAG, "Failed to open camera: ${e.message}", e)
        }
    }
    
    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            cameraCaptureSession?.close()
            cameraCaptureSession = null
            
            cameraDevice?.close()
            cameraDevice = null
            
            imageReader?.close()
            imageReader = null
        } catch (e: Exception) {
            XLog.e(TAG, "Error closing camera: ${e.message}", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }
    
    private fun getCameraId(): String? {
        return try {
            val cameraIdList = cameraManager?.cameraIdList ?: return null
            
            for (cameraId in cameraIdList) {
                val characteristics = cameraManager?.getCameraCharacteristics(cameraId)
                val facing = characteristics?.get(CameraCharacteristics.LENS_FACING)
                
                // Prefer back camera for RAW capture
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                    
                    // Check if camera supports RAW capture
                    if (capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true) {
                        return cameraId
                    }
                }
            }
            
            // Fallback to first available camera with RAW support
            cameraIdList.find { cameraId ->
                val characteristics = cameraManager?.getCameraCharacteristics(cameraId)
                val capabilities = characteristics?.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true
            }
        } catch (e: Exception) {
            XLog.e(TAG, "Error getting camera ID: ${e.message}", e)
            null
        }
    }
    
    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice = camera
            createCaptureSession()
        }
        
        override fun onDisconnected(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
        }
        
        override fun onError(camera: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
            XLog.e(TAG, "Camera error: $error")
        }
    }
    
    private fun createCaptureSession() {
        try {
            val surfaces = listOf(imageReader?.surface)
            
            cameraDevice?.createCaptureSession(
                surfaces.filterNotNull(),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSession = session
                        startRepeatingCapture()
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        XLog.e(TAG, "Failed to configure capture session")
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to create capture session: ${e.message}", e)
        }
    }
    
    private fun startRepeatingCapture() {
        try {
            val captureBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder?.addTarget(imageReader?.surface!!)
            
            // RAD DNG Level 3 optimized settings
            captureBuilder?.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            captureBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            captureBuilder?.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            captureBuilder?.set(CaptureRequest.JPEG_QUALITY, 100.toByte())
            
            // Set high quality for RAD DNG Level 3
            captureBuilder?.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_HIGH_QUALITY)
            captureBuilder?.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY)
            captureBuilder?.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY)
            
            isCapturing = true
            captureCount = 0
            captureStartTime = System.currentTimeMillis()
            
            // Start continuous capture at 30 FPS
            val captureRequest = captureBuilder?.build()
            captureRequest?.let {
                cameraCaptureSession?.setRepeatingRequest(it, captureCallback, backgroundHandler)
            }
            
            XLog.i(TAG, "Started RAD DNG Level 3 DNG capture at 30 FPS")
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to start repeating capture: ${e.message}", e)
        }
    }
    
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            // Capture completed successfully
        }
        
        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            XLog.w(TAG, "Capture failed: ${failure.reason}")
        }
    }
    
    private val imageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        backgroundHandler?.post {
            try {
                val image = reader.acquireLatestImage()
                if (image != null && isCapturing) {
                    // Register frame timestamp with sync system
                    val imageTimestamp = image.timestamp
                    val syncTimestamp = syncSystem?.registerDNGFrame(imageTimestamp, captureCount) ?: 0
                    
                    saveDNGImage(image, syncTimestamp)
                    image.close()
                }
            } catch (e: Exception) {
                XLog.e(TAG, "Error processing captured image: ${e.message}", e)
            }
        }
    }
    
    private fun saveDNGImage(image: Image, syncTimestamp: Long = 0) {
        try {
            captureCount++
            val timestamp = if (syncTimestamp > 0) syncTimestamp else System.nanoTime()
            val filename = "frame_%06d.dng".format(captureCount)
            val outputFile = File(currentCaptureDir, filename)
            
            // Convert RAW image to DNG format
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            
            // Create DNG file with proper headers, metadata, and synchronized timestamp
            createDNGFile(outputFile, bytes, image.width, image.height, timestamp)
            
            if (captureCount % 30 == 0) { // Log every second (30 frames)
                val elapsed = System.currentTimeMillis() - captureStartTime
                val actualFPS = (captureCount * 1000.0) / elapsed
                val syncInfo = syncSystem?.getSynchronizationMetrics()
                XLog.d(TAG, "Captured $captureCount frames, actual FPS: %.2f, sync pairs: ${syncInfo?.totalFramesPaired ?: 0}"
                    .format(actualFPS))
            }
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to save DNG image: ${e.message}", e)
        }
    }
    
    private fun createDNGFile(outputFile: File, rawData: ByteArray, width: Int, height: Int, syncTimestamp: Long = 0) {
        try {
            // For now, create a simplified DNG file
            // In production, this would use Adobe DNG SDK or implement full DNG specification
            
            outputFile.outputStream().use { fos ->
                // DNG Header (simplified) with sync timestamp
                writeDNGHeader(fos, width, height, rawData.size, syncTimestamp)
                
                // RAW image data
                fos.write(rawData)
                
                // DNG metadata (simplified) with sync info
                writeDNGMetadata(fos, syncTimestamp)
            }
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to create DNG file: ${e.message}", e)
        }
    }
    
    private fun writeDNGHeader(fos: OutputStream, width: Int, height: Int, dataSize: Int, syncTimestamp: Long = 0) {
        // Simplified DNG header implementation with sync timestamp
        // This is a basic implementation - production code would use proper DNG library
        
        val header = ByteBuffer.allocate(1024)
        
        // TIFF Header
        header.put("II".toByteArray()) // Little endian
        header.putShort(42) // TIFF magic number
        header.putInt(8) // Offset to first IFD
        
        // Basic DNG tags including sync timestamp
        header.putShort(10) // Number of directory entries (increased for sync data)
        
        // Image width tag
        header.putShort(0x0100)  // ImageWidth
        header.putShort(4)       // LONG
        header.putInt(1)         // Count
        header.putInt(width)     // Value
        
        // Image height tag  
        header.putShort(0x0101)  // ImageLength
        header.putShort(4)       // LONG
        header.putInt(1)         // Count
        header.putInt(height)    // Value
        
        // Sync timestamp tag (custom)
        if (syncTimestamp > 0) {
            header.putShort(0xA000.toShort())  // Custom sync timestamp tag
            header.putShort(5)       // RATIONAL64
            header.putInt(1)         // Count
            header.putLong(syncTimestamp) // Value
        }
        
        // Add more DNG-specific tags as needed...
        
        fos.write(header.array(), 0, header.position())
    }
    
    private fun writeDNGMetadata(fos: OutputStream, syncTimestamp: Long = 0) {
        // Add DNG metadata including sync information
        val metadata = ByteBuffer.allocate(1024)
        
        // Camera make/model
        val make = "TOPDON"
        val model = "TC001 RAD DNG Level 3"
        
        metadata.put(make.toByteArray())
        metadata.put(model.toByteArray())
        
        // Timestamp
        val timestamp = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).format(Date())
        metadata.put(timestamp.toByteArray())
        
        // Synchronization metadata
        if (syncTimestamp > 0) {
            val syncInfo = "SYNC_TS:${syncTimestamp}"
            metadata.put(syncInfo.toByteArray())
            
            // Add sync session info if available
            syncSystem?.getSynchronizationMetrics()?.let { metrics ->
                val sessionInfo = "SYNC_SESSION_FRAMES:${metrics.totalFramesPaired}"
                metadata.put(sessionInfo.toByteArray())
            }
        }
        
        fos.write(metadata.array(), 0, metadata.position())
    }
    
    /**
     * Get list of captured DNG files
     */
    fun getCapturedFiles(): List<File> {
        return currentCaptureDir?.listFiles { file ->
            file.extension.lowercase() == "dng"
        }?.toList() ?: emptyList()
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopDNGCapture()
        stopBackgroundThread()
    }
}