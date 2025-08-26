package com.topdon.tc001.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.launcher.ARouter
import com.topdon.lib.core.bean.event.SocketMsgEvent
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.ktbase.BaseFragment
import com.topdon.lib.core.repository.BatteryInfo
import com.topdon.lib.core.repository.TC007Repository
import com.topdon.lib.core.socket.SocketCmdUtil
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.tools.AppLanguageUtils
import com.topdon.lib.core.tools.DeviceTools
import com.topdon.lib.core.utils.NetWorkUtils
import com.topdon.lib.core.utils.WsCmdConstants
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.tc001.DeviceTypeActivity
import com.topdon.tc001.R
import com.topdon.tc001.databinding.ItemDeviceConnectBinding
import com.topdon.tc001.popup.DelPopup
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject

@SuppressLint("NotifyDataSetChanged")
class MainFragment : BaseFragment(), View.OnClickListener {
    
    private lateinit var adapter : MyAdapter

    override fun initContentView(): Int = R.layout.fragment_main

    override fun initView() {
        adapter = MyAdapter()
        
        findViewById<TextView>(R.id.tv_connect_device).setOnClickListener(this)
        findViewById<TextView>(R.id.tv_gsr_monitoring).setOnClickListener(this)
        findViewById<ImageView>(R.id.iv_add).setOnClickListener(this)
        
        adapter.hasConnectLine = DeviceTools.isConnect()
        adapter.hasConnectTS004 = WebSocketProxy.getInstance().isTS004Connect()
        adapter.hasConnectTC007 = WebSocketProxy.getInstance().isTC007Connect()
        
        adapter.onItemClickListener = {
            when (it) {
                ConnectType.LINE -> {
                    ARouter.getInstance()
                        .build(RouterConfig.IR_MAIN)
                        .withBoolean(ExtraKeyConfig.IS_TC007, false)
                        .navigation(requireContext())
                }
                ConnectType.TS004 -> {
                    if (WebSocketProxy.getInstance().isTS004Connect()) {
                        ARouter.getInstance().build(RouterConfig.IR_MONOCULAR).navigation(requireContext())
                    } else {
                        ARouter.getInstance()
                            .build(RouterConfig.IR_DEVICE_ADD)
                            .withBoolean("isTS004", true)
                            .navigation(requireContext())
                    }
                }
                ConnectType.TC007 -> {
                    ARouter.getInstance()
                        .build(RouterConfig.IR_MAIN)
                        .withBoolean(ExtraKeyConfig.IS_TC007, true)
                        .navigation(requireContext())
                }
            }
        }
        
        adapter.onItemLongClickListener = { view, type ->
            val popup = DelPopup(requireContext())
            popup.onDelListener = {
                TipDialog.Builder(requireContext())
                    .setTitleMessage(AppLanguageUtils.attachBaseContext(
                        context, SharedManager.getLanguage(requireContext())).getString(R.string.tc_delete_device))
                    .setMessage(R.string.tc_delete_device_tips)
                    .setPositiveListener(R.string.report_delete) {
                        when (type) {
                            ConnectType.LINE -> SharedManager.hasTcLine = false
                            ConnectType.TS004 -> SharedManager.hasTS004 = false
                            ConnectType.TC007 -> SharedManager.hasTC007 = false
                        }
                        refresh()
                        TToast.shortToast(requireContext(), R.string.test_results_delete_success)
                    }
                    .setCancelListener(R.string.app_cancel)
                    .create().show()
            }
            popup.show(view)
        }

        findViewById<RecyclerView>(R.id.recycler_view).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MainFragment.adapter
        }

        if (WebSocketProxy.getInstance().isTC007Connect()) {
            lifecycleScope.launch {
                val batteryInfo: BatteryInfo? = TC007Repository.getBatteryInfo()
                if (batteryInfo != null) {
                    adapter.tc007Battery = batteryInfo
                }
            }
        }
        
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {

                if (WebSocketProxy.getInstance().isConnected()) {
                    NetWorkUtils.switchNetwork(true)
                }
            }
        })
    }

    override fun initData() {

    }

    override fun onResume() {
        super.onResume()
        refresh()
        adapter.notifyDataSetChanged()
    }

    private fun refresh() {
        val hasAnyDevice = SharedManager.hasTcLine || SharedManager.hasTS004 || SharedManager.hasTC007
        findViewById<ConstraintLayout>(R.id.cl_has_device).isVisible = hasAnyDevice
        findViewById<ConstraintLayout>(R.id.cl_no_device).isVisible = !hasAnyDevice
        adapter.hasConnectLine = DeviceTools.isConnect(isAutoRequest = false)
        adapter.hasConnectTS004 = WebSocketProxy.getInstance().isTS004Connect()
        adapter.hasConnectTC007 = WebSocketProxy.getInstance().isTC007Connect()
        adapter.notifyDataSetChanged()
    }

    override fun connected() {
        adapter.hasConnectLine = true
        SharedManager.hasTcLine = true
        refresh()
    }

    override fun disConnected() {
        adapter.hasConnectLine = false
    }

    override fun onSocketConnected(isTS004: Boolean) {
        if (isTS004) {
            SharedManager.hasTS004 = true
            adapter.hasConnectTS004 = true
        } else {
            SharedManager.hasTC007 = true
            adapter.hasConnectTC007 = true
            lifecycleScope.launch {
                val batteryInfo: BatteryInfo? = TC007Repository.getBatteryInfo()
                if (batteryInfo != null) {
                    adapter.tc007Battery = batteryInfo
                }
            }
        }
    }

    override fun onSocketDisConnected(isTS004: Boolean) {
        if (isTS004) {
            adapter.hasConnectTS004 = false
        } else {
            adapter.hasConnectTC007 = false
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_connect_device, R.id.iv_add -> {

                startActivity(Intent(requireContext(), DeviceTypeActivity::class.java))
            }
            R.id.tv_gsr_monitoring -> {

                startActivity(Intent(requireContext(), com.topdon.tc001.gsr.GSRActivity::class.java))
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSocketMsgEvent(event: SocketMsgEvent) {
        if (SocketCmdUtil.getCmdResponse(event.text) == WsCmdConstants.APP_EVENT_HEART_BEATS) {
            if (!adapter.hasConnectTC007) {
                return
            }
            try {
                val battery: JSONObject = JSONObject(event.text).getJSONObject("battery")
                adapter.tc007Battery = BatteryInfo(battery.getString("status"), battery.getString("remaining"))
            } catch (_: Exception) {

            }
        }
    }

    private class MyAdapter : RecyclerView.Adapter<MyAdapter.ViewHolder>() {
        
        var hasConnectLine: Boolean = false
            set(value) {
                field = value
                notifyItemRangeChanged(0, 3)
            }
        
        var hasConnectTS004: Boolean = false
            set(value) {
                field = value
                notifyItemRangeChanged(0, itemCount)
            }
        
        var hasConnectTC007: Boolean = false
            set(value) {
                field = value
                notifyItemRangeChanged(0, itemCount)
            }
        
        var tc007Battery: BatteryInfo? = null
            set(value) {
                if (field != value) {
                    field = value
                    notifyItemRangeChanged(0, itemCount)
                }
            }

        var onItemClickListener: ((type: ConnectType) -> Unit)? = null
        var onItemLongClickListener: ((view: View, type: ConnectType) -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemDeviceConnectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val type = holder.getConnectType(position)
            val hasTitle: Boolean = when (position) {
                0 -> true
                1 -> SharedManager.hasTcLine
                else -> false
            }
            val hasConnect: Boolean = when (type) {
                ConnectType.LINE -> hasConnectLine
                ConnectType.TS004 -> hasConnectTS004
                ConnectType.TC007 -> hasConnectTC007
            }

            with(holder.binding) {

                tvTitle.isVisible = hasTitle
                tvTitle.text = AppLanguageUtils.attachBaseContext(
                    root.context, SharedManager.getLanguage(root.context))
                    .getString(if (type == ConnectType.LINE) R.string.tc_connect_line else R.string.tc_connect_wifi)

                ivBg.isSelected = hasConnect
                tvDeviceName.isSelected = hasConnect
                viewDeviceState.isSelected = hasConnect
                tvDeviceState.isSelected = hasConnect
                tvDeviceState.text = if (hasConnect) "online" else "offline"
                
                tvBattery.isVisible = type == ConnectType.TC007 && hasConnectTC007 && tc007Battery != null
                batteryView.isVisible = type == ConnectType.TC007 && hasConnectTC007 && tc007Battery != null

                when (type) {
                    ConnectType.LINE -> {
                        tvDeviceName.text = AppLanguageUtils.attachBaseContext(
                            root.context, SharedManager.getLanguage(root.context))
                            .getString(R.string.tc_has_line_device)
                        ivImage.setImageResource(
                            if (hasConnect) R.drawable.ic_main_device_line_connect 
                            else R.drawable.ic_main_device_line_disconnect
                        )
                    }
                    ConnectType.TS004 -> {
                        tvDeviceName.text = "TS004"
                        ivImage.setImageResource(
                            if (hasConnect) R.drawable.ic_main_device_ts004_connect 
                            else R.drawable.ic_main_device_ts004_disconnect
                        )
                    }
                    ConnectType.TC007 -> {
                        tvDeviceName.text = "TC007"
                        ivImage.setImageResource(
                            if (hasConnect) R.drawable.ic_main_device_tc007_connect 
                            else R.drawable.ic_main_device_tc007_disconnect
                        )
                        tvBattery.text = "${tc007Battery?.getBattery()}%"
                        batteryView.battery = tc007Battery?.getBattery() ?: 0
                        batteryView.isCharging = tc007Battery?.isCharging() ?: false
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            var result = 0
            if (SharedManager.hasTcLine) result++
            if (SharedManager.hasTS004) result++
            if (SharedManager.hasTC007) result++
            return result
        }

        inner class ViewHolder(val binding: ItemDeviceConnectBinding) : RecyclerView.ViewHolder(binding.root) {
            init {
                binding.ivBg.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener?.invoke(getConnectType(position))
                    }
                }
                
                binding.ivBg.setOnLongClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {

                        val deviceType = getConnectType(position)
                        when (deviceType) {
                            ConnectType.LINE -> {
                                if (DeviceTools.isConnect()) return@setOnLongClickListener true
                            }
                            ConnectType.TS004 -> {
                                if (WebSocketProxy.getInstance().isTS004Connect()) return@setOnLongClickListener true
                            }
                            ConnectType.TC007 -> {
                                if (WebSocketProxy.getInstance().isTC007Connect()) return@setOnLongClickListener true
                            }
                        }
                        onItemLongClickListener?.invoke(it, deviceType)
                    }
                    true
                }
            }

            fun getConnectType(position: Int): ConnectType = when (position) {
                0 -> if (SharedManager.hasTcLine) {
                    ConnectType.LINE
                } else if (SharedManager.hasTS004) {
                    ConnectType.TS004
                } else {
                    ConnectType.TC007
                }
                1 -> if (SharedManager.hasTcLine) {
                    if (SharedManager.hasTS004) ConnectType.TS004 else ConnectType.TC007
                } else {
                    ConnectType.TC007
                }
                else -> ConnectType.TC007
            }
        }
    }

    enum class ConnectType {
        
        LINE,
        
        TS004,
        
        TC007,
    }
