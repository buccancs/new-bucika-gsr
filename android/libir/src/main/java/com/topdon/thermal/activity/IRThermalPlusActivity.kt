package com.topdon.thermal.activity

import android.graphics.Bitmap
import android.view.SurfaceView
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.blankj.utilcode.util.ToastUtils
import com.energy.iruvc.sdkisp.LibIRProcess
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.DualCameraParams
import com.infisense.usbdual.Const
import com.infisense.usbir.utils.IRImageHelp
import com.infisense.usbir.utils.PseudocodeUtils
import com.infisense.usbir.view.TemperatureView
import com.topdon.lib.core.bean.CameraItemBean
import com.topdon.lib.core.common.ProductType.PRODUCT_NAME_TCP
import com.topdon.lib.core.common.SaveSettingUtil
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.tools.ToastTools
import com.topdon.menu.constant.TwoLightType
import com.topdon.thermal.R
import com.topdon.thermal.databinding.ActivityIrThermalDoubleBinding
import com.topdon.thermal.event.GalleryAddEvent
import com.topdon.thermal.video.VideoRecordFFmpeg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.nio.ByteBuffer


/**
 * Dual-light thermal imaging device interface activity.
 * 
 * This activity manages the main thermal imaging interface for dual-light devices,
 * providing comprehensive thermal imaging capabilities including dual-light fusion,
 * temperature measurement, and advanced imaging controls.
 * 
 * **Key Features:**
 * - Dual-light thermal imaging with fusion modes (LPY, Mean, IR-only, Visible-only)
 * - Real-time temperature monitoring and measurement
 * - Advanced camera controls and settings
 * - Video recording capabilities with FFmpeg
 * - Custom pseudocolor mapping and thermal visualization
 * - Temperature range adjustment and calibration
 * - Image rotation and display optimization
 * - Professional-grade thermal data processing
 * 
 * **Architecture:**
 * - Extends BaseIRPlushActivity for core thermal functionality
 * - Implements ViewBinding for type-safe view access
 * - Uses coroutines for background thermal processing
 * - Integrates with native camera interfaces for dual-light fusion
 * 
 * @author CaiSongL
 * @since 2024-01-17
 * @see BaseIRPlushActivity Base class for thermal IR functionality
 * @see VideoRecordFFmpeg Video recording implementation
 * @see TwoLightType Dual-light mode enumeration
 */
@Route(path = RouterConfig.IR_FRAME_PLUSH)
class IRThermalPlusActivity : BaseIRPlushActivity() {
    
    /** ViewBinding instance for type-safe view access */
    private lateinit var binding: ActivityIrThermalDoubleBinding
    
    /** IR image processing helper for custom pseudocolor operations */
    private val irImageHelp by lazy {
        IRImageHelp()
    }

    /**
     * Inflates the layout using ViewBinding for type-safe view access.
     * 
     * @return Layout resource ID for the dual-light thermal interface
     */
    override fun initContentView() = R.layout.activity_ir_thermal_double

    /**
     * Indicates this activity supports dual IR functionality.
     * 
     * @return true as this is a dual-light thermal device interface
     */
    override fun isDualIR(): Boolean {
        return true
    }

    /**
     * Provides the native camera surface view for dual-light rendering.
     * 
     * @return SurfaceView for dual-light camera output
     */
    override fun getSurfaceView(): SurfaceView {
        return binding.dualTextureViewNativeCamera
    }

    /**
     * Provides the temperature overlay view for thermal measurement display.
     * 
     * @return TemperatureView for thermal overlay rendering
     */
    override fun getTemperatureDualView(): TemperatureView {
        return binding.temperatureView
    }

    /**
     * Gets the product identifier for this thermal device.
     * 
     * @return Product name constant for TCP thermal devices
     */
    override fun getProductName(): String {
        return PRODUCT_NAME_TCP
    }

    /**
     * Initializes the view components and sets up dual-light interface.
     * 
     * Configures the camera views, steering controls, and fusion modes
     * based on current settings. Sets up the proper view visibility and
     * initializes the thermal menu system.
     * 
     * @throws RuntimeException if ViewBinding initialization fails
     */
    override fun initView() {
        super.initView()
        
        // Initialize ViewBinding after calling super.initView()
        binding = ActivityIrThermalDoubleBinding.bind(findViewById(android.R.id.content))
        
//        findViewById<TextView>(R.id.toolbar_title)?.text = "双光设备"
        binding.cameraView.visibility = View.GONE
        binding.dualTextureViewNativeCamera.visibility = View.VISIBLE
        binding.thermalSteeringView.listener = { action, moveX ->
            setDisp(action, moveX)
        }

        when (SaveSettingUtil.fusionType) {
            SaveSettingUtil.FusionTypeLPYFusion -> {//双光1
                binding.thermalRecyclerNight.twoLightType = TwoLightType.TWO_LIGHT_1
            }
            SaveSettingUtil.FusionTypeMeanFusion -> {//双光2
                binding.thermalRecyclerNight.twoLightType = TwoLightType.TWO_LIGHT_2
            }
            SaveSettingUtil.FusionTypeIROnly -> {//单红外
                binding.thermalRecyclerNight.twoLightType = TwoLightType.IR
            }
            SaveSettingUtil.FusionTypeVLOnly -> {//可见光
                binding.thermalRecyclerNight.twoLightType = TwoLightType.LIGHT
            }
        }
    }




    /**
     * Executes dual-light registration adjustment.
     * 
     * Handles steering wheel control input for precise alignment between
     * thermal and visible light images. Supports real-time adjustment
     * and persistent storage of calibration values.
     * 
     * @param action Movement direction: -1 (left), 1 (right), 0 (confirm)
     * @param data Current registration adjustment value
     * @throws Exception if dual camera communication fails
     */
    private fun setDisp(action: Int, data: Int) {
        if (action == -1 || action == 1) {
            // 移动
            lifecycleScope.launch(Dispatchers.IO) {
                dualDisp = data
                dualView?.dualUVCCamera!!.setDisp(data)
            }
        } else {
            // 确定
            val oemInfo = ByteArray(1024)
            ircmd?.oemRead(CommonParams.ProductType.P2, oemInfo)
            val dataStr = data.toString()
            System.arraycopy(dataStr.toByteArray(), 0, oemInfo, 194, dataStr.toByteArray().size)
            val result = ircmd?.oemWrite(CommonParams.ProductType.P2,oemInfo)
//            SharedManager.setIrDualDisp(dualDisp)
            if (result == 0){
                // 关闭控件
                if (binding.thermalSteeringView.isVisible) {
                    binding.thermalSteeringView.visibility = View.GONE
                    binding.thermalRecyclerNight.setTwoLightSelected(TwoLightType.CORRECT, false)
                }
            }else{
                ToastUtils.showShort(R.string.correction_fail)
            }

        }
    }

    /**
     * Handles dual-light mode selection and configuration.
     * 
     * Manages transitions between different fusion modes including dual-light
     * combinations, single IR, visible light, and registration mode. Updates
     * the camera configuration and UI state accordingly.
     * 
     * @param twoLightType The selected dual-light mode
     * @param isSelected Whether the mode is being activated or deactivated
     * @see TwoLightType Available dual-light modes
     */
    override fun setTwoLight(twoLightType: TwoLightType, isSelected: Boolean) {
        when (twoLightType) {
            TwoLightType.TWO_LIGHT_1 -> {//双光1
                mCurrentFusionType = DualCameraParams.FusionType.LPYFusion
                SaveSettingUtil.fusionType = SaveSettingUtil.FusionTypeLPYFusion
                setFusion(mCurrentFusionType)
            }
            TwoLightType.TWO_LIGHT_2 -> {//双光2
                mCurrentFusionType = DualCameraParams.FusionType.MeanFusion
                SaveSettingUtil.fusionType = SaveSettingUtil.FusionTypeMeanFusion
                setFusion(mCurrentFusionType)
            }
            TwoLightType.IR -> {//单红外
                mCurrentFusionType = DualCameraParams.FusionType.IROnly
                SaveSettingUtil.fusionType = SaveSettingUtil.FusionTypeIROnly
                setFusion(mCurrentFusionType)
                binding.thermalRecyclerNight.setTwoLightSelected(TwoLightType.CORRECT, false)
                binding.thermalSteeringView.visibility = View.GONE
            }
            TwoLightType.LIGHT -> {//单可见光
                mCurrentFusionType = DualCameraParams.FusionType.VLOnly
                SaveSettingUtil.fusionType = SaveSettingUtil.FusionTypeVLOnly
                setFusion(mCurrentFusionType)
                binding.thermalSteeringView.visibility = View.GONE
                binding.thermalRecyclerNight.setTwoLightSelected(TwoLightType.CORRECT, false)
            }
            TwoLightType.CORRECT -> {//配准
                if (isSelected){
                    binding.thermalSteeringView.visibility = View.VISIBLE
                    if (mCurrentFusionType != DualCameraParams.FusionType.LPYFusion && mCurrentFusionType != DualCameraParams.FusionType.MeanFusion) {
                        mCurrentFusionType = DualCameraParams.FusionType.LPYFusion
                        binding.thermalRecyclerNight.twoLightType = TwoLightType.TWO_LIGHT_1
                        SaveSettingUtil.fusionType = SaveSettingUtil.FusionTypeLPYFusion
                        setFusion(DualCameraParams.FusionType.LPYFusion)
                    }
                }else{
                    binding.thermalSteeringView.visibility = View.GONE
                }
            }
            else -> {
                super.setTwoLight(twoLightType, isSelected)
            }
        }
    }

    /**
     * Captures current camera view as bitmap for image processing.
     * 
     * Retrieves the current thermal image frame from the dual-light camera
     * system, ensuring proper data synchronization and format conversion.
     * 
     * @return Bitmap representation of current thermal frame
     * @throws IllegalStateException if dual camera view is not initialized
     */
    override fun getCameraViewBitmap(): Bitmap {
        if (imageEditBytes.size != dualView?.frameIrAndTempData?.size) {
            imageEditBytes = ByteArray(dualView!!.frameIrAndTempData.size)
        }
        System.arraycopy(dualView!!.frameIrAndTempData, 0, imageEditBytes, 0, imageEditBytes.size)
        return dualView?.scaledBitmap!!
    }

    /**
     * Configures temperature view for dual IR operation mode.
     * 
     * Sets the appropriate product type for both temperature overlay
     * and camera view components to ensure proper dual IR functionality.
     */
    override fun setTemperatureViewType() {
        binding.temperatureView.productType = Const.TYPE_IR_DUAL
        binding.cameraView.productType = Const.TYPE_IR_DUAL
    }

    /**
     * Initializes USB camera connection for dual-light operation.
     * 
     * @param isRestart Whether this is a restart operation
     * @param isBadFrames Whether to handle bad frame recovery
     */
    override fun startUSB(isRestart: Boolean, isBadFrames: Boolean) {

    }

    /**
     * Sets pseudocolor mode for thermal visualization.
     * 
     * Updates the thermal color mapping and UI controls to reflect
     * the selected pseudocolor scheme. Persists the selection for
     * future sessions.
     * 
     * @param code Pseudocolor mode identifier
     * @see PseudocodeUtils Pseudocolor conversion utilities
     */
    override fun setPColor(code: Int) {
        pseudoColorMode = code
        binding.temperatureSeekbar.setPseudocode(pseudoColorMode)
        /**
         * 设置伪彩【set pseudocolor】
         * 固件机芯实现(部分伪彩为预留,设置后可能无效果)
         */
//        dualView?.dualUVCCamera?.setPseudocolor(PseudocodeUtils.changeDualPseudocodeModelByOld(pseudoColorMode))
        SaveSettingUtil.pseudoColorMode = pseudoColorMode
        binding.thermalRecyclerNight.setPseudoColor(code)
    }

    /**
     * Initializes ISP (Image Signal Processor) with custom color settings.
     * 
     * Sets up custom pseudocolor mapping based on stored configuration
     * including color lists, temperature ranges, and grayscale settings.
     */
    override fun startISP() {
        setCustomPseudoColorList(
            customPseudoBean.getColorList(),
            customPseudoBean.getPlaceList(),
            customPseudoBean.isUseGray,
            customPseudoBean.maxTemp, customPseudoBean.minTemp
        )
    }

    /**
     * Configures custom pseudocolor mapping for thermal visualization.
     * 
     * Sets up advanced color mapping with custom temperature ranges,
     * color transitions, and grayscale fallback options for specialized
     * thermal imaging applications.
     * 
     * @param colorList Array of color values for mapping
     * @param places Array of temperature positions for color mapping
     * @param isUseGray Whether to enable grayscale fallback
     * @param customMaxTemp Maximum temperature for custom range
     * @param customMinTemp Minimum temperature for custom range
     */
    override fun setCustomPseudoColorList(
        colorList: IntArray?,
        places: FloatArray?,
        isUseGray: Boolean,
        customMaxTemp: Float,
        customMinTemp: Float
    ) {
        irImageHelp.setColorList(colorList, places, isUseGray,customMaxTemp,customMinTemp)
    }

    /**
     * Sets image rotation for dual-light camera system.
     * 
     * Handles rotation transformation for both UI elements and dual camera
     * hardware, ensuring proper orientation for thermal imaging display.
     * 
     * @param rotateInt Rotation angle in degrees (0, 90, 180, 270)
     */
    override fun setRotate(rotateInt: Int) {
        super.setRotate(rotateInt)
        runOnUiThread {
            binding.thermalSteeringView.rotationIR = rotateInt
        }
        //双光的旋转角度不同
        when (rotateInt) {
            0 -> dualView?.dualUVCCamera?.setImageRotate(DualCameraParams.TypeLoadParameters.ROTATE_90)
            90 -> dualView?.dualUVCCamera?.setImageRotate(DualCameraParams.TypeLoadParameters.ROTATE_180)
            180 -> dualView?.dualUVCCamera?.setImageRotate(DualCameraParams.TypeLoadParameters.ROTATE_270)
            270 -> dualView?.dualUVCCamera?.setImageRotate(DualCameraParams.TypeLoadParameters.ROTATE_0)
        }
    }

    /**
     * Processes incoming IR frame data with advanced thermal imaging algorithms.
     * 
     * Performs real-time thermal image processing including pseudocolor conversion,
     * custom color mapping, temperature range adjustment, and contour detection
     * for temperature monitoring applications.
     * 
     * @param irFrame Raw IR frame data from dual-light camera
     * @return Processed ARGB thermal image data ready for display
     * @throws Exception if image processing pipeline fails
     */
    override fun onIrFrame(irFrame: ByteArray?): ByteArray {
        System.arraycopy(irFrame, 0, preIrData, 0, preIrData.size)
        System.arraycopy(irFrame, preIrData.size, preTempData, 0, preTempData.size)
        if (irImageHelp.getColorList() != null){
            //转成灰度图进行自定义伪彩融合处理
            LibIRProcess.convertYuyvMapToARGBPseudocolor(
                preIrData, (Const.IR_WIDTH * Const.IR_HEIGHT).toLong(),
                CommonParams.PseudoColorType.PSEUDO_1, preIrARGBData
            )
        }else{
            LibIRProcess.convertYuyvMapToARGBPseudocolor(
                preIrData, (Const.IR_WIDTH * Const.IR_HEIGHT).toLong(),
                PseudocodeUtils.changePseudocodeModeByOld(pseudoColorMode), preIrARGBData
            )
        }
        irImageHelp.customPseudoColor(preIrARGBData,preTempData,Const.IR_WIDTH,Const.IR_HEIGHT)
        //等温尺处理,展示伪彩的温度范围内信息
        irImageHelp.setPseudoColorMaxMin(
            preIrARGBData, preTempData, editMaxValue,
            editMinValue, Const.IR_WIDTH,Const.IR_HEIGHT)
        //温度监控的轮廓检测，双光的原始图像不管旋转如何，原始数据都不变，（也就是宽高256*192）
       val tempData =irImageHelp.contourDetection(alarmBean,
           preIrARGBData,preTempData,
            Const.IR_HEIGHT,Const.IR_WIDTH)
        System.arraycopy(tempData,0, preIrARGBData, 0, preIrARGBData.size)
        return preIrARGBData
    }

    /**
     * Handles thermal imaging system shutdown and cleanup.
     * 
     * Performs comprehensive cleanup including job cancellation, timer stops,
     * video recording finalization, and hardware resource release. Ensures
     * proper system state for clean shutdown or restart.
     * 
     * @throws Exception if cleanup operations encounter errors
     */
    override fun irStop() {
        try {
            configJob?.cancel()
            binding.timeDownView.cancel()
            if (isVideo) {
                isVideo = false
                videoRecord?.stopRecord()
                videoTimeClose()
                CoroutineScope(Dispatchers.Main).launch {
                    delay(500)
                    EventBus.getDefault().post(GalleryAddEvent())
                }
                lifecycleScope.launch {
                    delay(500)
                    binding.thermalRecyclerNight.refreshImg()
                }
            }
        } catch (_: Exception) {
        }finally {
            ircmd?.onDestroy()
            ircmd = null
        }
    }

    /**
     * Initializes video recording system with FFmpeg integration.
     * 
     * Sets up comprehensive video recording including thermal overlay,
     * temperature display, camera preview, and UI elements for complete
     * thermal imaging video capture functionality.
     */
    override fun initVideoRecordFFmpeg() {
        videoRecord = VideoRecordFFmpeg(
            binding.cameraView,
            binding.cameraPreview,
            binding.temperatureView,
            curChooseTabPos == 1,
            binding.clSeekBar,
            binding.tempBg,
            binding.compassView, dualView,
            carView = binding.layCarDetectPrompt
        )
    }

    /**
     * Starts thermal imaging system operation.
     * 
     * Initializes the complete thermal imaging pipeline including USB connection,
     * ISP configuration, parameter restoration, and UI updates. Ensures proper
     * system readiness for thermal imaging operations.
     */
    override fun irStart() {
        if (!isrun) {
            binding.tvTypeInd.isVisible = false
            startUSB(false,false)
            startISP()
            isrun = true
            //恢复配置
            configParam()
            binding.thermalRecyclerNight.updateCameraModel()
            initIRConfig()
        }
    }
    
    /**
     * Updates steering wheel display with current displacement value.
     * 
     * @param dualDisp Current dual-light registration displacement value
     */
    override fun setDispViewData(dualDisp: Int) {
        binding.thermalSteeringView.moveX = dualDisp
    }
    
    /**
     * Executes automatic gain configuration for optimal thermal imaging.
     * 
     * Enables automatic gain control and updates the temperature level
     * indicator in the UI. Provides user feedback on configuration changes.
     */
    override fun autoConfig() {
        lifecycleScope.launch(Dispatchers.IO) {
            dualView?.let {
                if (!it.auto_gain_switch) {
                    switchAutoGain(true)
                    ToastTools.showShort(R.string.auto_open)
                }
                gainSelChar = CameraItemBean.TYPE_TMP_ZD
            }
        }
        dismissCameraLoading()
        binding.thermalRecyclerNight.setTempLevel(CameraItemBean.TYPE_TMP_ZD)
    }
    
    /**
     * Toggles automatic gain control for dual-light camera system.
     * 
     * @param boolean true to enable automatic gain, false to disable
     */
    override fun switchAutoGain(boolean: Boolean) {
        dualView?.auto_gain_switch = boolean
    }

}
