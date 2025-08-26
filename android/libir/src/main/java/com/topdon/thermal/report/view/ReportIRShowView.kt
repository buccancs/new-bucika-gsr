package com.topdon.thermal.report.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.blankj.utilcode.util.SizeUtils
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.thermal.R
import com.topdon.thermal.report.bean.ReportIRBean
import com.topdon.thermal.report.bean.ReportTempBean
import com.topdon.thermal.databinding.ViewReportIrShowBinding
import com.topdon.thermal.databinding.ItemReportIrShowBinding

class ReportIRShowView: LinearLayout {
    companion object {
        
        private const val TYPE_FULL = 0
        
        private const val TYPE_POINT = 1
        
        private const val TYPE_LINE = 2
        
        private const val TYPE_RECT = 3
    }

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
                TYPE_FULL, TYPE_POINT -> ""
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
            
            val point1Binding = ItemReportIrShowBinding.bind(clPoint1)
            val point2Binding = ItemReportIrShowBinding.bind(clPoint2)
            val point3Binding = ItemReportIrShowBinding.bind(clPoint3)
            val point4Binding = ItemReportIrShowBinding.bind(clPoint4)
            val point5Binding = ItemReportIrShowBinding.bind(clPoint5)
            
            point2Binding.tvTitle.isVisible = !clPoint1.isVisible
            point3Binding.tvTitle.isVisible = !clPoint1.isVisible && !clPoint2.isVisible
            point4Binding.tvTitle.isVisible = !clPoint1.isVisible && !clPoint2.isVisible && !clPoint3.isVisible
            point5Binding.tvTitle.isVisible = !clPoint1.isVisible && !clPoint2.isVisible && !clPoint3.isVisible && !clPoint4.isVisible

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
            
            val line1Binding = ItemReportIrShowBinding.bind(clLine1)
            val line2Binding = ItemReportIrShowBinding.bind(clLine2)
            val line3Binding = ItemReportIrShowBinding.bind(clLine3)
            val line4Binding = ItemReportIrShowBinding.bind(clLine4)
            val line5Binding = ItemReportIrShowBinding.bind(clLine5)
            
            line2Binding.tvTitle.isVisible = !clLine1.isVisible
            line3Binding.tvTitle.isVisible = !clLine1.isVisible && !clLine2.isVisible
            line4Binding.tvTitle.isVisible = !clLine1.isVisible && !clLine2.isVisible && !clLine3.isVisible
            line5Binding.tvTitle.isVisible = !clLine1.isVisible && !clLine2.isVisible && !clLine3.isVisible && !clLine4.isVisible

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
            
            val rect1Binding = ItemReportIrShowBinding.bind(clRect1)
            val rect2Binding = ItemReportIrShowBinding.bind(clRect2)
            val rect3Binding = ItemReportIrShowBinding.bind(clRect3)
            val rect4Binding = ItemReportIrShowBinding.bind(clRect4)
            val rect5Binding = ItemReportIrShowBinding.bind(clRect5)
            
            rect2Binding.tvTitle.isVisible = !clRect1.isVisible
            rect3Binding.tvTitle.isVisible = !clRect1.isVisible && !clRect2.isVisible
            rect4Binding.tvTitle.isVisible = !clRect1.isVisible && !clRect2.isVisible && !clRect3.isVisible
            rect5Binding.tvTitle.isVisible = !clRect1.isVisible && !clRect2.isVisible && !clRect3.isVisible && !clRect4.isVisible

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
