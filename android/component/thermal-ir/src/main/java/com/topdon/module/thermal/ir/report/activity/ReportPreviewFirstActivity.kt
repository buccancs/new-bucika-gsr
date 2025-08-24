package com.topdon.module.thermal.ir.report.activity

import com.alibaba.android.arouter.facade.annotation.Route
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.databinding.ActivityReportPreviewFirstBinding
import com.topdon.module.thermal.ir.report.bean.ReportInfoBean

/**
 * Report generation Step 1 preview activity with ViewBinding implementation.
 * 
 * Provides professional report preview interface for thermal imaging data analysis
 * with comprehensive watermark support and condition display capabilities.
 * 
 * Required parameters:
 * - **REPORT_INFO**: Report information bean (mandatory)
 * - **REPORT_CONDITION**: Detection conditions (optional)
 * 
 * Features include:
 * - Professional report information display
 * - Optional watermark text configuration  
 * - Research-grade condition parameter visualization
 * - Industry-standard navigation and error handling
 * 
 * @author Topdon Thermal Imaging Team
 * @since 2024-01-01
 * @see ReportInfoBean
 */
@Route(path = RouterConfig.REPORT_PREVIEW_FIRST)
class ReportPreviewFirstActivity: BaseActivity() {

    private lateinit var binding: ActivityReportPreviewFirstBinding

    override fun initContentView() = R.layout.activity_report_preview_first

    override fun initView() {
        binding = ActivityReportPreviewFirstBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupTitleView()
        setupReportContent()
    }

    /**
     * Configure title view with navigation controls.
     */
    private fun setupTitleView() {
        binding.titleView.setLeftDrawable(R.drawable.svg_arrow_left_e8)
        binding.titleView.setLeftClickListener { finish() }
    }

    /**
     * Load and display report information with optional conditions.
     */
    private fun setupReportContent() {
        val reportInfoBean: ReportInfoBean? = intent.getParcelableExtra(ExtraKeyConfig.REPORT_INFO)
        
        binding.reportInfoView.apply {
            refreshInfo(reportInfoBean)
            refreshCondition(intent.getParcelableExtra(ExtraKeyConfig.REPORT_CONDITION))
        }

        // Configure watermark if enabled
        reportInfoBean?.let { bean ->
            if (bean.is_report_watermark == 1) {
                binding.watermarkView.watermarkText = bean.report_watermark
            }
        }
    }

    override fun initData() {
        // No additional data initialization required
    }
}