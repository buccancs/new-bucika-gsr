package com.energy.iruvc.ircmd

// Minimal stub interface with simplified signatures
interface IRCMD {
    fun init(controlBlock: Any?)
    fun startPreview(dataFlowMode: Any?, frameCallback: Any?)
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
    fun close()
}