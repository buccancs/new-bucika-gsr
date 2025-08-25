package com.topdon.tc001

import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.CheckDoubleClick
import com.topdon.lib.core.utils.CommUtils
import com.topdon.lms.sdk.LMS
import com.topdon.lms.sdk.UrlConstant
import com.topdon.tc001.databinding.ActivityVersionBinding
import com.topdon.tc001.utils.AppVersionUtil
import com.topdon.tc001.utils.VersionUtils
import java.util.*

/**
 * Version Information Activity
 * 
 * This activity displays comprehensive version information for the BucikaGSR application,
 * including app version, copyright information, and legal document access.
 * 
 * Key Features:
 * - Current application version display
 * - Copyright and company information
 * - Direct access to legal documents (privacy policy, terms, etc.)
 * - App version checking and update notifications
 * - Debug environment switching capability
 * - Dynamic content based on domestic/international deployment
 * 
 * ViewBinding Integration:
 * - Modern type-safe view access using ActivityVersionBinding
 * - Eliminates synthetic view imports for better compile-time safety
 * - Follows Android development best practices
 * 
 * Legal Document Access:
 * - User Services Agreement
 * - Privacy Policy
 * - Third-party Components Licensing
 * - All documents accessible via PolicyActivity navigation
 * 
 * Version Management:
 * - Automatic version checking for domestic builds
 * - Visual indicators for available updates
 * - Integration with AppVersionUtil for update management
 * 
 * @author BucikaGSR Development Team
 * @since 1.0.0
 * @see PolicyActivity for legal document viewing
 * @see AppVersionUtil for version management
 */
@Route(path = RouterConfig.VERSION)
class VersionActivity : BaseActivity(), View.OnClickListener {

    /**
     * ViewBinding instance for type-safe view access
     * Provides compile-time verified access to all views in activity_version.xml
     */
    private lateinit var binding: ActivityVersionBinding

    /**
     * Version utility for checking app updates
     * Manages version checking and update notification display
     */
    private var appVersionUtil: AppVersionUtil? = null
    /**
     * Initializes the content view using ViewBinding
     * 
     * @return The layout resource ID for the activity
     */
    override fun initContentView() = R.layout.activity_version

    /**
     * Initializes view components and sets up user interactions
     * 
     * Sets up:
     * - Current version display with app name and version code
     * - Copyright year information
     * - Click listeners for legal document access
     * - Debug environment switching (double-click on app icon)
     * - Version check button functionality
     */
    override fun initView() {
        binding = ActivityVersionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.versionCodeText.text = "${getString(R.string.set_version)}V${VersionUtils.getCodeStr(this)}"
        val year = Calendar.getInstance().get(Calendar.YEAR)
        binding.versionYearTxt.text = getString(R.string.version_year, "2023-$year")
        
        binding.versionStatementPrivateTxt.setOnClickListener(this)
        binding.versionStatementPolicyTxt.setOnClickListener(this)
        binding.versionStatementCopyrightTxt.setOnClickListener(this)

        binding.settingVersionImg.setOnClickListener {
            if (BuildConfig.DEBUG && CheckDoubleClick.isFastDoubleClick()) {
                LMS.getInstance().activityEnv()
            }
        }
        
        binding.clNewVersion.setOnClickListener {
            if (!CheckDoubleClick.isFastDoubleClick()) {
                checkAppVersion(true)
            }
        }
        
        binding.settingVersionTxt.text = CommUtils.getAppName()
    }

    /**
     * Initializes data and performs version checking for domestic builds
     * 
     * For domestic deployments, automatically checks for app updates
     * and displays update notification if available.
     */
    override fun initData() {
        if (BaseApplication.instance.isDomestic()) {
            checkAppVersion(false)
        }
    }

    /**
     * Called when the activity resumes
     * Sets the base host URL for network operations
     */
    override fun onResume() {
        super.onResume()
        SharedManager.setBaseHost(UrlConstant.BASE_URL)
    }

    /**
     * Handles click events for legal document navigation
     * 
     * @param v The view that was clicked
     */
    override fun onClick(v: View?) {
        when (v) {
            binding.versionStatementPrivateTxt -> {
                ARouter.getInstance().build(RouterConfig.POLICY)
                    .withInt(PolicyActivity.KEY_THEME_TYPE, 1)
                    .navigation(this)
            }
            binding.versionStatementPolicyTxt -> {
                ARouter.getInstance().build(RouterConfig.POLICY)
                    .withInt(PolicyActivity.KEY_THEME_TYPE, 2)
                    .navigation(this)
            }
            binding.versionStatementCopyrightTxt -> {
                ARouter.getInstance().build(RouterConfig.POLICY)
                    .withInt(PolicyActivity.KEY_THEME_TYPE, 3)
                    .navigation(this)
            }
        }
    }

    /**
     * Checks for application version updates
     * 
     * Initializes the AppVersionUtil if not already created and performs
     * version checking. Updates the UI to show available version information
     * and update notifications.
     * 
     * @param isShow Whether to show the version check results immediately
     */
    private fun checkAppVersion(isShow: Boolean) {
        if (appVersionUtil == null) {
            appVersionUtil = AppVersionUtil(this, object : AppVersionUtil.DotIsShowListener {
                override fun isShow(show: Boolean) {
                    binding.clNewVersion.visibility = View.VISIBLE
                }

                override fun version(version: String) {
                    binding.tvNewVersion.text = version
                }
            })
        }
        appVersionUtil?.checkVersion(isShow)
    }

}