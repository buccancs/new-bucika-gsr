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
// import com.topdon.thermal.capture.video.EnhancedVideoRecorder // Wrong import - using local class
// import com.topdon.thermal.capture.parallel.ParallelCaptureManager // Wrong import - not implemented
import com.topdon.tc001.R
import com.topdon.tc001.databinding.ActivityEnhancedRecordingBinding
import com.topdon.tc001.gsr.GSRManager
import com.topdon.tc001.recording.LocalFileBrowserActivity
import com.topdon.tc001.orchestrator.OrchestratorConfigActivity

class EnhancedRecordingActivity : BaseActivity(), GSRManager.GSRDataListener {

    private lateinit var binding: ActivityEnhancedRecordingBinding

    private lateinit var videoRecorder: EnhancedVideoRecorder
    
    // TODO: Implement parallel capture manager for dual thermal/visual recording
    // private lateinit var parallelCaptureManager: ParallelCaptureManager
    
    private lateinit var gsrManager: GSRManager
    
    private var thermalView: TextureView? = null
    
    private var visualView: TextureView? = null
    
    private var isGSRConnected = false
    
    private var currentRecordingMode = EnhancedVideoRecorder.RecordingMode.SAMSUNG_4K_30FPS
    
    companion object {
        
        private const val PERMISSIONS_REQUEST_CODE = 1001
        
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    override fun initContentView(): Int = R.layout.activity_enhanced_recording

    override fun initView() {

        binding = ActivityEnhancedRecordingBinding.bind(findViewById(android.R.id.content))
        
        binding.titleView.setTitleText("Enhanced Recording - Bucika GSR")
        binding.titleView.setLeftClickListener { finish() }
        
        thermalView = binding.textureThermal
        visualView = binding.textureVisual
        
        videoRecorder = EnhancedVideoRecorder(this)
        gsrManager = GSRManager.getInstance(this)
        gsrManager.setGSRDataListener(this)
        
        setupClickListeners()
        updateUI()
    }

    override fun initData() {
        checkPermissions()
    }

    private fun setupClickListeners() {

        binding.btnSamsung4k.setOnClickListener {
            currentRecordingMode = EnhancedVideoRecorder.RecordingMode.SAMSUNG_4K_30FPS
            updateRecordingModeUI()
        }
        
        binding.btnRadDngLevel3.setOnClickListener {
            currentRecordingMode = EnhancedVideoRecorder.RecordingMode.STANDARD_1080P_30FPS
            updateRecordingModeUI()
        }
        
        binding.btnParallelRecording.setOnClickListener {
            currentRecordingMode = EnhancedVideoRecorder.RecordingMode.THERMAL_640x480_30FPS
            updateRecordingModeUI()
        }
        
        binding.btnStartRecording.setOnClickListener {
            startEnhancedRecording()
        }
        
        binding.btnStopRecording.setOnClickListener {
            stopEnhancedRecording()
        }
        
        binding.btnConnectGsr.setOnClickListener {
            connectGSRDevice()
        }
        
        binding.btnDisconnectGsr.setOnClickListener {
            disconnectGSRDevice()
        }
        
        binding.btnViewRecordings.setOnClickListener {
            openRecordingsBrowser()
        }
        
        binding.btnPcOrchestrator.setOnClickListener {
            openOrchestratorConfig()
        }
    }

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

    private fun startEnhancedRecording() {
        if (!videoRecorder.isRecording) {
            val success = videoRecorder.startRecording(currentRecordingMode)
            if (success) {
                val modeText = when (currentRecordingMode) {
                    EnhancedVideoRecorder.RecordingMode.SAMSUNG_4K_30FPS -> "Samsung 4K 30FPS"
                    EnhancedVideoRecorder.RecordingMode.STANDARD_1080P_30FPS -> "Standard 1080P 30FPS"
                    EnhancedVideoRecorder.RecordingMode.HIGH_SPEED_720P_60FPS -> "High Speed 720P 60FPS"
                    EnhancedVideoRecorder.RecordingMode.THERMAL_640x480_30FPS -> "Thermal 640x480 30FPS"
                }
                Toast.makeText(this, "$modeText recording started", Toast.LENGTH_SHORT).show()
                
                if (isGSRConnected && !gsrManager.isRecording()) {
                    gsrManager.startRecording()
                }
            } else {
                Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
            }
            updateUI()
        }
    }

    private fun stopEnhancedRecording() {
        if (videoRecorder.isRecording) {
            val success = videoRecorder.stopRecording()
            if (success) {
                Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
                
                if (gsrManager.isRecording()) {
                    gsrManager.stopRecording()
                }
                
                // File recording management - now properly implemented
                val recordedFiles = videoRecorder.getRecordedFiles()
                val fileList = recordedFiles.joinToString("\n") { it.name }
                
                // Special handling for thermal recording mode
                if (currentRecordingMode == EnhancedVideoRecorder.RecordingMode.THERMAL_640x480_30FPS) {
                    // TODO: Implement thermal capture statistics
                    // val dngStats = videoRecorder.getDNGCaptureStats()
                    // val framesCaptured = dngStats["framesCaptured"] as? Int ?: 0
                    // val actualFPS = dngStats["actualFPS"] as? Double ?: 0.0
                    Toast.makeText(this, "Thermal recording completed", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Recording saved successfully", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Failed to stop recording", Toast.LENGTH_SHORT).show()
            }
            updateUI()
        }
    }

    private fun connectGSRDevice() {
        val shimmerAddress = "00:06:66:XX:XX:XX"
        gsrManager.connectToShimmer(shimmerAddress)
        Toast.makeText(this, "Connecting to GSR device...", Toast.LENGTH_SHORT).show()
    }

    private fun disconnectGSRDevice() {
        gsrManager.disconnectShimmer()
        isGSRConnected = false
        updateUI()
    }

    private fun openRecordingsBrowser() {

        val intent = Intent(this, LocalFileBrowserActivity::class.java)
        startActivity(intent)
    }

    private fun openOrchestratorConfig() {

        val intent = Intent(this, OrchestratorConfigActivity::class.java)
        startActivity(intent)
    }

    private fun updateRecordingModeUI() {

        binding.btnSamsung4k.isSelected = (currentRecordingMode == EnhancedVideoRecorder.RecordingMode.SAMSUNG_4K_30FPS)
        binding.btnRadDngLevel3.isSelected = (currentRecordingMode == EnhancedVideoRecorder.RecordingMode.STANDARD_1080P_30FPS)
        binding.btnParallelRecording.isSelected = (currentRecordingMode == EnhancedVideoRecorder.RecordingMode.THERMAL_640x480_30FPS)
        
        val modeDescription = when (currentRecordingMode) {
            EnhancedVideoRecorder.RecordingMode.SAMSUNG_4K_30FPS -> 
                "Samsung optimized 4K recording at 30FPS with 20Mbps bitrate"
            EnhancedVideoRecorder.RecordingMode.STANDARD_1080P_30FPS -> 
                "Standard 1080p recording at 30FPS for balanced quality and file size"
            EnhancedVideoRecorder.RecordingMode.HIGH_SPEED_720P_60FPS -> 
                "High-speed 720p recording at 60FPS for smooth motion capture"
            EnhancedVideoRecorder.RecordingMode.THERMAL_640x480_30FPS -> 
                "Thermal imaging recording at 640x480 resolution for temperature analysis"
        }
        binding.tvModeDescription.text = modeDescription
    }

    private fun updateUI() {
        val isRecording = videoRecorder.isRecording
        
        binding.btnStartRecording.isEnabled = !isRecording
        binding.btnStopRecording.isEnabled = isRecording
        
        binding.btnSamsung4k.isEnabled = !isRecording
        binding.btnRadDngLevel3.isEnabled = !isRecording
        binding.btnParallelRecording.isEnabled = !isRecording
        
        binding.btnConnectGsr.isEnabled = !isGSRConnected
        binding.btnDisconnectGsr.isEnabled = isGSRConnected
        
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

    override fun onGSRDataReceived(timestamp: Long, gsrValue: Double, skinTemperature: Double) {
        runOnUiThread {
            binding.tvGsrValue.text = "GSR: %.3f µS".format(gsrValue)
            binding.tvSkinTemp.text = "Skin: %.2f °C".format(skinTemperature)
            
            if (videoRecorder.isRecording) {

                binding.tvRecordingOverlay.text = "Recording with GSR: ${gsrValue}µS @ ${skinTemperature}°C"
            }
        }
    }

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

    override fun onDestroy() {
        super.onDestroy()
        videoRecorder.cleanup()
        gsrManager.cleanup()
    }
}
