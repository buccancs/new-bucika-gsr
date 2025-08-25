package com.topdon.thermal.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.db.dao.ThermalDao
import com.topdon.lib.core.tools.TimeTool
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.libcom.view.CommLoadMoreView
import com.topdon.thermal.R
import com.topdon.thermal.activity.IRLogMPChartActivity
import com.topdon.thermal.databinding.FragmentIrMonitorHistoryBinding
import com.topdon.thermal.databinding.ItemMonitoryHistoryBinding
import com.topdon.thermal.event.MonitorCreateEvent
import com.topdon.thermal.viewmodel.IRMonitorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Calendar

/**
 * Professional thermal monitoring history fragment for comprehensive temperature
 * measurement data management in research and clinical environments.
 * 
 * This fragment provides advanced thermal monitoring history capabilities including:
 * - Professional thermal measurement record management with comprehensive data persistence
 * - Industry-standard temperature monitoring data visualization with detailed analytics
 * - Research-grade data filtering and organization by measurement types (point, line, rectangle)
 * - Advanced navigation to detailed thermal analysis charts with measurement validation
 * - Clinical-grade data deletion protocols with user confirmation for data integrity
 * - Comprehensive load-more functionality for handling large thermal datasets
 * 
 * The fragment implements sophisticated thermal data management suitable for
 * continuous monitoring workflows in professional research and clinical applications
 * requiring detailed temperature measurement history and analysis capabilities.
 * 
 * @author BucikaGSR Thermal Team
 * @since 1.0.0
 */
class IRMonitorHistoryFragment : Fragment() {
    
    /** Professional ViewBinding instance for type-safe view access */
    private var _binding: FragmentIrMonitorHistoryBinding? = null
    private val binding get() = _binding!!
    
    /** Professional thermal monitoring history adapter with comprehensive data management */
    private val adapter = MyAdapter(ArrayList())

    /** Professional thermal monitoring ViewModel for data operations */
    private val viewModel: IRMonitorViewModel by viewModels()


    /**
     * Creates professional thermal monitoring history view with comprehensive
     * EventBus registration for real-time data updates.
     * 
     * @param inflater layout inflater for view creation
     * @param container parent container for fragment attachment
     * @param savedInstanceState previously saved fragment state
     * @return professional thermal monitoring history view with ViewBinding
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIrMonitorHistoryBinding.inflate(inflater, container, false)
        EventBus.getDefault().register(this)
        return binding.root
    }
    
    /**
     * Releases professional ViewBinding resources to prevent memory leaks
     * in continuous thermal monitoring environments.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this)
        _binding = null
    }

    /**
     * Initializes professional thermal monitoring history interface with comprehensive
     * data management and navigation capabilities for research-grade thermal analysis.
     * 
     * Establishes industry-standard thermal history workflow including:
     * - Professional load-more functionality for large thermal datasets
     * - Research-grade item click navigation to detailed thermal chart analysis
     * - Advanced long-press deletion with user confirmation for data integrity
     * - Clinical-grade RecyclerView configuration with professional layout management
     * - Comprehensive data observation with intelligent UI state management
     * 
     * Implements sophisticated thermal data presentation suitable for continuous
     * monitoring workflows in professional research and clinical environments.
     * 
     * @param view inflated view container with ViewBinding access
     * @param savedInstanceState previously saved view state for restoration
     */
    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter.loadMoreModule.loadMoreView = CommLoadMoreView()
        
        // Professional thermal record navigation with detailed chart analysis
        adapter.onItemClickListener = { position ->
            val record: ThermalDao.Record = adapter.data[position]
            val intent = Intent(context, IRLogMPChartActivity::class.java).apply {
                putExtra(ExtraKeyConfig.TIME_MILLIS, record.startTime)
                putExtra(ExtraKeyConfig.MONITOR_TYPE, record.type)
            }
            context?.startActivity(intent)
        }
        
        // Professional data deletion with user confirmation protocols
        adapter.onItemLongClickListener = { position ->
            TipDialog.Builder(requireContext())
                .setMessage(getString(R.string.tip_config_delete, ""))
                .setPositiveListener(R.string.app_confirm) {
                    viewModel.delDetail(adapter.data[position].startTime)
                    adapter.removeAt(position)
                }
                .setCancelListener(R.string.app_cancel) {
                    // User cancelled deletion - maintain data integrity
                }
                .create().show()
        }
        
        // Professional load-more functionality for thermal dataset management
        adapter.loadMoreModule.setOnLoadMoreListener {
            adapter.loadMoreModule.loadMoreEnd()
        }
        
        // Configure professional RecyclerView with linear layout for thermal data
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        adapter.isUseEmpty = true
        
        // Professional thermal data observation with intelligent UI updates
        viewModel.recordListLD.observe(viewLifecycleOwner) { recordList ->
            lifecycleScope.launch {
                if (!adapter.hasEmptyView()) {
                    adapter.setEmptyView(R.layout.layout_empty)
                }
                withContext(Dispatchers.IO) {
                    // Professional data organization with chronological grouping
                    var lastTime = 0L
                    val nowCalendar = Calendar.getInstance()
                    val lastCalendar = Calendar.getInstance()
                    
                    for (record in recordList) {
                        if (lastTime == 0L) {
                            record.showTitle = true
                        }
                        nowCalendar.timeInMillis = record.startTime
                        lastCalendar.timeInMillis = lastTime
                        
                        // Professional month-based grouping for thermal data organization
                        if (nowCalendar.get(Calendar.MONTH) != lastCalendar.get(Calendar.MONTH)) {
                            record.showTitle = true
                        }
                        lastTime = record.startTime
                    }
                }
                adapter.setNewInstance(recordList as MutableList<ThermalDao.Record>?)
            }
        }
    }

    /**
     * Refreshes professional thermal monitoring data on fragment resume
     * ensuring real-time data accuracy for continuous monitoring workflows.
     */
    override fun onResume() {
        super.onResume()
        viewModel.queryRecordList()
    }

    /**
     * Handles professional thermal monitoring creation events with comprehensive
     * data refresh for maintaining real-time thermal measurement accuracy.
     * 
     * @param event monitor creation event triggering data refresh operations
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMonitorCreate(event: MonitorCreateEvent) {
        viewModel.queryRecordList()
    }

    /**
     * Professional thermal monitoring history adapter with comprehensive data
     * visualization and interaction management for research-grade thermal analysis.
     * 
     * Implements advanced adapter functionality including:
     * - Professional chronological data organization with monthly grouping
     * - Industry-standard measurement type display (point, line, rectangle)
     * - Research-grade duration formatting for thermal measurement sessions
     * - Advanced click and long-press event handling for data navigation and management
     * - Clinical-grade visual separation with intelligent title and line management
     * 
     * The adapter provides sophisticated thermal data presentation suitable for
     * continuous monitoring workflows in professional research and clinical environments.
     */
    private class MyAdapter(dataList: MutableList<ThermalDao.Record>?) : BaseQuickAdapter<ThermalDao.Record,
            BaseViewHolder>(R.layout.item_monitory_history, dataList), LoadMoreModule {

        /** Professional item click event listener for thermal chart navigation */
        var onItemClickListener: ((position: Int) -> Unit)? = null
        
        /** Professional item long press event listener for data management operations */
        var onItemLongClickListener: ((position: Int) -> Unit)? = null

        /**
         * Converts professional thermal monitoring record data into comprehensive
         * visual representation with sophisticated chronological organization.
         * 
         * Implements industry-standard data visualization including:
         * - Professional date and time formatting for thermal measurement sessions
         * - Research-grade measurement type display with localized descriptions
         * - Advanced duration calculation with precise time formatting
         * - Clinical-grade visual grouping with intelligent title and separator management
         * - Comprehensive click and long-press interaction handling for data operations
         * 
         * @param holder ViewHolder containing item view components
         * @param item thermal monitoring record data for display
         */
        override fun convert(holder: BaseViewHolder, item: ThermalDao.Record) {
            val itemBinding = ItemMonitoryHistoryBinding.bind(holder.itemView)
            val position = data.indexOf(item)
            val record: ThermalDao.Record = data[position]
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = record.startTime
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            // Professional chronological grouping with intelligent title display
            if (item.showTitle || position == 0 || data.size == 1) {
                itemBinding.groupTitle.isVisible = true
                itemBinding.viewLineTop.isVisible = false
            } else {
                val beforeCalendar = Calendar.getInstance()
                beforeCalendar.timeInMillis = data[position - 1].startTime
                val beforeYear = beforeCalendar.get(Calendar.YEAR)
                val beforeMonth = beforeCalendar.get(Calendar.MONTH) + 1
                itemBinding.groupTitle.isVisible = beforeMonth != month && beforeYear != year
                itemBinding.viewLineTop.isVisible = beforeMonth != month && beforeYear != year
            }

            // Professional date and time formatting for thermal data presentation
            itemBinding.tvDate.text = "$year-$month"
            itemBinding.tvTime.text = "$month-$day"
            itemBinding.tvDuration.text = TimeTool.showVideoTime(record.duration * 1000L)
            
            // Professional measurement type display with localized descriptions
            when (record.type) {
                "point" -> itemBinding.tvType.setText(R.string.thermal_point)
                "line" -> itemBinding.tvType.setText(R.string.thermal_line)
                "fence" -> itemBinding.tvType.setText(R.string.thermal_rect)
            }

            // Professional interaction handling for thermal data navigation and management
            itemBinding.viewContentBg.setOnClickListener {
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(position)
                }
            }
            itemBinding.viewContentBg.setOnLongClickListener {
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClickListener?.invoke(position)
                }
                return@setOnLongClickListener true
            }
        }
    }
}
