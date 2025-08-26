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
    private var isInitialized = false
    private var isPreviewRunning = false
    private var currentFps = 25
    private var gainSwitchEnabled = false
    
    override fun init(controlBlock: Any?) {
        // USB IRCMD initialization stub
        isInitialized = true
        println("USB IRCMD initialized with control block: $controlBlock")
    }
    
    override fun startPreview(dataFlowMode: Any?, frameCallback: Any?): Int {
        // USB preview start stub
        if (!isInitialized) return -1
        isPreviewRunning = true
        println("USB preview started with mode: $dataFlowMode")
        return 0
    }
    
    override fun startPreview(previewPath: Any?, startSource: Any?, fps: Int, previewMode: Any?, dataFlowMode: Any?): Int {
        // USB preview start with parameters stub
        if (!isInitialized) return -1
        
        currentFps = fps
        isPreviewRunning = true
        
        println("USB preview started - Path: $previewPath, Source: $startSource, FPS: $fps, Mode: $previewMode, DataFlow: $dataFlowMode")
        
        // Simulate successful start
        return 0
    }
    
    override fun startY16ModePreview(previewPath: Any?, srcType: Any?, frameCallback: Any?): Int {
        // USB Y16 mode preview stub
        if (!isInitialized) return -1
        isPreviewRunning = true
        println("Y16 mode preview started - Path: $previewPath, Type: $srcType")
        return 0
    }
    
    override fun isTempReplacedWithTNREnabled(param: Any?): Boolean {
        // Temperature replacement with TNR status stub
        println("Checking TNR status with param: $param")
        return false
    }
    
    override fun stopPreview(): Int {
        // USB stop preview stub
        isPreviewRunning = false
        println("USB preview stopped")
        return 0 // Return success code
    }
    
    override fun stopPreview(previewPath: Any?): Int {
        // USB stop preview with path stub
        isPreviewRunning = false
        println("USB preview stopped for path: $previewPath")
        return 0 // Return success code
    }
    
    override fun setPropImageParams(param: Any?, value: Any?) {
        // Image parameters setting stub
        println("Setting image parameter: $param = $value")
    }
    
    override fun setPropAutoShutterParameter(param: Any?, value: Any?) {
        // Auto shutter parameter setting stub
        println("Setting auto shutter parameter: $param = $value")
    }
    
    override fun autoGainSwitch(
        normalTempData: ByteArray,
        imageRes: Any?,
        info: Any?,
        param: Any?,
        callback: Any?
    ) {
        // Auto gain switch stub - minimal implementation
        gainSwitchEnabled = true
        println("Auto gain switch enabled - Temp data size: ${normalTempData.size}, ImageRes: $imageRes")
        
        // Simulate callback execution if provided
        try {
            callback?.let {
                if (it is Function1<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    (it as Function1<Boolean, Unit>).invoke(true)
                }
            }
        } catch (e: Exception) {
            println("Callback execution error: ${e.message}")
        }
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
        println("Overexposure avoidance: $enable, GainStatus: $gainStatus, PixelProp: $pixelAboveProp")
        
        // Simulate processing
        if (enable && pixelAboveProp > 0.9f) {
            println("High overexposure detected, adjusting gain")
        }
        
        // Simulate callback execution
        try {
            callback?.let {
                if (it is Function1<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    (it as Function1<Boolean, Unit>).invoke(true)
                }
            }
        } catch (e: Exception) {
            println("Overexposure callback error: ${e.message}")
        }
    }
    
    override fun close() {
        // Close stub
        isPreviewRunning = false
        println("USB IRCMD closed")
    }
    
    override fun onDestroy() {
        // Destroy stub
        isInitialized = false
        isPreviewRunning = false
        gainSwitchEnabled = false
        println("USB IRCMD destroyed")
    }
}

private class WifiIRCMDImpl : IRCMD {
    override fun init(controlBlock: Any?) {}
    override fun startPreview(dataFlowMode: Any?, frameCallback: Any?): Int = 0
    override fun startPreview(previewPath: Any?, startSource: Any?, fps: Int, previewMode: Any?, dataFlowMode: Any?): Int = 0
    override fun startY16ModePreview(previewPath: Any?, srcType: Any?, frameCallback: Any?): Int = 0
    override fun isTempReplacedWithTNREnabled(param: Any?): Boolean = false
    override fun stopPreview(): Int = 0
    override fun stopPreview(previewPath: Any?): Int = 0
    override fun setPropImageParams(param: Any?, value: Any?) {}
    override fun setPropAutoShutterParameter(param: Any?, value: Any?) {}
    override fun autoGainSwitch(normalTempData: ByteArray, imageRes: Any?, info: Any?, param: Any?, callback: Any?) {}
    override fun avoidOverexposure(enable: Boolean, gainStatus: Any?, normalTempData: ByteArray, imageRes: Any?, lowGainTempData: ByteArray, highGainTempData: ByteArray, pixelAboveProp: Float, switchFrameCnt: Int, closeFrameCnt: Int, callback: Any?) {}
    override fun close() {}
    override fun onDestroy() {}
}

private class BleIRCMDImpl : IRCMD {
    override fun init(controlBlock: Any?) {}
    override fun startPreview(dataFlowMode: Any?, frameCallback: Any?): Int = 0
    override fun startPreview(previewPath: Any?, startSource: Any?, fps: Int, previewMode: Any?, dataFlowMode: Any?): Int = 0
    override fun startY16ModePreview(previewPath: Any?, srcType: Any?, frameCallback: Any?): Int = 0
    override fun isTempReplacedWithTNREnabled(param: Any?): Boolean = false
    override fun stopPreview(): Int = 0
    override fun stopPreview(previewPath: Any?): Int = 0
    override fun setPropImageParams(param: Any?, value: Any?) {}
    override fun setPropAutoShutterParameter(param: Any?, value: Any?) {}
    override fun autoGainSwitch(normalTempData: ByteArray, imageRes: Any?, info: Any?, param: Any?, callback: Any?) {}
    override fun avoidOverexposure(enable: Boolean, gainStatus: Any?, normalTempData: ByteArray, imageRes: Any?, lowGainTempData: ByteArray, highGainTempData: ByteArray, pixelAboveProp: Float, switchFrameCnt: Int, closeFrameCnt: Int, callback: Any?) {}
    override fun close() {}
    override fun onDestroy() {}
}