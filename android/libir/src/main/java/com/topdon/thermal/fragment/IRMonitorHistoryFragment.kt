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

class IRMonitorHistoryFragment : Fragment() {
    
    private var _binding: FragmentIrMonitorHistoryBinding? = null
    private val binding get() = _binding!!
    
    private val adapter = MyAdapter(ArrayList())

    private val viewModel: IRMonitorViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIrMonitorHistoryBinding.inflate(inflater, container, false)
        EventBus.getDefault().register(this)
        return binding.root
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this)
        _binding = null
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter.loadMoreModule.loadMoreView = CommLoadMoreView()
        
        adapter.onItemClickListener = { position ->
            val record: ThermalDao.Record = adapter.data[position]
            val intent = Intent(context, IRLogMPChartActivity::class.java).apply {
                putExtra(ExtraKeyConfig.TIME_MILLIS, record.startTime)
                putExtra(ExtraKeyConfig.MONITOR_TYPE, record.type)
            }
            context?.startActivity(intent)
        }
        
        adapter.onItemLongClickListener = { position ->
            TipDialog.Builder(requireContext())
                .setMessage(getString(R.string.tip_config_delete, ""))
                .setPositiveListener(R.string.app_confirm) {
                    viewModel.delDetail(adapter.data[position].startTime)
                    adapter.removeAt(position)
                }
                .setCancelListener(R.string.app_cancel) {

                }
                .create().show()
        }
        
        adapter.loadMoreModule.setOnLoadMoreListener {
            adapter.loadMoreModule.loadMoreEnd()
        }
        
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        adapter.isUseEmpty = true
        
        viewModel.recordListLD.observe(viewLifecycleOwner) { recordList ->
            lifecycleScope.launch {
                if (!adapter.hasEmptyView()) {
                    adapter.setEmptyView(R.layout.layout_empty)
                }
                withContext(Dispatchers.IO) {

                    var lastTime = 0L
                    val nowCalendar = Calendar.getInstance()
                    val lastCalendar = Calendar.getInstance()
                    
                    for (record in recordList) {
                        if (lastTime == 0L) {
                            record.showTitle = true
                        }
                        nowCalendar.timeInMillis = record.startTime
                        lastCalendar.timeInMillis = lastTime
                        
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

    override fun onResume() {
        super.onResume()
        viewModel.queryRecordList()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMonitorCreate(event: MonitorCreateEvent) {
        viewModel.queryRecordList()
    }

    private class MyAdapter(dataList: MutableList<ThermalDao.Record>?) : BaseQuickAdapter<ThermalDao.Record,
            BaseViewHolder>(R.layout.item_monitory_history, dataList), LoadMoreModule {

        var onItemClickListener: ((position: Int) -> Unit)? = null
        
        var onItemLongClickListener: ((position: Int) -> Unit)? = null

        override fun convert(holder: BaseViewHolder, item: ThermalDao.Record) {
            val itemBinding = ItemMonitoryHistoryBinding.bind(holder.itemView)
            val position = data.indexOf(item)
            val record: ThermalDao.Record = data[position]
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = record.startTime
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)

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

            itemBinding.tvDate.text = "$year-$month"
            itemBinding.tvTime.text = "$month-$day"
            itemBinding.tvDuration.text = TimeTool.showVideoTime(record.duration * 1000L)
            
            when (record.type) {
                "point" -> itemBinding.tvType.setText(R.string.thermal_point)
                "line" -> itemBinding.tvType.setText(R.string.thermal_line)
                "fence" -> itemBinding.tvType.setText(R.string.thermal_rect)
            }

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
