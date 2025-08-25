package com.topdon.tc001

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.SparseArray
import android.view.KeyEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.Utils
import com.elvishew.xlog.XLog
import com.example.suplib.wrapper.SupHelp
import com.example.thermal_lite.activity.IRThermalLiteActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.topdon.hik.activity.IRThermalHikActivity
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.bean.event.TS004ResetEvent
import com.topdon.lib.core.bean.event.WinterClickEvent
import com.topdon.lib.core.bean.event.device.DevicePermissionEvent
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.AppConfig
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.dialog.FirmwareUpDialog
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.dialog.TipOtgDialog
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.repository.GalleryRepository
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.tools.DeviceTools
import com.topdon.lib.core.utils.CommUtils
import com.topdon.lib.core.utils.PermissionUtils
import com.topdon.lib.core.viewmodel.VersionViewModel
import com.topdon.lms.sdk.LMS
import com.topdon.thermal.activity.IRThermalNightActivity
import com.topdon.thermal.activity.IRThermalPlusActivity
import com.topdon.thermal.fragment.IRGalleryTabFragment
import com.topdon.module.user.fragment.MineFragment
import com.topdon.tc001.app.App
import com.topdon.tc001.fragment.MainFragment
import com.topdon.tc001.usb.USBHotPlugManager
import com.topdon.tc001.utils.AppVersionUtil
import com.zoho.commons.LauncherModes
import com.zoho.commons.LauncherProperties
import com.zoho.salesiqembed.ZohoSalesIQ

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.topdon.tc001.databinding.ActivityMainBinding
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

@Route(path = RouterConfig.MAIN)
class MainActivity : BaseActivity(), View.OnClickListener, USBHotPlugManager.USBDeviceListener {

    private val versionViewModel: VersionViewModel by viewModels()
    private lateinit var usbHotPlugManager: USBHotPlugManager
    private lateinit var binding: ActivityMainBinding

    private var checkPermissionType: Int = -1

    private fun logInfo() {
        try {
            val str = StringBuilder()
            str.append("Info").append("\n")
            str.append("FLAVOR: ${BuildConfig.FLAVOR}").append("\n")
            str.append("VERSION_CODE: ${BuildConfig.VERSION_CODE}").append("\n")
            str.append("VERSION_NAME: ${BuildConfig.VERSION_NAME}").append("\n")
            str.append("VERSION_DATE: ${BuildConfig.VERSION_DATE}").append("\n")
            str.append("BRAND: ${Build.BRAND}").append("\n")
            str.append("MODEL: ${Build.MODEL}").append("\n")
            str.append("PRODUCT: ${Build.PRODUCT}").append("\n")
            str.append("CPU_ABI: ${Build.CPU_ABI}").append("\n")
            str.append("SDK_INT: ${Build.VERSION.SDK_INT}").append("\n")
            str.append("RELEASE: ${Build.VERSION.RELEASE}").append("\n")
            if (SharedManager.getHasShowClause()) {
                XLog.i(str)
            }
        } catch (e: Exception) {
            if (SharedManager.getHasShowClause()) {
                XLog.e("log error: ${e.message}")
            }
        }
    }

    override fun initView() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        logInfo()
        lifecycleScope.launch(Dispatchers.IO){
            SupHelp.getInstance().initAiUpScaler(Utils.getApp())
        }
        binding.viewPage.offscreenPageLimit = 3
        binding.viewPage.isUserInputEnabled = false
        binding.viewPage.adapter = ViewPagerAdapter(this)
        binding.viewPage.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                refreshTabSelect(position)
            }
        })
        if (savedInstanceState == null) {
            binding.viewPage.setCurrentItem(1, false)
        }

        binding.viewMinePoint.isVisible = !SharedManager.hasClickWinter

        binding.clIconGallery.setOnClickListener(this)
        binding.viewMain.setOnClickListener(this)
        binding.clIconMine.setOnClickListener(this)
        App.instance.initWebSocket()
        
        usbHotPlugManager = USBHotPlugManager.getInstance(this)
        usbHotPlugManager.setUSBDeviceListener(this)
        
        copyFile("SR.pb", File(filesDir, "SR.pb"))
        BaseApplication.instance.clearDb()
        if (BaseApplication.instance.isDomestic()) {
            checkAppVersion(true)
        } else {
            versionViewModel.checkVersion()
        }

        if (!SharedManager.hasTcLine && !SharedManager.hasTS004 && !SharedManager.hasTC007) {

            if (DeviceTools.isConnect()) {
                if (!WebSocketProxy.getInstance().isConnected()) {
                    ARouter.getInstance()
                        .build(RouterConfig.IR_MAIN)
                        .withBoolean(ExtraKeyConfig.IS_TC007, false)
                        .navigation(this)
                }
            } else {
                if (WebSocketProxy.getInstance().isTS004Connect()) {
                    ARouter.getInstance().build(RouterConfig.IR_MONOCULAR).navigation(this)
                } else if (WebSocketProxy.getInstance().isTC007Connect()) {
                    ARouter.getInstance()
                        .build(RouterConfig.IR_MAIN)
                        .withBoolean(ExtraKeyConfig.IS_TC007, true)
                        .navigation(this)
                }
            }
        }

        if (DeviceTools.isConnect()) {
            SharedManager.hasTcLine = true
        }
        if (WebSocketProxy.getInstance().isTS004Connect()) {
            SharedManager.hasTS004 = true
        }
        if (WebSocketProxy.getInstance().isTC007Connect()) {
            SharedManager.hasTC007 = true
        }

    }

    override fun onStart() {
        super.onStart()

        versionViewModel.updateLiveData.observe(this) {
            FirmwareUpDialog(this).apply {
                titleStr = getString(com.topdon.lib.core.R.string.update_new_version)
                sizeStr = it.versionNo
                contentStr = it.description
                isShowCancel = !it.isForcedUpgrade
                onConfirmClickListener = {
                    updateApk(it.downPageUrl)
                }
                onCancelClickListener = {
                    SharedManager.setVersionCheckDate(System.currentTimeMillis())
                }
            }.show()
        }
    }

    private fun updateApk(url : String) {
        if (applicationInfo.targetSdkVersion < Build.VERSION_CODES.P) {

            val intent = Intent()
            intent.action = "android.intent.action.VIEW"
            intent.data = Uri.parse(url)
            startActivity(intent)
        } else {
            if (AppUtils.isAppInstalled("com.android.vending")) {
                try {
                    val intent = Intent()
                    intent.action = "android.intent.action.VIEW"
                    intent.data = Uri.parse(AppConfig.GOOGLE_APK_MARKET_URL)
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = "android.intent.action.VIEW"
                    intent.data = Uri.parse(AppConfig.GOOGLE_APK_URL)
                    startActivity(intent)
                }
            } else {
                val intent = Intent()
                intent.action = "android.intent.action.VIEW"
                intent.data = Uri.parse(AppConfig.GOOGLE_APK_URL)
                startActivity(intent)
            }
        }
    }

    private var resetTipsDialog: TipDialog? = null
    private fun showResetTipsDialog() {
        disconnectDialog?.dismiss()
        if (resetTipsDialog == null) {
            resetTipsDialog = TipDialog.Builder(this)
                .setMessage(R.string.device_reset_alert)
                .setPositiveListener(R.string.app_got_it) {
                }
                .create()
        }
        resetTipsDialog?.show()
    }

    private var disconnectDialog: TipDialog? = null
    private fun dialogDisconnect(){
        if (resetTipsDialog?.isShowing == true) {
            return
        }
        if (disconnectDialog == null) {
            disconnectDialog = TipDialog.Builder(this)
                .setMessage(R.string.device_disconnect_alert)
                .setPositiveListener(R.string.app_got_it) {
                }
                .create()
        }
        disconnectDialog?.show()
    }

    private fun copyFile(filename: String, targetFile: File) {
        if (targetFile.exists()) {
            return
        }
        try {
            val inputStream = assets.open(filename)
            val outputStream: OutputStream = FileOutputStream(targetFile)
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            inputStream.close()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun initData() {
        checkPermissionType = 0
        checkCameraPermission()
    }

    override fun onResume() {
        super.onResume()
        LMS.getInstance().language = SharedManager.getLanguage(this)

        usbHotPlugManager.startMonitoring()

    }

    override fun onPause() {
        super.onPause()

        usbHotPlugManager.stopMonitoring()
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.clIconGallery -> {
                checkPermissionType = 1
                checkStoragePermission()
            }
            binding.viewMain -> {
                binding.viewPage.setCurrentItem(1, false)
            }
            binding.clIconMine -> {
                binding.viewPage.setCurrentItem(2, false)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            TipDialog.Builder(this)
                .setMessage(getString(R.string.main_exit, CommUtils.getAppName()))
                .setCancelListener(R.string.app_no)
                .setPositiveListener(R.string.app_yes) {
                    BaseApplication.instance.exitAll()
                    finish()
                }
                .create().show()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun getDevicePermission(event: DevicePermissionEvent) {
        DeviceTools.requestUsb(this, 0, event.device)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWinterClick(event: WinterClickEvent) {
        binding.viewMinePoint.isVisible = false
    }

    private fun refreshTabSelect(index: Int) {
        binding.ivIconGallery.isSelected = false
        binding.tvIconGallery.isSelected = false
        binding.ivIconMine.isSelected = false
        binding.tvIconMine.isSelected = false
        binding.ivBottomMainBg.setImageResource(R.drawable.ic_main_bg_not_select)

        when (index) {
            0 -> {
                binding.ivIconGallery.isSelected = true
                binding.tvIconGallery.isSelected = true
            }
            1 -> {
                binding.ivBottomMainBg.setImageResource(R.drawable.ic_main_bg_select)
            }
            2 -> {
                binding.ivIconMine.isSelected = true
                binding.tvIconMine.isSelected = true
            }
        }
    }

    override fun connected() {
        if (SharedManager.isConnectAutoOpen) {
            checkPermissionType = 2
            checkCameraPermission()
        }
    }

    private var tipOtgDialog: TipOtgDialog? = null

    override fun disConnected() {
        if (WebSocketProxy.getInstance().isTS004Connect()) {
            ARouter.getInstance().build(RouterConfig.IR_MONOCULAR).navigation(this)
        }

        if (tipOtgDialog != null && tipOtgDialog!!.isShowing) {
            return
        }
        if (SharedManager.isTipOTG && !BaseApplication.instance.hasOtgShow) {
            tipOtgDialog = TipOtgDialog.Builder(this)
                .setMessage(R.string.tip_otg)
                .setPositiveListener(R.string.app_confirm) {
                    SharedManager.isTipOTG = !it
                }
                .create()
            tipOtgDialog?.show()
            BaseApplication.instance.hasOtgShow = true
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTS004ResetEvent(event: TS004ResetEvent) {
        showResetTipsDialog()
    }

    override fun onSocketConnected(isTS004: Boolean) {
        disconnectDialog?.dismiss()
    }

    override fun onSocketDisConnected(isTS004: Boolean) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED) && isTS004) {
            dialogDisconnect()
        }
    }

    private class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount() = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> {
                    IRGalleryTabFragment().apply {
                        arguments = Bundle().also {
                            it.putBoolean(ExtraKeyConfig.CAN_SWITCH_DIR, true)
                            it.putBoolean(ExtraKeyConfig.HAS_BACK_ICON, false)
                            it.putInt(ExtraKeyConfig.DIR_TYPE, GalleryRepository.DirType.LINE.ordinal)
                        }
                    }
                }
                1 -> MainFragment()
                else -> MineFragment()
            }
        }
    }

    private fun getNeedPermissionList(): SparseArray<List<String>> {
        val sparseArray = SparseArray<List<String>>()
        sparseArray.append(R.string.permission_request_camera_app, listOf(Manifest.permission.CAMERA))
        (if (this.applicationInfo.targetSdkVersion >= 34){
            listOf(
                Permission.READ_MEDIA_VIDEO,
                Permission.READ_MEDIA_IMAGES,
                Permission.WRITE_EXTERNAL_STORAGE
            )
        } else if (this.applicationInfo.targetSdkVersion == 33) {
            listOf(
                Permission.READ_MEDIA_VIDEO,
                Permission.READ_MEDIA_IMAGES,
                Permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            listOf(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE)
        }).let {
            sparseArray.append(R.string.permission_request_storage_app, it)
        }
        return sparseArray
    }

    private fun checkCameraPermission() {
        if (!PermissionUtils.isVisualUser() && !XXPermissions.isGranted(
                this,
                getNeedPermissionList()[R.string.permission_request_camera_app]
            )
        ) {
            if (BaseApplication.instance.isDomestic()) {
                if (SharedManager.getMainPermissionsState()) {

                    return
                }
                TipDialog.Builder(this)
                    .setMessage(getString(R.string.permission_request_camera_app, CommUtils.getAppName()))
                    .setCancelListener(R.string.app_cancel)
                    .setPositiveListener(R.string.app_confirm) {
                        initCameraPermission()
                    }
                    .create().show()
            } else {
                initCameraPermission()
            }
        } else {
            initCameraPermission()
        }
    }

    private fun initCameraPermission() {
        XXPermissions.with(this)
            .permission(getNeedPermissionList()[R.string.permission_request_camera_app])
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    if (allGranted) {
                        checkStoragePermission()
                    }
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    if (BaseApplication.instance.isDomestic()) {
                        SharedManager.setMainPermissionsState(true)
                    }
                    if (doNotAskAgain) {

                        TipDialog.Builder(this@MainActivity)
                            .setTitleMessage(getString(R.string.app_tip))
                            .setMessage(if (PermissionUtils.hasCameraPermission())
                                getString(R.string.app_album_content)
                                else getString(R.string.app_camera_content))
                            .setPositiveListener(R.string.app_open) {
                                AppUtils.launchAppDetailsSettings()
                            }
                            .setCancelListener(R.string.app_cancel) {
                            }
                            .setCanceled(true)
                            .create().show()
                    }
                }
            })
    }

    private fun checkStoragePermission() {
        if (!XXPermissions.isGranted(this, getNeedPermissionList()[R.string.permission_request_storage_app])) {
            if (BaseApplication.instance.isDomestic()) {
                TipDialog.Builder(this)
                    .setMessage(getString(R.string.permission_request_storage_app, CommUtils.getAppName()))
                    .setCancelListener(R.string.app_cancel)
                    .setPositiveListener(R.string.app_confirm) {
                        initStoragePermission()
                    }
                    .create().show()
            } else {
                initStoragePermission()
            }
        } else {
            initStoragePermission()
        }
    }

    private fun initStoragePermission() {
        if (PermissionUtils.isVisualUser()){
            jumpIRActivity()
            return
        }
        XXPermissions.with(this)
            .permission(
                getNeedPermissionList()[R.string.permission_request_storage_app]
            )
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    if (allGranted) {
                        jumpIRActivity()
                    }
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    if (doNotAskAgain) {

                        TipDialog.Builder(this@MainActivity)
                            .setTitleMessage(getString(R.string.app_tip))
                            .setMessage(getString(R.string.app_album_content))
                            .setPositiveListener(R.string.app_open) {
                                AppUtils.launchAppDetailsSettings()
                            }
                            .setCancelListener(R.string.app_cancel) {
                            }
                            .setCanceled(true)
                            .create().show()
                    }
                }
            })
    }

    fun jumpIRActivity(){
        when (checkPermissionType) {
            0 -> {
                DeviceTools.isConnect(isSendConnectEvent = true)
            }
            1 -> {
                binding.viewPage.setCurrentItem(0, false)
            }
            2 -> {

                if (DeviceTools.isTC001PlusConnect()) {
                    ARouter.getInstance().build(RouterConfig.IR_MAIN).navigation(this@MainActivity)
                    startActivityForResult(Intent(this@MainActivity, IRThermalPlusActivity::class.java), 101)
                }else if(DeviceTools.isTC001LiteConnect()){
                    ARouter.getInstance().build(RouterConfig.IR_MAIN).navigation(this@MainActivity)
                    startActivityForResult(Intent(this@MainActivity, IRThermalLiteActivity::class.java), 101)
                } else if (DeviceTools.isHikConnect()) {
                    ARouter.getInstance().build(RouterConfig.IR_MAIN).navigation(this@MainActivity)
                    startActivity(Intent(this, IRThermalHikActivity::class.java))
                } else{
                    ARouter.getInstance().build(RouterConfig.IR_MAIN).navigation(this@MainActivity)
                    startActivityForResult(Intent(this@MainActivity, IRThermalNightActivity::class.java), 101)
                }
            }
        }
    }

    private var appVersionUtil: AppVersionUtil? = null
    private fun checkAppVersion(isShow: Boolean) {
        if (appVersionUtil == null) {
            appVersionUtil = AppVersionUtil(this, object : AppVersionUtil.DotIsShowListener {
                override fun isShow(show: Boolean) {
                }

                override fun version(version: String) {
                }
            })
        }
        appVersionUtil?.checkVersion(isShow)
    }

    override fun onDeviceAttached(device: android.hardware.usb.UsbDevice) {
        XLog.i("MainActivity", "USB device attached: ${device.deviceName}")

        runOnUiThread {

        }
    }

    override fun onDeviceDetached(device: android.hardware.usb.UsbDevice) {
        XLog.i("MainActivity", "USB device detached: ${device.deviceName}")

        runOnUiThread {

        }
    }

    override fun onSupportedDeviceDetected(device: android.hardware.usb.UsbDevice, deviceType: USBHotPlugManager.DeviceType) {
        XLog.i("MainActivity", "Supported device detected: ${device.deviceName}, type: $deviceType")
        
        runOnUiThread {
            when (deviceType) {
                USBHotPlugManager.DeviceType.TC001_THERMAL -> {

                    SharedManager.hasTcLine = true

                }
                USBHotPlugManager.DeviceType.FTDI_SERIAL -> {

                    XLog.i("MainActivity", "FTDI serial device connected")
                }
                else -> {
                    XLog.i("MainActivity", "Other supported device connected")
                }
            }
        }
    }

    override fun onDevicePermissionGranted(device: android.hardware.usb.UsbDevice) {
        XLog.i("MainActivity", "Permission granted for: ${device.deviceName}")
        
        runOnUiThread {
            android.widget.Toast.makeText(
                this,
                "USB device ready: ${device.deviceName}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            
            if (DeviceTools.isConnect()) {
                SharedManager.hasTcLine = true
            }
        }
    }

    override fun onDevicePermissionDenied(device: android.hardware.usb.UsbDevice) {
        XLog.w("MainActivity", "Permission denied for: ${device.deviceName}")
        
        runOnUiThread {
            android.widget.Toast.makeText(
                this,
                "USB permission required for: ${device.deviceName}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
