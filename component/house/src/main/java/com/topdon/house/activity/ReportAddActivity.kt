package com.topdon.house.activity

import android.content.Intent
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.alibaba.android.arouter.launcher.ARouter
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.UriUtils
import com.topdon.house.R
import com.topdon.house.event.DetectDirListEvent
import com.topdon.house.event.DetectItemListEvent
import com.topdon.house.event.HouseReportAddEvent
import com.topdon.house.popup.ThreePickPopup
import com.topdon.house.viewmodel.DetectViewModel
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.db.entity.HouseDetect
import com.topdon.lib.core.db.entity.ItemDetect
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.tools.DeviceTools
import com.topdon.lib.core.tools.PermissionTool
import com.topdon.libcom.util.ARouterUtil
import com.topdon.lms.sdk.weiget.TToast
import kotlinx.android.synthetic.main.activity_report_add.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

/**
 * 报告生成（即检测）.
 *
 * 需要传递：
 * - [ExtraKeyConfig.DETECT_ID] - 执行检测的房屋检测 Id
 * - [ExtraKeyConfig.IS_TC007] - 当前设备是否为 TC007
 *
 * Created by LCG on 2024/8/23.
 */
class ReportAddActivity : BaseActivity(), View.OnClickListener {
    /**
     * 从上一界面传递过来的，当前是否为 TC007 设备类型.
     * true-TC007 false-其他插件式设备
     */
    private var isTC007 = false

    /**
     * 所有目录是否都已展开.
     * true-都已展开 false-至少有1个未展开
     */
    private var isAllExpand = false

    private val viewModel: DetectViewModel by viewModels()

    override fun initContentView(): Int = R.layout.activity_report_add

    override fun initView() {
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        iv_expand.isEnabled = false
        iv_back.setOnClickListener(this)
        iv_edit.setOnClickListener(this)
        iv_expand.setOnClickListener(this)
        tv_add.setOnClickListener(this)
        tv_export_report.setOnClickListener(this)

        initDetectViewListener()

        viewModel.detectLD.observe(this) {
            //查询当前检测结果
            if (it != null) {
                isAllExpand = false

                cl_empty.isVisible = it.dirList.isEmpty()
                view_house_detect.isVisible = it.dirList.isNotEmpty()
                tv_export_report.isVisible = it.dirList.isNotEmpty()

                view_house_detect.refresh(it.dirList)

                iv_edit.isEnabled = it.dirList.isNotEmpty()
                iv_expand.isEnabled = it.dirList.isNotEmpty()
                iv_expand.isSelected = isAllExpand
            }
        }
        viewModel.copyDirLD.observe(this) {
            //复制目录结果
            view_house_detect.notifyDirInsert(it.first, it.second)
            TToast.shortToast(this@ReportAddActivity, R.string.ts004_copy_success)
        }
        viewModel.copyItemLD.observe(this) {
            //复制项目结果
            view_house_detect.notifyItemInsert(it.first, it.second)
            TToast.shortToast(this@ReportAddActivity, R.string.ts004_copy_success)
        }
        viewModel.delItemLD.observe(this) {
            //删除项目结果
            view_house_detect.notifyItemRemove(it.first, it.second)
            TToast.shortToast(this@ReportAddActivity, R.string.test_results_delete_success)
        }


        viewModel.queryById(intent.getLongExtra(ExtraKeyConfig.DETECT_ID, 0))
    }

    override fun initData() {
    }

    override fun onClick(v: View?) {
        when (v) {
            iv_back -> finish()
            iv_edit -> {//目录编辑
                val newIntent = Intent(this, DirEditActivity::class.java)
                newIntent.putExtra(ExtraKeyConfig.DETECT_ID, intent.getLongExtra(ExtraKeyConfig.DETECT_ID, 0))
                startActivity(newIntent)
            }
            iv_expand -> {//展开收起
                isAllExpand = !isAllExpand
                if (isAllExpand) {
                    view_house_detect.expandAllDir()
                } else {
                    view_house_detect.retractAllDir()
                }
                iv_expand.isSelected = isAllExpand
            }
            tv_export_report -> {//导出报告
                ARouter.getInstance().build(RouterConfig.REPORT_PREVIEW)
                    .withBoolean(ExtraKeyConfig.IS_REPORT, false)
                    .withLong(ExtraKeyConfig.LONG_ID, intent.getLongExtra(ExtraKeyConfig.DETECT_ID, 0))
                    .navigation(this)
            }
            tv_add -> {//新增默认目录
                val detect: HouseDetect? = viewModel.detectLD.value
                if (detect != null) {
                    viewModel.insertDefaultDirs(detect)
                }
            }
        }
    }

    /**
     * 当前正在编辑的项目，在 view_house_detect 中的 index.
     */
    private var editLayoutIndex = 0
    /**
     * 当前正在编辑的项目
     */
    private var editItemDetect = ItemDetect()

    /**
     * 初始化 view_house_detect 的相关事件监听.
     */
    private fun initDetectViewListener() {
        view_house_detect.onDirCopyListener = {//目录复制
            viewModel.copyDir(it.first, it.second)
        }
        view_house_detect.onItemCopyListener = {//项目复制
            viewModel.copyItem(it.first, it.second)
        }
        view_house_detect.onItemDelListener = {
            viewModel.delItem(it.first, it.second)
        }

        view_house_detect.onImageAddListener = { layoutIndex, v, item ->
            //项目添加图片
            editLayoutIndex = layoutIndex
            editItemDetect = item
            ThreePickPopup(this, arrayListOf(R.string.person_headshot_phone, R.string.light_camera_take_photo, R.string.ir_camera_take_photo)) {
                when (it) {
                    0 -> {//从相册获取
                        PermissionTool.requestImageRead(this) {
                            galleryPickResult.launch("image/*")
                        }
                    }
                    1 -> {//相机拍照
                        PermissionTool.requestCamera(this) {
                            val fileName = "Item${System.currentTimeMillis()}.png"
                            val file = FileConfig.getDetectImageDir(this, fileName)
                            lightPhotoResult.launch(file)
                        }
                    }
                    2 -> {//红外线拍照
                        if ((isTC007 && !WebSocketProxy.getInstance().isTC007Connect()) || (!isTC007 && !DeviceTools.isConnect())) {
                            TToast.shortToast(this@ReportAddActivity, R.string.device_disconnect)
                        } else {
                            val fileName = "Item${System.currentTimeMillis()}.png"
                            val file = FileConfig.getDetectImageDir(this, fileName)
                            ARouterUtil.jumpImagePick(this@ReportAddActivity, isTC007, file.absolutePath)
                        }
                    }
                }
            }.show(v, true)
        }
        view_house_detect.onTextInputListener = {
            //项目文字输入
            editLayoutIndex = it.first
            editItemDetect = it.second
            val intent = Intent(this, TextInputActivity::class.java)
            intent.putExtra(ExtraKeyConfig.ITEM_NAME, it.second.itemName)
            intent.putExtra(ExtraKeyConfig.RESULT_INPUT_TEXT, it.second.inputText)
            textInputResult.launch(intent)
        }

        view_house_detect.onDirChangeListener = {
            //目录数据变更（3种状态数量）
            viewModel.updateDir(it)
        }
        view_house_detect.onDirExpandListener = {
            //一个目录展开收起状态变化
            if (it) {
                if (!isAllExpand) {
                    val detect: HouseDetect? = viewModel.detectLD.value
                    if (detect != null) {
                        isAllExpand = true
                        for (dir in detect.dirList) {
                            if (!dir.isExpand) {
                                isAllExpand = false
                                break
                            }
                        }
                    }
                }
            } else {
                isAllExpand = false
            }
            iv_expand.isSelected = isAllExpand
        }
        view_house_detect.onItemChangeListener = {
            //项目数据变更（3种状态、图片删除）
            viewModel.updateItem(it)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReportCreate(event: HouseReportAddEvent) {
        finish()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReportCreate(event: DetectDirListEvent) {
        //目录列表编辑成功，刷新数据
        viewModel.queryById(intent.getLongExtra(ExtraKeyConfig.DETECT_ID, 0))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReportCreate(event: DetectItemListEvent) {
        //项目列表编辑成功，刷新数据
        viewModel.queryById(intent.getLongExtra(ExtraKeyConfig.DETECT_ID, 0))
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 101) {
            val absolutePath: String = data?.getStringExtra(ExtraKeyConfig.RESULT_IMAGE_PATH) ?: return
            editItemDetect.addOneImage(absolutePath)
            viewModel.updateItem(editItemDetect)
            view_house_detect.notifyItemChange(editLayoutIndex)
        }
    }

    /**
     * 从系统相册拾取图片结果
     */
    private val galleryPickResult = registerForActivityResult(ActivityResultContracts.GetContent()) {
        val srcFile: File? = UriUtils.uri2File(it)
        if (srcFile != null) {
            val copyFile = FileConfig.getDetectImageDir(this, "Item${System.currentTimeMillis()}.png")
            FileUtils.copy(srcFile, copyFile)
            editItemDetect.addOneImage(copyFile.absolutePath)
            viewModel.updateItem(editItemDetect)
            view_house_detect.notifyItemChange(editLayoutIndex)
        }
    }

    /**
     * 从系统相机拍照结果
     */
    private val lightPhotoResult = registerForActivityResult(TakePhotoResult()) {
        if (it != null) {
            editItemDetect.addOneImage(it.absolutePath)
            viewModel.updateItem(editItemDetect)
            view_house_detect.notifyItemChange(editLayoutIndex)
        }
    }

    /**
     * 项目输入文字结果
     */
    private val textInputResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            val inputText: String = it.data?.getStringExtra(ExtraKeyConfig.RESULT_INPUT_TEXT) ?: ""
            if (editItemDetect.inputText != inputText) {//有变化，刷新
                editItemDetect.inputText = inputText
                viewModel.updateItem(editItemDetect)
                view_house_detect.notifyItemChange(editLayoutIndex)
            }
        }
    }
}