package com.topdon.thermal.report.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.topdon.thermal.R
import com.topdon.thermal.report.bean.ReportConditionBean
import com.topdon.thermal.report.bean.ReportInfoBean
import com.topdon.thermal.databinding.ViewReportInfoBinding

class ReportInfoView: LinearLayout {

    private val binding: ViewReportInfoBinding

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        binding = ViewReportInfoBinding.inflate(
            LayoutInflater.from(context), this, true
        )
    }

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

    fun refreshCondition(conditionBean: ReportConditionBean?) {
        with(binding) {

            clReportCondition.isVisible = conditionBean?.is_ambient_humidity == 1
                    || conditionBean?.is_ambient_temperature == 1
                    || conditionBean?.is_test_distance == 1
                    || conditionBean?.is_emissivity == 1

            groupAmbientTemperature.isVisible = conditionBean?.is_ambient_temperature == 1
            tvAmbientTemperature.text = conditionBean?.ambient_temperature
            viewLine1.isVisible = conditionBean?.is_ambient_temperature == 1 &&
                    (conditionBean.is_ambient_humidity == 1 || conditionBean.is_test_distance == 1 || conditionBean.is_emissivity == 1)

            groupAmbientHumidity.isVisible = conditionBean?.is_ambient_humidity == 1
            tvAmbientHumidity.text = conditionBean?.ambient_humidity
            viewLine2.isVisible = conditionBean?.is_ambient_humidity == 1 && (conditionBean.is_test_distance == 1 || conditionBean.is_emissivity == 1)

            groupTestDistance.isVisible = conditionBean?.is_test_distance == 1
            tvTestDistance.text = conditionBean?.test_distance
            viewLine3.isVisible = conditionBean?.is_test_distance == 1 && conditionBean.is_emissivity == 1

            groupEmissivity.isVisible = conditionBean?.is_emissivity == 1
            tvEmissivity.text = conditionBean?.emissivity
        }
    }

    fun getPrintViewList(): ArrayList<View> {
        val result = ArrayList<View>()
        with(binding) {
            result.add(clTop)
            result.add(clReportCondition)
        }
        return result
    }
