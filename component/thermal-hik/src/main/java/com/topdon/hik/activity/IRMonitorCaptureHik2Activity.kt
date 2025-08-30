package com.topdon.hik.activity

import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.energy.iruvc.sdkisp.LibIRTemp
import com.energy.iruvc.utils.Line
import com.topdon.hik.R
import com.topdon.hik.databinding.ActivityIrMonitorCaptureHik2Binding
import com.topdon.lib.core.common.SaveSettingUtil
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.db.AppDatabase
import com.topdon.lib.core.db.entity.ThermalEntity
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.ktbase.BaseBindingActivity
import com.topdon.lib.core.tools.NumberTools
import com.topdon.lib.core.tools.TimeTool
import com.topdon.libhik.util.HikHelper
import com.topdon.module.thermal.ir.bean.SelectPositionBean
import com.topdon.module.thermal.ir.event.MonitorSaveEvent
import com.topdon.module.thermal.ir.view.TemperatureBaseView.Mode
import com.topdon.module.thermal.ir.view.TemperatureHikView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

/**
 * 温度监控生成第2步 - 捕获.
 *
 * Created by LCG on 2025/1/10.
 */
class IRMonitorCaptureHik2Activity : BaseBindingActivity<ActivityIrMonitorCaptureHik2Binding>() {

    /**
     * 从上一界面传递过来的，当前选中的 点/线/面 信息.
     */
    private var selectBean = SelectPositionBean()

    override fun initContentLayoutId(): Int = R.layout.activity_ir_monitor_capture_hik2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectBean = intent.getParcelableExtra("select") ?: SelectPositionBean()

        HikHelper.bind(this)
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
        //热成像机芯在上一步已经初始化过了，这里不用再搞一遍

        binding.titleView.setRightClickListener {
            finish()
        }

        binding.monitorCurrentVol.setText(if (selectBean.type == 1) R.string.chart_temperature else R.string.chart_temperature_high)
        binding.monitorRealVol.isVisible = selectBean.type != 1
        binding.monitorRealImg.isVisible = selectBean.type != 1

        binding.temperatureView.mode = Mode.CLEAR
        binding.temperatureView.tempTextSize = SaveSettingUtil.tempTextSize
        binding.temperatureView.onTempResultListener = {
            saveOneRecord(it)
        }
        when (selectBean.type) {
            1 -> {
                binding.temperatureView.mode = Mode.POINT
                binding.temperatureView.addSourcePoint(selectBean.startPosition)
            }
            2 -> {
                binding.temperatureView.mode = Mode.LINE
                binding.temperatureView.addSourceLine(Line(selectBean.startPosition, selectBean.endPosition))
            }
            3 -> {
                binding.temperatureView.mode = Mode.RECT
                binding.temperatureView.addSourceRect(selectBean.getRect())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding.mpChartView.highlightValue(null) //关闭高亮点Marker
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun disConnected() {
        finish()
    }

    override fun finish() {
        super.finish()
        EventBus.getDefault().post(MonitorSaveEvent())
    }

    /**
     * 开始记录的时间戳
     */
    private var startTime: Long = 0L
    /**
     * 未使用的字段，为兼容旧版本，继续写入吧
     */
    private var thermalId: String = ""
    /**
     * 记录一个温度数据到数据库.
     */
    private fun saveOneRecord(tempInfo: TemperatureHikView.TempInfo) {
        lifecycleScope.launch(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            if (startTime == 0L) {
                startTime = currentTime
            }
            if (thermalId.isEmpty()) {
                thermalId = TimeTool.showDateSecond()
            }
            val typeStr = when (selectBean.type) {
                1 -> "point"
                2 -> "line"
                else -> "fence"
            }
            val result: LibIRTemp.TemperatureSampleResult = when (selectBean.type) {
                1 -> if (tempInfo.pointResults.isEmpty()) null else tempInfo.pointResults.first()
                2 -> if (tempInfo.lineResults.isEmpty()) null else tempInfo.lineResults.first()
                else -> if (tempInfo.rectResults.isEmpty()) null else tempInfo.rectResults.first()
            } ?: return@launch

            val entity = ThermalEntity()
            entity.userId = SharedManager.getUserId()
            entity.thermalId = thermalId
            entity.thermal = NumberTools.to02f(result.maxTemperature)
            entity.thermalMax = NumberTools.to02f(result.maxTemperature)
            entity.thermalMin = NumberTools.to02f(result.minTemperature)
            entity.type = typeStr
            entity.startTime = startTime
            entity.createTime = currentTime
            AppDatabase.getInstance().thermalDao().insert(entity)

            lifecycleScope.launch(Dispatchers.Main) {
                binding.mpChartView.addPointToChart(bean = entity, selectType = selectBean.type)
                binding.tvTime.text = TimeTool.showVideoLongTime(currentTime - startTime)
            }
        }
    }
}