package com.topdon.module.thermal.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.elvishew.xlog.XLog
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.db.AppDatabase
import com.topdon.lib.core.db.entity.ThermalDayEntity
import com.topdon.lib.core.db.entity.ThermalEntity
import com.topdon.lib.core.db.entity.ThermalHourEntity
import com.topdon.lib.core.db.entity.ThermalMinuteEntity
import com.topdon.lib.core.ktbase.BaseViewModel
import com.topdon.lib.core.tools.TimeTool
import kotlinx.coroutines.*
import java.util.*

class LogViewModel : BaseViewModel() {

    val resultLiveData = MutableLiveData<ChartList>()

    private var queryJob: Job? = null

    fun queryLogByType(selectType: Int) {
        if (queryJob != null && queryJob!!.isActive) {
            queryJob!!.cancel()
            queryJob = null
        }
        queryJob = viewModelScope.launch(Dispatchers.IO) {
            var dataList: ArrayList<ThermalEntity>? = arrayListOf()
            var startTime = 0L
            var endTime = 0L
            when (selectType) {
                1 -> {
                    Log.w("123", "查询秒")
                    //秒
                    endTime = Date().time
                    startTime = endTime - 7200 * 1000L //2小时
                    Log.w("123", "query startTime:$startTime, endTime:$endTime")
                    dataList = AppDatabase.getInstance().thermalDao()
                        .getThermalByDate(
                            SharedManager.getUserId(),
                            startTime,
                            endTime
                        ) as ArrayList<ThermalEntity>
                    Log.w("123", "data size: ${dataList.size}")
                }
                2 -> {
                    //分
                    endTime = Date().time
                    startTime = endTime - 7200 * 60 * 1000L
                    dataList = AppDatabase.getInstance().thermalDao()
                        .getThermalByDate(
                            SharedManager.getUserId(),
                            startTime,
                            endTime
                        ) as ArrayList<ThermalEntity>
                }
                3 -> {
                    //时
                    endTime = Date().time
                    startTime = endTime - 7200 * 60 * 60 * 1000L
                    dataList = AppDatabase.getInstance().thermalDao()
                        .getThermalByDate(
                            SharedManager.getUserId(),
                            startTime,
                            endTime
                        ) as ArrayList<ThermalEntity>
                }
                else -> {
                    //天
                    dataList = AppDatabase.getInstance().thermalDao()
                        .getAllThermalByDate(SharedManager.getUserId()) as ArrayList<ThermalEntity>
                }
            }
            delay(500)
            if (dataList == null) {
                dataList = arrayListOf()
            } else {
                Log.w("123", "dataList size:${dataList.size}")
            }
            resultLiveData.postValue(ChartList(dataList = dataList))
        }
    }

    /**
     * 第一项实时图表的历史记录查询
     * 查询历史电压数据(等待蓝牙传输历史记录结束后触发)
     * 时间区间: 现在时间 => 倒退到开始事件
     */
    suspend fun queryLogThermals(
        selectTimeType: Int,
        endLogTime: Long = System.currentTimeMillis(),
        action: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = SharedManager.getUserId()
            val bean = ChartList()
            //查询之前先同步数据
            val job = async { syncVol(selectTimeType) }
            job.await()
            syncRun = false//同步结束
            val startLogTime = when (selectTimeType) {
                /**
                 * 7200数据
                 * 秒:2小时
                 * 分:5天
                 * 时:300天
                 * 天:20年
                 */
                1 -> endLogTime - 7200 * 1000L //秒(2小时)
                2 -> endLogTime - 7200 * 60 * 1000L //分(5天)
                3 -> endLogTime - 7200 * 60 * 60 * 1000L //时(300天)
                4 -> endLogTime - 1 * 365 * 24 * 60 * 60 * 1000L //天(1年)
                else -> endLogTime - 7200 * 1000L
            }
            when (selectTimeType) {
                1 -> {
                    bean.dataList = AppDatabase.getInstance().thermalDao()
                        .queryByTime(
                            userId = userId,
                            startTime = startLogTime,
                            endTime = endLogTime
                        ) as ArrayList<ThermalEntity>
                    bean.maxVol = AppDatabase.getInstance().thermalDao()
                        .queryByTimeMax(
                            userId = userId,
                            startTime = startLogTime,
                            endTime = endLogTime
                        )
                    bean.minVol = AppDatabase.getInstance().thermalDao()
                        .queryByTimeMin(
                            userId = userId,
                            startTime = startLogTime,
                            endTime = endLogTime
                        )
                    Log.w("chart", "电压数据:${bean.dataList.size}")
                    Log.w("chart", "电压数据max vol:${bean.maxVol},min vol:${bean.minVol}")
                }
                2 -> {
                    val resultList = AppDatabase.getInstance().thermalMinDao()
                        .queryByTime(
                            userId = userId,
                            startTime = startLogTime,
                            endTime = endLogTime
                        ) as ArrayList<ThermalMinuteEntity>
                    resultList.forEach {
                        val entity = ThermalEntity()
                        entity.userId = it.userId
                        entity.sn = it.sn
                        entity.thermal = it.thermal
                        entity.thermalMax = it.thermalMax
                        entity.thermalMin = it.thermalMin
                        entity.info = it.info
                        entity.type = it.type
                        entity.createTime = it.createTime
                        bean.dataList.add(entity)
                    }
                    bean.maxVol = AppDatabase.getInstance().thermalMinDao()
                        .queryByTimeMax(
                            userId = userId,
                            startTime = startLogTime,
                            endTime = endLogTime
                        )
                    bean.minVol = AppDatabase.getInstance().thermalMinDao()
                        .queryByTimeMin(
                            userId = userId,
                            startTime = startLogTime,
                            endTime = endLogTime
                        )
                    Log.w("chart", "电压数据:${bean.dataList.size}")
                    Log.w("chart", "电压数据max vol:${bean.maxVol},min vol:${bean.minVol}")
                }
                3 -> {
                    val resultList = AppDatabase.getInstance().thermalHourDao()
                        .queryByTime(
                            userId = userId,
                            startTime = startLogTime,
                            endTime = endLogTime
                        ) as ArrayList<ThermalHourEntity>
                    resultList.forEach {
                        val entity = ThermalEntity()
                        entity.userId = it.userId
                        entity.sn = it.sn
                        entity.thermal = it.thermal
                        entity.thermalMax = it.thermalMax
                        entity.thermalMin = it.thermalMin
                        entity.info = it.info
                        entity.type = it.type
                        entity.createTime = it.createTime
                        bean.dataList.add(entity)
                    }
                    bean.maxVol = AppDatabase.getInstance().thermalHourDao()
                        .queryByTimeMax(
                            userId = userId,
                            startTime = startLogTime,
                            endTime = endLogTime
                        )
                    bean.minVol = AppDatabase.getInstance().thermalHourDao()
                        .queryByTimeMin(
                            userId = userId,
                            startTime = startLogTime,
                            endTime = endLogTime
                        )
                    Log.w("chart", "电压数据:${bean.dataList.size}")
                    Log.w("chart", "电压数据max vol:${bean.maxVol},min vol:${bean.minVol}")
                }
                4 -> {
                    val resultList = AppDatabase.getInstance().thermalDayDao()
                        .queryByTime(
                            userId = userId,
                            startTime = startLogTime,
                            endTime = endLogTime
                        ) as ArrayList<ThermalDayEntity>
                    resultList.forEach {
                        val entity = ThermalEntity()
                        entity.userId = it.userId
                        entity.sn = it.sn
                        entity.thermal = it.thermal
                        entity.thermalMax = it.thermalMax
                        entity.thermalMin = it.thermalMin
                        entity.info = it.info
                        entity.type = it.type
                        entity.createTime = it.createTime
                        bean.dataList.add(entity)
                    }
                    bean.maxVol = AppDatabase.getInstance().thermalDayDao()
                        .queryByTimeMax(
                            userId = userId,
                            startTime = startLogTime,
                            endTime = endLogTime
                        )
                    bean.minVol = AppDatabase.getInstance().thermalDayDao()
                        .queryByTimeMin(
                            userId = userId,
                            startTime = startLogTime,
                            endTime = endLogTime
                        )
                    Log.w("chart", "电压数据:${bean.dataList.size}")
                    Log.w("chart", "电压数据max vol:${bean.maxVol},min vol:${bean.minVol}")
                }
            }
            bean.action = action
            if (action == 4) {
                val startTime = TimeTool.showDateType(bean.dataList.first().createTime)
                val endTime = TimeTool.showDateType(bean.dataList.last().createTime)
                Log.w("123", "log start:${startTime}, end:$endTime")
            }
            resultLiveData.postValue(bean)
        }
    }

    /**
     * 第二项历史记录查询
     * 查询历史电压数据(等待蓝牙传输历史记录结束后触发)
     * 时间区间: 初始时间 => 推进到结束事件
     */
    suspend fun queryLogVolsByStartTime(
        type: Int = 3,
        selectTimeType: Int,
        endLogTime: Long = System.currentTimeMillis(),
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = SharedManager.getUserId()
                val typeStr = when (type) {
                    1 -> "point"
                    2 -> "line"
                    else -> "fence"
                }
                val bean = ChartList()
                //查询之前先同步数据
                val job = async { syncVol(selectTimeType) }
                job.await()
                syncRun = false//同步结束
                val startLogTime = when (selectTimeType) {
                    /**
                     * 7200数据
                     * 秒:2小时
                     * 分:5天
                     * 时:300天
                     * 天:20年
                     */
//                1 -> startLogTime + 2 * 60 * 60 * 1000L //秒(2小时)
//                2 -> startLogTime + 24 * 60 * 60 * 1000L //分(1天)
//                3 -> startLogTime + 30 * 24 * 60 * 60 * 1000L //时(30天)
//                4 -> startLogTime + 1 * 365 * 24 * 60 * 60 * 1000L //天(1年)

//                1 -> startLogTime + 7200 * 1000L //秒(2小时)
//                2 -> startLogTime + 7200 * 60 * 1000L //分(5天)
//                3 -> startLogTime + 7200 * 60 * 60 * 1000L //时(300天)
//                4 -> startLogTime + 1 * 365 * 24 * 60 * 60 * 1000L //天(1年)
//                else -> startLogTime + 7200 * 1000L

                    1 -> endLogTime - 7200 * 1000L //秒(2小时)
                    2 -> endLogTime - 7200 * 60 * 1000L //分(5天)
                    3 -> endLogTime - 7200 * 60 * 60 * 1000L //时(300天)
                    4 -> endLogTime - 10 * 365 * 24 * 60 * 60 * 1000L //天(10年)
                    else -> endLogTime - 7200 * 1000L
                }
                when (selectTimeType) {
                    1 -> {
                        bean.dataList = AppDatabase.getInstance().thermalDao()
                            .queryByTime(
                                userId = userId,
                                startTime = startLogTime,
                                endTime = endLogTime,
                                type = typeStr
                            ) as ArrayList<ThermalEntity>
                        bean.maxVol = AppDatabase.getInstance().thermalDao()
                            .queryByTimeMax(
                                userId = userId,
                                startTime = startLogTime,
                                endTime = endLogTime
                            )
                        bean.minVol = AppDatabase.getInstance().thermalDao()
                            .queryByTimeMin(
                                userId = userId,
                                startTime = startLogTime,
                                endTime = endLogTime
                            )
                        Log.w("chart", "电压数据:${bean.dataList.size}")
                    }
                    2 -> {
                        val resultList = AppDatabase.getInstance().thermalMinDao()
                            .queryByTime(
                                userId = userId,
                                startTime = startLogTime,
                                endTime = endLogTime,
                                type = typeStr
                            ) as ArrayList<ThermalMinuteEntity>
                        resultList.forEach {
                            val entity = ThermalEntity()
                            entity.userId = it.userId
                            entity.sn = it.sn
                            entity.thermal = it.thermal
                            entity.thermalMax = it.thermalMax
                            entity.thermalMin = it.thermalMin
                            entity.info = it.info
                            entity.type = it.type
                            entity.createTime = it.createTime
                            bean.dataList.add(entity)
                        }
                        bean.maxVol = AppDatabase.getInstance().thermalMinDao()
                            .queryByTimeMax(
                                userId = userId,
                                startTime = startLogTime,
                                endTime = endLogTime
                            )
                        bean.minVol = AppDatabase.getInstance().thermalMinDao()
                            .queryByTimeMin(
                                userId = userId,
                                startTime = startLogTime,
                                endTime = endLogTime
                            )
                        Log.w("chart", "电压数据:${bean.dataList.size}")
                    }
                    3 -> {
                        val resultList = AppDatabase.getInstance().thermalHourDao()
                            .queryByTime(
                                userId = userId,
                                startTime = startLogTime,
                                endTime = endLogTime,
                                type = typeStr
                            ) as ArrayList<ThermalHourEntity>
                        resultList.forEach {
                            val entity = ThermalEntity()
                            entity.userId = it.userId
                            entity.sn = it.sn
                            entity.thermal = it.thermal
                            entity.thermalMax = it.thermalMax
                            entity.thermalMin = it.thermalMin
                            entity.info = it.info
                            entity.type = it.type
                            entity.createTime = it.createTime
                            bean.dataList.add(entity)
                        }
                        bean.maxVol = AppDatabase.getInstance().thermalHourDao()
                            .queryByTimeMax(
                                userId = userId,
                                startTime = startLogTime,
                                endTime = endLogTime
                            )
                        bean.minVol = AppDatabase.getInstance().thermalHourDao()
                            .queryByTimeMin(
                                userId = userId,
                                startTime = startLogTime,
                                endTime = endLogTime
                            )
                        Log.w("chart", "电压数据:${bean.dataList.size}")
                    }
                    4 -> {
                        val resultList = AppDatabase.getInstance().thermalDayDao()
                            .queryByTime(
                                userId = userId,
                                startTime = startLogTime,
                                endTime = endLogTime,
                                type = typeStr
                            ) as ArrayList<ThermalDayEntity>
                        resultList.forEach {
                            val entity = ThermalEntity()
                            entity.userId = it.userId
                            entity.sn = it.sn
                            entity.thermal = it.thermal
                            entity.thermalMax = it.thermalMax
                            entity.thermalMin = it.thermalMin
                            entity.info = it.info
                            entity.type = it.type
                            entity.createTime = it.createTime
                            bean.dataList.add(entity)
                        }
                        bean.maxVol = AppDatabase.getInstance().thermalDayDao()
                            .queryByTimeMax(
                                userId = userId,
                                startTime = startLogTime,
                                endTime = endLogTime
                            )
                        bean.minVol = AppDatabase.getInstance().thermalDayDao()
                            .queryByTimeMin(
                                userId = userId,
                                startTime = startLogTime,
                                endTime = endLogTime
                            )
                        Log.w("chart", "电压数据:${bean.dataList.size}")
                    }
                }
                delay(500)
                resultLiveData.postValue(bean)
            } catch (e: Exception) {
                XLog.e("数据查询异常:${e.message}")
                resultLiveData.postValue(ChartList())
            }
        }
    }


    /**
     * @param type 1:秒 2:分 3:时 4:天
     */
    private fun getNewVolData(data: List<ThermalEntity>, type: Int = 2): ArrayList<ThermalEntity> {
        val newData: ArrayList<ThermalEntity> = arrayListOf()
        var startIndex = 0
        var endIndex = 0
        for (i in data.indices) {
            if (i == 0) {
                if (i == data.size - 1) {
                    //默认整个区间
                    addData(data, newData, 0, endIndex)
                }
            } else {
                //[1]..[size-1]
                val currencyTime = TimeTool.showDateType(data[i].createTime, type)
                val previewTime = TimeTool.showDateType(data[i - 1].createTime, type)
                if (i == data.size - 1) {
                    //最后一个值
                    if (currencyTime != previewTime) {
                        //同时计算上一个区间和当前最后一个区间
                        //最后上一个区间
                        endIndex = i - 1
                        addData(data, newData, startIndex, endIndex)
                        startIndex = i
                        //最后一个区间
                        endIndex = i
                        addData(data, newData, startIndex, endIndex)
                    } else {
                        endIndex = i
                        if (newData.size == 0) {
                            //默认整个区间
                            addData(data, newData, 0, endIndex)
                        } else {
                            //最后一个区间
                            addData(data, newData, startIndex, endIndex)
                        }
                    }
                } else {
                    if (currencyTime != previewTime) {
                        //计算上一个区间
                        endIndex = i - 1
                        addData(data, newData, startIndex, endIndex)
                        //新时间段
                        startIndex = i
                    }
                }
            }
        }
        return newData
    }

    //计算平均值
    private fun addData(
        data: List<ThermalEntity>,
        newData: ArrayList<ThermalEntity>,
        startIndex: Int,
        endIndex: Int
    ) {
        val tempVolEntity = data[startIndex]
        var temp = 0f
        var tempMax = 0f
        var tempMin = 0f
        for (x in startIndex..endIndex) {
            temp += data[x].thermal
            tempMax += data[x].thermalMax
            tempMin += data[x].thermalMin
        }
        //tempVol:0f    startIndex:2    endIndex:1 会出现vol:NaN
        tempVolEntity.thermal = temp / (endIndex - startIndex + 1)//区间电压平均值
        tempVolEntity.thermalMax = tempMax / (endIndex - startIndex + 1)//区间电压平均值
        tempVolEntity.thermalMin = tempMin / (endIndex - startIndex + 1)//区间电压平均值
        newData.add(tempVolEntity)
    }

    @Volatile
    private var syncRun = false

    /**
     * 同步数据
     * 最早时间: 1609430400000 (2021-1-1 00:00:00)
     *
     * 1. 查询保存记录的最新时间
     * 2. 获取要更新的时间段数据[最新数据 ~ 最新一个时间区间的起始点]
     * 3. 秒数据转分数据的平均值
     * 4. 添加到分数据库
     * 5. 删除多余的数据
     */
    private suspend fun syncVol(selectTimeType: Int) {
        Log.i("chart", "syncVol: $syncRun")
        if (syncRun) {
            //有任务正在执行
            return
        }
        Log.i("chart", "syncVol start")
        if (selectTimeType == 1) {
            //秒数据不用处理
            return
        }
        syncRun = true
        val userId = SharedManager.getUserId()
        //查询保存记录的最新时间
        when (selectTimeType) {
            2 -> {
                val minuteTime = TimeTool.timeToMinute(System.currentTimeMillis(), 2)
                //检查最新时间段有没有数据同步
                val minuteVolLatestList =
                    AppDatabase.getInstance().thermalMinDao()
                        .queryByTime(
                            userId = userId,
                            startTime = minuteTime,
                            endTime = System.currentTimeMillis()
                        )
                if (minuteVolLatestList.isNotEmpty()) {
                    Log.w("chart", "最新时间段已经有记录，不需要同步更新")
                    return
                }
                val maxTime =
                    AppDatabase.getInstance().thermalMinDao().queryMaxTime(userId = userId)
                Log.w("chart", "minute latest time: $maxTime, ${TimeTool.showDateType(maxTime)}")
                //获取要更新的时间段数据
                val secondVolList = AppDatabase.getInstance().thermalDao()
                    .queryByTime(
                        userId = userId,
                        startTime = maxTime,
//                        endTime = Long.MAX_VALUE
                        endTime = minuteTime
                    ) as ArrayList<ThermalEntity>
                if (secondVolList.size > 0) {
                    val startTime = TimeTool.showDateType(secondVolList.first().createTime)
                    val endTime = TimeTool.showDateType(secondVolList.last().createTime)
                    Log.w("chart", "要处理${secondVolList.size}个数据, start:$startTime, end:${endTime}")
                } else {
                    Log.w("chart", "无数据处理")
                }
                //秒数据转分数据的平均值
                val minVolList = getNewVolData(secondVolList, 2)
                //添加到分数据库
                minVolList.forEach {
                    val bean = ThermalMinuteEntity()
                    try {
                        bean.userId = it.userId
                        bean.sn = it.sn
                        bean.thermal = it.thermal
                        bean.thermalMax = it.thermalMax
                        bean.thermalMin = it.thermalMin
                        bean.info = it.info
                        bean.type = it.type
                        bean.createTime = TimeTool.timeToMinute(it.createTime, 2)//调整精确到分
                        bean.updateTime = System.currentTimeMillis()
                        AppDatabase.getInstance().thermalMinDao().insert(bean)
                    } catch (e: Exception) {
                        XLog.e("insert error:${e.message}")
                    }
                }
                val bean = ThermalMinuteEntity()
                try {
                    bean.userId = userId
                    bean.thermal = 0f
                    bean.thermalMax = 0f
                    bean.thermalMin = 0f
                    bean.createTime = TimeTool.timeToMinute(System.currentTimeMillis(), 2)//调整精确到分
                    bean.updateTime = System.currentTimeMillis()
                    AppDatabase.getInstance().thermalMinDao().insert(bean)
                } catch (e: Exception) {
                    XLog.e("insert error:${e.message}")
                }
                //删除多余的数据
                AppDatabase.getInstance().thermalMinDao()
                    .deleteRepeatVol(userId)
            }
            3 -> {
                val hourTime = TimeTool.timeToMinute(System.currentTimeMillis(), 3)
                //检查最新时间段有没有数据同步
                val hourVolLatestList =
                    AppDatabase.getInstance().thermalHourDao()
                        .queryByTime(
                            userId = userId,
                            startTime = hourTime,
                            endTime = System.currentTimeMillis()
                        )
                if (hourVolLatestList.isNotEmpty()) {
                    Log.w("chart", "最新时间段已经有记录，不需要同步更新")
                    return
                }
                val maxTime =
                    AppDatabase.getInstance().thermalHourDao().queryMaxTime(userId = userId)
                Log.w("chart", "hour latest  time: $maxTime, ${TimeTool.showDateType(maxTime)}")
                //获取要更新的时间段数据
                val secondVolList = AppDatabase.getInstance().thermalDao()
                    .queryByTime(
                        userId = userId,
                        startTime = maxTime,
                        endTime = hourTime
                    ) as ArrayList<ThermalEntity>
                if (secondVolList.size > 0) {
                    val startTime = TimeTool.showDateType(secondVolList.first().createTime)
                    val endTime = TimeTool.showDateType(secondVolList.last().createTime)
                    Log.w("chart", "要处理${secondVolList.size}个数据, start:$startTime, end:${endTime}")
                } else {
                    Log.w("chart", "无数据处理")
                }
                //秒数据转分数据的平均值
                val hourVolList = getNewVolData(secondVolList, 3)
                //添加到分数据库
                hourVolList.forEach {
                    val bean = ThermalHourEntity()
                    bean.userId = it.userId
                    bean.sn = it.sn
                    bean.thermal = it.thermal
                    bean.thermalMax = it.thermalMax
                    bean.thermalMin = it.thermalMin
                    bean.info = it.info
                    bean.type = it.type
                    bean.createTime = TimeTool.timeToMinute(it.createTime, 3)//调整精确到分
                    bean.updateTime = System.currentTimeMillis()
                    AppDatabase.getInstance().thermalHourDao().insert(bean)
                }
                val bean = ThermalHourEntity()
                bean.userId = userId
                bean.thermal = 0f
                bean.thermalMax = 0f
                bean.thermalMin = 0f
                bean.createTime = TimeTool.timeToMinute(System.currentTimeMillis(), 3)//调整精确到分
                bean.updateTime = System.currentTimeMillis()
                AppDatabase.getInstance().thermalHourDao().insert(bean)
                //删除多余的数据
                AppDatabase.getInstance().thermalHourDao().deleteRepeatVol(userId)
            }
            4 -> {
                val todayStartTime =
                    TimeTool.timeToMinute(System.currentTimeMillis(), 4)//天只更新到今天凌晨的数据
                //检查今天有没有数据同步
                val todayVolLatestList =
                    AppDatabase.getInstance().thermalDayDao()
                        .queryByTime(
                            userId = userId,
                            startTime = todayStartTime,
                            endTime = System.currentTimeMillis()
                        )
                if (todayVolLatestList.isNotEmpty()) {
                    //今天已经有记录，说明已经同步到今天，不需要同步更新
                    Log.w("chart", "今天已经有记录，不需要同步更新")
                    return
                }
                val maxTime =
                    AppDatabase.getInstance().thermalDayDao().queryMaxTime(userId = userId)
                Log.w("chart", "day latest time: $maxTime, ${TimeTool.showDateType(maxTime)}")
                //获取要更新的时间段数据
                val secondVolList = AppDatabase.getInstance().thermalDao()
                    .queryByTime(
                        userId = userId,
                        startTime = maxTime,
                        endTime = todayStartTime
                    ) as ArrayList<ThermalEntity>
                //秒数据转分数据的平均值
                if (secondVolList.size > 0) {
                    val startTime = TimeTool.showDateType(secondVolList.first().createTime)
                    val endTime = TimeTool.showDateType(secondVolList.last().createTime)
                    Log.w("chart", "要处理${secondVolList.size}个数据, start:$startTime, end:${endTime}")
                } else {
                    Log.w("chart", "无数据处理")
                }
                val dayVolList = getNewVolData(secondVolList, 4)
                //添加到分数据库
                dayVolList.forEach {
                    val bean = ThermalDayEntity()
                    bean.userId = it.userId
                    bean.sn = it.sn
                    bean.thermal = it.thermal
                    bean.thermalMax = it.thermalMax
                    bean.thermalMin = it.thermalMin
                    bean.info = it.info
                    bean.type = it.type
                    bean.createTime = TimeTool.timeToMinute(it.createTime, 4)//调整精确到分
                    bean.updateTime = System.currentTimeMillis()
                    AppDatabase.getInstance().thermalDayDao().insert(bean)
                    //此处只更新到今天凌晨
                }
                //今天的电压值还没检查结束，不能出平均值结果，并值添加一个无效数据(0f)，用updateTime来判断已同步到最新的时间节点
                val bean = ThermalDayEntity()
                bean.userId = userId
                bean.thermal = 0f
                bean.thermalMax = 0f
                bean.thermalMin = 0f
                bean.createTime = TimeTool.timeToMinute(System.currentTimeMillis(), 4)//调整精确到分
                bean.updateTime = System.currentTimeMillis()
                AppDatabase.getInstance().thermalDayDao().insert(bean)
                //删除多余的数据
                AppDatabase.getInstance().thermalDayDao().deleteRepeatVol(userId)
            }
        }
        syncRun = false
        Log.w("chart", "syncVol end")
    }

    data class ChartList(
        var dataList: ArrayList<ThermalEntity> = arrayListOf(),
        var maxVol: Float = 0f,
        var minVol: Float = 0f,
        var action: Int = 0
    )
}