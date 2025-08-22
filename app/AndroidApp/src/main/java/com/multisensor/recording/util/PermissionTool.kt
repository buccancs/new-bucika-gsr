package com.multisensor.recording.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions

object PermissionTool {
    fun requestAllDangerousPermissions(
        context: Context,
        callback: PermissionCallback,
    ) {
        val nonLocationPermissions = getNonLocationDangerousPermissions()
        if (nonLocationPermissions.isNotEmpty()) {
            requestPermissions(
                context,
                nonLocationPermissions,
                "App Permissions",
                ThreePhasePermissionCallback(context, callback),
            )
        } else {
            val foregroundLocationPermissions = getForegroundLocationPermissions()
            if (foregroundLocationPermissions.isNotEmpty()) {
                requestPermissions(
                    context,
                    foregroundLocationPermissions,
                    "Foreground Location Permissions",
                    ThreePhasePermissionCallback(context, callback, startFromPhase2 = true),
                )
            } else {
                val backgroundLocationPermissions = getBackgroundLocationPermissions()
                if (backgroundLocationPermissions.isNotEmpty()) {
                    requestPermissions(
                        context,
                        backgroundLocationPermissions,
                        "Background Location Permissions",
                        callback
                    )
                } else {
                    callback.onAllGranted()
                }
            }
        }
    }

    fun requestCamera(
        context: Context,
        callback: PermissionCallback,
    ) {
        requestPermissions(context, listOf(Permission.CAMERA), "Camera", callback)
    }

    fun requestMicrophone(
        context: Context,
        callback: PermissionCallback,
    ) {
        requestPermissions(context, listOf(Permission.RECORD_AUDIO), "Microphone", callback)
    }

    fun requestLocation(
        context: Context,
        callback: PermissionCallback,
    ) {
        val permissions = listOf(Permission.ACCESS_FINE_LOCATION, Permission.ACCESS_COARSE_LOCATION)
        requestPermissions(context, permissions, "Location", callback)
    }

    fun requestStorage(
        context: Context,
        callback: PermissionCallback,
    ) {
        val permissions = getStoragePermissions()
        requestPermissions(context, permissions, "Storage", callback)
    }

    fun areAllDangerousPermissionsGranted(context: Context): Boolean =
        XXPermissions.isGranted(context, getAllDangerousPermissions())

    fun getMissingDangerousPermissions(context: Context): List<String> {
        val allPermissions = getAllDangerousPermissions()
        return allPermissions.filter { !XXPermissions.isGranted(context, it) }
    }

    private fun requestPermissions(
        context: Context,
        permissions: List<String>,
        permissionType: String,
        callback: PermissionCallback,
    ) {
        val isBackgroundLocationRequest = permissions.size == 1 &&
                permissions.first() == Permission.ACCESS_BACKGROUND_LOCATION

        if (isBackgroundLocationRequest) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                requestBackgroundLocationPermission(context, callback)
            }, 100)
        } else {
            XXPermissions
                .with(context)
                .permission(permissions)
                .request(
                    object : OnPermissionCallback {
                        override fun onGranted(
                            permissions: MutableList<String>,
                            allGranted: Boolean,
                        ) {
                            if (allGranted) {
                                callback.onAllGranted()
                            } else {
                                callback.onPartiallyGranted(permissions.toList())
                            }
                        }

                        override fun onDenied(
                            permissions: MutableList<String>,
                            never: Boolean,
                        ) {
                            if (never) {
                                showPermanentDenialDialog(context, permissions.toList(), permissionType, callback)
                            } else {
                                callback.onTemporarilyDenied(permissions.toList())
                            }
                        }
                    },
                )
        }
    }

    private fun requestBackgroundLocationPermission(
        context: Context,
        callback: PermissionCallback,
    ) {
        try {
            XXPermissions
                .with(context)
                .permission(Permission.ACCESS_BACKGROUND_LOCATION)
                .request(
                    object : OnPermissionCallback {
                        override fun onGranted(
                            permissions: MutableList<String>,
                            allGranted: Boolean,
                        ) {
                            if (allGranted) {
                                callback.onAllGranted()
                            } else {
                                callback.onPartiallyGranted(permissions.toList())
                            }
                        }

                        override fun onDenied(
                            permissions: MutableList<String>,
                            never: Boolean,
                        ) {
                            if (never) {
                                showPermanentDenialDialog(
                                    context,
                                    permissions.toList(),
                                    "Background Location Permissions",
                                    callback
                                )
                            } else {
                                callback.onTemporarilyDenied(permissions.toList())
                            }
                        }
                    },
                )
        } catch (e: Exception) {
            android.util.Log.e("PermissionTool", "Error requesting background location permission", e)
            callback.onTemporarilyDenied(listOf(Permission.ACCESS_BACKGROUND_LOCATION))
        }
    }

    private fun showPermanentDenialDialog(
        context: Context,
        deniedPermissions: List<String>,
        permissionType: String,
        callback: PermissionCallback,
    ) {
        val permissionNames = deniedPermissions.map { getPermissionDisplayName(it) }
        val message =
            buildString {
                append("$permissionType permissions have been permanently denied.\n\n")
                append("To use this app properly, please enable the following permissions in Settings:\n\n")
                permissionNames.forEach { name ->
                    append("• $name\n")
                }
                append("\nWould you like to open Settings now?")
            }

        AlertDialog
            .Builder(context)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings(context)
                callback.onPermanentlyDeniedWithSettingsOpened(deniedPermissions)
            }.setNegativeButton("Cancel") { _, _ ->
                callback.onPermanentlyDeniedWithoutSettings(deniedPermissions)
            }.setCancelable(false)
            .show()
    }

    private fun openAppSettings(context: Context) {
        try {
            XXPermissions.startPermissionActivity(context, emptyList())
        } catch (e: Exception) {
            try {
                val intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Toast
                    .makeText(
                        context,
                        "Please go to Settings > Apps > ${
                            context.applicationInfo.loadLabel(
                                context.packageManager,
                            )
                        } > Permissions to enable required permissions",
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }
    }

    private fun getAllDangerousPermissions(): List<String> {
        val permissions = mutableListOf<String>()
        permissions.addAll(getNonLocationDangerousPermissions())
        permissions.addAll(getForegroundLocationPermissions())
        return permissions
    }

    private fun getNonLocationDangerousPermissions(): List<String> {
        val permissions = mutableListOf<String>()

        permissions.addAll(getStoragePermissions())

        permissions.addAll(
            listOf(
                Permission.CAMERA,
                Permission.RECORD_AUDIO,
            ),
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Permission.POST_NOTIFICATIONS)
        }

        return permissions
    }

    private fun getForegroundLocationPermissions(): List<String> =
        listOf(
            Permission.ACCESS_FINE_LOCATION,
            Permission.ACCESS_COARSE_LOCATION,
        )

    private fun getBackgroundLocationPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(Permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            emptyList()
        }

    private fun getLocationPermissions(): List<String> {
        val permissions = mutableListOf<String>()
        permissions.addAll(getForegroundLocationPermissions())
        permissions.addAll(getBackgroundLocationPermissions())
        return permissions
    }

    private fun getStoragePermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Permission.READ_MEDIA_IMAGES,
                Permission.READ_MEDIA_VIDEO,
                Permission.READ_MEDIA_AUDIO,
            )
        } else {
            listOf(
                Permission.READ_EXTERNAL_STORAGE,
                Permission.WRITE_EXTERNAL_STORAGE,
            )
        }

    private fun getPermissionDisplayName(permission: String): String =
        when (permission) {
            Permission.CAMERA -> "Camera"
            Permission.RECORD_AUDIO -> "Microphone"
            Permission.ACCESS_FINE_LOCATION -> "Fine Location"
            Permission.ACCESS_COARSE_LOCATION -> "Coarse Location"
            Permission.ACCESS_BACKGROUND_LOCATION -> "Background Location"
            Permission.READ_EXTERNAL_STORAGE -> "Read Storage"
            Permission.WRITE_EXTERNAL_STORAGE -> "Write Storage"
            Permission.READ_MEDIA_IMAGES -> "Read Images"
            Permission.READ_MEDIA_VIDEO -> "Read Videos"
            Permission.READ_MEDIA_AUDIO -> "Read Audio"
            Permission.POST_NOTIFICATIONS -> "Notifications"
            else -> permission.substringAfterLast(".")
        }

    private class ThreePhasePermissionCallback(
        private val context: Context,
        private val originalCallback: PermissionCallback,
        private val startFromPhase2: Boolean = false,
    ) : PermissionCallback {
        private var phase1Completed = startFromPhase2
        private var phase2Completed = false
        private val phase1DeniedPermissions = mutableListOf<String>()
        private val phase2DeniedPermissions = mutableListOf<String>()
        private val phase3DeniedPermissions = mutableListOf<String>()

        override fun onAllGranted() {
            when {
                !phase1Completed -> {
                    phase1Completed = true
                    val foregroundLocationPermissions = getForegroundLocationPermissions()
                    if (foregroundLocationPermissions.isNotEmpty()) {
                        requestPermissions(
                            context,
                            foregroundLocationPermissions,
                            "Foreground Location Permissions",
                            this
                        )
                    } else {
                        checkBackgroundLocationPermissions()
                    }
                }

                !phase2Completed -> {
                    phase2Completed = true
                    val backgroundLocationPermissions = getBackgroundLocationPermissions()
                    if (backgroundLocationPermissions.isNotEmpty()) {
                        requestPermissions(
                            context,
                            backgroundLocationPermissions,
                            "Background Location Permissions",
                            this
                        )
                    } else {
                        originalCallback.onAllGranted()
                    }
                }

                else -> {
                    originalCallback.onAllGranted()
                }
            }
        }

        override fun onTemporarilyDenied(deniedPermissions: List<String>) {
            when {
                !phase1Completed -> {
                    phase1DeniedPermissions.addAll(deniedPermissions)
                    phase1Completed = true

                    val foregroundLocationPermissions = getForegroundLocationPermissions()
                    if (foregroundLocationPermissions.isNotEmpty()) {
                        requestPermissions(
                            context,
                            foregroundLocationPermissions,
                            "Foreground Location Permissions",
                            this
                        )
                    } else {
                        checkBackgroundLocationPermissions()
                    }
                }

                !phase2Completed -> {
                    phase2DeniedPermissions.addAll(deniedPermissions)
                    phase2Completed = true

                    android.util.Log.d(
                        "PermissionTool",
                        "[DEBUG_LOG] Foreground location denied, skipping background location request"
                    )

                    val allDeniedPermissions = phase1DeniedPermissions + phase2DeniedPermissions
                    if (allDeniedPermissions.isNotEmpty()) {
                        originalCallback.onTemporarilyDenied(allDeniedPermissions)
                    } else {
                        originalCallback.onAllGranted()
                    }
                }

                else -> {
                    phase3DeniedPermissions.addAll(deniedPermissions)

                    val allDeniedPermissions =
                        phase1DeniedPermissions + phase2DeniedPermissions + phase3DeniedPermissions
                    if (allDeniedPermissions.isNotEmpty()) {
                        originalCallback.onTemporarilyDenied(allDeniedPermissions)
                    } else {
                        originalCallback.onAllGranted()
                    }
                }
            }
        }

        override fun onPermanentlyDeniedWithSettingsOpened(deniedPermissions: List<String>) {
            when {
                !phase1Completed -> {
                    phase1DeniedPermissions.addAll(deniedPermissions)
                    phase1Completed = true

                    val foregroundLocationPermissions = getForegroundLocationPermissions()
                    if (foregroundLocationPermissions.isNotEmpty()) {
                        requestPermissions(
                            context,
                            foregroundLocationPermissions,
                            "Foreground Location Permissions",
                            this
                        )
                    } else {
                        checkBackgroundLocationPermissions()
                    }
                }

                !phase2Completed -> {
                    phase2DeniedPermissions.addAll(deniedPermissions)
                    phase2Completed = true

                    val allDeniedPermissions = phase1DeniedPermissions + phase2DeniedPermissions
                    originalCallback.onPermanentlyDeniedWithSettingsOpened(allDeniedPermissions)
                }

                else -> {
                    phase3DeniedPermissions.addAll(deniedPermissions)
                    val allDeniedPermissions =
                        phase1DeniedPermissions + phase2DeniedPermissions + phase3DeniedPermissions
                    originalCallback.onPermanentlyDeniedWithSettingsOpened(allDeniedPermissions)
                }
            }
        }

        override fun onPermanentlyDeniedWithoutSettings(deniedPermissions: List<String>) {
            when {
                !phase1Completed -> {
                    phase1DeniedPermissions.addAll(deniedPermissions)
                    phase1Completed = true

                    val foregroundLocationPermissions = getForegroundLocationPermissions()
                    if (foregroundLocationPermissions.isNotEmpty()) {
                        requestPermissions(
                            context,
                            foregroundLocationPermissions,
                            "Foreground Location Permissions",
                            this
                        )
                    } else {
                        checkBackgroundLocationPermissions()
                    }
                }

                !phase2Completed -> {
                    phase2DeniedPermissions.addAll(deniedPermissions)
                    phase2Completed = true

                    val allDeniedPermissions = phase1DeniedPermissions + phase2DeniedPermissions
                    originalCallback.onPermanentlyDeniedWithoutSettings(allDeniedPermissions)
                }

                else -> {
                    phase3DeniedPermissions.addAll(deniedPermissions)
                    val allDeniedPermissions =
                        phase1DeniedPermissions + phase2DeniedPermissions + phase3DeniedPermissions
                    originalCallback.onPermanentlyDeniedWithoutSettings(allDeniedPermissions)
                }
            }
        }

        private fun checkBackgroundLocationPermissions() {
            val backgroundLocationPermissions = getBackgroundLocationPermissions()
            if (backgroundLocationPermissions.isNotEmpty()) {
                requestPermissions(context, backgroundLocationPermissions, "Background Location Permissions", this)
            } else {
                val allDeniedPermissions = phase1DeniedPermissions + phase2DeniedPermissions
                if (allDeniedPermissions.isNotEmpty()) {
                    originalCallback.onTemporarilyDenied(allDeniedPermissions)
                } else {
                    originalCallback.onAllGranted()
                }
            }
        }
    }

    interface PermissionCallback {
        fun onAllGranted()

        fun onPartiallyGranted(grantedPermissions: List<String>) {
            onTemporarilyDenied(emptyList())
        }

        fun onTemporarilyDenied(deniedPermissions: List<String>)

        fun onPermanentlyDeniedWithSettingsOpened(deniedPermissions: List<String>) {
            onPermanentlyDeniedWithoutSettings(deniedPermissions)
        }

        fun onPermanentlyDeniedWithoutSettings(deniedPermissions: List<String>)
    }
}
