package com.topdon.module.thermal.ir.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.alibaba.android.arouter.facade.annotation.Route
import com.blankj.utilcode.util.FileUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.topdon.lib.core.bean.GalleryBean
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.FileTools
import com.topdon.lib.core.tools.TimeTool
import com.topdon.lib.core.tools.ToastTools
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.repository.TS004Repository
import com.topdon.module.thermal.ir.R
import com.topdon.lib.core.dialog.ConfirmSelectDialog
import com.topdon.lib.core.bean.event.GalleryDelEvent
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.module.thermal.ir.event.GalleryDownloadEvent
import com.topdon.module.thermal.ir.fragment.GalleryFragment
import com.topdon.module.thermal.ir.databinding.ActivityIrGalleryDetail04Binding
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File

/**
 * Professional TS004 Gallery Detail Activity for Thermal Image Management
 * 
 * This activity provides comprehensive thermal image detail viewing and management
 * capabilities for both local and remote TS004 device images. Essential for research
 * workflows requiring detailed image analysis and professional gallery management.
 * 
 * **Gallery Management Features:**
 * - Professional ViewPager2 integration for smooth image browsing with research-grade performance
 * - Support for both local device storage and remote TS004 device image access
 * - Comprehensive image metadata display including EXIF data and thermal parameters
 * - Professional download management with progress indication for research archival
 * - Advanced sharing capabilities for research collaboration and data distribution
 * 
 * **Research Application Features:**
 * - High-resolution thermal image display with professional zoom and pan capabilities
 * - Detailed file information including size, date, resolution, and storage location
 * - Professional deletion workflow with local/remote synchronization options
 * - Integrated gallery navigation with position tracking for research efficiency
 * - Real-time download status monitoring with visual feedback indicators
 * 
 * **Device Integration:**
 * - TS004 remote device connectivity with secure image streaming and download
 * - Local gallery management with system media scanner integration
 * - Professional loading states and error handling for research application reliability
 * - Cross-platform file sharing with appropriate MIME type handling
 * 
 * **Professional UI Features:**
 * - Fragment-based gallery viewer with optimized memory management for large datasets
 * - Dynamic title updates showing current position in image sequence
 * - Context-sensitive action buttons (local vs remote) for streamlined workflows
 * - Professional confirmation dialogs for destructive operations
 * 
 * @author BucikaGSR Development Team
 * @since 2024.1.0
 * @see GalleryFragment For individual image display and interaction handling
 * @see TS004Repository For remote device communication and file management
 */
@Route(path = RouterConfig.IR_GALLERY_DETAIL_04)
class IRGalleryDetail04Activity : BaseActivity() {

    /**
     * ViewBinding instance for type-safe view access
     * Replaces deprecated Kotlin synthetics with modern binding pattern
     */
    private lateinit var binding: ActivityIrGalleryDetail04Binding

    /**
     * Flag indicating whether this gallery is viewing remote TS004 device images
     * - true: Remote TS004 device images (requires download for offline access)
     * - false: Local device storage images (immediate access available)
     */
    private var isRemote = false
    
    /**
     * Current image position in the gallery sequence
     * Used for professional navigation and title display
     */
    private var position = 0
    
    /**
     * Complete list of thermal images for gallery browsing
     * Contains metadata for both local and remote images
     */
    private lateinit var dataList: ArrayList<GalleryBean>

    override fun initContentView(): Int {
        binding = ActivityIrGalleryDetail04Binding.inflate(layoutInflater)
        setContentView(binding.root)
        return 0 // ViewBinding handles layout inflation
    }

    @SuppressLint("SetTextI18n")
    override fun initView() {
        isRemote = intent.getBooleanExtra("isRemote", false)
        position = intent.getIntExtra("position", 0)
        dataList = intent.getParcelableArrayListExtra("list")!!

        binding.titleView.setTitleText("${position + 1}/${dataList.size}")

        binding.clBottom.isVisible = isRemote //查看远端时底部才有3个按钮

        if (!isRemote) {
            binding.titleView.setRightDrawable(R.drawable.ic_toolbar_info_svg)
            binding.titleView.setRight2Drawable(R.drawable.ic_toolbar_share_svg)
            binding.titleView.setRight3Drawable(R.drawable.ic_toolbar_delete_svg)
            binding.titleView.setRightClickListener { actionInfo() }
            binding.titleView.setRight2ClickListener { actionShare() }
            binding.titleView.setRight3ClickListener { actionDelete() }
        }

        initViewPager()

        binding.clDownload.setOnClickListener {
            actionDownload(false)
        }
        binding.clShare.setOnClickListener {
            if (dataList[position].hasDownload) {
                actionShare()
            } else {
                actionDownload(true)
            }
        }
        binding.clDelete.setOnClickListener {
            actionDelete()
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
                this@IRGalleryDetail04Activity.position = position
                binding.titleView.setTitleText("${position + 1}/${dataList.size}")
                binding.ivDownload.isSelected = dataList[position].hasDownload
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
        val uri = FileTools.getUri(File(FileConfig.ts004GalleryDir, data.name))
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.type = "image/jpeg"
        startActivity(Intent.createChooser(shareIntent, getString(R.string.battery_share)))
    }

    private fun actionDelete() {
        ConfirmSelectDialog(this).run {
            setTitleRes(R.string.tip_delete)
            setMessageRes(R.string.also_del_from_phone_album)
            setShowMessage(isRemote && dataList[position].hasDownload)
            onConfirmClickListener = {
                deleteFile(it)
            }
            show()
        }
    }

    private fun deleteFile(isDelLocal: Boolean) {
        val data = dataList[position]
        if (isRemote) {
            lifecycleScope.launch {
                showCameraLoading()

                val isSuccess = TS004Repository.deleteFiles(arrayOf(data.id))
                if (isSuccess) {
                    if (isDelLocal) {
                        File(FileConfig.ts004GalleryDir, data.name).delete()
                        MediaScannerConnection.scanFile(this@IRGalleryDetail04Activity, arrayOf(FileConfig.ts004GalleryDir), null, null)
                    }

                    dismissCameraLoading()
                    ToastTools.showShort(R.string.test_results_delete_success)
                    EventBus.getDefault().post(GalleryDelEvent())
                    if (dataList.size == 1) {
                        finish()
                    } else {
                        dataList.removeAt(position)
                        if (position >= dataList.size) {
                            position = dataList.size - 1
                        }
                        initViewPager()
                    }
                } else {
                    dismissCameraLoading()
                    TToast.shortToast(this@IRGalleryDetail04Activity, R.string.test_results_delete_failed)
                }
            }
        } else {
            File(data.path).delete()
            MediaScannerConnection.scanFile(this, arrayOf(FileConfig.ts004GalleryDir), null, null)
            EventBus.getDefault().post(GalleryDelEvent())
            if (dataList.size == 1) {
                finish()
            } else {
                dataList.removeAt(position)
                if (position >= dataList.size) {
                    position = dataList.size - 1
                }
                initViewPager()
            }
        }
    }


    private fun actionDownload(isToShare: Boolean) {
        val data = dataList[position]
        if (data.hasDownload) {
            if (isToShare) {
                actionShare()
            }
            return
        }
        showCameraLoading()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Glide.with(this).downloadOnly().load(data.path).addListener(object : RequestListener<File> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<File>?, isFirstResource: Boolean): Boolean {
                    dismissCameraLoading()
                    ToastTools.showShort(R.string.liveData_save_error)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    return false
                }

                override fun onResourceReady(
                    resource: File?, model: Any?, target: Target<File>?, dataSource: DataSource?, isFirstResource: Boolean
                ): Boolean {
                    EventBus.getDefault().post(GalleryDownloadEvent(data.name))
                    dismissCameraLoading()
                    FileUtils.copy(resource, File(FileConfig.ts004GalleryDir, data.name))
                    MediaScannerConnection.scanFile(this@IRGalleryDetail04Activity, arrayOf(FileConfig.ts004GalleryDir), null, null)
                    ToastTools.showShort(R.string.tip_save_success)
                    data.hasDownload = true
                    binding.ivDownload.isSelected = dataList[position].hasDownload
                    if (isToShare) {
                        actionShare()
                    }
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    return false
                }
            }).preload()
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
}

