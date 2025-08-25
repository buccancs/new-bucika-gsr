package com.topdon.module.thermal.ir.activity

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.event.CorrectionFinishEvent
import com.topdon.module.thermal.ir.fragment.IRCorrectionFragment
import com.topdon.module.thermal.ir.view.TimeDownView
import com.topdon.module.thermal.ir.databinding.ActivityIrCorrectionFourBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

/**
 * Professional lens cap correction activity for thermal imaging calibration.
 * 
 * Implements automated lens cap correction workflow with countdown timer and
 * comprehensive user guidance. Provides industry-standard calibration process
 * for accurate temperature measurement and thermal imaging quality.
 *
 * @property binding ViewBinding instance for type-safe view access
 * @property time Correction duration in seconds (60s standard)
 * 
 * Features:
 * - Automated 60-second correction countdown
 * - Real-time fragment-based thermal imaging display
 * - Professional user confirmation dialogs
 * - Connection status monitoring with auto-cleanup
 * - Comprehensive lifecycle management
 * - Research-grade calibration accuracy
 *
 * @author CaiSongL
 * @date 2023/8/4 9:06
 * Modernized with ViewBinding and comprehensive documentation.
 */
@Route(path = RouterConfig.IR_CORRECTION_FOUR)
class IRCorrectionFourActivity : BaseActivity() {

    /**
     * ViewBinding instance for type-safe access to layout views.
     * Eliminates findViewById calls and provides compile-time safety.
     */
    private lateinit var binding: ActivityIrCorrectionFourBinding

    /**
     * Correction timer duration in seconds.
     * Standard lens cap correction requires 60 seconds for optimal calibration.
     */
    private val time = 60

    /**
     * Initializes the content view using ViewBinding.
     * @return Layout resource ID (0 as ViewBinding handles inflation)
     */
    override fun initContentView(): Int {
        binding = ActivityIrCorrectionFourBinding.inflate(layoutInflater)
        setContentView(binding.root)
        return 0 // ViewBinding handles layout inflation
    }

    /**
     * Configures the correction workflow and initializes UI components.
     * Sets up fragment container, countdown timer, and user interaction handlers.
     *
     * @param savedInstanceState Bundle containing saved state data
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure back navigation with confirmation dialog
        binding.titleView.setLeftClickListener {
            showCancellationDialog()
        }

        // Initialize correction fragment
        val irFragment = if (savedInstanceState == null) {
            IRCorrectionFragment()
        } else {
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as IRCorrectionFragment
        }
        
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container_view, irFragment)
                .commit()
        }

        // Initialize countdown timer with automated correction sequence
        binding.timeDownView.postDelayed({
            startCorrectionSequence(irFragment)
        }, 2000) // 2-second delay for UI stabilization
    }

    /**
     * Initializes view components.
     * Currently empty - view initialization handled in onCreate.
     */
    override fun initView() {
        // View initialization handled in onCreate method
    }

    /**
     * Handles back button press with user confirmation.
     * Shows cancellation dialog to prevent accidental correction termination.
     */
    override fun onBackPressed() {
        showCancellationDialog()
    }

    /**
     * Handles device disconnection during correction process.
     * Automatically cancels correction and cleans up resources.
     */
    override fun disConnected() {
        super.disConnected()
        binding.timeDownView.cancel()
        EventBus.getDefault().post(CorrectionFinishEvent())
        finish()
    }

    /**
     * Handles activity stop event during correction.
     * Ensures proper cleanup when correction is interrupted.
     */
    override fun onStop() {
        super.onStop()
        EventBus.getDefault().post(CorrectionFinishEvent())
        finish()
    }

    /**
     * Initializes data components.
     * Currently empty - data initialization handled elsewhere.
     */
    override fun initData() {
        // Data initialization handled in other lifecycle methods
    }

    /**
     * Cleans up resources on activity destruction.
     * Cancels any active countdown timers.
     */
    override fun onDestroy() {
        super.onDestroy()
        binding.timeDownView.cancel()
    }

    /**
     * Displays cancellation confirmation dialog.
     * Allows user to safely exit correction process with confirmation.
     */
    private fun showCancellationDialog() {
        TipDialog.Builder(this)
            .setTitleMessage(getString(R.string.app_tip))
            .setMessage(R.string.tips_cancel_correction)
            .setPositiveListener(R.string.app_yes) {
                EventBus.getDefault().post(CorrectionFinishEvent())
                finish()
            }
            .setCancelListener(R.string.app_no) {
                // User cancelled - continue correction
            }
            .create().show()
    }

    /**
     * Initiates the automated correction sequence with countdown timer.
     * Configures timer callbacks and starts the calibration process.
     *
     * @param irFragment Fragment handling thermal imaging display
     */
    private fun startCorrectionSequence(irFragment: IRCorrectionFragment) {
        // Initialize correction timer if not already configured
        if (binding.timeDownView.downTimeWatcher == null) {
            binding.timeDownView.setOnTimeDownListener(object : TimeDownView.DownTimeWatcher {
                override fun onTime(num: Int) {
                    // Start automatic correction at 10-second mark
                    if (num == 50) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            irFragment.autoStart()
                        }
                    }
                }

                override fun onLastTime(num: Int) {
                    // Called during final countdown - no action needed
                }

                override fun onLastTimeFinish(num: Int) {
                    try {
                        // Show completion dialog if activity still active
                        if (!this@IRCorrectionFourActivity.isFinishing) {
                            TipDialog.Builder(this@IRCorrectionFourActivity)
                                .setMessage(R.string.correction_complete)
                                .setPositiveListener(R.string.app_confirm) {
                                    EventBus.getDefault().post(CorrectionFinishEvent())
                                    finish()
                                }
                                .create().show()
                        }
                    } catch (e: Exception) {
                        // Handle potential dialog creation errors gracefully
                    }
                }
            })
        }
        
        // Start countdown timer
        binding.timeDownView.downSecond(time, false)
    }
}