package com.topdon.module.thermal.ir.activity

import android.content.Intent
import com.alibaba.android.arouter.facade.annotation.Route
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.databinding.ActivityManualStep1Binding
import com.topdon.module.thermal.ir.event.ManualFinishBean
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Dual-light calibration Step 1 with ViewBinding implementation.
 * 
 * Provides professional interface for thermal imaging dual-light calibration workflow
 * with comprehensive before/after visualization and research-grade documentation.
 * 
 * First step in the dual-light calibration process, demonstrating the improvement
 * achieved through proper calibration with visual comparison interface.
 * 
 * @author Topdon Thermal Imaging Team
 * @since 2023-12-29
 * @see ManualStep2Activity
 */
@Route(path = RouterConfig.MANUAL_START)
class ManualStep1Activity : BaseActivity() {

    private lateinit var binding: ActivityManualStep1Binding

    override fun initContentView(): Int = R.layout.activity_manual_step1

    override fun initView() {
        binding = ActivityManualStep1Binding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
    }

    /**
     * Configure click listeners for calibration workflow navigation.
     */
    private fun setupClickListeners() {
        binding.tvManual.setOnClickListener {
            startActivity(Intent(this, ManualStep2Activity::class.java))
        }
    }

    override fun initData() {
        // No initial data loading required for this step
    }

    /**
     * Handle device disconnection by terminating calibration process.
     */
    override fun disConnected() {
        super.disConnected()
        finish()
    }

    /**
     * Handle calibration completion event from subsequent steps.
     * 
     * @param manualFinishBean Event indicating calibration workflow completion
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onManualFinishBean(manualFinishBean: ManualFinishBean) {
        finish()
    }
}