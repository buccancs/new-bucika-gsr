package com.topdon.thermal.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.topdon.lib.core.tools.UnitTools
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.thermal.R
import com.topdon.thermal.databinding.DialogIrConfigInputBinding
import java.lang.NumberFormatException

class IRConfigInputDialog(context: Context, val type: Type, val isTC007: Boolean) : Dialog(context, R.style.TextInputDialog) {

    private lateinit var binding: DialogIrConfigInputBinding
    
    private var value: Float? = null
    private var onConfirmListener: ((value: Float) -> Unit)? = null

    fun setInput(value: Float?): IRConfigInputDialog {
        this.value = value
        return this
    }
    
    fun setConfirmListener(l: (value: Float) -> Unit): IRConfigInputDialog {
        this.onConfirmListener = l
        return this
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        binding = DialogIrConfigInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupParameterType()
        setupInputField()
        setupEventListeners()
        configureDialogWindow()
    }

    private fun setupParameterType() {
        with(binding) {
            when (type) {
                Type.TEMP -> {
                    tvTitle.text = "${context.getString(R.string.thermal_config_environment)} ${UnitTools.showConfigC(-10, if (isTC007) 50 else 55)}"
                    tvUnit.text = UnitTools.showUnit()
                    tvUnit.isVisible = true
                }
                Type.DIS -> {
                    tvTitle.text = "${context.getString(R.string.thermal_config_distance)} (0.2~${if (isTC007) 4 else 5}m)"
                    tvUnit.text = "m"
                    tvUnit.isVisible = true
                }
                Type.EM -> {
                    tvTitle.text = "${context.getString(R.string.thermal_config_radiation)} (${if (isTC007) "0.1" else "0.01"}~1.00)"
                    tvUnit.text = ""
                    tvUnit.isVisible = false
                }
            }
        }
    }

    private fun setupInputField() {
        with(binding.etInput) {
            setText(if (value == null) "" else value.toString())
            setSelection(0, length())
            requestFocus()
        }
    }

    private fun setupEventListeners() {
        with(binding) {
            tvCancel.setOnClickListener { dismiss() }
            tvConfirm.setOnClickListener { handleConfirmClick() }
        }
    }

    private fun handleConfirmClick() {
        try {
            val input: Float = binding.etInput.text.toString().toFloat()
            val isRight = when (type) {
                Type.TEMP -> input in UnitTools.showUnitValue(-10f) .. UnitTools.showUnitValue(if (isTC007) 50f else 55f)
                Type.DIS -> input in 0.2f .. if (isTC007) 4f else 5f
                Type.EM -> input in (if (isTC007) 0.1f else 0.01f) .. 1f
            }
            if (isRight) {
                dismiss()
                onConfirmListener?.invoke(input)
            } else {
                TToast.shortToast(context, R.string.tip_input_format)
            }
        } catch (e: NumberFormatException) {
            TToast.shortToast(context, R.string.tip_input_format)
        }
    }

    private fun configureDialogWindow() {
        window?.let {
            val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * if (isPortrait) 0.73f else 0.48f).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }
    }

    enum class Type {
        
        TEMP,

        DIS,

        EM,
    }
