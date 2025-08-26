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

class MonitoryHomeActivity : BaseActivity() {
    
    private lateinit var binding: ActivityMonitorHomeBinding
    
    override fun initContentView(): Int = R.layout.activity_monitor_home

    override fun initView() {

        binding = ActivityMonitorHomeBinding.bind(findViewById(android.R.id.content))
        
        val isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)
        binding.viewPager2.adapter = ViewPagerAdapter(this, isTC007)
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            tab.setText(if (position == 0) R.string.chart_history else R.string.chart_real_time)
        }.attach()
    }

    override fun initData() {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMonitorCreate(event: MonitorSaveEvent) {
        binding.viewPager2.currentItem = 0
    }

    private class ViewPagerAdapter(activity: MonitoryHomeActivity, val isTC007: Boolean) : FragmentStateAdapter(activity) {
        
        override fun getItemCount() = 2

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
}
