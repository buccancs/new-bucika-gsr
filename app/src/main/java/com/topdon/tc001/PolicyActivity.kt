package com.topdon.tc001

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.elvishew.xlog.XLog
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseViewModelActivity
import com.topdon.tc001.databinding.ActivityPolicyBinding
import com.topdon.tc001.viewmodel.PolicyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Policy and agreement activity for displaying various legal documents.
 * 
 * Types:
 * - 1: User Service Agreement
 * - 2: Privacy Policy  
 * - 3: Third Party Components
 *
 * Falls back to default local documents when network request fails.
 * 
 * @author BucikaGSR Development Team
 * @since 1.0.0
 */
@Route(path = RouterConfig.POLICY)
class PolicyActivity : BaseViewModelActivity<PolicyViewModel>() {

    private val mHandler = Handler(Looper.getMainLooper())
    private lateinit var binding: ActivityPolicyBinding

    companion object {
        const val KEY_THEME_TYPE = "key_theme_type"
        const val KEY_USE_TYPE = "key_use_type"     //使用类型 用本地和用网络
    }

    private var themeType = 1
    private var themeStr = ""
    private var reloadCount = 1
    private var keyUseType = 0

    override fun providerVMClass() = PolicyViewModel::class.java

    override fun initContentView() = R.layout.activity_policy

    /**
     * Initialize the activity views and set up the policy document loading logic.
     */
    override fun initView() {
        binding = ActivityPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        if (intent.hasExtra(KEY_THEME_TYPE)) {
            themeType = intent.getIntExtra(KEY_THEME_TYPE, 1)
        }
        if (intent.hasExtra(KEY_USE_TYPE)) {
            keyUseType = intent.getIntExtra(KEY_USE_TYPE, 0)
        }
        themeStr = when (themeType) {
            1 -> getString(R.string.user_services_agreement)
            2 -> getString(R.string.privacy_policy)
            3 -> getString(R.string.third_party_components)
            else -> getString(R.string.user_services_agreement)
        }

        binding.titleView.setTitleText(themeStr)
        viewModel.htmlViewData.observe(this) {
            dismissCameraLoading()
            if (it.action == 1) {
                initWeb(it.body ?: "")
            } else {
                loadHttp(binding.policyWeb)
                delayShowWebView()
            }
        }
        if (keyUseType != 0) {
            loadHttpWhenNotInit(binding.policyWeb)
            delayShowWebView()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
    }

    /**
     * Delays showing the WebView to resolve white screen flashing issues.
     */
    private fun delayShowWebView() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(200)
            launch(Dispatchers.Main) {
                binding.policyWeb.visibility = View.VISIBLE
            }
        }
    }

    override fun initData() {
        if (keyUseType == 0) {
            showCameraLoading()
            viewModel.getUrl(themeType)
        }
    }

    /**
     * Initialize WebView with the provided HTML content.
     * 
     * @param url HTML content to load in the WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun initWeb(url: String) {
        binding.policyWeb.visibility = View.INVISIBLE
        val webSettings: WebSettings = binding.policyWeb.settings
        webSettings.javaScriptEnabled = true //设置支持javascript

        binding.policyWeb.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.w("123", "onPageFinished url: $url")
            }
        }

        binding.policyWeb.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (title!!.contains("404") && reloadCount > 0) {
                    loadHttp(view!!)
                    delayShowWebView()
                } else {
                    mHandler.postDelayed({
                        binding.policyWeb.visibility = View.VISIBLE
                    }, 200)
                }
            }

        }

        binding.policyWeb.settings.defaultTextEncodingName = "utf-8"
        binding.policyWeb.loadDataWithBaseURL(null, url, "text/html", "utf-8", null)

    }

    /**
     * Processes rich text HTML content with custom styling.
     *
     * @param htmlBody The HTML body content
     * @param fontColor Font color to apply  
     * @param backgroundColor Background color to apply
     * @return Formatted HTML string with styling
     */
    fun getHtmlData(htmlBody: String, fontColor: String, backgroundColor: String): String {
        val head = "<head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\"> " +
                "<style>img{max-width: 100%; width:100%; height:auto;}video{max-width: 100%; width:100%; height:auto;}*{margin:0px;}body{font-size:16px;color: ${fontColor}; background-color: ${backgroundColor};}</style>" + "</head>"
        return "<html>$head<body>$htmlBody</body></html>"
    }

    override fun httpErrorTip(text: String, requestUrl: String) {
        XLog.w("声明接口异常,打开默认链接")
        loadHttp(binding.policyWeb)
        delayShowWebView()
    }

    /**
     * Load network policy content when not initialized locally.
     * 
     * @param view WebView to load content into
     */
    fun loadHttpWhenNotInit(view: WebView) {
        reloadCount--
        when (themeType) {
            1 -> {
                //用户服务协议
                view.loadUrl("https://plat.topdon.com/topdon-plat/out-user/baseinfo/template/getHtmlContentById?softCode=${BaseApplication.instance.getSoftWareCode()}&language=1&type=21")
            }

            2 -> {
                //隐私政策
                view.loadUrl("https://plat.topdon.com/topdon-plat/out-user/baseinfo/template/getHtmlContentById?softCode=${BaseApplication.instance.getSoftWareCode()}&language=1&type=22")
            }

            3 -> {
                //第三方组件
                view.loadUrl("file:///android_asset/web/third_statement.html")
            }
        }
    }

    /**
     * Load default local policy documents (fallback when network fails).
     * 
     * @param view WebView to load content into
     */
    fun loadHttp(view: WebView) {
        reloadCount--
        when (themeType) {
            1 -> {
                if (BaseApplication.instance.isDomestic()) {
                    view.loadUrl("file:///android_asset/web/services_agreement_default_inside_china.html")
                } else {
                    //用户服务协议
                    view.loadUrl("file:///android_asset/web/services_agreement_default.html")
                }
            }

            2 -> {
                if (BaseApplication.instance.isDomestic()) {
                    view.loadUrl("file:///android_asset/web/privacy_default_inside_china.html")
                } else {
                    //隐私政策
                    view.loadUrl("file:///android_asset/web/privacy_default.html")
                }
            }

            3 -> {
                //第三方组件
                view.loadUrl("file:///android_asset/web/third_statement.html")
            }
        }
    }
}