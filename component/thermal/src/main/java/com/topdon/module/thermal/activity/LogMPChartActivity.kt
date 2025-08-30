package com.topdon.module.thermal.activity

import android.graphics.Color
import android.util.Log
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.blankj.utilcode.util.SizeUtils
import com.elvishew.xlog.XLog
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.db.entity.ThermalEntity
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.ToastTools
import com.topdon.module.thermal.R
import com.topdon.module.thermal.adapter.SettingTimeAdapter
import com.topdon.module.thermal.chart.MyValueFormatter
import com.topdon.module.thermal.view.MyMarkerView
import com.topdon.module.thermal.viewmodel.LogViewModel
import kotlinx.android.synthetic.main.activity_log_mp_chart.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Route(path = RouterConfig.THERMAL_LOG_MP_CHART)
class LogMPChartActivity : BaseActivity(), OnChartValueSelectedListener {

    private val viewModel: LogViewModel by viewModels()

    private val adapter: SettingTimeAdapter by lazy { SettingTimeAdapter(this) }

    //    private var dataList: ArrayList<ThermalEntity> = arrayListOf()
    private lateinit var chart: LineChart
    private var selectType = 1

    override fun initContentView() = R.layout.activity_log_mp_chart

    override fun initView() {
        setTitleText(R.string.app_record)
        chart = log_chart_time_chart
        log_chart_time_recycler.layoutManager = GridLayoutManager(this, 4)
        log_chart_time_recycler.adapter = adapter
        adapter.listener = object : SettingTimeAdapter.OnItemClickListener {
            override fun onClick(index: Int, time: Int) {
                //切换类型
                chart.highlightValue(null) //关闭高亮点Marker
                selectType = index + 1
                queryLog()
            }
        }
        viewModel.resultLiveData.observe(this) {
            dismissLoading()
            try {
                initEntry(it.dataList)
            } catch (e: Exception) {
                XLog.e("刷新图表异常:${e.message}")
                ToastTools.showShort("图表异常，请重新加载")
            }
        }
        clearEntity(true)
    }

    override fun initData() {
        queryLog()
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun queryLog() {
        showLoading()
//        viewModel.queryLogByType(selectType)
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.queryLogVolsByStartTime(
                type = SharedManager.getSelectFenceType(),
                selectTimeType = selectType
            )
        }
    }

    private fun initChart() {
        chart.clear()
        chart.setOnChartValueSelectedListener(this)
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setDrawGridBackground(false)
        chart.description = null//图标描述文本
        chart.setBackgroundResource(R.color.chart_bg)
        chart.setScaleEnabled(true)//缩放
        chart.setPinchZoom(false)//禁用后，可以分别在x轴和y轴上进行缩放
        chart.isDoubleTapToZoomEnabled = false//双击不可缩放
        chart.isScaleYEnabled = false//禁止Y轴缩放
        chart.setExtraOffsets(
            0f,
            0f,
            SizeUtils.dp2px(8f).toFloat(),
            SizeUtils.dp2px(4f).toFloat()
        )//图表区域偏移
        chart.setNoDataText(getString(R.string.lms_http_code998))
        chart.setNoDataTextColor(textColor)
        val mv = MyMarkerView(this, R.layout.marker_lay)
        mv.chartView = chart
        chart.marker = mv//设置点击坐标显示提示框
        val data = LineData()
        data.setValueTextColor(textColor)
        chart.data = data
        val l = chart.legend
        l.form = Legend.LegendForm.CIRCLE
        l.textColor = textColor
        l.isEnabled = false//隐藏曲线标签
        val xAxis = chart.xAxis
        xAxis.textColor = textColor
        xAxis.setDrawGridLines(true)
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.isEnabled = true
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.axisLineColor = textColor
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true//重复值不显示
        xAxis.textSize = 9f
        val leftAxis = chart.axisLeft
        leftAxis.textSize = 9f
        leftAxis.textColor = textColor
        leftAxis.setDrawGridLines(true)
        leftAxis.setLabelCount(6, false)//固定x刻度
        val rightAxis = chart.axisRight
        rightAxis.isEnabled = false
//        chart.zoom(10f, 1f, chart.xChartMax, 0f)
    }

    private val bgChartColors = intArrayOf(
        R.drawable.bg_chart_fill,
        R.drawable.bg_chart_fill2,
        R.drawable.bg_chart_fill3
    )
    private val lineChartColors = intArrayOf(
        R.color.chart_line_max,
        R.color.chart_line_min,
        R.color.chart_line_center
    )
    private val textColor by lazy { ContextCompat.getColor(this, R.color.chart_text) }

    /**
     * 曲线样式
     */
    private fun createSet(index: Int, label: String): LineDataSet {
        val set = LineDataSet(null, label)
//        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.mode = LineDataSet.Mode.LINEAR
        set.setDrawFilled(false)
        set.fillDrawable = ContextCompat.getDrawable(this, bgChartColors[index])//设置填充颜色渐变
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.color = ContextCompat.getColor(this, lineChartColors[index])//曲线颜色
        set.setCircleColor(ContextCompat.getColor(this, R.color.white))//坐标颜色
//        set.fillColor = ContextCompat.getColor(this, R.color.purple_500)
//        set.highLightColor = ContextCompat.getColor(this, R.color.white)
        set.valueTextColor = Color.WHITE
        set.lineWidth = 2f
        set.circleRadius = 1f//不显示坐标点
        set.setCircleColor(ContextCompat.getColor(this, lineChartColors[index]))//坐标颜色(隐藏处理)
//        set.setCircleColor(ContextCompat.getColor(this, R.color.white))//坐标颜色(隐藏处理)
        set.fillAlpha = 200
        set.valueTextSize = 10f
        set.setDrawValues(false)//设置是否显示坐标值文本
        return set
    }

    private fun initEntry(data: ArrayList<ThermalEntity>) {
        synchronized(chart) {
            lifecycleScope.launch(Dispatchers.IO) {
                clearEntity(data.size == 0)
                if (data.size == 0) {
                    return@launch
                }
                Log.i("chart", "update chart start")
                val lineData: LineData = chart.data
                if (lineData != null) {
                    Log.w(
                        "123",
                        "时间区间:${(data.last().createTime - data.first().createTime) / 1000}"
                    )
                    val startTime = data[0].createTime
                    Log.w("123", "设置初始时间startTime:$startTime")
                    chart.xAxis.valueFormatter =
                        MyValueFormatter(startTime = startTime, type = selectType)
                    XLog.w("chart init startTime:$startTime")
                    data[0].type = "default"
                    when (data[0].type) {
                        "point" -> {
                            var set = lineData.getDataSetByIndex(0)//读取x为0的坐标点
                            if (set == null) {
                                set = createSet(2, "temp")
                                lineData.addDataSet(set)
                            }
                            data.forEach {
                                val x = (it.createTime - startTime).toFloat()
                                val entity = Entry(x, it.thermal)
                                entity.data = it
                                set.addEntry(entity)
                            }
                            XLog.w("DataSet:${set.entryCount}")
                        }
                        "line" -> {
                            var maxDataSet = lineData.getDataSetByIndex(0)//读取x为0的坐标点
                            if (maxDataSet == null) {
                                maxDataSet = createSet(0, "line maxTemp")
                                lineData.addDataSet(maxDataSet)
                            }

                            var minDataSet = lineData.getDataSetByIndex(1)//读取x为0的坐标点
                            if (minDataSet == null) {
                                minDataSet = createSet(1, "line minTemp")
                                lineData.addDataSet(minDataSet)
                            }
                            Log.w("123", "两条曲线")
                            data.forEach {
                                val x = (it.createTime - startTime).toFloat()
                                //max
                                val entity = Entry(x, it.thermalMax)
                                entity.data = it
                                maxDataSet.addEntry(entity)
                                //min
                                val entityMin = Entry(x, it.thermalMin)
                                entityMin.data = it
                                minDataSet.addEntry(entityMin)
                            }
                            XLog.w("DataSet:${maxDataSet.entryCount}")
                        }
                        else -> {
                            //max
                            var maxTempDataSet = lineData.getDataSetByIndex(0)//读取x为0的坐标点
                            if (maxTempDataSet == null) {
                                maxTempDataSet = createSet(0, "fence maxTemp")
                                lineData.addDataSet(maxTempDataSet)
                            }
                            //center
                            var centerTempDataSet = lineData.getDataSetByIndex(1)//读取x为0的坐标点
                            if (centerTempDataSet == null) {
                                centerTempDataSet = createSet(1, "fence minTemp")
                                lineData.addDataSet(centerTempDataSet)
                            }
                            data.forEach {
                                val x = (it.createTime - startTime).toFloat()
                                //max
                                val entityMax = Entry(x, it.thermalMax)
                                entityMax.data = it
                                maxTempDataSet.addEntry(entityMax)
                                //min
                                val entity = Entry(x, it.thermalMin)
                                entity.data = it
                                centerTempDataSet.addEntry(entity)
                            }
                            XLog.w("DataSet:${centerTempDataSet.entryCount}")
                        }
                    }
                    lineData.notifyDataChanged()
                    chart.notifyDataSetChanged()
                    chart.setVisibleXRangeMinimum(getMinimum())//设置显示X轴区间大小
                    chart.setVisibleXRangeMaximum(getMaximum())//设置显示X轴区间大小
                    chart.xAxis.setLabelCount(5, false)//true保证有刻度数量不变
                    chart.moveViewToX(chart.xChartMax)//移动到最右端
                    chart.zoom(1f, 1f, chart.xChartMax, 0f)//默认无缩放，全部显示
                }
                Log.w("chart", "update chart finish")
            }
        }
    }


    override fun onValueSelected(e: Entry?, h: Highlight?) {

    }

    override fun onNothingSelected() {

    }

    /**
     * x轴显示多少个刻度
     */
    private fun getLabCount(count: Int): Int {
        return when (count) {
            in 0..2 -> 1
            in 3..4 -> 2
            in 5..6 -> 3
            in 7..9 -> 4
            else -> 5
        }
    }

    //获取显示最小区间
    private fun getMinimum(): Float {
        val min = when (selectType) {
            1 -> 1 * 10 * 1000f //10s
            2 -> 10 * 60 * 1000f //10min
            3 -> 10 * 60 * 60 * 1000f //10hour
            4 -> 10 * 24 * 60 * 60 * 1000f //10day
            else -> 1 * 10 * 1000f //10s
        }
        return min
    }

    //获取显示最大区间，以最小区间的50倍
    private fun getMaximum(): Float {
        return getMinimum() * 50f
    }

    private fun clearEntity(isEmpty: Boolean) {
        initChart()
        if (isEmpty) {
            chart.clear()
        } else {
            chart.clearValues()
        }
    }

}