package com.topdon.thermal.activity

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.thermal.R
import com.topdon.thermal.event.CorrectionFinishEvent
import com.topdon.thermal.fragment.IRCorrectionFragment
import com.topdon.thermal.view.TimeDownView
import com.topdon.thermal.databinding.ActivityIrCorrectionFourBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

@Route(path = RouterConfig.IR_CORRECTION_FOUR)
class IRCorrectionFourActivity : BaseActivity() {

    private lateinit var binding: ActivityIrCorrectionFourBinding

    private val time = 60

    override fun initContentView(): Int {
        binding = ActivityIrCorrectionFourBinding.inflate(layoutInflater)
        setContentView(binding.root)
        return 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding.titleView.setLeftClickListener {
            showCancellationDialog()
        }

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

        binding.timeDownView.postDelayed({
            startCorrectionSequence(irFragment)
        }, 2000)
    }

    override fun initView() {

    }

    override fun onBackPressed() {
        showCancellationDialog()
    }

    override fun disConnected() {
        super.disConnected()
        binding.timeDownView.cancel()
        EventBus.getDefault().post(CorrectionFinishEvent())
        finish()
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().post(CorrectionFinishEvent())
        finish()
    }

    override fun initData() {

    }

    override fun onDestroy() {
        super.onDestroy()
        binding.timeDownView.cancel()
    }

    private fun showCancellationDialog() {
        TipDialog.Builder(this)
            .setTitleMessage(getString(R.string.app_tip))
            .setMessage(R.string.tips_cancel_correction)
            .setPositiveListener(R.string.app_yes) {
                EventBus.getDefault().post(CorrectionFinishEvent())
                finish()
            }
            .setCancelListener(R.string.app_no) {

            }
            .create().show()
    }

    private fun startCorrectionSequence(irFragment: IRCorrectionFragment) {

        if (binding.timeDownView.downTimeWatcher == null) {
            binding.timeDownView.setOnTimeDownListener(object : TimeDownView.DownTimeWatcher {
                override fun onTime(num: Int) {

                    if (num == 50) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            irFragment.autoStart()
                        }
                    }
                }

                override fun onLastTime(num: Int) {

                }

                override fun onLastTimeFinish(num: Int) {
                    try {

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

                    }
                }
            })
        }
        
        binding.timeDownView.downSecond(time, false)
    }
}
