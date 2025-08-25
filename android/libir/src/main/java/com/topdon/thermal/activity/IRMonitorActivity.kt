package com.topdon.thermal.activity

import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.ui.dialog.MonitorSelectDialog
import com.topdon.thermal.R
import com.topdon.thermal.bean.SelectPositionBean
import com.topdon.thermal.event.MonitorSaveEvent
import com.topdon.thermal.event.ThermalActionEvent
import com.topdon.thermal.databinding.ActivityIrMonitorBinding
import org.greenrobot.eventbus.EventBus

@Route(path = RouterConfig.IR_THERMAL_MONITOR)
class IRMonitorActivity : BaseActivity(), View.OnClickListener {

    private lateinit var binding: ActivityIrMonitorBinding

    private var selectIndex: SelectPositionBean? = null

    override fun initContentView(): Int {
        binding = ActivityIrMonitorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        return 0
    }

    override fun initView() {
        binding.motionBtn.setOnClickListener(this)
        binding.motionStartBtn.setOnClickListener(this)
    }

    override fun initData() {

    }

    override fun onClick(v: View?) {
        when (v) {
            binding.motionBtn -> {
                MonitorSelectDialog.Builder(this)
                    .setPositiveListener {
                        updateUI()
                        when (it) {
                            1 -> EventBus.getDefault().post(ThermalActionEvent(action = 2001))
                            2 -> EventBus.getDefault().post(ThermalActionEvent(action = 2002))
                            else -> EventBus.getDefault().post(ThermalActionEvent(action = 2003))
                        }
                    }
                    .create().show()
            }
            binding.motionStartBtn -> {
                if (selectIndex == null) {
                    MonitorSelectDialog.Builder(this)
                        .setPositiveListener {
                            updateUI()
                            when (it) {
                                1 -> EventBus.getDefault().post(ThermalActionEvent(action = 2001))
                                2 -> EventBus.getDefault().post(ThermalActionEvent(action = 2002))
                                else -> EventBus.getDefault().post(ThermalActionEvent(action = 2003))
                            }
                        }
                        .create().show()
                    return
                }

                ARouter.getInstance().build(RouterConfig.IR_MONITOR_CHART)
                    .withParcelable("select", selectIndex)
                    .navigation(this)
                finish()
            }
        }
    }

    fun select(selectIndex: SelectPositionBean?) {
        this.selectIndex = selectIndex
    }

    private fun updateUI() {
        binding.motionStartBtn.visibility = View.VISIBLE
        binding.motionBtn.visibility = View.GONE
    }

    override fun disConnected() {
        super.disConnected()
        finish()
    }
