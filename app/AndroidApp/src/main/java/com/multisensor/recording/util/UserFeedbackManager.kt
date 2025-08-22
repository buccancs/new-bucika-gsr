package com.multisensor.recording.util

import android.content.Context
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.multisensor.recording.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserFeedbackManager @Inject constructor(
    private val context: Context
) {

    private val _feedbackEvents = MutableSharedFlow<FeedbackEvent>()
    val feedbackEvents: SharedFlow<FeedbackEvent> = _feedbackEvents.asSharedFlow()

    fun showError(
        view: View,
        message: String,
        actionText: String? = null,
        action: (() -> Unit)? = null,
        duration: Int = Snackbar.LENGTH_LONG
    ) {
        val snackbar = Snackbar.make(view, message, duration)
            .setBackgroundTint(context.getColor(R.color.buttonDanger))
            .setTextColor(context.getColor(R.color.colorOnPrimary))
            .setActionTextColor(context.getColor(R.color.colorOnPrimary))

        if (actionText != null && action != null) {
            snackbar.setAction(actionText) { action() }
        }

        snackbar.show()
    }

    fun showSuccess(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT
    ) {
        Snackbar.make(view, message, duration)
            .setBackgroundTint(context.getColor(R.color.statusIndicatorConnected))
            .setTextColor(context.getColor(R.color.colorOnPrimary))
            .show()
    }

    fun showWarning(
        view: View,
        message: String,
        actionText: String? = null,
        action: (() -> Unit)? = null,
        duration: Int = Snackbar.LENGTH_LONG
    ) {
        val snackbar = Snackbar.make(view, message, duration)
            .setBackgroundTint(context.getColor(R.color.statusIndicatorWarning))
            .setTextColor(context.getColor(R.color.colorOnPrimary))
            .setActionTextColor(context.getColor(R.color.colorOnPrimary))

        if (actionText != null && action != null) {
            snackbar.setAction(actionText) { action() }
        }

        snackbar.show()
    }

    fun showInfo(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT
    ) {
        Snackbar.make(view, message, duration)
            .setBackgroundTint(context.getColor(R.color.statusIndicatorInfo))
            .setTextColor(context.getColor(R.color.colorOnPrimary))
            .show()
    }

    fun showErrorDialog(
        context: Context,
        title: String,
        message: String,
        positiveText: String = "OK",
        positiveAction: (() -> Unit)? = null,
        negativeText: String? = null,
        negativeAction: (() -> Unit)? = null
    ) {
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { _, _ -> positiveAction?.invoke() }

        if (negativeText != null) {
            builder.setNegativeButton(negativeText) { _, _ -> negativeAction?.invoke() }
        }

        builder.show()
    }

    fun showConfirmationDialog(
        context: Context,
        title: String,
        message: String,
        confirmText: String = "Confirm",
        cancelText: String = "Cancel",
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null,
        isDestructive: Boolean = false
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(confirmText) { _, _ -> onConfirm() }
            .setNegativeButton(cancelText) { _, _ -> onCancel?.invoke() }
            .show()
    }

    fun getUserFriendlyErrorMessage(throwable: Throwable): String {
        return when {
            throwable.message?.contains("Permission") == true ->
                "Permission required. Please grant the necessary permissions to continue."

            throwable.message?.contains("Camera") == true ->
                "Camera is not available. Please check if another app is using the camera."

            throwable.message?.contains("Network") == true || throwable.message?.contains("Connection") == true ->
                "Network connection issue. Please check your connection and try again."

            throwable.message?.contains("Storage") == true || throwable.message?.contains("Space") == true ->
                "Insufficient storage space. Please free up space and try again."

            throwable.message?.contains("Battery") == true ->
                "Low battery detected. Connect charger for optimal performance."

            throwable.message?.contains("Bluetooth") == true ->
                "Bluetooth connection issue. Please check Bluetooth settings and device pairing."

            throwable.message?.contains("USB") == true ->
                "USB device connection issue. Please check USB cable and device connection."

            throwable is SecurityException ->
                "Security permission required. Please grant the necessary permissions."

            throwable is IllegalStateException ->
                "Application is in an invalid state. Please restart the app."

            throwable is OutOfMemoryError ->
                "Memory is running low. Please close other apps and try again."

            else -> throwable.localizedMessage ?: "An unexpected error occurred. Please try again."
        }
    }

    suspend fun emitFeedbackEvent(event: FeedbackEvent) {
        _feedbackEvents.emit(event)
    }

    fun getSystemStatusMessage(
        isCameraOk: Boolean,
        isThermalOk: Boolean,
        isShimmerOk: Boolean,
        isPcConnected: Boolean
    ): String {
        val issues = mutableListOf<String>()

        if (!isCameraOk) issues.add("camera")
        if (!isThermalOk) issues.add("thermal sensor")
        if (!isShimmerOk) issues.add("Shimmer sensor")
        if (!isPcConnected) issues.add("PC connection")

        return when {
            issues.isEmpty() -> "All systems ready for recording"
            issues.size == 1 -> "Issue with ${issues[0]}. Some features may be limited."
            issues.size == 2 -> "Issues with ${issues[0]} and ${issues[1]}. Recording capabilities limited."
            else -> "Multiple system issues detected. Please check connections and permissions."
        }
    }

    fun getBatteryStatusMessage(level: Int, isCharging: Boolean): String {
        return when {
            isCharging -> "Battery charging ($level%)"
            level >= 80 -> "Battery excellent ($level%)"
            level >= 50 -> "Battery good ($level%)"
            level >= 30 -> "Battery moderate ($level%)"
            level >= 15 -> "Battery low ($level%) - Consider charging for long recordings"
            else -> "Battery critical ($level%) - Charging recommended"
        }
    }

    fun getRecordingStatusMessage(
        isRecording: Boolean,
        duration: Long,
        deviceCount: Int
    ): String {
        return if (isRecording) {
            val minutes = duration / 60000
            val seconds = (duration % 60000) / 1000
            "Recording active - ${String.format("%02d:%02d", minutes, seconds)} ($deviceCount devices)"
        } else {
            "Ready to record with $deviceCount devices"
        }
    }
}

sealed class FeedbackEvent {
    data class Error(val message: String, val throwable: Throwable? = null) : FeedbackEvent()
    data class Success(val message: String) : FeedbackEvent()
    data class Warning(val message: String) : FeedbackEvent()
    data class Info(val message: String) : FeedbackEvent()
    data class Progress(val message: String, val isVisible: Boolean) : FeedbackEvent()
}

fun UserFeedbackManager.showRecordingStarted(view: View, sessionId: String) {
    showSuccess(view, "Recording started - Session: ${sessionId.take(8)}")
}

fun UserFeedbackManager.showRecordingStopped(view: View, duration: Long) {
    val minutes = duration / 60000
    val seconds = (duration % 60000) / 1000
    showSuccess(view, "Recording stopped - Duration: ${String.format("%02d:%02d", minutes, seconds)}")
}

fun UserFeedbackManager.showDeviceConnected(view: View, deviceName: String) {
    showSuccess(view, "$deviceName connected")
}

fun UserFeedbackManager.showDeviceDisconnected(view: View, deviceName: String) {
    showWarning(view, "$deviceName disconnected")
}

fun UserFeedbackManager.showLowBatteryWarning(view: View, level: Int) {
    showWarning(
        view,
        "Low battery ($level%). Connect charger for extended recording.",
        "Settings",
        {
        }
    )
}

fun UserFeedbackManager.showPermissionRequired(view: View, permission: String) {
    showError(
        view,
        "Permission required: $permission",
        "Grant",
        {
        }
    )
}
