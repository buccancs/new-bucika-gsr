package com.topdon.module.thermal.ir.activity

import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.blankj.utilcode.util.AppUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.bean.event.PDFEvent
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.repository.GalleryRepository.DirType
import com.topdon.lib.core.repository.TC007Repository
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.tools.DeviceTools
import com.topdon.lib.core.utils.CommUtils
import com.topdon.lib.core.utils.NetWorkUtils
import com.topdon.lib.core.utils.PermissionUtils
import com.topdon.lms.sdk.LMS
import com.topdon.module.thermal.ir.BuildConfig
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.dialog.HomeGuideDialog
import com.topdon.module.thermal.ir.fragment.IRGalleryTabFragment
import com.topdon.module.thermal.ir.fragment.IRThermalFragment
import com.topdon.module.thermal.ir.fragment.AbilityFragment
import com.topdon.module.thermal.ir.fragment.PDFListFragment
import com.topdon.module.thermal.ir.databinding.ActivityIrMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

/**
 * Professional thermal imaging main activity supporting both TC007 and plugin-style devices.
 * 
 * This activity serves as the primary navigation hub for the thermal imaging application,
 * providing access to monitoring, gallery, reporting, and device management features.
 * Implements ViewBinding for type-safe view access and comprehensive lifecycle management.
 *
 * @property isTC007 Device type flag - true for TC007, false for plugin-style devices
 * @property binding ViewBinding instance for type-safe view access
 * 
 * Features:
 * - Multi-device support (TC007 and plugin-style devices)  
 * - Real-time connection status monitoring
 * - Professional tab-based navigation system
 * - Dynamic permission management for storage access
 * - Interactive user guidance system with blur effects
 * - Professional thermal imaging workflow integration
 *
 * Created by LCG on 2024/4/18.
 * Modernized with ViewBinding and comprehensive documentation.
 */
@Route(path = RouterConfig.IR_MAIN)
class IRMainActivity : BaseActivity(), View.OnClickListener {

    /**
     * ViewBinding instance for type-safe access to layout views.
     * Provides compile-time safety and eliminates findViewById calls.
     */
    private lateinit var binding: ActivityIrMainBinding

    /**
     * Device type identifier from parent activity.
     * - true: TC007 device with WebSocket communication
     * - false: Plugin-style device with standard connection protocol
     */
    private var isTC007 = false

    /**
     * Initializes the content view using ViewBinding.
     * @return Layout resource ID for activity_ir_main
     */
    override fun initContentView(): Int {
        binding = ActivityIrMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        return 0 // ViewBinding handles layout inflation
    }

    /**
     * Handles new intent when activity is restarted.
     * Reinitializes the view components with updated intent data.
     *
     * @param intent New intent containing updated parameters
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        initView()
    }

    /**
     * Initializes the user interface components and sets up navigation.
     * Configures ViewPager2 with fragments and establishes click listeners.
     */
    override fun initView() {
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        with(binding) {
            // Configure ViewPager2 for seamless navigation
            viewPage.apply {
                offscreenPageLimit = 5
                isUserInputEnabled = false
                adapter = ViewPagerAdapter(this@IRMainActivity, isTC007)
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        refreshTabSelect(position)
                    }
                })
                setCurrentItem(2, false) // Start with thermal imaging tab
            }

            // Set up navigation click listeners
            clIconMonitor.setOnClickListener(this@IRMainActivity)
            clIconGallery.setOnClickListener(this@IRMainActivity)
            viewMainThermal.setOnClickListener(this@IRMainActivity)
            clIconReport.setOnClickListener(this@IRMainActivity)
            clIconMine.setOnClickListener(this@IRMainActivity)
        }

        showGuideDialog()
    }

    /**
     * Updates connection status and manages automatic operations on resume.
     * Handles device-specific connection protocols and UI state updates.
     */
    override fun onResume() {
        super.onResume()
        // DeviceTools.isConnect(true) // Commented out for performance
        
        if (isTC007) {
            // TC007 device uses WebSocket connection
            if (WebSocketProxy.getInstance().isTC007Connect()) {
                NetWorkUtils.switchNetwork(false)
                binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_connect)
                
                // Sync device time in coroutine scope
                lifecycleScope.launch {
                    TC007Repository.syncTime()
                }
                
                // Auto-open thermal imaging if enabled
                if (SharedManager.isConnect07AutoOpen) {
                    ARouter.getInstance().build(RouterConfig.IR_THERMAL_07).navigation(this)
                }
            } else {
                binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_disconnect)
            }
        } else {
            // Plugin-style device connection check
            if (DeviceTools.isConnect(isAutoRequest = false)) {
                binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_connect)
            } else {
                binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_disconnect)
            }
        }
    }

    /**
     * Initializes data components.
     * Currently empty - data initialization handled in other lifecycle methods.
     */
    override fun initData() {
        // Data initialization handled in onResume and other lifecycle methods
    }

    /**
     * Handles plugin-style device connection events.
     * Updates background image to reflect connected state.
     */
    override fun connected() {
        if (!isTC007) {
            binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_connect)
        }
    }

    /**
     * Handles plugin-style device disconnection events.
     * Updates background image to reflect disconnected state.
     */
    override fun disConnected() {
        if (!isTC007) {
            binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_disconnect)
        }
    }

    /**
     * Handles WebSocket connection events for TC007 devices.
     * @param isTS004 Whether this is a TS004 device connection
     */
    override fun onSocketConnected(isTS004: Boolean) {
        if (!isTS004 && isTC007) {
            binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_connect)
        }
    }

    /**
     * Handles WebSocket disconnection events for TC007 devices.
     * @param isTS004 Whether this is a TS004 device disconnection
     */
    override fun onSocketDisConnected(isTS004: Boolean) {
        if (!isTS004 && isTC007) {
            binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_disconnect)
        }
    }

    /**
     * Handles click events for navigation tabs.
     * Manages ViewPager navigation and permission checks for storage access.
     *
     * @param v The clicked view
     */
    override fun onClick(v: View?) {
        with(binding) {
            when (v) {
                clIconMonitor -> { // Monitor/Function tab
                    viewPage.setCurrentItem(0, false)
                }
                clIconGallery -> { // Gallery tab - requires storage permission
                    checkStoragePermission()
                }
                viewMainThermal -> { // Main thermal imaging tab
                    viewPage.setCurrentItem(2, false)
                }
                clIconReport -> { // Report tab - requires login
                    if (LMS.getInstance().isLogin) {
                        viewPage.setCurrentItem(3, false)
                    } else {
                        LMS.getInstance().activityLogin(null) { loginSuccess ->
                            if (loginSuccess) {
                                viewPage.setCurrentItem(3, false)
                                EventBus.getDefault().post(PDFEvent())
                            }
                        }
                    }
                }
                clIconMine -> { // Settings/More tab
                    viewPage.setCurrentItem(4, false)
                }
            }
        }
    }

    /**
     * Updates the visual selection state of navigation tabs.
     * Ensures only the active tab appears selected with proper visual feedback.
     *
     * @param index Currently selected tab index (0-4)
     *              0=Monitor, 1=Gallery, 2=Thermal, 3=Report, 4=Mine
     */
    private fun refreshTabSelect(index: Int) {
        with(binding) {
            // Reset all tab selection states
            ivIconMonitor.isSelected = false
            tvIconMonitor.isSelected = false
            ivIconGallery.isSelected = false
            tvIconGallery.isSelected = false
            ivIconReport.isSelected = false
            tvIconReport.isSelected = false
            ivIconMine.isSelected = false
            tvIconMine.isSelected = false

            // Set selected state for active tab
            when (index) {
                0 -> { // Monitor tab
                    ivIconMonitor.isSelected = true
                    tvIconMonitor.isSelected = true
                }
                1 -> { // Gallery tab
                    ivIconGallery.isSelected = true
                    tvIconGallery.isSelected = true
                }
                3 -> { // Report tab
                    ivIconReport.isSelected = true
                    tvIconReport.isSelected = true
                }
                4 -> { // Mine tab
                    ivIconMine.isSelected = true
                    tvIconMine.isSelected = true
                }
                // Note: Index 2 (Thermal) has no tab selector as it's the main action button
            }
        }
    }

    /**
     * Displays the interactive user guidance dialog system.
     * Provides step-by-step onboarding with visual blur effects and navigation.
     * 
     * The guide progresses through three steps:
     * 1. Monitor functionality overview
     * 2. Settings and configuration options  
     * 3. Main thermal imaging features
     */
    private fun showGuideDialog() {
        if (SharedManager.homeGuideStep == 0) { // Guide completed or disabled
            return
        }

        // Navigate to appropriate tab for current guide step
        with(binding) {
            when (SharedManager.homeGuideStep) {
                1 -> viewPage.setCurrentItem(0, false) // Monitor tab
                2 -> viewPage.setCurrentItem(4, false) // Settings tab
                3 -> viewPage.setCurrentItem(2, false) // Main thermal tab
            }
        }

        val guideDialog = HomeGuideDialog(this, SharedManager.homeGuideStep)
        
        // Handle guide step navigation
        guideDialog.onNextClickListener = { step ->
            when (step) {
                1 -> {
                    binding.viewPage.setCurrentItem(4, false)
                    if (Build.VERSION.SDK_INT < 31) {
                        lifecycleScope.launch {
                            delay(100)
                            guideDialog.blurBg(binding.clRoot)
                        }
                    }
                    SharedManager.homeGuideStep = 2
                }
                2 -> {
                    binding.viewPage.setCurrentItem(2, false)
                    if (Build.VERSION.SDK_INT < 31) {
                        lifecycleScope.launch {
                            delay(100)
                            guideDialog.blurBg(binding.clRoot)
                        }
                    }
                    SharedManager.homeGuideStep = 3
                }
                3 -> {
                    SharedManager.homeGuideStep = 0 // Mark as completed
                }
            }
        }
        
        // Handle skip functionality
        guideDialog.onSkinClickListener = {
            SharedManager.homeGuideStep = 0
        }
        
        // Clean up blur effects when dialog is dismissed
        guideDialog.setOnDismissListener {
            if (Build.VERSION.SDK_INT >= 31) {
                window?.decorView?.setRenderEffect(null)
            }
        }
        
        guideDialog.show()

        // Apply background blur effects
        if (Build.VERSION.SDK_INT >= 31) {
            // Modern blur effect for API 31+
            window?.decorView?.setRenderEffect(
                RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.MIRROR)
            )
        } else {
            // Legacy blur effect with delay for proper rendering
            lifecycleScope.launch {
                delay(100) // Allow UI to settle before applying blur
                guideDialog.blurBg(binding.clRoot)
            }
        }
    }


    /**
     * Validates and requests storage permissions for gallery access.
     * Handles different Android API levels with appropriate permission sets.
     * Shows user-friendly dialogs for permission rationale and system settings.
     */
    private fun checkStoragePermission() {
        // Define permission requirements based on target SDK version
        val permissionList: List<String> = when {
            applicationInfo.targetSdkVersion >= 34 -> listOf(
                Permission.READ_MEDIA_VIDEO,
                Permission.READ_MEDIA_IMAGES,
                Permission.WRITE_EXTERNAL_STORAGE
            )
            applicationInfo.targetSdkVersion == 33 -> listOf(
                Permission.READ_MEDIA_VIDEO,
                Permission.READ_MEDIA_IMAGES,
                Permission.WRITE_EXTERNAL_STORAGE
            )
            else -> listOf(
                Permission.READ_EXTERNAL_STORAGE,
                Permission.WRITE_EXTERNAL_STORAGE
            )
        }

        if (!XXPermissions.isGranted(this, permissionList)) {
            if (BaseApplication.instance.isDomestic()) {
                // Show rationale dialog for domestic users
                TipDialog.Builder(this)
                    .setMessage(getString(R.string.permission_request_storage_app, CommUtils.getAppName()))
                    .setCancelListener(R.string.app_cancel)
                    .setPositiveListener(R.string.app_confirm) {
                        initStoragePermission(permissionList)
                    }
                    .create().show()
            } else {
                initStoragePermission(permissionList)
            }
        } else {
            initStoragePermission(permissionList)
        }
    }

    /**
     * Requests storage permissions from the system.
     * Handles special visual user cases and provides comprehensive error handling.
     *
     * @param permissionList List of required permissions to request
     */
    private fun initStoragePermission(permissionList: List<String>) {
        // Handle visual user special case
        if (PermissionUtils.isVisualUser()) {
            binding.viewPage.setCurrentItem(1, false)
            return
        }
        
        XXPermissions.with(this)
            .permission(permissionList)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    if (allGranted) {
                        binding.viewPage.setCurrentItem(1, false)
                    }
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    if (doNotAskAgain) {
                        // Permission permanently denied - guide user to settings
                        TipDialog.Builder(this@IRMainActivity)
                            .setTitleMessage(getString(R.string.app_tip))
                            .setMessage(getString(R.string.app_album_content))
                            .setPositiveListener(R.string.app_open) {
                                AppUtils.launchAppDetailsSettings()
                            }
                            .setCancelListener(R.string.app_cancel) {
                                // User cancelled - no action needed
                            }
                            .setCanceled(true)
                            .create().show()
                    }
                }
            })
    }



    /**
     * Professional ViewPager adapter for thermal imaging navigation.
     * Manages fragment lifecycle and device-specific configurations.
     *
     * @param activity Parent FragmentActivity for fragment transactions
     * @param isTC007 Device type flag for fragment configuration
     */
    private class ViewPagerAdapter(
        activity: FragmentActivity,
        private val isTC007: Boolean
    ) : FragmentStateAdapter(activity) {
        
        /**
         * Returns the total number of fragments in the ViewPager.
         * @return Fragment count (5 tabs total)
         */
        override fun getItemCount() = 5

        /**
         * Creates and configures fragments based on position and device type.
         * @param position Tab position (0-4)
         * @return Configured Fragment instance
         */
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                1 -> { // Gallery Fragment
                    IRGalleryTabFragment().apply {
                        arguments = Bundle().apply {
                            val dirType = if (isTC007) DirType.TC007.ordinal else DirType.LINE.ordinal
                            putBoolean(ExtraKeyConfig.CAN_SWITCH_DIR, false)
                            putBoolean(ExtraKeyConfig.HAS_BACK_ICON, false)
                            putInt(ExtraKeyConfig.DIR_TYPE, dirType)
                        }
                    }
                }
                0 -> AbilityFragment() // Monitor/Function Fragment
                2 -> IRThermalFragment() // Main Thermal Fragment
                3 -> PDFListFragment() // Report Fragment  
                else -> ARouter.getInstance().build(RouterConfig.TC_MORE).navigation() as Fragment // Settings Fragment
            }.apply {
                // Configure device type for all fragments except gallery
                if (position != 1) {
                    arguments = Bundle().apply { 
                        putBoolean(ExtraKeyConfig.IS_TC007, isTC007) 
                    }
                }
            }
        }
    }
}