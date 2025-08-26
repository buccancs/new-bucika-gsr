package com.topdon.thermal.activity

import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.topdon.lib.core.bean.GalleryTitle
import com.topdon.lib.core.bean.event.ReportCreateEvent
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.FileTools.getUri
import com.topdon.lib.core.tools.ToastTools
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.repository.GalleryRepository.DirType
import com.topdon.thermal.R
import com.topdon.thermal.adapter.GalleryAdapter
import com.topdon.lib.core.bean.event.GalleryDelEvent
import com.topdon.lib.core.utils.Constants.IS_REPORT_FIRST
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.thermal.databinding.ActivityReportPickImgBinding
import com.topdon.thermal.viewmodel.IRGalleryViewModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

@Route(path = RouterConfig.REPORT_PICK_IMG)
class ReportPickImgActivity : BaseActivity(), View.OnClickListener {

    private lateinit var binding: ActivityReportPickImgBinding

    private var isTC007 = false

    private val viewModel: IRGalleryViewModel by viewModels()

    private val adapter = GalleryAdapter()

    override fun initContentView() = R.layout.activity_report_pick_img.also {
        binding = ActivityReportPickImgBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun initView() {
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        with(binding) {
            titleView.setRightDrawable(R.drawable.ic_toolbar_check_svg)
            titleView.setRightClickListener { setEditMode(true) }

            clShare.setOnClickListener(this@ReportPickImgActivity)
            clDelete.setOnClickListener(this@ReportPickImgActivity)
        }

        initRecycler()

        showLoadingDialog()

        viewModel.showListLD.observe(this) {
            adapter.refreshList(it)
            dismissLoadingDialog()
        }
        viewModel.deleteResultLD.observe(this) {
            if (it) {
                TToast.shortToast(this@ReportPickImgActivity, R.string.test_results_delete_success)
                adapter.isEditMode = false
                EventBus.getDefault().post(GalleryDelEvent())
                MediaScannerConnection.scanFile(this, arrayOf(if (isTC007) FileConfig.tc007GalleryDir else FileConfig.lineGalleryDir), null, null)
                viewModel.queryAllReportImg(if (isTC007) DirType.TC007 else DirType.LINE)
            } else {
                TToast.shortToast(this@ReportPickImgActivity, R.string.test_results_delete_failed)
            }
        }
        viewModel.queryAllReportImg(if (isTC007) DirType.TC007 else DirType.LINE)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onReportCreate(event: ReportCreateEvent) {
        finish()
    }

    override fun initData() {

    }

    override fun onBackPressed() {
        if (adapter.isEditMode) {
            setEditMode(false)
        } else {
            super.onBackPressed()
        }
    }

    private fun setEditMode(isEditMode: Boolean) {
        adapter.isEditMode = isEditMode
        binding.groupBottom.isVisible = isEditMode
        binding.titleView.setTitleText(if (isEditMode) getString(R.string.chosen_item, adapter.selectList.size) else getString(R.string.app_gallery))
        binding.titleView.setLeftDrawable(if (isEditMode) R.drawable.svg_x_cc else R.drawable.ic_back_white_svg)
        binding.titleView.setLeftClickListener {
            if (isEditMode) {
                setEditMode(false)
            } else {
                finish()
            }
        }
        binding.titleView.setRightDrawable(if (isEditMode) 0 else R.drawable.ic_toolbar_check_svg)
        binding.titleView.setRightText(if (isEditMode) getString(R.string.report_select_all) else "")
        binding.titleView.setRightClickListener {
            if (isEditMode) {
                adapter.selectAll()
            } else {
                setEditMode(true)
            }
        }
    }

    override fun onClick(v: View?) {
        with(binding) {
            when (v) {
                clShare -> {
                    shareImage()
                }
                clDelete -> {
                    deleteImage()
                }
            }
        }
    }

    private fun initRecycler() {
        val spanCount = 3
        val gridLayoutManager = GridLayoutManager(this, spanCount)

        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter.dataList[position] is GalleryTitle) spanCount else 1
            }
        }
        binding.irGalleryRecycler.adapter = adapter
        binding.irGalleryRecycler.layoutManager = gridLayoutManager

        adapter.onLongEditListener = {

            binding.groupBottom.isVisible = true
            binding.titleView.setTitleText(getString(R.string.chosen_item, adapter.selectList.size))
            title_view.setLeftDrawable(R.drawable.svg_x_cc)
            title_view.setLeftClickListener {
                setEditMode(false)
            }
            title_view.setRightDrawable(0)
            title_view.setRightText(R.string.report_select_all)
            binding.titleView.setRightClickListener {
                adapter.selectAll()
            }
        }

        adapter.selectCallback = {
            binding.titleView.setTitleText(getString(R.string.chosen_item, it.size))
        }
        adapter.itemClickCallback = {
            val data = adapter.dataList[it]
            val fileName = data.name.substringBeforeLast(".")
            val irPath = "${FileConfig.lineIrGalleryDir}/${fileName}.ir"
            if (File(irPath).exists()) {
                ARouter.getInstance().build(RouterConfig.IR_GALLERY_EDIT)
                    .withBoolean(ExtraKeyConfig.IS_TC007, isTC007)
                    .withBoolean(ExtraKeyConfig.IS_PICK_REPORT_IMG, true)
                    .withBoolean(IS_REPORT_FIRST, false)
                    .withString(ExtraKeyConfig.FILE_ABSOLUTE_PATH, irPath)
                    .withParcelable(ExtraKeyConfig.REPORT_INFO, intent.getParcelableExtra(ExtraKeyConfig.REPORT_INFO))
                    .withParcelable(ExtraKeyConfig.REPORT_CONDITION, intent.getParcelableExtra(ExtraKeyConfig.REPORT_CONDITION))
                    .withParcelableArrayList(ExtraKeyConfig.REPORT_IR_LIST, intent.getParcelableArrayListExtra(ExtraKeyConfig.REPORT_IR_LIST))
                    .navigation(this)
            } else {
                ToastTools.showShort(R.string.album_report_on_edit)
            }
        }
    }

    private fun deleteImage() {
        val deleteList = adapter.buildSelectList()
        if (deleteList.size > 0) {
            TipDialog.Builder(this)
                .setMessage(getString(
                        R.string.tip_delete_chosen,
                        deleteList.size
                    ))
                .setPositiveListener(R.string.app_confirm) {
                    viewModel.delete(deleteList, if (isTC007) DirType.TC007 else DirType.LINE, true)
                }.setCancelListener(R.string.app_cancel)
                .create().show()
        } else {
            ToastTools.showShort(getString(R.string.tip_least_select))
        }
    }

    private fun shareImage() {
        val data = adapter.buildSelectList()
        if (data.size == 0) {
            ToastTools.showShort(getString(R.string.tip_least_select))
            return
        }
        if (data.size > 9) {
            ToastTools.showShort(getString(R.string.Limite_di_9carte))
            return
        }
        val imageUris = ArrayList<Uri>()
        val shareIntent = Intent()
        if (data.size == 1) {
            if (data[0].name.uppercase().endsWith(".MP4")) {
                shareIntent.type = "video/*"
            } else {
                shareIntent.type = "image/*"
            }
            shareIntent.action = Intent.ACTION_SEND
            val uri = getUri(File(data[0].path))
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        } else {
            shareIntent.type = "video/*"
            for (bean in data) {
                imageUris.add(getUri(File(bean.path)))
            }
            shareIntent.action = Intent.ACTION_SEND_MULTIPLE
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUris)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.battery_share)))
    }
}
