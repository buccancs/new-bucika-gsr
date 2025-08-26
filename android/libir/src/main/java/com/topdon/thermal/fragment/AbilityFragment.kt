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

class AbilityFragment : BaseFragment(), View.OnClickListener {
    
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
    
    private var mIsTC007 = false

    override fun initView() {
        mIsTC007 = arguments?.getBoolean(ExtraKeyConfig.IS_TC007, false) ?: false
        binding.ivWinter.setOnClickListener(this)
        binding.viewMonitory.setOnClickListener(this)
        binding.viewCar.setOnClickListener(this)
        
        binding.viewHouse.visibility = View.GONE
    }

    override fun initData() {

    }

    override fun onClick(v: View?) {
        when (v) {
            binding.ivWinter -> {

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

                val intent = Intent(requireContext(), MonitoryHomeActivity::class.java).apply {
                    putExtra(ExtraKeyConfig.IS_TC007, mIsTC007)
                }
                startActivity(intent)
            }

            binding.viewCar -> {

                if (mIsTC007) {

                    if (WebSocketProxy.getInstance().isConnected()) {
                        ARouter.getInstance().build(RouterConfig.IR_THERMAL_07)
                            .withBoolean(ExtraKeyConfig.IS_CAR_DETECT_ENTER, true)
                            .navigation(requireContext())
                    }
                } else {

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
