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

class ReportIRInputView: LinearLayout {

    companion object {
        
        private const val TYPE_FULL = 0
        
        private const val TYPE_POINT = 1
        
        private const val TYPE_LINE = 2
        
        private const val TYPE_RECT = 3
    }

    private val binding: ViewReportIrInputBinding

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    @SuppressLint("SetTextI18n")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        binding = ViewReportIrInputBinding.inflate(
            android.view.LayoutInflater.from(context), this, true
        )

        val explainBinding = ItemReportIrInputBinding.bind(binding.clExplain)
        with(explainBinding) {
            etItem.inputType = InputType.TYPE_CLASS_TEXT
            etItem.filters = arrayOf(LengthFilter(150))
        }

        with(binding) {
            val maxBinding = ItemReportIrInputBinding.bind(clMax)
            val minBinding = ItemReportIrInputBinding.bind(clMin)
            val averageBinding = ItemReportIrInputBinding.bind(clAverage)
            
            setSwitchListener(maxBinding.switchItem, maxBinding.etItem)
            setSwitchListener(minBinding.switchItem, minBinding.etItem)
            setSwitchListener(averageBinding.switchItem, averageBinding.etItem)
            setSwitchListener(explainBinding.switchItem, explainBinding.etItem)
        }

        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.ReportIRInputView)
        val type = typeArray.getInt(R.styleable.ReportIRInputView_type, TYPE_FULL)
        val index = typeArray.getInt(R.styleable.ReportIRInputView_index, 0)
        typeArray.recycle()

        with(binding) {
            clTitle.isVisible = index == 0
            viewLine.isVisible = index > 0

            setupMeasurementType(type, index)
        }
    }

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

    fun isSwitchMaxCheck(): Boolean {
        val maxBinding = ItemReportIrInputBinding.bind(binding.clMax)
        return maxBinding.switchItem.isChecked
    }

    fun isSwitchMinCheck(): Boolean {
        val minBinding = ItemReportIrInputBinding.bind(binding.clMin)
        return minBinding.switchItem.isChecked
    }

    fun isSwitchAverageCheck(): Boolean {
        val averageBinding = ItemReportIrInputBinding.bind(binding.clAverage)
        return averageBinding.switchItem.isChecked
    }

    fun isSwitchExplainCheck(): Boolean {
        val explainBinding = ItemReportIrInputBinding.bind(binding.clExplain)
        return explainBinding.switchItem.isChecked
    }

    fun getMaxInput(): String {
        val maxBinding = ItemReportIrInputBinding.bind(binding.clMax)
        return maxBinding.etItem.text.toString()
    }

    fun getMinInput(): String {
        val minBinding = ItemReportIrInputBinding.bind(binding.clMin)
        return minBinding.etItem.text.toString()
    }

    fun getAverageInput(): String {
        val averageBinding = ItemReportIrInputBinding.bind(binding.clAverage)
        return averageBinding.etItem.text.toString()
    }

    fun getExplainInput(): String {
        val explainBinding = ItemReportIrInputBinding.bind(binding.clExplain)
        return explainBinding.etItem.text.toString()
    }

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

    private fun setSwitchListener(switchCompat: SwitchCompat, editText: EditText) {
        switchCompat.setOnCheckedChangeListener { _, isChecked ->
            editText.isVisible = isChecked
        }
    }
