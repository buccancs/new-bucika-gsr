package com.topdon.house.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.FileUtils
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.db.AppDatabase
import com.topdon.lib.core.db.entity.HouseReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * 房屋报告 ViewModel.
 *
 * Created by LCG on 2024/8/28.
 */
class ReportViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * 所有房屋报告列表，调用 [queryAll] 会触发更改.
     */
    val reportListLD =  MutableLiveData<List<HouseReport>>()
    /**
     * 查询所有房屋检测列表，结果通过 [reportListLD] 返回.
     */
    fun queryAll() {
        viewModelScope.launch(Dispatchers.IO) {
            reportListLD.postValue(AppDatabase.getInstance().houseReportDao().queryAllReport())
        }
    }


    /**
     * 一项房屋报告，调用 [queryById] 会触发更改.
     */
    val reportLD = MutableLiveData<HouseReport?>()
    /**
     * 查询指定 id 的房屋报告数据，结果通过 [reportLD] 返回.
     */
    fun queryById(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            reportLD.postValue(AppDatabase.getInstance().houseReportDao().queryById(id))
        }
    }


    /**
     * 更新指定的一项房屋报告.
     */
    fun update(houseReport: HouseReport) {
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.getInstance().houseReportDao().updateReport(houseReport)
        }
    }


    /**
     * 删除指定的房屋报告数据.
     */
    fun deleteMore(vararg houseReports: HouseReport) {
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.getInstance().houseReportDao().deleteReport(*houseReports)
            for (houseReport in houseReports) {
                FileUtils.delete(File(FileConfig.documentsDir, houseReport.getPdfFileName()))
            }
            queryAll()
        }
    }
}