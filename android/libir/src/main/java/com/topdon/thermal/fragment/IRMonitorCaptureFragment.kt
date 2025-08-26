package com.topdon.thermal.fragment

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
import com.topdon.thermal.R
import com.topdon.thermal.activity.IRMonitorActivity
import com.topdon.thermal.databinding.FragmentIrMonitorCaptureBinding

class IRMonitorCaptureFragment : BaseFragment() {
    
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

    private var isTC007 = false

    override fun initView() {
        isTC007 = arguments?.getBoolean(ExtraKeyConfig.IS_TC007, false) ?: false
        binding.animationView.setAnimation(
            if (isTC007) "TC007AnimationJSON.json" else "TDAnimationJSON.json"
        )

        binding.viewStart.setOnClickListener {
            if (isTC007) {

                if (WebSocketProxy.getInstance().isTC007Connect()) {
                    ARouter.getInstance().build(RouterConfig.IR_MONITOR_CAPTURE_07)
                        .navigation(requireContext())
                } else {
                    ToastTools.showShort(R.string.device_connect_tip)
                }
            } else {

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

    override fun onResume() {
        super.onResume()
        refreshUI(if (isTC007) WebSocketProxy.getInstance().isTC007Connect() else DeviceTools.isConnect())
    }

    override fun initData() {

    }

    private fun refreshUI(isConnect: Boolean) {
        binding.animationView.isVisible = !isConnect
        binding.ivIcon.isVisible = isConnect
        binding.viewStart.isVisible = isConnect
        binding.tvStart.isVisible = isConnect
    }

    override fun connected() {
        if (!isTC007) {
            refreshUI(true)
        }
    }

    override fun disConnected() {
        if (!isTC007) {
            refreshUI(false)
        }
    }

    override fun onSocketConnected(isTS004: Boolean) {
        if (isTC007 && !isTS004) {
            refreshUI(true)
        }
    }

    override fun onSocketDisConnected(isTS004: Boolean) {
        if (isTC007 && !isTS004) {
            refreshUI(false)
        }
    }
