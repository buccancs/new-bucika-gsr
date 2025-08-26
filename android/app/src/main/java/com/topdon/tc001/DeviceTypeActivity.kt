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
import com.topdon.tc001.databinding.ActivityDeviceTypeBinding
import com.topdon.tc001.databinding.ItemDeviceTypeBinding

class DeviceTypeActivity : BaseActivity() {

    private lateinit var binding: ActivityDeviceTypeBinding

    private var clientType: IRDeviceType? = null

    override fun initContentView(): Int = R.layout.activity_device_type

    override fun initView() {
        binding = ActivityDeviceTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = MyAdapter(this).apply {
            onItemClickListener = {
                clientType = it

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

        if (clientType?.isLine() == true) {
            finish()
        }
    }

    override fun onSocketConnected(isTS004: Boolean) {

        finish()
    }

    private class MyAdapter(val context: Context) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

        var onItemClickListener: ((type: IRDeviceType) -> Unit)? = null

        private data class ItemInfo(val isTitle: Boolean, val firstType: IRDeviceType, val secondType: IRDeviceType?)

        private val dataList: ArrayList<ItemInfo> = arrayListOf(
            ItemInfo(true, IRDeviceType.TC001, null),
        )

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemDeviceTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val firstType: IRDeviceType = dataList[position].firstType
            val secondType: IRDeviceType? = dataList[position].secondType
            
            with(holder.binding) {
                tvTitle.isVisible = dataList[position].isTitle
                tvTitle.text = context.getString(if (firstType.isLine()) R.string.tc_connect_line else R.string.tc_connect_wifi)

                tvItem1.text = firstType.getDeviceName()

                ivItem1.setImageResource(R.drawable.ic_device_type_tc001)

                groupItem2.isVisible = secondType != null
                if (secondType != null) {
                    tvItem2.text = secondType.getDeviceName()

                    ivItem2.setImageResource(R.drawable.ic_device_type_tc001)
                }
            }
        }

        override fun getItemCount(): Int = dataList.size

        inner class ViewHolder(val binding: ItemDeviceTypeBinding) : RecyclerView.ViewHolder(binding.root) {
            init {
                binding.viewBgItem1.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener?.invoke(dataList[position].firstType)
                    }
                }
                binding.viewBgItem2.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val irDeviceType: IRDeviceType = dataList[position].secondType ?: return@setOnClickListener
                        onItemClickListener?.invoke(irDeviceType)
                    }
                }
            }
        }
    }

    enum class IRDeviceType {
        
        TC001 {
            override fun isLine(): Boolean = true
            override fun getDeviceName(): String = "TC001"
        };

        abstract fun isLine(): Boolean
        
        abstract fun getDeviceName(): String
    }
