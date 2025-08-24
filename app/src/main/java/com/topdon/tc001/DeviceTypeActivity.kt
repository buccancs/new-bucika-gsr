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

/**
 * Device Type Selection Activity
 * 
 * This activity provides device type selection functionality for the BucikaGSR application,
 * allowing users to choose their thermal imaging device type for optimal configuration.
 * 
 * Key Features:
 * - Device type selection interface with visual device representation
 * - Specialized support for TC001 thermal imaging device
 * - Connection type indication (line/Wi-Fi connectivity)
 * - Automatic navigation to main thermal imaging interface
 * - Connection state management and auto-finish on successful connection
 * - Optimized for BucikaGSR research application requirements
 * 
 * ViewBinding Integration:
 * - Modern type-safe view access using ActivityDeviceTypeBinding
 * - ItemDeviceTypeBinding for RecyclerView items
 * - Eliminates synthetic view imports for better compile-time safety
 * - Follows Android development best practices
 * 
 * Technical Implementation:
 * - RecyclerView with custom adapter for device type display
 * - Connection state monitoring with DeviceTools integration
 * - ARouter navigation to thermal imaging main interface
 * - Support for multiple connection types (line/Wi-Fi)
 * 
 * Device Support:
 * - TC001: Primary thermal imaging device with line connectivity
 * - Extensible architecture for additional device types
 * 
 * @author BucikaGSR Development Team
 * @since 1.0.0
 * @see BaseActivity for common activity functionality
 * @see IRDeviceType for supported device enumeration
 */
class DeviceTypeActivity : BaseActivity() {

    /**
     * ViewBinding instance for type-safe view access
     * Provides compile-time verified access to all views in activity_device_type.xml
     */
    private lateinit var binding: ActivityDeviceTypeBinding

    /**
     * Currently selected device type
     * Tracks user selection for connection management
     */
    private var clientType: IRDeviceType? = null

    /**
     * Initializes the content view using ViewBinding
     * 
     * @return The layout resource ID for the activity
     */
    override fun initContentView(): Int = R.layout.activity_device_type

    /**
     * Initializes view components and sets up the device selection interface
     * 
     * Sets up:
     * - RecyclerView with LinearLayoutManager for device type list
     * - Custom adapter with device selection handling
     * - Navigation to thermal imaging main interface on selection
     * - Automatic activity finish on successful device connection
     */
    override fun initView() {
        binding = ActivityDeviceTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = MyAdapter(this).apply {
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

    /**
     * Initializes activity data
     * No specific data initialization required for device type selection
     */
    override fun initData() {
    }

    /**
     * Called when device connection is established
     * Automatically finishes activity for line-connected devices (TC001)
     */
    override fun connected() {
        // Only TC001 is supported - always finish on line connection
        if (clientType?.isLine() == true) {
            finish()
        }
    }

    /**
     * Called when socket connection is established
     * Handles connection completion and activity lifecycle management
     * 
     * @param isTS004 Whether the connected device is TS004 (not applicable for BucikaGSR)
     */
    override fun onSocketConnected(isTS004: Boolean) {
        // Only TC001 is supported - simplified connection handling
        finish()
    }

    /**
     * Custom RecyclerView adapter for device type selection
     * 
     * Provides a visual interface for device type selection with:
     * - Device type categorization (line/Wi-Fi connection)
     * - Device-specific icons and names
     * - Click handling for device selection
     */
    private class MyAdapter(val context: Context) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

        /**
         * Click listener for device type selection
         */
        var onItemClickListener: ((type: IRDeviceType) -> Unit)? = null

        /**
         * Data class representing a device type item in the list
         * 
         * @param isTitle Whether this item represents a category title
         * @param firstType Primary device type for the item
         * @param secondType Secondary device type (if applicable)
         */
        private data class ItemInfo(val isTitle: Boolean, val firstType: IRDeviceType, val secondType: IRDeviceType?)

        /**
         * Device type data list
         * Modified for bucika_gsr - only TC001 support for research applications
         */
        private val dataList: ArrayList<ItemInfo> = arrayListOf(
            ItemInfo(true, IRDeviceType.TC001, null),
        )

        /**
         * Creates view holder for device type items
         * 
         * @param parent Parent view group
         * @param viewType View type identifier
         * @return Configured ViewHolder instance
         */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemDeviceTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        /**
         * Binds data to view holder for display
         * 
         * @param holder ViewHolder instance to bind data to
         * @param position Position in the adapter
         */
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val firstType: IRDeviceType = dataList[position].firstType
            val secondType: IRDeviceType? = dataList[position].secondType
            
            with(holder.binding) {
                tvTitle.isVisible = dataList[position].isTitle
                tvTitle.text = context.getString(if (firstType.isLine()) R.string.tc_connect_line else R.string.tc_connect_wifi)

                tvItem1.text = firstType.getDeviceName()
                // Only TC001 is supported for bucika_gsr
                ivItem1.setImageResource(R.drawable.ic_device_type_tc001)

                groupItem2.isVisible = secondType != null
                if (secondType != null) {
                    tvItem2.text = secondType.getDeviceName()
                    // Only TC001 is supported for bucika_gsr
                    ivItem2.setImageResource(R.drawable.ic_device_type_tc001)
                }
            }
        }

        /**
         * Returns the total number of items in the adapter
         * 
         * @return Total item count
         */
        override fun getItemCount(): Int = dataList.size

        /**
         * ViewHolder class for device type items
         * 
         * @param binding ViewBinding instance for the item layout
         */
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

    /**
     * Supported thermal imaging device types for BucikaGSR research application
     * 
     * Currently supports TC001 with line connectivity for optimal research data collection.
     * Architecture allows for future expansion to additional device types.
     */
    enum class IRDeviceType {
        /**
         * TC001 thermal imaging device
         * - Line connectivity for stable data transmission
         * - Optimized for research applications
         * - Primary device for BucikaGSR GSR data collection
         */
        TC001 {
            override fun isLine(): Boolean = true
            override fun getDeviceName(): String = "TC001"
        };

        /**
         * Indicates whether the device uses line connectivity
         * 
         * @return true for line-connected devices, false for Wi-Fi
         */
        abstract fun isLine(): Boolean
        
        /**
         * Returns the display name of the device
         * 
         * @return Device display name
         */
        abstract fun getDeviceName(): String
    }
}