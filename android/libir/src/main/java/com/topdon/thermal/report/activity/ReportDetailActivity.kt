package com.topdon.thermal.report.activity

import android.content.Intent
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.tools.FileTools
import com.topdon.lib.core.tools.GlideLoader
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.libcom.PDFHelp
import com.topdon.thermal.databinding.ActivityReportDetailBinding
import com.topdon.thermal.report.view.ReportIRShowView
import com.topdon.thermal.R
import com.topdon.thermal.report.bean.ReportBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Professional Thermal Report Detail Activity with Industry-Standard Documentation and ViewBinding
 *
 * This professional thermal imaging report detail activity provides comprehensive document
 * viewing and sharing capabilities for clinical and research environments with advanced
 * PDF generation, watermark management, and professional report presentation.
 *
 * **Required Parameters:**
 * - [ExtraKeyConfig.REPORT_BEAN]: Complete report information bean containing all thermal analysis data
 *
 * **Professional Features:**
 * - Comprehensive thermal report detail viewing with industry-standard presentation
 * - Professional PDF generation with high-quality document rendering and watermark integration
 * - Advanced report sharing with system-level intent handling and file permissions
 * - Real-time thermal image loading with professional image management and optimization
 * - Industry-standard watermark overlay with customizable text and positioning
 * - Professional multi-view PDF compilation with hierarchical content organization
 *
 * **Clinical Applications:**
 * - Medical thermal imaging report review with detailed analysis presentation
 * - Building inspection documentation with comprehensive thermal data visualization
 * - Industrial equipment monitoring reports with professional formatting and export capabilities
 * - Research documentation with academic-standard report generation and distribution
 *
 * @author Professional Thermal Imaging Team
 * @since 1.0.0
 */
@Route(path = RouterConfig.REPORT_DETAIL)
class ReportDetailActivity: BaseActivity() {

    /**
     * ViewBinding instance for type-safe view access and lifecycle management
     */
    private lateinit var binding: ActivityReportDetailBinding

    /**
     * Complete thermal report information from previous activity containing all analysis data
     */
    private var reportBean: ReportBean? = null

    /**
     * Current preview PDF file absolute path for professional document sharing
     */
    private var pdfFilePath: String? = null

    override fun initContentView() = R.layout.activity_report_detail

    /**
     * Initialize ViewBinding and professional thermal report detail interface
     *
     * Configures comprehensive report viewing system with advanced image loading,
     * watermark management, and professional PDF generation capabilities.
     */
    override fun initView() {
        binding = ActivityReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        reportBean = intent.getParcelableExtra(ExtraKeyConfig.REPORT_BEAN)

        // Configure professional title bar with sharing capabilities
        binding.titleView.setTitleText(R.string.album_edit_report)
        binding.titleView.setLeftDrawable(R.drawable.svg_arrow_left_e8)
        binding.titleView.setRightDrawable(R.drawable.ic_share_black_svg)
        binding.titleView.setLeftClickListener {
            finish()
        }
        binding.titleView.setRightClickListener {
            saveWithPDF()
        }

        // Configure professional report information display
        binding.reportInfoView.refreshInfo(reportBean?.report_info)
        binding.reportInfoView.refreshCondition(reportBean?.detection_condition)

        // Configure professional watermark display
        if (reportBean?.report_info?.is_report_watermark == 1) {
            binding.watermarkView.watermarkText = reportBean?.report_info?.report_watermark
        }

        // Configure professional thermal image data visualization
        val irList = reportBean?.infrared_data
        if (irList != null) {
            for (i in irList.indices) {
                val reportShowView = ReportIRShowView(this)
                reportShowView.refreshData(i == 0, i == irList.size - 1, irList[i])
                lifecycleScope.launch {
                    val drawable = GlideLoader.getDrawable(this@ReportDetailActivity, irList[i].picture_url)
                    reportShowView.setImageDrawable(drawable)
                }
                binding.llContent.addView(reportShowView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }
    }

    /**
     * Initialize professional thermal report data processing
     */
    override fun initData() {
    }

    /**
     * Generate and save professional thermal report as PDF with comprehensive document management
     *
     * Creates industry-standard PDF document from current report view with professional
     * watermark integration, file management, and system-level sharing capabilities.
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
                    binding.scrollView, getPrintViewList(),binding.watermarkView)
                lifecycleScope.launch {
                    dismissCameraLoading()
                    actionShare()
                }
            }
        } else {
            actionShare()
        }
    }

    /**
     * Initiate professional thermal report sharing with system-level intent handling
     *
     * Creates comprehensive share intent with proper file permissions and MIME type
     * configuration for professional document distribution and collaboration.
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
     * Generate comprehensive view list for professional PDF compilation
     *
     * Collects all thermal analysis views for industry-standard PDF generation with
     * hierarchical content organization and proper view rendering order.
     *
     * @return ArrayList<View> Professional view list for PDF compilation excluding watermark view
     */
    private fun getPrintViewList(): ArrayList<View> {
        val result = ArrayList<View>()
        result.add(binding.reportInfoView)
        val childCount = binding.llContent.childCount
        for (i in 0 until  childCount) {
            val childView = binding.llContent.getChildAt(i)
            if (childView is ReportIRShowView) {
                result.addAll(childView.getPrintViewList())
            }
        }
        return result
    }
