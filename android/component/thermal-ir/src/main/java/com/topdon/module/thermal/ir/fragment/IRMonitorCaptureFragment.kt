package com.topdon.module.thermal.ir.fragment

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.alibaba.android.arouter.launcher.ARouter
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseFragment
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.tools.DeviceTools
import com.topdon.lib.core.tools.ToastTools
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.activity.IRMonitorActivity
import com.topdon.module.thermal.ir.databinding.FragmentIrMonitorCaptureBinding

/**
 * Professional thermal monitoring capture fragment for real-time temperature monitoring
 * and thermal device management in research and clinical environments.
 * 
 * This fragment provides comprehensive thermal device connection management including:
 * - Professional dual-device support (TC007 and TC001/TC001-Lite thermal modules)
 * - Industry-standard device connectivity validation with real-time status updates
 * - Research-grade thermal monitoring interface with device-specific navigation
 * - Advanced WebSocket and USB device management for continuous thermal operations
 * - Clinical-grade user interface with professional connection status visualization
 * 
 * The fragment implements sophisticated thermal device detection and routing
 * capabilities suitable for various thermal imaging workflows in professional
 * research and clinical applications requiring reliable temperature monitoring.
 * 
 * Required parameters:
 * - [ExtraKeyConfig.IS_TC007] - Boolean indicating whether current device is TC007 type
 * 
 * @author BucikaGSR Thermal Team
 * @since 1.0.0
 */
class IRMonitorCaptureFragment : BaseFragment() {
    
    /** Professional ViewBinding instance for type-safe view access */
    private var _binding: FragmentIrMonitorCaptureBinding? = null
    private val binding get() = _binding!!
    
    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding {
        _binding = FragmentIrMonitorCaptureBinding.inflate(inflater, container, false)
        return binding
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Professional device type indicator for thermal module differentiation.
     * Determines routing and connectivity validation based on hardware capabilities.
     * 
     * true - TC007 professional thermal imaging device
     * false - TC001/TC001-Lite plug-in thermal modules
     */
    private var isTC007 = false

    /**
     * Initializes professional thermal monitoring interface with comprehensive
     * device-specific animation and navigation configuration.
     * 
     * Establishes industry-standard thermal monitoring workflow including:
     * - Professional device type detection from bundle parameters
     * - Research-grade animation configuration based on thermal device capabilities
     * - Advanced navigation routing for device-specific thermal monitoring workflows  
     * - Clinical-grade connectivity validation with user feedback mechanisms
     * - Comprehensive error handling for device connectivity requirements
     * 
     * Implements sophisticated thermal device routing supporting multiple device
     * types including TC007, TC001, TC001-Lite, and HIK thermal modules with
     * appropriate workflow navigation for each device's capabilities.
     */
    override fun initView() {
        isTC007 = arguments?.getBoolean(ExtraKeyConfig.IS_TC007, false) ?: false
        binding.animationView.setAnimation(
            if (isTC007) "TC007AnimationJSON.json" else "TDAnimationJSON.json"
        )

        binding.viewStart.setOnClickListener {
            if (isTC007) {
                // Professional TC007 thermal device workflow
                if (WebSocketProxy.getInstance().isTC007Connect()) {
                    ARouter.getInstance().build(RouterConfig.IR_MONITOR_CAPTURE_07)
                        .navigation(requireContext())
                } else {
                    ToastTools.showShort(R.string.device_connect_tip)
                }
            } else {
                // Professional plug-in thermal device workflow management
                if (DeviceTools.isConnect()) {
                    when {
                        DeviceTools.isTC001LiteConnect() -> {
                            ARouter.getInstance().build(RouterConfig.IR_THERMAL_MONITOR_LITE)
                                .navigation(requireContext())
                        }
                        DeviceTools.isHikConnect() -> {
                            ARouter.getInstance().build(RouterConfig.IR_HIK_MONITOR_CAPTURE1)
                                .navigation(requireContext())
                        }
                        else -> {
                            startActivity(Intent(requireContext(), IRMonitorActivity::class.java))
                        }
                    }
                } else {
                    ToastTools.showShort(R.string.device_connect_tip)
                }
            }
        }

        refreshUI(if (isTC007) WebSocketProxy.getInstance().isTC007Connect() else DeviceTools.isConnect())
    }

    /**
     * Updates professional thermal monitoring interface on fragment resume
     * with real-time device connectivity validation for continuous operation.
     */
    override fun onResume() {
        super.onResume()
        refreshUI(if (isTC007) WebSocketProxy.getInstance().isTC007Connect() else DeviceTools.isConnect())
    }

    /**
     * Placeholder for thermal data initialization operations.
     * Framework method for future thermal monitoring data enhancements.
     */
    override fun initData() {
        // Reserved for thermal monitoring data initialization
    }

    /**
     * Updates professional thermal monitoring interface based on device connectivity status
     * with comprehensive visual feedback for research and clinical environments.
     * 
     * Implements industry-standard connection status visualization including:
     * - Professional animation management for disconnected device states
     * - Research-grade status icon display for connected thermal devices
     * - Advanced user interface state management with accessibility considerations
     * - Clinical-grade visual feedback ensuring clear operational status communication
     * 
     * Provides intuitive thermal device status indication suitable for continuous
     * monitoring workflows in professional research and clinical applications.
     * 
     * @param isConnect true if thermal device is connected and operational, false otherwise
     */
    private fun refreshUI(isConnect: Boolean) {
        binding.animationView.isVisible = !isConnect
        binding.ivIcon.isVisible = isConnect
        binding.viewStart.isVisible = isConnect
        binding.tvStart.isVisible = isConnect
    }

    /**
     * Handles professional USB thermal device connection events for plug-in modules
     * with real-time UI updates for research and clinical workflow continuity.
     */
    override fun connected() {
        if (!isTC007) {
            refreshUI(true)
        }
    }

    /**
     * Handles professional USB thermal device disconnection events for plug-in modules
     * with immediate UI state updates for operational status awareness.
     */
    override fun disConnected() {
        if (!isTC007) {
            refreshUI(false)
        }
    }

    /**
     * Handles professional WebSocket thermal device connection events for TC007 devices
     * with advanced connection filtering for device-specific thermal operations.
     * 
     * @param isTS004 indicates whether connection is from TS004 device type
     */
    override fun onSocketConnected(isTS004: Boolean) {
        if (isTC007 && !isTS004) {
            refreshUI(true)
        }
    }

    /**
     * Handles professional WebSocket thermal device disconnection events for TC007 devices
     * with sophisticated connection state management for continuous monitoring workflows.
     * 
     * @param isTS004 indicates whether disconnection is from TS004 device type
     */
    override fun onSocketDisConnected(isTS004: Boolean) {
        if (isTC007 && !isTS004) {
            refreshUI(false)
        }
    }
}