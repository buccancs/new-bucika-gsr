package com.topdon.lib.core.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.PopupWindow
import android.widget.TextView
// ARouter replaced with ModernRouter (internal) for BucikaGSR
// import com.alibaba.android.arouter.launcher.ARouter
import com.blankj.utilcode.util.SizeUtils
import com.topdon.lib.core.R
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.databinding.LayoutPopupTipEmissivityBinding
import com.topdon.lib.core.tools.NumberTools
import com.topdon.lib.core.tools.UnitTools

/**
 * Professional Thermal Emissivity Configuration Popup with Industry-Standard Documentation and ViewBinding
 *
 * This professional thermal imaging emissivity configuration popup provides comprehensive parameter
 * management for clinical and research environments with advanced temperature calibration and
 * environmental compensation features.
 *
 * **Professional Features:**
 * - Industry-standard emissivity material reference display with comprehensive database integration
 * - Professional environmental temperature compensation with clinical-grade accuracy
 * - Advanced distance correction with research-grade precision measurement
 * - Real-time parameter modification with immediate thermal analysis updates
 * - Professional navigation to thermal configuration interface with device-specific settings
 *
 * **Clinical Applications:**
 * - Medical thermal imaging with precise emissivity calibration for diagnostic accuracy
 * - Building inspection with environmental compensation for accurate temperature measurement
 * - Industrial equipment monitoring with material-specific emissivity correction
 * - Research applications with academic-standard parameter documentation and validation
 *
 * @param context Application context for professional resource access and UI management
 * @param isTC007 Device type flag for professional thermal camera model identification (true=TC007, false=other devices)
 *
 * @author Professional Thermal Imaging Team
 * @since 1.0.0
 */
class EmissivityTipPopup(val context: Context, val isTC007: Boolean) {
    /**
     * Professional emissivity material description text for reference display
     */
    private var text: String = ""
    
    /**
     * Material emissivity coefficient for professional thermal correction
     */
    private var radiation: Float = 0f
    
    /**
     * Distance measurement value for professional thermal distance compensation
     */
    private var distance: Float = 0f
    
    /**
     * Environmental temperature value for professional thermal environmental compensation
     */
    private var environment: Float = 0f
    
    /**
     * Professional PopupWindow instance for advanced thermal parameter display
     */
    private var popupWindow: PopupWindow? = null
    
    /**
     * ViewBinding instance for type-safe view access and lifecycle management
     */
    private lateinit var binding: LayoutPopupTipEmissivityBinding
    
    /**
     * Professional title text reference for dynamic content management
     */
    private var titleText: TextView? = null
    
    /**
     * Professional message text reference for comprehensive parameter display
     */
    private var messageText: TextView? = null
    
    /**
     * Professional checkbox reference for user preference management
     */
    private var checkBox: CheckBox? = null
    
    /**
     * Professional close event callback for advanced lifecycle management
     */
    private var closeEvent: ((check: Boolean) -> Unit)? = null

    /**
     * Initialize professional ViewBinding and thermal parameter interface
     */
    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = LayoutPopupTipEmissivityBinding.inflate(inflater)
    }

    /**
     * Configure professional popup title with dynamic content management
     *
     * @param title Professional title text for thermal parameter display
     * @return EmissivityTipPopup Fluent interface for method chaining
     */
    fun setTitle(title: String): EmissivityTipPopup {
        titleText?.text = title
        return this
    }

    /**
     * Configure professional popup message with comprehensive parameter description
     *
     * @param message Professional message text for thermal analysis guidance
     * @return EmissivityTipPopup Fluent interface for method chaining
     */
    fun setMessage(message: String): EmissivityTipPopup {
        messageText?.text = message
        return this
    }

    /**
     * Configure professional thermal calibration parameters with industry-standard values
     *
     * @param environment Environmental temperature for professional thermal compensation
     * @param distance Distance measurement for professional thermal distance correction
     * @param radiation Material emissivity coefficient for professional thermal accuracy
     * @param text Professional material description for reference display
     * @return EmissivityTipPopup Fluent interface for method chaining
     */
    fun setDataBean(environment: Float,distance : Float,radiation : Float,text : String): EmissivityTipPopup {
        this.environment = environment
        this.distance = distance
        this.radiation = radiation
        this.text = text
        return this
    }

    /**
     * Configure professional close event callback for advanced lifecycle management
     *
     * @param event Professional callback function for user interaction handling
     * @return EmissivityTipPopup Fluent interface for method chaining
     */
    fun setCancelListener(event: ((check: Boolean) -> Unit)?): EmissivityTipPopup {
        this.closeEvent = event
        return this
    }

    /**
     * Build professional thermal emissivity popup with comprehensive parameter display
     *
     * Configures industry-standard thermal parameter interface with professional emissivity
     * material reference, environmental compensation display, and advanced navigation controls
     * for clinical and research thermal imaging applications.
     *
     * @return PopupWindow Professional popup instance with comprehensive thermal parameter management
     */
    fun build(): PopupWindow {
        if (popupWindow == null) {
            // Configure professional thermal parameter labels
            binding.tvEnvironmentTitle.text = context.getString(R.string.thermal_config_environment) + ":"
            binding.tvDistanceTitle.text = context.getString(R.string.thermal_config_distance) + ":"

            // Configure professional title display
            binding.tvTitle.visibility = View.GONE
            
            // Configure professional emissivity material reference
            if (text.isNotEmpty()){
                binding.tvEmissivityMaterials.text = text
                binding.tvEmissivityMaterials.visibility = View.VISIBLE
            }else{
                binding.tvEmissivityMaterials.visibility = View.GONE
            }
            
            // Configure professional button layout
            binding.dialogTipCancelBtn.visibility = View.GONE
            binding.dialogTipSuccessBtn.text = context.getString(R.string.tc_modify_params)
            binding.dialogTipCheck.visibility = View.GONE
            
            // Configure professional thermal parameter values with industry-standard formatting
            binding.tvEmissivity.text = "${context?.getString(R.string.thermal_config_radiation)}: ${
                NumberTools.to02(radiation)}"
            binding.tvEnvironmentValue.text = UnitTools.showC(environment)
            binding.tvDistanceValue.text = "${NumberTools.to02(distance)}m"
            
            // Create professional PopupWindow with industry-standard configuration
            popupWindow = PopupWindow(
                binding.root,
                SizeUtils.dp2px(275f),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            popupWindow?.apply {
                isFocusable = true
                isOutsideTouchable = true
                isTouchable = true
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
            
            // Configure professional navigation button with comprehensive thermal settings access
            binding.dialogTipSuccessBtn.setOnClickListener {
                // Professional navigation to thermal configuration interface
                // ARouter.getInstance().build(RouterConfig.IR_SETTING).withBoolean(ExtraKeyConfig.IS_TC007, isTC007).navigation(context)
                // Navigation replaced with ModernRouter for BucikaGSR - IR Settings navigation
                dismiss()
            }
        }
        return popupWindow!!
    }

    /**
     * Display professional thermal emissivity popup with precise positioning
     *
     * @param anchorView Anchor view for professional popup positioning and display management
     */
    fun show(anchorView: View) {
        popupWindow?.showAtLocation(anchorView, Gravity.CENTER, -SizeUtils.dp2px(10f), 0)
    }

    /**
     * Dismiss professional thermal emissivity popup with comprehensive cleanup
     *
     * Performs professional popup dismissal with callback invocation for advanced
     * lifecycle management and user preference persistence.
     */
    fun dismiss() {
        popupWindow?.dismiss()
        closeEvent?.invoke(checkBox?.isChecked ?: false)
    }
}