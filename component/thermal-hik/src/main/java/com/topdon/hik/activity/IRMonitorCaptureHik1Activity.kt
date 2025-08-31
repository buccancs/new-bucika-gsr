package com.topdon.hik.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.topdon.hik.R
import com.topdon.hik.databinding.ActivityIrMonitorCaptureHik1Binding
import com.topdon.lib.core.common.SaveSettingUtil
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.ktbase.BaseBindingActivity
import com.topdon.lib.ui.dialog.MonitorSelectDialog
import com.topdon.libhik.util.HikHelper
import com.topdon.module.thermal.ir.bean.DataBean
import com.topdon.module.thermal.ir.bean.SelectPositionBean
import com.topdon.module.thermal.ir.repository.ConfigRepository
import com.topdon.module.thermal.ir.view.TemperatureBaseView.Mode
import kotlinx.coroutines.launch

/**
 * 海康温度监控生成第1步 - 选取区域监听.
 *
 * Created by LCG on 2025/1/10.
 */
@Route(path = RouterConfig.IR_HIK_MONITOR_CAPTURE1)
class IRMonitorCaptureHik1Activity : BaseBindingActivity<ActivityIrMonitorCaptureHik1Binding>(), View.OnClickListener {

    /**
     * 是否点击了下一步.
     * 用于判断是否需要在 onStop() 阶段 stopStream()；
     * 是否需要在 onDestroy() 阶段 release()
     */
    private var hasClickNext = false

    /**
     * 当前拾取的点、线、面信息
     */
    private var selectIndex: SelectPositionBean? = null

    override fun initContentLayoutId(): Int = R.layout.activity_ir_monitor_capture_hik1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HikHelper.init(this)
        HikHelper.setTempListener {
            binding.temperatureView.refreshTemp(it)
        }
        HikHelper.setFrameListener { yuvArray, tempArray ->
            binding.hikSurfaceView.refresh(yuvArray, tempArray)
        }
        HikHelper.onTimeoutListener = {
            // TODO: 跟进超时弹框逻辑
            TipDialog.Builder(this)
                .setMessage("机芯出了毛病，5秒了没个回调过来")
                .setPositiveListener(R.string.app_got_it) {
                    finish()
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

        binding.temperatureView.mode = Mode.CLEAR
        binding.temperatureView.tempTextSize = SaveSettingUtil.tempTextSize
        binding.temperatureView.onPointListener = {
            if (it.isNotEmpty()) {
                selectIndex = SelectPositionBean(1, it.first())
            }
        }
        binding.temperatureView.onLineListener = {
            if (it.size > 1) {
                selectIndex = SelectPositionBean(2, it[0], it[1])
            }
        }
        binding.temperatureView.onRectListener = {
            if (it.isNotEmpty()) {
                selectIndex = SelectPositionBean(it.first())
            }
        }

        binding.btnSelect.setOnClickListener(this)
        binding.btnStart.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            HikHelper.startStream()
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        super.onStop()
        if (!hasClickNext) {
            HikHelper.stopStream()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!hasClickNext) {
            HikHelper.release()
        }
    }

    override fun disConnected() {
        finish()
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.btnSelect -> {//开始拾取点、线、面
                showSelectDialog()
            }
            binding.btnStart -> {//跳转下一步
                if (selectIndex == null) {
                    showSelectDialog()
                } else {
                    hasClickNext = true
                    HikHelper.stopStream()
                    HikHelper.release()
                    val intent = Intent(this, IRMonitorCaptureHik2Activity::class.java)
                    intent.putExtra("select", selectIndex)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    /**
     * 显示点、线、面拾取弹窗.
     */
    private fun showSelectDialog() {
        MonitorSelectDialog.Builder(this)
            .setPositiveListener {
                binding.btnStart.isVisible = true
                binding.btnSelect.isVisible = false
                when (it) {
                    1 -> binding.temperatureView.mode = Mode.POINT
                    2 -> binding.temperatureView.mode = Mode.LINE
                    3 -> binding.temperatureView.mode = Mode.RECT
                }
            }
            .create().show()
    }
}