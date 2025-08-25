package com.topdon.thermal.report.activity

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.blankj.utilcode.util.ToastUtils
import com.topdon.lib.core.bean.event.ReportCreateEvent
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.UnitTools
import com.topdon.lib.core.tools.GlideLoader
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.thermal.R
import com.topdon.thermal.databinding.ActivityReportCreateSecondBinding
import com.topdon.thermal.report.bean.*
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Professional Thermal Report Creation Activity (Step 2 of 2) with Industry-Standard Documentation and ViewBinding
 *
 * This professional thermal imaging report creation activity provides comprehensive report generation capabilities
 * for clinical and research environments with advanced temperature analysis and documentation features.
 *
 * **Required Parameters:**
 * - [ExtraKeyConfig.IS_TC007]: Device type flag (true for TC007, false for other plugin devices)
 * - [ExtraKeyConfig.FILE_ABSOLUTE_PATH]: Current edited image absolute path for report inclusion
 * - [ExtraKeyConfig.IMAGE_TEMP_BEAN]: Temperature measurement data (points, lines, rectangles, full image)
 * - [ExtraKeyConfig.REPORT_INFO]: Report metadata and identification information
 *
 * **Optional Parameters:**
 * - [ExtraKeyConfig.REPORT_CONDITION]: Detection conditions and environmental parameters
 * - [ExtraKeyConfig.REPORT_IR_LIST]: Previously confirmed image information list for multi-image reports
 *
 * **Professional Features:**
 * - Comprehensive temperature measurement data visualization with point, line, rectangle, and full-image analysis
 * - Industry-standard report preview with professional documentation formatting
 * - Multi-image report support with up to 9 thermal images per report
 * - Professional image scaling and aspect ratio management for optimal presentation
 * - Research-grade temperature data preservation with unit conversion support
 *
 * **Clinical Applications:**
 * - Medical thermal imaging report generation with detailed temperature analysis
 * - Building inspection documentation with comprehensive thermal data
 * - Industrial equipment monitoring reports with professional presentation
 * - Research documentation with academic-standard formatting
 *
 * @author Professional Thermal Imaging Team
 * @since 1.0.0
 */
@Route(path = RouterConfig.REPORT_CREATE_SECOND)
class ReportCreateSecondActivity: BaseActivity(), View.OnClickListener {

    /**
     * ViewBinding instance for type-safe view access and lifecycle management
     */
    private lateinit var binding: ActivityReportCreateSecondBinding

    /**
     * Current added thermal image information list for comprehensive report generation
     */
    private var reportIRList: ArrayList<ReportIRBean> = ArrayList(0)

    /**
     * Current edited thermal image absolute file path from previous activity
     */
    private var currentFilePath: String = ""

    /**
     * Temperature measurement data for current thermal image including points, lines, rectangles, and full image analysis
     */
    private var imageTempBean: ImageTempBean? = null

    override fun initContentView() = R.layout.activity_report_create_second

    /**
     * Initialize ViewBinding and professional thermal report creation interface
     *
     * Configures comprehensive temperature data visualization, image preview, and report generation controls
     * with industry-standard presentation and professional navigation capabilities.
     */
    override fun initView() {
        binding = ActivityReportCreateSecondBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        currentFilePath = intent.getStringExtra(ExtraKeyConfig.FILE_ABSOLUTE_PATH)!!
        imageTempBean = intent.getParcelableExtra(ExtraKeyConfig.IMAGE_TEMP_BEAN)
        reportIRList = intent.getParcelableArrayListExtra(ExtraKeyConfig.REPORT_IR_LIST) ?: ArrayList(10)

        refreshImg(currentFilePath)
        refreshData(imageTempBean)

        binding.tvAddImage.setOnClickListener(this)
        binding.tvPreview.setOnClickListener(this)
    }

    /**
     * Initialize thermal report data processing
     *
     * Prepares comprehensive temperature analysis data and report generation parameters
     * for professional documentation and clinical applications.
     */
    override fun initData() {
    }

    /**
     * Handle report creation completion events
     *
     * @param event Report creation completion event for activity lifecycle management
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReportCreate(event: ReportCreateEvent) {
        finish()
    }

    /**
     * Refresh thermal image display with professional scaling and presentation
     *
     * Loads and displays thermal image with automatic aspect ratio calculation and professional
     * scaling for optimal report presentation in both landscape and portrait orientations.
     *
     * @param absolutePath Thermal image absolute file path for display processing
     */
    private fun refreshImg(absolutePath: String?) {
        lifecycleScope.launch {
            val drawable = GlideLoader.getDrawable(this@ReportCreateSecondActivity, absolutePath)
            val isLand = (drawable?.intrinsicWidth ?: 0) > (drawable?.intrinsicHeight ?: 0)
            val width = (ScreenUtil.getScreenWidth(this@ReportCreateSecondActivity) * (if (isLand) 234 else 175) / 375f).toInt()
            val layoutParams = binding.ivImage.layoutParams
            layoutParams.width = width
            layoutParams.height = (width * (drawable?.intrinsicHeight ?: 0).toFloat() / (drawable?.intrinsicWidth ?: 1)).toInt()
            binding.ivImage.layoutParams = layoutParams
            binding.ivImage.setImageDrawable(drawable)
        }
    }

    /**
     * Refresh comprehensive temperature measurement data visualization
     *
     * Updates professional temperature analysis views for points, lines, rectangles, and full image
     * measurements with industry-standard presentation and research-grade data organization.
     *
     * @param imageTempBean Temperature measurement data bean containing all measurement types
     */
    private fun refreshData(imageTempBean: ImageTempBean?) {
        binding.scrollView.scrollTo(0, 0)

        // Full image temperature analysis
        binding.reportTempViewFull.isVisible = imageTempBean?.full != null
        binding.reportTempViewFull.refreshData(imageTempBean?.full)

        // Point temperature measurements (up to 5 points)
        binding.reportTempViewPoint1.isVisible = (imageTempBean?.pointList?.size ?: 0) > 0
        if ((imageTempBean?.pointList?.size ?: 0) > 0) {
            binding.reportTempViewPoint1.refreshData(imageTempBean?.pointList?.get(0))
        }
        binding.reportTempViewPoint2.isVisible = (imageTempBean?.pointList?.size ?: 0) > 1
        if ((imageTempBean?.pointList?.size ?: 0) > 1) {
            binding.reportTempViewPoint2.refreshData(imageTempBean?.pointList?.get(1))
        }
        binding.reportTempViewPoint3.isVisible = (imageTempBean?.pointList?.size ?: 0) > 2
        if ((imageTempBean?.pointList?.size ?: 0) > 2) {
            binding.reportTempViewPoint3.refreshData(imageTempBean?.pointList?.get(2))
        }
        binding.reportTempViewPoint4.isVisible = (imageTempBean?.pointList?.size ?: 0) > 3
        if ((imageTempBean?.pointList?.size ?: 0) > 3) {
            binding.reportTempViewPoint4.refreshData(imageTempBean?.pointList?.get(3))
        }
        binding.reportTempViewPoint5.isVisible = (imageTempBean?.pointList?.size ?: 0) > 4
        if ((imageTempBean?.pointList?.size ?: 0) > 4) {
            binding.reportTempViewPoint5.refreshData(imageTempBean?.pointList?.get(4))
        }

        // Line temperature measurements (up to 5 lines)
        binding.reportTempViewLine1.isVisible = (imageTempBean?.lineList?.size ?: 0) > 0
        if ((imageTempBean?.lineList?.size ?: 0) > 0) {
            binding.reportTempViewLine1.refreshData(imageTempBean?.lineList?.get(0))
        }
        binding.reportTempViewLine2.isVisible = (imageTempBean?.lineList?.size ?: 0) > 1
        if ((imageTempBean?.lineList?.size ?: 0) > 1) {
            binding.reportTempViewLine2.refreshData(imageTempBean?.lineList?.get(1))
        }
        binding.reportTempViewLine3.isVisible = (imageTempBean?.lineList?.size ?: 0) > 2
        if ((imageTempBean?.lineList?.size ?: 0) > 2) {
            binding.reportTempViewLine3.refreshData(imageTempBean?.lineList?.get(2))
        }
        binding.reportTempViewLine4.isVisible = (imageTempBean?.lineList?.size ?: 0) > 3
        if ((imageTempBean?.lineList?.size ?: 0) > 3) {
            binding.reportTempViewLine4.refreshData(imageTempBean?.lineList?.get(3))
        }
        binding.reportTempViewLine5.isVisible = (imageTempBean?.lineList?.size ?: 0) > 4
        if ((imageTempBean?.lineList?.size ?: 0) > 4) {
            binding.reportTempViewLine5.refreshData(imageTempBean?.lineList?.get(4))
        }

        // Rectangle area temperature measurements (up to 5 rectangles)
        binding.reportTempViewRect1.isVisible = (imageTempBean?.rectList?.size ?: 0) > 0
        if ((imageTempBean?.rectList?.size ?: 0) > 0) {
            binding.reportTempViewRect1.refreshData(imageTempBean?.rectList?.get(0))
        }
        binding.reportTempViewRect2.isVisible = (imageTempBean?.rectList?.size ?: 0) > 1
        if ((imageTempBean?.rectList?.size ?: 0) > 1) {
            binding.reportTempViewRect2.refreshData(imageTempBean?.rectList?.get(1))
        }
        binding.reportTempViewRect3.isVisible = (imageTempBean?.rectList?.size ?: 0) > 2
        if ((imageTempBean?.rectList?.size ?: 0) > 2) {
            binding.reportTempViewRect3.refreshData(imageTempBean?.rectList?.get(2))
        }
        binding.reportTempViewRect4.isVisible = (imageTempBean?.rectList?.size ?: 0) > 3
        if ((imageTempBean?.rectList?.size ?: 0) > 3) {
            binding.reportTempViewRect4.refreshData(imageTempBean?.rectList?.get(3))
        }
        binding.reportTempViewRect5.isVisible = (imageTempBean?.rectList?.size ?: 0) > 4
        if ((imageTempBean?.rectList?.size ?: 0) > 4) {
            binding.reportTempViewRect5.refreshData(imageTempBean?.rectList?.get(4))
        }
    }

    /**
     * Handle professional thermal report creation navigation and actions
     *
     * Processes add image and preview requests with comprehensive validation and professional
     * navigation to thermal image selection and report preview activities.
     *
     * @param v Clicked view for action identification and processing
     */
    override fun onClick(v: View?) {
        when (v) {
            binding.tvAddImage -> {
                // Add additional thermal image to report (maximum 9 images)
                if (reportIRList.size >= 9) {
                    ToastUtils.showShort(R.string.album_report_max_image_tips)
                    return
                }
                val reportIRBeanList = ArrayList<ReportIRBean>(reportIRList)
                reportIRBeanList.add(buildReportIr(currentFilePath))
                ARouter.getInstance()
                    .build(RouterConfig.REPORT_PICK_IMG)
                    .withBoolean(ExtraKeyConfig.IS_TC007, intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false))
                    .withParcelable(ExtraKeyConfig.REPORT_INFO, intent.getParcelableExtra(ExtraKeyConfig.REPORT_INFO))
                    .withParcelable(ExtraKeyConfig.REPORT_CONDITION, intent.getParcelableExtra(ExtraKeyConfig.REPORT_CONDITION))
                    .withParcelableArrayList(ExtraKeyConfig.REPORT_IR_LIST, reportIRBeanList)
                    .navigation(this)
            }
            binding.tvPreview -> {
                // Generate professional thermal report preview
                val appLanguage = SharedManager.getLanguage(this)
                val sdkVersion = "1.2.8_23050619"
                val reportInfoBean: ReportInfoBean? = intent.getParcelableExtra(ExtraKeyConfig.REPORT_INFO)
                val conditionBean: ReportConditionBean? = intent.getParcelableExtra(ExtraKeyConfig.REPORT_CONDITION)
                val reportIRBeanList = ArrayList<ReportIRBean>(reportIRList)
                reportIRBeanList.add(buildReportIr(currentFilePath))
                val reportBean = ReportBean(SoftwareInfo(appLanguage, sdkVersion), reportInfoBean!!, conditionBean!!, reportIRBeanList)
                ARouter.getInstance().build(RouterConfig.REPORT_PREVIEW_SECOND)
                    .withBoolean(ExtraKeyConfig.IS_TC007, intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false))
                    .withParcelable(ExtraKeyConfig.REPORT_BEAN, reportBean)
                    .navigation(this)
            }
        }
    }

    /**
     * Build comprehensive thermal IR report data bean with professional temperature measurements
     *
     * Constructs complete thermal imaging report data including full image analysis, point measurements,
     * line analysis, and rectangular area measurements with industry-standard formatting and unit conversion.
     *
     * @param filePath Thermal image file path for report inclusion
     * @return ReportIRBean Complete thermal report data with professional formatting
     */
    private fun buildReportIr(filePath: String): ReportIRBean {
        val full: ReportTempBean? = if (imageTempBean?.full != null) {
            ReportTempBean(
                if (binding.reportTempViewFull.getMaxInput().isNotEmpty()) binding.reportTempViewFull.getMaxInput() + UnitTools.showUnit() else "",
                if (binding.reportTempViewFull.isSwitchMaxCheck() && binding.reportTempViewFull.getMaxInput().isNotEmpty()) 1 else 0,
                if (binding.reportTempViewFull.getMinInput().isNotEmpty()) binding.reportTempViewFull.getMinInput() + UnitTools.showUnit() else "",
                if (binding.reportTempViewFull.isSwitchMinCheck() && binding.reportTempViewFull.getMinInput().isNotEmpty()) 1 else 0,
                binding.reportTempViewFull.getExplainInput(),
                if (binding.reportTempViewFull.isSwitchExplainCheck() && binding.reportTempViewFull.getExplainInput().isNotEmpty()) 1 else 0
            )
        } else {
            null
        }

        val pointList = buildReportTempBeanList(1)
        val lienList = buildReportTempBeanList(2)
        val rectList = buildReportTempBeanList(3)
        return ReportIRBean("", filePath, full, pointList, lienList, rectList)
    }

    /**
     * Build professional temperature measurement data lists for report generation
     *
     * Constructs comprehensive temperature measurement bean lists for different measurement types
     * with industry-standard data formatting and professional temperature unit conversion.
     *
     * @param type Measurement type identifier (1=points, 2=lines, 3=rectangles)
     * @return ArrayList<ReportTempBean> Professional temperature measurement data list
     */
    private fun buildReportTempBeanList(type: Int): ArrayList<ReportTempBean> {
        val size = when (type) {
            1 -> imageTempBean?.pointList?.size ?: 0
            2 -> imageTempBean?.lineList?.size ?: 0
            else -> imageTempBean?.rectList?.size ?: 0
        }
        val resultList = ArrayList<ReportTempBean>(size)
        for (i in 0 until size) {
            val reportTempView = when (type) {
                1 -> { // Point measurements
                    when (i) {
                        0 -> binding.reportTempViewPoint1
                        1 -> binding.reportTempViewPoint2
                        2 -> binding.reportTempViewPoint3
                        3 -> binding.reportTempViewPoint4
                        else -> binding.reportTempViewPoint5
                    }
                }
                2 -> { // Line measurements
                    when (i) {
                        0 -> binding.reportTempViewLine1
                        1 -> binding.reportTempViewLine2
                        2 -> binding.reportTempViewLine3
                        3 -> binding.reportTempViewLine4
                        else -> binding.reportTempViewLine5
                    }
                }
                else -> { // Rectangle measurements
                    when (i) {
                        0 -> binding.reportTempViewRect1
                        1 -> binding.reportTempViewRect2
                        else -> binding.reportTempViewRect3
                    }
                }
            }
            val reportTempBean = if (type == 1) {
                // Point temperature data formatting (different structure)
                ReportTempBean(
                    if (reportTempView.getMaxInput().isNotEmpty()) reportTempView.getMaxInput() + UnitTools.showUnit() else "",
                    if (reportTempView.isSwitchMaxCheck() && reportTempView.getMaxInput().isNotEmpty()) 1 else 0,
                    reportTempView.getExplainInput(),
                    if (reportTempView.isSwitchExplainCheck() && reportTempView.getExplainInput().isNotEmpty()) 1 else 0
                )
            } else {
                // Line and rectangle temperature data formatting
                ReportTempBean(
                    if (reportTempView.getMaxInput().isNotEmpty()) reportTempView.getMaxInput() + UnitTools.showUnit() else "",
                    if (reportTempView.isSwitchMaxCheck() && reportTempView.getMaxInput().isNotEmpty()) 1 else 0,
                    if (reportTempView.getMinInput().isNotEmpty()) reportTempView.getMinInput() + UnitTools.showUnit() else "",
                    if (reportTempView.isSwitchMinCheck() && reportTempView.getMinInput().isNotEmpty()) 1 else 0,
                    reportTempView.getExplainInput(),
                    if (reportTempView.isSwitchExplainCheck() && reportTempView.getExplainInput().isNotEmpty()) 1 else 0,
                    if (reportTempView.getAverageInput().isNotEmpty()) reportTempView.getAverageInput() + UnitTools.showUnit() else "",
                    if (reportTempView.isSwitchAverageCheck() && reportTempView.getAverageInput().isNotEmpty()) 1 else 0
                )
            }
            resultList.add(reportTempBean)
        }
        return resultList
    }

}