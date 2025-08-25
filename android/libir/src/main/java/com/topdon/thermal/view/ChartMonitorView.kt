package com.topdon.thermal.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.topdon.lib.core.bean.tools.ThermalBean
import com.topdon.lib.core.db.entity.ThermalEntity
import com.topdon.thermal.R

class ChartMonitorView : View {

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
        canvas.drawText("监控图表功能已移除", centerX, centerY, paint)
        canvas.drawText("Monitor chart functionality removed", centerX, centerY + 60f, paint)
    }

    fun addEntry(data: ThermalBean?) {

        invalidate()
    }

    fun clearData() {

        invalidate()
    }

    fun setOnChartSlidingListener(listener: OnChartSlidingListener?) {

    }

    interface OnChartSlidingListener {
        fun onChartSliding()
    }
