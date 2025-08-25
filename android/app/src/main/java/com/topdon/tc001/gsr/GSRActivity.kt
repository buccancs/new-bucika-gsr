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
import com.topdon.tc001.databinding.ActivityGsrBinding
import com.topdon.tc001.gsr.data.GSRDataWriter
import com.topdon.tc001.gsr.settings.GSRSettingsActivity
import com.shimmerresearch.driver.ProcessedGSRData

class GSRActivity : BaseActivity(), GSRManager.GSRDataListener, GSRDataWriter.DataWriteListener {
    
    private lateinit var gsrManager: GSRManager
    private lateinit var gsrDataWriter: GSRDataWriter
    private lateinit var binding: ActivityGsrBinding
    private var bluetoothAdapter: BluetoothAdapter? = null
    
    companion object {
        private const val BLUETOOTH_PERMISSION_REQUEST = 1001
        private const val BLUETOOTH_ENABLE_REQUEST = 1000
        private const val SHIMMER_SAMPLE_RATE = 128
        private const val DEFAULT_SHIMMER_ADDRESS = "00:06:66:XX:XX:XX"
    }
    
    override fun initContentView(): Int = R.layout.activity_gsr
    
    override fun initView() {
        binding = ActivityGsrBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.titleView.setTitle("GSR Monitoring - Bucika")
        binding.titleView.setLeftClickListener { finish() }
        
        gsrManager = GSRManager.getInstance(this)
        gsrManager.setGSRDataListener(this)
        gsrManager.initializeShimmer()
        
        gsrDataWriter = GSRDataWriter.getInstance(this)
        gsrDataWriter.setDataWriteListener(this)
        
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        
        setupClickListeners()
        updateUI()
    }
    
    override fun initData() {
        checkBluetoothPermissions()
    }
    
    private fun setupClickListeners() {
        binding.btnConnectShimmer.setOnClickListener {
            if (checkBluetoothPermissions()) {
                connectToShimmer()
            }
        }
        
        binding.btnDisconnectShimmer.setOnClickListener {
            gsrManager.disconnectShimmer()
        }
        
        binding.btnStartRecording.setOnClickListener {
            startGSRRecording()
        }
        
        binding.btnStopRecording.setOnClickListener {
            stopGSRRecording()
        }
        
        binding.btnGsrSettings.setOnClickListener {
            val intent = Intent(this, GSRSettingsActivity::class.java)
            startActivity(intent)
        }
        
        binding.btnEnhancedRecording.setOnClickListener {
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
            startActivityForResult(enableBtIntent, BLUETOOTH_ENABLE_REQUEST)
            return
        }
        
        val shimmerAddress = DEFAULT_SHIMMER_ADDRESS
        gsrManager.connectToShimmer(shimmerAddress)
        
        Toast.makeText(this, "Attempting to connect to Shimmer device...", Toast.LENGTH_SHORT).show()
    }
    
    private fun startGSRRecording() {
        if (gsrManager.isConnected()) {
            if (gsrManager.startRecording()) {

                val sessionName = "gsr_session_${System.currentTimeMillis()}"
                if (gsrDataWriter.startRecording(sessionName)) {
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
            gsrDataWriter.stopRecording()
            Toast.makeText(this, "GSR recording and file writing stopped", Toast.LENGTH_SHORT).show()
            updateUI()
        }
    }
    
    private fun updateUI() {
        val isConnected = gsrManager.isConnected()
        val isRecording = gsrManager.isRecording()
        
        binding.btnConnectShimmer.isEnabled = !isConnected
        binding.btnDisconnectShimmer.isEnabled = isConnected
        binding.btnStartRecording.isEnabled = isConnected && !isRecording
        binding.btnStopRecording.isEnabled = isRecording
        
        binding.tvConnectionStatus.text = if (isConnected) {
            "Connected to: ${gsrManager.getConnectedDeviceName()}"
        } else {
            "Not connected"
        }
        
        binding.tvRecordingStatus.text = if (isRecording) {
            "Recording GSR data..."
        } else {
            "Ready to record"
        }
    }
    
    override fun onGSRDataReceived(timestamp: Long, gsrValue: Double, skinTemperature: Double) {
        runOnUiThread {
            binding.tvGsrValue.text = "GSR: %.3f µS".format(gsrValue)
            binding.tvSkinTemp.text = "Skin Temp: %.2f °C".format(skinTemperature)
            binding.tvLastUpdate.text = "Last update: ${java.text.SimpleDateFormat("HH:mm:ss.SSS").format(timestamp)}"
            
            if (gsrDataWriter.isRecording()) {
                val processedData = ProcessedGSRData(
                    timestamp = timestamp,
                    rawGSR = gsrValue,
                    filteredGSR = gsrValue,
                    rawTemperature = skinTemperature,
                    filteredTemperature = skinTemperature,
                    gsrDerivative = 0.0,
                    gsrVariability = 0.0,
                    temperatureVariability = 0.0,
                    signalQuality = 95.0,
                    hasArtifact = false,
                    isValid = true,
                    sampleIndex = timestamp / (1000 / SHIMMER_SAMPLE_RATE)
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
    
    override fun onRecordingStarted(fileName: String, filePath: String) {
        runOnUiThread {
            binding.tvRecordingStatus.text = "Recording to file: $fileName"
        }
    }
    
    override fun onDataWritten(samplesWritten: Long, fileSize: Long) {

        if (samplesWritten % SHIMMER_SAMPLE_RATE == 0L) {
            runOnUiThread {
                binding.tvRecordingStatus.text = "Recording: $samplesWritten samples (${fileSize / 1024} KB)"
            }
        }
    }
    
    override fun onRecordingStopped(fileName: String, totalSamples: Long, fileSize: Long) {
        runOnUiThread {
            binding.tvRecordingStatus.text = "Last recording: $totalSamples samples (${fileSize / 1024} KB)"
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
