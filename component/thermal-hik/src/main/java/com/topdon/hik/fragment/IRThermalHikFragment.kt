package com.topdon.hik.fragment

import android.graphics.Bitmap
import android.os.Bundle
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.topdon.hik.R
import com.topdon.hik.databinding.FragmentIrThermalHikBinding
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.ktbase.BaseBindingFragment
import com.topdon.libcom.bean.SaveSettingBean
import com.topdon.libhik.util.HikHelper
import com.topdon.module.thermal.ir.bean.DataBean
import com.topdon.module.thermal.ir.repository.ConfigRepository
import kotlinx.coroutines.launch

/**
 * 海康出图 Fragment.
 *
 * Created by LCG on 2025/1/14.
 */
class IRThermalHikFragment : BaseBindingFragment<FragmentIrThermalHikBinding>() {
    override fun initContentLayoutId(): Int = R.layout.fragment_ir_thermal_hik

    override fun initView(savedInstanceState: Bundle?) {
        HikHelper.bind(this)
        HikHelper.setFrameListener { yuvArray, tempArray ->
            binding.hikSurfaceView.refresh(yuvArray, tempArray)
        }
        HikHelper.onTimeoutListener = {
            // TODO: 跟进超时弹框逻辑
            TipDialog.Builder(requireContext())
                .setMessage("机芯出了毛病，5秒了没个回调过来")
                .setPositiveListener(R.string.app_got_it) {
                    activity?.finish()
                }
                .create().show()
        }
        HikHelper.onReadyListener = {
            //热成像机芯相关参数初始化
            lifecycleScope.launch {
                //自动快门强制打开；
                //对比度、锐度强制使用默认值；
                //房屋检测时伪彩跟随设置，温度监控、锅盖标定伪彩强制使用铁红
                //高低温档由于历史遗留，TC001那些都是没有重置的，这里保持一致，也不去重置

                HikHelper.initConfig()
                HikHelper.setAutoShutter(true)
                HikHelper.setContrast(50) //使用默认对比度
                HikHelper.setEnhanceLevel(50) //使用默认细节增强等级

                val config: DataBean = ConfigRepository.readConfig(false)
                HikHelper.setEmissivity((config.radiation * 100).toInt()) //应用发射率
                HikHelper.setDistance((config.distance * 100).toInt().coerceAtLeast(30))
            }
        }

        val saveSetBean = SaveSettingBean()
        binding.hikSurfaceView.setPseudoCode(saveSetBean.pseudoColorMode) //图片拾取伪彩跟随保存设置开关
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun getBitmap(): Bitmap = binding.hikSurfaceView.getScaleBitmap()
}