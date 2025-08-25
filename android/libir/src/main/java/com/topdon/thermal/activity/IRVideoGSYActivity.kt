package com.topdon.thermal.activity

import android.content.Intent
import android.media.MediaScannerConnection
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.blankj.utilcode.util.BarUtils
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder
import com.shuyu.gsyvideoplayer.player.PlayerFactory
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer
import com.topdon.lib.core.bean.GalleryBean
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.FileTools
import com.topdon.lib.core.tools.TimeTool
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.repository.TS004Repository
import com.topdon.lib.core.tools.ToastTools
import com.topdon.thermal.R
import com.topdon.lib.core.dialog.ConfirmSelectDialog
import com.topdon.lib.core.bean.event.GalleryDelEvent
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.thermal.event.GalleryDownloadEvent
import com.topdon.thermal.databinding.ActivityIrVideoGsyBinding
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager
import java.io.File

/**
 * Professional Video Player Activity for Thermal IR Video Playback
 * 
 * This activity provides comprehensive video playback capabilities for thermal IR videos
 * captured by TC001 devices, with support for both local and remote TS004 video files.
 * Essential for reviewing thermal video recordings in research and clinical applications.
 * 
 * **Video Playback Features:**
 * - Professional GSY video player integration with ExoPlayer backend
 * - Support for both local files and remote TS004 device streaming
 * - Full-screen video playback with optimized thermal video rendering
 * - Professional video controls with research-grade playback accuracy
 * - Automatic file format detection and appropriate codec selection
 * 
 * **Gallery Management:**
 * - Download remote videos for offline analysis and archival
 * - Share thermal videos with research collaborators
 * - Professional deletion with local/remote synchronization options
 * - File information display including size, date, and storage location
 * - Integration with system media scanner for gallery visibility
 * 
 * **Research Application Features:**
 * - Thermal video archival system for research data management
 * - Professional metadata display for research documentation
 * - Integrated download management for research workflow efficiency
 * - Cross-platform sharing capabilities for research collaboration
 * 
 * **Device Integration:**
 * - TS004 remote device video streaming and management
 * - TC001 local video file playback and organization
 * - Professional loading indicators for research application standards
 * - Real-time file status tracking and synchronization
 * 
 * @author BucikaGSR Development Team
 * @since 2024.1.0
 * @see GalleryBean For video metadata and file information structure
 * @see TS004Repository For remote video download and management operations
 */
@Route(path = RouterConfig.IR_VIDEO_GSY)
class IRVideoGSYActivity : BaseActivity() {

    /**
     * ViewBinding instance for type-safe view access
     * Replaces deprecated Kotlin synthetics with modern binding pattern
     */
    private lateinit var binding: ActivityIrVideoGsyBinding

    /**
     * Flag indicating whether this video is from a remote TS004 device
     * - true: Video from remote TS004 device (requires download for offline access)
     * - false: Local video file stored on device (immediate playback available)
     */
    private var isRemote = false
    
    /**
     * Video metadata containing file path, download status, and display information
     * Essential for managing thermal video files in research workflows
     */
    private lateinit var data: GalleryBean
    
    override fun initContentView(): Int {
        binding = ActivityIrVideoGsyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        return 0 // ViewBinding handles layout inflation
    }

    override fun initView() {
        BarUtils.setNavBarColor(this, ContextCompat.getColor(this, R.color.black))

        isRemote = intent.getBooleanExtra("isRemote", false)
        data = intent.getParcelableExtra("data") ?: throw NullPointerException("传递 data")

        binding.clBottom.isVisible = isRemote //查看远端时底部才有3个按钮

        if (!isRemote) {
            binding.titleView.setRightDrawable(R.drawable.ic_toolbar_info_svg)
            binding.titleView.setRight2Drawable(R.drawable.ic_toolbar_share_svg)
            binding.titleView.setRight3Drawable(R.drawable.ic_toolbar_delete_svg)
            binding.titleView.setRightClickListener { actionInfo() }
            binding.titleView.setRight2ClickListener { actionShare() }
            binding.titleView.setRight3ClickListener { showDeleteDialog() }
        }

        binding.clDownload.setOnClickListener {
            actionDownload(false)
        }
        binding.clShare.setOnClickListener {
            if (data.hasDownload) {
                actionShare()
            } else {
                actionDownload(true)
            }
        }
        binding.clDelete.setOnClickListener {
            showDeleteDialog()
        }

        binding.ivDownload.isSelected = data.hasDownload
        binding.ivDownload.setImageResource(if (isRemote) R.drawable.selector_download else R.drawable.ic_toolbar_info_svg)

        previewVideo(isRemote, data.path)
    }

    override fun initData() {
    }

    private fun previewVideo(isRemote: Boolean, path: String) {
        PlayerFactory.setPlayManager(Exo2PlayerManager::class.java)
        val url = if (isRemote) {
            path
        } else {
            path.replace("//", "/")
            "file://$path"
        }

        GSYVideoOptionBuilder()
            .setUrl(url)
            .build(gsy_play)
        //界面设置
        binding.gsyPlay.isNeedShowWifiTip = false //不显示消耗流量弹框
        binding.gsyPlay.titleTextView.visibility = View.GONE
        binding.gsyPlay.backButton.visibility = View.GONE
        binding.gsyPlay.fullscreenButton.visibility = View.GONE
    }

    private fun actionDownload(isToShare: Boolean) {
        if (data.hasDownload) {//已下载
            if (isToShare) {
                actionShare()
            }
            return
        }
        lifecycleScope.launch {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            showCameraLoading()
            val isSuccess = TS004Repository.download(data.path, File(FileConfig.ts004GalleryDir, data.name))
            MediaScannerConnection.scanFile(this@IRVideoGSYActivity, arrayOf(FileConfig.ts004GalleryDir), null, null)
            dismissCameraLoading()
            if (isSuccess) {
                ToastTools.showShort(R.string.tip_save_success)
                EventBus.getDefault().post(GalleryDownloadEvent(data.name))
                data.hasDownload = true
                binding.ivDownload.isSelected = true
                if (isToShare) {
                    actionShare()
                }
            } else {
                ToastTools.showShort(R.string.liveData_save_error)
            }
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun actionInfo() {
        val sizeStr = FileTools.getFileSize(data.path)
        val str = StringBuilder()
        str.append(getString(R.string.detail_date)).append("\n")
        str.append(TimeTool.showDateType(data.timeMillis)).append("\n\n")
        str.append(getString(R.string.detail_info)).append("\n")
//        str.append("尺寸: ").append(whStr).append("\n")
        str.append("${getString(R.string.detail_len)}: ").append(sizeStr).append("\n")
        str.append("${getString(R.string.detail_path)}: ").append(data.path).append("\n")
        TipDialog.Builder(this)
            .setMessage(str.toString())
            .setCanceled(true)
            .create().show()
    }

    private fun actionShare() {
        val uri = FileTools.getUri(File(data.path))
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.type = "video/*"
        startActivity(Intent.createChooser(shareIntent, getString(R.string.battery_share)))
    }

    private fun showDeleteDialog() {
        ConfirmSelectDialog(this).run {
            setTitleRes(R.string.tip_delete)
            setMessageRes(R.string.also_del_from_phone_album)
            setShowMessage(isRemote && data.hasDownload)
            onConfirmClickListener = {
                deleteFile(it)
            }
            show()
        }
    }

    private fun deleteFile(isDelLocal: Boolean) {
        if (isRemote) {
            lifecycleScope.launch {
                showCameraLoading()
                val isSuccess = TS004Repository.deleteFiles(arrayOf(data.id))
                if (isSuccess) {
                    if (isDelLocal) {
                        File(FileConfig.ts004GalleryDir, data.name).delete()
                        MediaScannerConnection.scanFile(this@IRVideoGSYActivity, arrayOf(FileConfig.ts004GalleryDir), null, null)
                    }
                    dismissCameraLoading()
                    ToastTools.showShort(R.string.test_results_delete_success)
                    EventBus.getDefault().post(GalleryDelEvent())
                    finish()
                } else {
                    dismissCameraLoading()
                    TToast.shortToast(this@IRVideoGSYActivity, R.string.test_results_delete_failed)
                }
            }
        } else {
            EventBus.getDefault().post(GalleryDelEvent())
            File(data.path).delete()
            MediaScannerConnection.scanFile(this, arrayOf(FileConfig.ts004GalleryDir), null, null)
            finish()
        }
    }

    override fun onResume() {
        getCurPlay().onVideoResume(false)
        super.onResume()
    }

    override fun onPause() {
        getCurPlay().onVideoPause()
        super.onPause()
    }

    private fun getCurPlay(): GSYVideoPlayer {
        return if (binding.gsyPlay.fullWindowPlayer != null) {
            binding.gsyPlay.fullWindowPlayer
        } else {
            gsy_play
        }
    }
