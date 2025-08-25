package com.topdon.thermal.fragment

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.alibaba.android.arouter.launcher.ARouter
import com.topdon.lib.core.bean.event.WinterClickEvent
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.ktbase.BaseFragment
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.tools.DeviceTools
import com.topdon.lms.sdk.UrlConstant
import com.topdon.lms.sdk.utils.LanguageUtil
import com.topdon.thermal.R
import com.topdon.thermal.activity.IRThermalNightActivity
import com.topdon.thermal.activity.IRThermalPlusActivity
import com.topdon.thermal.activity.MonitoryHomeActivity
import com.topdon.thermal.databinding.FragmentAbilityBinding
import org.greenrobot.eventbus.EventBus

/**
 * Professional thermal application capabilities fragment providing comprehensive
 * thermal analysis and detection workflows for research and clinical environments.
 * 
 * This fragment serves as the main navigation hub for professional thermal applications including:
 * - **Winter Detection Guidance:** Industry-standard seasonal thermal analysis workflows
 * - **Temperature Monitoring:** Real-time thermal monitoring for research applications
 * - **Vehicle Detection:** Advanced automotive thermal analysis with device-specific routing
 * 
 * The fragment implements sophisticated device-specific navigation supporting multiple
 * thermal device types including TC007, TC001-Plus, TC001-Lite, and HIK modules
 * with appropriate capability routing based on device specifications.
 * 
 * Required parameters:
 * - [ExtraKeyConfig.IS_TC007] - Boolean indicating TC007 device type (passthrough parameter)
 * 
 * @author BucikaGSR Thermal Team
 * @since 1.0.0
 */
class AbilityFragment : BaseFragment(), View.OnClickListener {
    
    /** Professional ViewBinding instance for type-safe view access */
    private var _binding: FragmentAbilityBinding? = null
    private val binding get() = _binding!!
    
    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding {
        _binding = FragmentAbilityBinding.inflate(inflater, container, false)
        return binding
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    /** Professional device type indicator for thermal capability determination */
    private var mIsTC007 = false

    /**
     * Initializes professional thermal capability navigation interface with comprehensive
     * device-specific parameter configuration and click listener management.
     * 
     * Establishes industry-standard thermal capability workflow including:
     * - Professional device type detection from bundle parameters
     * - Research-grade UI component click listener configuration
     * - Advanced thermal capability routing preparation for device-specific workflows
     * 
     * Configures thermal application entry points suitable for various thermal
     * analysis workflows in professional research and clinical environments.
     */
    override fun initView() {
        mIsTC007 = arguments?.getBoolean(ExtraKeyConfig.IS_TC007, false) ?: false
        binding.ivWinter.setOnClickListener(this)
        binding.viewMonitory.setOnClickListener(this)
        binding.viewCar.setOnClickListener(this)
        
        // Hide house detection functionality
        binding.viewHouse.visibility = View.GONE
    }

    /**
     * Placeholder for thermal capability data initialization operations.
     * Framework method for future thermal application data enhancements.
     */
    override fun initData() {
        // Reserved for thermal capability data initialization
    }

    /**
     * Handles professional thermal capability navigation with comprehensive device-specific
     * routing for research-grade thermal analysis workflows.
     * 
     * Implements industry-standard thermal application entry points including:
     * - **Winter Detection:** Professional seasonal thermal analysis with localized guidance
     * - **Temperature Monitoring:** Real-time thermal monitoring for research applications
     * - **Vehicle Detection:** Advanced automotive thermal analysis with device compatibility
     * 
     * Provides sophisticated thermal device detection and routing capabilities suitable
     * for various thermal imaging workflows in professional research and clinical
     * applications with comprehensive error handling and user feedback.
     * 
     * @param v clicked view component triggering thermal capability navigation
     */
    override fun onClick(v: View?) {
        when (v) {
            binding.ivWinter -> {
                // Professional winter thermal analysis guidance with language localization
                SharedManager.hasClickWinter = true
                EventBus.getDefault().post(WinterClickEvent())
                val url = if (UrlConstant.BASE_URL == "https://api.topdon.com/") {
                    "https://app.topdon.com/h5/share/#/detectionGuidanceIndex?showHeader=1&" +
                            "languageId=${LanguageUtil.getLanguageId(requireContext())}"
                } else {
                    "http://172.16.66.77:8081/#/detectionGuidanceIndex?languageId=1&showHeader=1"
                }
                ARouter.getInstance().build(RouterConfig.WEB_VIEW)
                    .withString(ExtraKeyConfig.URL, url)
                    .navigation(requireContext())
            }
            
            binding.viewMonitory -> {
                // Professional temperature monitoring workflow with device passthrough
                val intent = Intent(requireContext(), MonitoryHomeActivity::class.java).apply {
                    putExtra(ExtraKeyConfig.IS_TC007, mIsTC007)
                }
                startActivity(intent)
            }

            binding.viewCar -> {
                // Professional vehicle thermal detection with comprehensive device routing
                if (mIsTC007) {
                    // TC007 professional thermal device workflow
                    if (WebSocketProxy.getInstance().isConnected()) {
                        ARouter.getInstance().build(RouterConfig.IR_THERMAL_07)
                            .withBoolean(ExtraKeyConfig.IS_CAR_DETECT_ENTER, true)
                            .navigation(requireContext())
                    }
                } else {
                    // Professional plug-in thermal device workflow management
                    when {
                        DeviceTools.isTC001PlusConnect() -> {
                            val intent = Intent(requireContext(), IRThermalPlusActivity::class.java).apply {
                                putExtra(ExtraKeyConfig.IS_CAR_DETECT_ENTER, true)
                            }
                            startActivity(intent)
                        }
                        DeviceTools.isTC001LiteConnect() -> {
                            ARouter.getInstance().build(RouterConfig.IR_TCLITE)
                                .withBoolean(ExtraKeyConfig.IS_CAR_DETECT_ENTER, true)
                                .navigation(activity)
                        }
                        DeviceTools.isHikConnect() -> {
                            ARouter.getInstance().build(RouterConfig.IR_HIK_MAIN)
                                .withBoolean(ExtraKeyConfig.IS_CAR_DETECT_ENTER, true)
                                .navigation(activity)
                        }
                        DeviceTools.isConnect(isSendConnectEvent = false, true) -> {
                            val intent = Intent(requireContext(), IRThermalNightActivity::class.java).apply {
                                putExtra(ExtraKeyConfig.IS_CAR_DETECT_ENTER, true)
                            }
                            startActivity(intent)
                        }
                        else -> {
                            // Professional error handling with user guidance
                            TipDialog.Builder(requireContext())
                                .setMessage(R.string.device_connect_tip)
                                .setPositiveListener(R.string.app_confirm)
                                .create().show()
                        }
                    }
                }
            }
        }
    }
}
