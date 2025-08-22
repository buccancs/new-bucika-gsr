package com.multisensor.recording.ui.util

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.multisensor.recording.R

object UIUtils {

    fun updateConnectionIndicator(context: Context, indicator: View, isConnected: Boolean) {
        val colorRes = if (isConnected) {
            R.color.statusIndicatorConnected
        } else {
            R.color.statusIndicatorDisconnected
        }
        indicator.setBackgroundColor(ContextCompat.getColor(context, colorRes))
    }

    fun updateRecordingIndicator(context: Context, indicator: View, isRecording: Boolean) {
        val colorRes = if (isRecording) {
            R.color.recordingActive
        } else {
            R.color.recordingInactive
        }
        indicator.setBackgroundColor(ContextCompat.getColor(context, colorRes))
    }

    fun showStatusMessage(context: Context, message: String, isLongDuration: Boolean = false) {
        val duration = if (isLongDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        Toast.makeText(context, message, duration).show()
    }

    fun getConnectionStatusText(deviceName: String, isConnected: Boolean): String {
        return "$deviceName: ${if (isConnected) "Connected" else getDisconnectedText(deviceName)}"
    }

    private fun getDisconnectedText(deviceName: String): String {
        return when (deviceName.lowercase()) {
            "pc" -> "Waiting..."
            "shimmer" -> "Off"
            "thermal" -> "Off"
            else -> "Disconnected"
        }
    }

    fun formatBatteryText(batteryLevel: Int): String {
        return if (batteryLevel >= 0) {
            "Battery: $batteryLevel%"
        } else {
            "Battery: ---%"
        }
    }

    fun formatStreamingText(isStreaming: Boolean, frameRate: Int, dataSize: String): String {
        return if (isStreaming && frameRate > 0) {
            "Streaming: ${frameRate}fps ($dataSize)"
        } else {
            "Ready to stream"
        }
    }

    fun getRecordingStatusText(isRecording: Boolean): String {
        return if (isRecording) "Recording in progress..." else "Ready to record"
    }

    fun styleButton(context: Context, button: View, buttonType: ButtonType, isEnabled: Boolean = true) {
        val colorRes = when (buttonType) {
            ButtonType.PRIMARY -> R.color.colorSecondary
            ButtonType.SUCCESS -> R.color.colorPrimary
            ButtonType.DANGER -> R.color.recordingActive
            ButtonType.SECONDARY -> R.color.textColorSecondary
        }

        button.setBackgroundColor(ContextCompat.getColor(context, colorRes))
        button.isEnabled = isEnabled
        button.alpha = if (isEnabled) 1.0f else 0.6f
    }

    enum class ButtonType {
        PRIMARY,
        SUCCESS,
        DANGER,
        SECONDARY
    }

    fun setViewVisibilityWithAnimation(view: View, isVisible: Boolean, duration: Long = 300) {
        if (isVisible) {
            view.visibility = View.VISIBLE
            view.animate()
                .alpha(1.0f)
                .setDuration(duration)
                .start()
        } else {
            view.animate()
                .alpha(0.0f)
                .setDuration(duration)
                .withEndAction {
                    view.visibility = View.GONE
                }
                .start()
        }
    }

    fun getOperationTimeout(operationType: OperationType): Long {
        return when (operationType) {
            OperationType.CONNECTION -> 10000L
            OperationType.RECORDING_START -> 5000L
            OperationType.RECORDING_STOP -> 3000L
            OperationType.CALIBRATION -> 30000L
            OperationType.FILE_OPERATION -> 15000L
        }
    }

    enum class OperationType {
        CONNECTION,
        RECORDING_START,
        RECORDING_STOP,
        CALIBRATION,
        FILE_OPERATION
    }
}
