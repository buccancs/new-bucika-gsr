package com.topdon.tc001

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.blankj.utilcode.util.SizeUtils
import com.elvishew.xlog.XLog
import com.energy.iruvc.ircmd.IRCMDType
import com.energy.iruvc.ircmd.IRUtils
import com.energy.iruvc.utils.CommonParams
import com.example.thermal_lite.IrConst
import com.example.thermal_lite.util.CommonUtil
import com.example.thermal_lite.util.IRTool
import com.infisense.usbir.utils.OpencvTools
import com.topdon.lib.core.utils.BitmapUtils
import com.infisense.usbir.utils.PseudocodeUtils.changePseudocodeModeByOld
import com.infisense.usbir.view.ITsTempListener
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.bean.event.ReportCreateEvent
import com.topdon.lib.core.common.ProductType.PRODUCT_NAME_TC001LITE
import com.topdon.lib.core.common.ProductType.PRODUCT_NAME_TS
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.ScreenTool
import com.topdon.lib.core.tools.TimeTool
import com.topdon.lib.core.tools.ToastTools
import com.topdon.lib.core.tools.UnitTools
import com.topdon.lib.core.tools.UnitTools.showToCValue
import com.topdon.lib.core.tools.UnitTools.showUnitValue
import com.topdon.lib.core.utils.ImageUtils
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.dialog.TipWaterMarkDialog
import com.topdon.lib.core.utils.Constants.IS_REPORT_FIRST
import com.topdon.lib.core.utils.ScreenUtil

import com.topdon.libcom.dialog.ColorPickDialog
import com.topdon.libcom.dialog.TempAlarmSetDialog
import com.topdon.lms.sdk.LMS.mContext
import com.topdon.menu.constant.FenceType
import com.topdon.menu.constant.SettingType
import com.topdon.thermal.event.GalleryAddEvent
import com.topdon.thermal.event.ImageGalleryEvent
import com.topdon.thermal.frame.FrameStruct
import com.topdon.thermal.frame.FrameTool
import com.topdon.thermal.frame.ImageParams
import com.topdon.thermal.report.bean.ImageTempBean
import com.topdon.thermal.view.TemperatureBaseView.Mode
import com.topdon.thermal.viewmodel.IRGalleryEditViewModel
import com.topdon.pseudo.activity.PseudoSetActivity
import com.topdon.pseudo.bean.CustomPseudoBean
import com.topdon.tc001.databinding.ActivityIrGalleryEditBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

/**
 * Professional IR Gallery Image Editing Activity for BucikaGSR Research Application
 * 
 * This activity provides comprehensive thermal image editing capabilities including:
 * - Thermal image display with temperature overlay and analysis
 * - Point, line, and rectangular temperature measurement tools
 * - Pseudo-color palette customization and temperature range adjustment
 * - Real-time temperature correction and calibration
 * - Watermark overlay and alarm threshold configuration
 * - Professional image export with merged temperature data layers
 * - Research-grade report generation integration
 * 
 * **TC001 Device Support:**
 * - This activity is specifically optimized for TC001 thermal imaging devices
 * - Supports 256x192 thermal resolution with professional temperature accuracy
 * - Integrates with TAU calibration data for precise temperature measurements
 * - Provides dual-light imaging capabilities for enhanced analysis
 * 
 * **Research Application Features:**
 * - Professional temperature measurement tools for clinical studies
 * - Comprehensive data export capabilities for research workflows
 * - Advanced pseudo-color mapping for enhanced thermal visualization
 * - Real-time temperature overlay with customizable display options
 * - Professional watermarking system for research documentation
 * 
 * @author BucikaGSR Development Team
 * @since 2024.1.0
 * @see TemperatureEditView For temperature measurement overlay functionality
 * @see FrameTool For thermal image processing and analysis
 * @see IRGalleryEditViewModel For business logic and data management
 */
@Route(path = RouterConfig.IR_GALLERY_EDIT)
class IRGalleryEditActivity : BaseActivity(), View.OnClickListener, ITsTempListener {

    /**
     * ViewBinding instance for type-safe view access
     * Replaces deprecated Kotlin synthetics with modern binding pattern
     */
    private lateinit var binding: ActivityIrGalleryEditBinding


    private var isShowC: Boolean = false

    /**
     * Device type flag - Always false for bucika_gsr as only TC001 is supported.
     * TC001 is not TC007, so this is always false.
     */
    private var isTC007 = false

    private val imageWidth = 256
    private val imageHeight = 192
    private val viewModel: IRGalleryEditViewModel by viewModels()
    private var filePath = ""

    //    private var mCapital = ByteArray(1024)
    private var mFrame = ByteArray(192 * 256 * 4)
    private val frameTool by lazy { FrameTool() }

    //图像参数
    private var pseudocodeMode = 3
    private var leftValue = 0f
    private var rightValue = 10000f
    private var max = 10000f
    private var min = 0f
    private var rotate = ImageParams.ROTATE_270
    private var struct: FrameStruct = FrameStruct() //首部信息
    private var ts_data_H: ByteArray? = null
    private var ts_data_L: ByteArray? = null

    override fun initContentView(): Int {
        binding = ActivityIrGalleryEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        return 0 // ViewBinding handles layout inflation
    }

    override fun initView() {
        initIntent()
        initUI()
        initListener()
        initRecycler()
        initObserve()
    }

    private fun initIntent() {
        lifecycleScope.launch{
            // Removed TS001 asset loading since only TC001 is supported

            if (BaseApplication.instance.tau_data_H == null){
                BaseApplication.instance.tau_data_H = CommonUtil.getAssetData(mContext, IrConst.TAU_HIGH_GAIN_ASSET_PATH)
            }
            if (BaseApplication.instance.tau_data_L == null){
                BaseApplication.instance.tau_data_L = CommonUtil.getAssetData(mContext, IrConst.TAU_LOW_GAIN_ASSET_PATH)
            }
        }
        if (intent.hasExtra(ExtraKeyConfig.FILE_ABSOLUTE_PATH)) {
            filePath = intent.getStringExtra(ExtraKeyConfig.FILE_ABSOLUTE_PATH)!!
        }
        isReportPick = intent.getBooleanExtra(ExtraKeyConfig.IS_PICK_REPORT_IMG, false)
        // Always false for bucika_gsr since only TC001 is supported (TC001 is not TC007)
        isTC007 = false

        binding.editRecyclerSecond.fenceSelectType = FenceType.DEL
        binding.temperatureView.isShowName = isReportPick
        binding.temperatureView.mode = Mode.CLEAR
        binding.temperatureView.setITsTempListener(this)
        // Removed TC007-specific logic since only TC001 is supported
    }

    private fun initObserve() {
        viewModel.resultLiveData.observe(this) {
//            System.arraycopy(it.capital, 0, mCapital, 0, it.capital.size)
            System.arraycopy(it.frame, 0, mFrame, 0, it.frame.size)
            showImage(it.capital, it.frame)
        }
    }

    override fun initData() {
        viewModel.initData(filePath)

        binding.editRecyclerFirst.isBarSelect = true
        binding.colorBarView.isVisible = true
    }

    private fun initListener() {
        binding.temperatureIvLock.setOnClickListener(this)
        binding.temperatureIvInput.setOnClickListener(this)
    }

    private fun setRotate(rotate: ImageParams) {
        if (rotate == ImageParams.ROTATE_270 || rotate == ImageParams.ROTATE_90) {
            binding.temperatureView.setImageSize(imageHeight, imageWidth)
        } else {
            binding.temperatureView.setImageSize(imageWidth, imageHeight)
        }
    }


    @SuppressLint("SetTextI18n")
    private fun showImage(capital: ByteArray, frame: ByteArray) {
        lifecycleScope.launch {
            frameTool.read(frame)
            struct = FrameStruct(capital)
            frameTool.initStruct(struct)
            isShowC = SharedManager.getTemperature() == 1
            rotate = frameTool.initRotate()
            pseudocodeMode = struct.pseudo
            setRotate(rotate)
            delay(200)
            updateImage(
                frameTool.getScrPseudoColorScaledBitmap(
                    changePseudocodeModeByOld(pseudocodeMode),
                    rotate = rotate,
                    customPseudoBean = struct.customPseudoBean,
                    maxTemperature = tempCorrect(frameTool.getSrcTemp().maxTemperature),
                    minTemperature = tempCorrect(frameTool.getSrcTemp().minTemperature),
                    isAmplify = struct.isAmplify
                )
            )

            val tempResult = frameTool.getSrcTemp()
            if (!struct.customPseudoBean.isUseCustomPseudo) {
                struct.customPseudoBean.maxTemp = tempCorrect(tempResult.maxTemperature)
                struct.customPseudoBean.minTemp = tempCorrect(tempResult.minTemperature)
                binding.editRecyclerSecond.setPseudoColor(pseudocodeMode)
            }
//        伪彩条默认处于打开状态
//        binding.colorBarView.isVisible = struct.isShowPseudoBar
//        adapter.enPseudoColorBar(struct.isShowPseudoBar)

            binding.editRecyclerSecond.setSettingSelected(SettingType.ALARM, struct.alarmBean.isHighOpen || struct.alarmBean.isLowOpen)
            binding.editRecyclerSecond.setSettingSelected(SettingType.WATERMARK, struct.watermarkBean.isOpen)
            binding.editRecyclerSecond.setSettingSelected(SettingType.FONT,
                struct.textColor != 0xffffffff.toInt() || struct.textSize != SizeUtils.sp2px(14f))
            binding.temperatureView.textColor = struct.textColor
            binding.temperatureView.tempTextSize = struct.textSize
            binding.temperatureView.setData(frameTool.getTempBytes(rotate = rotate))
            updateTemperatureSeekBar(false, R.drawable.svg_pseudo_bar_lock, "lock")//加锁
            if (struct.customPseudoBean.isUseCustomPseudo) {
                binding.tvTempContent.visibility = View.VISIBLE
                binding.tvTempContent.text = "Max:${UnitTools.showC(tempCorrect(tempResult.maxTemperature),isShowC)}\nMin:${UnitTools.showC(tempCorrect(tempResult.minTemperature),isShowC)}"
                rightValue = showUnitValue(struct.customPseudoBean.maxTemp,isShowC)
                leftValue = showUnitValue(struct.customPseudoBean.minTemp,isShowC)
                binding.temperatureIvInput.setImageResource(R.drawable.ir_model)
                binding.temperatureIvLock.visibility = View.INVISIBLE
            } else {
                binding.tvTempContent.visibility = View.GONE
                binding.tvTempContent.text = "Max:${UnitTools.showC(tempCorrect(tempResult.maxTemperature),isShowC)}\nMin:${UnitTools.showC(tempCorrect(tempResult.minTemperature),isShowC)}"
                rightValue = showUnitValue(tempCorrect(tempResult.maxTemperature),isShowC)
                leftValue = showUnitValue(tempCorrect(tempResult.minTemperature),isShowC)
                binding.temperatureIvInput.setImageResource(R.drawable.ic_color_edit)
                binding.temperatureIvLock.visibility = View.VISIBLE
            }
            if (ScreenTool.isIPad(this@IRGalleryEditActivity)) {
                binding.colorBarView.setPadding(0, SizeUtils.dp2px(40f), 0, SizeUtils.dp2px(40f))
            }
        }
    }

    /**
     * 更新图像
     */
    private fun updateImage(bitmap: Bitmap?) {
        bitmap?.let {
            val params = binding.irImageView.layoutParams as ConstraintLayout.LayoutParams
            params.dimensionRatio = "${bitmap.width}:${bitmap.height}"
            runOnUiThread {
                binding.irImageView.layoutParams = params
            }
            if (struct.watermarkBean.isOpen) {
                val width = ScreenUtil.getScreenWidth(this)
                val height = (width * bitmap.height / bitmap.width.toFloat()).toInt()
                binding.irImageView.setImageBitmap(
                    BitmapUtils.drawCenterLable(
                        Bitmap.createScaledBitmap(it, width, height, true),
                        struct.watermarkBean.title,
                        struct.watermarkBean.address,
                        if (struct.watermarkBean.isAddTime) TimeTool.getNowTime() else "",
                        0  // No longer using temperature seekbar width
                    )
                )
            } else {
                binding.irImageView.setImageBitmap(it)
            }
        }
    }

    /**
     * 一级菜单
     */
    private fun initRecycler() {
        binding.editRecyclerFirst.onTabClickListener = {
            when (it) {
                0 -> binding.editRecyclerSecond.selectPosition(1) //点线面
                1 -> binding.editRecyclerSecond.selectPosition(3) //伪彩颜色
                2 -> binding.editRecyclerSecond.selectPosition(4) //设置
            }
        }
        binding.editRecyclerFirst.onBarClickListener = {
            binding.colorBarView.isVisible = it
        }

        binding.editRecyclerSecond.onFenceListener = { fenceType, isSelected ->
            when (fenceType) {
                FenceType.POINT -> binding.temperatureView.mode = Mode.POINT
                FenceType.LINE -> binding.temperatureView.mode = Mode.LINE
                FenceType.RECT -> binding.temperatureView.mode = Mode.RECT
                FenceType.DEL -> binding.temperatureView.mode = Mode.CLEAR
                FenceType.FULL -> binding.temperatureView.isShowFull = isSelected
                FenceType.TREND -> {
                    //2D编辑没有趋势图
                }
            }
        }
        binding.editRecyclerSecond.onColorListener = { _, it, _ ->
            if (struct.customPseudoBean.isUseCustomPseudo) {
                TipDialog.Builder(this)
                    .setTitleMessage(getString(R.string.app_tip))
                    .setMessage(R.string.tip_change_pseudo_mode)
                    .setPositiveListener(R.string.app_yes) {
                        struct.customPseudoBean.isUseCustomPseudo = false
                        setDefLimit()
                        setPColor(it)
                        updateImageAndSeekbarColorList(struct.customPseudoBean)
                    }.setCancelListener(R.string.app_no) {

                    }
                    .create().show()
            } else {
                setPColor(it)
            }
        }
        binding.editRecyclerSecond.onSettingListener = { type, _ ->
            setSettingValue(type)
        }
    }

    /**
     * 最高最低温复原
     */
    private fun setDefLimit() {
        val tempResult = frameTool.getSrcTemp()
        rightValue = showUnitValue(tempCorrect(tempResult.maxTemperature),isShowC)
        leftValue = showUnitValue(tempCorrect(tempResult.minTemperature),isShowC)
    }

    //设置伪彩
    private fun setPColor(code: Int) {
        pseudocodeMode = code
        updateImage(
            frameTool.getScrPseudoColorScaledBitmap(
                changePseudocodeModeByOld(pseudocodeMode),
                showToCValue(max),
                showToCValue(min),
                rotate,
                struct.customPseudoBean,
                maxTemperature = tempCorrect(frameTool.getSrcTemp().maxTemperature),
                minTemperature = tempCorrect(frameTool.getSrcTemp().minTemperature),
                struct.isAmplify
            )
        )
        binding.editRecyclerSecond.setPseudoColor(code)
    }

    private var tempAlarmSetDialog: TempAlarmSetDialog? = null
    private fun setSettingValue(type: SettingType) {
        when (type) {
            SettingType.ALARM -> {
                // 预警
                if (tempAlarmSetDialog == null) {
                    tempAlarmSetDialog = TempAlarmSetDialog(this, true)
                    tempAlarmSetDialog?.onSaveListener = {
                        binding.editRecyclerSecond.setSettingSelected(SettingType.ALARM, it.isHighOpen || it.isLowOpen)
                        struct.alarmBean = it
                        frameTool.initStruct(struct)
                        updateImage(
                            frameTool.getScrPseudoColorScaledBitmap(
                                changePseudocodeModeByOld(pseudocodeMode),
                                showToCValue(max),
                                showToCValue(min),
                                rotate,
                                struct.customPseudoBean,
                                maxTemperature = tempCorrect(frameTool.getSrcTemp().maxTemperature),
                                minTemperature = tempCorrect(frameTool.getSrcTemp().minTemperature),
                                struct.isAmplify
                            )
                        )
                    }
                }
                tempAlarmSetDialog?.alarmBean = struct.alarmBean
                tempAlarmSetDialog?.show()
            }
            SettingType.FONT -> {//字体颜色
                val colorPickDialog = ColorPickDialog(this, binding.temperatureView.textColor,binding.temperatureView.tempTextSize)
                colorPickDialog.onPickListener = { it: Int, textSize: Int ->
                    temperature_view?.textColor = it
                    struct.textSize = SizeUtils.sp2px(textSize.toFloat())
                    temperature_view?.tempTextSize = SizeUtils.sp2px(textSize.toFloat())
                    binding.editRecyclerSecond.setSettingSelected(SettingType.FONT,
                        it != 0xffffffff.toInt() || textSize != SizeUtils.sp2px(14f))
                }
                colorPickDialog.show()
            }
            SettingType.WATERMARK -> { //水印
                TipWaterMarkDialog.Builder(this, struct.watermarkBean)
                    .setCancelListener {
                        struct.watermarkBean = it
                        frameTool.initStruct(struct)
                        binding.editRecyclerSecond.setSettingSelected(SettingType.WATERMARK, it.isOpen)
                        updateImage(
                            frameTool.getScrPseudoColorScaledBitmap(
                                changePseudocodeModeByOld(pseudocodeMode),
                                showToCValue(max),
                                showToCValue(min),
                                rotate,
                                struct.customPseudoBean,
                                maxTemperature = tempCorrect(frameTool.getSrcTemp().maxTemperature),
                                minTemperature = tempCorrect(frameTool.getSrcTemp().minTemperature),
                                struct.isAmplify
                            )
                        )
                    }
                    .create().show()
            }
            else -> {
                //其他设置选项 2D 编辑没有
            }
        }
    }

    private fun updateTemperatureSeekBar(isEnabled: Boolean, resource: Int, content: String) {
        binding.temperatureIvLock.setImageResource(resource)
        binding.temperatureIvLock.contentDescription = content
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.temperatureIvLock -> {
                if (binding.temperatureIvLock.contentDescription == "lock") {
                    updateTemperatureSeekBar(
                        true,
                        R.drawable.svg_pseudo_bar_unlock,
                        "unlock"
                    )//解锁
                } else {
                    setDefLimit()
                    updateTemperatureSeekBar(false, R.drawable.svg_pseudo_bar_lock, "lock")//加锁
                }
            }
            binding.temperatureIvInput -> {
                val intent = Intent(this, PseudoSetActivity::class.java)
                intent.putExtra(ExtraKeyConfig.IS_TC007, isTC007)
                intent.putExtra(ExtraKeyConfig.CUSTOM_PSEUDO_BEAN, struct.customPseudoBean)
                pseudoSetResult.launch(intent)
            }
        }
    }

    private val pseudoSetResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val tmp = it.data?.getParcelableExtra(ExtraKeyConfig.CUSTOM_PSEUDO_BEAN)
                    ?: CustomPseudoBean()
                updateImageAndSeekbarColorList(tmp)
                binding.temperatureSeekbar.setColorList(tmp.getColorList(struct.isTC007())?.reversedArray())
                binding.temperatureSeekbar.setPlaces(tmp.getPlaceList())
//            customPseudoBean.saveToShared()

            }
        }

    //更新自定义伪彩的颜色的属性值
    private fun updateImageAndSeekbarColorList(customPseudoBean: CustomPseudoBean?) {
        customPseudoBean?.let {
            updateImage(
                frameTool.getScrPseudoColorScaledBitmap(
                    changePseudocodeModeByOld(
                        pseudocodeMode
                    ), rotate = rotate, customPseudoBean = customPseudoBean,
                    maxTemperature = tempCorrect(frameTool.getSrcTemp().maxTemperature),
                    minTemperature = tempCorrect(frameTool.getSrcTemp().minTemperature),
                    isAmplify = struct.isAmplify
                )
            )
            if (it.isUseCustomPseudo) {
                binding.temperatureIvLock.visibility = View.INVISIBLE
                binding.tvTempContent.visibility = View.VISIBLE
                updateTemperatureSeekBar(false, R.drawable.svg_pseudo_bar_lock, "lock")//加锁
                binding.editRecyclerSecond.setPseudoColor(-1)
                binding.temperatureIvInput.setImageResource(R.drawable.ir_model)
            } else {
                binding.temperatureIvLock.visibility = View.VISIBLE
                if (struct.customPseudoBean.isUseCustomPseudo) {
                    setDefLimit()
                }
                binding.tvTempContent.visibility = View.GONE
                binding.editRecyclerSecond.setPseudoColor(pseudocodeMode)
                binding.temperatureIvInput.setImageResource(R.drawable.ic_color_edit)
            }
            struct.customPseudoBean = customPseudoBean
        }
//        binding.tvTempContent.visibility = View.VISIBLE
    }

    /**
     * 从上一界面传递过来的，是否从生成报告拾取图片中跳转过来.
     */
    private var isReportPick = false
    private fun initUI() {
        isReportPick = intent.getBooleanExtra(ExtraKeyConfig.IS_PICK_REPORT_IMG, false)
        binding.titleView.setLeftClickListener {
            if (isReportPick) {
                finish()
            } else {
                saveImage()
            }
        }
        binding.titleView.setRightText(if (isReportPick) R.string.app_next else R.string.person_save)
        binding.titleView.setRightClickListener {
            if (!isReportPick) {
                updateIconSave()
            } else {
                showLoadingDialog()
                lifecycleScope.launch(Dispatchers.IO) {
                    // 获取展示图像信息的图层数据
                    var irBitmap = if (struct.isAmplify){
                        //超分四倍使用原始图像继续超分一次
                        OpencvTools.supImageFourExToBitmap(frameTool.getBaseBitmap(rotate))
                    }else{
                        binding.irImageView.drawToBitmap()
                    }
                    if (binding.temperatureView.mode != Mode.CLEAR) {
                        // 获取温度图层的数据，包括点线框，温度值等，重新合成bitmap
                        irBitmap = BitmapUtils.mergeBitmap(irBitmap, binding.temperatureView.drawToBitmap(), 0, 0)
                    }
                    // 合并伪彩条
                    if (binding.colorBarView.visibility == View.VISIBLE) {
                        irBitmap = BitmapUtils.mergeBitmap(irBitmap, binding.colorBarView.drawToBitmap(), 0, 0)
                    }
                    // 保存图片
                    val fileAbsolutePath = ImageUtils.saveToCache(this@IRGalleryEditActivity, irBitmap)
                    launch(Dispatchers.Main) {
                        dismissLoadingDialog()
                        if (intent.getBooleanExtra(IS_REPORT_FIRST, true)) {
                            ARouter.getInstance().build(RouterConfig.REPORT_CREATE_FIRST)
                                .withBoolean(ExtraKeyConfig.IS_TC007, isTC007)
                                .withString(ExtraKeyConfig.FILE_ABSOLUTE_PATH, fileAbsolutePath)
                                .withParcelable(ExtraKeyConfig.IMAGE_TEMP_BEAN, buildImageTempBean())
                                .navigation(this@IRGalleryEditActivity)
                        } else {
                            ARouter.getInstance().build(RouterConfig.REPORT_CREATE_SECOND)
                                .withBoolean(ExtraKeyConfig.IS_TC007, isTC007)
                                .withString(ExtraKeyConfig.FILE_ABSOLUTE_PATH, fileAbsolutePath)
                                .withParcelable(ExtraKeyConfig.IMAGE_TEMP_BEAN, buildImageTempBean())
                                .withParcelable(ExtraKeyConfig.REPORT_INFO, intent.getParcelableExtra(ExtraKeyConfig.REPORT_INFO))
                                .withParcelable(ExtraKeyConfig.REPORT_CONDITION, intent.getParcelableExtra(ExtraKeyConfig.REPORT_CONDITION))
                                .withParcelableArrayList(ExtraKeyConfig.REPORT_IR_LIST, intent.getParcelableArrayListExtra(ExtraKeyConfig.REPORT_IR_LIST))
                                .navigation(this@IRGalleryEditActivity)
                        }
                    }
                }
            }
        }
        binding.editRecyclerSecond.selectPosition(-1)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReportCreate(event: ReportCreateEvent) {
        if (isReportPick) {
            finish()
        }
    }

    private fun keepOneDigit(float: Float) = String.format(Locale.ENGLISH, "%.1f", float)

    private fun buildImageTempBean(): ImageTempBean {
        var full: ImageTempBean.TempBean? = null
        if (binding.temperatureView.isShowFull) {
            binding.temperatureView.fullInfo?.let {
                val max = keepOneDigit(tempCorrect(it.maxTemperature))
                val min = keepOneDigit(tempCorrect(it.minTemperature))
                full = ImageTempBean.TempBean(max, min)
            }
        }

        val pointList = arrayListOf<ImageTempBean.TempBean>()
        for (temp in binding.temperatureView.tempListData.pointTemps) {
            if (temp.type != -99) {
                pointList.add(ImageTempBean.TempBean(keepOneDigit(tempCorrect(temp.maxTemperature))))
            }
        }

        val lineList = arrayListOf<ImageTempBean.TempBean>()
        for (temp in binding.temperatureView.tempListData.lineTemps) {
            if (temp.type != -99) {
                val max = keepOneDigit(tempCorrect(temp.maxTemperature))
                val min = keepOneDigit(tempCorrect(temp.minTemperature))
                val average = keepOneDigit(temp.averageTemperature)
                lineList.add(ImageTempBean.TempBean(max, min, average))
            }
        }

        val rectList = arrayListOf<ImageTempBean.TempBean>()
        for (temp in binding.temperatureView.tempListData.rectangleTemps) {
            if (temp.type != -99) {
                val max = keepOneDigit(tempCorrect(temp.maxTemperature))
                val min = keepOneDigit(tempCorrect(temp.minTemperature))
                val average = keepOneDigit(temp.averageTemperature)
                rectList.add(ImageTempBean.TempBean(max, min, average))
            }
        }

        return ImageTempBean(full, pointList, lineList, rectList)
    }


    private fun saveImage() {
        TipDialog.Builder(this)
            .setTitleMessage(getString(R.string.app_tip))
            .setMessage(R.string.app_save_image)
            .setPositiveListener(R.string.app_yes) {
                updateIconSave()
            }.setCancelListener(R.string.app_no) {
                finish()
            }
            .create().show()

    }

    private fun updateIconSave() {
        lifecycleScope.launch(Dispatchers.IO) {
            // 获取展示图像信息的图层数据
            var irBitmap = if (struct.isAmplify){
                //超分四倍使用原始图像继续超分一次
                OpencvTools.supImageFourExToBitmap(frameTool.getBaseBitmap(rotate))
            }else{
                binding.irImageView.drawToBitmap()
            }
            if (binding.temperatureView.mode != Mode.CLEAR) {
                // 获取温度图层的数据，包括点线框，温度值等，重新合成bitmap
                irBitmap = BitmapUtils.mergeBitmap(irBitmap, binding.temperatureView.drawToBitmap(), 0, 0)
            }
            // 合并伪彩条
            if (binding.colorBarView.visibility == View.VISIBLE) {
                irBitmap = BitmapUtils.mergeBitmap(irBitmap, binding.colorBarView.drawToBitmap(), 0, 0)
            }
            // 保存图片
            var name: String
            irBitmap.let {
                name = ImageUtils.save(bitmap = it,isTC007)
            }
            ImageUtils.saveFrame(bs = mFrame, capital = getCapital(), name = name)
            ToastTools.showShort(R.string.tip_photo_saved)
            EventBus.getDefault().post(GalleryAddEvent())
            MediaScannerConnection.scanFile(
                this@IRGalleryEditActivity,
                arrayOf(FileConfig.lineGalleryDir),
                null,
                null
            )
            EventBus.getDefault().post(ImageGalleryEvent())
            finish()
        }
    }

    private fun getCapital(): ByteArray {
        val capital: ByteArray? //首部
        capital = FrameStruct.toCode(
            name = struct.name,
            width = struct.width,
            height = struct.height,
            rotate = struct.rotate,
            pseudo = pseudocodeMode,
            initRotate = struct.initRotate,
            correctRotate = struct.correctRotate,
            customPseudoBean = struct.customPseudoBean,
            isShowPseudoBar = binding.colorBarView.isVisible,
            textColor = binding.temperatureView.textColor,
            watermarkBean = struct.watermarkBean,
            alarmBean = struct.alarmBean,
            gainStatus = struct.gainStatus,
            textSize = struct.textSize,
            struct.environment,
            struct.distance,
            struct.radiation,
            false
        )
        return capital
    }

    override fun onBackPressed() {
        lifecycleScope.launch {
            if (isReportPick) {
                finish()
            } else {
                saveImage()
            }
        }
    }

    override fun tempCorrectByTs(temp: Float?): Float {
        var tmp = temp
        try {
            tmp = tempCorrect(temp!!)
        } catch (e: Exception) {
            XLog.i("温度校正失败: ${e.message}")
        }
        return tmp!!
    }

    /**
     * 单点修正过程
     */
    private fun tempCorrect(
        temp: Float): Float {
        var newTemp = temp
        try {
            if (struct == null || struct.distance <= 0 || struct.radiation <= 0){
                return temp
            }
            val paramsArray = floatArrayOf(
                temp, struct.radiation, struct.environment,
                struct.environment, struct.distance, 0.8f
            )
            if (struct.name.startsWith(PRODUCT_NAME_TS)) {
                if (ts_data_H == null || ts_data_L == null) return temp
                newTemp = IRUtils.temperatureCorrection(
                    IRCMDType.USB_IR_256_384,
                    CommonParams.ProductType.WN256_ADVANCED,
                    paramsArray[0],
                    ts_data_H,
                    ts_data_L,
                    paramsArray[1],
                    paramsArray[2],
                    paramsArray[3],
                    paramsArray[4],
                    paramsArray[5],
                    0,
                    if (struct.gainStatus == 1){
                        CommonParams.GainStatus.HIGH_GAIN
                    }else{
                        CommonParams.GainStatus.LOW_GAIN
                    }
                )
            }else if (struct.name.startsWith(PRODUCT_NAME_TC001LITE)){
                //lite的模组
                if (BaseApplication.instance.tau_data_H == null || BaseApplication.instance.tau_data_L == null) return temp
                newTemp = IRTool.temperatureCorrection(temp,paramsArray,BaseApplication.instance.tau_data_H!!,
                    BaseApplication.instance.tau_data_L!!,struct.gainStatus)
            }
        }catch (e : Exception){
            XLog.e("$TAG:tempCorrect-${e.message}")
        }finally {
            return newTemp
        }
    }
}