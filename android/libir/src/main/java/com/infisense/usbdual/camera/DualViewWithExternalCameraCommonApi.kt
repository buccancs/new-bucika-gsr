package com.infisense.usbdual.camera

import android.graphics.Bitmap
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import android.view.ViewGroup
import com.energy.commonlibrary.view.SurfaceNativeWindow
import com.energy.iruvc.dual.ConcreateDualBuilder
import com.energy.iruvc.dual.DualType
import com.energy.iruvc.dual.DualUVCCamera
import com.energy.iruvc.sdkisp.LibIRParse
import com.energy.iruvc.sdkisp.LibIRProcess
import com.energy.iruvc.utils.AutoGainSwitchCallback
import com.energy.iruvc.utils.AvoidOverexposureCallback
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.DualCameraParams
import com.energy.iruvc.utils.IFrameCallback
import com.energy.iruvc.utils.IIRFrameCallback
import com.energy.iruvc.uvc.UVCCamera
import com.infisense.usbdual.Const
import com.infisense.usbdual.camera.IFrameData.FRAME_LEN
import com.infisense.usbir.utils.OpencvTools
import java.nio.ByteBuffer
import java.util.*

class DualViewWithExternalCameraCommonApi(
    val cameraview: SurfaceView,
    irUVCCamera: UVCCamera,
    dataFlowMode: CommonParams.DataFlowMode,
    irCameraWidth: Int,
    irCameraHeight: Int,
    vlCameraWidth: Int,
    vlCameraHeight: Int,
    dualCameraWidth: Int,
    dualCameraHeight: Int,
    private val isUseIRISP: Boolean,
    val rotate: Int,
    private val irFrameCallback: IIRFrameCallback
) : BaseDualView() {

    companion object {
        private const val TAG = "DualViewWithExternalCameraCommonApi"
        const val MULTIPLE = 2
    }

    // dualUVCCamera is inherited from BaseDualView, no need to redeclare
    private val iFrameCallback: IFrameCallback
    var isRun = true

    var count = 0
    private var timestart = 0L
    private var fps = 0.0

    var auto_gain_switch = false
    var auto_gain_switch_running = true
    var auto_over_protect = false
    private val auto_gain_switch_info = LibIRProcess.AutoGainSwitchInfo_t()
    private val gain_switch_param = LibIRProcess.GainSwitchParam_t()
    private var gainStatus = CommonParams.GainStatus.HIGH_GAIN
    var bitmap: Bitmap? = null
    var supIROlyNoFusionBitmap: Bitmap? = null
    var supMixBitmap: Bitmap? = null
    var supIROlyBitmap: Bitmap? = null

    private var valid = false
    private var mScaledBitmap: Bitmap? = null
    private var handler: Handler? = null

    private val mSurfaceNativeWindow = SurfaceNativeWindow()
    private var mSurface: Surface? = null

    private var mCurrentFusionType: DualCameraParams.FusionType? = null
    private var firstFrame = false
    private var irRGBAData: ByteArray
    private var preIrData: ByteArray
    private var preTempData: ByteArray
    private var preIrARGBData: ByteArray
    var frameData = ByteArray(FRAME_LEN)

    val frameIrAndTempData = ByteArray(192 * 256 * 4)

    @Volatile
    private var isOpenAmplify = false
    private val amplifyMixRotateArray: ByteArray
    private val amplifyIRRotateArray: ByteArray

    private var saveData = false

    fun isOpenAmplify(): Boolean = isOpenAmplify

    fun setOpenAmplify(openAmplify: Boolean) {
        isOpenAmplify = openAmplify
    }

    fun setHandler(handler: Handler?) {
        this.handler = handler
    }

    init {
        Const.CAMERA_WIDTH = vlCameraWidth
        Const.CAMERA_HEIGHT = vlCameraHeight
        Const.IR_WIDTH = irCameraHeight
        Const.IR_HEIGHT = irCameraWidth
        Const.VL_WIDTH = vlCameraHeight
        Const.VL_HEIGHT = vlCameraWidth
        Const.DUAL_WIDTH = dualCameraWidth
        Const.DUAL_HEIGHT = dualCameraHeight

        onFrameCallbacks = ArrayList()
        fusionLength = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 4
        irSize = Const.IR_WIDTH * Const.IR_HEIGHT
        vlSize = Const.VL_WIDTH * Const.VL_HEIGHT * 3
        remapTempSize = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2
        remapTempData = ByteArray(Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2)
        mixData = ByteArray(fusionLength)
        normalTempData = ByteArray(irSize * 2)
        irData = ByteArray(irSize * 2)
        vlData = ByteArray(vlSize)
        vlARGBData = ByteArray(fusionLength)
        amplifyMixRotateArray = ByteArray(fusionLength * MULTIPLE * MULTIPLE)
        amplifyIRRotateArray = ByteArray(irData.size * MULTIPLE * MULTIPLE)

        bitmap = Bitmap.createBitmap(dualCameraWidth, dualCameraHeight, Bitmap.Config.ARGB_8888)
        supIROlyNoFusionBitmap = Bitmap.createBitmap(
            irCameraWidth * MULTIPLE,
            irCameraHeight * MULTIPLE, Bitmap.Config.ARGB_8888
        )
        supIROlyBitmap = Bitmap.createBitmap(irCameraWidth, irCameraHeight, Bitmap.Config.ARGB_8888)
        supMixBitmap = Bitmap.createBitmap(
            Const.DUAL_WIDTH * MULTIPLE,
            Const.DUAL_HEIGHT * MULTIPLE, Bitmap.Config.ARGB_8888
        )

        val concreateDualBuilder = ConcreateDualBuilder()
        dualUVCCamera = concreateDualBuilder
            .setDualType(DualType.USB_DUAL)
            .setIRSize(Const.IR_WIDTH, Const.IR_HEIGHT)
            .setVLSize(Const.VL_WIDTH, Const.VL_HEIGHT)
            .setDualSize(Const.DUAL_HEIGHT, Const.DUAL_WIDTH)
            .setDataFlowMode(dataFlowMode)
            .setPreviewCameraStyle(CommonParams.PreviewCameraStyle.EXTERNAL_CAMERA)
            .setDeviceStyle(CommonParams.DeviceStyle.ALL_IN_ONE)
            .setUseDualGPU(false)
            .setMultiThreadHandleDualEnable(false)
            .build()

        val rotateT = when (rotate) {
            0 -> DualCameraParams.TypeLoadParameters.ROTATE_0
            90 -> DualCameraParams.TypeLoadParameters.ROTATE_90
            180 -> DualCameraParams.TypeLoadParameters.ROTATE_180
            270 -> DualCameraParams.TypeLoadParameters.ROTATE_270
            else -> DualCameraParams.TypeLoadParameters.ROTATE_0
        }

        dualUVCCamera?.apply {
            setImageRotate(rotateT)
            addIrUVCCamera(irUVCCamera)
        }

        gain_switch_param.above_pixel_prop = 0.1f
        gain_switch_param.above_temp_data = ((130 + 273.15) * 16 * 4).toInt()
        gain_switch_param.below_pixel_prop = 0.95f
        gain_switch_param.below_temp_data = ((110 + 273.15) * 16 * 4).toInt()
        auto_gain_switch_info.switch_frame_cnt = 5 * 15
        auto_gain_switch_info.waiting_frame_cnt = 7 * 15

        val low_gain_over_temp_data = ByteArray(irSize * 2)
        val high_gain_over_temp_data = ByteArray(irSize * 2)
        val pixel_above_prop = 0.02f
        val switch_frame_cnt = 7 * 15
        val close_frame_cnt = 10 * 15

        val imageRes = LibIRProcess.ImageRes_t().apply {
            height = 192.toChar()
            width = 256.toChar()
        }

        irRGBAData = ByteArray(irSize * 4)
        preIrData = ByteArray(irSize * 2)
        preTempData = ByteArray(irSize * 2)
        preIrARGBData = ByteArray(irSize * 2 * 2)

        iFrameCallback = object : IFrameCallback {
            override fun onFrame(frame: ByteArray) {
                if (frame.size == 1) {
                    handler?.sendEmptyMessage(Const.RESTART_USB)
                    Log.d(TAG, "RESTART_USB")
                    return
                }

                count++
                if (count == 100) {
                    count = 0
                    val currentTimeMillis = System.currentTimeMillis()
                    if (timestart != 0L) {
                        val timeuse = currentTimeMillis - timestart
                        fps = 100 * 1000 / (timeuse + 0.0)
                    }
                    timestart = currentTimeMillis
                    Log.d(TAG, "frame.length = ${frame.size} fps=${String.format(Locale.US, "%.1f", fps)} dataFlowMode = $dataFlowMode")
                }

                System.arraycopy(frame, 0, mixData, 0, fusionLength)
                System.arraycopy(frame, fusionLength, irData, 0, irSize * 2)
                System.arraycopy(frame, fusionLength + irSize * 2, normalTempData, 0, irSize * 2)
                System.arraycopy(frame, fusionLength + irSize * 4, remapTempData, 0, Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2)
                System.arraycopy(frame, fusionLength + irSize * 4 + Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2, vlData, 0, vlSize)
                System.arraycopy(frame, 0, frameData, 0, FRAME_LEN)
                System.arraycopy(frame, dualCameraWidth * dualCameraHeight * 4, frameIrAndTempData, 0, frameIrAndTempData.size)

                val tempData = if (mCurrentFusionType == DualCameraParams.FusionType.IROnlyNoFusion) normalTempData else remapTempData
                onFrameCallbacks.forEach { callback ->
                    callback.onFame(mixData, tempData, fps)
                }

                mSurface = cameraview.holder.surface

                when (mCurrentFusionType) {
                    DualCameraParams.FusionType.IROnlyNoFusion -> {
                        LibIRParse.converyArrayYuv422ToARGB(irData, Const.IR_WIDTH * Const.IR_HEIGHT, irRGBAData)
                        if (isOpenAmplify) {
                            OpencvTools.supImage(irData, Const.IR_HEIGHT, Const.IR_WIDTH, amplifyIRRotateArray)
                            mSurface?.let { surface ->
                                mSurfaceNativeWindow.onDrawFrame(
                                    surface, amplifyIRRotateArray,
                                    Const.IR_WIDTH * MULTIPLE,
                                    Const.IR_HEIGHT * MULTIPLE
                                )
                            }
                        } else {
                            mSurface?.let { surface ->
                                mSurfaceNativeWindow.onDrawFrame(surface, irRGBAData, Const.IR_HEIGHT, Const.IR_WIDTH)
                            }
                        }
                    }
                    else -> {
                        if (isOpenAmplify) {
                            when (mCurrentFusionType) {
                                DualCameraParams.FusionType.IROnly -> {
                                    OpencvTools.supImageMix(mixData, Const.DUAL_HEIGHT, Const.DUAL_WIDTH, mixData)
                                    mSurface?.let { surface ->
                                        mSurfaceNativeWindow.onDrawFrame(surface, mixData, Const.DUAL_WIDTH, Const.DUAL_HEIGHT)
                                    }
                                }
                                else -> {
                                    OpencvTools.supImage(mixData, Const.DUAL_HEIGHT, Const.DUAL_WIDTH, amplifyMixRotateArray)
                                    mSurface?.let { surface ->
                                        mSurfaceNativeWindow.onDrawFrame(
                                            surface, amplifyMixRotateArray,
                                            Const.DUAL_WIDTH * MULTIPLE,
                                            Const.DUAL_HEIGHT * MULTIPLE
                                        )
                                    }
                                }
                            }
                        } else {
                            mSurface?.let { surface ->
                                mSurfaceNativeWindow.onDrawFrame(surface, mixData, Const.DUAL_WIDTH, Const.DUAL_HEIGHT)
                            }
                        }
                    }
                }

                if (!isUseIRISP && !firstFrame) {
                    firstFrame = true
                    handler?.sendEmptyMessage(Const.HIDE_LOADING)
                }

                if (dataFlowMode == CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT) {
                    System.arraycopy(frame, fusionLength + irSize * 2, normalTempData, 0, irSize * 2)
                    
                    if (auto_gain_switch && auto_gain_switch_running) {
                        USBMonitorManager.getInstance().ircmd?.autoGainSwitch(
                            normalTempData, imageRes,
                            auto_gain_switch_info, gain_switch_param,
                            object : AutoGainSwitchCallback {
                                override fun onAutoGainSwitchState(gainselStatus: CommonParams.PropTPDParamsValue.GAINSELStatus) {
                                    Log.d(TAG, "onAutoGainSwitchState = ${gainselStatus.value}")
                                    auto_gain_switch_running = false
                                    resetAutoGainInfo()
                                }

                                override fun onAutoGainSwitchResult(gainselStatus: CommonParams.PropTPDParamsValue.GAINSELStatus, result: Int) {
                                    Log.d(TAG, "onAutoGainSwitchResult = ${gainselStatus.value} result:$result")
                                    auto_gain_switch_running = true
                                }
                            }
                        )
                    }

                    if (auto_over_protect) {
                        USBMonitorManager.getInstance().ircmd?.avoidOverexposure(
                            false, gainStatus,
                            normalTempData, imageRes, low_gain_over_temp_data, high_gain_over_temp_data,
                            pixel_above_prop, switch_frame_cnt, close_frame_cnt,
                            object : AvoidOverexposureCallback {
                                override fun onAvoidOverexposureState(avoidOverexpol: Boolean) {
                                    Log.d(TAG, "onAvoidOverexposureState = $avoidOverexpol")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    fun resetAutoGainInfo() {
        auto_gain_switch_info.apply {
            switched_flag = 0
            cur_switched_cnt = 0
            cur_detected_low_cnt = 0
            cur_detected_high_cnt = 0
        }
    }

    fun startPreview() {
        switchIrPreDataHandleEnable(true)
        dualUVCCamera?.apply {
            setFrameCallback(iFrameCallback)
            onStartPreview()
        }
        firstFrame = false
    }

    fun getDualUVCCamera(): DualUVCCamera? = dualUVCCamera

    fun stopPreview() {
        dualUVCCamera?.apply {
            setFrameCallback(null)
            onStopPreview()
            SystemClock.sleep(200)
            onDestroy()
        }
    }

    fun switchIrPreDataHandleEnable(enable: Boolean) {
        dualUVCCamera?.apply {
            setIrDataPreHandleEnable(enable)
            setIrFrameCallback(if (enable) irFrameCallback else null)
        }
    }

    fun getRemapTempData(): ByteArray = remapTempData

    fun getScaledBitmap(): Bitmap? {
        val parentWidth = (cameraview.parent as? ViewGroup)?.width ?: return null
        val parentHeight = (cameraview.parent as? ViewGroup)?.height ?: return null

        return when {
            isOpenAmplify -> {
                when (mCurrentFusionType) {
                    DualCameraParams.FusionType.IROnlyNoFusion -> {
                        supIROlyNoFusionBitmap?.let { bitmap ->
                            bitmap.copyPixelsFromBuffer(
                                ByteBuffer.wrap(amplifyIRRotateArray, 0, bitmap.width * bitmap.height * 4)
                            )
                            Bitmap.createScaledBitmap(bitmap, parentWidth, parentHeight, true)
                        }
                    }
                    DualCameraParams.FusionType.IROnly -> {
                        bitmap?.let { bmp ->
                            bmp.copyPixelsFromBuffer(ByteBuffer.wrap(mixData, 0, bmp.width * bmp.height * 4))
                            Bitmap.createScaledBitmap(bmp, parentWidth, parentHeight, true)
                        }
                    }
                    else -> {
                        supMixBitmap?.let { bitmap ->
                            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(mixData, 0, bitmap.width * bitmap.height * 4))
                            Bitmap.createScaledBitmap(bitmap, parentWidth, parentHeight, true)
                        }
                    }
                }
            }
            else -> {
                bitmap?.let { bmp ->
                    bmp.copyPixelsFromBuffer(ByteBuffer.wrap(mixData, 0, bmp.width * bmp.height * 4))
                    Bitmap.createScaledBitmap(bmp, parentWidth, parentHeight, true)
                }
            }
        }.also { mScaledBitmap = it }
    }

    fun saveData() {
        saveData = true
    }

    fun setGainStatus(gainStatus: CommonParams.GainStatus) {
        this.gainStatus = gainStatus
    }

    fun setCurrentFusionType(currentFusionType: DualCameraParams.FusionType) {
        this.mCurrentFusionType = currentFusionType
        dualUVCCamera?.setFusion(currentFusionType)
    }
}