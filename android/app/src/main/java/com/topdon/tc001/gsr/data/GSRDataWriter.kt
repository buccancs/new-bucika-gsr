package com.topdon.tc001.gsr.data

import android.content.Context
import android.os.Environment
import com.elvishew.xlog.XLog
import com.shimmerresearch.driver.ProcessedGSRData
import com.topdon.tc001.gsr.api.GSRAPIHelper
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Comprehensive GSR Data Writing Service
 * Handles file system integration for GSR data export and storage
 */
class GSRDataWriter private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "GSRDataWriter"
        private const val GSR_DATA_FOLDER = "GSRData"
        private const val TEMP_FOLDER = "temp"
        private const val BUFFER_SIZE = 1000
        
        @Volatile
        private var INSTANCE: GSRDataWriter? = null
        
        fun getInstance(context: Context): GSRDataWriter {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GSRDataWriter(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dataQueue = ConcurrentLinkedQueue<ProcessedGSRData>()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    private val timestampFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    // Recording state
    private var isRecordingData = false
    private var currentRecordingFile: File? = null
    private var currentRecordingWriter: FileWriter? = null
    private var samplesWritten = 0L
    private var recordingStartTime = 0L
    
    // Listeners
    private var dataWriteListener: DataWriteListener? = null
    
    interface DataWriteListener {
        fun onRecordingStarted(fileName: String, filePath: String)
        fun onDataWritten(samplesWritten: Long, fileSize: Long)
        fun onRecordingStopped(fileName: String, totalSamples: Long, fileSize: Long)
        fun onWriteError(error: String)
    }
    
    /**
     * Set data write listener
     */
    fun setDataWriteListener(listener: DataWriteListener) {
        this.dataWriteListener = listener
    }
    
    /**
     * Start recording GSR data to file
     */
    fun startRecording(sessionName: String? = null): Boolean {
        if (isRecordingData) {
            XLog.w(TAG, "Recording already in progress")
            return false
        }
        
        try {
            val fileName = generateFileName(sessionName)
            val gsrDataDir = getGSRDataDirectory()
            currentRecordingFile = File(gsrDataDir, fileName)
            
            currentRecordingWriter = FileWriter(currentRecordingFile!!)
            writeCSVHeader()
            
            isRecordingData = true
            samplesWritten = 0L
            recordingStartTime = System.currentTimeMillis()
            
            // Start background writing coroutine
            startDataWritingCoroutine()
            
            dataWriteListener?.onRecordingStarted(fileName, currentRecordingFile!!.absolutePath)
            XLog.i(TAG, "Started GSR data recording: $fileName")
            
            return true
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to start recording: ${e.message}", e)
            dataWriteListener?.onWriteError("Failed to start recording: ${e.message}")
            cleanupRecording()
            return false
        }
    }
    
    /**
     * Stop recording GSR data
     */
    fun stopRecording(): Boolean {
        if (!isRecordingData) {
            XLog.w(TAG, "No recording in progress")
            return false
        }
        
        try {
            isRecordingData = false
            
            // Process any remaining data in queue
            runBlocking {
                processDataQueue()
            }
            
            val fileName = currentRecordingFile?.name ?: "Unknown"
            val fileSize = currentRecordingFile?.length() ?: 0L
            
            cleanupRecording()
            
            dataWriteListener?.onRecordingStopped(fileName, samplesWritten, fileSize)
            XLog.i(TAG, "Stopped GSR data recording: $fileName, $samplesWritten samples, ${fileSize / 1024} KB")
            
            return true
            
        } catch (e: Exception) {
            XLog.e(TAG, "Error stopping recording: ${e.message}", e)
            dataWriteListener?.onWriteError("Error stopping recording: ${e.message}")
            cleanupRecording()
            return false
        }
    }
    
    /**
     * Add GSR data to writing queue
     */
    fun addGSRData(data: ProcessedGSRData) {
        if (!isRecordingData) return
        
        dataQueue.offer(data)
        
        // Process queue if it's getting full
        if (dataQueue.size >= BUFFER_SIZE) {
            coroutineScope.launch {
                processDataQueue()
            }
        }
    }
    
    /**
     * Export existing GSR data to CSV file
     */
    suspend fun exportGSRDataToFile(
        gsrData: List<ProcessedGSRData>,
        fileName: String? = null,
        includeAnalysis: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        
        val finalFileName = fileName ?: "gsr_export_${dateFormatter.format(Date())}.csv"
        val gsrDataDir = getGSRDataDirectory()
        val exportFile = File(gsrDataDir, finalFileName)
        
        try {
            FileWriter(exportFile).use { writer ->
                // Write header
                if (includeAnalysis) {
                    writer.write("Timestamp,DateTime,Raw_GSR_µS,Filtered_GSR_µS,Raw_Temp_°C,Filtered_Temp_°C,GSR_Derivative,GSR_Variability,Temp_Variability,Signal_Quality,Has_Artifact,Is_Valid,Sample_Index\n")
                } else {
                    writer.write("Timestamp,DateTime,GSR_µS,Temperature_°C\n")
                }
                
                // Write data
                gsrData.forEach { data ->
                    val dateTime = timestampFormatter.format(Date(data.timestamp))
                    
                    if (includeAnalysis) {
                        writer.write("${data.timestamp},$dateTime,${data.rawGSR},${data.filteredGSR},${data.rawTemperature},${data.filteredTemperature},${data.gsrDerivative},${data.gsrVariability},${data.temperatureVariability},${data.signalQuality},${data.hasArtifact},${data.isValid},${data.sampleIndex}\n")
                    } else {
                        writer.write("${data.timestamp},$dateTime,${data.filteredGSR},${data.filteredTemperature}\n")
                    }
                }
            }
            
            XLog.i(TAG, "Exported ${gsrData.size} GSR samples to: $finalFileName")
            exportFile.absolutePath
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to export GSR data: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Export GSR statistics to file
     */
    suspend fun exportStatisticsToFile(
        statistics: GSRAPIHelper.GSRStatistics,
        analysisData: Map<String, Any> = emptyMap(),
        fileName: String? = null
    ): String = withContext(Dispatchers.IO) {
        
        val finalFileName = fileName ?: "gsr_statistics_${dateFormatter.format(Date())}.txt"
        val gsrDataDir = getGSRDataDirectory()
        val statsFile = File(gsrDataDir, finalFileName)
        
        try {
            FileWriter(statsFile).use { writer ->
                writer.write("GSR Data Analysis Report\n")
                writer.write("Generated: ${timestampFormatter.format(Date())}\n")
                writer.write("${"=".repeat(50)}\n\n")
                
                writer.write("Basic Statistics:\n")
                writer.write("- Sample Count: ${statistics.sampleCount}\n")
                writer.write("- Average GSR: ${String.format("%.3f", statistics.avgGSR)} µS\n")
                writer.write("- Min GSR: ${String.format("%.3f", statistics.minGSR)} µS\n")
                writer.write("- Max GSR: ${String.format("%.3f", statistics.maxGSR)} µS\n")
                writer.write("- Average Temperature: ${String.format("%.2f", statistics.avgTemperature)} °C\n")
                writer.write("- Min Temperature: ${String.format("%.2f", statistics.minTemperature)} °C\n")
                writer.write("- Max Temperature: ${String.format("%.2f", statistics.maxTemperature)} °C\n")
                writer.write("- Overall Signal Quality: ${String.format("%.1f", statistics.overallQuality)}%\n\n")
                
                if (analysisData.isNotEmpty()) {
                    writer.write("Advanced Analysis:\n")
                    analysisData.forEach { (key, value) ->
                        writer.write("- $key: $value\n")
                    }
                }
            }
            
            XLog.i(TAG, "Exported GSR statistics to: $finalFileName")
            statsFile.absolutePath
            
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to export statistics: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Get list of recorded GSR files
     */
    fun getRecordedFiles(): List<File> {
        val gsrDataDir = getGSRDataDirectory()
        return gsrDataDir.listFiles { file ->
            file.isFile && file.name.endsWith(".csv")
        }?.toList() ?: emptyList()
    }
    
    /**
     * Delete recorded file
     */
    fun deleteRecordedFile(fileName: String): Boolean {
        val gsrDataDir = getGSRDataDirectory()
        val file = File(gsrDataDir, fileName)
        
        return if (file.exists()) {
            val deleted = file.delete()
            if (deleted) {
                XLog.i(TAG, "Deleted GSR file: $fileName")
            } else {
                XLog.w(TAG, "Failed to delete GSR file: $fileName")
            }
            deleted
        } else {
            XLog.w(TAG, "File not found: $fileName")
            false
        }
    }
    
    /**
     * Get GSR data directory size
     */
    fun getDataDirectorySize(): Long {
        val gsrDataDir = getGSRDataDirectory()
        return gsrDataDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }
    
    /**
     * Clean up old files (older than specified days)
     */
    fun cleanupOldFiles(olderThanDays: Int = 30): Int {
        val gsrDataDir = getGSRDataDirectory()
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        
        val oldFiles = gsrDataDir.listFiles { file ->
            file.isFile && file.lastModified() < cutoffTime
        }
        
        var deletedCount = 0
        oldFiles?.forEach { file ->
            if (file.delete()) {
                deletedCount++
                XLog.d(TAG, "Deleted old file: ${file.name}")
            }
        }
        
        XLog.i(TAG, "Cleaned up $deletedCount old GSR files")
        return deletedCount
    }
    
    private fun generateFileName(sessionName: String?): String {
        val timestamp = dateFormatter.format(Date())
        val baseName = sessionName ?: "gsr_recording"
        return "${baseName}_${timestamp}.csv"
    }
    
    private fun getGSRDataDirectory(): File {
        val externalDir = context.getExternalFilesDir(null)
            ?: context.filesDir
        
        val gsrDataDir = File(externalDir, GSR_DATA_FOLDER)
        if (!gsrDataDir.exists()) {
            gsrDataDir.mkdirs()
        }
        
        return gsrDataDir
    }
    
    private fun writeCSVHeader() {
        currentRecordingWriter?.write(
            "Timestamp,DateTime,Raw_GSR_µS,Filtered_GSR_µS,Raw_Temp_°C,Filtered_Temp_°C,GSR_Derivative,GSR_Variability,Temp_Variability,Signal_Quality,Has_Artifact,Is_Valid,Sample_Index\n"
        )
        currentRecordingWriter?.flush()
    }
    
    private fun startDataWritingCoroutine() {
        coroutineScope.launch {
            while (isRecordingData) {
                processDataQueue()
                delay(100) // Check queue every 100ms
            }
        }
    }
    
    private suspend fun processDataQueue() = withContext(Dispatchers.IO) {
        val writer = currentRecordingWriter ?: return@withContext
        
        var processedCount = 0
        while (dataQueue.isNotEmpty() && processedCount < BUFFER_SIZE) {
            val data = dataQueue.poll() ?: break
            
            try {
                val dateTime = timestampFormatter.format(Date(data.timestamp))
                writer.write("${data.timestamp},$dateTime,${data.rawGSR},${data.filteredGSR},${data.rawTemperature},${data.filteredTemperature},${data.gsrDerivative},${data.gsrVariability},${data.temperatureVariability},${data.signalQuality},${data.hasArtifact},${data.isValid},${data.sampleIndex}\n")
                
                samplesWritten++
                processedCount++
                
                // Periodic flush and size update
                if (samplesWritten % 100 == 0L) {
                    writer.flush()
                    val fileSize = currentRecordingFile?.length() ?: 0L
                    dataWriteListener?.onDataWritten(samplesWritten, fileSize)
                }
                
            } catch (e: IOException) {
                XLog.e(TAG, "Error writing GSR data: ${e.message}", e)
                dataWriteListener?.onWriteError("Error writing data: ${e.message}")
                break
            }
        }
        
        // Final flush
        try {
            writer.flush()
        } catch (e: IOException) {
            XLog.e(TAG, "Error flushing writer: ${e.message}", e)
        }
    }
    
    private fun cleanupRecording() {
        try {
            currentRecordingWriter?.close()
            currentRecordingWriter = null
            currentRecordingFile = null
            samplesWritten = 0L
            recordingStartTime = 0L
            dataQueue.clear()
        } catch (e: Exception) {
            XLog.e(TAG, "Error during cleanup: ${e.message}", e)
        }
    }
    
    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = isRecordingData
    
    /**
     * Get current recording file info
     */
    fun getCurrentRecordingInfo(): RecordingInfo? {
        return if (isRecordingData && currentRecordingFile != null) {
            RecordingInfo(
                fileName = currentRecordingFile!!.name,
                filePath = currentRecordingFile!!.absolutePath,
                startTime = recordingStartTime,
                samplesWritten = samplesWritten,
                fileSize = currentRecordingFile!!.length()
            )
        } else null
    }
    
    data class RecordingInfo(
        val fileName: String,
        val filePath: String,
        val startTime: Long,
        val samplesWritten: Long,
        val fileSize: Long
    )
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        if (isRecordingData) {
            stopRecording()
        }
        coroutineScope.cancel()
        dataWriteListener = null
    }
