package com.topdon.house.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.UriUtils
import com.bumptech.glide.Glide
import com.github.gzuliyujiang.wheelpicker.DatimePicker
import com.github.gzuliyujiang.wheelpicker.StrArrayPicker
import com.github.gzuliyujiang.wheelpicker.YearPicker
import com.github.gzuliyujiang.wheelpicker.entity.DateEntity
import com.github.gzuliyujiang.wheelpicker.entity.DatimeEntity
import com.github.gzuliyujiang.wheelpicker.entity.TimeEntity
import com.topdon.house.R
import com.topdon.house.dialog.ImagePickFromDialog
import com.topdon.house.event.HouseDetectAddEvent
import com.topdon.house.event.HouseDetectEditEvent
import com.topdon.house.viewmodel.DetectViewModel
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.db.AppDatabase
import com.topdon.lib.core.db.entity.HouseDetect
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.PermissionTool
import com.topdon.lib.core.tools.SpanBuilder
import com.topdon.lib.core.utils.LocationUtil
import com.topdon.lms.sdk.weiget.TToast
import kotlinx.android.synthetic.main.activity_detect_add.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 创建或编辑检测.
 *
 * 可选传递参数：
 * - [ExtraKeyConfig.DETECT_ID] - 仅当编辑检测时，要编辑的房屋检测 Id
 * - [ExtraKeyConfig.IS_TC007] - 仅当新增检测时，当前设备是否为 TC007（不使用，透传）
 *
 * Created by LCG on 2024/8/21.
 */
class DetectAddActivity : BaseActivity(), View.OnClickListener {
    private val viewModel: DetectViewModel  by viewModels()

    /**
     * 仅当编辑模式时，从上一界面传递过来的，要编辑的房屋检测 Id.
     */
    private var editId: Long = 0
    /**
     * 当前编辑或新增的房屋检测信息.
     */
    private var houseDetect = HouseDetect()

    /**
     * 当前输入的检测时间.
     */
    private var inputDetectTime: Long? = null


    override fun initContentView(): Int = R.layout.activity_detect_add

    override fun initView() {
        editId = intent.getLongExtra(ExtraKeyConfig.DETECT_ID, 0)

        title_view.setTitleText(if (editId > 0) R.string.edit_detection_report else R.string.add_detection_report)
        title_view.setLeftClickListener { showExitTipsDialog() }

        tv_create_report.setText(if (editId > 0) R.string.person_save else R.string.create_report)

        viewModel.detectLD.observe(this) {
            houseDetect = it ?: return@observe
            inputDetectTime = houseDetect.detectTime
            et_detect_name.setText(houseDetect.name)
            et_inspector_name.setText(houseDetect.inspectorName)
            tv_detect_time.text = TimeUtils.millis2String(houseDetect.detectTime, "yyyy-MM-dd HH:mm")
            et_house_address.setText(houseDetect.address)

            Glide.with(iv_house_image).load(houseDetect.imagePath).into(iv_house_image)
            iv_house_image_camera.isVisible = false
            tv_house_image_camera.isVisible = false

            tv_house_year.text = houseDetect.year?.toString() ?: ""

            et_house_space.setText(houseDetect.houseSpace)
            tv_house_space_unit.text = resources.getStringArray(R.array.area)[houseDetect.houseSpaceUnit]

            et_cost.setText(houseDetect.cost)
            tv_cost_unit.text = resources.getStringArray(R.array.currency)[houseDetect.costUnit]
        }

        if (editId > 0) {
            viewModel.queryById(editId)
        } else {
            houseDetect.houseSpaceUnit = SharedManager.houseSpaceUnit
            houseDetect.costUnit = SharedManager.costUnit
            tv_house_space_unit.text = resources.getStringArray(R.array.area)[houseDetect.houseSpaceUnit]
            tv_cost_unit.text = resources.getStringArray(R.array.currency)[houseDetect.costUnit]
        }


        tv_detect_time.setOnClickListener(this)
        iv_address_location.setOnClickListener(this)
        iv_house_image.setOnClickListener(this)
        tv_house_year.setOnClickListener(this)
        tv_house_space_unit.setOnClickListener(this)
        tv_cost_unit.setOnClickListener(this)
        tv_create_report.setOnClickListener(this)

        // 给各个标题添加红色*号
        tv_detect_name_title.text = SpanBuilder().appendColor("*", 0xffff4848.toInt()).append(getString(R.string.album_report_name))
        tv_inspector_name_title.text = SpanBuilder().appendColor("*", 0xffff4848.toInt()).append(getString(R.string.inspector_name))
        tv_detect_time_title.text = SpanBuilder().appendColor("*", 0xffff4848.toInt()).append(getString(R.string.detect_time))
        tv_house_address_title.text = SpanBuilder().appendColor("*", 0xffff4848.toInt()).append(getString(R.string.house_detail_address))
        tv_house_image_title.text = SpanBuilder().appendColor("*", 0xffff4848.toInt()).append(getString(R.string.house_image))

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitTipsDialog()
            }
        })
    }

    override fun initData() {
    }

    override fun onClick(v: View?) {
        when (v) {
            tv_detect_time -> {//检测时间
                showDetectTimeDialog()
            }
            iv_address_location -> {//房屋地址定位图标
                getLocation()
            }
            iv_house_image -> {//房屋图片
                ImagePickFromDialog(this)
                    .setSelectListener {
                        if (it == 0) {//相册
                            PermissionTool.requestImageRead(this) {
                                galleryPickResult.launch("image/*")
                            }
                        } else {
                            PermissionTool.requestCamera(this) {
                                val fileName = "Cover${System.currentTimeMillis()}.png"
                                val file = FileConfig.getDetectImageDir(this, fileName)
                                lightPhotoResult.launch(file)
                            }
                        }
                    }
                    .show()
            }
            tv_house_year -> {//建筑年份
                YearPicker(this, houseDetect.year).also {
                    it.setTitle(R.string.year_built)
                    it.setOnYearPickedListener { year ->
                        houseDetect.year = year
                        tv_house_year.text = houseDetect.year?.toString()
                    }
                }.show()
            }
            tv_house_space_unit -> {//建筑面积单位
                StrArrayPicker(this, resources.getStringArray(R.array.area), SharedManager.houseSpaceUnit).also {
                    it.setTitle(R.string.area)
                    it.setOnOptionPickedListener { position, item ->
                        SharedManager.houseSpaceUnit = position
                        houseDetect.houseSpaceUnit = position
                        tv_house_space_unit.text = item.toString()
                    }
                }.show()
            }
            tv_cost_unit -> {//检测费用单位
                StrArrayPicker(this, resources.getStringArray(R.array.currency), SharedManager.costUnit).also {
                    it.setTitle(R.string.diagnosis_unit)
                    it.setOnOptionPickedListener { position, item ->
                        SharedManager.costUnit = position
                        houseDetect.costUnit = position
                        tv_cost_unit.text = item.toString()
                    }
                }.show()
            }
            tv_create_report -> {//创建报告 or 编辑报告
                val reportName = et_detect_name.text.toString()
                if (reportName.isEmpty()) {
                    TToast.shortToast(this, R.string.album_report_input_name_tips)
                    return
                }

                val inspectorName = et_inspector_name.text.toString()
                if (inspectorName.isEmpty()) {
                    TToast.shortToast(this, R.string.inspector_name_input_hint)
                    return
                }

                val detectTime = this.inputDetectTime
                if (detectTime == null) {
                    TToast.shortToast(this, R.string.please_select_detect_time)
                    return
                }

                val address = et_house_address.text.toString()
                if (address.isEmpty()) {
                    TToast.shortToast(this, R.string.house_detail_address_input_hint)
                    return
                }

                if (houseDetect.imagePath.isEmpty()) {
                    TToast.shortToast(this, R.string.house_image_input_hint)
                    return
                }

                lifecycleScope.launch {
                    showLoadingDialog()
                    withContext(Dispatchers.IO) {
                        val currentTime = System.currentTimeMillis()
                        houseDetect.name = reportName
                        houseDetect.inspectorName = inspectorName
                        houseDetect.address = address
                        houseDetect.houseSpace = et_house_space.text.toString()
                        houseDetect.cost = et_cost.text.toString()
                        houseDetect.createTime = if (editId > 0) houseDetect.createTime else currentTime
                        houseDetect.updateTime = currentTime

                        if (editId > 0) {//编辑模式
                            AppDatabase.getInstance().houseDetectDao().updateDetect(houseDetect)
                            EventBus.getDefault().post(HouseDetectEditEvent(houseDetect.id))
                        } else {
                            houseDetect.id = AppDatabase.getInstance().houseDetectDao().insert(houseDetect)
                            EventBus.getDefault().post(HouseDetectAddEvent())
                        }
                    }
                    if (editId == 0L) {
                        val newIntent = Intent(this@DetectAddActivity, ReportAddActivity::class.java)
                        newIntent.putExtra(ExtraKeyConfig.DETECT_ID, houseDetect.id)
                        newIntent.putExtra(ExtraKeyConfig.IS_TC007, intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false))
                        startActivity(newIntent)
                    }
                    dismissLoadingDialog()
                    finish()
                }
            }
        }
    }

    /**
     * 尝试获取位置信息并反向地址信息编码为 省市区.
     */
    @SuppressLint("MissingPermission")
    private fun getLocation() {
        PermissionTool.requestLocation(this) {
            lifecycleScope.launch {
                showLoadingDialog(R.string.get_current_address)
                val addressText = LocationUtil.getLastLocationStr(this@DetectAddActivity)
                dismissLoadingDialog()
                if (addressText == null) {
                    TToast.shortToast(this@DetectAddActivity, R.string.get_Location_failed)
                } else {
                    houseDetect.address = addressText
                    et_house_address.setText(addressText)
                }
            }
        }
    }

    /**
     * 从系统相册拾取图片结果
     */
    private val galleryPickResult = registerForActivityResult(ActivityResultContracts.GetContent()) {
        val srcFile: File? = UriUtils.uri2File(it)
        if (srcFile != null) {
            val copyFile = FileConfig.getDetectImageDir(this, "Cover${System.currentTimeMillis()}.png")
            FileUtils.copy(srcFile, copyFile)
            houseDetect.imagePath = copyFile.absolutePath
            Glide.with(iv_house_image).load(copyFile.absolutePath).into(iv_house_image)
            iv_house_image_camera.isVisible = false
            tv_house_image_camera.isVisible = false
        }
    }

    /**
     * 从系统相机拍照结果
     */
    private val lightPhotoResult = registerForActivityResult(TakePhotoResult()) {
        if (it != null) {
            houseDetect.imagePath = it.absolutePath
            Glide.with(iv_house_image).load(it.absolutePath).into(iv_house_image)
            iv_house_image_camera.isVisible = false
            tv_house_image_camera.isVisible = false
        }
    }

    /**
     * 显示退出不保存提示弹框
     */
    private fun showExitTipsDialog() {
        TipDialog.Builder(this)
            .setMessage(R.string.diy_tip_save)
            .setPositiveListener(R.string.app_exit) {
                finish()
            }
            .setCancelListener(R.string.app_cancel)
            .create().show()
    }

    /**
     * 显示检测时间拾取弹窗
     */
    private fun showDetectTimeDialog() {
        val picker = DatimePicker(this)
        picker.setTitle(R.string.detect_time)
        picker.setOnDatimePickedListener { year, month, day, hour, minute, second ->
            val timeStr = "$year-$month-$day $hour:$minute:$second"
            val pattern = "yyyy-MM-dd HH:mm:ss"
            val time: Long = SimpleDateFormat(pattern, Locale.getDefault()).parse(timeStr, ParsePosition(0))?.time ?: 0
            tv_detect_time.text = TimeUtils.millis2String(time, "yyyy-MM-dd HH:mm")
            inputDetectTime = time
            houseDetect.detectTime = time
        }

        val calendar = Calendar.getInstance()
        val nowYear = calendar.get(Calendar.YEAR)

        val startTimeEntity = DatimeEntity()
        startTimeEntity.date = DateEntity.target(nowYear - 1000, 1, 1)

        val endTimeEntity = DatimeEntity.yearOnFuture(1000)

        val timeEntity = if (inputDetectTime == null) DatimeEntity.now() else DatimeEntity()
        if (inputDetectTime != null) {
            calendar.timeInMillis = inputDetectTime ?: 0
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val hours = calendar.get(Calendar.HOUR_OF_DAY)
            val minutes = calendar.get(Calendar.MINUTE)
            val seconds = calendar.get(Calendar.SECOND)
            timeEntity.date = DateEntity.target(year, month, day)
            timeEntity.time = TimeEntity.target(hours, minutes, seconds)
        }

        picker.wheelLayout.setRange(startTimeEntity, endTimeEntity, timeEntity)
        picker.show()
    }
}