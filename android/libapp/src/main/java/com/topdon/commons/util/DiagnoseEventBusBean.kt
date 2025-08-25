package com.topdon.commons.util

data class DiagnoseEventBusBean(
    var what: Int = 0,
    var language: String? = null,
    var isSnConnection: Boolean = false,
    var isDiagnose: Boolean = false,
    var mDiagEntryType: Long = 0,
    var mDiagMenuMask: Long = 0,
    var snPath: String? = null
) {
    fun getDiagMenuMask(): Long = mDiagMenuMask
    
    fun setDiagMenuMask(diagMenuMask: Long) {
        mDiagMenuMask = diagMenuMask
    }
    
    fun getmDiagEntryType(): Long = mDiagEntryType
    
    fun setmDiagEntryType(mDiagEntryType: Long) {
        this.mDiagEntryType = mDiagEntryType
    }
    
    fun setDiagnose(diagnose: Boolean) {
        isDiagnose = diagnose
    }
}