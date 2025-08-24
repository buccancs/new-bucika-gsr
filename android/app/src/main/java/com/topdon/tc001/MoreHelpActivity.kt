package com.topdon.tc001
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.text.*
import android.text.style.UnderlineSpan
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.utils.Constants
import com.topdon.tc001.databinding.ActivityMoreHelpBinding

/**
 * More Help Activity
 * 
 * This activity provides comprehensive help and guidance for device connection and troubleshooting
 * in the BucikaGSR application. It offers contextual assistance based on connection type and
 * device state.
 * 
 * Key Features:
 * - Connection guidance for different device types
 * - Wi-Fi configuration assistance and troubleshooting
 * - Context-aware help content based on connection type
 * - Direct access to device Wi-Fi settings
 * - Underlined text styling for interactive elements
 * - Support for Android 10+ Wi-Fi panel integration
 * 
 * ViewBinding Integration:
 * - Modern type-safe view access using ActivityMoreHelpBinding
 * - Eliminates synthetic view imports for better compile-time safety
 * - Follows Android development best practices
 * 
 * Connection Types Supported:
 * - Standard connection mode with step-by-step guidance
 * - Disconnection troubleshooting with settings access
 * - TS004 device-specific connection instructions
 * 
 * Technical Implementation:
 * - WifiManager integration for network state management
 * - Android version-aware Wi-Fi settings access
 * - SpannableStringBuilder for text formatting
 * - Dynamic UI visibility based on connection context
 * 
 * @author BucikaGSR Development Team
 * @since 1.0.0
 * @see BaseActivity for common activity functionality
 * @see Constants for connection type definitions
 */
@Route(path = RouterConfig.IR_MORE_HELP)
class MoreHelpActivity : BaseActivity() {

    /**
     * ViewBinding instance for type-safe view access
     * Provides compile-time verified access to all views in activity_more_help.xml
     */
    private lateinit var binding: ActivityMoreHelpBinding

    /**
     * Connection type identifier determining help content display
     * Uses Constants.SETTING_CONNECTION for standard connection guidance
     */
    private var connectionType: Int = 0
    
    /**
     * Wi-Fi manager for network state management and settings access
     */
    private lateinit var wifiManager: WifiManager
    /**
     * Initializes the content view using ViewBinding
     * 
     * @return The layout resource ID for the activity
     */
    override fun initContentView() = R.layout.activity_more_help

    /**
     * Initializes view components and Wi-Fi manager
     * 
     * Sets up:
     * - Intent parameter processing for connection type
     * - Wi-Fi manager for network operations
     * - Dynamic UI configuration based on connection context
     */
    override fun initView() {
        binding = ActivityMoreHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initIntent()
        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    /**
     * Processes intent parameters and configures UI based on connection type
     * 
     * Configures the interface for either:
     * - Connection guidance mode: Step-by-step device connection help
     * - Disconnection troubleshooting mode: Wi-Fi settings and reconnection assistance
     * 
     * UI adjustments include:
     * - Title and content text updates
     * - Visibility changes for context-specific elements
     * - Interactive element styling (underlined text for settings access)
     */
    private fun initIntent() {
        connectionType = intent.getIntExtra(Constants.SETTING_CONNECTION_TYPE, 0)
        if (connectionType == Constants.SETTING_CONNECTION) {
            binding.tvTitle.text = getString(R.string.ts004_guide_text8)
            binding.titleView.setTitleText(R.string.ts004_guide_text6)
            binding.mainGuideTip1.visibility = View.VISIBLE
            binding.mainGuideTip2.visibility = View.VISIBLE
            binding.mainGuideTip4.visibility = View.VISIBLE
            binding.disconnectTip1.visibility = View.GONE
            binding.disconnectTip2.visibility = View.GONE
            binding.ivTvSetting.visibility = View.GONE
        } else {
            binding.tvTitle.text = getString(R.string.ts004_disconnect_tips1)
            binding.mainGuideTip1.visibility = View.GONE
            binding.mainGuideTip2.visibility = View.GONE
            binding.mainGuideTip4.visibility = View.GONE
            binding.disconnectTip1.visibility = View.VISIBLE
            binding.disconnectTip2.visibility = View.VISIBLE
            binding.ivTvSetting.visibility = View.VISIBLE
            val spannable = SpannableStringBuilder(getString(R.string.ts004_disconnect_tips4))
            spannable.setSpan(
                UnderlineSpan(), 
                0, 
                getString(R.string.ts004_disconnect_tips4).length, 
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.ivTvSetting.text = spannable
        }
    }

    /**
     * Initializes data and sets up click listeners
     * 
     * Configures Wi-Fi settings access button for disconnection troubleshooting mode
     */
    override fun initData() {
        binding.ivTvSetting.setOnClickListener {
            startWifiList()
        }
    }

    /**
     * Launches Wi-Fi settings interface with version-aware implementation
     * 
     * Handles Wi-Fi configuration access for both enabled and disabled Wi-Fi states:
     * - Android 10+ (API 29+): Uses Settings Panel for modern Wi-Fi access
     * - Earlier versions: Direct WifiManager control
     * - Fallback to standard Wi-Fi settings if panel unavailable
     * 
     * For disabled Wi-Fi, presents confirmation dialog before opening settings.
     */
    private fun startWifiList() {
        if (wifiManager.isWifiEnabled) {
            if (Build.VERSION.SDK_INT < 29) { // 低于 Android10
                wifiManager.isWifiEnabled = true
            } else {
                var wifiIntent = Intent(Settings.Panel.ACTION_WIFI)
                if (wifiIntent.resolveActivity(packageManager) == null) {
                    wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    if (wifiIntent.resolveActivity(packageManager) != null) {
                        startActivity(wifiIntent)
                    }
                } else {
                    startActivity(wifiIntent)
                }
            }
        } else {
            TipDialog.Builder(this)
                .setTitleMessage(getString(R.string.app_tip))
                .setMessage(R.string.ts004_wlan_tips)
                .setPositiveListener(R.string.app_open) {
                    if (Build.VERSION.SDK_INT < 29) { // 低于 Android10
                        wifiManager.isWifiEnabled = true
                    } else {
                        var wifiIntent = Intent(Settings.Panel.ACTION_WIFI)
                        if (wifiIntent.resolveActivity(packageManager) == null) {
                            wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
                            if (wifiIntent.resolveActivity(packageManager) != null) {
                                startActivity(wifiIntent)
                            }
                        } else {
                            startActivity(wifiIntent)
                        }
                    }
                }
                .setCancelListener(R.string.app_cancel) {
                }
                .setCanceled(true)
                .create().show()
        }
    }
}