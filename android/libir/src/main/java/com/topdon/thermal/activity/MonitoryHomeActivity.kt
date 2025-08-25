package com.topdon.thermal.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.thermal.R
import com.topdon.thermal.databinding.ActivityMonitorHomeBinding
import com.topdon.thermal.event.MonitorSaveEvent
import com.topdon.thermal.fragment.IRMonitorCaptureFragment
import com.topdon.thermal.fragment.IRMonitorHistoryFragment
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Temperature monitoring tab-based activity interface.
 * 
 * Provides comprehensive temperature monitoring functionality with dual-mode
 * interface supporting both historical data analysis and real-time monitoring
 * capabilities for professional thermal imaging applications.
 * 
 * **Core Features:**
 * - Historical temperature monitoring with [IRMonitorHistoryFragment]
 * - Real-time temperature capture with [IRMonitorCaptureFragment]
 * - Device-specific configuration support (TC007 compatibility)
 * - Professional tab-based interface with industry-standard navigation
 * - EventBus integration for cross-component communication
 * 
 * **Required Parameters:**
 * - [ExtraKeyConfig.IS_TC007] - Device compatibility flag for TC007 thermal cameras
 * 
 * **Architecture:**
 * - Extends BaseActivity for consistent app behavior
 * - Implements ViewBinding for type-safe view access
 * - Uses ViewPager2 with FragmentStateAdapter for smooth tab navigation
 * - Integrates TabLayoutMediator for synchronized tab-content interaction
 * 
 * @author LCG
 * @since 2024-08-20
 * @see IRMonitorHistoryFragment Historical monitoring interface
 * @see IRMonitorCaptureFragment Real-time monitoring interface
 * @see BaseActivity Application base activity class
 */
class MonitoryHomeActivity : BaseActivity() {
    
    /** ViewBinding instance for type-safe view access */
    private lateinit var binding: ActivityMonitorHomeBinding
    
    /**
     * Inflates the layout using ViewBinding for type-safe view access.
     * 
     * @return Layout resource ID for the monitoring home interface
     */
    override fun initContentView(): Int = R.layout.activity_monitor_home

    /**
     * Initializes the view components and sets up the tab-based interface.
     * 
     * Configures ViewPager2 with device-specific parameters and establishes
     * tab navigation with proper text labels for historical and real-time modes.
     * 
     * @throws RuntimeException if ViewBinding initialization fails
     */
    override fun initView() {
        // Initialize ViewBinding after layout is set by base class
        binding = ActivityMonitorHomeBinding.bind(findViewById(android.R.id.content))
        
        val isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)
        binding.viewPager2.adapter = ViewPagerAdapter(this, isTC007)
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            tab.setText(if (position == 0) R.string.chart_history else R.string.chart_real_time)
        }.attach()
    }

    /**
     * Initializes data components for the monitoring interface.
     * 
     * Currently no additional data initialization is required as fragments
     * handle their own data loading and management.
     */
    override fun initData() {
    }

    /**
     * Handles monitoring save events from the EventBus system.
     * 
     * Automatically switches to the historical monitoring tab when a new
     * monitoring session is saved, providing immediate access to the recorded data.
     * 
     * @param event Monitor save event containing save operation details
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMonitorCreate(event: MonitorSaveEvent) {
        binding.viewPager2.currentItem = 0
    }

    /**
     * ViewPager adapter for managing temperature monitoring fragments.
     * 
     * Provides proper fragment instantiation with device-specific configuration
     * for both historical and real-time monitoring interfaces.
     * 
     * @param activity Parent activity reference for fragment management
     * @param isTC007 Device compatibility flag for TC007 thermal cameras
     */
    private class ViewPagerAdapter(activity: MonitoryHomeActivity, val isTC007: Boolean) : FragmentStateAdapter(activity) {
        
        /**
         * Returns the number of tabs in the monitoring interface.
         * 
         * @return Fixed count of 2 tabs (historical and real-time)
         */
        override fun getItemCount() = 2

        /**
         * Creates fragment instances for each tab position.
         * 
         * Provides proper fragment configuration with device-specific parameters
         * for optimal thermal monitoring functionality.
         * 
         * @param position Tab position (0 for history, 1 for real-time)
         * @return Configured fragment instance for the specified position
         */
        override fun createFragment(position: Int): Fragment {
            return if (position == 0) {
                IRMonitorHistoryFragment()
            } else {
                val fragment = IRMonitorCaptureFragment()
                fragment.arguments = Bundle().also { it.putBoolean(ExtraKeyConfig.IS_TC007, isTC007) }
                fragment
            }
        }
    }
