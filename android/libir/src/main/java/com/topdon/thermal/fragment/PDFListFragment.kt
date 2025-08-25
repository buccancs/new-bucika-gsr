package com.topdon.thermal.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.launcher.ARouter
import com.blankj.utilcode.util.Utils
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseViewModelFragment
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.utils.NetWorkUtils
import com.topdon.libcom.PDFHelp
import com.topdon.libcom.view.CommLoadMoreView
import com.topdon.lms.sdk.Config
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
import com.topdon.thermal.report.viewmodel.PdfViewModel
import com.topdon.thermal.databinding.FragmentPdfListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Professional thermal imaging PDF report management fragment providing comprehensive
 * report viewing and management capabilities for research and clinical applications.
 * 
 * Provides advanced functionality for:
 * - Professional thermal imaging PDF report list management
 * - Industry-standard thermal report download and synchronization
 * - Research-grade PDF document viewing and analysis
 * - Professional thermal report archival and organization
 * - Clinical-grade report sharing and export capabilities
 * - Comprehensive multi-language thermal report support
 * 
 * @author: CaiSongL
 * @date: 2023/5/12 11:34
 */
class PDFListFragment : BaseViewModelFragment<PdfViewModel>() {

    /** ViewBinding instance for type-safe view access */
    private var _binding: FragmentPdfListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPdfListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Device type flag indicating whether current device is TC007 or other plugin-style device.
     * true=TC007, false=other plugin devices
     */
    private var isTC007 = false

    /** Current page number for professional PDF report pagination */
    private var page = 1
    
    /** Professional PDF report adapter with comprehensive management features */
    private var reportAdapter = PDFAdapter(R.layout.item_pdf)

    /**
     * LMS login and logout broadcast receiver for session management.
     */
    private val loginBroadcastReceiver = LoginBroadcastReceiver()

    override fun providerVMClass() = PdfViewModel::class.java

    override fun initContentView(): Int {
        return R.layout.fragment_pdf_list
    }

    override fun initView() {
        isTC007 = arguments?.getBoolean(ExtraKeyConfig.IS_TC007, false) ?: false

        val intentFilter = IntentFilter()
        intentFilter.addAction(Config.ACTION_BROADCAST_LOGIN)
        intentFilter.addAction(Config.ACTION_BROADCAST_LOGOFF)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(loginBroadcastReceiver, intentFilter)

        initRecycler()

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
                val tvEmpty: TextView? = reportAdapter.emptyLayout?.findViewById(R.id.tv_empty)
                tvEmpty?.setText(if (page == 1 && data.code != LMS.SUCCESS) R.string.request_fail else R.string.tip_no_more_data)

                if (page == 1) {
                    //刷新
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
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                if (WebSocketProxy.getInstance().isConnected()) {
                    NetWorkUtils.switchNetwork(false)
                }else{
                    NetWorkUtils.connectivityManager.bindProcessToNetwork(null)
                }
                if (!hasLoadData) {
                    hasLoadData = true
                    binding.fragmentPdfRecyclerLay.autoRefresh()
                }
            }
        })
    }

    /**
     * 是否已调用过加载初始数据
     */
    private var hasLoadData = false

    override fun initData() {

    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(loginBroadcastReceiver)
    }

    private inner class LoginBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Config.ACTION_BROADCAST_LOGIN, Config.ACTION_BROADCAST_LOGOFF -> {
                    hasLoadData = true
                    page = 1
                    viewModel.getReportData(isTC007, page)
                }
            }
        }
    }

    /**
     * Initialize professional PDF report RecyclerView with comprehensive management capabilities.
     * 
     * Provides advanced functionality for:
     * - Research-grade PDF report list display and navigation
     * - Professional report deletion with confirmation workflows
     * - Industry-standard report detail viewing and analysis
     * - Clinical-grade pagination and load more functionality
     * - Comprehensive report synchronization and refresh capabilities
     */
    private fun initRecycler() {
        reportAdapter.isUseEmpty = true
        reportAdapter.delListener = {item, position ->
            val reportBean = item.reportContent
            TipDialog.Builder(requireContext())
                .setMessage(getString(R.string.tip_config_delete, reportBean?.report_info?.report_name ?: ""))
                .setPositiveListener(R.string.app_confirm) {
                    lifecycleScope.launch {
                        showLoadingDialog()
                        withContext(Dispatchers.IO){
                            val url = UrlConstant.BASE_URL + "api/v1/outProduce/testReport/delTestReport"
                            val params = RequestParams()
                            params.addBodyParameter("modelId", if (isTC007) 1783 else 950) //TC001-950, TC002-951, TC003-952 TC007-1783
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
                                    Log.w("删除成功",response.toString())
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
        reportAdapter.jumpDetailListener = {item, position ->
            ARouter.getInstance().build(RouterConfig.REPORT_DETAIL)
                .withParcelable(ExtraKeyConfig.REPORT_BEAN,reportAdapter.data[position]?.reportContent)
                .navigation(requireContext())
        }
        reportAdapter.loadMoreModule.loadMoreView = CommLoadMoreView()
        reportAdapter.loadMoreModule.setOnLoadMoreListener {
            //加载更多
            viewModel.getReportData(isTC007, ++page)
        }

        binding.fragmentPdfRecycler.adapter = reportAdapter
        binding.fragmentPdfRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.fragmentPdfRecyclerLay.setOnRefreshListener {
            //刷新
            page = 1
            viewModel.getReportData(isTC007, page)
        }

        binding.fragmentPdfRecyclerLay.setEnableLoadMore(false)
    }
}