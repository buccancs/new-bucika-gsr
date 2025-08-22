package com.multisensor.recording.ui.utils
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.multisensor.recording.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class ShimmerDataExporter @Inject constructor(
    private val logger: Logger
) {
    data class SensorDataPoint(
        val timestamp: Long,
        val sensorType: String,
        val values: FloatArray,
        val units: String
    )
    data class ExportSession(
        val sessionId: String,
        val startTime: Long,
        val endTime: Long,
        val deviceInfo: Map<String, String>,
        val dataPoints: List<SensorDataPoint>
    )
    suspend fun exportToCsv(
        context: Context,
        session: ExportSession,
        includeHeaders: Boolean = true
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val filename = generateFilename(session.sessionId, "csv")
            val file = File(context.getExternalFilesDir("exports"), filename)
            file.parentFile?.mkdirs()
            file.bufferedWriter().use { writer ->
                if (includeHeaders) {
                    writer.write("Timestamp,Sensor,Channel,Value,Unit\n")
                }
                session.dataPoints.forEach { dataPoint ->
                    dataPoint.values.forEachIndexed { index, value ->
                        writer.write(
                            "${dataPoint.timestamp}," +
                            "${dataPoint.sensorType}," +
                            "${index}," +
                            "${value}," +
                            "${dataPoint.units}\n"
                        )
                    }
                }
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            logger.info("Successfully exported CSV data to: ${file.absolutePath}")
            Result.success(uri)
        } catch (e: Exception) {
            logger.error("Failed to export CSV data: ${e.message}")
            Result.failure(e)
        }
    }
    suspend fun exportToJson(
        context: Context,
        session: ExportSession
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val filename = generateFilename(session.sessionId, "json")
            val file = File(context.getExternalFilesDir("exports"), filename)
            file.parentFile?.mkdirs()
            val jsonData = buildString {
                append("{\n")
                append("  \"sessionInfo\": {\n")
                append("    \"sessionId\": \"${session.sessionId}\",\n")
                append("    \"startTime\": ${session.startTime},\n")
                append("    \"endTime\": ${session.endTime},\n")
                append("    \"duration\": ${session.endTime - session.startTime},\n")
                append("    \"deviceInfo\": {\n")
                session.deviceInfo.entries.forEachIndexed { index, (key, value) ->
                    append("      \"$key\": \"$value\"")
                    if (index < session.deviceInfo.size - 1) append(",")
                    append("\n")
                }
                append("    }\n")
                append("  },\n")
                append("  \"data\": [\n")
                session.dataPoints.forEachIndexed { pointIndex, dataPoint ->
                    append("    {\n")
                    append("      \"timestamp\": ${dataPoint.timestamp},\n")
                    append("      \"sensor\": \"${dataPoint.sensorType}\",\n")
                    append("      \"values\": [${dataPoint.values.joinToString(", ")}],\n")
                    append("      \"units\": \"${dataPoint.units}\"\n")
                    append("    }")
                    if (pointIndex < session.dataPoints.size - 1) append(",")
                    append("\n")
                }
                append("  ]\n")
                append("}")
            }
            file.writeText(jsonData)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            logger.info("Successfully exported JSON data to: ${file.absolutePath}")
            Result.success(uri)
        } catch (e: Exception) {
            logger.error("Failed to export JSON data: ${e.message}")
            Result.failure(e)
        }
    }
    fun shareExportedData(context: Context, uri: Uri, mimeType: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Shimmer Sensor Data Export")
            putExtra(Intent.EXTRA_TEXT, "Exported sensor data from Shimmer device.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share sensor data")
        context.startActivity(chooser)
    }
    private fun generateFilename(sessionId: String, extension: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "shimmer_data_${sessionId}_${timestamp}.${extension}"
    }
    fun getExportDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir("exports") ?: context.filesDir, "shimmer_exports")
        dir.mkdirs()
        return dir
    }
    fun cleanupOldExports(context: Context, maxAgeHours: Int = 24) {
        try {
            val exportDir = getExportDirectory(context)
            val cutoffTime = System.currentTimeMillis() - (maxAgeHours * 60 * 60 * 1000)
            exportDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        logger.info("Cleaned up old export file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to cleanup old exports: ${e.message}")
        }
    }
}