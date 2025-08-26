package com.energy.iruvc.ircmd

// Minimal stub interface with simplified signatures
interface IRCMD {
    fun init(controlBlock: Any?)
    
    // Support both simple and complex startPreview signatures
    fun startPreview(dataFlowMode: Any?, frameCallback: Any?): Int
    fun startPreview(previewPath: Any?, startSource: Any?, fps: Int, previewMode: Any?, dataFlowMode: Any?): Int
    fun stopPreview(): Int
    fun stopPreview(previewPath: Any?): Int
    
    // Y16 mode preview methods  
    fun startY16ModePreview(previewPath: Any?, srcType: Any?, frameCallback: Any? = null): Int
    
    // Temperature and TNR methods
    fun isTempReplacedWithTNREnabled(param: Any?): Boolean
    
    fun setPropImageParams(param: Any?, value: Any?)
    fun setPropAutoShutterParameter(param: Any?, value: Any?)
    fun autoGainSwitch(
        normalTempData: ByteArray,
        imageRes: Any?,
        info: Any?,
        param: Any?,
        callback: Any?
    )
    fun avoidOverexposure(
        enable: Boolean,
        gainStatus: Any?,
        normalTempData: ByteArray,
        imageRes: Any?,
        lowGainTempData: ByteArray,
        highGainTempData: ByteArray,
        pixelAboveProp: Float,
        switchFrameCnt: Int,
        closeFrameCnt: Int,
        callback: Any?
    )
    
    fun onDestroy()
    fun close()
}