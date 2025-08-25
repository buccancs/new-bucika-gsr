package com.topdon.thermal.activity

import android.content.Intent
import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.thermal.R
import com.topdon.thermal.databinding.ActivityIrCorrectionThreeBinding
import com.topdon.thermal.fragment.IRCorrectionFragment

/**
 * Lens cap correction Step 3 with ViewBinding implementation.
 * 
 * Provides professional interface for thermal imaging correction workflow
 * with real-time frame readiness validation and fragment-based correction display.
 * 
 * Features include:
 * - IRCorrectionFragment integration for live correction preview
 * - Frame readiness validation before proceeding
 * - Professional correction workflow navigation
 * - Research-grade error handling and state management
 * 
 * @author CaiSongL
 * @since 2023-08-04
 * @see IRCorrectionFragment
 * @see IRCorrectionFourActivity
 */
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

    /**
     * Initialize correction fragment with proper state management.
     * 
     * @param savedInstanceState Bundle containing saved fragment state
     */
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

    /**
     * Configure correction workflow advancement button with frame validation.
     */
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
        // Fragment-based initialization handled in onCreate
    }

    override fun initData() {
        // No additional data initialization required
    }
}