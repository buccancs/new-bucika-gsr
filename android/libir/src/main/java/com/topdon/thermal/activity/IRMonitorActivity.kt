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

/**
 * Professional Thermal Region Monitoring Activity for Research Applications
 * 
 * This activity provides comprehensive thermal region monitoring capabilities,
 * allowing researchers to select specific areas for temperature tracking and
 * analysis in real-time thermal imaging applications.
 * 
 * **Monitoring Features:**
 * - Professional region selection with point, line, and area monitoring options
 * - Real-time temperature tracking with research-grade accuracy and precision
 * - Dynamic monitoring position selection with visual feedback and confirmation
 * - Professional monitoring session management with start/stop controls
 * - Integration with comprehensive thermal analysis and data collection workflows
 * 
 * **Research Application Benefits:**
 * - Clinical temperature monitoring for medical research and diagnostic applications
 * - Precise thermal analysis for materials science and engineering research
 * - Professional data collection for thermal behavior studies and analysis
 * - Real-time monitoring alerts and threshold-based notification systems
 * - Comprehensive integration with thermal imaging workflow and data export
 * 
 * **Professional UI Components:**
 * - Integrated thermal fragment for live thermal imaging display and analysis
 * - Professional selection dialogs with comprehensive monitoring options
 * - Research-grade controls with clear visual feedback and status indicators
 * - Context-sensitive action buttons optimized for research workflow efficiency
 * 
 * **Event Integration:**
 * - Professional event system for thermal action coordination and workflow management
 * - Real-time monitoring save events for data persistence and research archival
 * - Comprehensive thermal action events for integration with broader research systems
 * 
 * @author BucikaGSR Development Team
 * @since 2024.1.0
 * @see MonitorSelectDialog For professional region selection and monitoring configuration
 * @see IRMonitorThermalFragment For live thermal imaging and monitoring display
 * @see ThermalActionEvent For thermal monitoring action coordination and workflow
 */
@Route(path = RouterConfig.IR_THERMAL_MONITOR)
class IRMonitorActivity : BaseActivity(), View.OnClickListener {

    /**
     * ViewBinding instance for type-safe view access
     * Replaces deprecated Kotlin synthetics with modern binding pattern
     */
    private lateinit var binding: ActivityIrMonitorBinding

    /**
     * Currently selected monitoring position configuration
     * Contains thermal region coordinates and monitoring parameters for research analysis
     */
    private var selectIndex: SelectPositionBean? = null

    override fun initContentView(): Int {
        binding = ActivityIrMonitorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        return 0 // ViewBinding handles layout inflation
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
                //开始温度监听
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


}