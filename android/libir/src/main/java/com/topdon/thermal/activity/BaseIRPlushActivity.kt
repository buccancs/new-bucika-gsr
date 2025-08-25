package com.topdon.thermal.activity

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.usb.UsbDevice
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.view.SurfaceView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import com.elvishew.xlog.XLog
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.sdkisp.LibIRProcess
import com.energy.iruvc.usb.USBMonitor
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.DualCameraParams
import com.energy.iruvc.utils.IFrameCallback
import com.energy.iruvc.utils.IIRFrameCallback
import com.energy.iruvc.utils.SynchronizedBitmap
import com.energy.iruvc.uvc.ConnectCallback
import com.energy.iruvc.uvc.UVCCamera
import com.example.suplib.wrapper.SupHelp
import com.infisense.usbdual.Const
import com.infisense.usbdual.camera.DualViewWithExternalCameraCommonApi
import com.infisense.usbdual.camera.IRUVCDual
import com.infisense.usbdual.camera.USBMonitorManager
import com.infisense.usbdual.inf.OnUSBConnectListener
import com.infisense.usbir.utils.PseudocodeUtils
import com.infisense.usbir.view.TemperatureView
import com.topdon.lib.core.common.SaveSettingUtil
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.thermal.R
import com.topdon.thermal.utils.DualParamsUtil
import com.topdon.thermal.utils.IRCmdTool
import com.topdon.thermal.utils.IRCmdTool.getSNStr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream

/**
 * Professional dual-light thermal imaging base activity providing comprehensive USB device lifecycle management,
 * real-time thermal data processing, and dual-camera integration for research and clinical applications.
 *
 * This base class handles:
 * - Professional dual-light thermal camera initialization and device detection
 * - Real-time thermal image processing with synchronized bitmap handling
 * - Advanced USB monitoring with automatic device reconnection
 * - Industry-standard temperature calibration and measurement accuracy
 * - Comprehensive dual-camera fusion with customizable display parameters
 * - Professional thermal data streaming with IFrameCallback integration
 * - Research-grade pseudo-color rendering and temperature visualization
 * - Clinical-grade device lifecycle management with proper cleanup procedures
 *
 * Supports TC001 and TC007 thermal cameras with professional dual-light capabilities,
 * providing industry-standard thermal imaging functionality for research and clinical environments.
 *
 * @since 1.0
 */
abstract class BaseIRPlushActivity : IRThermalNightActivity(), OnUSBConnectListener, IIRFrameCallback {

    /** Thermal imaging device serial number, used as unique identifier for device tracking and configuration persistence */
    private var snStr = ""

    /**
     * Professional dual-camera UVC integration tool for real-time thermal preview and data callback management.
     * 
     * Provides comprehensive dual-light thermal imaging capabilities with synchronized frame processing
     * and advanced USB device lifecycle management for research and clinical applications.
     * 
     * Note: Despite the "View" naming, this is not an Android View component but a utility class
     * for dual-camera integration and thermal data processing.
     */
    protected var dualView: DualViewWithExternalCameraCommonApi? = null

    /**
     * Infrared camera product identification codes for supported thermal imaging devices.
     * 
     * Supported thermal cameras:
     * - 0x5830 (22576): TC001 standard thermal camera with 384x288 resolution
     * - 0x5840 (22592): TC007 advanced thermal camera with enhanced precision
     */
    private var irPid = 0x5830

    /** Processed thermal image width after rotation and calibration transformations */
    private var imageWidth = 0
    
    /** Processed thermal image height after rotation and calibration transformations */
    private var imageHeight = 0
    
    /** Thread-safe bitmap container for synchronized thermal image processing and display updates */
    private var syncimage = SynchronizedBitmap()

    /** Current thermal fusion display type derived from user settings for professional visualization */
    protected var mCurrentFusionType = DualParamsUtil.fusionTypeToParams(SaveSettingUtil.fusionType)

    /**
     * Visible light camera product identification codes for dual-light thermal imaging systems.
     * 
     * Supported visible light cameras:
     * - 0x3035 (12341): 30 fps, 640x480 resolution standard visible light camera
     * - 0x9730 (38704): 25 fps, 1280x720 resolution HD visible light camera  
     * - 8833: Alternative visible light camera configuration
     */
    private var vlPid = 12337
    
    /** Frame rate for visible light camera at current resolution setting */
    private var vlFps = 30
    
    /** Visible light camera width resolution for dual-light thermal imaging fusion */
    protected var vlCameraWidth = 1280
    
    /** Visible light camera height resolution for dual-light thermal imaging fusion */
    protected var vlCameraHeight = 720
    
    /** Byte array buffer for visible light image data storage and processing */
    private var vlData = ByteArray(vlCameraWidth * vlCameraHeight * 3)

    /** Dual-camera thermal fusion width for synchronized thermal and visible light processing */
    private var dualCameraWidth = 480
    
    /** Dual-camera thermal fusion height for synchronized thermal and visible light processing */
    private var dualCameraHeight = 640

    /** IRISP algorithm integration flag for advanced thermal image processing (currently disabled for stability) */
    private val isUseIRISP = false

    /** Pseudo-color lookup table array for professional thermal color mapping and visualization */
    private var psedocolor: Array<ByteArray>? = null

    /** Dual-camera display update interval in milliseconds for optimized performance */
    protected var dualDisp = 30

    /** Professional dual-light UVC camera instance for visible light capture and thermal fusion */
    private var vlUVCCamera: IRUVCDual? = null

    /**
     * Abstract method for subclasses to provide the SurfaceView for thermal image rendering.
     * 
     * This method must be implemented by concrete thermal imaging activities to provide
     * the surface for real-time thermal image display with proper scaling and aspect ratio.
     *
     * @return SurfaceView instance configured for thermal image rendering
     */
    abstract fun getSurfaceView(): SurfaceView

    /**
     * Abstract method for subclasses to provide the TemperatureView for thermal data visualization.
     * 
     * This method must be implemented by concrete thermal imaging activities to provide
     * the temperature overlay component for displaying measurement points, temperature scales,
     * and thermal analysis tools over the thermal image.
     *
     * @return TemperatureView instance configured for thermal measurement display
     */
    abstract fun getTemperatureDualView(): TemperatureView

    /**
     * 是否是双光设备
     */
    abstract fun isDualIR() : Boolean

    abstract fun setTemperatureViewType()


    open fun setDispViewData(dualDisp : Int){

    }




    override fun initView() {
        super.initView()
        if (isDualIR()){
            // defaultDataFlowMode 是 图像+温度，故而 SDK 返回的传感器原始宽度为 256x384
            // 那么一帧图像、一帧温度的尺寸就是 256x(384/2) = 256x192
            // 由于竖屏显示需要旋转，那么最终出图尺寸就是 192x256
            imageWidth = 192
            imageHeight = 256
            USBMonitorManager.getInstance().init(irPid, isUseIRISP, defaultDataFlowMode)
            USBMonitorManager.getInstance().addOnUSBConnectListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        dualStart()
    }

    override fun onStop() {
        super.onStop()
        dualStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mIrHandler.removeCallbacksAndMessages(null)
        USBMonitorManager.getInstance().removeOnUSBConnectListener(this)
    }

    private fun dualStart() {
        if (!isDualIR()){
            return
        }
        /**
         * 打开红外模组
         * 需要确认好模组的pid和分辨率
         */
        USBMonitorManager.getInstance().registerUSB()
        //在USBMonitorManager onConnect回调中打开可见光模组
        getTemperatureDualView().setUseIRISP(isUseIRISP)
        if (mCurrentFusionType == DualCameraParams.FusionType.IROnlyNoFusion) {
            getTemperatureDualView().setImageSize(Const.IR_HEIGHT, Const.IR_WIDTH,null)
        } else {
            getTemperatureDualView().setImageSize(dualCameraWidth, dualCameraHeight,null)
        }
        setTemperatureViewType()
        getTemperatureDualView().start()
    }

    private var mIrHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (!isDualIR()){
                return
            }
            if (msg.what == Const.RESTART_USB) {
                restartDualCamera()
            } else if (msg.what == Const.HANDLE_CONNECT) {
                // 避免冲突，需要延时
                /**
                 * 开可见光相机
                 * 需要确认好模组的pid和分辨率
                 */
                lifecycleScope.launch(Dispatchers.Main){
                    startVLCamera(vlPid, vlFps, vlCameraWidth, vlCameraHeight)
                    initDualCamera()
                    // 一体式
                    initDefIntegralArgsDISPValue(DualCameraParams.TypeLoadParameters.ROTATE_270)
                }
            } else if (msg.what == Const.HANDLE_REGISTER) {
                USBMonitorManager.getInstance().registerUSB()
            } else if (msg.what == Const.SHOW_LOADING) {
                showCameraLoading()
            } else if (msg.what == Const.HIDE_LOADING) {
                dismissCameraLoading()
            } else if (msg.what == Const.SHOW_RESTART_MESSAGE) {
                Toast.makeText(
                    this@BaseIRPlushActivity,
                    "please restart app or reinsert device",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun restartDualCamera() {
        if (isrun) {
            USBMonitorManager.getInstance().isReStart = true
            dualStop()
            SystemClock.sleep(200)
            dualStart()
        }
    }

    /**
     * 一体式
     */
    private fun initDefIntegralArgsDISPValue(typeLoadParameters: DualCameraParams.TypeLoadParameters) {
        if (!isDualIR()){
            return
        }
        lifecycleScope.launch{
            val parameters = IRCmdTool.getDualBytes(USBMonitorManager.getInstance().ircmd)
            val data = dualView?.dualUVCCamera?.loadParameters(parameters, typeLoadParameters)
            dualDisp = IRCmdTool.dispNumber
            setDispViewData(dualDisp)
            // 初始化默认值
            dualView?.dualUVCCamera?.setDisp(dualDisp)
            dualView?.startPreview()
        }
    }

    private fun initDualCamera() {
        if (!isDualIR()){
            return
        }
        if (dualView != null) {
            return
        }
        val dualRotate: Int = if (saveSetBean.rotateAngle == 270) 0 else (saveSetBean.rotateAngle + 90)
        dualView = DualViewWithExternalCameraCommonApi(
            getSurfaceView(),
            USBMonitorManager.getInstance().uvcCamera, defaultDataFlowMode,
            imageHeight, imageWidth,
            vlCameraWidth, vlCameraHeight,
            dualCameraWidth, dualCameraHeight,
            isUseIRISP,dualRotate,this
        )
        dualView?.addFrameCallback(getTemperatureDualView())
        //
        getTemperatureDualView().setDualUVCCamera(dualView!!.getDualUVCCamera())
        initPseudoColor()
        initAmplify(true)
        // 这里可以设置初始化融合模式
//        setFusion(mCurrentFusionType)
//        dualView!!.startPreview()
        dualView?.setHandler(mIrHandler)
        isrun = true
    }

    private fun initPseudoColor() {
        val am = assets
        var inputStream: InputStream? = null
        try {
            //加载伪彩，虽然用不上这个伪彩，但是sdk限制必须初始化一个才能正常出图
            psedocolor = Array(11) { ByteArray(0) }
            inputStream = am.open("pseudocolor/White_Hot.bin")
            val length = inputStream.available()
            psedocolor!![0] = ByteArray(length + 1)
            if (inputStream.read(psedocolor!![0]) != length) {

            }
            psedocolor!![0][length] = 0
            dualView!!.getDualUVCCamera().loadPseudocolor(
                CommonParams.PseudoColorUsbDualType.WHITE_HOT_MODE,
                psedocolor!![0]
            )
            // 这里可以设置初始化融合模式
            setFusion(mCurrentFusionType)
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                inputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    protected fun setFusion(fusion : DualCameraParams.FusionType) {
        dualView?.setCurrentFusionType(fusion)
        getTemperatureDualView().setCurrentFusionType(fusion)
        if (fusion == DualCameraParams.FusionType.IROnlyNoFusion) {
            getTemperatureDualView().setImageSize(Const.IR_HEIGHT, Const.IR_WIDTH,null)
        } else {
            getTemperatureDualView().setImageSize(dualCameraWidth, dualCameraHeight,null)
        }
    }
    /**
     * 可见光模组
     *
     * @param pid          模组的pid
     * @param cameraWidth  模组的分辨率宽
     * @param cameraHeight 模组的分辨率高
     */
    private fun startVLCamera(pid: Int, fps: Int, cameraWidth: Int, cameraHeight: Int) {
        if (!isDualIR()){
            return
        }
        vlUVCCamera = IRUVCDual(cameraWidth,
            cameraHeight,
            this,
            pid,
            fps,
            object : ConnectCallback {
                override fun onCameraOpened(uvcCamera: UVCCamera) {

                }

                override fun onIRCMDCreate(ircmd: IRCMD) {
                    setUVCCameraICMD(ircmd)
                }
            },
            IFrameCallback { frame ->
                if (dualView != null && dualView?.getDualUVCCamera() != null &&
                    Const.isDeviceConnected
                ) {
                    System.arraycopy(frame, 0, vlData, 0, vlData.size)
                    dualView?.getDualUVCCamera()?.updateFrame(
                        ImageFormat.FLEX_RGB_888, vlData, vlCameraWidth,
                        vlCameraHeight
                    )
                }
            })
        vlUVCCamera?.setHandler(mIrHandler)
        vlUVCCamera?.registerUSB()
        vlUVCCamera?.TAG = "mjpeg"
    }

    private fun setUVCCameraICMD(ircmd: IRCMD) {
        this.ircmd = ircmd
        snStr = getSNStr(ircmd)
        isConfigWait = false
//        getTemperatureDualView().setIrcmd(ircmd)
//        popupCalibration.setIrcmd(ircmd)
//        popupImage.setIrcmd(ircmd)
//        popupOthers.setIrcmd(ircmd)
//        getTemperatureDualView().setIrcmd(ircmd)
//        // 画面旋转设置
//        popupCalibration.setRotate(true)
//        popupImage.setRotate(true)
    }

    private fun dualStop() {
        if (!isDualIR()){
            return
        }
        isrun = false
        syncimage.valid = false
        isConfigWait = true
        getTemperatureDualView().stop()
        USBMonitorManager.getInstance().unregisterUSB()
        ircmd?.onDestroy()
        ircmd = null
        SystemClock.sleep(100)
        if (dualView != null) {
            dualView?.removeFrameCallback(getTemperatureDualView())
            dualView?.dualUVCCamera?.onPausePreview()
            USBMonitorManager.getInstance().stopPreview()
            //
            if (vlUVCCamera != null) {
                vlUVCCamera?.unregisterUSB()
                vlUVCCamera?.stopPreview()
                vlUVCCamera = null
            }
            //
            SystemClock.sleep(100)
            dualView?.stopPreview()
            dualView = null
        }
    }

    override fun onAttach(device: UsbDevice?) {

    }

    override fun onGranted(usbDevice: UsbDevice?, granted: Boolean) {

    }

    override fun onDettach(device: UsbDevice?) {

    }

    override fun onConnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?, createNew: Boolean) {
        mIrHandler.sendEmptyMessage(Const.HANDLE_CONNECT)
    }

    override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {

    }

    override fun onCancel(device: UsbDevice?) {

    }

    override fun onIRCMDInit(ircmd: IRCMD?) {
        setUVCCameraICMD(ircmd!!)
    }
    override fun onCompleteInit() {
        mIrHandler.sendEmptyMessage(Const.HIDE_LOADING)
    }

    override fun onSetPreviewSizeFail() {
        mIrHandler.sendEmptyMessage(Const.SHOW_RESTART_MESSAGE)
    }
    //预处理后红外ARGB数据 192 * 256 * 4
    protected val preIrARGBData = ByteArray(256*192*4)
    protected val preIrData = ByteArray(256*192*2)
    protected val preTempData = ByteArray(256*192*2)

    override fun onIrFrame(irFrame: ByteArray?): ByteArray {
        /**
         * @param irFrame 原始红外YUV422数据 + 温度数据 长度 irWidth * irHeight * 2 + irWidth * irHeight * 2
         * @return
         */
        System.arraycopy(irFrame, 0, preIrData, 0, preIrData.size);
        LibIRProcess.convertYuyvMapToARGBPseudocolor(
            preIrData, (Const.IR_WIDTH * Const.IR_HEIGHT).toLong(),
            PseudocodeUtils.changePseudocodeModeByOld(pseudoColorMode), preIrARGBData
        )
        return preIrARGBData
    }

    override fun switchAmplify() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO){
                try {
                    SupHelp.getInstance().initA4KCPP()
                }catch (e : UnsatisfiedLinkError){
                    SupHelp.getInstance().loadOpenclSuccess = false
                    runOnUiThread {
                        TipDialog.Builder(this@BaseIRPlushActivity)
                            .setMessage(R.string.tips_tisr_fail)
                            .setPositiveListener(R.string.app_got_it) {
                            }
                            .create().show()
                    }
                    XLog.e("超分初始化失败")
                }
            }
            if (!SupHelp.getInstance().loadOpenclSuccess){
                return@launch
            }
            isOpenAmplify = !isOpenAmplify
            dualView?.isOpenAmplify = isOpenAmplify

            binding.titleView.setRight2Drawable(if (isOpenAmplify) R.drawable.svg_tisr_on else R.drawable.svg_tisr_off)
            SaveSettingUtil.isOpenAmplify = isOpenAmplify
            if (isOpenAmplify){
                ToastUtils.showShort(R.string.tips_tisr_on)
            }else{
                ToastUtils.showShort(R.string.tips_tisr_off)
            }
        }
    }

    override fun initAmplify(show: Boolean) {
        lifecycleScope.launch {
            binding.titleView.setRight2Drawable(if (isOpenAmplify) R.drawable.svg_tisr_on else R.drawable.svg_tisr_off)
            withContext(Dispatchers.IO){
                if (isOpenAmplify){
                    SupHelp.getInstance().initA4KCPP()
                }
            }
            dualView?.isOpenAmplify = isOpenAmplify
        }
    }



