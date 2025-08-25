package com.topdon.thermal.activity

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
import com.topdon.thermal.BuildConfig
import com.topdon.thermal.R
import com.topdon.thermal.dialog.HomeGuideDialog
import com.topdon.thermal.fragment.IRGalleryTabFragment
import com.topdon.thermal.fragment.IRThermalFragment
import com.topdon.thermal.fragment.AbilityFragment
import com.topdon.thermal.fragment.PDFListFragment
import com.topdon.thermal.databinding.ActivityIrMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

@Route(path = RouterConfig.IR_MAIN)
class IRMainActivity : BaseActivity(), View.OnClickListener {

    private lateinit var binding: ActivityIrMainBinding

    private var isTC007 = false

    override fun initContentView(): Int {
        binding = ActivityIrMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        return 0
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        initView()
    }

    override fun initView() {
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        with(binding) {

            viewPage.apply {
                offscreenPageLimit = 5
                isUserInputEnabled = false
                adapter = ViewPagerAdapter(this@IRMainActivity, isTC007)
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        refreshTabSelect(position)
                    }
                })
                setCurrentItem(2, false)
            }

            clIconMonitor.setOnClickListener(this@IRMainActivity)
            clIconGallery.setOnClickListener(this@IRMainActivity)
            viewMainThermal.setOnClickListener(this@IRMainActivity)
            clIconReport.setOnClickListener(this@IRMainActivity)
            clIconMine.setOnClickListener(this@IRMainActivity)
        }

        showGuideDialog()
    }

    override fun onResume() {
        super.onResume()

        if (isTC007) {

            if (WebSocketProxy.getInstance().isTC007Connect()) {
                NetWorkUtils.switchNetwork(false)
                binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_connect)
                
                lifecycleScope.launch {
                    TC007Repository.syncTime()
                }
                
                if (SharedManager.isConnect07AutoOpen) {
                    ARouter.getInstance().build(RouterConfig.IR_THERMAL_07).navigation(this)
                }
            } else {
                binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_disconnect)
            }
        } else {

            if (DeviceTools.isConnect(isAutoRequest = false)) {
                binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_connect)
            } else {
                binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_disconnect)
            }
        }
    }

    override fun initData() {

    }

    override fun connected() {
        if (!isTC007) {
            binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_connect)
        }
    }

    override fun disConnected() {
        if (!isTC007) {
            binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_disconnect)
        }
    }

    override fun onSocketConnected(isTS004: Boolean) {
        if (!isTS004 && isTC007) {
            binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_connect)
        }
    }

    override fun onSocketDisConnected(isTS004: Boolean) {
        if (!isTS004 && isTC007) {
            binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_disconnect)
        }
    }

    override fun onClick(v: View?) {
        with(binding) {
            when (v) {
                clIconMonitor -> {
                    viewPage.setCurrentItem(0, false)
                }
                clIconGallery -> {
                    checkStoragePermission()
                }
                viewMainThermal -> {
                    viewPage.setCurrentItem(2, false)
                }
                clIconReport -> {
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
                clIconMine -> {
                    viewPage.setCurrentItem(4, false)
                }
            }
        }
    }

    private fun refreshTabSelect(index: Int) {
        with(binding) {

            ivIconMonitor.isSelected = false
            tvIconMonitor.isSelected = false
            ivIconGallery.isSelected = false
            tvIconGallery.isSelected = false
            ivIconReport.isSelected = false
            tvIconReport.isSelected = false
            ivIconMine.isSelected = false
            tvIconMine.isSelected = false

            when (index) {
                0 -> {
                    ivIconMonitor.isSelected = true
                    tvIconMonitor.isSelected = true
                }
                1 -> {
                    ivIconGallery.isSelected = true
                    tvIconGallery.isSelected = true
                }
                3 -> {
                    ivIconReport.isSelected = true
                    tvIconReport.isSelected = true
                }
                4 -> {
                    ivIconMine.isSelected = true
                    tvIconMine.isSelected = true
                }

            }
        }
    }

    private fun showGuideDialog() {
        if (SharedManager.homeGuideStep == 0) {
            return
        }

        with(binding) {
            when (SharedManager.homeGuideStep) {
                1 -> viewPage.setCurrentItem(0, false)
                2 -> viewPage.setCurrentItem(4, false)
                3 -> viewPage.setCurrentItem(2, false)
            }
        }

        val guideDialog = HomeGuideDialog(this, SharedManager.homeGuideStep)
        
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
                    SharedManager.homeGuideStep = 0
                }
            }
        }
        
        guideDialog.onSkinClickListener = {
            SharedManager.homeGuideStep = 0
        }
        
        guideDialog.setOnDismissListener {
            if (Build.VERSION.SDK_INT >= 31) {
                window?.decorView?.setRenderEffect(null)
            }
        }
        
        guideDialog.show()

        if (Build.VERSION.SDK_INT >= 31) {

            window?.decorView?.setRenderEffect(
                RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.MIRROR)
            )
        } else {

            lifecycleScope.launch {
                delay(100)
                guideDialog.blurBg(binding.clRoot)
            }
        }
    }

    private fun checkStoragePermission() {

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

    private fun initStoragePermission(permissionList: List<String>) {

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

                        TipDialog.Builder(this@IRMainActivity)
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

    private class ViewPagerAdapter(
        activity: FragmentActivity,
        private val isTC007: Boolean
    ) : FragmentStateAdapter(activity) {
        
        override fun getItemCount() = 5

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                1 -> {
                    IRGalleryTabFragment().apply {
                        arguments = Bundle().apply {
                            val dirType = if (isTC007) DirType.TC007.ordinal else DirType.LINE.ordinal
                            putBoolean(ExtraKeyConfig.CAN_SWITCH_DIR, false)
                            putBoolean(ExtraKeyConfig.HAS_BACK_ICON, false)
                            putInt(ExtraKeyConfig.DIR_TYPE, dirType)
                        }
                    }
                }
                0 -> AbilityFragment()
                2 -> IRThermalFragment()
                3 -> PDFListFragment()
                else -> ARouter.getInstance().build(RouterConfig.TC_MORE).navigation() as Fragment
            }.apply {

                if (position != 1) {
                    arguments = Bundle().apply { 
                        putBoolean(ExtraKeyConfig.IS_TC007, isTC007) 
                    }
                }
            }
        }
    }
