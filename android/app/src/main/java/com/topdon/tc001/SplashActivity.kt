package com.topdon.tc001

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.launcher.ARouter
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.utils.CommUtils
import com.topdon.lms.sdk.Config
import com.topdon.lms.sdk.LMS
import com.topdon.tc001.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash screen activity that displays the app logo and name.
 * Handles initial app setup and navigation to the main activity or clause screen.
 * 
 * @author BucikaGSR Development Team
 * @since 1.0.0
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    /**
     * Initialize the splash screen and set up navigation logic.
     * 
     * @param savedInstanceState If the activity is being re-initialized after previously 
     * being shut down then this Bundle contains the data it most recently supplied.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        LMS.getInstance().screenOrientation = Config.SCREEN_PORTRAIT
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.navigationBarColor = ContextCompat.getColor(this, R.color.toolbar_16131E)

        lifecycleScope.launch {
            delay(if (BuildConfig.DEBUG) 3000 else 1000)
            if (SharedManager.getHasShowClause()) {
                ARouter.getInstance().build(RouterConfig.MAIN).navigation(this@SplashActivity)
            } else {
                ARouter.getInstance().build(RouterConfig.CLAUSE).navigation(this@SplashActivity)
            }
            finish()
        }
        binding.tvAppName.text = CommUtils.getAppName()
    }

    /**
     * Override back press to prevent users from accidentally exiting the splash screen.
     */
    override fun onBackPressed() {
        // Intentionally empty - splash screen should not be dismissible via back press
    }
}