package com.topdon.tc001.gsr

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.tc001.R
import com.topdon.tc001.gsr.data.GSRDataWriter
import com.topdon.tc001.gsr.settings.GSRSettingsActivity
import com.shimmerresearch.driver.ProcessedGSRData
import kotlinx.android.synthetic.main.activity_gsr.*

/**
 * Enhanced GSR Activity for bucika_gsr version
 * Provides comprehensive UI for GSR sensor management and data visualization
 * Integrates with ShimmerAndroidAPI for professional-grade GSR data collection
 * Includes data writing and sensor configuration capabilities
 */
class GSRActivity : BaseActivity(), GSRManager.GSRDataListener, GSRDataWriter.DataWriteListener {
    
    private lateinit var gsrManager: GSRManager
    private lateinit var gsrDataWriter: GSRDataWriter
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val BLUETOOTH_PERMISSION_REQUEST = 1001
    
    override fun initContentView(): Int = R.layout.activity_gsr
    
    override fun initView() {
        title_view.setTitle("GSR Monitoring - Bucika")
        title_view.setLeftClickListener { finish() }
        
        // Initialize GSR manager and data writer
        gsrManager = GSRManager.getInstance(this)
        gsrManager.setGSRDataListener(this)
        gsrManager.initializeShimmer()
        
        gsrDataWriter = GSRDataWriter.getInstance(this)
        gsrDataWriter.setDataWriteListener(this)
        
        // Initialize Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        
        setupClickListeners()
        updateUI()
    }
    
    override fun initData() {
        // Check permissions
        checkBluetoothPermissions()
    }
    
    private fun setupClickListeners() {
        btn_connect_shimmer.setOnClickListener {
            if (checkBluetoothPermissions()) {
                connectToShimmer()
            }
        }
        
        btn_disconnect_shimmer.setOnClickListener {
            gsrManager.disconnectShimmer()
        }
        
        btn_start_recording.setOnClickListener {
            startGSRRecording()
        }
        
        btn_stop_recording.setOnClickListener {
            stopGSRRecording()
        }
        
        // Settings button
        btn_gsr_settings.setOnClickListener {
            val intent = Intent(this, GSRSettingsActivity::class.java)
            startActivity(intent)
        }
        
        // Enhanced Recording button
        btn_enhanced_recording.setOnClickListener {
            val intent = Intent(this, com.topdon.tc001.recording.EnhancedRecordingActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun checkBluetoothPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, BLUETOOTH_PERMISSION_REQUEST)
                return false
            }
        }
        return true
    }
    
    private fun connectToShimmer() {
        if (bluetoothAdapter?.isEnabled != true) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1000)
            return
        }
        
        // For now, we'll use a demo Shimmer address - in practice, this would come from device discovery
        val shimmerAddress = "00:06:66:XX:XX:XX" // Replace with actual Shimmer device address
        gsrManager.connectToShimmer(shimmerAddress)
        
        Toast.makeText(this, "Attempting to connect to Shimmer device...", Toast.LENGTH_SHORT).show()
    }
    
    private fun startGSRRecording() {
        if (gsrManager.isConnected()) {
            if (gsrManager.startRecording()) {
                // Also start file recording
                if (gsrDataWriter.startRecording("gsr_session_${System.currentTimeMillis()}")) {
                    Toast.makeText(this, "GSR recording and file writing started", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "GSR recording started (file writing failed)", Toast.LENGTH_SHORT).show()
                }
                updateUI()
            } else {
                Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please connect to Shimmer device first", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopGSRRecording() {
        if (gsrManager.stopRecording()) {
            // Also stop file recording
            gsrDataWriter.stopRecording()
            Toast.makeText(this, "GSR recording and file writing stopped", Toast.LENGTH_SHORT).show()
            updateUI()
        }
    }
    
    private fun updateUI() {
        val isConnected = gsrManager.isConnected()
        val isRecording = gsrManager.isRecording()
        
        btn_connect_shimmer.isEnabled = !isConnected
        btn_disconnect_shimmer.isEnabled = isConnected
        btn_start_recording.isEnabled = isConnected && !isRecording
        btn_stop_recording.isEnabled = isRecording
        
        tv_connection_status.text = if (isConnected) {
            "Connected to: ${gsrManager.getConnectedDeviceName()}"
        } else {
            "Not connected"
        }
        
        tv_recording_status.text = if (isRecording) {
            "Recording GSR data..."
        } else {
            "Ready to record"
        }
    }
    
    // GSRManager.GSRDataListener implementation
    override fun onGSRDataReceived(timestamp: Long, gsrValue: Double, skinTemperature: Double) {
        runOnUiThread {
            tv_gsr_value.text = "GSR: %.3f µS".format(gsrValue)
            tv_skin_temp.text = "Skin Temp: %.2f °C".format(skinTemperature)
            tv_last_update.text = "Last update: ${java.text.SimpleDateFormat("HH:mm:ss.SSS").format(timestamp)}"
            
            // Add data to file writer if recording
            if (gsrDataWriter.isRecording()) {
                // Create a basic ProcessedGSRData object for file writing
                val processedData = ProcessedGSRData(
                    timestamp = timestamp,
                    rawGSR = gsrValue,
                    filteredGSR = gsrValue,
                    rawTemperature = skinTemperature,
                    filteredTemperature = skinTemperature,
                    gsrDerivative = 0.0, // Simplified for basic recording
                    gsrVariability = 0.0,
                    temperatureVariability = 0.0,
                    signalQuality = 95.0, // Default quality
                    hasArtifact = false,
                    isValid = true,
                    sampleIndex = timestamp / 8 // Approximate sample index
                )
                gsrDataWriter.addGSRData(processedData)
            }
        }
    }
    
    override fun onConnectionStatusChanged(isConnected: Boolean, deviceName: String?) {
        runOnUiThread {
            updateUI()
            val message = if (isConnected) {
                "Connected to $deviceName"
            } else {
                "Disconnected from Shimmer device"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    // GSRDataWriter.DataWriteListener implementation
    override fun onRecordingStarted(fileName: String, filePath: String) {
        runOnUiThread {
            tv_recording_status.text = "Recording to file: $fileName"
        }
    }
    
    override fun onDataWritten(samplesWritten: Long, fileSize: Long) {
        // Update can be too frequent, so we'll just update the recording status periodically
        if (samplesWritten % 128 == 0L) { // Every second at 128 Hz
            runOnUiThread {
                tv_recording_status.text = "Recording: $samplesWritten samples (${fileSize / 1024} KB)"
            }
        }
    }
    
    override fun onRecordingStopped(fileName: String, totalSamples: Long, fileSize: Long) {
        runOnUiThread {
            tv_recording_status.text = "Last recording: $totalSamples samples (${fileSize / 1024} KB)"
            Toast.makeText(this@GSRActivity, "Recording saved: $fileName", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onWriteError(error: String) {
        runOnUiThread {
            Toast.makeText(this@GSRActivity, "File write error: $error", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissions granted
                Toast.makeText(this, "Bluetooth permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bluetooth permissions required for GSR functionality", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        gsrManager.cleanup()
    }
}