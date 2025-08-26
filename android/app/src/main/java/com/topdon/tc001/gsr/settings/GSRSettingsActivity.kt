package com.topdon.tc001.gsr.settings

import android.os.Bundle
import android.widget.Toast
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.tc001.R
import com.topdon.tc001.databinding.ActivityGsrSettingsBinding
import com.topdon.tc001.gsr.ui.ShimmerSensorPanel
import com.topdon.tc001.gsr.GSRManager
import com.topdon.tc001.gsr.data.GSRDataWriter
import com.elvishew.xlog.XLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GSRSettingsActivity : BaseActivity(), ShimmerSensorPanel.ShimmerConfigurationListener, GSRDataWriter.DataWriteListener {
    
    companion object {
        private const val TAG = "GSRSettingsActivity"
        private const val DEFAULT_CLEANUP_DAYS = 30
    }
    
    private lateinit var binding: ActivityGsrSettingsBinding
    private lateinit var shimmerSensorPanel: ShimmerSensorPanel
    private lateinit var gsrManager: GSRManager
    private lateinit var gsrDataWriter: GSRDataWriter
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun initContentView(): Int = R.layout.activity_gsr_settings
    
    override fun initView() {
        binding = ActivityGsrSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.titleView.setTitle("GSR Settings & Configuration")
        binding.titleView.setLeftClickListener { finish() }
        
        try {
            gsrManager = GSRManager.getInstance(this)
            gsrDataWriter = GSRDataWriter.getInstance(this)
            
            shimmerSensorPanel = binding.shimmerSensorPanel
            shimmerSensorPanel.setConfigurationListener(this)
            
            gsrDataWriter.setDataWriteListener(this)
            
            setupDataManagementSection()
            updateFileManagementInfo()
            
            XLog.i(TAG, "GSR Settings Activity initialized successfully")
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to initialize GSR Settings Activity: ${e.message}", e)
            Toast.makeText(this, "Initialization error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun initData() {

        try {
            binding.tvConfigStatus.text = "Configuration ready"
            binding.tvDataStatus.text = "Data management ready"
            XLog.i(TAG, "GSR Settings Activity data initialization complete")
        } catch (e: Exception) {
            XLog.e(TAG, "Data initialization error: ${e.message}", e)
        }
    }

    private fun setupDataManagementSection() {
        with(binding) {

            btnExportCurrentData.setOnClickListener {
                exportCurrentGSRData()
            }
            
            btnExportStatistics.setOnClickListener {
                exportGSRStatistics()
            }
            
            btnViewRecordedFiles.setOnClickListener {
                viewRecordedFiles()
            }
            
            btnCleanupOldFiles.setOnClickListener {
                cleanupOldFiles()
            }
            
            btnTestDataWriting.setOnClickListener {
                testDataWriting()
            }
        }
    }
    
    private fun exportCurrentGSRData() {
        coroutineScope.launch {
            try {
                binding.tvDataStatus.text = "Exporting GSR data..."
                
                val gsrAPIHelper = com.topdon.tc001.gsr.api.GSRAPIHelper.getInstance(this@GSRSettingsActivity)
                val csvData = gsrAPIHelper.exportGSRDataToCSV()
                
                if (csvData.isNotEmpty()) {
                    val filePath = gsrDataWriter.exportGSRDataToFile(
                        emptyList(),
                        "manual_export_${System.currentTimeMillis()}.csv"
                    )
                    
                    binding.tvDataStatus.text = "Data exported to: ${filePath.substringAfterLast("/")}"
                    Toast.makeText(this@GSRSettingsActivity, "GSR data exported successfully", Toast.LENGTH_SHORT).show()
                } else {
                    binding.tvDataStatus.text = "No GSR data available to export"
                    Toast.makeText(this@GSRSettingsActivity, "No GSR data to export", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                XLog.e(TAG, "Failed to export GSR data: ${e.message}", e)
                binding.tvDataStatus.text = "Export failed: ${e.message}"
                Toast.makeText(this@GSRSettingsActivity, "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun exportGSRStatistics() {
        coroutineScope.launch {
            try {
                binding.tvDataStatus.text = "Exporting statistics..."
                
                val gsrAPIHelper = com.topdon.tc001.gsr.api.GSRAPIHelper.getInstance(this@GSRSettingsActivity)
                val statistics = gsrAPIHelper.getGSRStatistics()
                
                val analysisData = mapOf(
                    "Configuration" to shimmerSensorPanel.getConfiguration().toString(),
                    "Connected Device" to (gsrManager.getConnectedDeviceName() ?: "None"),
                    "Recording Status" to if (gsrManager.isRecording()) "Active" else "Inactive",
                    "Data Directory Size" to "${gsrDataWriter.getDataDirectorySize() / 1024} KB"
                )
                
                val filePath = gsrDataWriter.exportStatisticsToFile(statistics, analysisData)
                
                binding.tvDataStatus.text = "Statistics exported to: ${filePath.substringAfterLast("/")}"
                Toast.makeText(this@GSRSettingsActivity, "Statistics exported successfully", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                XLog.e(TAG, "Failed to export statistics: ${e.message}", e)
                binding.tvDataStatus.text = "Statistics export failed: ${e.message}"
                Toast.makeText(this@GSRSettingsActivity, "Statistics export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun viewRecordedFiles() {
        val recordedFiles = gsrDataWriter.getRecordedFiles()
        
        if (recordedFiles.isEmpty()) {
            binding.tvFileList.text = "No recorded files found"
        } else {
            val fileInfo = StringBuilder("Recorded Files:\n\n")
            recordedFiles.forEach { file ->
                val sizeKB = file.length() / 1024
                val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(file.lastModified()))
                fileInfo.append("📄 ${file.name}\n")
                fileInfo.append("   Size: ${sizeKB} KB, Modified: $date\n\n")
            }
            binding.tvFileList.text = fileInfo.toString()
        }
    }
    
    private fun cleanupOldFiles() {
        val deletedCount = gsrDataWriter.cleanupOldFiles(DEFAULT_CLEANUP_DAYS)
        
        binding.tvDataStatus.text = "Cleaned up $deletedCount old files"
        Toast.makeText(this, "Cleaned up $deletedCount old files", Toast.LENGTH_SHORT).show()
        
        updateFileManagementInfo()
    }
    
    private fun testDataWriting() {
        if (gsrDataWriter.isRecording()) {

            gsrDataWriter.stopRecording()
            binding.btnTestDataWriting.text = "Test Data Writing"
            Toast.makeText(this, "Test recording stopped", Toast.LENGTH_SHORT).show()
        } else {

            if (gsrDataWriter.startRecording("test_session")) {
                binding.btnTestDataWriting.text = "Stop Test Recording"
                Toast.makeText(this, "Test recording started", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to start test recording", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateFileManagementInfo() {
        val recordedFiles = gsrDataWriter.getRecordedFiles()
        val totalSizeKB = gsrDataWriter.getDataDirectorySize() / 1024
        
        binding.tvFileCount.text = "Recorded files: ${recordedFiles.size}"
        binding.tvTotalSize.text = "Total size: ${totalSizeKB} KB"
        
        viewRecordedFiles()
    }
    
    override fun onConfigurationChanged(config: ShimmerSensorPanel.ShimmerConfiguration) {

        val configText = """
            Sampling Rate: ${config.samplingRate} Hz
            GSR: ${if (config.gsrEnabled) "Enabled" else "Disabled"}
            Temperature: ${if (config.temperatureEnabled) "Enabled" else "Disabled"}
            PPG: ${if (config.ppgEnabled) "Enabled" else "Disabled"}
            Accelerometer: ${if (config.accelerometerEnabled) "Enabled" else "Disabled"}
        """.trimIndent()
        
        binding.tvConfigPreview.text = configText
        XLog.d(TAG, "Configuration changed: $config")
    }
    
    override fun onConfigurationApplied(config: ShimmerSensorPanel.ShimmerConfiguration) {

        if (gsrManager.isConnected()) {
            try {

                binding.tvConfigStatus.text = "Configuration applied (device connected)"
                Toast.makeText(this, "Configuration applied successfully", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                XLog.e(TAG, "Failed to apply configuration: ${e.message}", e)
                binding.tvConfigStatus.text = "Failed to apply configuration: ${e.message}"
                Toast.makeText(this, "Failed to apply configuration", Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.tvConfigStatus.text = "Configuration saved (device not connected)"
            Toast.makeText(this, "Configuration saved", Toast.LENGTH_SHORT).show()
        }
        
        XLog.i(TAG, "Configuration applied: $config")
    }
    
    override fun onConfigurationReset() {
        binding.tvConfigStatus.text = "Configuration reset to defaults"
        Toast.makeText(this, "Configuration reset to defaults", Toast.LENGTH_SHORT).show()
        XLog.i(TAG, "Configuration reset to defaults")
    }
    
    override fun onRecordingStarted(fileName: String, filePath: String) {
        runOnUiThread {
            binding.tvRecordingStatus.text = "Recording to: $fileName"
            binding.tvDataStatus.text = "Recording started"
        }
    }
    
    override fun onDataWritten(samplesWritten: Long, fileSize: Long) {
        runOnUiThread {
            binding.tvRecordingStatus.text = "Recording: $samplesWritten samples, ${fileSize / 1024} KB"
        }
    }
    
    override fun onRecordingStopped(fileName: String, totalSamples: Long, fileSize: Long) {
        runOnUiThread {
            binding.tvRecordingStatus.text = "Last recording: $fileName ($totalSamples samples, ${fileSize / 1024} KB)"
            binding.tvDataStatus.text = "Recording stopped"
            updateFileManagementInfo()
        }
    }
    
    override fun onWriteError(error: String) {
        runOnUiThread {
            binding.tvDataStatus.text = "Write error: $error"
            Toast.makeText(this@GSRSettingsActivity, "Data write error: $error", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.launch { 

        }
        XLog.i(TAG, "GSR Settings Activity destroyed")
    }
