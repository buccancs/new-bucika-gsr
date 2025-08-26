package com.topdon.thermal.report.activity

import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.blankj.utilcode.util.Utils
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseViewModelActivity
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.utils.NetWorkUtils
import com.topdon.libcom.PDFHelp
import com.topdon.libcom.view.CommLoadMoreView
import com.topdon.lms.sdk.LMS
import com.topdon.lms.sdk.UrlConstant
import com.topdon.lms.sdk.network.HttpProxy
import com.topdon.lms.sdk.network.IResponseCallback
import com.topdon.lms.sdk.utils.LanguageUtil
import com.topdon.lms.sdk.utils.StringUtils
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.lms.sdk.xutils.http.RequestParams
import com.topdon.thermal.R
import com.topdon.thermal.adapter.PDFAdapter
import com.topdon.thermal.databinding.ActivityPdfListBinding
import com.topdon.thermal.report.viewmodel.PdfViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Route(path = RouterConfig.REPORT_LIST)
class PDFListActivity : BaseViewModelActivity<PdfViewModel>() {

    private lateinit var binding: ActivityPdfListBinding

    private var isTC007 = false

    var page = 1

    override fun providerVMClass() = PdfViewModel::class.java

    var reportAdapter = PDFAdapter(R.layout.item_pdf)

    override fun initContentView(): Int {
        return R.layout.activity_pdf_list
    }

    override fun initView() {
        binding = ActivityPdfListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        viewModel.listData.observe(this) {
            dismissLoadingDialog()
            if (!reportAdapter.hasEmptyView()){
                reportAdapter.setEmptyView(R.layout.layout_empty)
            }
            if (it == null) {
                if (page == 1) {
                    binding.fragmentPdfRecyclerLay.finishRefresh(false)
                } else {
                    reportAdapter.loadMoreModule.loadMoreComplete()
                }
            }
            it?.let {data->
                if (page == 1) {

                    if (data.code == LMS.SUCCESS){
                        reportAdapter.loadMoreModule.isEnableLoadMore = !data.data?.records.isNullOrEmpty()
                        binding.fragmentPdfRecyclerLay.finishRefresh()
                    }else{
                        binding.fragmentPdfRecyclerLay.finishRefresh(false)
                    }
                    reportAdapter.setNewInstance(data.data?.records)
                } else {

                    data.data?.records?.let { it1 -> reportAdapter.addData(it1) }
                    if (data.code == LMS.SUCCESS){
                        if (data.data?.records.isNullOrEmpty()){
                            reportAdapter.loadMoreModule.loadMoreEnd()
                        }else{
                            reportAdapter.loadMoreModule.loadMoreComplete()
                        }
                    }else{
                        reportAdapter.loadMoreModule.loadMoreFail()
                    }
                }
            }
        }
        
        if (WebSocketProxy.getInstance().isConnected()) {
            NetWorkUtils.switchNetwork(false)
        }else{
            NetWorkUtils.connectivityManager.bindProcessToNetwork(null)
        }
        initRecycler()
    }

    override fun initData() {
    }

    private fun initRecycler() {
        binding.fragmentPdfRecycler.layoutManager = LinearLayoutManager(this)
        
        binding.fragmentPdfRecyclerLay.setOnRefreshListener {
            page = 1
            viewModel.getReportData(isTC007, page)
        }
        
        binding.fragmentPdfRecyclerLay.setEnableLoadMore(false)
        reportAdapter.loadMoreModule.loadMoreView = CommLoadMoreView()
        binding.fragmentPdfRecyclerLay.autoRefresh()
        
        reportAdapter.loadMoreModule.setOnLoadMoreListener {
            viewModel.getReportData(isTC007, ++page)
        }
        
        reportAdapter.jumpDetailListener = {item, position ->
            ARouter.getInstance().build(RouterConfig.REPORT_DETAIL)
                .withParcelable(ExtraKeyConfig.REPORT_BEAN,reportAdapter.data[position]?.reportContent)
                .navigation(this)
        }
        
        reportAdapter.isUseEmpty = true
        
        reportAdapter.delListener = {item, position ->
            val reportBean = item.reportContent
            TipDialog.Builder(this)
                .setMessage(getString(R.string.tip_config_delete, reportBean?.report_info?.report_name ?: ""))
                .setPositiveListener(R.string.app_confirm) {
                    lifecycleScope.launch {
                        showLoadingDialog()
                        withContext(Dispatchers.IO){
                            val url = UrlConstant.BASE_URL + "api/v1/outProduce/testReport/delTestReport"
                            val params = RequestParams()

                            params.addBodyParameter("modelId", if (isTC007) 1783 else 950)
                            params.addBodyParameter("testReportIds", arrayOf(item.testReportId))
                            params.addBodyParameter("status", 1)
                            params.addBodyParameter("languageId",  LanguageUtil.getLanguageId(Utils.getApp()))
                            params.addBodyParameter("reportType", 2)
                            HttpProxy.instant.post(url,params, object :
                                IResponseCallback {
                                override fun onResponse(response: String?) {
                                    val reportNumber = item.reportContent?.report_info?.report_number ?: ""
                                    val file = File(FileConfig.getPdfDir() + "/$reportNumber.pdf")
                                    if (file.exists()) {
                                        file.delete()
                                    }
                                    Log.w("Professional deletion successful",response.toString())
                                }

                                override fun onFail(exception: Exception?) {

                                }

                                override fun onFail(failMsg: String?, errorCode: String) {
                                    super.onFail(failMsg, errorCode)
                                    try {
                                        StringUtils.getResString(
                                            LMS.mContext,
                                            if (TextUtils.isEmpty(errorCode)) -500 else errorCode.toInt()
                                        ).let {
                                            TToast.shortToast(LMS.mContext, it)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            })
                        }
                        dismissLoadingDialog()
                        
                        if (item.isShowTitleTime){
                            reportAdapter.remove(item)
                            reportAdapter.setNewInstance(reportAdapter.data)
                            reportAdapter.notifyDataSetChanged()
                        }else{
                            reportAdapter.data.removeAt(position)
                            reportAdapter.notifyItemRemoved(position)
                        }
                    }
                }
                .setCancelListener(R.string.app_cancel) {

                }
                .create().show()
        }

        binding.fragmentPdfRecycler.adapter = reportAdapter
    }
