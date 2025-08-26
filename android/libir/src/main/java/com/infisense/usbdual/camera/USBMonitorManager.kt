package com.infisense.usbdual.camera

import android.hardware.usb.UsbDevice
import android.os.SystemClock
import android.util.Log
import com.blankj.utilcode.util.Utils
import com.energy.iruvc.ircmd.ConcreteIRCMDBuilder
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.ircmd.IRCMDType
import com.energy.iruvc.usb.USBMonitor
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.DeviceType
import com.energy.iruvc.uvc.CameraSize
import com.energy.iruvc.uvc.ConcreateUVCBuilder
import com.energy.iruvc.uvc.UVCCamera
import com.energy.iruvc.uvc.UVCType
import com.infisense.usbdual.Const
import com.infisense.usbdual.inf.OnUSBConnectListener

object USBMonitorManager {
    
    const val TAG = "USBMonitorManager"
    
    // For compatibility with getInstance() calls
    @JvmStatic
    fun getInstance(): USBMonitorManager = this
    
    private var mUSBMonitor: USBMonitor? = null
    private var mUvcCamera: UVCCamera? = null
    private var mIrcmd: IRCMD? = null
    private var mDefaultDataFlowMode = CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT
    
    private var gainMode = CommonParams.GainMode.GAIN_MODE_HIGH_LOW
    private var isUseIRISP = false
    private var isUseGPU = true
    private var cameraWidth = 0
    private var cameraHeight = 0
    private var tau_data_H: ByteArray? = null
    private var tau_data_L: ByteArray? = null
    private var tempinfo = 0L
    private val nuc_table_high = ShortArray(8192)
    private val nuc_table_low = ShortArray(8192)
    
    private val priv_high = ByteArray(1201)
    private val priv_low = ByteArray(1201)
    private val kt_high = ShortArray(1201)
    private val kt_low = ShortArray(1201)
    private val bt_high = ShortArray(1201)
    private val bt_low = ShortArray(1201)
    private var isGetNucFromFlash = false
    
    private val mOnUSBConnectListeners = mutableListOf<OnUSBConnectListener>()
    
    private var deviceType = DeviceType.DUAL_DEVICE
    private var isReStart = false
    
    fun addOnUSBConnectListener(onUSBConnectListener: OnUSBConnectListener) {
        mOnUSBConnectListeners.add(onUSBConnectListener)
    }
    
    fun removeOnUSBConnectListener(onUSBConnectListener: OnUSBConnectListener) {
        mOnUSBConnectListeners.remove(onUSBConnectListener)
    }
    
    fun isReStart(): Boolean = isReStart
    
    fun setReStart(reStart: Boolean) {
        isReStart = reStart
    }
    
    fun init(pid: Int, isUseIRISP: Boolean, defaultDataFlowMode: CommonParams.DataFlowMode) {
        this.isUseIRISP = isUseIRISP
        mDefaultDataFlowMode = defaultDataFlowMode
        
        if (mUSBMonitor == null) {
            mUSBMonitor = USBMonitor(Utils.getApp(), object : USBMonitor.OnDeviceConnectListener {
                
                override fun onAttach(device: UsbDevice) {
                    Log.w(TAG, "USBMonitor-onAttach-getProductId = ${device.productId}")
                    mUSBMonitor?.requestPermission(device)
                    mOnUSBConnectListeners.forEach { listener ->
                        listener.onAttach(device)
                    }
                }
                
                override fun onGranted(usbDevice: UsbDevice, granted: Boolean) {
                    Log.d(TAG, "USBMonitor-onGranted")
                    mOnUSBConnectListeners.forEach { listener ->
                        listener.onGranted(usbDevice, granted)
                    }
                }
                
                override fun onDettach(device: UsbDevice) {
                    Log.d(TAG, "USBMonitor-onDettach")
                    Const.isDeviceConnected = false
                    mOnUSBConnectListeners.forEach { listener ->
                        listener.onDettach(device)
                    }
                }
                
                override fun onConnect(
                    device: UsbDevice,
                    ctrlBlock: USBMonitor.UsbControlBlock,
                    createNew: Boolean
                ) {
                    Log.w(TAG, "USBMonitor-onConnect createNew $createNew")
                    Log.w(TAG, "USBMonitor-onConnect Pid ${device.productId}")
                    
                    if (device.productId == pid) {
                        openUVCCamera(device, ctrlBlock, createNew)
                        mOnUSBConnectListeners.forEach { listener ->
                            listener.onConnect(device, ctrlBlock, createNew)
                        }
                    }
                }
                
                override fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock) {
                    Log.w(TAG, "USBMonitor-onDisconnect")
                    Const.isDeviceConnected = false
                    mOnUSBConnectListeners.forEach { listener ->
                        listener.onDisconnect(device, ctrlBlock)
                    }
                }
            })
            mUSBMonitor?.register()
        }
    }
    
    private fun openUVCCamera(
        device: UsbDevice,
        controlBlock: USBMonitor.UsbControlBlock,
        createNew: Boolean
    ) {
        Log.w(TAG, "USBMonitor-onConnect ${device.productId}")
        
        if (mUvcCamera == null) {
            val concreateUVCBuilder = ConcreateUVCBuilder()
            mUvcCamera = concreateUVCBuilder
                .setUVCType(UVCType.USB_UVC)
                .build()
            
            val concreteIRCMDBuilder = ConcreteIRCMDBuilder()
            mIrcmd = concreteIRCMDBuilder
                .setIRCMDType(IRCMDType.USB_IRCMD)
                .build()
            
            mUvcCamera?.apply {
                setDefaultPreviewMode(CommonParams.FRAMEFORMATType.FRAME_FORMAT_YUYV)
                setDefaultBandwidth(13.0f)
                setDefaultPreviewMaxFps(25)
                openUVCCamera(controlBlock)
                
                val supportedSizeList = supportSizeList
                var cameraSize: CameraSize? = null
                
                for (size in supportedSizeList) {
                    Log.i(TAG, "getSupportSizeList width = ${size.width}, height = ${size.height}")
                    if (size.width == 256 && size.height == 192) {
                        cameraSize = size
                        break
                    }
                }
                
                if (cameraSize != null) {
                    cameraWidth = cameraSize.width
                    cameraHeight = cameraSize.height
                    setUSBPreviewSize(cameraWidth, cameraHeight)
                } else {
                    cameraWidth = 256
                    cameraHeight = 192
                    setUSBPreviewSize(cameraWidth, cameraHeight)
                }
                
                onStartPreview()
            }
            
            mIrcmd?.init(controlBlock)
            startPreview()
            
            Log.w(TAG, "USBMonitor-onConnect complete ${device.productId}")
        }
    }
    
    fun getUvcCamera(): UVCCamera? = mUvcCamera
    
    fun getIrcmd(): IRCMD? = mIrcmd
    
    fun getDeviceType(): DeviceType = deviceType
    
    fun setDeviceType(deviceType: DeviceType) {
        this.deviceType = deviceType
    }
    
    fun getCameraWidth(): Int = cameraWidth
    
    fun getCameraHeight(): Int = cameraHeight
    
    fun setGainMode(gainMode: CommonParams.GainMode) {
        this.gainMode = gainMode
    }
    
    fun getGainMode(): CommonParams.GainMode = gainMode
    
    fun setUseGPU(useGPU: Boolean) {
        isUseGPU = useGPU
    }
    
    fun isUseGPU(): Boolean = isUseGPU
    
    fun setTauData(tauDataH: ByteArray?, tauDataL: ByteArray?) {
        tau_data_H = tauDataH
        tau_data_L = tauDataL
    }
    
    fun getTauDataH(): ByteArray? = tau_data_H
    
    fun getTauDataL(): ByteArray? = tau_data_L
    
    fun setTempinfo(tempinfo: Long) {
        this.tempinfo = tempinfo
    }
    
    fun getTempinfo(): Long = tempinfo
    
    fun getNucTableHigh(): ShortArray = nuc_table_high
    
    fun getNucTableLow(): ShortArray = nuc_table_low
    
    fun getPrivHigh(): ByteArray = priv_high
    
    fun getPrivLow(): ByteArray = priv_low
    
    fun getKtHigh(): ShortArray = kt_high
    
    fun getKtLow(): ShortArray = kt_low
    
    fun getBtHigh(): ShortArray = bt_high
    
    fun getBtLow(): ShortArray = bt_low
    
    fun setGetNucFromFlash(getNucFromFlash: Boolean) {
        isGetNucFromFlash = getNucFromFlash
    }
    
    fun isGetNucFromFlash(): Boolean = isGetNucFromFlash
    
    private fun startPreview() {
        if (isUseIRISP) {
            mIrcmd?.let { ircmd ->
                if (ircmd.startY16ModePreview(
                        CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                        CommonParams.Y16ModePreviewSrcType.Y16_MODE_TEMPERATURE
                    ) == 0
                ) {
                    ircmd.setPropImageParams(
                        CommonParams.PropImageParams.IMAGE_PROP_SEL_MIRROR_FLIP,
                        Const.IR_MIRROR_FLIP_TYPE
                    )
                }
            }
        } else {
            mIrcmd?.let { ircmd ->
                if (ircmd.startPreview(
                        CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                        CommonParams.StartPreviewSource.SOURCE_SENSOR,
                        25,
                        CommonParams.StartPreviewMode.VOC_DVP_MODE,
                        CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT
                    ) == 0
                ) {
                    ircmd.setPropImageParams(
                        CommonParams.PropImageParams.IMAGE_PROP_SEL_MIRROR_FLIP,
                        Const.IR_MIRROR_FLIP_TYPE
                    )
                }
            }
        }
    }
    
    fun stopPreview() {
        Log.i(TAG, "stopPreview")
        mUvcCamera?.let { camera ->
            if (camera.openStatus) {
                camera.onStopPreview()
            }
            SystemClock.sleep(200)
            camera.onDestroyPreview()
            mUvcCamera = null
        }
    }
    
    fun onPauseUvcPreview() {
        mUvcCamera?.onPausePreview()
    }
    
    fun onResumeUvcPreview() {
        mUvcCamera?.onResumePreview()
    }
}