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

/**
 * Professional thermal imaging configuration input dialog for clinical and research applications.
 *
 * Provides comprehensive parameter input interface for thermal measurement correction including:
 * - Environmental temperature configuration with device-specific ranges (TC007: -10°C to 50°C, others: -10°C to 55°C)
 * - Test distance configuration with precision ranging (TC007: 0.2m to 4m, others: 0.2m to 5m)
 * - Material emissivity configuration with research-grade accuracy (TC007: 0.1 to 1.0, others: 0.01 to 1.0)
 *
 * This dialog implements professional thermal parameter input validation suitable for
 * research documentation and clinical thermal imaging measurement correction workflows.
 *
 * @constructor Creates thermal configuration input dialog
 * @param context The application context for resource access
 * @param type The configuration parameter type (TEMP, DIS, EM)
 * @param isTC007 Whether this is for TC007 device with specific parameter ranges
 */
class IRConfigInputDialog(context: Context, val type: Type, val isTC007: Boolean) : Dialog(context, R.style.TextInputDialog) {

    /** ViewBinding for the dialog layout */
    private lateinit var binding: DialogIrConfigInputBinding
    
    private var value: Float? = null
    private var onConfirmListener: ((value: Float) -> Unit)? = null

    /**
     * Sets professional default input value for thermal parameter configuration.
     *
     * @param value The default parameter value for initialization
     * @return This dialog instance for method chaining
     */
    fun setInput(value: Float?): IRConfigInputDialog {
        this.value = value
        return this
    }
    
    /**
     * Sets professional confirmation listener for thermal parameter validation.
     *
     * @param l The listener callback invoked with validated parameter value
     * @return This dialog instance for method chaining
     */
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

    /**
     * Configures professional parameter type-specific UI elements.
     */
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

    /**
     * Configures professional input field with default value and focus.
     */
    private fun setupInputField() {
        with(binding.etInput) {
            setText(if (value == null) "" else value.toString())
            setSelection(0, length())
            requestFocus()
        }
    }

    /**
     * Sets up professional event listeners for dialog interaction.
     */
    private fun setupEventListeners() {
        with(binding) {
            tvCancel.setOnClickListener { dismiss() }
            tvConfirm.setOnClickListener { handleConfirmClick() }
        }
    }

    /**
     * Handles professional parameter validation and confirmation.
     */
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

    /**
     * Configures professional dialog window dimensions for optimal usability.
     */
    private fun configureDialogWindow() {
        window?.let {
            val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * if (isPortrait) 0.73f else 0.48f).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }
    }

    /**
     * Professional thermal parameter configuration types for clinical and research applications.
     */
    enum class Type {
        /** Environmental temperature configuration for thermal measurement correction */
        TEMP,

        /** Test distance configuration for accurate thermal analysis */
        DIS,

        /** Material emissivity configuration for research-grade thermal measurement */
        EM,
    }
