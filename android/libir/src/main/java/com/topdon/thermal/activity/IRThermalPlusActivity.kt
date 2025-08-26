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

@Route(path = RouterConfig.IR_FRAME_PLUSH)
class IRThermalPlusActivity : BaseIRPlushActivity() {
    
    private lateinit var binding: ActivityIrThermalDoubleBinding
    
    private val irImageHelp by lazy {
        IRImageHelp()
    }

    override fun initContentView() = R.layout.activity_ir_thermal_double

    override fun isDualIR(): Boolean {
        return true
    }

    override fun getSurfaceView(): SurfaceView {
        return binding.dualTextureViewNativeCamera
    }

    override fun getTemperatureDualView(): TemperatureView {
        return binding.temperatureView
    }

    override fun getProductName(): String {
        return PRODUCT_NAME_TCP
    }

    override fun initView() {
        super.initView()
        
        binding = ActivityIrThermalDoubleBinding.bind(findViewById(android.R.id.content))
        
        binding.cameraView.visibility = View.GONE
        binding.dualTextureViewNativeCamera.visibility = View.VISIBLE
        binding.thermalSteeringView.listener = { action, moveX ->
            setDisp(action, moveX)
        }

        when (SaveSettingUtil.fusionType) {
            SaveSettingUtil.FusionTypeLPYFusion -> {
                binding.thermalRecyclerNight.twoLightType = TwoLightType.TWO_LIGHT_1
            }
            SaveSettingUtil.FusionTypeMeanFusion -> {
                binding.thermalRecyclerNight.twoLightType = TwoLightType.TWO_LIGHT_2
            }
            SaveSettingUtil.FusionTypeIROnly -> {
                binding.thermalRecyclerNight.twoLightType = TwoLightType.IR
            }
            SaveSettingUtil.FusionTypeVLOnly -> {
                binding.thermalRecyclerNight.twoLightType = TwoLightType.LIGHT
            }
        }
    }

    private fun setDisp(action: Int, data: Int) {
        if (action == -1 || action == 1) {

            lifecycleScope.launch(Dispatchers.IO) {
                dualDisp = data
                dualView?.dualUVCCamera!!.setDisp(data)
            }
        } else {

            val oemInfo = ByteArray(1024)
            ircmd?.oemRead(CommonParams.ProductType.P2, oemInfo)
            val dataStr = data.toString()
            System.arraycopy(dataStr.toByteArray(), 0, oemInfo, 194, dataStr.toByteArray().size)
            val result = ircmd?.oemWrite(CommonParams.ProductType.P2,oemInfo)

            if (result == 0){

                if (binding.thermalSteeringView.isVisible) {
                    binding.thermalSteeringView.visibility = View.GONE
                    binding.thermalRecyclerNight.setTwoLightSelected(TwoLightType.CORRECT, false)
                }
            }else{
                ToastUtils.showShort(R.string.correction_fail)
            }

        }
    }

    override fun setTwoLight(twoLightType: TwoLightType, isSelected: Boolean) {
        when (twoLightType) {
            TwoLightType.TWO_LIGHT_1 -> {
                mCurrentFusionType = DualCameraParams.FusionType.LPYFusion
                SaveSettingUtil.fusionType = SaveSettingUtil.FusionTypeLPYFusion
                setFusion(mCurrentFusionType)
            }
            TwoLightType.TWO_LIGHT_2 -> {
                mCurrentFusionType = DualCameraParams.FusionType.MeanFusion
                SaveSettingUtil.fusionType = SaveSettingUtil.FusionTypeMeanFusion
                setFusion(mCurrentFusionType)
            }
            TwoLightType.IR -> {
                mCurrentFusionType = DualCameraParams.FusionType.IROnly
                SaveSettingUtil.fusionType = SaveSettingUtil.FusionTypeIROnly
                setFusion(mCurrentFusionType)
                binding.thermalRecyclerNight.setTwoLightSelected(TwoLightType.CORRECT, false)
                binding.thermalSteeringView.visibility = View.GONE
            }
            TwoLightType.LIGHT -> {
                mCurrentFusionType = DualCameraParams.FusionType.VLOnly
                SaveSettingUtil.fusionType = SaveSettingUtil.FusionTypeVLOnly
                setFusion(mCurrentFusionType)
                binding.thermalSteeringView.visibility = View.GONE
                binding.thermalRecyclerNight.setTwoLightSelected(TwoLightType.CORRECT, false)
            }
            TwoLightType.CORRECT -> {
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

    override fun getCameraViewBitmap(): Bitmap {
        if (imageEditBytes.size != dualView?.frameIrAndTempData?.size) {
            imageEditBytes = ByteArray(dualView!!.frameIrAndTempData.size)
        }
        System.arraycopy(dualView!!.frameIrAndTempData, 0, imageEditBytes, 0, imageEditBytes.size)
        return dualView?.scaledBitmap!!
    }

    override fun setTemperatureViewType() {
        binding.temperatureView.productType = Const.TYPE_IR_DUAL
        binding.cameraView.productType = Const.TYPE_IR_DUAL
    }

    override fun startUSB(isRestart: Boolean, isBadFrames: Boolean) {

    }

    override fun setPColor(code: Int) {
        pseudoColorMode = code
        binding.temperatureSeekbar.setPseudocode(pseudoColorMode)
        
        SaveSettingUtil.pseudoColorMode = pseudoColorMode
        binding.thermalRecyclerNight.setPseudoColor(code)
    }

    override fun startISP() {
        setCustomPseudoColorList(
            customPseudoBean.getColorList(),
            customPseudoBean.getPlaceList(),
            customPseudoBean.isUseGray,
            customPseudoBean.maxTemp, customPseudoBean.minTemp
        )
    }

    override fun setCustomPseudoColorList(
        colorList: IntArray?,
        places: FloatArray?,
        isUseGray: Boolean,
        customMaxTemp: Float,
        customMinTemp: Float
    ) {
        irImageHelp.setColorList(colorList, places, isUseGray,customMaxTemp,customMinTemp)
    }

    override fun setRotate(rotateInt: Int) {
        super.setRotate(rotateInt)
        runOnUiThread {
            binding.thermalSteeringView.rotationIR = rotateInt
        }

        when (rotateInt) {
            0 -> dualView?.dualUVCCamera?.setImageRotate(DualCameraParams.TypeLoadParameters.ROTATE_90)
            90 -> dualView?.dualUVCCamera?.setImageRotate(DualCameraParams.TypeLoadParameters.ROTATE_180)
            180 -> dualView?.dualUVCCamera?.setImageRotate(DualCameraParams.TypeLoadParameters.ROTATE_270)
            270 -> dualView?.dualUVCCamera?.setImageRotate(DualCameraParams.TypeLoadParameters.ROTATE_0)
        }
    }

    override fun onIrFrame(irFrame: ByteArray?): ByteArray {
        System.arraycopy(irFrame, 0, preIrData, 0, preIrData.size)
        System.arraycopy(irFrame, preIrData.size, preTempData, 0, preTempData.size)
        if (irImageHelp.getColorList() != null){

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

        irImageHelp.setPseudoColorMaxMin(
            preIrARGBData, preTempData, editMaxValue,
            editMinValue, Const.IR_WIDTH,Const.IR_HEIGHT)

       val tempData =irImageHelp.contourDetection(alarmBean,
           preIrARGBData,preTempData,
            Const.IR_HEIGHT,Const.IR_WIDTH)
        System.arraycopy(tempData,0, preIrARGBData, 0, preIrARGBData.size)
        return preIrARGBData
    }

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

    override fun irStart() {
        if (!isrun) {
            binding.tvTypeInd.isVisible = false
            startUSB(false,false)
            startISP()
            isrun = true

            configParam()
            binding.thermalRecyclerNight.updateCameraModel()
            initIRConfig()
        }
    }
    
    override fun setDispViewData(dualDisp: Int) {
        binding.thermalSteeringView.moveX = dualDisp
    }
    
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
    
    override fun switchAutoGain(boolean: Boolean) {
        dualView?.auto_gain_switch = boolean
    }

}
