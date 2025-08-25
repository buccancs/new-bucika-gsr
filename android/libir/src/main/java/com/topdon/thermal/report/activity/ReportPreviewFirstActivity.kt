package com.topdon.thermal.report.activity

import com.alibaba.android.arouter.facade.annotation.Route
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.thermal.R
import com.topdon.thermal.databinding.ActivityReportPreviewFirstBinding
import com.topdon.thermal.report.bean.ReportInfoBean

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

    private fun setupTitleView() {
        binding.titleView.setLeftDrawable(R.drawable.svg_arrow_left_e8)
        binding.titleView.setLeftClickListener { finish() }
    }

    private fun setupReportContent() {
        val reportInfoBean: ReportInfoBean? = intent.getParcelableExtra(ExtraKeyConfig.REPORT_INFO)
        
        binding.reportInfoView.apply {
            refreshInfo(reportInfoBean)
            refreshCondition(intent.getParcelableExtra(ExtraKeyConfig.REPORT_CONDITION))
        }

        reportInfoBean?.let { bean ->
            if (bean.is_report_watermark == 1) {
                binding.watermarkView.watermarkText = bean.report_watermark
            }
        }
    }

    override fun initData() {

    }
