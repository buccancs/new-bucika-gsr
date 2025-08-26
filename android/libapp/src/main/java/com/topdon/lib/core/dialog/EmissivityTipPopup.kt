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

import com.blankj.utilcode.util.SizeUtils
import com.topdon.lib.core.R
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.databinding.LayoutPopupTipEmissivityBinding
import com.topdon.lib.core.tools.NumberTools
import com.topdon.lib.core.tools.UnitTools

class EmissivityTipPopup(val context: Context, val isTC007: Boolean) {
    
    private var text: String = ""
    
    private var radiation: Float = 0f
    
    private var distance: Float = 0f
    
    private var environment: Float = 0f
    
    private var popupWindow: PopupWindow? = null
    
    private lateinit var binding: LayoutPopupTipEmissivityBinding
    
    private var titleText: TextView? = null
    
    private var messageText: TextView? = null
    
    private var checkBox: CheckBox? = null
    
    private var closeEvent: ((check: Boolean) -> Unit)? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = LayoutPopupTipEmissivityBinding.inflate(inflater)
    }

    fun setTitle(title: String): EmissivityTipPopup {
        titleText?.text = title
        return this
    }

    fun setMessage(message: String): EmissivityTipPopup {
        messageText?.text = message
        return this
    }

    fun setDataBean(environment: Float,distance : Float,radiation : Float,text : String): EmissivityTipPopup {
        this.environment = environment
        this.distance = distance
        this.radiation = radiation
        this.text = text
        return this
    }

    fun setCancelListener(event: ((check: Boolean) -> Unit)?): EmissivityTipPopup {
        this.closeEvent = event
        return this
    }

    fun build(): PopupWindow {
        if (popupWindow == null) {

            binding.tvEnvironmentTitle.text = context.getString(R.string.thermal_config_environment) + ":"
            binding.tvDistanceTitle.text = context.getString(R.string.thermal_config_distance) + ":"

            binding.tvTitle.visibility = View.GONE
            
            if (text.isNotEmpty()){
                binding.tvEmissivityMaterials.text = text
                binding.tvEmissivityMaterials.visibility = View.VISIBLE
            }else{
                binding.tvEmissivityMaterials.visibility = View.GONE
            }
            
            binding.dialogTipCancelBtn.visibility = View.GONE
            binding.dialogTipSuccessBtn.text = context.getString(R.string.tc_modify_params)
            binding.dialogTipCheck.visibility = View.GONE
            
            binding.tvEmissivity.text = "${context?.getString(R.string.thermal_config_radiation)}: ${
                NumberTools.to02(radiation)}"
            binding.tvEnvironmentValue.text = UnitTools.showC(environment)
            binding.tvDistanceValue.text = "${NumberTools.to02(distance)}m"
            
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
            
            binding.dialogTipSuccessBtn.setOnClickListener {

                dismiss()
            }
        }
        return popupWindow!!
    }

    fun show(anchorView: View) {
        popupWindow?.showAtLocation(anchorView, Gravity.CENTER, -SizeUtils.dp2px(10f), 0)
    }

    fun dismiss() {
        popupWindow?.dismiss()
        closeEvent?.invoke(checkBox?.isChecked ?: false)
    }
}
