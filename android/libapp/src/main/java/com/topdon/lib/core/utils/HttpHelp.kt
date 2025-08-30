package com.topdon.lib.core.utils

import com.blankj.utilcode.util.Utils
import com.topdon.lib.core.config.HttpConfig
import com.topdon.lms.network.UrlConstant
import com.topdon.lms.utils.instant
import com.topdon.lms.core.IResponseCallback
import com.topdon.lms.utils.LanguageUtil
import com.topdon.lms.xutils.http.RequestParams

object HttpHelp{

    fun getFirstReportData(isTC007: Boolean, pageNumber:Int, iResponseCallback : IResponseCallback<String>){
        val url = UrlConstant.BASE_URL + "api/v1/outProduce/testReport/getTestReport"
        val params = RequestParams()
        params.addBodyParameter("modelId", if (isTC007) 1783 else 950)
        params.addBodyParameter("status", 1)
        params.addBodyParameter("reportType", 2)
        params.addBodyParameter("languageId",  LanguageUtil.getLanguageId(Utils.getApp()))
        params.addBodyParameter("current", pageNumber)
        params.addBodyParameter("size", 20)
        instant.post(url, params, iResponseCallback)
    }
}
