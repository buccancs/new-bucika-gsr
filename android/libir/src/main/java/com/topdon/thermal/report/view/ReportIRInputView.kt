package com.topdon.thermal.report.view

import android.annotation.SuppressLint
import android.content.Context
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.util.AttributeSet
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import com.topdon.lib.core.tools.UnitTools
import com.topdon.thermal.R
import com.topdon.thermal.report.bean.ImageTempBean
import com.topdon.thermal.databinding.ViewReportIrInputBinding
import com.topdon.thermal.databinding.ItemReportIrInputBinding

/**
 * Professional thermal imaging report input view component for clinical and research applications.
 *
 * Provides comprehensive temperature measurement input interface supporting:
 * - Full-screen thermal analysis input with min/max temperature configuration
 * - Multi-point temperature measurement input (up to 5 points)
 * - Linear temperature profile input (up to 5 lines)
 * - Rectangular region temperature input (up to 5 areas)
 * - Professional measurement comment and explanation input
 *
 * This component implements industry-standard thermal measurement input patterns
 * suitable for research documentation and clinical thermal imaging workflows.
 *
 * @constructor Creates thermal IR input view with professional measurement capabilities
 * @param context The application context for resource access
 * @param attrs Optional XML attributes for type and index configuration
 * @param defStyleAttr Optional default style attributes
 */
class ReportIRInputView: LinearLayout {

    companion object {
        /** Temperature measurement type: Full screen thermal analysis */
        private const val TYPE_FULL = 0
        /** Temperature measurement type: Point measurement */
        private const val TYPE_POINT = 1
        /** Temperature measurement type: Linear temperature profile */
        private const val TYPE_LINE = 2
        /** Temperature measurement type: Rectangular region analysis */
        private const val TYPE_RECT = 3
    }

    /** ViewBinding for the main report IR input layout */
    private val binding: ViewReportIrInputBinding

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    @SuppressLint("SetTextI18n")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        binding = ViewReportIrInputBinding.inflate(
            android.view.LayoutInflater.from(context), this, true
        )

        // Configure explanation text input with professional limits
        val explainBinding = ItemReportIrInputBinding.bind(binding.clExplain)
        with(explainBinding) {
            etItem.inputType = InputType.TYPE_CLASS_TEXT
            etItem.filters = arrayOf(LengthFilter(150))
        }

        // Set up professional switch listeners for all measurement types
        with(binding) {
            val maxBinding = ItemReportIrInputBinding.bind(clMax)
            val minBinding = ItemReportIrInputBinding.bind(clMin)
            val averageBinding = ItemReportIrInputBinding.bind(clAverage)
            
            setSwitchListener(maxBinding.switchItem, maxBinding.etItem)
            setSwitchListener(minBinding.switchItem, minBinding.etItem)
            setSwitchListener(averageBinding.switchItem, averageBinding.etItem)
            setSwitchListener(explainBinding.switchItem, explainBinding.etItem)
        }

        // Parse custom attributes for measurement type and index
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.ReportIRInputView)
        val type = typeArray.getInt(R.styleable.ReportIRInputView_type, TYPE_FULL)
        val index = typeArray.getInt(R.styleable.ReportIRInputView_index, 0)
        typeArray.recycle()

        // Configure title visibility and separator lines
        with(binding) {
            clTitle.isVisible = index == 0
            viewLine.isVisible = index > 0

            setupMeasurementType(type, index)
        }
    }

    /**
     * Configures professional measurement type interface based on thermal analysis type.
     *
     * @param type The measurement type (full screen, point, line, rectangle)
     * @param index The measurement index within the type
     */
    private fun setupMeasurementType(type: Int, index: Int) {
        val maxBinding = ItemReportIrInputBinding.bind(binding.clMax)
        val minBinding = ItemReportIrInputBinding.bind(binding.clMin)
        val averageBinding = ItemReportIrInputBinding.bind(binding.clAverage)
        val explainBinding = ItemReportIrInputBinding.bind(binding.clExplain)

        when (type) {
            TYPE_FULL -> {
                binding.tvTitle.setText(R.string.thermal_full_rect)
                binding.clMin.isVisible = true
                binding.clAverage.isVisible = false
                maxBinding.tvItemName.text = context.getString(R.string.chart_temperature_high) + " (${UnitTools.showUnit()})"
                minBinding.tvItemName.text = context.getString(R.string.chart_temperature_low) + " (${UnitTools.showUnit()})"
                explainBinding.tvItemName.text = context.getString(R.string.album_report_comment)
            }
            TYPE_POINT -> {
                binding.tvTitle.text = context.getString(R.string.thermal_point) + "(P)"
                binding.clMin.isVisible = false
                binding.clAverage.isVisible = false
                maxBinding.tvItemName.text = "P${index + 1} " + context.getString(R.string.chart_temperature) + " (${UnitTools.showUnit()})"
                explainBinding.tvItemName.text = "P${index + 1} " + context.getString(R.string.album_report_comment)
            }
            TYPE_LINE -> {
                binding.tvTitle.text = context.getString(R.string.thermal_line) + "(L)"
                binding.clMin.isVisible = true
                binding.clAverage.isVisible = true
                maxBinding.tvItemName.text = "L${index + 1} " + context.getString(R.string.chart_temperature_high) + " (${UnitTools.showUnit()})"
                minBinding.tvItemName.text = "L${index + 1} " + context.getString(R.string.chart_temperature_low) + " (${UnitTools.showUnit()})"
                averageBinding.tvItemName.text = "L${index + 1} " + context.getString(R.string.album_report_mean_temperature) + " (${UnitTools.showUnit()})"
                explainBinding.tvItemName.text = "L${index + 1} " + context.getString(R.string.album_report_comment)
            }
            TYPE_RECT -> {
                binding.tvTitle.text = context.getString(R.string.thermal_rect) + "(R)"
                binding.clMin.isVisible = true
                binding.clAverage.isVisible = true
                maxBinding.tvItemName.text = "R${index + 1} " + context.getString(R.string.chart_temperature_high) + " (${UnitTools.showUnit()})"
                minBinding.tvItemName.text = "R${index + 1} " + context.getString(R.string.chart_temperature_low) + " (${UnitTools.showUnit()})"
                averageBinding.tvItemName.text = "R${index + 1} " + context.getString(R.string.album_report_mean_temperature) + " (${UnitTools.showUnit()})"
                explainBinding.tvItemName.text = "R${index + 1} " + context.getString(R.string.album_report_comment)
            }
        }
    }

    /**
     * Checks if the maximum temperature switch is enabled.
     * @return true if maximum temperature input is enabled
     */
    fun isSwitchMaxCheck(): Boolean {
        val maxBinding = ItemReportIrInputBinding.bind(binding.clMax)
        return maxBinding.switchItem.isChecked
    }

    /**
     * Checks if the minimum temperature switch is enabled.
     * @return true if minimum temperature input is enabled
     */
    fun isSwitchMinCheck(): Boolean {
        val minBinding = ItemReportIrInputBinding.bind(binding.clMin)
        return minBinding.switchItem.isChecked
    }

    /**
     * Checks if the average temperature switch is enabled.
     * @return true if average temperature input is enabled
     */
    fun isSwitchAverageCheck(): Boolean {
        val averageBinding = ItemReportIrInputBinding.bind(binding.clAverage)
        return averageBinding.switchItem.isChecked
    }

    /**
     * Checks if the explanation switch is enabled.
     * @return true if explanation input is enabled
     */
    fun isSwitchExplainCheck(): Boolean {
        val explainBinding = ItemReportIrInputBinding.bind(binding.clExplain)
        return explainBinding.switchItem.isChecked
    }

    /**
     * Gets the maximum temperature input value.
     * @return The entered maximum temperature as string
     */
    fun getMaxInput(): String {
        val maxBinding = ItemReportIrInputBinding.bind(binding.clMax)
        return maxBinding.etItem.text.toString()
    }

    /**
     * Gets the minimum temperature input value.
     * @return The entered minimum temperature as string
     */
    fun getMinInput(): String {
        val minBinding = ItemReportIrInputBinding.bind(binding.clMin)
        return minBinding.etItem.text.toString()
    }

    /**
     * Gets the average temperature input value.
     * @return The entered average temperature as string
     */
    fun getAverageInput(): String {
        val averageBinding = ItemReportIrInputBinding.bind(binding.clAverage)
        return averageBinding.etItem.text.toString()
    }

    /**
     * Gets the explanation input value.
     * @return The entered explanation text as string
     */
    fun getExplainInput(): String {
        val explainBinding = ItemReportIrInputBinding.bind(binding.clExplain)
        return explainBinding.etItem.text.toString()
    }

    /**
     * Refreshes professional temperature measurement data display.
     *
     * Updates all temperature input fields with unit-converted values suitable
     * for clinical and research documentation standards.
     *
     * @param tempBean The temperature measurement data to display
     */
    fun refreshData(tempBean: ImageTempBean.TempBean?) {
        val maxBinding = ItemReportIrInputBinding.bind(binding.clMax)
        val minBinding = ItemReportIrInputBinding.bind(binding.clMin)
        val averageBinding = ItemReportIrInputBinding.bind(binding.clAverage)
        val explainBinding = ItemReportIrInputBinding.bind(binding.clExplain)
        
        tempBean?.max?.let {
            maxBinding.etItem.setText(UnitTools.showUnitValue(it.toFloat())?.toString())
        }
        tempBean?.min?.let {
            minBinding.etItem.setText(UnitTools.showUnitValue(it.toFloat())?.toString())
        }
        tempBean?.average?.let {
            averageBinding.etItem.setText(UnitTools.showUnitValue(it.toFloat())?.toString())
        }
        explainBinding.etItem.setText("")
    }

    /**
     * Sets up professional switch listener for temperature input enabling/disabling.
     *
     * @param switchCompat The switch control for enabling input
     * @param editText The corresponding temperature input field
     */
    private fun setSwitchListener(switchCompat: SwitchCompat, editText: EditText) {
        switchCompat.setOnCheckedChangeListener { _, isChecked ->
            editText.isVisible = isChecked
        }
    }
}