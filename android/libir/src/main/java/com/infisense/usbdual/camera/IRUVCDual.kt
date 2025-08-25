package com.infisense.usbdual.camera

import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.TextureView
import com.energy.iruvc.dual.DualUVCCamera
import com.energy.iruvc.ircmd.ConcreteIRCMDBuilder
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.ircmd.IRCMDType
import com.energy.iruvc.sdkisp.LibIRProcess
import com.energy.iruvc.usb.DeviceFilter
import com.energy.iruvc.usb.USBMonitor
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.IFrameCallback
import com.energy.iruvc.utils.SynchronizedBitmap
import com.energy.iruvc.uvc.CameraSize
import com.energy.iruvc.uvc.ConcreateUVCBuilder
import com.energy.iruvc.uvc.ConnectCallback
import com.energy.iruvc.uvc.UVCCamera
import com.energy.iruvc.uvc.UVCType
import com.infisense.usbdual.Const
import com.infisense.usbir.R

class IRUVCDual {
    
    companion object {
        private const val TAG = "IRUVC"
    }
    
    private val mContext: Context
    private var iFrameCallback: IFrameCallback? = null
    private var uvcCamera: UVCCamera? = null
    private var ircmd: IRCMD? = null
    private var mUSBMonitor: USBMonitor? = null
    private val cameraWidth: Int
    private val cameraHeight: Int
    private var image: ByteArray? = null
    private var temperature: ByteArray? = null
    private var syncimage: SynchronizedBitmap? = null
    private var cameraview: TextureView? = null
    private var status = 0
    private var isRequest = false
    private var mPid = 0
    private var vid = 0
    private var mFps = 0
    private var auto_gain_switch = false
    private var auto_over_protect = false
    private val auto_gain_switch_info = LibIRProcess.AutoGainSwitchInfo_t()
    private val gain_switch_param = LibIRProcess.GainSwitchParam_t()

    private var isUseIRISP = false
    private var isUseGPU = false

    private var gainStatus = CommonParams.GainStatus.HIGH_GAIN
    private var gainMode = CommonParams.GainMode.GAIN_MODE_HIGH_LOW
    private val nuc_table_high = ShortArray(8192)
    private val nuc_table_low = ShortArray(8192)
    private var isGetNucFromFlash = false

    private val priv_high = ByteArray(1201)
    private val priv_low = ByteArray(1201)
    private val kt_high = ShortArray(1201)
    private val kt_low = ShortArray(1201)
    private val bt_high = ShortArray(1201)
    private val bt_low = ShortArray(1201)

    private val curVtemp = IntArray(1)

    private var mConnectCallback: ConnectCallback? = null
    var dualUVCCamera: DualUVCCamera? = null
    private var pseudocolorMode: CommonParams.PseudoColorType? = null
    private var handler: Handler? = null
    var rotate = false

    constructor(
        cameraWidth: Int,
        cameraHeight: Int,
        context: Context,
        syncimage: SynchronizedBitmap?,
        connectCallback: ConnectCallback?
    ) {
        this.cameraWidth = cameraWidth
        this.cameraHeight = cameraHeight
        this.mContext = context
        this.syncimage = syncimage
        this.mConnectCallback = connectCallback
        
        initUVCCamera(cameraWidth, cameraHeight)
        
        mUSBMonitor = USBMonitor(context, object : USBMonitor.OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice) {
                if (mPid != 0) return
                Log.d(TAG, "onAttach")
                if (!isRequest) {
                    isRequest = true
                    requestPermission(0)
                }
            }

            override fun onGranted(usbDevice: UsbDevice, granted: Boolean) {}

            override fun onConnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
                Log.w(TAG, "onConnect")
                if (createNew) {
                    mConnectCallback?.let { callback ->
                        uvcCamera?.let { camera ->
                            Log.d(TAG, "onCameraOpened")
                            callback.onCameraOpened(camera)
                        }
                    }
                    Const.isDeviceConnected = true
                    handleUSBConnect(ctrlBlock)
                }
            }

            override fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock) {
                Log.w(TAG, "onDisconnect")
                Const.isDeviceConnected = false
            }

            override fun onDettach(device: UsbDevice) {
                Log.w(TAG, "onDettach$isRequest")
                Const.isDeviceConnected = false
                if (isRequest) {
                    isRequest = false
                    stopPreview()
                }
            }

            override fun onCancel(device: UsbDevice) {
                Const.isDeviceConnected = false
            }
        })

        initGainSwitchParams()
    }

    constructor(
        cameraWidth: Int,
        cameraHeight: Int,
        context: Context,
        pid: Int,
        fps: Int,
        connectCallback: ConnectCallback?,
        iFrameCallback: IFrameCallback?
    ) {
        this.mPid = pid
        this.mFps = fps
        this.cameraWidth = cameraWidth
        this.cameraHeight = cameraHeight
        this.mContext = context
        this.mConnectCallback = connectCallback
        this.iFrameCallback = iFrameCallback

        initUVCCamera(cameraWidth, cameraHeight)

        mUSBMonitor = USBMonitor(context, object : USBMonitor.OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice) {
                Log.w(TAG, "USBMonitor-onAttach mPid = $pid getProductId = ${device.productId}")
                
                if (device.productId != mPid) return
                
                if (uvcCamera?.openStatus != true) {
                    mUSBMonitor?.requestPermission(device)
                }
            }

            override fun onGranted(usbDevice: UsbDevice, granted: Boolean) {
                Log.w(TAG, "USBMonitor-onGranted")
            }

            override fun onDettach(device: UsbDevice) {
                Log.w(TAG, "USBMonitor-onDettach mPid = $pid")
                Const.isDeviceConnected = false
                if (uvcCamera?.openStatus == true) {
                    if (handler != null && status != 2) {
                        handler?.sendEmptyMessage(Const.RESTART_USB)
                    }
                    status = 2
                }
            }

            override fun onConnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
                Log.w(TAG, "USBMonitor-onConnect mPid = $pid")
                Log.w(TAG, "USBMonitor-onConnect createNew = $createNew")
                if (createNew && device.productId == pid) {
                    handler?.sendEmptyMessage(Const.SHOW_LOADING)

                    mConnectCallback?.let { callback ->
                        uvcCamera?.let { camera ->
                            Log.w(TAG, "USBMonitor-onCameraOpened")
                            callback.onCameraOpened(camera)
                        }
                    }
                    Const.isDeviceConnected = true
                    handleUSBConnect(ctrlBlock)
                    status = 3
                }
            }

            override fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock) {
                Log.w(TAG, "USBMonitor-onDisconnect mPid = $pid")
                Const.isDeviceConnected = false
                status = 4
            }

            override fun onCancel(device: UsbDevice) {
                Log.w(TAG, "USBMonitor-onCancel mPid = $pid")
                Const.isDeviceConnected = false
            }
        })
    }

    constructor(
        cameraWidth: Int,
        cameraHeight: Int,
        context: Context,
        syncimage: SynchronizedBitmap?,
        pid: Int,
        fps: Int,
        connectCallback: ConnectCallback?
    ) {
        this.mPid = pid
        this.mFps = fps
        this.cameraWidth = cameraWidth
        this.cameraHeight = cameraHeight
        this.mContext = context
        this.syncimage = syncimage
        this.mConnectCallback = connectCallback

        initUVCCamera(cameraWidth, cameraHeight)

        mUSBMonitor = USBMonitor(context, object : USBMonitor.OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice) {
                Log.w(TAG, "USBMonitor-onAttach mPid = $pid getProductId = ${device.productId}")
                
                if (device.productId != mPid) return
                
                if (uvcCamera?.openStatus != true) {
                    mUSBMonitor?.requestPermission(device)
                }
            }

            override fun onGranted(usbDevice: UsbDevice, granted: Boolean) {
                Log.w(TAG, "USBMonitor-onGranted")
            }

            override fun onDettach(device: UsbDevice) {
                Log.w(TAG, "USBMonitor-onDettach mPid = $pid")
                Const.isDeviceConnected = false
                if (uvcCamera?.openStatus == true) {
                    if (handler != null && status != 2) {
                        handler?.sendEmptyMessage(Const.RESTART_USB)
                    }
                    status = 2
                }
            }

            override fun onConnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
                Log.w(TAG, "USBMonitor-onConnect mPid = $pid")
                Log.w(TAG, "USBMonitor-onConnect createNew = $createNew")
                if (createNew && device.productId == pid) {
                    handler?.sendEmptyMessage(Const.SHOW_LOADING)

                    mConnectCallback?.let { callback ->
                        uvcCamera?.let { camera ->
                            Log.w(TAG, "USBMonitor-onCameraOpened")
                            callback.onCameraOpened(camera)
                        }
                    }
                    Const.isDeviceConnected = true
                    handleUSBConnect(ctrlBlock)
                    status = 3
                }
            }

            override fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock) {
                Log.w(TAG, "USBMonitor-onDisconnect mPid = $pid")
                Const.isDeviceConnected = false
                status = 4
            }

            override fun onCancel(device: UsbDevice) {
                Log.w(TAG, "USBMonitor-onCancel mPid = $pid")
                Const.isDeviceConnected = false
            }
        })
    }

    constructor(
        cameraWidth: Int,
        cameraHeight: Int,
        context: Context,
        syncimage: SynchronizedBitmap?,
        pid: Int,
        connectCallback: ConnectCallback?,
        isUseIRISP: Boolean
    ) {
        this.mPid = pid
        this.cameraWidth = cameraWidth
        this.cameraHeight = cameraHeight
        this.mContext = context
        this.syncimage = syncimage
        this.isUseIRISP = isUseIRISP
        this.mConnectCallback = connectCallback

        initUVCCamera(cameraWidth, cameraHeight)

        mUSBMonitor = USBMonitor(context, object : USBMonitor.OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice) {
                Log.w(TAG, "onAttach${device.productId}")
                if (pid != 0) {
                    if (uvcCamera?.openStatus != true) {
                        Log.w(TAG, "USBMonitor onAttach requestPermission $pid")
                        mUSBMonitor?.requestPermission(device)
                    }
                }
            }

            override fun onGranted(usbDevice: UsbDevice, granted: Boolean) {}

            override fun onDettach(device: UsbDevice) {
                Log.w(TAG, "onDettach")
                if (pid != 0 && device != null) {
                    Const.isDeviceConnected = false
                    if (uvcCamera?.openStatus == true) {
                        status = 2
                    }
                }
            }

            override fun onConnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
                Log.w(TAG, "onConnect")
                if (pid != 0) {
                    if (createNew) {
                        handler?.sendEmptyMessage(Const.SHOW_LOADING)

                        mConnectCallback?.let { callback ->
                            uvcCamera?.let { camera ->
                                Log.d(TAG, "onCameraOpened")
                                callback.onCameraOpened(camera)
                            }
                        }
                        Const.isDeviceConnected = true
                        handleUSBConnect(ctrlBlock)
                        status = 3
                    }
                }
            }

            override fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock) {
                Log.w(TAG, "onDisconnect")
                if (pid != 0 && status != 4) {
                    Const.isDeviceConnected = false
                    status = 4
                }
            }

            override fun onCancel(device: UsbDevice) {
                Const.isDeviceConnected = false
            }
        })
    }

    private fun initGainSwitchParams() {
        gain_switch_param.above_pixel_prop = 0.1f
        gain_switch_param.above_temp_data = ((130 + 273.15) * 16 * 4).toInt()
        gain_switch_param.below_pixel_prop = 0.95f
        gain_switch_param.below_temp_data = ((110 + 273.15) * 16 * 4).toInt()
        auto_gain_switch_info.switch_frame_cnt = 5 * 15
        auto_gain_switch_info.waiting_frame_cnt = 7 * 15

        val low_gain_over_temp_data = ((550 + 273.15) * 16 * 4).toInt()
        val high_gain_over_temp_data = ((100 + 273.15) * 16 * 4).toInt()
        val pixel_above_prop = 0.02f
        val switch_frame_cnt = 7 * 15
        val close_frame_cnt = 10 * 15
    }

    fun setDualUVCCamera(dualUVCCamera: DualUVCCamera?) {
        this.dualUVCCamera = dualUVCCamera
    }

    fun setPseudocolorMode(pseudocolorMode: CommonParams.PseudoColorType?) {
        this.pseudocolorMode = pseudocolorMode
    }

    fun setCameraview(cameraview: TextureView?) {
        this.cameraview = cameraview
    }

    fun setmPid(mPid: Int) {
        this.mPid = mPid
    }

    fun setVid(vid: Int) {
        this.vid = vid
    }

    fun setHandler(handler: Handler?) {
        this.handler = handler
    }

    fun setRotate(rotate: Boolean) {
        this.rotate = rotate
    }

    fun setImage(image: ByteArray?) {
        this.image = image
    }

    fun setTemperature(temperature: ByteArray?) {
        this.temperature = temperature
    }

    private fun initUVCCamera(cameraWidth: Int, cameraHeight: Int) {
        Log.i(TAG, "initUVCCamera->cameraWidth = $cameraWidth cameraHeight = $cameraHeight")

        val concreateUVCBuilder = ConcreateUVCBuilder()
        uvcCamera = concreateUVCBuilder
            .setUVCType(UVCType.USB_UVC)
            .build()
    }

    fun getUvcCamera(): UVCCamera? = uvcCamera

    fun getIrcmd(): IRCMD? = ircmd

    fun registerUSB() {
        Log.i(TAG, "registerUSB")
        mUSBMonitor?.register()
    }

    fun unregisterUSB() {
        Log.i(TAG, "unregisterUSB")
        mUSBMonitor?.destroy()
    }

    fun getUsbDeviceList(): List<UsbDevice>? {
        val deviceFilters = DeviceFilter.getDeviceFilters(mContext, R.xml.device_filter)
        if (mUSBMonitor == null || deviceFilters == null) return null
        
        return mUSBMonitor?.getDeviceList(deviceFilters)
    }

    fun requestPermission(index: Int): Boolean {
        Log.i(TAG, "requestPermission")
        val devList = getUsbDeviceList()
        if (devList == null || devList.isEmpty()) return false
        
        val count = devList.size
        if (index >= count) {
            IllegalArgumentException("index illegal,should be < devList.size()")
        }
        
        return mUSBMonitor?.let { monitor ->
            if (getUsbDeviceList()?.get(index)?.productId == mPid) {
                monitor.requestPermission(getUsbDeviceList()!![index])
            } else false
        } ?: false
    }

    fun openUVCCamera(ctrlBlock: USBMonitor.UsbControlBlock) {
        Log.i(TAG, "openUVCCamera")
        if (ctrlBlock.productId == 0x3901) {
            syncimage?.type = 1
        }
        if (uvcCamera == null) {
            initUVCCamera(cameraWidth, cameraHeight)
        }

        uvcCamera?.openUVCCamera(ctrlBlock)
    }

    fun startPreview() {
        Log.w(TAG, "startPreview mPid = $mPid isUseIRISP = $isUseIRISP")
        uvcCamera?.openStatus = true

        iFrameCallback?.let { callback ->
            uvcCamera?.setFrameCallback(callback)
        }
        uvcCamera?.onStartPreview()
        
        if (mPid == 0x5830 || mPid == 0x5840) {
            ircmd?.startPreview(
                CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                CommonParams.StartPreviewSource.SOURCE_SENSOR,
                25,
                CommonParams.StartPreviewMode.VOC_DVP_MODE,
                CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT
            )
            ircmd?.setPropImageParams(
                CommonParams.PropImageParams.IMAGE_PROP_SEL_MIRROR_FLIP,
                Const.IR_MIRROR_FLIP_TYPE
            )
        }
    }

    private fun getAllSupportedSize(): List<CameraSize> {
        val previewList = uvcCamera?.supportedSizeList ?: emptyList()
        previewList.forEach { size ->
            Log.i(TAG, "SupportedSize : ${size.width} * ${size.height}")
        }
        return previewList
    }

    fun initIRCMD(previewList: List<CameraSize>) {
        uvcCamera?.let { camera ->
            val concreteIRCMDBuilder = ConcreteIRCMDBuilder()
            ircmd = concreteIRCMDBuilder
                .setIrcmdType(IRCMDType.USB_IR_256_384)
                .setIdCamera(camera.nativePtr)
                .build()

            mConnectCallback?.let { callback ->
                Log.d(TAG, "onIRCMDCreate")
                callback.onIRCMDCreate(ircmd!!)
            }
        }
    }

    private fun setPreviewSize(cameraWidth: Int, cameraHeight: Int): Int {
        return uvcCamera?.let { camera ->
            Log.d(TAG, "setUSBPreviewSize mPid = $mPid cameraWidth = $cameraWidth cameraHeight = $cameraHeight")
            camera.setUSBPreviewSize(cameraWidth, cameraHeight)
        } ?: -1
    }

    fun stopPreview() {
        Log.i(TAG, "stopPreview")
        uvcCamera?.let { camera ->
            if (camera.openStatus) {
                camera.onStopPreview()
            }
            camera.setFrameCallback(null)
            SystemClock.sleep(200)
            camera.onDestroyPreview()
            uvcCamera = null
        }
    }

    fun setConnectCallback(connectCallback: ConnectCallback?) {
        Log.d(TAG, "setConnectCallback")
        this.mConnectCallback = connectCallback
    }

    private fun handleUSBConnect(ctrlBlock: USBMonitor.UsbControlBlock) {
        Log.d(TAG, "handleUSBConnect mPid = $mPid")
        openUVCCamera(ctrlBlock)

        val previewList = getAllSupportedSize()

        uvcCamera?.let { camera ->
            when (mPid) {
                0x5830, 0x5840 -> {
                    initIRCMD(previewList)
                    camera.setDefaultBandwidth(1.0f)
                    camera.setDefaultPreviewMinFps(1)
                    camera.setDefaultPreviewMaxFps(mFps)
                }
                else -> {
                    Log.d(TAG, "startVLCamera handleUSBConnect mPid = $mPid setDefaultPreviewMode")
                    camera.setDefaultPreviewMode(CommonParams.FRAMEFORMATType.FRAME_FORMAT_MJPEG)
                    camera.setDefaultBandwidth(0.6f)
                    camera.setDefaultPreviewMinFps(1)
                    camera.setDefaultPreviewMaxFps(mFps)
                }
            }
        }

        val result = setPreviewSize(cameraWidth, cameraHeight)
        if (result == 0) {
            Log.d(TAG, "handleUSBConnect setPreviewSize success")
            startPreview()
        } else {
            Log.d(TAG, "handleUSBConnect setPreviewSize fail")
            stopPreview()
        }
    }
}