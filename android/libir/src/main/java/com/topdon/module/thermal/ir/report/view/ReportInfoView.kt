package com.topdon.module.thermal.ir.report.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.report.bean.ReportConditionBean
import com.topdon.module.thermal.ir.report.bean.ReportInfoBean
import com.topdon.module.thermal.ir.databinding.ViewReportInfoBinding

/**
 * Professional thermal imaging report information view component for clinical and research applications.
 *
 * Provides comprehensive report metadata display including:
 * - Report identification information (name, author, location, date)
 * - Environmental measurement conditions (temperature, humidity)
 * - Technical measurement parameters (test distance, emissivity)
 * - Industry-standard PDF report generation compatibility
 *
 * This component implements professional thermal report information patterns
 * suitable for research documentation and clinical thermal imaging workflows.
 *
 * @constructor Creates thermal report info view with professional metadata display
 * @param context The application context for resource access
 * @param attrs Optional XML attributes for customization
 * @param defStyleAttr Optional default style attributes
 */
class ReportInfoView: LinearLayout {

    /** ViewBinding for the report info layout */
    private val binding: ViewReportInfoBinding

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        binding = ViewReportInfoBinding.inflate(
            LayoutInflater.from(context), this, true
        )
    }

    /**
     * Refreshes professional report information display with metadata.
     *
     * Updates report identification fields including name, author, location, and date
     * with appropriate visibility controls for clinical documentation standards.
     *
     * @param reportInfoBean The report information metadata container
     */
    fun refreshInfo(reportInfoBean: ReportInfoBean?) {
        with(binding) {
            tvReportName.text = reportInfoBean?.report_name

            tvReportAuthor.isVisible = reportInfoBean?.is_report_author == 1
            tvReportAuthor.text = reportInfoBean?.report_author

            groupReportPlace.isVisible = reportInfoBean?.is_report_place == 1
            tvReportPlace.text = reportInfoBean?.report_place

            tvReportDate.isVisible = reportInfoBean?.is_report_date == 1
            tvReportDate.text = reportInfoBean?.report_date
        }
    }

    /**
     * Refreshes professional environmental measurement conditions display.
     *
     * Updates technical measurement parameters including ambient temperature,
     * humidity, test distance, and emissivity with industry-standard formatting
     * suitable for research and clinical thermal imaging documentation.
     *
     * @param conditionBean The environmental measurement conditions data
     */
    fun refreshCondition(conditionBean: ReportConditionBean?) {
        with(binding) {
            // Show condition section if any parameter is enabled
            clReportCondition.isVisible = conditionBean?.is_ambient_humidity == 1
                    || conditionBean?.is_ambient_temperature == 1
                    || conditionBean?.is_test_distance == 1
                    || conditionBean?.is_emissivity == 1

            // Configure ambient temperature display
            groupAmbientTemperature.isVisible = conditionBean?.is_ambient_temperature == 1
            tvAmbientTemperature.text = conditionBean?.ambient_temperature
            viewLine1.isVisible = conditionBean?.is_ambient_temperature == 1 &&
                    (conditionBean.is_ambient_humidity == 1 || conditionBean.is_test_distance == 1 || conditionBean.is_emissivity == 1)

            // Configure ambient humidity display
            groupAmbientHumidity.isVisible = conditionBean?.is_ambient_humidity == 1
            tvAmbientHumidity.text = conditionBean?.ambient_humidity
            viewLine2.isVisible = conditionBean?.is_ambient_humidity == 1 && (conditionBean.is_test_distance == 1 || conditionBean.is_emissivity == 1)

            // Configure test distance display
            groupTestDistance.isVisible = conditionBean?.is_test_distance == 1
            tvTestDistance.text = conditionBean?.test_distance
            viewLine3.isVisible = conditionBean?.is_test_distance == 1 && conditionBean.is_emissivity == 1

            // Configure emissivity display
            groupEmissivity.isVisible = conditionBean?.is_emissivity == 1
            tvEmissivity.text = conditionBean?.emissivity
        }
    }

    /**
     * Retrieves comprehensive list of views for professional PDF thermal report generation.
     *
     * Collects all visible report information views including metadata and environmental
     * conditions for industry-standard documentation output. Supports research-grade
     * thermal analysis with complete measurement parameter traceability.
     *
     * @return ArrayList of views ready for PDF conversion in clinical report format
     */
    fun getPrintViewList(): ArrayList<View> {
        val result = ArrayList<View>()
        with(binding) {
            result.add(clTop)
            result.add(clReportCondition)
        }
        return result
    }
}