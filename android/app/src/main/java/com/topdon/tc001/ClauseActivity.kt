package com.topdon.tc001

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.dialog.TipProgressDialog
import com.topdon.lib.core.utils.CommUtils
import com.topdon.lms.sdk.utils.NetworkUtil
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.tc001.app.App
import com.topdon.tc001.databinding.ActivityClauseBinding
import com.topdon.tc001.utils.VersionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * Clause Agreement Activity
 * 
 * This activity displays the terms of service, privacy policy, and third-party component agreements
 * for the BucikaGSR application. It provides a comprehensive legal framework for user consent
 * before allowing access to the main application functionality.
 * 
 * Key Features:
 * - Terms of service display and navigation
 * - Privacy policy acknowledgment
 * - Third-party component licensing information
 * - User consent management with agree/disagree options
 * - App initialization process after consent
 * - Network connectivity validation for policy viewing
 * - Localization support for domestic and international users
 * 
 * ViewBinding Integration:
 * - Modern type-safe view access using ActivityClauseBinding
 * - Eliminates synthetic view imports for better compile-time safety
 * - Follows Android development best practices
 * 
 * @author BucikaGSR Development Team
 * @since 1.0.0
 * @see PolicyActivity for detailed policy viewing
 * @see MainActivity for main application entry point
 */
@Route(path = RouterConfig.CLAUSE)
class ClauseActivity : AppCompatActivity() {

    /**
     * ViewBinding instance for type-safe view access
     * Provides compile-time verified access to all views in activity_clause.xml
     */
    private lateinit var binding: ActivityClauseBinding

    /**
     * Progress dialog for displaying loading state during app initialization
     * Shows user feedback while background initialization processes complete
     */
    private lateinit var dialog: TipProgressDialog
    /**
     * Initializes the activity and sets up the clause agreement interface
     * 
     * @param savedInstanceState Previously saved instance state, or null for first creation
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClauseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    /**
     * Initializes all view components and sets up user interaction handlers
     * 
     * Sets up:
     * - Progress dialog for loading states
     * - Copyright year display
     * - Agree/disagree button click handlers
     * - Policy navigation links
     * - App name and version display
     * - Localization-specific content
     */
    private fun initView() {
        dialog = TipProgressDialog.Builder(this)
            .setMessage(com.topdon.lib.core.R.string.tip_loading)
            .setCanceleable(false)
            .create()

        val year = Calendar.getInstance().get(Calendar.YEAR)
        binding.clauseYearTxt.text = getString(R.string.version_year, "2023-$year")

        binding.clauseAgreeBtn.setOnClickListener {
            confirmInitApp()
        }
        
        binding.clauseDisagreeBtn.setOnClickListener {
            //再次弹框确认是否退出
            TipDialog.Builder(this)
                .setMessage(getString(R.string.privacy_tips))
                .setPositiveListener(R.string.privacy_confirm) {
                    confirmInitApp()
                }
                .setCancelListener(R.string.privacy_cancel) {
                    this.finish()
                }
                .setCanceled(true)
                .create().show()
        }
        
        val keyUseType = if (BaseApplication.instance.isDomestic()) 1 else 0
        
        binding.clauseItem.setOnClickListener {
            if (!NetworkUtil.isConnected(this)) {
                TToast.shortToast(this, R.string.lms_setting_http_error)
            } else {
                //服务条款
                ARouter.getInstance()
                    .build(RouterConfig.POLICY)
                    .withInt(PolicyActivity.KEY_THEME_TYPE, 1)
                    .withInt(PolicyActivity.KEY_USE_TYPE, keyUseType)
                    .navigation(this)
            }
        }
        
        binding.clauseItem2.setOnClickListener {
            if (!NetworkUtil.isConnected(this)) {
                TToast.shortToast(this, R.string.lms_setting_http_error)
            } else {
                //隐私条款
                ARouter.getInstance()
                    .build(RouterConfig.POLICY)
                    .withInt(PolicyActivity.KEY_THEME_TYPE, 2)
                    .withInt(PolicyActivity.KEY_USE_TYPE, keyUseType)
                    .navigation(this)
            }
        }
        
        binding.clauseItem3.setOnClickListener {
            //第三方
            if (!NetworkUtil.isConnected(this)) {
                TToast.shortToast(this, R.string.lms_setting_http_error)
            } else {
                ARouter.getInstance()
                    .build(RouterConfig.POLICY)
                    .withInt(PolicyActivity.KEY_THEME_TYPE, 3)
                    .withInt(PolicyActivity.KEY_USE_TYPE, keyUseType)
                    .navigation(this)
            }
        }

        if (BaseApplication.instance.isDomestic()) {
            binding.tvPrivacy.text = "    ${getString(R.string.privacy_agreement_tips_new, CommUtils.getAppName())}"
            binding.tvPrivacy.visibility = View.VISIBLE
            binding.tvPrivacy.movementMethod = ScrollingMovementMethod.getInstance()
        }
        
        binding.tvWelcome.text = getString(R.string.welcome_use_app, CommUtils.getAppName())
        binding.tvVersion.text = "${getString(R.string.set_version)}V${VersionUtils.getCodeStr(this)}"
        binding.clauseName.text = CommUtils.getAppName()
    }

    /**
     * Handles user confirmation to initialize the application
     * 
     * Performs background initialization and navigates to main activity upon completion.
     * Shows loading dialog during the process and manages shared preferences to track
     * clause acceptance.
     * 
     * Process:
     * 1. Show loading dialog
     * 2. Initialize App with delayed initialization
     * 3. Wait for initialization completion (1000ms)
     * 4. Navigate to main activity
     * 5. Mark clause as shown in preferences
     * 6. Dismiss loading dialog and finish activity
     */
    private fun confirmInitApp() {
        lifecycleScope.launch {
            showLoading()
            //初始化
            App.delayInit()
            async(Dispatchers.IO) {
                //等待1000ms 初始化结束
                delay(1000)
                return@async
            }.await().let {
                ARouter.getInstance().build(RouterConfig.MAIN).navigation(this@ClauseActivity)
                SharedManager.setHasShowClause(true)
                dismissLoading()
                finish()
            }
        }
    }

    /**
     * Shows the loading progress dialog
     * Provides user feedback during app initialization process
     */
    private fun showLoading() {
        dialog.show()
    }

    /**
     * Dismisses the loading progress dialog
     * Called when initialization process completes
     */
    private fun dismissLoading() {
        dialog.dismiss()
    }
