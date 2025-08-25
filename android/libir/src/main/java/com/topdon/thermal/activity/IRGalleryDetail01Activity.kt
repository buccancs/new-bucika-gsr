package com.topdon.thermal.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.elvishew.xlog.XLog
import com.topdon.lib.core.bean.GalleryBean
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.FileTools
import com.topdon.lib.core.tools.TimeTool
import com.topdon.lib.core.tools.ToastTools
import com.topdon.lib.core.utils.ByteUtils.bytesToInt
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.ui.dialog.ProgressDialog
import com.topdon.libcom.ExcelUtil
import com.topdon.thermal.R
import com.topdon.lib.core.bean.event.GalleryDelEvent
import com.topdon.lib.core.utils.Constants.IS_REPORT_FIRST
import com.topdon.thermal.event.ImageGalleryEvent
import com.topdon.thermal.fragment.GalleryFragment
import com.topdon.thermal.frame.FrameTool
import com.topdon.thermal.viewmodel.IRGalleryEditViewModel
import com.topdon.thermal.databinding.ActivityIrGalleryDetail01Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

@Route(path = RouterConfig.IR_GALLERY_DETAIL_01)
class IRGalleryDetail01Activity : BaseActivity(), View.OnClickListener {

    private lateinit var binding: ActivityIrGalleryDetail01Binding

    private var isTC007 = false

    private var position = 0
    
    private lateinit var dataList: ArrayList<GalleryBean>

    private var irPath: String? = null
    
    private val irViewModel: IRGalleryEditViewModel by viewModels()

    override fun initContentView(): Int {
        binding = ActivityIrGalleryDetail01Binding.inflate(layoutInflater)
        setContentView(binding.root)
        return 0
    }
    
    private val frameTool by lazy { FrameTool() }

    override fun initView() {
        position = intent.getIntExtra("position", 0)
        dataList = intent.getParcelableArrayListExtra("list")!!
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        binding.titleView.setTitleText("${position + 1}/${dataList.size}")
        binding.titleView.setRightClickListener { actionInfo() }
        binding.titleView.setRight2ClickListener { actionShare() }
        binding.titleView.setRight3ClickListener { deleteImage() }

        initViewPager()

        ll_ir_edit_2D?.setOnClickListener(this)
        ll_ir_edit_3D?.setOnClickListener(this)
        ll_ir_report?.setOnClickListener(this)
        ll_ir_ex?.setOnClickListener(this)

        irViewModel.resultLiveData.observe(this) {
            lifecycleScope.launch {
                val filePath: String?
                withContext(Dispatchers.IO) {
                    frameTool.read(it.frame)
                    filePath = ExcelUtil.exportExcel(
                        excelName,
                        192,
                        256,
                        frameTool.getRotate90Temp(frameTool.temperatureBytes)
                    ) { current, total ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            progressDialog?.max = total
                            progressDialog?.progress = current
                        }
                    }
                }
                progressDialog?.dismiss()
                if (filePath.isNullOrEmpty()) {
                    ToastTools.showShort(R.string.liveData_save_error)
                } else {
                    val uri = FileTools.getUri(File(filePath))
                    val shareIntent = Intent()
                    shareIntent.action = Intent.ACTION_SEND
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                    shareIntent.type = "application/xlsx"
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.battery_share)))
                }
            }
        }
    }

    override fun initData() {

    }

    @SuppressLint("SetTextI18n")
    private fun initViewPager() {
        binding.irGalleryViewpager.adapter = GalleryViewPagerAdapter(this)
        binding.irGalleryViewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                this@IRGalleryDetail01Activity.position = position
                binding.titleView.setTitleText("${position + 1}/${dataList.size}")

                irPath = "${FileConfig.lineIrGalleryDir}/${dataList[position].name.substringBeforeLast(".")}.ir"
                val hasIrData = File(irPath!!).exists()
                ll_ir_edit_3D?.isVisible = hasIrData
                ll_ir_report?.isVisible = hasIrData
                ll_ir_edit_2D?.isVisible = hasIrData
                ll_ir_ex?.isVisible = hasIrData
            }
        })
        ir_gallery_viewpager?.setCurrentItem(position, false)
    }

    private fun actionInfo() {
        try {
            val data = dataList[position]
            val exif = ExifInterface(data.path)
            val width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
            val length = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
            val whStr = "${width}x${length}"
            val sizeStr = FileTools.getFileSize(data.path)

            val str = StringBuilder()
            str.append(getString(R.string.detail_date)).append("\n")
            str.append(TimeTool.showDateType(data.timeMillis)).append("\n\n")
            str.append(getString(R.string.detail_info)).append("\n")
            str.append("${getString(R.string.detail_size)}: ").append(whStr).append("\n")
            str.append("${getString(R.string.detail_len)}: ").append(sizeStr).append("\n")
            str.append("${getString(R.string.detail_path)}: ").append(data.path).append("\n")
            TipDialog.Builder(this).setMessage(str.toString()).setCanceled(true).create().show()
        } catch (e: Exception) {
            ToastTools.showShort(R.string.status_error_load_fail)
        }
    }

    private fun actionShare() {
        val data = dataList[position]
        val uri = FileTools.getUri(File(data.path))
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.type = "image/jpeg"
        startActivity(Intent.createChooser(shareIntent, getString(R.string.battery_share)))
    }

    private fun deleteImage() {
        TipDialog.Builder(this)
            .setMessage(getString(R.string.tip_delete))
            .setPositiveListener(R.string.app_confirm) {
                val data = dataList[position]
                if (dataList.size == 1) {
                    File(data.path).delete()
                    finish()
                } else {
                    File(data.path).delete()
                    dataList.removeAt(position)
                    if (position >= dataList.size) {
                        position = dataList.size - 1
                    }
                    initViewPager()
                }
                EventBus.getDefault().post(GalleryDelEvent())
            }
            .setCancelListener(R.string.app_cancel)
            .create()
            .show()
    }

    private var progressDialog: ProgressDialog? = null
    private var excelName: String = ""

    private fun actionExcel() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this)
        }
        progressDialog?.show()

        excelName = dataList[position].name.substringBeforeLast(".")
        val irPath = "${FileConfig.lineIrGalleryDir}/${excelName}.ir"
        if (!File(irPath).exists()) {
            ToastTools.showShort(getString(R.string.album_report_on_edit))
            progressDialog?.dismiss()
            return
        }
        irViewModel.initData(irPath)
    }

    override fun onClick(v: View?) {
        when (v) {
            ll_ir_edit_2D -> {

                actionEditOrReport(false)
            }

            ll_ir_edit_3D -> {

                val data = dataList[position]
                val fileName = data.name.substringBeforeLast(".")
                val irPath = "${FileConfig.lineIrGalleryDir}/${fileName}.ir"
                if (!File(irPath).exists()) {
                    ToastTools.showShort(R.string.album_report_on_edit)
                    return
                }
                var tempHigh = 0f
                var tempLow = 0f
                lifecycleScope.launch {

                    withContext(Dispatchers.IO) {
                        val file = File(irPath)
                        if (!file.exists()) {
                            XLog.w("IR文件不存在: ${file.absolutePath}")
                            return@withContext
                        }
                        XLog.w("IR文件: ${file.absolutePath}")
                        val bytes = file.readBytes()
                        val headLenBytes = ByteArray(2)
                        System.arraycopy(bytes, 0, headLenBytes, 0, 2)
                        val headLen = headLenBytes.bytesToInt()
                        val headDataBytes = ByteArray(headLen)
                        val frameDataBytes = ByteArray(bytes.size - headLen)
                        System.arraycopy(bytes, 0, headDataBytes, 0, headDataBytes.size)
                        System.arraycopy(bytes, headLen, frameDataBytes, 0, frameDataBytes.size)
                        frameTool.read(frameDataBytes)
                        tempHigh = frameTool.getSrcTemp().maxTemperature
                        tempLow = frameTool.getSrcTemp().minTemperature
                    }

                    ARouter.getInstance().build(RouterConfig.IR_GALLERY_3D).withString(ExtraKeyConfig.IR_PATH, irPath)
                        .withFloat(ExtraKeyConfig.TEMP_HIGH, tempHigh).withFloat(ExtraKeyConfig.TEMP_LOW, tempLow)
                        .navigation(this@IRGalleryDetail01Activity)
                }

            }

            ll_ir_report -> {

                actionEditOrReport(true)
            }

            ll_ir_ex -> {
                TipDialog.Builder(this).setMessage(R.string.tip_album_temp_exportfile).setPositiveListener(R.string.app_confirm) {
                        actionExcel()
                    }.setCancelListener(R.string.app_cancel) {}.setCanceled(true).create().show()
            }
        }
    }

    private fun actionEditOrReport(isReport: Boolean) {
        val data = dataList[position]
        val fileName = data.name.substringBeforeLast(".")
        val irPath = "${FileConfig.lineIrGalleryDir}/${fileName}.ir"
        if (!File(irPath).exists()) {
            ToastTools.showShort(R.string.album_report_on_edit)
            return
        }
        ARouter.getInstance().build(RouterConfig.IR_GALLERY_EDIT)
            .withBoolean(ExtraKeyConfig.IS_TC007, isTC007)
            .withBoolean(ExtraKeyConfig.IS_PICK_REPORT_IMG, isReport)
            .withBoolean(IS_REPORT_FIRST, true)
            .withString(ExtraKeyConfig.FILE_ABSOLUTE_PATH, irPath)
            .navigation(this)
    }

    inner class GalleryViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

        override fun getItemCount(): Int {
            return dataList.size
        }

        override fun createFragment(position: Int): Fragment {
            val fragment = GalleryFragment()
            val bundle = Bundle()
            bundle.putString("path", dataList[position].path)
            fragment.arguments = bundle
            return fragment
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSaveFinishBean(imageGalleryEvent : ImageGalleryEvent) {
        finish()
    }
}
