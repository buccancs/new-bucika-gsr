//package com.topdon.module.thermal.activity.temp
//
//import android.util.Log
//import androidx.lifecycle.lifecycleScope
//import com.alibaba.android.arouter.facade.annotation.Route
//import com.github.aachartmodel.aainfographics.aachartcreator.*
//import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAScrollablePlotArea
//import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAStyle
//import com.topdon.lib.core.config.RouterConfig
//import com.topdon.lib.core.ktbase.BaseActivity
//import com.topdon.module.thermal.R
//import kotlinx.android.synthetic.main.activity_chart.*
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.collect
//import kotlinx.coroutines.flow.flow
//import kotlinx.coroutines.flow.map
//import kotlinx.coroutines.launch
//import kotlin.math.sin
//
//@Route(path = RouterConfig.CHART)
//class ChartActivity : BaseActivity() {
//
//    override fun initContentView() = R.layout.activity_chart
//
//    override fun initView() {
//        setTitleText("图表")
//        //初始数据
//        aa_chart_view.aa_drawChartWithChartOptions(
//            configureSpecialStyleMarkerOfSingleDataElementChart().aa_toAAOptions()
//        )
//        //动态更新
//        lifecycleScope.launch {
//            flow {
//                repeat(40) {
//                    delay(1000)
//                    emit(it.toFloat())
//                }
//            }.map {
//                val max = 38
//                val min = 1
//                val random = (Math.random() * (max - min) + min).toInt()
//                val y1 = sin(random * (it * Math.PI / 180)) + it * 2 * 0.01 + 10
//                getSeriesModel(y1.toFloat())
//                y1
//            }.collect {
//                Log.w("123", "data:${dataSeries.joinToString()}")
//                aa_chart_view.aa_addPointToChartSeriesElement(0, it, true)
//            }
//        }
//    }
//
//    override fun initData() {
//
//    }
//
//    private var dataSeries = arrayOfNulls<Float>(0)
//
//    private fun getSeriesModel(data: Float): Array<AASeriesElement> {
//        dataSeries = dataSeries.plus(data)
//        return arrayOf(
//            AASeriesElement()
//                .name("Tokyo")
//                .data(dataSeries as Array<Any>)
//        )
//    }
//
//    private fun configureSpecialStyleMarkerOfSingleDataElementChart(): AAChartModel {
//        return AAChartModel()
//            .chartType(AAChartType.Spline)
//            .title("监测记录")
//            .subtitle("2021-10-20")
//            .titleStyle(AAStyle.Companion.style("#FFFFFF"))
//            .subtitleStyle(AAStyle.Companion.style(color = "#FFFFFF", fontSize = 12f))
//            .backgroundColor("#3598E8")
//            .yAxisTitle("")
//            .axesTextColor("#FFFFFF")
//            .dataLabelsEnabled(false)//坐标点是否显示值
//            .tooltipEnabled(true)
//            .markerRadius(0f)
//            .scrollablePlotArea(AAScrollablePlotArea().minWidth(10).minHeight(10))
//            .xAxisVisible(true)
//            .yAxisVisible(true)
//            .series(
//                arrayOf(
//                    AASeriesElement()
//                        .name("vol")
//                        .color("#FFFFFF")
//                        .lineWidth(2f)
//                        .data(
//                            arrayOf(
//                                7.0,
//                                6.9,
//                                2.5,
//                                14.5,
//                                18.2,
//                                5.2,
//                                16.5,
//                                13.3,
//                                15.3,
//                                13.9,
//                                9.6
//                            )
//                        ).color("#FFFFFF")
//                )
//            )
//    }
//}