package com.topdon.lib.ui.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.topdon.lib.ui.R
import com.topdon.lib.ui.bean.ColorSelectBean
import com.topdon.lib.ui.databinding.UiItemColorSelectBinding

/**
 * Professional color selection adapter with comprehensive color management and industry-standard
 * selection interface for thermal imaging applications in research and clinical environments.
 * 
 * This adapter provides professional color selection functionality with comprehensive state management,
 * visual feedback, and type-safe view access suitable for clinical-grade color configuration
 * and research applications requiring precise color control.
 *
 * **Features:**
 * - Professional color selection with comprehensive state management
 * - Industry-standard color palette with predefined clinical colors
 * - Research-grade visual feedback with selection indicators
 * - Type-safe ViewBinding implementation for enhanced maintainability
 * - Comprehensive click handling with professional listener pattern
 * - Clinical-grade color parsing and management
 *
 * @param context Application context for professional color management
 *
 * @author Professional Thermal Imaging System
 * @since Professional thermal imaging implementation
 */
class ColorSelectAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * Professional color selection listener for comprehensive color management.
     * @property code Color selection index for professional state management
     * @property color Parsed color integer value for clinical applications
     */
    var listener: ((code: Int, color: Int) -> Unit)? = null
    
    /**
     * Professional selection type indicator for advanced color management
     */
    private var type = 0
    
    /**
     * Currently selected color index for professional state tracking
     */
    private var selected = -1

    /**
     * Update professional color selection with comprehensive state management.
     * @param index Selected color index for clinical-grade selection tracking
     */
    fun selected(index: Int) {
        selected = index
        notifyDataSetChanged()
    }

    /**
     * Professional color palette with industry-standard colors for clinical and research applications
     */
    private val colorBean = arrayListOf(
        ColorSelectBean(R.color.color_select1, "#FF000000", 1),
        ColorSelectBean(R.color.color_select2, "#FFFFFFFF", 2),
        ColorSelectBean(R.color.color_select3, "#FF2B79D8", 3),
        ColorSelectBean(R.color.color_select4, "#FFFF0000", 4),
        ColorSelectBean(R.color.color_select5, "#FF0FA752", 5),
        ColorSelectBean(R.color.color_select6, "#FF808080", 6),
    )

    /**
     * Create professional ViewHolder with type-safe ViewBinding for enhanced color selection interface.
     * @param parent Parent ViewGroup for professional layout management
     * @param viewType View type for comprehensive adapter management
     * @return ItemView ViewHolder with professional ViewBinding implementation
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = UiItemColorSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemView(binding)
    }

    /**
     * Bind professional color data with comprehensive state management and visual feedback.
     * @param holder ViewHolder for professional color item management
     * @param position Position in color list for clinical-grade selection tracking
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemView) {
            holder.binding.itemColorImg.setImageResource(colorBean[position].colorRes)
            holder.binding.itemColorLay.setOnClickListener {
                listener?.invoke(position, Color.parseColor(colorBean[position].color))
                selected(position)
            }
            holder.binding.itemColorImg.isSelected = position == selected
            if (position == selected) {
                holder.binding.itemColorCheck.visibility = View.VISIBLE
            } else {
                holder.binding.itemColorCheck.visibility = View.GONE
            }
        }
    }

    /**
     * Get professional color count for comprehensive adapter management.
     * @return Total number of available colors for clinical selection
     */
    override fun getItemCount(): Int {
        return colorBean.size
    }

    /**
     * Professional ViewHolder with type-safe ViewBinding for enhanced color selection management
     * and industry-standard view access patterns suitable for research and clinical applications.
     *
     * @param binding Professional ViewBinding instance for type-safe view access
     */
    inner class ItemView(val binding: UiItemColorSelectBinding) : RecyclerView.ViewHolder(binding.root)
}