package com.energy.iruvc.ircmd

// Minimal stub implementation
class ConcreteIRCMDBuilder {
    private var ircmdType: IRCMDType? = null
    private var cameraId: Long? = null
    
    fun setIRCMDType(type: IRCMDType): ConcreteIRCMDBuilder {
        this.ircmdType = type
        return this
    }
    
    fun setIdCamera(id: Long): ConcreteIRCMDBuilder {
        this.cameraId = id
        return this
    }
    
    fun build(): IRCMD {
        return when (ircmdType) {
            IRCMDType.USB_IRCMD -> USBIRCMDImpl()
            IRCMDType.WIFI_IRCMD -> WifiIRCMDImpl()
            IRCMDType.BLE_IRCMD -> BleIRCMDImpl()
            IRCMDType.USB_IR_256_384 -> USBIRCMDImpl()
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
    
    override fun startPreview(previewPath: Any?, startSource: Any?, fps: Int, previewMode: Any?, dataFlowMode: Any?) {
        // USB preview start with parameters stub
    }
    
    override fun stopPreview(): Int {
        // USB stop preview stub
        return 0 // Return success code
    }
    
    override fun stopPreview(previewPath: Any?): Int {
        // USB stop preview with path stub
        return 0 // Return success code
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
    override fun startPreview(previewPath: Any?, startSource: Any?, fps: Int, previewMode: Any?, dataFlowMode: Any?) {}
    override fun stopPreview(): Int = 0
    override fun stopPreview(previewPath: Any?): Int = 0
    override fun setPropImageParams(param: Any?, value: Any?) {}
    override fun setPropAutoShutterParameter(param: Any?, value: Any?) {}
    override fun autoGainSwitch(normalTempData: ByteArray, imageRes: Any?, info: Any?, param: Any?, callback: Any?) {}
    override fun avoidOverexposure(enable: Boolean, gainStatus: Any?, normalTempData: ByteArray, imageRes: Any?, lowGainTempData: ByteArray, highGainTempData: ByteArray, pixelAboveProp: Float, switchFrameCnt: Int, closeFrameCnt: Int, callback: Any?) {}
    override fun close() {}
}

private class BleIRCMDImpl : IRCMD {
    override fun init(controlBlock: Any?) {}
    override fun startPreview(dataFlowMode: Any?, frameCallback: Any?) {}
    override fun startPreview(previewPath: Any?, startSource: Any?, fps: Int, previewMode: Any?, dataFlowMode: Any?) {}
    override fun stopPreview(): Int = 0
    override fun stopPreview(previewPath: Any?): Int = 0
    override fun setPropImageParams(param: Any?, value: Any?) {}
    override fun setPropAutoShutterParameter(param: Any?, value: Any?) {}
    override fun autoGainSwitch(normalTempData: ByteArray, imageRes: Any?, info: Any?, param: Any?, callback: Any?) {}
    override fun avoidOverexposure(enable: Boolean, gainStatus: Any?, normalTempData: ByteArray, imageRes: Any?, lowGainTempData: ByteArray, highGainTempData: ByteArray, pixelAboveProp: Float, switchFrameCnt: Int, closeFrameCnt: Int, callback: Any?) {}
    override fun close() {}
}