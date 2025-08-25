package com.topdon.thermal.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.topdon.lib.core.bean.ObserveBean
import com.topdon.lib.ui.bean.ColorBean
import com.topdon.thermal.R
import com.topdon.thermal.databinding.ItmeTargetModeBinding

/**
 * Professional thermal measurement target adapter for distance-based calibration.
 * 
 * Manages thermal measurement targets with different distance configurations
 * for accurate temperature measurement across various object sizes and distances.
 * Implements ViewBinding for type-safe view access and professional UI patterns.
 *
 * @property context Android context for resource access
 * @property listener Click event handler with position and target code
 * @property selected Currently selected measurement target identifier
 * 
 * Features:
 * - Professional measurement target selection
 * - Distance-based temperature calibration (0.2m - 1.8m)
 * - Research-grade accuracy for different object types
 * - Real-time selection state management
 * - Industry-standard target configuration
 *
 * Supported targets:
 * - Person (1.8m) - Human body temperature measurement
 * - Sheep (1.0m) - Large animal thermal monitoring  
 * - Dog (0.5m) - Small animal temperature assessment
 * - Bird (0.2m) - Precision wildlife thermal analysis
 */
class MeasureItemAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    /**
     * Click event listener for target selection.
     * Provides position index and measurement type code.
     */
    var listener: ((index: Int, code: Int) -> Unit)? = null
    
    /**
     * Currently selected measurement target type.
     * -1 indicates no selection, otherwise matches ObserveBean target constants.
     */
    private var selected = -1

    /**
     * Updates the selected measurement target and refreshes UI.
     * @param index Target type code from ObserveBean constants
     */
    fun selected(index: Int) {
        selected = index
        notifyDataSetChanged()
    }

    /**
     * Professional measurement target configurations with optimal distance settings.
     * Each target type is calibrated for specific measurement scenarios.
     */
    private val measurementTargets = arrayListOf(
        ColorBean(R.drawable.ic_menu_thermal7001, "1.8m", ObserveBean.TYPE_MEASURE_PERSON), // Human body measurement
        ColorBean(R.drawable.ic_menu_thermal7002, "1.0m", ObserveBean.TYPE_MEASURE_SHEEP),  // Large animal monitoring
        ColorBean(R.drawable.ic_menu_thermal7003, "0.5m", ObserveBean.TYPE_MEASURE_DOG),   // Small animal assessment  
        ColorBean(R.drawable.ic_menu_thermal7004, "0.2m", ObserveBean.TYPE_MEASURE_BIRD),  // Precision wildlife analysis
    )


    /**
     * Creates ViewHolder with ViewBinding for type-safe view access.
     * @param parent Parent ViewGroup for inflation context
     * @param viewType View type identifier (unused in this adapter)
     * @return ItemView ViewHolder with bound views
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItmeTargetModeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemView(binding)
    }

    /**
     * Binds measurement target data to ViewHolder with selection state management.
     * @param holder ItemView ViewHolder with bound views
     * @param position Position in the measurement targets list
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemView) {
            val target = measurementTargets[position]
            
            with(holder.binding) {
                // Configure target icon and distance label
                itemMenuTabImg.setImageResource(target.res)
                itemMenuTabText.apply {
                    visibility = View.VISIBLE
                    text = target.name
                    isSelected = target.code == selected
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                }
                
                // Configure selection state
                itemMenuTabImg.isSelected = target.code == selected
                
                // Set click listener
                itemMenuTabLay.setOnClickListener {
                    listener?.invoke(position, target.code)
                    selected(target.code)
                }
            }
        }
    }

    /**
     * Returns the total number of measurement targets.
     * @return Size of measurement targets list
     */
    override fun getItemCount(): Int = measurementTargets.size

    /**
     * Professional ViewHolder implementation using ViewBinding.
     * Provides type-safe access to layout views with optimal performance.
     *
     * @property binding ViewBinding instance for the measurement target item layout
     */
    inner class ItemView(val binding: ItmeTargetModeBinding) : RecyclerView.ViewHolder(binding.root)
}