package com.multisensor.recording.controllers

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.multisensor.recording.managers.PermissionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionController @Inject constructor(
    private val permissionManager: PermissionManager
) {

    interface PermissionCallback {
        fun onAllPermissionsGranted()
        fun onPermissionsTemporarilyDenied(deniedPermissions: List<String>, grantedCount: Int, totalCount: Int)
        fun onPermissionsPermanentlyDenied(deniedPermissions: List<String>)
        fun onPermissionCheckStarted()
        fun onPermissionRequestCompleted()
        fun updateStatusText(text: String)
        fun showPermissionButton(show: Boolean)
    }

    private var callback: PermissionCallback? = null
    private var hasCheckedPermissionsOnStartup = false
    private var permissionRetryCount = 0

    private var sharedPreferences: SharedPreferences? = null

    companion object {
        private const val PREFS_NAME = "permission_controller_prefs"
        private const val KEY_HAS_CHECKED_PERMISSIONS = "has_checked_permissions_on_startup"
        private const val KEY_PERMISSION_RETRY_COUNT = "permission_retry_count"
        private const val KEY_LAST_PERMISSION_REQUEST_TIME = "last_permission_request_time"
        private const val KEY_PERMANENTLY_DENIED_PERMISSIONS = "permanently_denied_permissions"
    }

    fun setCallback(callback: PermissionCallback) {
        this.callback = callback

        if (callback is Context) {
            initializeStateStorage(callback as Context)
        }
    }

    private fun initializeStateStorage(context: Context) {
        try {
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadPersistedState()
            android.util.Log.d("PermissionController", "[DEBUG_LOG] State persistence initialized")
        } catch (e: Exception) {
            android.util.Log.e(
                "PermissionController",
                "[DEBUG_LOG] Failed to initialize state persistence: ${e.message}"
            )
        }
    }

    private fun loadPersistedState() {
        sharedPreferences?.let { prefs ->
            hasCheckedPermissionsOnStartup = prefs.getBoolean(KEY_HAS_CHECKED_PERMISSIONS, false)
            permissionRetryCount = prefs.getInt(KEY_PERMISSION_RETRY_COUNT, 0)

            val lastRequestTime = prefs.getLong(KEY_LAST_PERMISSION_REQUEST_TIME, 0)
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastRequestTime > 24 * 60 * 60 * 1000) {
                android.util.Log.d("PermissionController", "[DEBUG_LOG] Resetting permission state after 24 hours")
                hasCheckedPermissionsOnStartup = false
                permissionRetryCount = 0
                persistState()
            }

            android.util.Log.d(
                "PermissionController",
                "[DEBUG_LOG] Loaded persisted state: checked=$hasCheckedPermissionsOnStartup, retries=$permissionRetryCount"
            )
        }
    }

    private fun persistState() {
        sharedPreferences?.edit()?.apply {
            putBoolean(KEY_HAS_CHECKED_PERMISSIONS, hasCheckedPermissionsOnStartup)
            putInt(KEY_PERMISSION_RETRY_COUNT, permissionRetryCount)
            putLong(KEY_LAST_PERMISSION_REQUEST_TIME, System.currentTimeMillis())
            apply()
        }
    }

    fun areAllPermissionsGranted(context: Context): Boolean {
        return permissionManager.areAllPermissionsGranted(context)
    }

    fun checkPermissions(context: Context) {
        android.util.Log.d("PermissionController", "[DEBUG_LOG] Starting permission check via PermissionManager...")

        callback?.onPermissionCheckStarted()

        if (permissionManager.areAllPermissionsGranted(context)) {
            android.util.Log.d("PermissionController", "[DEBUG_LOG] All permissions already granted")
            hasCheckedPermissionsOnStartup = true
            persistState()
            callback?.onAllPermissionsGranted()
        } else {
            android.util.Log.d("PermissionController", "[DEBUG_LOG] Requesting permissions via PermissionManager...")
            callback?.updateStatusText("Requesting permissions...")

            if (context is Activity) {
                permissionManager.requestPermissions(context, createPermissionManagerCallback())
            }
        }

        updatePermissionButtonVisibility(context)
    }

    fun requestPermissionsManually(context: Context) {
        android.util.Log.d("PermissionController", "[DEBUG_LOG] Manual permission request initiated by user")

        hasCheckedPermissionsOnStartup = false

        permissionRetryCount = 0
        persistState()
        android.util.Log.d("PermissionController", "[DEBUG_LOG] Reset permission retry counter to 0 for manual request")

        callback?.showPermissionButton(false)
        callback?.updateStatusText("Requesting permissions...")

        checkPermissions(context)
    }

    fun updatePermissionButtonVisibility(context: Context) {
        val allPermissionsGranted = permissionManager.areAllPermissionsGranted(context)

        if (!allPermissionsGranted) {
            android.util.Log.d(
                "PermissionController",
                "[DEBUG_LOG] Showing permission request button - permissions missing"
            )
            callback?.showPermissionButton(true)
        } else {
            android.util.Log.d(
                "PermissionController",
                "[DEBUG_LOG] Hiding permission request button - all permissions granted"
            )
            callback?.showPermissionButton(false)
        }
    }

    private fun showTemporaryDenialMessage(
        context: Context,
        temporarilyDenied: List<String>,
        grantedCount: Int,
        totalCount: Int
    ) {
        android.util.Log.d(
            "PermissionController",
            "[DEBUG_LOG] Showing temporary denial message for ${temporarilyDenied.size} permissions"
        )

        val message = buildString {
            append("Some permissions were denied but can be requested again.\n\n")
            append("Denied permissions:\n")
            append(temporarilyDenied.joinToString("\n") { "• ${getPermissionDisplayName(it)}" })
            append("\n\nYou can grant these permissions using the 'Request Permissions' button.")
        }

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()

        callback?.updateStatusText("Permissions: $grantedCount/$totalCount granted - Some permissions denied")

        android.util.Log.i(
            "PermissionController",
            "Temporary permission denial: ${temporarilyDenied.joinToString(", ")}"
        )
    }

    private fun showPermanentlyDeniedMessage(context: Context, permanentlyDenied: List<String>) {
        android.util.Log.d("PermissionController", "[DEBUG_LOG] Showing permanently denied permissions message")

        val message = buildString {
            append("Some permissions have been permanently denied. ")
            append("Please enable them manually in Settings > Apps > Multi-Sensor Recording > Permissions.\n\n")
            append("Permanently denied permissions:\n")
            append(permanentlyDenied.joinToString("\n") { "• ${getPermissionDisplayName(it)}" })
        }

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()

        callback?.updateStatusText("Permissions required - Please enable in Settings")

        android.util.Log.w(
            "PermissionController",
            "Permanently denied permissions: ${permanentlyDenied.joinToString(", ")}"
        )
    }

    fun getPermissionDisplayName(permission: String): String {
        return permissionManager.getPermissionDisplayName(permission)
    }

    private fun createPermissionManagerCallback(): PermissionManager.PermissionCallback {
        return object : PermissionManager.PermissionCallback {
            override fun onAllPermissionsGranted() {
                android.util.Log.d("PermissionController", "[DEBUG_LOG] All permissions granted callback received")
                hasCheckedPermissionsOnStartup = true
                persistState()
                callback?.onAllPermissionsGranted()
                callback?.onPermissionRequestCompleted()
            }

            override fun onPermissionsTemporarilyDenied(
                deniedPermissions: List<String>,
                grantedCount: Int,
                totalCount: Int
            ) {
                android.util.Log.d("PermissionController", "[DEBUG_LOG] Temporary denial callback received")
                persistState()
                callback?.let { cb ->
                    if (cb is Context) {
                        showTemporaryDenialMessage(cb as Context, deniedPermissions, grantedCount, totalCount)
                    }
                }
                callback?.onPermissionsTemporarilyDenied(deniedPermissions, grantedCount, totalCount)
                callback?.onPermissionRequestCompleted()
            }

            override fun onPermissionsPermanentlyDenied(deniedPermissions: List<String>) {
                android.util.Log.d("PermissionController", "[DEBUG_LOG] Permanent denial callback received")

                storePermanentlyDeniedPermissions(deniedPermissions)
                persistState()

                callback?.let { cb ->
                    if (cb is Context) {
                        showPermanentlyDeniedMessage(cb as Context, deniedPermissions)
                    }
                }
                callback?.onPermissionsPermanentlyDenied(deniedPermissions)
                callback?.onPermissionRequestCompleted()
            }
        }
    }

    private fun storePermanentlyDeniedPermissions(deniedPermissions: List<String>) {
        sharedPreferences?.edit()?.apply {
            putStringSet(KEY_PERMANENTLY_DENIED_PERMISSIONS, deniedPermissions.toSet())
            apply()
        }
        android.util.Log.d(
            "PermissionController",
            "[DEBUG_LOG] Stored permanently denied permissions: ${deniedPermissions.joinToString(", ")}"
        )
    }

    fun getPermanentlyDeniedPermissions(): Set<String> {
        return sharedPreferences?.getStringSet(KEY_PERMANENTLY_DENIED_PERMISSIONS, emptySet()) ?: emptySet()
    }

    fun resetState() {
        hasCheckedPermissionsOnStartup = false
        permissionRetryCount = 0
        persistState()
        android.util.Log.d("PermissionController", "[DEBUG_LOG] Permission controller state reset and persisted")
    }

    fun getPermissionRetryCount(): Int = permissionRetryCount

    fun hasCheckedPermissionsOnStartup(): Boolean = hasCheckedPermissionsOnStartup

    fun getPermissionStatus(): String {
        return buildString {
            append("Permission Controller Status:\n")
            append("- Has checked permissions on startup: $hasCheckedPermissionsOnStartup\n")
            append("- Permission retry count: $permissionRetryCount\n")
            append("- State persistence: ${if (sharedPreferences != null) "Enabled" else "Disabled"}\n")
            val permanentlyDenied = getPermanentlyDeniedPermissions()
            append(
                "- Permanently denied permissions: ${
                    if (permanentlyDenied.isEmpty()) "None" else permanentlyDenied.joinToString(
                        ", "
                    )
                }\n"
            )
            append("- Last request time: ${sharedPreferences?.getLong(KEY_LAST_PERMISSION_REQUEST_TIME, 0) ?: 0}")
        }
    }

    fun clearPersistedState() {
        sharedPreferences?.edit()?.clear()?.apply()
        hasCheckedPermissionsOnStartup = false
        permissionRetryCount = 0
        android.util.Log.d("PermissionController", "[DEBUG_LOG] All persisted permission state cleared")
    }

    fun initializePermissionsOnStartup(context: Context) {
        if (!hasCheckedPermissionsOnStartup) {
            android.util.Log.d("PermissionController", "[DEBUG_LOG] First startup - checking permissions")
            hasCheckedPermissionsOnStartup = true
            checkPermissions(context)
        } else {
            android.util.Log.d("PermissionController", "[DEBUG_LOG] Subsequent startup - skipping permission check")
            updatePermissionButtonVisibility(context)
        }
    }

    fun validateInternalState(): ValidationResult {
        val violations = mutableListOf<String>()

        if (permissionRetryCount < 0) {
            violations.add("Retry count cannot be negative: $permissionRetryCount")
        }

        sharedPreferences?.let { prefs ->
            val lastRequestTime = prefs.getLong(KEY_LAST_PERMISSION_REQUEST_TIME, 0)
            val currentTime = System.currentTimeMillis()
            if (lastRequestTime > currentTime) {
                violations.add("Last request time cannot be in the future: $lastRequestTime > $currentTime")
            }
        }

        sharedPreferences?.let { prefs ->
            val persistedRetryCount = prefs.getInt(KEY_PERMISSION_RETRY_COUNT, 0)
            if (persistedRetryCount != permissionRetryCount) {
                violations.add("In-memory retry count ($permissionRetryCount) differs from persisted ($persistedRetryCount)")
            }
        }

        return ValidationResult(
            isValid = violations.isEmpty(),
            violations = violations,
            validationTimestamp = System.currentTimeMillis()
        )
    }

    fun analyzeComplexity(context: Context): ComplexityAnalysis {
        val totalPermissions = permissionManager.getAllRequiredPermissions().size
        val stateSpaceSize = Math.pow(4.0, totalPermissions.toDouble()).toInt()
        val currentGrantedCount = permissionManager.getGrantedPermissions(context).size
        val transitionComplexity = calculateTransitionComplexity(totalPermissions, currentGrantedCount)

        return ComplexityAnalysis(
            totalPermissions = totalPermissions,
            stateSpaceSize = stateSpaceSize,
            currentGrantedCount = currentGrantedCount,
            transitionComplexity = transitionComplexity,
            retryCount = permissionRetryCount,
            analysisTimestamp = System.currentTimeMillis()
        )
    }

    private fun calculateTransitionComplexity(totalPermissions: Int, grantedCount: Int): Int {
        val remainingPermissions = totalPermissions - grantedCount
        return when {
            remainingPermissions == 0 -> 1
            remainingPermissions == totalPermissions -> 3 * totalPermissions
            else -> 2 * remainingPermissions + 1
        }
    }

    data class ValidationResult(
        val isValid: Boolean,
        val violations: List<String>,
        val validationTimestamp: Long
    ) {
        override fun toString(): String = buildString {
            append("ValidationResult(valid=$isValid, timestamp=$validationTimestamp")
            if (violations.isNotEmpty()) {
                append(", violations=[${violations.joinToString("; ")}]")
            }
            append(")")
        }
    }

    data class ComplexityAnalysis(
        val totalPermissions: Int,
        val stateSpaceSize: Int,
        val currentGrantedCount: Int,
        val transitionComplexity: Int,
        val retryCount: Int,
        val analysisTimestamp: Long
    ) {
        val completionRatio: Double = currentGrantedCount.toDouble() / totalPermissions
        val stateComplexityClass: String = when {
            stateSpaceSize <= 16 -> "Simple"
            stateSpaceSize <= 256 -> "Moderate"
            stateSpaceSize <= 4096 -> "Complex"
            else -> "Highly Complex"
        }

        override fun toString(): String = buildString {
            append("ComplexityAnalysis(")
            append("permissions=$totalPermissions, ")
            append("stateSpace=$stateSpaceSize, ")
            append("granted=$currentGrantedCount, ")
            append("completion=${String.format("%.2f", completionRatio * 100)}%, ")
            append("complexity=$stateComplexityClass, ")
            append("transitions=$transitionComplexity")
            append(")")
        }
    }

    fun logCurrentPermissionStates(context: Context) {
        permissionManager.logCurrentPermissionStates(context)
    }
}
