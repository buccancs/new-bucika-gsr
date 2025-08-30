package com.topdon.module.thermal.activity

import android.graphics.Color
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.blankj.utilcode.util.SizeUtils
import com.blankj.utilcode.util.ToastUtils
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.guide.zm04c.matrix.GuideInterface
import com.topdon.lib.core.bean.tools.ThermalBean
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.db.AppDatabase
import com.topdon.lib.core.db.entity.ThermalEntity
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.TimeTool
import com.topdon.module.thermal.R
import com.topdon.module.thermal.adapter.SettingCheckAdapter
import com.topdon.module.thermal.adapter.SettingTimeAdapter
import com.topdon.module.thermal.chart.MyValueFormatter
import com.topdon.module.thermal.utils.ArrayUtils
import com.topdon.module.thermal.view.MyMarkerView
import com.topdon.module.thermal.viewmodel.LogViewModel
import kotlinx.android.synthetic.main.activity_monitor_chart.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * 温度监控
 */
@Route(path = RouterConfig.MONITOR_CHART)
class MonitorChartActivity : BaseActivity(), View.OnClickListener, OnChartValueSelectedListener {

    private val viewModel: LogViewModel by viewModels()

    private val timeAdapter: SettingTimeAdapter by lazy { SettingTimeAdapter(this) }//时分秒
    private val adapter: SettingCheckAdapter by lazy { SettingCheckAdapter(this) }//时间间隔

    //    var MONITOR_ACTION = STATS_START
    private var selectDuration = 1
    private var selectType = 1//选取点 1:单点    2:线条    3:区域
    private var selectIndex: ArrayList<Int> = arrayListOf()//选取点
    private val bean = ThermalBean()
    private var selectTimeType = 1
    private var latestTime = 0L//记录当前图表最新时间戳,用于判断是否刷新(分, 时, 天)电压数据
    private var startMonitor = false

    private lateinit var chart: LineChart

    override fun initContentView() = R.layout.activity_monitor_chart

    override fun initView() {
        setTitleText(R.string.main_thermal_motion)
        selectType = intent.getIntExtra("type", 3)
        selectIndex = intent.getIntegerArrayListExtra("select")!!
        Log.w("123", "selectType:$selectType")
        Log.w("123", "selectIndex:${selectIndex.joinToString()}")
        SharedManager.setSelectFenceType(selectType)
        type = when (selectType) {
            1 -> "point"
            2 -> "line"
            else -> "fence"
        }
        chart = mp_chart_view
        initChart()
        initRecycler()
        viewModel.resultLiveData.observe(this)
        {
            //查询到历史数据
            Log.w("123", "查询到历史数据:${it.dataList.size}")
            resultVol(it)
        }
        lifecycleScope.launch {
            delay(300)
            onIrVideoStart()
        }
    }

    override fun initData() {

    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
        onIrVideoStop()
    }

    override fun onClick(v: View?) {
        when (v) {

        }
    }

    private fun initRecycler() {
        monitor_chart_time_recycler.layoutManager = GridLayoutManager(this, 4)
        monitor_chart_time_recycler.adapter = timeAdapter
        monitor_chart_setting_recycler.layoutManager = GridLayoutManager(this, 3)
        monitor_chart_setting_recycler.adapter = adapter
        //设置时间段类型(秒 分 时 天)
        timeAdapter.listener = object : SettingTimeAdapter.OnItemClickListener {
            override fun onClick(index: Int, timeType: Int) {
                selectTimeType = timeType
                chart.highlightValue(null) //关闭高亮点Marker
                latestTime = 0L
                showLoading()
                lifecycleScope.launch {
                    delay(500)
                    queryLog(2)
                }
            }
        }
        //时间间隔
        adapter.listener = object : SettingCheckAdapter.OnItemClickListener {
            override fun onClick(index: Int, time: Int) {
                if (recordTask != null && recordTask!!.isActive) {
                    recordTask!!.cancel()
                    recordTask = null
                }
//                canUpdate = false
                Log.w("123", "select:$time")
                adapter.setCheck(index)
                timeMillis = time * 1000L
                pointIndex = startIndex - defaultCount
                recordThermal()//开始记录
            }
        }
    }

    val defaultCount = 20//默认显示10个数
    val startIndex = 0f
    var pointIndex = startIndex - defaultCount

    ///////////
    var mIsIrVideoStart = false

    private var mGuideInterface: GuideInterface? = null
    var rotateType = 3

    /**
     * 开启视频流
     */
    private fun onIrVideoStart() {
        mIsIrVideoStart = if (mIsIrVideoStart) {
            ToastUtils.showShort("视频流已开启")
            return
        } else {
            true
        }
        mGuideInterface = GuideInterface()
        val ret = mGuideInterface!!.init(this, object : GuideInterface.IrDataCallback {
            override fun processIrData(yuv: ByteArray, temp: FloatArray) {
                try {
                    //选取区域
                    val centerTempIndex: Int = 256 * (192 / 2) + 256 / 2
                    val maxTempIndex = ArrayUtils.getMaxIndex(temp, rotateType, selectIndex)
                    val minTempIndex = ArrayUtils.getMinIndex(temp, rotateType, selectIndex)
                    val rotateData = ArrayUtils.matrixRotate(srcData = temp, rotateType)
                    val bigDecimal = BigDecimal.valueOf(rotateData[centerTempIndex].toDouble())
                    val maxBigDecimal = BigDecimal.valueOf(rotateData[maxTempIndex].toDouble())
                    val minBigDecimal = BigDecimal.valueOf(rotateData[minTempIndex].toDouble())
                    bean.centerTemp = bigDecimal.setScale(1, BigDecimal.ROUND_HALF_UP).toFloat()
                    bean.maxTemp = maxBigDecimal.setScale(1, BigDecimal.ROUND_HALF_UP).toFloat()
                    bean.minTemp = minBigDecimal.setScale(1, BigDecimal.ROUND_HALF_UP).toFloat()
                    bean.createTime = System.currentTimeMillis()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "提取温度异常:${e.message}")
                }
            }

        })

        if (ret == 5) {
            Log.w("123", "视频流开启完成")
            recordThermal()//开始记录
        } else {
//            ToastUtils.showShort("视频流开启失败")
            Log.w("123", "视频流开启失败")
            mGuideInterface = null
            mIsIrVideoStart = false
        }
    }

    /**
     * 停止视频流
     */
    private fun onIrVideoStop() {
        mIsIrVideoStart = if (!mIsIrVideoStart) {
            Log.w("123", "视频流已停止")
            return
        } else {
            false
        }
        mGuideInterface!!.exit()
        mGuideInterface = null
        Log.w("123", "视频流停止完成")
    }

    var isRecord = false
    var type = ""
    var timeMillis = 1000L //间隔1s
    var canUpdate = false
    var recordTask: Job? = null
    var thermalId = TimeTool.showDateSecond()
    var startTime = 0L

    /**
     * 循环监听-数据保存
     */
    private fun recordThermal() {
        recordTask = lifecycleScope.launch(Dispatchers.IO) {
            isRecord = true
            startTime = System.currentTimeMillis()
            var time = 0L
            while (isRecord) {
                if (canUpdate) {
                    val entity = ThermalEntity()
                    entity.userId = SharedManager.getUserId()
                    entity.thermalId = thermalId
                    entity.thermal = NumberTools.to02f(bean.centerTemp)
                    entity.thermalMax = NumberTools.to02f(bean.maxTemp)
                    entity.thermalMin = NumberTools.to02f(bean.minTemp)
                    entity.type = type
                    entity.startTime = startTime
                    entity.createTime = System.currentTimeMillis()
                    AppDatabase.getInstance().thermalDao().insert(entity)
                    time++
                    launch(Dispatchers.Main) {
                        updateChart()
                    }
                    delay(timeMillis)
                } else {
                    Log.w("123", "当前不可更新")
                }
            }
            Log.w("123", "停止记录, 数据量:$time")
        }
    }


    //MPChart
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
//        xAxis.setLabelCount(6, true)//true保证有刻度数量不变
        xAxis.setLabelCount(6, false)//true保证有刻度数量不变
        val leftAxis = chart.axisLeft
        leftAxis.textSize = 9f
        leftAxis.textColor = textColor
        leftAxis.setDrawGridLines(true)
        leftAxis.setLabelCount(6, true)
        val rightAxis = chart.axisRight
        rightAxis.isEnabled = false
        chart.zoom(100f, 1f, chart.xChartMax, 0f)
        selectDuration = adapter.selectTime
        startTime = System.currentTimeMillis()
        canUpdate = true//可以开始更新记录
    }


    /**
     * 分类处理更新图表数据
     */
    private fun updateChart() {
        ++pointIndex
        when (selectTimeType) {
            1 -> {
                //秒
                addPointToChart(bean)
            }
            2 -> {
                //分
                val addTime = 2 * 60 * 1000L
                if (bean.createTime > TimeTool.timeToMinute(latestTime, 2) + addTime) {
                    queryLog(3)
                }
            }
            3 -> {
                //时
                val addTime = 2 * 60 * 60 * 1000L
                if (bean.createTime > TimeTool.timeToMinute(latestTime, 3) + addTime) {
                    queryLog(3)
                }
            }
            4 -> {
                //天(图表显示最后一个时间在昨天，要多加一天)
                val addTime = 2 * 24 * 60 * 60 * 1000L
                if (bean.createTime > TimeTool.timeToMinute(latestTime, 4) + addTime) {
                    queryLog(3)
                }
            }
        }
    }

    /**
     * 秒更新图表数据
     */
    private fun addPointToChart(bean: ThermalBean) {
        synchronized(chart) {
            try {
                if (bean.createTime == 0L) {
                    Log.w("123", "createTime = 0L, bean:${bean}")
                    return
                }
                val data = ThermalEntity()
                data.thermalMax = bean.maxTemp
                data.thermalMin = bean.minTemp
                data.thermal = bean.centerTemp
                data.createTime = bean.createTime
                val lineData: LineData = chart.data
                var volDataSet = lineData.getDataSetByIndex(0) //读取x为0的坐标点
                if (volDataSet == null) {
                    startTime = data.createTime
                    Log.w("123", "设置初始时间startTime:$startTime")
                    chart.xAxis.valueFormatter = MyValueFormatter(startTime = startTime)
                }
                val x = (data.createTime - startTime).toFloat()
                when (type) {
                    "point" -> {
                        if (volDataSet == null) {
                            volDataSet = createSet("green")
                            lineData.addDataSet(volDataSet)
                            Log.w("123", "volDataSet.entryCount:${volDataSet.entryCount}")
                        }
                        val entity = Entry(x, data.thermal)
                        entity.data = data
                        volDataSet.addEntry(entity)
                        Log.w("123", "添加一个数据:$entity")
                    }
                    "line" -> {
                        //第一条线
                        if (volDataSet == null) {
                            volDataSet = createSet("red")
                            lineData.addDataSet(volDataSet)
                            Log.w("123", "volDataSet.entryCount:${volDataSet.entryCount}")
                        }
                        val entity = Entry(x, data.thermalMax)
                        entity.data = data
                        volDataSet.addEntry(entity)

                        //第二条线
                        var secondDataSet = lineData.getDataSetByIndex(1) //读取x为0的坐标点
                        if (secondDataSet == null) {
                            secondDataSet = createSet("blue")
                            lineData.addDataSet(secondDataSet)
                        }
                        val secondEntity = Entry(x, data.thermalMin)
                        secondEntity.data = data
                        secondDataSet.addEntry(secondEntity)
                    }
                    else -> {
                        //第一条线
                        if (volDataSet == null) {
                            volDataSet = createSet("red")
                            lineData.addDataSet(volDataSet)
                        }
                        val entity = Entry(x, data.thermalMax)
                        entity.data = data
                        volDataSet.addEntry(entity)

                        //第二条线
                        var secondDataSet = lineData.getDataSetByIndex(1) //读取x为0的坐标点
                        if (secondDataSet == null) {
                            secondDataSet = createSet("blue")
                            lineData.addDataSet(secondDataSet)
                        }
                        val secondEntity = Entry(x, data.thermalMin)
                        secondEntity.data = data
                        secondDataSet.addEntry(secondEntity)
                    }
                }

                lineData.notifyDataChanged()
                chart.notifyDataSetChanged()
                chart.setVisibleXRangeMinimum(getMinimum())//设置显示X轴区间大小
                chart.setVisibleXRangeMaximum(getMaximum())//设置显示X轴区间大小
                chart.xAxis.setLabelCount(getLabCount(volDataSet.entryCount), false)//true保证有刻度数量不变
                chart.moveViewToX(chart.xChartMax)//移动到最右端
                if (volDataSet.entryCount == 20) {
                    chart.zoom(100f, 1f, chart.xChartMax, 0f)
                }
                return@synchronized
            } catch (e: Exception) {
                Log.e("123", "添加数据时异常:${e.message}")
                return@synchronized
            }
        }
    }

    private val fillColor by lazy { ContextCompat.getDrawable(this, R.drawable.bg_chart_fill2) }
    private val lineRed by lazy { ContextCompat.getColor(this, R.color.chart_line_max) }
    private val lineBlue by lazy { ContextCompat.getColor(this, R.color.chart_line_min) }
    private val lineGreen by lazy { ContextCompat.getColor(this, R.color.chart_line_center) }
    private val whiteColors by lazy { ContextCompat.getColor(this, R.color.circle_white) }
    private val textColor by lazy { ContextCompat.getColor(this, R.color.chart_text) }

    /**
     * 曲线样式
     */
    private fun createSet(label: String): LineDataSet {
        val set = LineDataSet(null, label)
//        set.mode = LineDataSet.Mode.LINEAR
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.setDrawFilled(false)
//        set.fillDrawable = fillColor//设置填充颜色渐变
        set.axisDependency = YAxis.AxisDependency.LEFT

        when (label) {
            "red" -> {
                set.color = lineRed//曲线颜色
                set.circleHoleColor = lineRed//坐标内部颜色
            }
            "blue" -> {
                set.color = lineBlue//曲线颜色
                set.circleHoleColor = lineBlue//坐标内部颜色
            }
            else -> {
                set.color = lineGreen//曲线颜色
                set.circleHoleColor = lineGreen//坐标内部颜色
            }
        }

        set.setCircleColor(whiteColors)//坐标颜色
        set.circleHoleRadius = 4f//坐标点内部半径
        set.circleRadius = 5f//坐标点外部半径
        set.valueTextColor = Color.WHITE
        set.lineWidth = 2f
        set.fillAlpha = 200
        set.valueTextSize = 10f
        set.setDrawValues(false)//设置是否显示坐标值文本
        set.isHighlightEnabled = true//允许辅助线
        set.setDrawHorizontalHighlightIndicator(false)//水平辅助线关闭
        set.enableDashedHighlightLine(8f, 8f, 0f)//辅助虚线
        return set
    }

    /**
     * 查询历史电压数据(等待蓝牙传输历史记录结束后触发)
     * 时间区间: 现在时间 => 倒退到开始事件
     *
     * @param action
     * 0: 初始查询
     * 1: 刷新查询
     * 2: 切换查询
     * 3: 监听查询
     * 4: 加载历史数据后查询
     */
    private fun queryLog(action: Int) {
        startMonitor = false
        lifecycleScope.launch(Dispatchers.IO) {
//            dataList.clear()//清空数据
//            dataList = arrayListOf()
            viewModel.queryLogThermals(selectTimeType = selectTimeType, action = action)
        }
    }

    private fun resultVol(bean: LogViewModel.ChartList) {
        dismissLoading()
        if (selectTimeType != 1 && bean.dataList.size > 0) {
            val logTime = TimeTool.showDateType(bean.dataList.last().createTime, selectTimeType)
            val nowTime = TimeTool.showDateType(System.currentTimeMillis(), selectTimeType)
            if (TextUtils.equals(logTime, nowTime)) {
                //分时天,当前时间段没结束，应当删除最新当前时间段数据
                bean.dataList.removeLast()
            }
        }
//        dataList = bean.dataList
        if (latestTime == 0L) {
            //图表无数据需要更新
            addEntity(bean.dataList)
        } else if (bean.dataList.size > 0 && latestTime < bean.dataList.last().createTime) {
            //有新数据再更新
            addEntity(bean.dataList)
        }
    }

    //整体刷新
    private fun addEntity(data: ArrayList<ThermalEntity>) {
        clearEntity(data.size == 0)
        if (data.size == 0) {
            return
        }
        latestTime = data[data.size - 1].createTime
        startTime = data[0].createTime
        chart.xAxis.valueFormatter = MyValueFormatter(startTime = startTime)
        val lineData: LineData = chart.data
        var volDataSet = lineData.getDataSetByIndex(0) //读取x为0的坐标点
        if (volDataSet == null) {
            volDataSet = createSet("vol")
            lineData.addDataSet(volDataSet)
        }
        chart.xAxis.valueFormatter = MyValueFormatter(startTime = startTime, type = selectTimeType)
        val mv = MyMarkerView(this, R.layout.marker_lay)
        mv.chartView = chart
        chart.marker = mv//设置点击坐标显示提示框
        data.forEach {
            val x = (it.createTime - startTime).toFloat()
            when (type) {
                "point" -> {
                    if (volDataSet == null) {
                        volDataSet = createSet("green")
                        lineData.addDataSet(volDataSet)
                        Log.w("123", "volDataSet.entryCount:${volDataSet.entryCount}")
                    }
                    val entity = Entry(x, it.thermal)
                    entity.data = it
                    volDataSet.addEntry(entity)
                    Log.w("123", "添加一个数据:$entity")
                }
                "line" -> {
                    //第一条线
                    if (volDataSet == null) {
                        volDataSet = createSet("red")
                        lineData.addDataSet(volDataSet)
                        Log.w("123", "volDataSet.entryCount:${volDataSet.entryCount}")
                    }
                    val entity = Entry(x, it.thermalMax)
                    entity.data = it
                    volDataSet.addEntry(entity)

                    //第二条线
                    var secondDataSet = lineData.getDataSetByIndex(1) //读取x为0的坐标点
                    if (secondDataSet == null) {
                        secondDataSet = createSet("blue")
                        lineData.addDataSet(secondDataSet)
                    }
                    val secondEntity = Entry(x, it.thermalMin)
                    secondEntity.data = it
                    secondDataSet.addEntry(secondEntity)
                }
                else -> {
                    //第一条线
                    if (volDataSet == null) {
                        volDataSet = createSet("red")
                        lineData.addDataSet(volDataSet)
                    }
                    val entity = Entry(x, it.thermalMax)
                    entity.data = it
                    volDataSet.addEntry(entity)

                    //第二条线
                    var secondDataSet = lineData.getDataSetByIndex(1) //读取x为0的坐标点
                    if (secondDataSet == null) {
                        secondDataSet = createSet("blue")
                        lineData.addDataSet(secondDataSet)
                    }
                    val secondEntity = Entry(x, it.thermalMax)
                    secondEntity.data = it
                    secondDataSet.addEntry(secondEntity)
                }
            }
        }
        Log.w("123", "曲线数据:${volDataSet.entryCount}个")
        lineData.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.setVisibleXRangeMinimum(getMinimum())//设置显示X轴区间大小
        chart.setVisibleXRangeMaximum(getMaximum())//设置显示X轴区间大小
        Log.i(
            "123",
            "list moveViewToX:${chart.xChartMax}, chart.highestVisibleX:${chart.highestVisibleX}"
        )
        chart.moveViewToX(chart.xChartMax)//移动到最右端
        chart.xAxis.setLabelCount(getLabCount(volDataSet.entryCount), false)//true保证有刻度数量不变
        chart.zoom(100f, 1f, chart.xChartMax, 0f)
        startMonitor = true
    }

    private fun clearEntity(isEmpty: Boolean) {
        initChart()
        if (isEmpty) {
            chart.clear()
        } else {
            chart.clearValues()
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
        val min = when (selectTimeType) {
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
}