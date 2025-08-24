package com.topdon.module.thermal.ir.adapter

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
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.databinding.ItmeTargetModeBinding

/**
 * Professional target selection adapter for thermal imaging analysis.
 * 
 * Manages target shape selection for thermal measurement and observation with
 * comprehensive configuration options. Implements ViewBinding for type-safe
 * view access and professional UI patterns.
 *
 * @property context Android context for resource access
 * @property listener Click event handler with position and target code
 * @property selected Currently selected target type identifier
 * 
 * Features:
 * - Professional target shape selection
 * - Horizontal, vertical, and circular target modes
 * - Research-grade thermal analysis configuration
 * - Real-time selection state management
 * - Industry-standard visual feedback
 *
 * Supported target types:
 * - Horizontal target: Linear horizontal measurement
 * - Vertical target: Linear vertical measurement  
 * - Circle target: Circular area measurement
 */
class TargetItemAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    /**
     * Click event listener for target selection.
     * Provides position index and target type code.
     */
    var listener: ((index: Int, code: Int) -> Unit)? = null
    
    /**
     * Currently selected target type.
     * -1 indicates no selection, otherwise matches ObserveBean target constants.
     */
    private var selected = -1

    /**
     * Updates the selected target type and refreshes UI.
     * @param index Target type code from ObserveBean constants
     */
    fun selected(index: Int) {
        selected = index
        notifyDataSetChanged()
    }

    /**
     * Returns the currently selected target type.
     * @return Selected target type code, or -1 if none selected
     */
    fun getSelected(): Int = selected

    /**
     * Professional target shape configurations for thermal measurement.
     * Each target type provides specific measurement capabilities.
     */
    private val targetShapes = arrayListOf(
        ColorBean(R.drawable.ic_menu_thermal6002, "", ObserveBean.TYPE_TARGET_HORIZONTAL), // Horizontal line target
        ColorBean(R.drawable.ic_menu_thermal6001, "", ObserveBean.TYPE_TARGET_VERTICAL),   // Vertical line target  
        ColorBean(R.drawable.ic_menu_thermal6003, "", ObserveBean.TYPE_TARGET_CIRCLE),     // Circular area target
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
     * Binds target shape data to ViewHolder with selection state management.
     * @param holder ItemView ViewHolder with bound views
     * @param position Position in the target shapes list
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemView) {
            val targetShape = targetShapes[position]
            
            with(holder.binding) {
                // Configure target icon
                itemMenuTabImg.setImageResource(targetShape.res)
                itemMenuTabImg.isSelected = targetShape.code == selected
                
                // Configure target name (empty for icon-only display)
                itemMenuTabText.apply {
                    text = targetShape.name
                    isSelected = targetShape.code == selected
                    setTextColor(
                        if (targetShape.code == selected) {
                            ContextCompat.getColor(context, R.color.white)
                        } else {
                            ContextCompat.getColor(context, R.color.font_third_color)
                        }
                    )
                }
                
                // Set click listener
                itemMenuTabLay.setOnClickListener {
                    listener?.invoke(position, targetShape.code)
                    selected(targetShape.code)
                }
            }
        }
    }

    /**
     * Returns the total number of target shapes available.
     * @return Size of target shapes list
     */
    override fun getItemCount(): Int = targetShapes.size

    /**
     * Professional ViewHolder implementation using ViewBinding.
     * Provides type-safe access to layout views with optimal performance.
     *
     * @property binding ViewBinding instance for the target item layout
     */
    inner class ItemView(val binding: ItmeTargetModeBinding) : RecyclerView.ViewHolder(binding.root)
}