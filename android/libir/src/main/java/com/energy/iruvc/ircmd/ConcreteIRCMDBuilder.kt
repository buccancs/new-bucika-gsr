package com.energy.iruvc.ircmd

// Minimal stub implementation
class ConcreteIRCMDBuilder {
    private var ircmdType: IRCMDType? = null
    
    fun setIRCMDType(type: IRCMDType): ConcreteIRCMDBuilder {
        this.ircmdType = type
        return this
    }
    
    fun build(): IRCMD {
        return when (ircmdType) {
            IRCMDType.USB_IRCMD -> USBIRCMDImpl()
            IRCMDType.WIFI_IRCMD -> WifiIRCMDImpl()
            IRCMDType.BLE_IRCMD -> BleIRCMDImpl()
            else -> USBIRCMDImpl()
        }
    }
}

// Minimal stub implementations
private class USBIRCMDImpl : IRCMD {
    override fun init(controlBlock: Any?) {
        // USB IRCMD initialization stub
    }
    
    override fun startPreview(dataFlowMode: Any?, frameCallback: Any?) {
        // USB preview start stub
    }
    
    override fun setPropImageParams(param: Any?, value: Any?) {
        // Image parameters setting stub
    }
    
    override fun setPropAutoShutterParameter(param: Any?, value: Any?) {
        // Auto shutter parameter setting stub
    }
    
    override fun autoGainSwitch(
        normalTempData: ByteArray,
        imageRes: Any?,
        info: Any?,
        param: Any?,
        callback: Any?
    ) {
        // Auto gain switch stub - minimal implementation
    }
    
    override fun avoidOverexposure(
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
    ) {
        // Avoid overexposure stub - minimal implementation
    }
    
    override fun close() {
        // Close stub
    }
}

private class WifiIRCMDImpl : IRCMD {
    override fun init(controlBlock: Any?) {}
    override fun startPreview(dataFlowMode: Any?, frameCallback: Any?) {}
    override fun setPropImageParams(param: Any?, value: Any?) {}
    override fun setPropAutoShutterParameter(param: Any?, value: Any?) {}
    override fun autoGainSwitch(normalTempData: ByteArray, imageRes: Any?, info: Any?, param: Any?, callback: Any?) {}
    override fun avoidOverexposure(enable: Boolean, gainStatus: Any?, normalTempData: ByteArray, imageRes: Any?, lowGainTempData: ByteArray, highGainTempData: ByteArray, pixelAboveProp: Float, switchFrameCnt: Int, closeFrameCnt: Int, callback: Any?) {}
    override fun close() {}
}

private class BleIRCMDImpl : IRCMD {
    override fun init(controlBlock: Any?) {}
    override fun startPreview(dataFlowMode: Any?, frameCallback: Any?) {}
    override fun setPropImageParams(param: Any?, value: Any?) {}
    override fun setPropAutoShutterParameter(param: Any?, value: Any?) {}
    override fun autoGainSwitch(normalTempData: ByteArray, imageRes: Any?, info: Any?, param: Any?, callback: Any?) {}
    override fun avoidOverexposure(enable: Boolean, gainStatus: Any?, normalTempData: ByteArray, imageRes: Any?, lowGainTempData: ByteArray, highGainTempData: ByteArray, pixelAboveProp: Float, switchFrameCnt: Int, closeFrameCnt: Int, callback: Any?) {}
    override fun close() {}
}