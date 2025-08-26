package com.infisense.usbir.camera

import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.SystemClock
import android.util.Log
import com.energy.iruvc.ircmd.ConcreteIRCMDBuilder
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.ircmd.IRCMDType
import com.energy.iruvc.sdkisp.LibIRProcess
import com.energy.iruvc.usb.USBMonitor
import com.energy.iruvc.utils.AutoGainSwitchCallback
import com.energy.iruvc.utils.AvoidOverexposureCallback
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.DeviceType
import com.energy.iruvc.utils.IFrameCallback
import com.energy.iruvc.utils.SynchronizedBitmap
import com.energy.iruvc.uvc.CameraSize
import com.energy.iruvc.uvc.ConcreateUVCBuilder
import com.energy.iruvc.uvc.ConnectCallback
import com.energy.iruvc.uvc.UVCCamera
import com.energy.iruvc.uvc.UVCType
import com.infisense.usbir.config.MsgCode
import com.infisense.usbir.event.IRMsgEvent
import com.infisense.usbir.event.PreviewComplete
import com.infisense.usbir.utils.FileUtil
import com.infisense.usbir.utils.ScreenUtils
import com.infisense.usbir.utils.USBMonitorCallback
import org.greenrobot.eventbus.EventBus

class IRUVCTC(
    private val cameraWidth: Int,
    private val cameraHeight: Int,
    context: Context,
    private val syncimage: SynchronizedBitmap?,
    dataFlowMode: CommonParams.DataFlowMode,
    private val mConnectCallback: ConnectCallback?,
    usbMonitorCallback: USBMonitorCallback?
) {
    
    companion object {
        private const val TAG = "IRUVC_DATA"
    }

    private val iFrameCallback: IFrameCallback
    var uvcCamera: UVCCamera? = null
        private set
    private var ircmd: IRCMD? = null

    private val mUSBMonitor: USBMonitor
    private var imageSrc: ByteArray? = null
    private var temperatureSrc: ByteArray? = null
    private val imageOrTempDataLength = 256 * 192 * 2
    
    private val auto_gain_switch_info = LibIRProcess.AutoGainSwitchInfo_t()
    private val gain_switch_param = LibIRProcess.GainSwitchParam_t()
    private var rotateInt = 0

    private var isFrameReady = true

    private val gainStatus = CommonParams.GainStatus.HIGH_GAIN
    private val temperatureTemp = ByteArray(imageOrTempDataLength)

    private var isTempReplacedWithTNREnabled = false
    private val defaultDataFlowMode: CommonParams.DataFlowMode = dataFlowMode
    private var isRestart = false
    var auto_gain_switch = false
    private val auto_over_portect = false
    var imageEditTemp: ByteArray? = null
    private val pids = intArrayOf(0x5840, 0x3901, 0x5830, 0x5838)
    private var iFrameCallBackListener: IFrameCallBackListener? = null
    private var iFrameReadListener: IFrameReadListener? = null

    @Volatile
    var isFirstFrame = false

    interface IFrameCallBackListener {
        fun updateData()
    }

    interface IFrameReadListener {
        fun frameRead()
    }

    fun setIFrameCallBackListener(listener: IFrameCallBackListener?) {
        this.iFrameCallBackListener = listener
    }

    fun setiFirstFrameListener(listener: IFrameReadListener?) {
        this.iFrameReadListener = listener
    }

    init {
        isFirstFrame = true
        initUVCCamera()

        mUSBMonitor = USBMonitor(context, object : USBMonitor.OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice) {
                Log.w(TAG, "onAttach")
                if (uvcCamera?.openStatus != true) {
                    mUSBMonitor.requestPermission(device)
                }
                usbMonitorCallback?.onAttach()
            }

            override fun onGranted(usbDevice: UsbDevice, granted: Boolean) {
                Log.w(TAG, "onGranted")
                usbMonitorCallback?.onGranted()
            }

            override fun onConnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
                Log.w(TAG, "onConnect")
                if (isIRpid(device.productId)) {
                    if (createNew) {
                        openUVCCamera(ctrlBlock)

                        val previewList = getAllSupportedSize()
                        previewList.forEach { size ->
                            Log.i(TAG, "SupportedSize : ${size.width} * ${size.height}")
                        }

                        initIRCMD()

                        ircmd?.let { cmd ->
                            Log.d(TAG, "startPreview")

                            isTempReplacedWithTNREnabled = cmd.isTempReplacedWithTNREnabled(DeviceType.P2)
                            
                            uvcCamera?.let { camera ->
                                val previewSize = if (isTempReplacedWithTNREnabled) {
                                    camera.setUSBPreviewSize(cameraWidth, cameraHeight * 2)
                                } else {
                                    camera.setUSBPreviewSize(cameraWidth, cameraHeight)
                                }
                            }
                            startPreview()
                        }

                        usbMonitorCallback?.onConnect()
                    }
                }
            }

            override fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock) {
                Log.w(TAG, "onDisconnect")
                usbMonitorCallback?.onDisconnect()
            }

            override fun onDettach(device: UsbDevice) {
                Log.w(TAG, "onDettach")
                if (uvcCamera?.openStatus == true) {
                    usbMonitorCallback?.onDettach()
                }
            }

            override fun onCancel(device: UsbDevice) {
                Log.w(TAG, "onCancel")
                usbMonitorCallback?.onCancel()
            }
        })

        initGainSwitchParams()

        val imageRes = LibIRProcess.ImageRes_t().apply {
            height = (if (dataFlowMode == CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT) cameraHeight / 2 else cameraHeight).toChar()
            width = cameraWidth.toChar()
        }

        iFrameCallback = object : IFrameCallback {
            override fun onFrame(frame: ByteArray) {
                if (!isFrameReady || syncimage == null) return
                
                syncimage.start = true

                synchronized(syncimage.dataLock) {
                    val length = frame.size - 1
                    if (frame[length] == 1.toByte()) {
                        EventBus.getDefault().post(IRMsgEvent(MsgCode.RESTART_USB))
                        return
                    }
                    
                    imageEditTemp?.let { temp ->
                        if (temp.size >= length) {
                            System.arraycopy(frame, 0, temp, 0, length)
                        }
                    }

                    when (dataFlowMode) {
                        CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT -> {
                            imageSrc?.let { img ->
                                System.arraycopy(frame, 0, img, 0, imageOrTempDataLength)
                            }
                            
                            if (length >= imageOrTempDataLength * 2) {
                                temperatureSrc?.let { tempSrc ->
                                    when (rotateInt) {
                                        270 -> {
                                            System.arraycopy(frame, imageOrTempDataLength, temperatureTemp, 0, imageOrTempDataLength)
                                            LibIRProcess.rotateRight90(temperatureTemp, imageRes, CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14, tempSrc)
                                        }
                                        90 -> {
                                            System.arraycopy(frame, imageOrTempDataLength, temperatureTemp, 0, imageOrTempDataLength)
                                            LibIRProcess.rotateLeft90(temperatureTemp, imageRes, CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14, tempSrc)
                                        }
                                        180 -> {
                                            System.arraycopy(frame, imageOrTempDataLength, temperatureTemp, 0, imageOrTempDataLength)
                                            LibIRProcess.rotate180(temperatureTemp, imageRes, CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14, tempSrc)
                                        }
                                        else -> {
                                            System.arraycopy(frame, imageOrTempDataLength, tempSrc, 0, imageOrTempDataLength)
                                        }
                                    }
                                }
                                
                                ircmd?.let { cmd ->
                                    if (auto_gain_switch) {
                                        temperatureSrc?.let { tempSrc ->
                                            cmd.autoGainSwitch(tempSrc, imageRes, auto_gain_switch_info, gain_switch_param,
                                                object : AutoGainSwitchCallback {
                                                    override fun onAutoGainSwitchState(gainselStatus: CommonParams.PropTPDParamsValue.GAINSELStatus) {
                                                        Log.i(TAG, "onAutoGainSwitchState->${gainselStatus.value}")
                                                    }

                                                    override fun onAutoGainSwitchResult(gainselStatus: CommonParams.PropTPDParamsValue.GAINSELStatus, result: Int) {
                                                        Log.i(TAG, "onAutoGainSwitchResult->${gainselStatus.value} result=$result")
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    if (auto_over_portect) {
                                        temperatureSrc?.let { tempSrc ->
                                            val low_gain_over_temp_data = ByteArray(320 * 240 * 2)
                                            val high_gain_over_temp_data = ByteArray(320 * 240 * 2)
                                            val pixel_above_prop = 0.02f
                                            val switch_frame_cnt = 7 * 15
                                            val close_frame_cnt = 10 * 15
                                            
                                            cmd.avoidOverexposure(false, gainStatus, tempSrc, imageRes,
                                                low_gain_over_temp_data, high_gain_over_temp_data, pixel_above_prop,
                                                switch_frame_cnt, close_frame_cnt,
                                                object : AvoidOverexposureCallback {
                                                    override fun onAvoidOverexposureState(avoidOverexpol: Boolean) {
                                                        Log.i(TAG, "onAvoidOverexposureState->avoidOverexpol=$avoidOverexpol")
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            imageSrc?.let { img ->
                                System.arraycopy(frame, 0, img, 0, imageOrTempDataLength)
                            }
                        }
                    }
                    
                    iFrameCallBackListener?.updateData()
                }
                
                if (isFirstFrame) {
                    iFrameReadListener?.frameRead()
                    isFirstFrame = false
                }
            }
        }
    }

    private fun initGainSwitchParams() {
        gain_switch_param.above_pixel_prop = 0.1f
        gain_switch_param.above_temp_data = ((130 + 273.15) * 16 * 4).toInt()
        gain_switch_param.below_pixel_prop = 0.95f
        gain_switch_param.below_temp_data = ((110 + 273.15) * 16 * 4).toInt()
        auto_gain_switch_info.switch_frame_cnt = 5 * 15
        auto_gain_switch_info.waiting_frame_cnt = 7 * 15
    }

    fun setRotate(rotateInt: Int) {
        this.rotateInt = rotateInt
    }

    fun setImageSrc(image: ByteArray?) {
        this.imageSrc = image
    }

    fun setTemperatureSrc(temperatureSrc: ByteArray?) {
        this.temperatureSrc = temperatureSrc
    }

    fun setFrameReady(frameReady: Boolean) {
        isFrameReady = frameReady
    }

    fun isRestart(): Boolean = isRestart

    fun setRestart(restart: Boolean) {
        isRestart = restart
    }

    private fun initUVCCamera() {
        Log.i(TAG, "uvcCamera create")
        uvcCamera = ConcreateUVCBuilder()
            .setUVCType(UVCType.USB_UVC)
            .build()
        
        uvcCamera?.setDefaultBandwidth(0.5f)
    }

    private fun initIRCMD() {
        uvcCamera?.let { camera ->
            ircmd = ConcreteIRCMDBuilder()
                .setIRCMDType(IRCMDType.USB_IR_256_384)
                .setIdCamera(camera.nativePtr)
                .build()

            if (ircmd == null) {
                EventBus.getDefault().post(PreviewComplete())
                return
            }
            
            mConnectCallback?.onIRCMDCreate(ircmd!!)
        }
    }

    fun registerUSB() {
        mUSBMonitor.register()
    }

    fun unregisterUSB() {
        mUSBMonitor.unregister()
    }

    private fun openUVCCamera(ctrlBlock: USBMonitor.UsbControlBlock) {
        Log.i(TAG, "openUVCCamera")
        if (ctrlBlock.productId == 0x3901) {
            syncimage?.type = 1
        }
        if (uvcCamera == null) {
            initUVCCamera()
        }

        uvcCamera?.let { camera ->
            if (camera.openUVCCamera(ctrlBlock) == 0) {
                mConnectCallback?.onCameraOpened(camera)
            }
        }
    }

    private fun getAllSupportedSize(): List<CameraSize> {
        val previewList = uvcCamera?.supportedSizeList ?: emptyList()
        Log.w(TAG, "getSupportedSize = ${uvcCamera?.supportedSize}")
        previewList.forEach { size ->
            Log.i(TAG, "SupportedSize : ${size.width} * ${size.height}")
        }
        return previewList
    }

    private fun isIRpid(devpid: Int): Boolean {
        return pids.contains(devpid)
    }

    private fun startPreview() {
        val cmd = ircmd ?: return
        
        Log.i(TAG, "startPreview isRestart : $isRestart defaultDataFlowMode : $defaultDataFlowMode")
        uvcCamera?.apply {
            openStatus = true
            setFrameCallback(iFrameCallback)
            onStartPreview()
        }

        when (defaultDataFlowMode) {
            CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT,
            CommonParams.DataFlowMode.IMAGE_OUTPUT -> {
                Log.i(TAG, "defaultDataFlowMode = IMAGE_AND_TEMP_OUTPUT or IMAGE_OUTPUT")
                
                setFrameReady(false)
                if (isRestart) {
                    if (cmd.stopPreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0) == 0) {
                        Log.i(TAG, "stopPreview complete")

                        if (cmd.startPreview(
                                CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                                CommonParams.StartPreviewSource.SOURCE_SENSOR,
                                ScreenUtils.getPreviewFPSByDataFlowMode(defaultDataFlowMode),
                                CommonParams.StartPreviewMode.VOC_DVP_MODE,
                                defaultDataFlowMode
                            ) == 0) {
                            Log.i(TAG, "startPreview complete")
                            handleStartPreviewComplete()
                        }
                    } else {
                        Log.e(TAG, "stopPreview error")
                    }
                } else {
                    handleStartPreviewComplete()
                }
            }
            else -> {
                setFrameReady(false)
                if (isRestart) {
                    handleRestartPreview(cmd)
                } else {
                    handleNormalPreview(cmd)
                }
            }
        }
    }

    private fun handleRestartPreview(cmd: IRCMD) {
        if (cmd.stopPreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0) == 0) {
            Log.i(TAG, "stopPreview complete 中间出图 restart")
            if (cmd.startPreview(
                    CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                    CommonParams.StartPreviewSource.SOURCE_SENSOR,
                    ScreenUtils.getPreviewFPSByDataFlowMode(defaultDataFlowMode),
                    CommonParams.StartPreviewMode.VOC_DVP_MODE, defaultDataFlowMode
                ) == 0) {
                Log.i(TAG, "startPreview complete 中间出图 restart")
                try {
                    Thread.sleep(1500)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                if (cmd.startY16ModePreview(
                        CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                        FileUtil.getY16SrcTypeByDataFlowMode(defaultDataFlowMode)
                    ) == 0) {
                    handleStartPreviewComplete()
                } else {
                    Log.e(TAG, "startY16ModePreview error 中间出图 restart")
                }
            } else {
                Log.e(TAG, "startPreview error 中间出图 restart")
            }
        } else {
            Log.e(TAG, "stopPreview error 中间出图 restart")
        }
    }

    private fun handleNormalPreview(cmd: IRCMD) {
        val isTempReplacedWithTNREnabled = cmd.isTempReplacedWithTNREnabled(DeviceType.P2)
        Log.i(TAG, "defaultDataFlowMode = others isTempReplacedWithTNREnabled = $isTempReplacedWithTNREnabled")
        
        if (isTempReplacedWithTNREnabled) {
            handleTNRPreview(cmd, "红外+TNR", CommonParams.DataFlowMode.TNR_OUTPUT)
        } else {
            handleTNRPreview(cmd, "单TNR", defaultDataFlowMode)
        }
    }

    private fun handleTNRPreview(cmd: IRCMD, logPrefix: String, y16DataFlowMode: CommonParams.DataFlowMode) {
        if (cmd.stopPreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0) == 0) {
            Log.i(TAG, "stopPreview complete $logPrefix")
            
            val startDataFlowMode = if (logPrefix == "红外+TNR") CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT else defaultDataFlowMode
            val fps = if (logPrefix == "红外+TNR") ScreenUtils.getPreviewFPSByDataFlowMode(CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT) else ScreenUtils.getPreviewFPSByDataFlowMode(defaultDataFlowMode)
            
            if (cmd.startPreview(
                    CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                    CommonParams.StartPreviewSource.SOURCE_SENSOR,
                    fps,
                    CommonParams.StartPreviewMode.VOC_DVP_MODE,
                    startDataFlowMode
                ) == 0) {
                Log.i(TAG, "startPreview complete $logPrefix")
                try {
                    Thread.sleep(1500)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                if (cmd.startY16ModePreview(
                        CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                        FileUtil.getY16SrcTypeByDataFlowMode(y16DataFlowMode)
                    ) == 0) {
                    handleStartPreviewComplete()
                } else {
                    Log.e(TAG, "startY16ModePreview error $logPrefix")
                }
            } else {
                Log.e(TAG, "startPreview error $logPrefix")
            }
        } else {
            Log.e(TAG, "stopPreview error $logPrefix")
        }
    }

    fun stopPreview() {
        Log.i(TAG, "stopPreview")
        uvcCamera?.let { camera ->
            if (camera.openStatus) {
                camera.onStopPreview()
            }
            camera.setFrameCallback(null)
            uvcCamera = null

            ircmd?.apply {
                onDestroy()
                ircmd = null
            }

            SystemClock.sleep(200)
            camera.onDestroyPreview()
        }
    }

    private fun handleStartPreviewComplete() {
        Thread { EventBus.getDefault().post(PreviewComplete()) }.start()
    }
}