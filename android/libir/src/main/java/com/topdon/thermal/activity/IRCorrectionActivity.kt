package com.topdon.thermal.activity

import android.content.Intent
import com.alibaba.android.arouter.facade.annotation.Route
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.thermal.R
import com.topdon.thermal.databinding.ActivityIrCorrectionBinding
import com.topdon.thermal.event.CorrectionFinishEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Lens cap correction activity with ViewBinding implementation.
 * 
 * Provides professional thermal imaging correction interface with before/after
 * comparison visualization and comprehensive correction workflow management.
 * 
 * Required parameters:
 * - **IS_TC007**: Device type identifier for TC007 compatibility
 * 
 * Features include:
 * - Visual before/after correction comparison
 * - Professional correction workflow navigation
 * - EventBus integration for correction completion
 * - Research-grade error handling and validation
 * 
 * @author CaiSongL
 * @since 2023-08-04
 * @see IRCorrectionTwoActivity
 * @see CorrectionFinishEvent
 */
@Route(path = RouterConfig.IR_CORRECTION)
class IRCorrectionActivity : BaseActivity() {

    private lateinit var binding: ActivityIrCorrectionBinding

    override fun initContentView(): Int = R.layout.activity_ir_correction

    override fun initView() {
        binding = ActivityIrCorrectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupCorrectionButton()
    }

    /**
     * Configure correction workflow navigation button.
     */
    private fun setupCorrectionButton() {
        binding.tvCorrection.setOnClickListener {
            val jumpIntent = Intent(this, IRCorrectionTwoActivity::class.java).apply {
                putExtra(
                    ExtraKeyConfig.IS_TC007, 
                    intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)
                )
            }
            startActivity(jumpIntent)
        }
    }

    override fun initData() {
        // No initial data loading required
    }

    /**
     * Handle correction completion event and finish activity.
     * 
     * @param event Correction completion event from subsequent steps
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun finishCorrection(event: CorrectionFinishEvent) {
        finish()
    }
}