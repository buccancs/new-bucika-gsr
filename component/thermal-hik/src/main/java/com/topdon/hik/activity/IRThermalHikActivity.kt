package com.topdon.hik.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.drawToBitmap
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.blankj.utilcode.util.SizeUtils
import com.blankj.utilcode.util.ToastUtils
import com.elvishew.xlog.XLog
import com.example.suplib.wrapper.SupHelp
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.topdon.hik.R
import com.topdon.hik.databinding.ActivityIrThermalHikBinding
import com.topdon.lib.core.bean.CameraItemBean
import com.topdon.lib.core.bean.WatermarkBean
import com.topdon.lib.core.common.ProductType
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.dialog.CarDetectDialog
import com.topdon.lib.core.dialog.EmissivityTipPopup
import com.topdon.lib.core.dialog.LongTextDialog
import com.topdon.lib.core.dialog.NotTipsSelectDialog
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.dialog.TipEmissivityDialog
import com.topdon.lib.core.dialog.TipShutterDialog
import com.topdon.lib.core.ktbase.BaseBindingActivity
import com.topdon.lib.core.repository.GalleryRepository
import com.topdon.lib.core.tools.CheckDoubleClick
import com.topdon.lib.core.tools.PermissionTool
import com.topdon.lib.core.tools.ScreenTool
import com.topdon.lib.core.tools.SpanBuilder
import com.topdon.lib.core.tools.TimeTool
import com.topdon.lib.core.tools.ToastTools
import com.topdon.lib.core.tools.UnitTools
import com.topdon.lib.core.utils.BitmapUtils
import com.topdon.lib.core.utils.ImageUtils
import com.topdon.lib.ui.dialog.TipPreviewDialog
import com.topdon.lib.ui.widget.seekbar.OnRangeChangedListener
import com.topdon.lib.ui.widget.seekbar.RangeSeekBar
import com.topdon.libcom.AlarmHelp
import com.topdon.libcom.bean.SaveSettingBean
import com.topdon.libcom.dialog.ColorPickDialog
import com.topdon.libcom.dialog.TempAlarmSetDialog
import com.topdon.libhik.util.HikHelper
import com.topdon.menu.constant.FenceType
import com.topdon.menu.constant.SettingType
import com.topdon.menu.constant.TwoLightType
import com.topdon.module.thermal.ir.bean.DataBean
import com.topdon.module.thermal.ir.event.GalleryAddEvent
import com.topdon.module.thermal.ir.frame.FrameStruct
import com.topdon.module.thermal.ir.popup.CameraItemPopup
import com.topdon.module.thermal.ir.popup.SeekBarPopup
import com.topdon.module.thermal.ir.repository.ConfigRepository
import com.topdon.module.thermal.ir.utils.IRConfigData
import com.topdon.module.thermal.ir.video.VideoRecordFFmpeg
import com.topdon.module.thermal.ir.view.TemperatureBaseView.Mode
import com.topdon.pseudo.activity.PseudoSetActivity
import com.topdon.pseudo.bean.CustomPseudoBean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import kotlin.math.roundToInt

/**
 * 热成像-海康.
 *
 * Created by LCG on 2024/11/30.
 */
@Route(path = RouterConfig.IR_HIK_MAIN)
class IRThermalHikActivity : BaseBindingActivity<ActivityIrThermalHikBinding>() {

    /**
     * 保存设置开关影响的相关配置项.
     */
    private val saveSetBean = SaveSettingBean()

    /**
     * 自定义渲染相关配置.
     */
    private var customPseudoBean = CustomPseudoBean.loadFromShared()


    /**
     * 双光-融合度、设置-对比度、设置-锐度 PopupWindow，用于在点击其他操作时关掉.
     */
    private var popupWindow: PopupWindow? = null


    /**
     * 等温尺限制的低温值，单位摄氏度，MIN_VALUE 表示未设置。
     * 伪彩条里的逻辑太乱了，先这么顶着吧
     */
    private var limitTempMin = Float.MIN_VALUE
        set(value) {
            field = value
            binding.hikSurfaceView.limitTempMin = value
        }
    /**
     * 等温尺限制的高温值，单位摄氏度，MAX_VALUE 表示未设置
     */
    private var limitTempMax = Float.MAX_VALUE
        set(value) {
            field = value
            binding.hikSurfaceView.limitTempMax = value
        }
    /**
     * 当前全图最低温，单位跟随用户设置。
     * 本来这个东西可以直接从伪彩条里取的，但伪彩条里的逻辑太乱了，先这么顶着吧
     */
    private var currentMin: Float = 0f
    /**
     * 当前全图最高温，单位跟随用户设置。
     */
    private var currentMax: Float = 0f


    override fun initContentLayoutId(): Int = R.layout.activity_ir_thermal_hik

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HikHelper.bind(this)
        HikHelper.setTempListener {
            binding.temperatureView.refreshTemp(it)
        }
        HikHelper.setFrameListener { yuvArray, tempArray ->
            binding.hikSurfaceView.refresh(yuvArray, tempArray)
        }
        HikHelper.onTimeoutListener = {
            // TODO: 跟进超时弹框逻辑
            TipDialog.Builder(this)
                .setMessage("机芯出了毛病，5秒了没个回调过来")
                .setPositiveListener(R.string.app_got_it) {
                    finish()
                }
                .create().show()
        }
        HikHelper.onReadyListener = {
            //热成像机芯相关参数初始化
            lifecycleScope.launch {
                HikHelper.initConfig()
                HikHelper.setAutoShutter(saveSetBean.isAutoShutter)
                HikHelper.setContrast((saveSetBean.contrastValue * 100 / 255f).toInt()) //重置对比度
                HikHelper.setEnhanceLevel((saveSetBean.ddeConfig * 100f / 4).toInt()) //重置细节增强等级
                HikHelper.setMirror(saveSetBean.rotateAngle, saveSetBean.isOpenMirror)
                HikHelper.setTemperatureMode(saveSetBean.temperatureMode)

                val config: DataBean = ConfigRepository.readConfig(false)
                HikHelper.setEmissivity((config.radiation * 100).toInt())
                HikHelper.setDistance((config.distance * 100).toInt().coerceAtLeast(30))
            }
        }

        binding.titleView.setRight2Drawable(if (saveSetBean.isOpenAmplify) R.drawable.svg_tisr_on else R.drawable.svg_tisr_off)
        binding.titleView.setLeftClickListener {
            if (!binding.timeDownView.isRunning) {//未在延时拍照录像
                finish()
            }
        }
        binding.titleView.setRightClickListener {
            val config: DataBean = ConfigRepository.readConfig(false)
            val emText = IRConfigData.getTextByEmissivity(this, config.radiation)
            EmissivityTipPopup(this, false)
                .setDataBean(config.environment, config.distance, config.radiation, emText)
                .build()
                .showAsDropDown(binding.titleView, 0, 0, Gravity.END)
        }
        binding.titleView.setRight2ClickListener {
            switchAmplify()
        }



        binding.timeDownView.onFinishListener = {
            if (!saveSetBean.isVideoMode) {
                //延迟拍照倒计时结束后，要把中间按钮的状态重置，放出 拍照/录像 切换；视频本来就这个状态，不用改
                binding.menuSecondView.updateCameraModel()
            }
            centerCamera()
        }

        binding.hikSurfaceView.isOpenAmplify = saveSetBean.isOpenAmplify
        binding.hikSurfaceView.rotateAngle = saveSetBean.rotateAngle
        binding.hikSurfaceView.alarmBean = saveSetBean.alarmBean
        binding.hikSurfaceView.setPseudoCode(saveSetBean.pseudoColorMode)
        if (saveSetBean.isOpenAmplify) {
            lifecycleScope.launch(Dispatchers.IO) {
                SupHelp.getInstance().initA4KCPP()
            }
        }

        binding.cameraPreView.cameraPreViewCloseListener = {
            if (binding.cameraPreView.isPreviewing) {
                popupWindow?.dismiss()
                switchPinPState(false)
            }
        }
        if (saveSetBean.isOpenTwoLight && XXPermissions.isGranted(this, Permission.CAMERA)) {
            binding.cameraPreView.postDelayed(500) {
                switchPinPState(false)
            }
        }

        initPseudoBar() //初始化伪彩条
        initTempView()  //初始化点线面温度 View
        initCarDetect() //初始化汽车检测 View
        initMenu()      //初始化菜单
        refreshCustomPseudo(customPseudoBean)
        rotateUI(saveSetBean.isRotatePortrait())

        AlarmHelp.getInstance(this).updateData(saveSetBean.alarmBean)

        lifecycleScope.launch {
            delay(1000)
            showEmissivityTipsDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.menuSecondView.refreshImg()
        AlarmHelp.getInstance(this).onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        stopIfContinua()
        AlarmHelp.getInstance(this).pause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        super.onStop()
        binding.timeDownView.cancel()
        stopIfVideoing(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.tempLayout.stopAnimation()
        AlarmHelp.getInstance(this).onDestroy(saveSetBean.isSaveSetting)
    }

    override fun disConnected() {
        finish()
    }

    /**
     * 执行伪彩条的初始化
     */
    private fun initPseudoBar() {
        binding.clPseudoBar.isVisible = saveSetBean.isOpenPseudoBar
        if (ScreenTool.isIPad(this)) {
            binding.clPseudoBar.setPadding(0, SizeUtils.dp2px(40f), 0, SizeUtils.dp2px(40f))
        }

        binding.ivPseudoBarLock.setOnClickListener {
            if (it.isVisible) {//自定义伪彩模式下为 INVISIBLE
                it.isSelected = !it.isSelected
                binding.pseudoSeekBar.isEnabled = it.isSelected
                binding.pseudoSeekBar.leftSeekBar.indicatorBackgroundColor = if (it.isSelected) 0xffe17606.toInt() else 0
                binding.pseudoSeekBar.rightSeekBar.indicatorBackgroundColor = if (it.isSelected) 0xffe17606.toInt() else 0
                if (!it.isSelected) {//解锁->锁
                    limitTempMin = Float.MIN_VALUE
                    limitTempMax = Float.MAX_VALUE
                    binding.pseudoSeekBar.setRangeAndPro(limitTempMin, limitTempMax, currentMin, currentMax)
                }
            }
        }
        binding.ivPseudoBarInput.setOnClickListener {
            val intent = Intent(this, PseudoSetActivity::class.java)
            intent.putExtra(ExtraKeyConfig.IS_TC007, false)
            pseudoSetResult.launch(intent)
        }

        binding.pseudoSeekBar.isEnabled = false
        binding.pseudoSeekBar.setIndicatorTextDecimalFormat("0.0")
        binding.pseudoSeekBar.setPseudocode(saveSetBean.pseudoColorMode)
        binding.pseudoSeekBar.setOnRangeChangedListener(object : OnRangeChangedListener {
            override fun onRangeChanged(view: RangeSeekBar?, leftValue: Float, rightValue: Float, isFromUser: Boolean, tempMode: Int) {
                limitTempMin = if (tempMode == RangeSeekBar.TEMP_MODE_MIN || tempMode == RangeSeekBar.TEMP_MODE_INTERVAL) {
                    UnitTools.showToCValue(leftValue)
                } else {
                    Float.MIN_VALUE
                }
                limitTempMax = if (tempMode == RangeSeekBar.TEMP_MODE_MAX || tempMode == RangeSeekBar.TEMP_MODE_INTERVAL) {
                    UnitTools.showToCValue(rightValue)
                } else {
                    Float.MAX_VALUE
                }
            }

            override fun onStartTrackingTouch(view: RangeSeekBar?, isLeft: Boolean) {
            }

            override fun onStopTrackingTouch(view: RangeSeekBar?, isLeft: Boolean) {
            }
        })
    }

    /**
     * 执行点线面温度图层的初始化
     */
    @SuppressLint("SetTextI18n")
    private fun initTempView() {
        binding.temperatureView.mode = Mode.FULL
        binding.temperatureView.textColor = saveSetBean.tempTextColor
        binding.temperatureView.tempTextSize = saveSetBean.tempTextSize
        binding.temperatureView.rotateAngle = saveSetBean.rotateAngle
        binding.temperatureView.onTempChangeListener = { min, max ->
            AlarmHelp.getInstance(this).alarmData(max, min, binding.tempLayout)
            currentMin = UnitTools.showUnitValue(min)
            currentMax = UnitTools.showUnitValue(max)
            if (customPseudoBean.isUseCustomPseudo) {
                binding.tvTempContent.text = "Max:${UnitTools.showC(max)}\nMin:${UnitTools.showC(min)}"
            } else {
                val limitMin: Float = if (limitTempMin == Float.MIN_VALUE) Float.MIN_VALUE else UnitTools.showUnitValue(limitTempMin)
                val limitMax: Float = if (limitTempMax == Float.MAX_VALUE) Float.MAX_VALUE else UnitTools.showUnitValue(limitTempMax)
                binding.pseudoSeekBar.setRangeAndPro(limitMin, limitMax, currentMin, currentMax)
            }
            if (cameraState == CameraState.TAKING) {
                binding.clPseudoBar.requestLayout()
                binding.clPseudoBar.updateBitmap()
            }
        }
        binding.temperatureView.onTrendChangeListener = {
            binding.trendView.refreshChart(it)
        }
        binding.temperatureView.onTrendOperateListener = { isAdd ->
            if (isAdd) {
                if (hasClickTrendDel) {
                    hasClickTrendDel = false
                    binding.trendView.expand()
                }
            } else {
                binding.trendView.setToEmpty()
            }
        }
    }

    /**
     * 执行汽车检测提示 View 的初始化
     */
    private fun initCarDetect() {
        binding.llCarDetect.isVisible = intent.getBooleanExtra(ExtraKeyConfig.IS_CAR_DETECT_ENTER, false)
        binding.tvCarDetect.text = SharedManager.getCarDetectInfo().buildString()
        binding.ivCarDetect.setOnClickListener {
            LongTextDialog(this, SharedManager.getCarDetectInfo().item, SharedManager.getCarDetectInfo().description).show()
        }
        binding.tvCarDetect.setOnClickListener {
            CarDetectDialog(this) {
                binding.tvCarDetect.text = it.buildString()
            }
        }
    }

    /**
     * IOS 搞成点删除后再次绘制趋势图才自动弹出折线图，还得搞个变量跟着一起卷。
     */
    private var hasClickTrendDel = true
    /**
     * 执行菜单的初始化
     */
    private fun initMenu() {
        binding.menuFirstView.onTabClickListener = {
            popupWindow?.dismiss()
            binding.temperatureView.isEnabled = it.selectPosition == 1
            binding.menuSecondView.selectPosition(it.selectPosition)
        }

        binding.menuSecondView.onCameraClickListener = {
            when (it) {
                0 -> {//拍照 or 录像
                    when (cameraState) {
                        CameraState.INIT -> PermissionTool.requestFile(this) {
                            if (saveSetBean.delayCaptureSecond > 0) {//开启延迟拍照或录像
                                cameraState = CameraState.DELAY
                                binding.menuSecondView.setToRecord(true)
                                binding.timeDownView.downSecond(saveSetBean.delayCaptureSecond)
                            } else {
                                centerCamera()
                            }
                        }
                        CameraState.DELAY -> {//延迟中则取消
                            cameraState = CameraState.INIT
                            binding.timeDownView.cancel()
                            binding.menuSecondView.updateCameraModel()
                        }
                        CameraState.TAKING -> {//拍照录像中则取消
                            if (saveSetBean.isVideoMode) {//录像 录制中->结束录制
                                stopIfVideoing()
                            } else {//拍照 连续拍照或拍照->取消连续拍照
                                stopIfContinua()
                            }
                        }
                    }
                }
                1 -> {//图库
                    lifecycleScope.launch {
                        stopIfVideoing()
                        delay(500)
                        ARouter.getInstance()
                            .build(RouterConfig.IR_GALLERY_HOME)
                            .withInt(ExtraKeyConfig.DIR_TYPE, GalleryRepository.DirType.LINE.ordinal)
                            .navigation()
                    }
                }
                2 -> {//更多菜单
                    showCameraItemPopup()
                }
                3 -> {//切换到拍照
                    stopIfContinua()
                    saveSetBean.isVideoMode = false
                }
                4 -> {//切换到录像
                    stopIfContinua()
                    saveSetBean.isVideoMode = true
                }
            }
        }
        binding.menuSecondView.onFenceListener = { fenceType, isSelected ->
            when (fenceType) {
                FenceType.POINT -> binding.temperatureView.mode = Mode.POINT
                FenceType.LINE -> binding.temperatureView.mode = Mode.LINE
                FenceType.RECT -> binding.temperatureView.mode = Mode.RECT
                FenceType.FULL -> binding.temperatureView.isShowFull = isSelected
                FenceType.TREND -> {
                    binding.temperatureView.mode = Mode.TREND
                    if (SharedManager.isNeedShowTrendTips) {
                        NotTipsSelectDialog(this)
                            .setTipsResId(R.string.thermal_trend_tips)
                            .setOnConfirmListener {
                                SharedManager.isNeedShowTrendTips = !it
                            }
                            .show()
                    }
                    if (!binding.trendView.isVisible) {//当前趋势图如果已显示着的话，则不去更改
                        binding.trendView.isVisible = true
                        binding.trendView.close()
                    }
                }
                FenceType.DEL -> {
                    hasClickTrendDel = true
                    binding.temperatureView.mode = Mode.CLEAR
                    binding.trendView.isVisible = false
                }
            }
        }
        binding.menuSecondView.onTwoLightListener = { twoLightType, isSelected ->
            popupWindow?.dismiss()
            when (twoLightType) {
                TwoLightType.P_IN_P -> {//画中画
                    switchPinPState(true)
                }
                TwoLightType.BLEND_EXTENT -> {//融合度
                    if (!binding.cameraPreView.isPreviewing && isSelected) {
                        //未开启画中画开启融合度设置，需要把画中画打开
                        switchPinPState(false)
                    }
                    if (isSelected) {
                        showBlendExtentPopup()
                    }
                }
                else -> {//其他不用处理，不是双光设备

                }
            }
        }
        binding.menuSecondView.onColorListener = { _, it, _ ->
            if (customPseudoBean.isUseCustomPseudo) {
                TipDialog.Builder(this)
                    .setTitleMessage(getString(R.string.app_tip))
                    .setMessage(R.string.tip_change_pseudo_mode)
                    .setPositiveListener(R.string.app_yes) {
                        customPseudoBean.isUseCustomPseudo = false
                        customPseudoBean.saveToShared()

                        saveSetBean.pseudoColorMode = it
                        binding.hikSurfaceView.setPseudoCode(it)
                        binding.pseudoSeekBar.setPseudocode(it)
                        refreshCustomPseudo(customPseudoBean)
                    }.setCancelListener(R.string.app_no) {

                    }
                    .create().show()
            } else {
                saveSetBean.pseudoColorMode = it
                binding.hikSurfaceView.setPseudoCode(it)
                binding.pseudoSeekBar.setPseudocode(it)
            }
        }
        binding.menuSecondView.onSettingListener = { type, isSelected ->
            popupWindow?.dismiss()
            when (type) {
                SettingType.PSEUDO_BAR -> {//伪彩条
                    saveSetBean.isOpenPseudoBar = !saveSetBean.isOpenPseudoBar
                    binding.clPseudoBar.isVisible = saveSetBean.isOpenPseudoBar
                    binding.menuSecondView.setSettingSelected(SettingType.PSEUDO_BAR, saveSetBean.isOpenPseudoBar)
                }
                SettingType.CONTRAST -> {//对比度
                    if (!isSelected) {
                        showContrastPopup()
                    }
                }
                SettingType.DETAIL -> {//细节
                    if (!isSelected) {
                        showSharpnessPopup()
                    }
                }
                SettingType.ALARM -> {//预警
                    showTempAlarmSetDialog()
                }
                SettingType.ROTATE -> {//旋转
                    saveSetBean.rotateAngle = if (saveSetBean.rotateAngle == 0) 270 else (saveSetBean.rotateAngle - 90)
                    binding.menuSecondView.setSettingRotate(saveSetBean.rotateAngle)

                    binding.hikSurfaceView.rotateAngle = saveSetBean.rotateAngle

                    binding.temperatureView.rotateAngle = saveSetBean.rotateAngle
                    binding.temperatureView.mode = Mode.CLEAR
                    binding.temperatureView.mode = Mode.FULL
                    binding.menuSecondView.fenceSelectType = FenceType.FULL

                    hasClickTrendDel = true
                    binding.trendView.isVisible = false

                    rotateUI(saveSetBean.isRotatePortrait())
                }
                SettingType.FONT -> {//字体颜色
                    val colorPickDialog = ColorPickDialog(this, saveSetBean.tempTextColor, saveSetBean.tempTextSize)
                    colorPickDialog.onPickListener = { color, size ->
                        saveSetBean.tempTextColor = color
                        saveSetBean.tempTextSize = SizeUtils.sp2px(size.toFloat())
                        binding.temperatureView.textColor = saveSetBean.tempTextColor
                        binding.temperatureView.tempTextSize = saveSetBean.tempTextSize
                        binding.menuSecondView.setSettingSelected(SettingType.FONT, !saveSetBean.isTempTextDefault())
                    }
                    colorPickDialog.show()
                }
                SettingType.MIRROR -> {//镜像
                    saveSetBean.isOpenMirror = !isSelected
                    binding.menuSecondView.setSettingSelected(SettingType.MIRROR, !isSelected)
                    lifecycleScope.launch {
                        HikHelper.setMirror(saveSetBean.rotateAngle, !isSelected)
                    }
                }
                SettingType.COMPASS -> {//指南针
                    //指南针只有 TS001 才有
                }
                SettingType.WATERMARK -> {
                    //水印菜单只有 2D 编辑才有
                }
            }
        }
        binding.menuSecondView.onTempLevelListener = {
            saveSetBean.temperatureMode = it
            lifecycleScope.launch {
                showLoadingDialog()
                HikHelper.setTemperatureMode(saveSetBean.temperatureMode)
                dismissLoadingDialog()
            }
            if (it == CameraItemBean.TYPE_TMP_H && SharedManager.isTipHighTemp) {//切换到高温档
                TipShutterDialog.Builder(this)
                    .setTitle(R.string.tc_high_temp_test)
                    .setMessage(
                        SpanBuilder(getString(R.string.tc_high_temp_test_tips1))
                            .appendDrawable(this, R.drawable.svg_title_temp, SizeUtils.sp2px(24f))
                            .append(getString(R.string.tc_high_temp_test_tips2))
                    )
                    .setCancelListener { isCheck ->
                        SharedManager.isTipHighTemp = !isCheck
                    }
                    .create().show()
            }
            if (it == CameraItemBean.TYPE_TMP_ZD) {
                ToastTools.showShort(R.string.auto_open)
            }
        }

        binding.menuSecondView.isVideoMode = saveSetBean.isVideoMode //恢复拍照/录像状态
        binding.menuSecondView.fenceSelectType = FenceType.FULL //默认选中全图
        binding.menuSecondView.setPseudoColor(saveSetBean.pseudoColorMode) //选中伪彩
        binding.menuSecondView.setSettingRotate(saveSetBean.rotateAngle) //旋转角度
        binding.menuSecondView.setSettingSelected(SettingType.MIRROR, saveSetBean.isOpenMirror) //镜像
        binding.menuSecondView.setSettingSelected(SettingType.PSEUDO_BAR, saveSetBean.isOpenPseudoBar) //伪彩条
        binding.menuSecondView.setSettingSelected(SettingType.ALARM, saveSetBean.alarmBean.isOpen()) //温度报警
        binding.menuSecondView.setSettingSelected(SettingType.FONT, !saveSetBean.isTempTextDefault())//字体颜色
        binding.menuSecondView.setTempLevel(saveSetBean.temperatureMode) //温度档位
        binding.menuSecondView.isUnitF = SharedManager.getTemperature() == 0 //温度档位单位
    }


    /**
     * 当前 拍照/录像 状态.
     */
    private var cameraState = CameraState.INIT
    private enum class CameraState {
        /** 拍照或录像状态-初始状态 */
        INIT,
        /** 拍照或录像状态-延迟拍照、延迟录像中 */
        DELAY,
        /** 拍照中、连续拍照中、录像中 */
        TAKING,
    }

    /**
     * 执行连续拍照的 Job
     */
    private var continuousJob: Job? = null
    /**
     * 如果正在连续拍照，则结束连接拍照.
     */
    private fun stopIfContinua() {
        if (cameraState == CameraState.TAKING) {
            continuousJob?.cancel()
            cameraState = CameraState.INIT
            binding.menuSecondView.refreshImg()
            binding.tvContinuousTips.isVisible = false
        }
    }

    /**
     * 执行 拍照 或 录像 操作.
     */
    private fun centerCamera() {
        if (saveSetBean.isVideoMode) {//录制视频
            startVideo()
        } else {//拍照
            cameraState = CameraState.TAKING
            val continuousBean = SharedManager.continuousBean
            if (continuousBean.isOpen) {
                continuousJob = lifecycleScope.launch {
                    binding.tvContinuousTips.isVisible = true
                    flow {
                        repeat(continuousBean.count) {
                            emit(it)
                            delay(continuousBean.continuaTime)
                        }
                    }.collect {
                        camera()
                    }
                    cameraState = CameraState.INIT
                    binding.tvContinuousTips.isVisible = false
                }
            } else {
                lifecycleScope.launch {
                    camera()
                    cameraState = CameraState.INIT
                }
            }
        }
    }

    /**
     * 执行视频软编码的工具类
     */
    private var videoRecord: VideoRecordFFmpeg? = null
    /**
     * 录制视频时计时 Job
     */
    private var videoTimeJob: Job? = null

    /**
     * 开始录像
     */
    private fun startVideo() {
        if (!VideoRecordFFmpeg.canStartVideoRecord(this)) {
            return
        }
        cameraState = CameraState.TAKING
        binding.clPseudoBar.updateBitmap()
        binding.menuSecondView.setToRecord(false)

        videoRecord = VideoRecordFFmpeg(
            binding.hikSurfaceView,
            binding.cameraPreView,
            binding.temperatureView,
            false,
            binding.clPseudoBar,
            binding.tempLayout,
            carView = binding.llCarDetect
        )
        videoRecord?.stopVideoRecordListener = { isShowVideoRecordTips ->
            runOnUiThread {
                if (isShowVideoRecordTips) {
                    TipDialog.Builder(this)
                        .setMessage(R.string.tip_video_record)
                        .create().show()
                }
                stopIfVideoing()
            }
        }
        videoRecord?.updateAudioState(saveSetBean.isRecordAudio)
        videoRecord?.startRecord()

        videoTimeJob = lifecycleScope.launch {
            flow {
                repeat(60 * 60 * 2) {
                    emit(it)
                    delay(1000)
                }
            }.collect {
                binding.tvVideoTime.text = TimeTool.showVideoTime(it * 1000L)
                //实际上的1小时检测在视频录制里触发，故而这里结束不需要处理
            }
        }
        binding.tvVideoTime.isVisible = true
    }

    /**
     * 如果正在进行录像，则停止录像.
     * @param isLifecycle 是否使用 lifecycleScope
     */
    private fun stopIfVideoing(isLifecycle: Boolean = true) {
        if (cameraState == CameraState.TAKING) {
            cameraState = CameraState.INIT
            videoRecord?.stopRecord()

            videoTimeJob?.cancel()
            videoTimeJob = null
            binding.tvVideoTime.isVisible = false

            val scope = if (isLifecycle) lifecycleScope else CoroutineScope(Dispatchers.Main)
            scope.launch(Dispatchers.Main) {
                delay(500)
                binding.menuSecondView.refreshImg()
                EventBus.getDefault().post(GalleryAddEvent())
            }
        }
    }

    /**
     * 执行一次拍照操作.
     */
    private suspend fun camera() = withContext(Dispatchers.Default) {
        withContext(Dispatchers.Main) {
            binding.menuSecondView.setToCamera()
        }

        var bitmap: Bitmap = binding.hikSurfaceView.getScaleBitmap()

        //画中画
        if (saveSetBean.isOpenTwoLight) {
            bitmap = BitmapUtils.mergeBitmapByViewNonNull(bitmap, binding.cameraPreView.getBitmap(), binding.cameraPreView)
            //保存了画中画，但是没有使用，这里保持与旧逻辑一致，也保存一下好了
            ImageUtils.saveImageToApp(bitmap)
        }

        //伪彩条
        if (binding.clPseudoBar.isVisible) {
            val seekBarBitmap = binding.clPseudoBar.drawToBitmap()
            bitmap = BitmapUtils.mergeBitmap(
                bitmap,
                seekBarBitmap,
                bitmap.width - seekBarBitmap.width,
                (bitmap.height - seekBarBitmap.height) / 2,
            )
        }

        //温度、点线面
        if (binding.temperatureView.mode != Mode.CLEAR) {
            binding.temperatureView.draw(Canvas(bitmap))
        }

        //汽车检测
        if (binding.llCarDetect.isVisible) {
            bitmap = BitmapUtils.mergeBitmap(bitmap, binding.llCarDetect.drawToBitmap(), 0, 0)
        }

        //水印
        val watermarkBean: WatermarkBean = SharedManager.watermarkBean
        if (watermarkBean.isOpen) {
            bitmap = BitmapUtils.drawCenterLable(
                bitmap,
                watermarkBean.title,
                watermarkBean.address,
                if (watermarkBean.isAddTime) TimeTool.getNowTime() else "",
                if (binding.pseudoSeekBar.isVisible) binding.pseudoSeekBar.measuredWidth else 0
            )
        }

        val filename: String = ImageUtils.save(bitmap)

        val emConfig: DataBean = ConfigRepository.readConfig(false)
        val capital = FrameStruct.toCode(
            name = ProductType.PRODUCT_NAME_TC002C_DUO,
            width = if (bitmap.width > bitmap.height) 256 else 192,
            height = if (bitmap.width > bitmap.height) 192 else 256,
            rotate = saveSetBean.rotateAngle,
            pseudo = saveSetBean.pseudoColorMode,
            initRotate = saveSetBean.rotateAngle,
            correctRotate = saveSetBean.rotateAngle,
            customPseudoBean = customPseudoBean,
            isShowPseudoBar = binding.clPseudoBar.isVisible,
            textColor = saveSetBean.tempTextColor,
            watermarkBean = watermarkBean,
            alarmBean = saveSetBean.alarmBean,
            gainStatus = 0,
            textSize = saveSetBean.tempTextSize,
            environment = emConfig.environment,
            distance = emConfig.distance,
            radiation = emConfig.radiation,
            isAmplify = saveSetBean.isOpenAmplify
        )
        ImageUtils.saveFrame(bs = HikHelper.copyFrameData(), capital = capital, name = filename)

        withContext(Dispatchers.Main) {
            binding.menuSecondView.refreshImg()
        }
        EventBus.getDefault().post(GalleryAddEvent())
    }

    /**
     * 切换 画中画 开启或关闭 状态
     * @param isShowTips 是否需要显示操作指引弹框.
     */
    private fun switchPinPState(isShowTips: Boolean) {
        if (!CheckDoubleClick.isFastDoubleClick()) {//防止手速快的用户快速点击
            if (binding.cameraPreView.isPreviewing) {//开启->关闭
                saveSetBean.isOpenTwoLight = false
                binding.cameraPreView.isVisible = false
                binding.cameraPreView.closeCamera()
                binding.menuSecondView.setTwoLightSelected(TwoLightType.P_IN_P, false)
            } else {//关闭->开启
                PermissionTool.requestCamera(this) {
                    saveSetBean.isOpenTwoLight = true
                    binding.cameraPreView.isVisible = true
                    binding.cameraPreView.setCameraAlpha(saveSetBean.twoLightAlpha / 100f)
                    binding.cameraPreView.post {
                        binding.cameraPreView.openCamera()
                    }
                    binding.menuSecondView.setTwoLightSelected(TwoLightType.P_IN_P, true)

                    if (isShowTips && SharedManager.isTipPinP) {
                        val dialog = TipPreviewDialog.newInstance()
                        dialog.closeEvent = {
                            SharedManager.isTipPinP = !it
                        }
                        dialog.show(supportFragmentManager, "")
                    }
                }
            }
        }
    }

    /**
     * 执行 UI 相关旋转操作。
     * 旋转除了更改机芯旋转角度外，还需要更改 APP 的显示容器尺寸、温度 View 尺寸。
     * @param isPortrait true192x256 false-256x192
     */
    private fun rotateUI(isPortrait: Boolean) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.clRoot)
        constraintSet.setDimensionRatio(R.id.thermal_lay, (if (isPortrait) 192/256f else (256/192f)).toString())
        constraintSet.applyTo(binding.clRoot)

        binding.clPseudoBar.requestLayout()
        binding.clPseudoBar.updateBitmap()
    }


    private val pseudoSetResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            lifecycleScope.launch {
                val customPseudoBean: CustomPseudoBean = it.data?.getParcelableExtra(ExtraKeyConfig.CUSTOM_PSEUDO_BEAN) ?: CustomPseudoBean()
                customPseudoBean.saveToShared()
                refreshCustomPseudo(customPseudoBean)
            }
        }
    }

    /**
     * 刷新自定义渲染影响的相关内容.
     */
    private fun refreshCustomPseudo(newData: CustomPseudoBean) {
        val isCustom: Boolean = newData.isUseCustomPseudo
        binding.tvTempContent.isVisible = isCustom
        binding.ivPseudoBarLock.isInvisible = isCustom
        binding.ivPseudoBarInput.setImageResource(if (isCustom) R.drawable.ir_model else R.drawable.ic_color_edit)
        binding.pseudoSeekBar.setColorList(newData.getColorList()?.reversedArray())
        binding.pseudoSeekBar.setPlaces(newData.getPlaceList())
        binding.hikSurfaceView.refreshCustomPseudo(newData)
        binding.menuSecondView.setPseudoColor(if (isCustom) -1 else saveSetBean.pseudoColorMode)

        //动态渲染或自定义渲染切换时，要将等温尺重置
        limitTempMin = Float.MIN_VALUE
        limitTempMax = Float.MAX_VALUE
        binding.pseudoSeekBar.setRangeAndPro(limitTempMin, limitTempMax, currentMin, currentMax)

        if (isCustom) {//使用自定义渲染
            val min: Float = UnitTools.showUnitValue(newData.minTemp)
            val max: Float = UnitTools.showUnitValue(newData.maxTemp)
            binding.pseudoSeekBar.isEnabled = false
            binding.ivPseudoBarLock.isSelected = false
            binding.pseudoSeekBar.setRangeAndPro(min, max, min, max)
            binding.pseudoSeekBar.leftSeekBar.indicatorBackgroundColor = 0
            binding.pseudoSeekBar.rightSeekBar.indicatorBackgroundColor = 0
        }
        this.customPseudoBean = newData
    }

    /**
     * 切换超分开关
     */
    private fun switchAmplify() {
        if (SupHelp.getInstance().loadOpenclSuccess) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        SupHelp.getInstance().initA4KCPP()
                    } catch (e: UnsatisfiedLinkError) {
                        SupHelp.getInstance().loadOpenclSuccess = false
                        XLog.e("超分初始化失败")
                    }
                }
                if (!SupHelp.getInstance().loadOpenclSuccess) {
                    TipDialog.Builder(this@IRThermalHikActivity)
                        .setMessage(R.string.tips_tisr_fail)
                        .setPositiveListener(R.string.app_got_it) {
                        }
                        .create().show()
                    return@launch
                }
                saveSetBean.isOpenAmplify = !saveSetBean.isOpenAmplify
                binding.hikSurfaceView.isOpenAmplify = saveSetBean.isOpenAmplify
                binding.titleView.setRight2Drawable(if (saveSetBean.isOpenAmplify) R.drawable.svg_tisr_on else R.drawable.svg_tisr_off)
                if (saveSetBean.isOpenAmplify) {
                    ToastUtils.showShort(R.string.tips_tisr_on)
                } else {
                    ToastUtils.showShort(R.string.tips_tisr_off)
                }
            }
        } else {
            TipDialog.Builder(this)
                .setMessage(R.string.tips_tisr_fail)
                .setPositiveListener(R.string.app_got_it) {
                }
                .create().show()
        }
    }


    /**
     * 显示材料发射率提示弹窗.
     */
    private fun showEmissivityTipsDialog() {
        if (SharedManager.isHideEmissivityTips) {
            return
        }
        val config: DataBean = ConfigRepository.readConfig(false)
        val emText = IRConfigData.getTextByEmissivity(this, config.radiation)
        val dialog = TipEmissivityDialog.Builder(this)
            .setDataBean(config.environment, config.distance, config.radiation, emText)
            .create()
        dialog.onDismissListener = {
            SharedManager.isHideEmissivityTips = it
        }
        dialog.show()
    }

    /**
     * 显示 拍照/录像 菜单
     */
    private fun showCameraItemPopup() {
        //这个菜单跟融合度、对比度、锐度不太一样，只有点菜单时才切换显示隐藏/隐藏
        if (popupWindow?.isShowing == true) {
            popupWindow?.dismiss()
            return
        }
        val cameraItemPopup = CameraItemPopup(this, saveSetBean)
        cameraItemPopup.onDelayClickListener = { !binding.timeDownView.isRunning }
        cameraItemPopup.onAutoCLickListener = {
            lifecycleScope.launch {
                HikHelper.setAutoShutter(it)
            }
        }
        cameraItemPopup.onShutterClickListener = {
            cameraItemPopup.isShutterSelect = true
            ToastUtils.showShort(R.string.app_Manual_Shutter)
            lifecycleScope.launch {
                HikHelper.shutter()
                cameraItemPopup.isShutterSelect = false
            }
        }
        cameraItemPopup.onAudioCLickListener = {
            if (saveSetBean.isRecordAudio) {//开启->关闭
                saveSetBean.isRecordAudio = false
                cameraItemPopup.isAudioSelect = false
                videoRecord?.updateAudioState(false)
            } else {//关闭->开启
                PermissionTool.requestRecordAudio(this) {
                    saveSetBean.isRecordAudio = true
                    cameraItemPopup.isAudioSelect = true
                    videoRecord?.updateAudioState(true)
                }
            }
        }
        cameraItemPopup.showAsUp(binding.menuSecondView)
        popupWindow = cameraItemPopup
    }

    /**
     * 显示融合度设置弹框
     */
    private fun showBlendExtentPopup() {
        val seekBarPopup = SeekBarPopup(this, true)
        seekBarPopup.isRealTimeTrigger = true
        seekBarPopup.progress = saveSetBean.twoLightAlpha
        seekBarPopup.onValuePickListener = {
            saveSetBean.twoLightAlpha = it
            binding.cameraPreView.setCameraAlpha(it / 100f)
        }
        seekBarPopup.setOnDismissListener {
            binding.menuSecondView.setTwoLightSelected(TwoLightType.BLEND_EXTENT, false)
        }
        seekBarPopup.show(binding.thermalLay, !saveSetBean.isRotatePortrait())
        popupWindow = seekBarPopup
    }

    /**
     * 显示对比度设置 PopupWindow
     */
    private fun showContrastPopup() {
        binding.menuSecondView.setSettingSelected(SettingType.CONTRAST, true)

        val maxContrast = 255 //saveSettingBean 里对比度取值 [0, 255]
        val seekBarPopup = SeekBarPopup(this)
        seekBarPopup.progress = (saveSetBean.contrastValue * 100 / 255f).toInt()
        seekBarPopup.onValuePickListener = {
            saveSetBean.contrastValue = (it * maxContrast / 100f).toInt().coerceAtMost(maxContrast)
            lifecycleScope.launch {
                HikHelper.setContrast(it)
            }
        }
        seekBarPopup.setOnDismissListener {
            binding.menuSecondView.setSettingSelected(SettingType.CONTRAST, false)
        }
        seekBarPopup.show(binding.thermalLay, !saveSetBean.isRotatePortrait())
        popupWindow = seekBarPopup
    }

    /**
     * 显示细节(锐度) 设置 PopupWindow
     */
    private fun showSharpnessPopup() {
        binding.menuSecondView.setSettingSelected(SettingType.DETAIL, true)

        val maxSharpness = 4 //实际对比度取值 [0, 4]，用于百分比转换
        val seekBarPopup = SeekBarPopup(this)
        seekBarPopup.progress = (saveSetBean.ddeConfig * 100f / maxSharpness).toInt()
        seekBarPopup.onValuePickListener = {
            saveSetBean.ddeConfig = (it * maxSharpness / 100f).roundToInt()
            val newProgress = (saveSetBean.ddeConfig * 100f / maxSharpness).toInt()
            seekBarPopup.progress = newProgress
            lifecycleScope.launch {
                HikHelper.setEnhanceLevel(newProgress)
            }
        }
        seekBarPopup.setOnDismissListener {
            binding.menuSecondView.setSettingSelected(SettingType.DETAIL, false)
        }
        seekBarPopup.show(binding.thermalLay, !saveSetBean.isRotatePortrait())
        popupWindow = seekBarPopup
    }

    /**
     * 显示温度报警设置弹窗.
     */
    private fun showTempAlarmSetDialog() {
        val tempAlarmSetDialog = TempAlarmSetDialog(this, false)
        tempAlarmSetDialog.alarmBean = saveSetBean.alarmBean
        tempAlarmSetDialog.onSaveListener = {
            saveSetBean.alarmBean = it
            binding.menuSecondView.setSettingSelected(SettingType.ALARM, it.isOpen())
            binding.hikSurfaceView.alarmBean = it
            AlarmHelp.getInstance(this).updateData(
                if (it.isLowOpen) it.lowTemp else null,
                if (it.isHighOpen) it.highTemp else null,
                if (it.isRingtoneOpen) it.ringtoneType else null
            )
        }
        tempAlarmSetDialog.show()
    }
}