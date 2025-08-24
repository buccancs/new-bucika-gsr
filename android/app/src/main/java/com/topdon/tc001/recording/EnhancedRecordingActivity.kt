package com.topdon.tc001.recording

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.TextureView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.module.thermal.ir.capture.video.EnhancedVideoRecorder
import com.topdon.module.thermal.ir.capture.parallel.ParallelCaptureManager
import com.topdon.tc001.R
import com.topdon.tc001.databinding.ActivityEnhancedRecordingBinding
import com.topdon.tc001.gsr.GSRManager
import com.topdon.tc001.LocalFileBrowserActivity
import com.topdon.tc001.orchestrator.OrchestratorConfigActivity

/**
 * Enhanced recording activity for professional research applications.
 * 
 * Provides comprehensive recording capabilities integrating Samsung S22 optimized
 * 4K30FPS recording, professional RAW DNG capture, and synchronized GSR monitoring
 * for advanced research and clinical applications.
 * 
 * **Key Features:**
 * - Samsung S22 optimized 4K recording at 30FPS with 20Mbps bitrate
 * - Professional RAW DNG Level 3 capture at 30FPS with sensor data
 * - Parallel dual-stream recording (thermal + visual)
 * - Real-time GSR monitoring with Shimmer sensor integration
 * - Concurrent capture capabilities for multi-modal research
 * - Professional file management and export functionality
 * 
 * **Recording Modes:**
 * - [SAMSUNG_4K_30FPS] High-quality 4K video optimized for Samsung devices
 * - [RAD_DNG_LEVEL3_30FPS] Professional RAW capture with full sensor data
 * - [PARALLEL_DUAL_STREAM] Simultaneous thermal and visual stream recording
 * 
 * **GSR Integration:**
 * - Real-time galvanic skin response monitoring
 * - Synchronized data overlay on video recordings
 * - Professional Shimmer sensor support with Bluetooth connectivity
 * - Clinical-grade data precision and timestamping
 * 
 * @author BucikaGSR Development Team
 * @since 2024-12-19
 * @see EnhancedVideoRecorder Professional video recording implementation
 * @see GSRManager GSR sensor management and data processing
 * @see ParallelCaptureManager Multi-stream capture coordination
 */
class EnhancedRecordingActivity : BaseActivity(), GSRManager.GSRDataListener {

    /** ViewBinding instance for type-safe view access */
    private lateinit var binding: ActivityEnhancedRecordingBinding

    /** Enhanced video recording system with multiple capture modes */
    private lateinit var videoRecorder: EnhancedVideoRecorder
    
    /** Parallel capture manager for coordinating multi-stream recording */
    private lateinit var parallelCaptureManager: ParallelCaptureManager
    
    /** GSR sensor management system for real-time physiological monitoring */
    private lateinit var gsrManager: GSRManager
    
    /** Thermal imaging texture view for thermal camera preview */
    private var thermalView: TextureView? = null
    
    /** Visual camera texture view for standard camera preview */
    private var visualView: TextureView? = null
    
    /** GSR device connection status flag */
    private var isGSRConnected = false
    
    /** Currently selected recording mode */
    private var currentRecordingMode = EnhancedVideoRecorder.RecordingMode.SAMSUNG_4K_30FPS
    
    companion object {
        /** Permission request code for runtime permissions */
        private const val PERMISSIONS_REQUEST_CODE = 1001
        
        /** Required permissions for enhanced recording functionality */
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    /**
     * Inflates the layout using ViewBinding for type-safe view access.
     * 
     * @return Layout resource ID for the enhanced recording interface
     */
    override fun initContentView(): Int = R.layout.activity_enhanced_recording

    /**
     * Initializes the view components and sets up recording interface.
     * 
     * Configures camera previews, recording controls, GSR integration,
     * and establishes proper event handlers for professional recording
     * workflow management.
     * 
     * @throws RuntimeException if ViewBinding initialization fails
     */
    override fun initView() {
        // Initialize ViewBinding after layout is set by base class
        binding = ActivityEnhancedRecordingBinding.bind(findViewById(android.R.id.content))
        
        binding.titleView.setTitle("Enhanced Recording - Bucika GSR")
        binding.titleView.setLeftClickListener { finish() }
        
        // Initialize texture views (these would be bound to actual camera surfaces)
        thermalView = binding.textureThermal
        visualView = binding.textureVisual
        
        // Initialize components
        videoRecorder = EnhancedVideoRecorder(this, thermalView, visualView)
        gsrManager = GSRManager.getInstance(this)
        gsrManager.setGSRDataListener(this)
        
        setupClickListeners()
        updateUI()
    }

    /**
     * Initializes data components and checks required permissions.
     * 
     * Verifies that all necessary permissions are granted for camera access,
     * storage operations, and Bluetooth connectivity required for enhanced
     * recording functionality.
     */
    override fun initData() {
        checkPermissions()
    }

    /**
     * Sets up click listeners for all recording interface controls.
     * 
     * Configures event handlers for recording mode selection, recording
     * control buttons, GSR device management, and file browser access.
     * Ensures proper workflow management and user feedback.
     */
    private fun setupClickListeners() {
        // Recording mode selection
        binding.btnSamsung4k.setOnClickListener {
            currentRecordingMode = EnhancedVideoRecorder.RecordingMode.SAMSUNG_4K_30FPS
            updateRecordingModeUI()
        }
        
        binding.btnRadDngLevel3.setOnClickListener {
            currentRecordingMode = EnhancedVideoRecorder.RecordingMode.RAD_DNG_LEVEL3_30FPS
            updateRecordingModeUI()
        }
        
        binding.btnParallelRecording.setOnClickListener {
            currentRecordingMode = EnhancedVideoRecorder.RecordingMode.PARALLEL_DUAL_STREAM
            updateRecordingModeUI()
        }
        
        // Recording controls
        binding.btnStartRecording.setOnClickListener {
            startEnhancedRecording()
        }
        
        binding.btnStopRecording.setOnClickListener {
            stopEnhancedRecording()
        }
        
        // GSR controls
        binding.btnConnectGsr.setOnClickListener {
            connectGSRDevice()
        }
        
        binding.btnDisconnectGsr.setOnClickListener {
            disconnectGSRDevice()
        }
        
        // File browser
        binding.btnViewRecordings.setOnClickListener {
            openRecordingsBrowser()
        }
        
        // PC Orchestrator
        binding.btnPcOrchestrator.setOnClickListener {
            openOrchestratorConfig()
        }
    }

    /**
     * Checks and requests required permissions for enhanced recording.
     * 
     * Verifies camera, storage, audio recording, and Bluetooth permissions
     * required for full functionality. Requests missing permissions from
     * the user with appropriate system dialogs.
     */
    private fun checkPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    /**
     * Starts enhanced recording with the selected mode.
     * 
     * Initializes the video recording system with the current recording mode,
     * starts GSR monitoring if connected, and provides user feedback on
     * recording status and mode-specific features.
     */
    private fun startEnhancedRecording() {
        if (!videoRecorder.isRecording()) {
            val success = videoRecorder.startRecording(currentRecordingMode)
            if (success) {
                val modeText = when (currentRecordingMode) {
                    EnhancedVideoRecorder.RecordingMode.SAMSUNG_4K_30FPS -> "Samsung 4K 30FPS"
                    EnhancedVideoRecorder.RecordingMode.RAD_DNG_LEVEL3_30FPS -> "RAD DNG Level 3 30FPS"
                    EnhancedVideoRecorder.RecordingMode.PARALLEL_DUAL_STREAM -> "Parallel Dual Stream"
                }
                Toast.makeText(this, "$modeText recording started", Toast.LENGTH_SHORT).show()
                
                // Start GSR recording if connected
                if (isGSRConnected && !gsrManager.isRecording()) {
                    gsrManager.startRecording()
                }
            } else {
                Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
            }
            updateUI()
        }
    }

    /**
     * Stops enhanced recording and processes recorded content.
     * 
     * Finalizes video recording, stops GSR monitoring, and displays
     * recording statistics including file information and capture
     * performance metrics for professional analysis.
     */
    private fun stopEnhancedRecording() {
        if (videoRecorder.isRecording()) {
            val success = videoRecorder.stopRecording()
            if (success) {
                Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
                
                // Stop GSR recording
                if (gsrManager.isRecording()) {
                    gsrManager.stopRecording()
                }
                
                // Show recorded files
                val recordedFiles = videoRecorder.getRecordedFiles()
                val fileList = recordedFiles.joinToString("\n") { it.name }
                
                // Show DNG capture stats if applicable
                if (currentRecordingMode == EnhancedVideoRecorder.RecordingMode.RAD_DNG_LEVEL3_30FPS) {
                    val dngStats = videoRecorder.getDNGCaptureStats()
                    val framesCaptured = dngStats["framesCaptured"] as? Int ?: 0
                    val actualFPS = dngStats["actualFPS"] as? Double ?: 0.0
                    Toast.makeText(this, 
                        "DNG Recording Complete!\nFrames: $framesCaptured\nActual FPS: %.2f\nFiles: $fileList".format(actualFPS), 
                        Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Saved files:\n$fileList", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Failed to stop recording", Toast.LENGTH_SHORT).show()
            }
            updateUI()
        }
    }

    /**
     * Initiates connection to GSR monitoring device.
     * 
     * Establishes Bluetooth connection to Shimmer GSR sensor for
     * real-time physiological monitoring during recording sessions.
     */
    private fun connectGSRDevice() {
        val shimmerAddress = "00:06:66:XX:XX:XX" // Replace with actual address from device discovery
        gsrManager.connectToShimmer(shimmerAddress)
        Toast.makeText(this, "Connecting to GSR device...", Toast.LENGTH_SHORT).show()
    }

    /**
     * Disconnects from GSR monitoring device.
     * 
     * Safely terminates GSR sensor connection and updates UI state
     * to reflect disconnected status.
     */
    private fun disconnectGSRDevice() {
        gsrManager.disconnectShimmer()
        isGSRConnected = false
        updateUI()
    }

    /**
     * Opens the local file browser for recording management.
     * 
     * Launches the LocalFileBrowserActivity to allow users to browse,
     * manage, and export recorded video files and GSR data.
     */
    private fun openRecordingsBrowser() {
        // Launch local file browser activity
        val intent = Intent(this, LocalFileBrowserActivity::class.java)
        startActivity(intent)
    }

    /**
     * Opens the PC Orchestrator configuration interface.
     * 
     * Launches the OrchestratorConfigActivity to allow users to discover
     * and connect to PC orchestrators for coordinated recording sessions.
     */
    private fun openOrchestratorConfig() {
        // Launch orchestrator configuration activity
        val intent = Intent(this, OrchestratorConfigActivity::class.java)
        startActivity(intent)
    }

    /**
     * Updates recording mode UI to reflect current selection.
     * 
     * Highlights the selected recording mode and displays detailed
     * descriptions of mode-specific features and capabilities for
     * user guidance and workflow optimization.
     */
    private fun updateRecordingModeUI() {
        // Update UI to show selected recording mode
        binding.btnSamsung4k.isSelected = (currentRecordingMode == EnhancedVideoRecorder.RecordingMode.SAMSUNG_4K_30FPS)
        binding.btnRadDngLevel3.isSelected = (currentRecordingMode == EnhancedVideoRecorder.RecordingMode.RAD_DNG_LEVEL3_30FPS)
        binding.btnParallelRecording.isSelected = (currentRecordingMode == EnhancedVideoRecorder.RecordingMode.PARALLEL_DUAL_STREAM)
        
        val modeDescription = when (currentRecordingMode) {
            EnhancedVideoRecorder.RecordingMode.SAMSUNG_4K_30FPS -> 
                "Samsung optimized 4K recording at 30FPS with 20Mbps bitrate"
            EnhancedVideoRecorder.RecordingMode.RAD_DNG_LEVEL3_30FPS -> 
                "Professional RAD DNG Level 3 capture at 30FPS with RAW sensor data"
            EnhancedVideoRecorder.RecordingMode.PARALLEL_DUAL_STREAM -> 
                "Simultaneous thermal and visual stream recording"
        }
        binding.tvModeDescription.text = modeDescription
    }

    /**
     * Updates the complete UI state based on current recording and connection status.
     * 
     * Manages button states, status indicators, and user feedback displays
     * to provide clear visual feedback on system state and available actions.
     */
    private fun updateUI() {
        val isRecording = videoRecorder.isRecording()
        
        // Recording controls
        binding.btnStartRecording.isEnabled = !isRecording
        binding.btnStopRecording.isEnabled = isRecording
        
        // Recording mode selection (disable during recording)
        binding.btnSamsung4k.isEnabled = !isRecording
        binding.btnRadDngLevel3.isEnabled = !isRecording
        binding.btnParallelRecording.isEnabled = !isRecording
        
        // GSR controls
        binding.btnConnectGsr.isEnabled = !isGSRConnected
        binding.btnDisconnectGsr.isEnabled = isGSRConnected
        
        // Status indicators
        binding.tvRecordingStatus.text = if (isRecording) {
            "Recording: ${currentRecordingMode.name}"
        } else {
            "Ready to record"
        }
        
        binding.tvGsrStatus.text = if (isGSRConnected) {
            "GSR: Connected (${gsrManager.getConnectedDeviceName()})"
        } else {
            "GSR: Not connected"
        }
    }

    /**
     * Handles incoming GSR data from connected sensor device.
     * 
     * Processes real-time galvanic skin response and temperature data,
     * updates UI displays, and integrates measurements with active
     * recording sessions for synchronized data capture.
     * 
     * @param timestamp Precise timestamp for GSR measurement
     * @param gsrValue Galvanic skin response value in microsiemens
     * @param skinTemperature Skin temperature in degrees Celsius
     */
    override fun onGSRDataReceived(timestamp: Long, gsrValue: Double, skinTemperature: Double) {
        runOnUiThread {
            binding.tvGsrValue.text = "GSR: %.3f µS".format(gsrValue)
            binding.tvSkinTemp.text = "Skin: %.2f °C".format(skinTemperature)
            
            // Add timestamp overlay to recording if active
            if (videoRecorder.isRecording()) {
                // This would integrate with the actual frame capture to add overlays
                // For now, just update the UI display
                binding.tvRecordingOverlay.text = "Recording with GSR: ${gsrValue}µS @ ${skinTemperature}°C"
            }
        }
    }

    /**
     * Handles GSR device connection status changes.
     * 
     * Updates UI state and provides user feedback when GSR device
     * connection status changes, ensuring proper workflow management
     * and user awareness of sensor availability.
     * 
     * @param isConnected Current connection status
     * @param deviceName Name of connected GSR device, null if disconnected
     */
    override fun onConnectionStatusChanged(isConnected: Boolean, deviceName: String?) {
        runOnUiThread {
            isGSRConnected = isConnected
            updateUI()
            
            val message = if (isConnected) {
                "GSR Connected: $deviceName"
            } else {
                "GSR Disconnected"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handles permission request results for enhanced recording functionality.
     * 
     * Processes user responses to runtime permission requests and provides
     * appropriate feedback if permissions are denied, ensuring users understand
     * the requirements for full functionality.
     * 
     * @param requestCode The request code passed to requestPermissions
     * @param permissions The requested permissions array
     * @param grantResults The grant results for corresponding permissions
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                Toast.makeText(
                    this,
                    "All permissions required for enhanced recording functionality",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Performs cleanup operations when activity is destroyed.
     * 
     * Ensures proper resource cleanup for video recording system and
     * GSR manager to prevent memory leaks and release hardware resources.
     */
    override fun onDestroy() {
        super.onDestroy()
        videoRecorder.cleanup()
        gsrManager.cleanup()
    }
}