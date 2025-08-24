package com.topdon.module.thermal.ir.report.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.blankj.utilcode.util.SizeUtils
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.report.bean.ReportIRBean
import com.topdon.module.thermal.ir.report.bean.ReportTempBean
import com.topdon.module.thermal.ir.databinding.ViewReportIrShowBinding
import com.topdon.module.thermal.ir.databinding.ItemReportIrShowBinding

/**
 * Professional thermal imaging report IR data preview view component for clinical and research applications.
 *
 * Provides comprehensive temperature measurement visualization supporting:
 * - Full-screen thermal analysis with min/max temperature detection
 * - Multi-point temperature measurement (up to 5 points)
 * - Linear temperature profile analysis (up to 5 lines)
 * - Rectangular region temperature analysis (up to 5 areas)
 * - Industry-standard PDF report generation compatibility
 *
 * This component implements professional thermal data visualization patterns
 * suitable for research documentation and clinical thermal imaging workflows.
 *
 * @constructor Creates thermal IR show view with professional measurement capabilities
 * @param context The application context for resource access
 * @param attrs Optional XML attributes for customization
 * @param defStyleAttr Optional default style attributes
 */
class ReportIRShowView: LinearLayout {
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

    /** ViewBinding for the main report IR show layout */
    private val binding: ViewReportIrShowBinding

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        binding = ViewReportIrShowBinding.inflate(
            android.view.LayoutInflater.from(context), this, true
        )

        with(binding) {
            initTitleText(clFull, TYPE_FULL, 0)

            initTitleText(clPoint1, TYPE_POINT, 0)
            initTitleText(clPoint2, TYPE_POINT, 1)
            initTitleText(clPoint3, TYPE_POINT, 2)
            initTitleText(clPoint4, TYPE_POINT, 3)
            initTitleText(clPoint5, TYPE_POINT, 4)

            initTitleText(clLine1, TYPE_LINE, 0)
            initTitleText(clLine2, TYPE_LINE, 1)
            initTitleText(clLine3, TYPE_LINE, 2)
            initTitleText(clLine4, TYPE_LINE, 3)
            initTitleText(clLine5, TYPE_LINE, 4)

            initTitleText(clRect1, TYPE_RECT, 0)
            initTitleText(clRect2, TYPE_RECT, 1)
            initTitleText(clRect3, TYPE_RECT, 2)
            initTitleText(clRect4, TYPE_RECT, 3)
            initTitleText(clRect5, TYPE_RECT, 4)
        }
    }

    /**
     * Initializes professional temperature measurement title text for specified measurement type.
     *
     * Sets up localized title text, average temperature labels, and explanatory text
     * for different thermal measurement modes (full screen, point, line, rectangular).
     *
     * @param itemRoot The root view container for this measurement item
     * @param type The measurement type (TYPE_FULL, TYPE_POINT, TYPE_LINE, TYPE_RECT)
     * @param index The measurement index within the type (0-4 for multiple measurements)
     */
    private fun initTitleText(itemRoot: View, type: Int, index: Int) {
        val itemBinding = ItemReportIrShowBinding.bind(itemRoot)
        
        with(itemBinding) {
            tvTitle.isVisible = index == 0
            tvTitle.text = when (type) {
                TYPE_FULL -> context.getString(R.string.thermal_full_rect)
                TYPE_POINT -> context.getString(R.string.thermal_point) + "(P)"
                TYPE_LINE -> context.getString(R.string.thermal_line) + "(L)"
                else -> context.getString(R.string.thermal_rect) + "(R)"
            }
            tvAverageTitle.text = when (type) {
                TYPE_FULL, TYPE_POINT -> "" //全图、点没有平均温
                TYPE_LINE -> "L${index + 1} " + context.getString(R.string.album_report_mean_temperature)
                else -> "R${index + 1} " + context.getString(R.string.album_report_mean_temperature)
            }
            tvExplainTitle.text = when (type) {
                TYPE_FULL -> context.getString(R.string.album_report_comment)
                TYPE_POINT -> "P${index + 1} " + context.getString(R.string.album_report_comment)
                TYPE_LINE -> "L${index + 1} " + context.getString(R.string.album_report_comment)
                else -> "R${index + 1} " + context.getString(R.string.album_report_comment)
            }
        }
    }

    /**
     * Retrieves comprehensive list of views for professional PDF thermal report generation.
     *
     * Collects all visible thermal measurement views including image, temperature ranges,
     * averages, and explanatory text for industry-standard documentation output.
     * Supports research-grade thermal analysis with complete measurement traceability.
     *
     * @return ArrayList of views ready for PDF conversion in clinical report format
     */
    fun getPrintViewList(): ArrayList<View> {
        val result = ArrayList<View>()
        
        with(binding) {
            result.add(clImage)

            getItemChild(clFull, result)

            getItemChild(clPoint1, result)
            getItemChild(clPoint2, result)
            getItemChild(clPoint3, result)
            getItemChild(clPoint4, result)
            getItemChild(clPoint5, result)

            getItemChild(clLine1, result)
            getItemChild(clLine2, result)
            getItemChild(clLine3, result)
            getItemChild(clLine4, result)
            getItemChild(clLine5, result)

            getItemChild(clRect1, result)
            getItemChild(clRect2, result)
            getItemChild(clRect3, result)
            getItemChild(clRect4, result)
            getItemChild(clRect5, result)
        }
        
        return result
    }

    /**
     * Collects visible measurement child views for PDF generation.
     *
     * @param itemRoot The measurement item container
     * @param resultList The collection to add visible views to
     */
    private fun getItemChild(itemRoot: View, resultList: ArrayList<View>) {
        if (itemRoot.isVisible) {
            val itemBinding = ItemReportIrShowBinding.bind(itemRoot)
            with(itemBinding) {
                if (clRange.isVisible) {
                    resultList.add(clRange)
                }
                if (clAverage.isVisible) {
                    resultList.add(clAverage)
                }
                if (clExplain.isVisible) {
                    resultList.add(clExplain)
                }
            }
        }
    }

    /**
     * Sets professional thermal image drawable with research-grade aspect ratio management.
     *
     * Automatically calculates optimal display dimensions based on screen size and image
     * aspect ratio for consistent thermal data visualization across different devices.
     *
     * @param drawable The thermal image drawable to display
     */
    fun setImageDrawable(drawable: Drawable?) {
        val isLand = (drawable?.intrinsicWidth ?: 0) > (drawable?.intrinsicHeight ?: 0)
        val width = (ScreenUtil.getScreenWidth(context) * (if (isLand) 234 else 175) / 375f).toInt()
        val height = (width * (drawable?.intrinsicHeight ?: 0).toFloat() / (drawable?.intrinsicWidth ?: 1)).toInt()
        
        with(binding.ivImage) {
            val layoutParams = this.layoutParams
            layoutParams.width = width
            layoutParams.height = height
            this.layoutParams = layoutParams
            setImageDrawable(drawable)
        }
    }

    /**
     * Refreshes comprehensive thermal measurement data with professional formatting.
     *
     * Updates all thermal measurement displays including full-screen analysis, multi-point
     * measurements, linear profiles, and rectangular regions. Implements industry-standard
     * thermal data visualization patterns suitable for clinical and research applications.
     *
     * @param isFirst Whether this is the first measurement in a series
     * @param isLast Whether this is the last measurement in a series  
     * @param reportIRBean The thermal measurement data container with all measurement types
     */
    fun refreshData(isFirst: Boolean, isLast: Boolean, reportIRBean: ReportIRBean) {
        with(binding) {
            tvHead.isVisible = isFirst
            viewNotHead.isVisible = !isFirst
            viewImageBg.setBackgroundResource(
                if (isFirst) R.drawable.layer_report_ir_show_top_bg 
                else R.drawable.layer_report_ir_show_item_bg
            )
            clImage.setPadding(0, if (isFirst) SizeUtils.dp2px(20f) else 0, 0, 0)

            refreshItem(clFull, reportIRBean.full_graph_data, TYPE_FULL, 0)

            // Process point measurements (up to 5 points)
            val pointList = reportIRBean.point_data
            for (i in pointList.indices) {
                when (i) {
                    0 -> refreshItem(clPoint1, pointList[i], TYPE_POINT, i)
                    1 -> refreshItem(clPoint2, pointList[i], TYPE_POINT, i)
                    2 -> refreshItem(clPoint3, pointList[i], TYPE_POINT, i)
                    3 -> refreshItem(clPoint4, pointList[i], TYPE_POINT, i)
                    4 -> refreshItem(clPoint5, pointList[i], TYPE_POINT, i)
                }
            }
            
            // Handle point measurement title visibility
            val point1Binding = ItemReportIrShowBinding.bind(clPoint1)
            val point2Binding = ItemReportIrShowBinding.bind(clPoint2)
            val point3Binding = ItemReportIrShowBinding.bind(clPoint3)
            val point4Binding = ItemReportIrShowBinding.bind(clPoint4)
            val point5Binding = ItemReportIrShowBinding.bind(clPoint5)
            
            point2Binding.tvTitle.isVisible = !clPoint1.isVisible
            point3Binding.tvTitle.isVisible = !clPoint1.isVisible && !clPoint2.isVisible
            point4Binding.tvTitle.isVisible = !clPoint1.isVisible && !clPoint2.isVisible && !clPoint3.isVisible
            point5Binding.tvTitle.isVisible = !clPoint1.isVisible && !clPoint2.isVisible && !clPoint3.isVisible && !clPoint4.isVisible

            // Process line measurements (up to 5 lines)
            val lineList = reportIRBean.line_data
            for (i in lineList.indices) {
                when (i) {
                    0 -> refreshItem(clLine1, lineList[i], TYPE_LINE, i)
                    1 -> refreshItem(clLine2, lineList[i], TYPE_LINE, i)
                    2 -> refreshItem(clLine3, lineList[i], TYPE_LINE, i)
                    3 -> refreshItem(clLine4, lineList[i], TYPE_LINE, i)
                    4 -> refreshItem(clLine5, lineList[i], TYPE_LINE, i)
                }
            }
            
            // Handle line measurement title visibility
            val line1Binding = ItemReportIrShowBinding.bind(clLine1)
            val line2Binding = ItemReportIrShowBinding.bind(clLine2)
            val line3Binding = ItemReportIrShowBinding.bind(clLine3)
            val line4Binding = ItemReportIrShowBinding.bind(clLine4)
            val line5Binding = ItemReportIrShowBinding.bind(clLine5)
            
            line2Binding.tvTitle.isVisible = !clLine1.isVisible
            line3Binding.tvTitle.isVisible = !clLine1.isVisible && !clLine2.isVisible
            line4Binding.tvTitle.isVisible = !clLine1.isVisible && !clLine2.isVisible && !clLine3.isVisible
            line5Binding.tvTitle.isVisible = !clLine1.isVisible && !clLine2.isVisible && !clLine3.isVisible && !clLine4.isVisible

            // Process rectangular measurements (up to 5 rectangles)  
            val rectList = reportIRBean.surface_data
            for (i in rectList.indices) {
                when (i) {
                    0 -> refreshItem(clRect1, rectList[i], TYPE_RECT, i)
                    1 -> refreshItem(clRect2, rectList[i], TYPE_RECT, i)
                    2 -> refreshItem(clRect3, rectList[i], TYPE_RECT, i)
                    3 -> refreshItem(clRect4, rectList[i], TYPE_RECT, i)
                    4 -> refreshItem(clRect5, rectList[i], TYPE_RECT, i)
                }
            }
            
            // Handle rectangular measurement title visibility
            val rect1Binding = ItemReportIrShowBinding.bind(clRect1)
            val rect2Binding = ItemReportIrShowBinding.bind(clRect2)
            val rect3Binding = ItemReportIrShowBinding.bind(clRect3)
            val rect4Binding = ItemReportIrShowBinding.bind(clRect4)
            val rect5Binding = ItemReportIrShowBinding.bind(clRect5)
            
            rect2Binding.tvTitle.isVisible = !clRect1.isVisible
            rect3Binding.tvTitle.isVisible = !clRect1.isVisible && !clRect2.isVisible
            rect4Binding.tvTitle.isVisible = !clRect1.isVisible && !clRect2.isVisible && !clRect3.isVisible
            rect5Binding.tvTitle.isVisible = !clRect1.isVisible && !clRect2.isVisible && !clRect3.isVisible && !clRect4.isVisible

            // Handle bottom line visibility for last measurement
            when {
                rectList.isNotEmpty() -> {
                    when (rectList.size) {
                        1 -> hideLastLine(isLast, clRect1, rectList[0], TYPE_RECT)
                        2 -> hideLastLine(isLast, clRect2, rectList[1], TYPE_RECT)
                        3 -> hideLastLine(isLast, clRect3, rectList[2], TYPE_RECT)
                        4 -> hideLastLine(isLast, clRect4, rectList[3], TYPE_RECT)
                        5 -> hideLastLine(isLast, clRect5, rectList[4], TYPE_RECT)
                    }
                    return
                }
                lineList.isNotEmpty() -> {
                    when (lineList.size) {
                        1 -> hideLastLine(isLast, clLine1, lineList[0], TYPE_LINE)
                        2 -> hideLastLine(isLast, clLine2, lineList[1], TYPE_LINE)
                        3 -> hideLastLine(isLast, clLine3, lineList[2], TYPE_LINE)
                        4 -> hideLastLine(isLast, clLine4, lineList[3], TYPE_LINE)
                        5 -> hideLastLine(isLast, clLine5, lineList[4], TYPE_LINE)
                    }
                    return
                }
                pointList.isNotEmpty() -> {
                    when (pointList.size) {
                        1 -> hideLastLine(isLast, clPoint1, pointList[0], TYPE_POINT)
                        2 -> hideLastLine(isLast, clPoint2, pointList[1], TYPE_POINT)
                        3 -> hideLastLine(isLast, clPoint3, pointList[2], TYPE_POINT)
                        4 -> hideLastLine(isLast, clPoint4, pointList[3], TYPE_POINT)
                        5 -> hideLastLine(isLast, clPoint5, pointList[4], TYPE_POINT)
                    }
                    return
                }
                else -> hideLastLine(isLast, clFull, reportIRBean.full_graph_data, TYPE_FULL)
            }
        }
    }

    /**
     * Hides the last separator line for professional report formatting.
     *
     * @param isLast Whether this is the last measurement item
     * @param itemRoot The measurement item container
     * @param tempBean The temperature data bean
     * @param type The measurement type
     */
    private fun hideLastLine(isLast: Boolean, itemRoot: View, tempBean: ReportTempBean?, type: Int) {
        if (tempBean == null) return
        
        val itemBinding = ItemReportIrShowBinding.bind(itemRoot)
        with(itemBinding) {
            when {
                tempBean.isExplainOpen() -> {
                    viewLineExplain.isVisible = false
                    clExplain.setPadding(0, 0, 0, SizeUtils.dp2px(if (isLast) 12f else 20f))
                    if (isLast) {
                        clExplain.setBackgroundResource(R.drawable.layer_report_ir_show_bottom_bg)
                    }
                }
                (type == TYPE_LINE || type == TYPE_RECT) && tempBean.isAverageOpen() -> {
                    viewLineAverage.isVisible = false
                    clAverage.setPadding(0, 0, 0, SizeUtils.dp2px(if (isLast) 12f else 20f))
                    if (isLast) {
                        clAverage.setBackgroundResource(R.drawable.layer_report_ir_show_bottom_bg)
                    }
                }
                else -> {
                    viewLineRange.isVisible = false
                    clRange.setPadding(0, 0, 0, SizeUtils.dp2px(if (isLast) 12f else 20f))
                    if (isLast) {
                        clRange.setBackgroundResource(R.drawable.layer_report_ir_show_bottom_bg)
                    }
                }
            }
        }
    }

    /**
     * Refreshes individual thermal measurement item with professional data formatting.
     *
     * Updates temperature ranges, averages, and explanatory text for specific measurement types.
     * Implements industry-standard thermal data presentation suitable for clinical documentation.
     *
     * @param itemRoot The measurement item container view
     * @param tempBean The temperature measurement data
     * @param type The measurement type (full screen, point, line, rectangle)
     * @param index The measurement index within the type
     */
    private fun refreshItem(itemRoot: View, tempBean: ReportTempBean?, type: Int, index: Int) {
        if (tempBean == null) {
            itemRoot.isVisible = false
            return
        }

        itemRoot.isVisible = when (type) {
            TYPE_FULL -> tempBean.isMaxOpen() || tempBean.isMinOpen() || tempBean.isExplainOpen()
            TYPE_POINT -> tempBean.isTempOpen() || tempBean.isExplainOpen()
            else -> tempBean.isMaxOpen() || tempBean.isMinOpen() || tempBean.isAverageOpen() || tempBean.isExplainOpen()
        }
        
        if (!itemRoot.isVisible) return

        val itemBinding = ItemReportIrShowBinding.bind(itemRoot)
        
        // Determine appropriate title and value text
        val rangeTitle = if (type == TYPE_POINT) {
            "P${index + 1} " + context.getString(R.string.chart_temperature)
        } else {
            val prefix = when (type) {
                TYPE_LINE -> "L${index + 1} "
                TYPE_RECT -> "R${index + 1} "
                else -> ""
            }
            prefix + if (tempBean.isMinOpen() && tempBean.isMaxOpen()) {
                context.getString(R.string.chart_temperature_low) + "-" + context.getString(R.string.chart_temperature_high)
            } else if (tempBean.isMinOpen()) {
                context.getString(R.string.chart_temperature_low)
            } else {
                context.getString(R.string.chart_temperature_high)
            }
        }
        
        val rangeValue = if (type == TYPE_POINT) {
            tempBean.temperature
        } else {
            if (tempBean.isMinOpen() && tempBean.isMaxOpen()) {
                tempBean.min_temperature + "~" + tempBean.max_temperature
            } else if (tempBean.isMinOpen()) {
                tempBean.min_temperature
            } else {
                tempBean.max_temperature
            }
        }

        // Update UI elements with professional thermal data
        with(itemBinding) {
            tvRangeTitle.isVisible = if (type == TYPE_POINT) tempBean.isTempOpen() else tempBean.isMinOpen() || tempBean.isMaxOpen()
            tvRangeValue.isVisible = if (type == TYPE_POINT) tempBean.isTempOpen() else tempBean.isMinOpen() || tempBean.isMaxOpen()
            viewLineRange.isVisible = if (type == TYPE_POINT) tempBean.isTempOpen() else tempBean.isMinOpen() || tempBean.isMaxOpen()
            clAverage.isVisible = (type == TYPE_LINE || type == TYPE_RECT) && tempBean.isAverageOpen()
            clExplain.isVisible = tempBean.isExplainOpen()
            tvRangeTitle.text = rangeTitle
            tvRangeValue.text = rangeValue
            tvAverageValue.text = tempBean.mean_temperature
            tvExplainValue.text = tempBean.comment
        }
    }
}