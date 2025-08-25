package com.topdon.thermal.activity

import android.content.Intent
import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.thermal.R
import com.topdon.thermal.databinding.ActivityIrCorrectionThreeBinding
import com.topdon.thermal.fragment.IRCorrectionFragment

@Route(path = RouterConfig.IR_CORRECTION_THREE)
class IRCorrectionThreeActivity : BaseActivity() {

    private lateinit var binding: ActivityIrCorrectionThreeBinding
    private lateinit var correctionFragment: IRCorrectionFragment

    override fun initContentView(): Int = R.layout.activity_ir_correction_three

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIrCorrectionThreeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupCorrectionFragment(savedInstanceState)
        setupCorrectionButton()
    }

    private fun setupCorrectionFragment(savedInstanceState: Bundle?) {
        correctionFragment = if (savedInstanceState == null) {
            IRCorrectionFragment()
        } else {
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as IRCorrectionFragment
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container_view, correctionFragment)
                .commit()
        }
    }

    private fun setupCorrectionButton() {
        binding.tvCorrection.setOnClickListener {
            if (correctionFragment.frameReady) {
                val intent = Intent(this, IRCorrectionFourActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun initView() {

    }

    override fun initData() {

    }
