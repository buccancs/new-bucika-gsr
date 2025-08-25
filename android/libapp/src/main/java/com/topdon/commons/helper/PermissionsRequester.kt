package com.topdon.commons.helper

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class PermissionsRequester {
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 10
        private const val REQUEST_CODE_WRITE_SETTINGS = 11
        private const val REQUEST_CODE_UNKNOWN_APP_SOURCES = 12
    }

    private val allPermissions = mutableListOf<String>()
    private val refusedPermissions = mutableListOf<String>()
    private var callback: Callback? = null
    private val activity: Activity?
    private val fragment: Fragment?
    private var checking: Boolean = false

    constructor(@NonNull activity: Activity) {
        this.activity = activity
        this.fragment = null
    }

    constructor(@NonNull fragment: Fragment) {
        this.activity = null
        this.fragment = fragment
    }

    fun interface Callback {
        fun onRequestResult(refusedPermissions: List<String>)
    }

    fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    fun checkAndRequest(@NonNull permissions: List<String>) {
        if (checking) return
        
        refusedPermissions.clear()
        allPermissions.clear()
        allPermissions.addAll(permissions)
        checkPermissions(allPermissions.toMutableList(), false)
    }

    fun hasPermissions(@NonNull permissions: List<String>): Boolean {
        return checkPermissions(permissions.toMutableList(), true)
    }

    @Suppress("DEPRECATION")
    private fun checkPermissions(permissions: MutableList<String>, onlyCheck: Boolean): Boolean {
        val context = activity ?: fragment?.context ?: return false

        if (permissions.remove(Manifest.permission.WRITE_SETTINGS) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                if (!onlyCheck) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    )
                    activity?.startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS)
                        ?: fragment?.startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS)
                    checking = true
                }
                return false
            }
        }

        if (permissions.remove(Manifest.permission.REQUEST_INSTALL_PACKAGES) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                if (!onlyCheck) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    )
                    activity?.startActivityForResult(intent, REQUEST_CODE_UNKNOWN_APP_SOURCES)
                        ?: fragment?.startActivityForResult(intent, REQUEST_CODE_UNKNOWN_APP_SOURCES)
                    checking = true
                }
                return false
            }
        }

        val needRequestPermissionList = findDeniedPermissions(permissions)
        return when {
            onlyCheck -> needRequestPermissionList.isEmpty()
            needRequestPermissionList.isNotEmpty() -> {
                val permissionArray = needRequestPermissionList.toTypedArray()
                activity?.let { ActivityCompat.requestPermissions(it, permissionArray, PERMISSION_REQUEST_CODE) }
                    ?: fragment?.requestPermissions(permissionArray, PERMISSION_REQUEST_CODE)
                checking = true
                false
            }
            else -> {
                callback?.onRequestResult(refusedPermissions)
                checking = false
                true
            }
        }
    }

    private fun findDeniedPermissions(permissions: List<String>): List<String> {
        val needRequestPermissionList = mutableListOf<String>()
        val context = activity ?: fragment?.activity ?: return needRequestPermissionList

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
                needRequestPermissionList.add(permission)
            }
        }
        return needRequestPermissionList
    }

    fun onActivityResult(requestCode: Int) {
        val context = activity ?: fragment?.context ?: return

        when (requestCode) {
            REQUEST_CODE_WRITE_SETTINGS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
                    refusedPermissions.add(Manifest.permission.WRITE_SETTINGS)
                }
                checkPermissions(allPermissions.toMutableList(), false)
            }
            REQUEST_CODE_UNKNOWN_APP_SOURCES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                    refusedPermissions.add(Manifest.permission.REQUEST_INSTALL_PACKAGES)
                }
                checkPermissions(allPermissions.toMutableList(), false)
            }
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (i in permissions.indices) {
                val permission = permissions[i]
                if (allPermissions.remove(permission) && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    refusedPermissions.add(permission)
                }
            }
            callback?.onRequestResult(refusedPermissions)
            checking = false
        }
    }
}