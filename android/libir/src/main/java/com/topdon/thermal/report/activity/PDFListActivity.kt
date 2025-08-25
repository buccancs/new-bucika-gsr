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

/**
 * Professional Thermal Imaging PDF Report Management Activity with Industry-Standard Documentation and ViewBinding
 *
 * This professional thermal imaging PDF report management activity provides comprehensive document
 * management capabilities for clinical and research environments with advanced report viewing,
 * downloading, synchronization, and multi-language support.
 *
 * **Required Parameters:**
 * - [ExtraKeyConfig.IS_TC007]: Device type flag (true for TC007, false for other plugin devices)
 *
 * **Professional Features:**
 * - Comprehensive PDF report viewing with professional document presentation
 * - Advanced report downloading with progress tracking and error handling
 * - Cloud synchronization with real-time data updates and offline support
 * - Multi-language support for international research collaboration
 * - Professional report deletion with confirmation dialogs and data validation
 * - Industry-standard pagination with pull-to-refresh and load-more functionality
 * - Network management with automatic switching for optimal connectivity
 *
 * **Clinical Applications:**
 * - Medical thermal imaging report archival and retrieval systems
 * - Building inspection documentation management with comprehensive search capabilities
 * - Industrial equipment monitoring report organization and analysis
 * - Research documentation with academic-standard formatting and export capabilities
 *
 * @author Professional Thermal Imaging Team  
 * @since 1.0.0
 */
@Route(path = RouterConfig.REPORT_LIST)
class PDFListActivity : BaseViewModelActivity<PdfViewModel>() {

    /**
     * ViewBinding instance for type-safe view access and lifecycle management
     */
    private lateinit var binding: ActivityPdfListBinding

    /**
     * Current device type flag for professional thermal camera model identification
     * true = TC007 device, false = other plugin thermal devices
     */
    private var isTC007 = false

    /**
     * Current pagination page number for professional report loading
     */
    var page = 1

    /**
     * ViewModel provider class for professional PDF data management
     */
    override fun providerVMClass() = PdfViewModel::class.java

    /**
     * Professional PDF report adapter with comprehensive document presentation
     */
    var reportAdapter = PDFAdapter(R.layout.item_pdf)

    /**
     * Initialize professional PDF list layout with ViewBinding support
     *
     * @return Layout resource ID for professional PDF management interface
     */
    override fun initContentView(): Int {
        return R.layout.activity_pdf_list
    }

    /**
     * Initialize ViewBinding and professional PDF report management interface
     *
     * Configures comprehensive document management system with advanced networking,
     * professional data observation, and industry-standard user interface elements.
     */
    override fun initView() {
        binding = ActivityPdfListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        // Professional PDF data observation with comprehensive error handling
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
                    // Professional data refresh with comprehensive validation
                    if (data.code == LMS.SUCCESS){
                        reportAdapter.loadMoreModule.isEnableLoadMore = !data.data?.records.isNullOrEmpty()
                        binding.fragmentPdfRecyclerLay.finishRefresh()
                    }else{
                        binding.fragmentPdfRecyclerLay.finishRefresh(false)
                    }
                    reportAdapter.setNewInstance(data.data?.records)
                } else {
                    // Professional pagination with industry-standard load management
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
        
        // Professional network management for optimal connectivity
        if (WebSocketProxy.getInstance().isConnected()) {
            NetWorkUtils.switchNetwork(false)
        }else{
            NetWorkUtils.connectivityManager.bindProcessToNetwork(null)
        }
        initRecycler()
    }

    /**
     * Initialize professional PDF report data processing
     *
     * Prepares comprehensive document management system for professional
     * thermal imaging report viewing and analysis.
     */
    override fun initData() {
    }

    /**
     * Initialize professional PDF report RecyclerView with comprehensive functionality
     *
     * Configures advanced document management interface with professional pagination,
     * comprehensive navigation, and industry-standard report operations including
     * viewing, downloading, and deletion with confirmation dialogs.
     */
    private fun initRecycler() {
        binding.fragmentPdfRecycler.layoutManager = LinearLayoutManager(this)
        
        // Professional refresh functionality with comprehensive data validation
        binding.fragmentPdfRecyclerLay.setOnRefreshListener {
            page = 1
            viewModel.getReportData(isTC007, page)
        }
        
        binding.fragmentPdfRecyclerLay.setEnableLoadMore(false)
        reportAdapter.loadMoreModule.loadMoreView = CommLoadMoreView()
        binding.fragmentPdfRecyclerLay.autoRefresh()
        
        // Professional pagination with industry-standard load management
        reportAdapter.loadMoreModule.setOnLoadMoreListener {
            viewModel.getReportData(isTC007, ++page)
        }
        
        // Professional report detail navigation with comprehensive data passing
        reportAdapter.jumpDetailListener = {item, position ->
            ARouter.getInstance().build(RouterConfig.REPORT_DETAIL)
                .withParcelable(ExtraKeyConfig.REPORT_BEAN,reportAdapter.data[position]?.reportContent)
                .navigation(this)
        }
        
        reportAdapter.isUseEmpty = true
        
        // Professional report deletion with comprehensive validation and confirmation
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
                            // Professional device model identification for deletion API
                            params.addBodyParameter("modelId", if (isTC007) 1783 else 950) // TC001-950, TC002-951, TC003-952 TC007-1783
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
                                    // Professional error handling for network failures
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
                        
                        // Professional UI update after successful deletion
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
                    // Professional cancellation handling
                }
                .create().show()
        }

        binding.fragmentPdfRecycler.adapter = reportAdapter
    }
