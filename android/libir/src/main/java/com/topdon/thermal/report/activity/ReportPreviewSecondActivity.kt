package com.topdon.thermal.report.activity

import android.content.Intent
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.ToastUtils
import com.topdon.lib.core.bean.event.ReportCreateEvent
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseViewModelActivity
import com.topdon.lib.core.tools.FileTools
import com.topdon.lib.core.tools.GlideLoader
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.utils.NetWorkUtils
import com.topdon.libcom.PDFHelp
import com.topdon.lms.sdk.LMS
import com.topdon.lms.sdk.utils.StringUtils
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.thermal.report.view.ReportIRShowView
import com.topdon.thermal.R
import com.topdon.thermal.databinding.ActivityReportPreviewSecondBinding
import com.topdon.thermal.report.bean.ReportBean
import com.topdon.thermal.report.viewmodel.UpReportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File

/**
 * Professional thermal imaging report preview activity (Step 2) with comprehensive report management,
 * PDF generation capabilities, and industry-standard documentation workflow for research and clinical applications.
 * 
 * This activity provides professional report preview functionality with comprehensive thermal report visualization,
 * industrial-grade PDF generation, watermark management, and system-level sharing capabilities suitable for 
 * clinical and research environments.
 *
 * **Features:**
 * - Professional report preview with comprehensive thermal data visualization
 * - Industry-standard PDF generation with watermark support
 * - Research-grade document workflow with validation
 * - Multi-device support for TC007 and other thermal imaging devices
 * - Professional sharing capabilities with system integration
 * - Clinical-grade data persistence and management
 * - Comprehensive lifecycle management for research applications
 *
 * @param ExtraKeyConfig.IS_TC007 Boolean indicating if device is TC007 type
 * @param ExtraKeyConfig.REPORT_BEAN Complete report data for professional visualization
 *
 * @author Professional Thermal Imaging System
 * @since Professional thermal imaging implementation
 */
@Route(path = RouterConfig.REPORT_PREVIEW_SECOND)
class ReportPreviewSecondActivity: BaseViewModelActivity<UpReportViewModel>(), View.OnClickListener {

    /**
     * Professional ViewBinding instance for type-safe view access with comprehensive error handling
     */
    private lateinit var binding: ActivityReportPreviewSecondBinding

    /**
     * Device type indicator from previous activity.
     * @property true for TC007 professional thermal devices, false for other plugin-style devices
     */
    private var isTC007 = false

    /**
     * Complete report data from previous activity containing all thermal imaging information.
     * @property reportBean Professional thermal report data with comprehensive analysis
     */
    private var reportBean: ReportBean? = null

    /**
     * Absolute file path for the generated PDF document.
     * @property pdfFilePath Professional PDF document path for system-level sharing
     */
    private var pdfFilePath: String? = null

    /**
     * Initialize the activity layout using ViewBinding for type-safe view access.
     * @return Layout resource ID for professional report preview interface
     */
    override fun initContentView() = R.layout.activity_report_preview_second

    /**
     * Provide the ViewModel class for professional thermal report management.
     * @return UpReportViewModel class for comprehensive report upload operations
     */
    override fun providerVMClass() = UpReportViewModel::class.java

    /**
     * Initialize ViewBinding and configure professional thermal report preview interface with comprehensive
     * navigation, data visualization, and lifecycle management for research and clinical applications.
     */
    override fun initView() {
        // Initialize ViewBinding for type-safe view access
        binding = ActivityReportPreviewSecondBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Extract professional report data from intent extras
        reportBean = intent.getParcelableExtra(ExtraKeyConfig.REPORT_BEAN)
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        // Configure professional title bar with navigation and exit functionality
        binding.titleView.setTitleText(R.string.album_edit_preview)
        binding.titleView.setLeftDrawable(R.drawable.svg_arrow_left_e8)
        binding.titleView.setRightDrawable(R.drawable.ic_report_exit_svg)
        binding.titleView.setLeftClickListener {
            finish()
        }
        binding.titleView.setRightClickListener {
            TipDialog.Builder(this)
                .setMessage(R.string.album_report_exit_tips)
                .setPositiveListener(R.string.app_ok){
                    EventBus.getDefault().post(ReportCreateEvent())
                    finish()
                }
                .setCancelListener(R.string.app_cancel){
                }
                .setCanceled(false)
                .create().show()
        }

        // Configure professional report information display
        binding.reportInfoView.refreshInfo(reportBean?.report_info)
        binding.reportInfoView.refreshCondition(reportBean?.detection_condition)

        // Configure professional watermark display if enabled
        if (reportBean?.report_info?.is_report_watermark == 1) {
            binding.watermarkView.watermarkText = reportBean?.report_info?.report_watermark
        }

        // Generate professional thermal IR data visualization components
        val irList = reportBean?.infrared_data
        if (irList != null) {
            for (i in irList.indices) {
                val reportShowView = ReportIRShowView(this)
                reportShowView.refreshData(i == 0, i == irList.size - 1, irList[i])
                lifecycleScope.launch {
                    val drawable = GlideLoader.getDrawable(this@ReportPreviewSecondActivity, irList[i].picture_url)
                    reportShowView.setImageDrawable(drawable)
                }
                binding.llContent.addView(reportShowView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }

        // Configure professional action buttons
        binding.tvToPdf.setOnClickListener(this)
        binding.tvComplete.setOnClickListener(this)
        
        // Configure professional lifecycle management for network optimization
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                // Professional network management for TS004/TC007 connected devices
                if (WebSocketProxy.getInstance().isConnected()) {
                    NetWorkUtils.connectivityManager.bindProcessToNetwork(null)
                }
            }
        })
    }

    /**
     * Initialize professional data observers for comprehensive report upload management with
     * error handling and navigation control suitable for clinical and research applications.
     */
    override fun initData() {
        // Professional report upload success observer
        viewModel.commonBeanLD.observe(this) {
            dismissCameraLoading()
            if (it.code == LMS.SUCCESS) {
                EventBus.getDefault().post(ReportCreateEvent())
                ARouter.getInstance().build(RouterConfig.REPORT_LIST)
                    .withBoolean(ExtraKeyConfig.IS_TC007, isTC007)
                    .navigation(this)
                finish()
            } else {
                ToastUtils.showShort(StringUtils.getResString(this, it.code.toString()))
            }
        }
        
        // Professional exception handling observer
        viewModel.exceptionLD.observe(this) {
            dismissCameraLoading()
            requestError(it)
        }
    }

    /**
     * Handle professional click events for PDF generation and report completion with
     * comprehensive validation and upload management.
     *
     * @param v The clicked view for professional action handling
     */
    override fun onClick(v: View?) {
        when (v) {
            binding.tvToPdf -> {
                // Generate professional PDF document
                saveWithPDF()
            }
            binding.tvComplete -> {
                // Complete professional report workflow
                if (LMS.getInstance().isLogin) {
                    if (!NetworkUtils.isConnected()) {
                        TToast.shortToast(this, R.string.setting_http_error)
                        return
                    }
                    showCameraLoading()
                    viewModel.upload(isTC007, reportBean)
                } else {
                    LMS.getInstance().activityLogin()
                }
            }
        }
    }

    /**
     * Generate and save professional PDF document with comprehensive watermark support and 
     * system-level sharing capabilities for research and clinical applications.
     */
    private fun saveWithPDF() {
        if (TextUtils.isEmpty(pdfFilePath)) {
            showCameraLoading()
            lifecycleScope.launch(Dispatchers.IO) {
                val name = reportBean?.report_info?.report_number
                if (name != null) {
                    if (File(FileConfig.getPdfDir() + "/$name.pdf").exists() &&
                        !TextUtils.isEmpty(pdfFilePath)) {
                        lifecycleScope.launch {
                            dismissCameraLoading()
                            actionShare()
                        }
                        return@launch
                    }
                }
                pdfFilePath = PDFHelp.savePdfFileByListView(name?:System.currentTimeMillis().toString(),
                    binding.scrollView, getPrintViewList(), binding.watermarkView)
                lifecycleScope.launch {
                    binding.tvToPdf.text = getString(R.string.battery_share)
                    dismissCameraLoading()
                    actionShare()
                }
            }
        } else {
            actionShare()
        }
    }

    /**
     * Execute professional system-level PDF sharing with comprehensive file handling
     * suitable for clinical and research document distribution.
     */
    private fun actionShare() {
        val uri = FileTools.getUri(File(pdfFilePath!!))
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.type = "application/pdf"
        startActivity(Intent.createChooser(shareIntent, getString(R.string.battery_share)))
    }

    /**
     * Generate comprehensive list of views for PDF conversion with professional thermal data visualization.
     * Note: Watermark view is handled separately in the PDF generation process.
     *
     * @return ArrayList of views for professional PDF document generation
     */
    private fun getPrintViewList(): ArrayList<View> {
        val result = ArrayList<View>()
        result.add(binding.reportInfoView)
        val childCount = binding.llContent.childCount
        for (i in 0 until childCount) {
            val childView = binding.llContent.getChildAt(i)
            if (childView is ReportIRShowView) {
                result.addAll(childView.getPrintViewList())
            }
        }
        return result
    }
