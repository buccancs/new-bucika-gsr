package com.topdon.tc001

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.launcher.ARouter
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.DeviceTools
import kotlinx.android.synthetic.main.activity_device_type.*
import kotlinx.android.synthetic.main.item_device_type.view.*

/**
 * 设备类型选择.
 *
 * Created by LCG on 2024/4/22.
 */
class DeviceTypeActivity : BaseActivity() {

    /**
     * 当前点击的设备类型.
     */
    private var clientType: IRDeviceType? = null

    override fun initContentView(): Int = R.layout.activity_device_type

    override fun initView() {
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = MyAdapter(this).apply {
            onItemClickListener = {
                clientType = it
                // Only TC001 is supported for bucika_gsr
                ARouter.getInstance()
                    .build(RouterConfig.IR_MAIN)
                    .withBoolean(ExtraKeyConfig.IS_TC007, false)
                    .navigation(this@DeviceTypeActivity)
                if (DeviceTools.isConnect()) {
                    finish()
                }
            }
        }
    }

    override fun initData() {
    }

    override fun connected() {
        // Only TC001 is supported - always finish on line connection
        if (clientType?.isLine() == true) {
            finish()
        }
    }

    override fun onSocketConnected(isTS004: Boolean) {
        // Only TC001 is supported - simplified connection handling
        finish()
    }

    private class MyAdapter(val context: Context) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

        var onItemClickListener: ((type: IRDeviceType) -> Unit)? = null

        private data class ItemInfo(val isTitle:Boolean, val firstType: IRDeviceType, val secondType: IRDeviceType?)

        // Modified for bucika_gsr - only TC001 support
        private val dataList: ArrayList<ItemInfo> = arrayListOf(
            ItemInfo(true, IRDeviceType.TC001, null),
        )

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_device_type, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val firstType: IRDeviceType = dataList[position].firstType
            val secondType: IRDeviceType? = dataList[position].secondType
            holder.itemView.tv_title.isVisible = dataList[position].isTitle
            holder.itemView.tv_title.text = context.getString(if (firstType.isLine()) R.string.tc_connect_line else R.string.tc_connect_wifi)

            holder.itemView.tv_item1.text = firstType.getDeviceName()
            // Only TC001 is supported for bucika_gsr
            holder.itemView.iv_item1.setImageResource(R.drawable.ic_device_type_tc001)

            holder.itemView.group_item2.isVisible = secondType != null
            if (secondType != null) {
                holder.itemView.tv_item2.text = secondType.getDeviceName()
                // Only TC001 is supported for bucika_gsr
                holder.itemView.iv_item2.setImageResource(R.drawable.ic_device_type_tc001)
            }
        }

        override fun getItemCount(): Int = dataList.size

        inner class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
            init {
                rootView.view_bg_item1.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener?.invoke(dataList[position].firstType)
                    }
                }
                rootView.view_bg_item2.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val irDeviceType: IRDeviceType = dataList[position].secondType ?: return@setOnClickListener
                        onItemClickListener?.invoke(irDeviceType)
                    }
                }
            }
        }
    }

    /**
     * 支持的热成像设备类型 - Only TC001 supported for bucika_gsr.
     */
    enum class IRDeviceType {
        TC001 {
            override fun isLine(): Boolean = true
            override fun getDeviceName(): String = "TC001"
        };

        abstract fun isLine(): Boolean
        abstract fun getDeviceName(): String
    }
}