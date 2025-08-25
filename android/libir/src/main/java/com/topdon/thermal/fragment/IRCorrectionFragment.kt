package com.topdon.thermal.fragment

import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.yt.jni.Usbcontorl
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.elvishew.xlog.XLog
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.SynchronizedBitmap
import com.energy.iruvc.uvc.ConnectCallback
import com.energy.iruvc.uvc.UVCCamera
import com.infisense.usbir.camera.IRUVCTC
import com.infisense.usbir.config.MsgCode
import com.infisense.usbir.event.IRMsgEvent
import com.infisense.usbir.event.PreviewComplete
import com.infisense.usbir.thread.ImageThreadTC
import com.infisense.usbir.utils.USBMonitorCallback
import com.infisense.usbir.view.ITsTempListener
import com.infisense.usbir.view.TemperatureView.*
import com.topdon.lib.core.common.SaveSettingUtil
import com.topdon.lib.core.config.DeviceConfig
import com.topdon.lib.core.ktbase.BaseFragment
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.thermal.R
import com.topdon.thermal.databinding.FragmentIrMonitorThermalBinding
import com.topdon.thermal.repository.ConfigRepository
import com.topdon.thermal.utils.CalibrationTools
import kotlinx.coroutines.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class IRCorrectionFragment : BaseFragment(), ITsTempListener {
    
    private var _binding: FragmentIrMonitorThermalBinding? = null
    private val binding get() = _binding!!
    
    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding {
        _binding = FragmentIrMonitorThermalBinding.inflate(inflater, container, false)
        return binding
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    protected var defaultDataFlowMode = CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT

    private var ircmd: IRCMD? = null

    private var rotateAngle = 270

    override fun initView() {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initDataIR()
    }

    override fun initData() {

    }

    private var imageThread: ImageThreadTC? = null
    
    private var bitmap: Bitmap? = null
    
    private var iruvc: IRUVCTC? = null
    
    private val cameraWidth = 256
    private val cameraHeight = 384
    private val tempHeight = 192
    
    private var imageWidth = cameraWidth
    private var imageHeight = cameraHeight - tempHeight
    
    private val image = ByteArray(imageWidth * imageHeight * 2)
    private val temperature = ByteArray(imageWidth * imageHeight * 2)
    
    private val syncimage = SynchronizedBitmap()
    
    private var isrun = false
    
    private var pseudocolorMode = 0

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun irEvent(event: IRMsgEvent) {
        if (event.code == MsgCode.RESTART_USB) {
            restartUsbCamera()
        }
    }

    private fun initDataIR() {
        imageWidth = cameraHeight - tempHeight
        imageHeight = cameraWidth
        binding.temperatureView.setTextSize(SaveSettingUtil.tempTextSize)
        if (ScreenUtil.isPortrait(requireContext())) {
            bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
            binding.temperatureView.setImageSize(imageWidth, imageHeight, this@IRCorrectionFragment)
            rotateAngle = DeviceConfig.S_ROTATE_ANGLE
        } else {
            bitmap = Bitmap.createBitmap(imageHeight, imageWidth, Bitmap.Config.ARGB_8888)
            binding.temperatureView.setImageSize(imageHeight, imageWidth, this@IRCorrectionFragment)
            rotateAngle = DeviceConfig.ROTATE_ANGLE
        }
        binding.cameraView.setSyncimage(syncimage)
        binding.cameraView.bitmap = bitmap
        binding.cameraView.isDrawLine = false
        binding.temperatureView.setSyncimage(syncimage)
        binding.temperatureView.setTemperature(temperature)
        binding.temperatureView.isEnabled = false
        setViewLay()

        if (Usbcontorl.isload) {
            Usbcontorl.usb3803_mode_setting(1)
            Log.w("IRCorrectionFragment", "5V power enabled for thermal sensor")
        }
        binding.temperatureView.clear()
        binding.temperatureView.temperatureRegionMode = REGION_MODE_CLEAN
    }

    private fun startISP() {
        try {
            imageThread = ImageThreadTC(context, imageWidth, imageHeight)
            imageThread!!.setDataFlowMode(defaultDataFlowMode)
            imageThread!!.setSyncImage(syncimage)
            imageThread!!.setImageSrc(image)
            imageThread!!.setTemperatureSrc(temperature)
            imageThread!!.setBitmap(bitmap)
            imageThread?.setRotate(rotateAngle)
            imageThread!!.setRotate(true)
            imageThread!!.start()
        } catch (e: Exception) {
            Log.e("IRCorrectionFragment", "Failed to initialize thermal processing thread: ${e.message}")
        }
    }

    private fun startUSB(isRestart: Boolean) {
        context?.let { ctx ->
            iruvc = IRUVCTC(cameraWidth, cameraHeight, ctx, syncimage,
                defaultDataFlowMode, object : ConnectCallback {
                    override fun onCameraOpened(uvcCamera: UVCCamera) {

                    }

                    override fun onIRCMDCreate(ircmd: IRCMD) {
                        Log.i(TAG, "Professional IR command interface initialized")
                        this@IRCorrectionFragment.ircmd = ircmd

                    }
                }, object : USBMonitorCallback {
                    override fun onAttach() {}
                    override fun onGranted() {}
                    override fun onConnect() {}
                    override fun onDisconnect() {}
                    override fun onDettach() {
                        activity?.finish()
                    }
                    override fun onCancel() {
                        activity?.finish()
                    }
                })
            iruvc!!.isRestart = isRestart
            iruvc!!.setImageSrc(image)
            iruvc!!.setTemperatureSrc(temperature)
            iruvc!!.setRotate(rotateAngle)
            iruvc!!.registerUSB()
        }
    }

    private fun restartUsbCamera() {
        if (iruvc != null) {
            iruvc!!.stopPreview()
            iruvc!!.unregisterUSB()
        }
        startUSB(true)
    }

    override fun onStart() {
        super.onStart()
        Log.w(TAG, "Professional thermal correction fragment starting")
        if (!isrun) {

            binding.temperatureView.postDelayed({
                pseudocolorMode = 3
                startUSB(false)
                startISP()
                binding.temperatureView.start()
                binding.cameraView.start()
                isrun = true

                configParam()
            }, 1500)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.w(TAG, "Professional thermal correction fragment stopping")
        if (iruvc != null) {
            iruvc!!.stopPreview()
            iruvc!!.unregisterUSB()
        }
        imageThread?.interrupt()
        syncimage.valid = false
        binding.temperatureView.stop()
        binding.cameraView.stop()
        isrun = false
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "Professional thermal correction fragment destroying")
        try {
            imageThread?.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, "Thermal processing thread termination interrupted: ${e.message}")
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun iruvctc(event: PreviewComplete) {
        dealY16ModePreviewComplete()
    }

    var frameReady = false

    private fun dealY16ModePreviewComplete() {
        isConfigWait = false
        iruvc?.setFrameReady(true)
        frameReady = true
    }

    private fun setViewLay() {
        binding.thermalLay.post {
            if (ScreenUtil.isPortrait(requireContext())) {
                val params = binding.thermalLay.layoutParams
                params.width = ScreenUtil.getScreenWidth(requireContext())
                params.height = params.width * imageHeight / imageWidth
                binding.thermalLay.layoutParams = params
            } else {

                val params = binding.thermalLay.layoutParams
                params.height = binding.thermalLay.height
                params.width = params.height * imageHeight / imageWidth
                binding.thermalLay.layoutParams = params
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun cameraEvent(event: DeviceCameraEvent) {
        when (event.action) {
            100 -> {

                showLoadingDialog()
            }
            101 -> {

                lifecycleScope.launch {
                    delay(500)
                    isConfigWait = false
                    delay(1000)
                    dismissLoadingDialog()
                }
            }
        }
    }

    private var isConfigWait = true

    private fun configParam() {
        lifecycleScope.launch {
            isConfigWait = true
            while (isConfigWait) {
                delay(100)
            }
            val config = ConfigRepository.readConfig(false)
            val disChar = (config.distance * 128).toInt()
            val emsChar = (config.radiation * 128).toInt()
            XLog.w("Professional TPD parameter configuration - DISTANCE: $disChar, EMS: $emsChar")
            
            val timeMillis = 250L
            
            delay(timeMillis)

            ircmd?.setPropTPDParams(
                CommonParams.PropTPDParams.TPD_PROP_EMS,
                CommonParams.PropTPDParamsValue.NumberType(emsChar.toString())
            )
            
            delay(timeMillis)

            ircmd?.setPropTPDParams(
                CommonParams.PropTPDParams.TPD_PROP_DISTANCE,
                CommonParams.PropTPDParamsValue.NumberType(disChar.toString())
            )
            
            delay(timeMillis)
            ircmd?.zoomCenterDown(
                CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                CommonParams.ZoomScaleStep.ZOOM_STEP2
            )
            delay(timeMillis)
            ircmd?.zoomCenterDown(
                CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                CommonParams.ZoomScaleStep.ZOOM_STEP2
            )
            delay(timeMillis)
            ircmd?.zoomCenterDown(
                CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                CommonParams.ZoomScaleStep.ZOOM_STEP2
            )
            delay(timeMillis)
            ircmd?.zoomCenterDown(
                CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                CommonParams.ZoomScaleStep.ZOOM_STEP2
            )
            
            iruvc?.let {

                withContext(Dispatchers.IO) {
                    if (SaveSettingUtil.isAutoShutter) {
                        ircmd?.setPropAutoShutterParameter(
                            CommonParams.PropAutoShutterParameter.SHUTTER_PROP_SWITCH,
                            CommonParams.PropAutoShutterParameterValue.StatusSwith.ON
                        )
                    } else {
                        ircmd?.setPropAutoShutterParameter(
                            CommonParams.PropAutoShutterParameter.SHUTTER_PROP_SWITCH,
                            CommonParams.PropAutoShutterParameterValue.StatusSwith.OFF
                        )
                    }
                }
            }
            
            delay(timeMillis)
            ircmd?.setPropImageParams(
                CommonParams.PropImageParams.IMAGE_PROP_LEVEL_CONTRAST,
                CommonParams.PropImageParamsValue.NumberType(128.toString())
            )
            delay(timeMillis)
            ircmd?.setPropImageParams(
                CommonParams.PropImageParams.IMAGE_PROP_LEVEL_DDE,
                CommonParams.PropImageParamsValue.DDEType.DDE_2
            )
            delay(timeMillis)
            ircmd?.setPropImageParams(
                CommonParams.PropImageParams.IMAGE_PROP_ONOFF_AGC,
                CommonParams.PropImageParamsValue.StatusSwith.ON
            )
        }
    }

    suspend fun autoStart() {
        withContext(Dispatchers.IO) {

            XLog.w("Professional thermal correction: Automated calibration sequence starting")
            
            CalibrationTools.autoShutter(irCmd = ircmd, false)
            XLog.w("Professional thermal correction: Automatic shutter disabled for calibration")
            
            delay(2000)
            XLog.w("Professional thermal correction: Thermal stabilization complete")
            CalibrationTools.stsSwitch(irCmd = ircmd, false)
            
            CalibrationTools.pot(irCmd = ircmd!!, 1)
            XLog.w("Professional thermal correction: Pot lid calibration marker transmitted")
            
            delay(5000)
            XLog.w("Professional thermal correction: Thermal correction system enabled")
            CalibrationTools.stsSwitch(irCmd = ircmd, true)
            
            delay(20000)
            XLog.w("Professional thermal correction: Extended thermal stabilization complete")
            
            delay(2000)
            CalibrationTools.stsSwitch(irCmd = ircmd, false)
            XLog.w("Professional thermal correction: High-temperature calibration phase")
            
            CalibrationTools.pot(irCmd = ircmd!!, 1)
            
            delay(5000)
            XLog.w("Professional thermal correction: Final calibration system activation")
            CalibrationTools.stsSwitch(irCmd = ircmd, true)
            
            CalibrationTools.autoShutter(irCmd = ircmd, true)
            XLog.w("Professional thermal correction: Automated calibration sequence completed successfully")
        }
    }
