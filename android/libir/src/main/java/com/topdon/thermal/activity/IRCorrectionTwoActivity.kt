package com.topdon.thermal.activity

import android.content.Intent
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.tools.DeviceTools
import com.topdon.thermal.R
import com.topdon.thermal.event.CorrectionFinishEvent
import com.topdon.thermal.databinding.ActivityIrCorrectionTwoBinding
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Professional Lens Cap Correction Activity for Thermal IR Calibration
 * 
 * This activity provides the second step in the thermal imaging calibration workflow,
 * specifically for lens cap (pot cover) correction which is essential for accurate
 * temperature measurements in research and clinical applications.
 * 
 * **Calibration Process:**
 * - Guides users through proper lens cap placement for thermal calibration
 * - Supports both TC001 and TC007 device types with appropriate visual guidance
 * - Provides real-time connection status monitoring for device readiness
 * - Integrates with professional calibration workflow for research-grade accuracy
 * 
 * **Device Support:**
 * - TC001: Line-type thermal imaging devices with USB connectivity
 * - TC007: Wireless thermal imaging devices with socket connectivity
 * - Supports multiple TC001 variants (Lite, HIK) with device-specific routing
 * 
 * **Professional Features:**
 * - Real-time device connection monitoring with visual feedback
 * - Professional UI with device-specific calibration instructions
 * - Integration with comprehensive correction workflow system
 * - Research-grade calibration accuracy for clinical applications
 * 
 * **Required Parameters:**
 * - `ExtraKeyConfig.IS_TC007`: Boolean indicating TC007 device type (false for TC001)
 * 
 * @author BucikaGSR Development Team  
 * @since 2024.1.0
 * @see IRCorrectionThreeActivity For the next step in TC001 calibration workflow
 * @see CorrectionFinishEvent For calibration completion event handling
 */
@Route(path = RouterConfig.IR_CORRECTION_TWO)
class IRCorrectionTwoActivity : BaseActivity() {

    /**
     * ViewBinding instance for type-safe view access
     * Replaces deprecated Kotlin synthetics with modern binding pattern
     */
    private lateinit var binding: ActivityIrCorrectionTwoBinding

    /**
     * Device type flag indicating whether this is a TC007 wireless device
     * For BucikaGSR: Always false since only TC001 devices are supported
     * - true: TC007 wireless thermal imaging device
     * - false: TC001 line-type thermal imaging device (BucikaGSR default)
     */
    private var isTC007 = false

    override fun initContentView(): Int {
        binding = ActivityIrCorrectionTwoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        return 0 // ViewBinding handles layout inflation
    }

    override fun initView() {
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        binding.ivSketchMap.setImageResource(if (isTC007) R.drawable.ic_corrected_tc007 else R.drawable.ic_corrected_line)

        if (if (isTC007) WebSocketProxy.getInstance().isTC007Connect() else DeviceTools.isConnect()) {
            binding.tvCorrection.setBackgroundResource(R.drawable.bg_corners05_solid_theme)
        } else {
            binding.tvCorrection.setBackgroundResource(R.drawable.bg_corners05_solid_50_theme)
        }

        binding.tvCorrection.setOnClickListener {
            if (if (isTC007) WebSocketProxy.getInstance().isTC007Connect() else DeviceTools.isConnect()) {
                if (isTC007) {
                    ARouter.getInstance().build(RouterConfig.IR_CORRECTION_07).navigation(this)
                } else {
                    if (DeviceTools.isTC001LiteConnect()){
                        ARouter.getInstance().build(RouterConfig.IR_CORRECTION_THREE_LITE).navigation(this)
                    } else if (DeviceTools.isHikConnect()) {
                        ARouter.getInstance().build(RouterConfig.IR_HIK_CORRECT_THREE).navigation(this)
                    } else{
                        startActivity(Intent(this, IRCorrectionThreeActivity::class.java))
                    }
                }
            }
        }
    }


    override fun connected() {
        if (!isTC007) {
            binding.tvCorrection.setBackgroundResource(R.drawable.bg_corners05_solid_theme)
        }
    }

    override fun disConnected() {
        if (!isTC007) {
            binding.tvCorrection.setBackgroundResource(R.drawable.bg_corners05_solid_50_theme)
        }
    }

    override fun onSocketConnected(isTS004: Boolean) {
        if (isTC007 && !isTS004) {
            binding.tvCorrection.setBackgroundResource(R.drawable.bg_corners05_solid_theme)
        }
    }

    override fun onSocketDisConnected(isTS004: Boolean) {
        if (isTC007 && !isTS004) {
            binding.tvCorrection.setBackgroundResource(R.drawable.bg_corners05_solid_50_theme)
        }
    }

    override fun initData() {}

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun finishCorrection(event: CorrectionFinishEvent) {
        finish()
    }
}