package com.topdon.thermal.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.topdon.lib.core.ktbase.BaseViewModel

class IRGalleryEditViewModel : BaseViewModel() {
    
    private val _resultLiveData = MutableLiveData<ResultData>()
    val resultLiveData: LiveData<ResultData> = _resultLiveData
    
    fun initData(filePath: String?) {
        // Initialize data processing
    }
    
    data class ResultData(
        val frame: ByteArray,
        val capital: String
    )
}