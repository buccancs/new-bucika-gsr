package com.topdon.thermal.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.topdon.thermal.R

/**
 * 简化的趋势图表视图 - 图表功能已移除
 * Simple trend chart view - Chart functionality removed
 */
class ChartTrendView : View {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textColor by lazy { ContextCompat.getColor(context, R.color.chart_text) }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        paint.color = textColor
        paint.textSize = 48f
        paint.textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        canvas.drawText("趋势图表功能已移除", centerX, centerY, paint)
        canvas.drawText("Trend chart functionality removed", centerX, centerY + 60f, paint)
    }

    // 保持原有的公共方法以防其他代码调用
    fun refreshData(vararg temp: Float) {
        // 空实现 - 图表功能已移除
        invalidate()
    }

    fun clearData() {
        // 空实现 - 图表功能已移除
        invalidate()
    }
}