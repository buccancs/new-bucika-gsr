package com.topdon.house.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

/**
 * 房屋检测 ViewModel.
 *
 * Created by LCG on 2024/8/22.
 */
internal class TabViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * 是否处于编辑模式.
     */
    val isEditModeLD: MutableLiveData<Boolean> = MutableLiveData(false)
    /**
     * 当前选中数量.
     */
    val selectSizeLD: MutableLiveData<Int> = MutableLiveData(0)
}