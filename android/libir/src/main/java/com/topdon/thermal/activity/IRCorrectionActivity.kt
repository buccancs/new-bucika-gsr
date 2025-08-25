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

@Route(path = RouterConfig.IR_CORRECTION)
class IRCorrectionActivity : BaseActivity() {

    private lateinit var binding: ActivityIrCorrectionBinding

    override fun initContentView(): Int = R.layout.activity_ir_correction

    override fun initView() {
        binding = ActivityIrCorrectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupCorrectionButton()
    }

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

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun finishCorrection(event: CorrectionFinishEvent) {
        finish()
    }
