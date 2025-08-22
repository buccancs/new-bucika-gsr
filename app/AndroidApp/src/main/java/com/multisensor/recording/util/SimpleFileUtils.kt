package com.multisensor.recording.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object SimpleFileUtils {
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun createVideoFile(context: Context, sessionId: String): File {
        val timestamp = dateFormat.format(Date())
        val fileName = "video_${sessionId}_${timestamp}.mp4"
        return File(context.getExternalFilesDir(null), fileName)
    }

    fun createThermalFile(context: Context, sessionId: String): File {
        val timestamp = dateFormat.format(Date())
        val fileName = "thermal_${sessionId}_${timestamp}.dat"
        return File(context.getExternalFilesDir(null), fileName)
    }

    fun createRawFile(context: Context, sessionId: String, count: Int): File {
        val fileName = "raw_${sessionId}_${String.format("%04d", count)}.dng"
        return File(context.getExternalFilesDir(null), fileName)
    }

    fun saveTextFile(context: Context, fileName: String, content: String): Boolean {
        return try {
            val file = File(context.getExternalFilesDir(null), fileName)
            FileOutputStream(file).use { it.write(content.toByteArray()) }
            true
        } catch (e: Exception) {
            false
        }
    }
}