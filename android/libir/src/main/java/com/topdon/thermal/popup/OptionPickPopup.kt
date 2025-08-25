package com.topdon.thermal.popup

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.SizeUtils
import com.topdon.lib.core.view.MyTextView
import com.topdon.thermal.R
import com.topdon.thermal.databinding.PopupOptionPickBinding

/**
 * Option selection PopupWindow with ViewBinding implementation.
 * 
 * Provides modern dropdown interface for thermal imaging parameter selection
 * with support for custom icons and dynamic sizing based on content.
 * 
 * @param context Application context for popup display
 * @param strArray Array of option strings to display
 * @param resIdArray Optional array of drawable resource IDs for option icons
 * @author Topdon Thermal Imaging Team
 * @since 2024-01-05
 */
class OptionPickPopup(
    private val context: Context, 
    private val strArray: Array<String>, 
    private val resIdArray: Array<Int>? = null
) : PopupWindow() {

    companion object {
        /**
         * Option text size in sp units.
         */
        private const val TEXT_SIZE_SP: Float = 14f
        /**
         * Text top/bottom padding in dp units.
         */
        private const val TEXT_PADDING: Float = 7f
    }

    private val binding: PopupOptionPickBinding = PopupOptionPickBinding.inflate(
        LayoutInflater.from(context)
    )

    /**
     * Selection event listener for option changes.
     * 
     * @param position Selected option position in array
     * @param str Selected option text content
     */
    var onPickListener: ((position: Int, str: String) -> Unit)? = null

    init {
        setupDimensions()
        setupRecyclerView()
    }

    /**
     * Calculate and configure popup dimensions based on content.
     */
    private fun setupDimensions() {
        val textView = TextView(context)
        textView.textSize = TEXT_SIZE_SP

        val fontMetrics = textView.paint.fontMetricsInt
        val canSeeItem: Int = strArray.size.coerceAtMost(2)
        val itemHeight: Int = fontMetrics.bottom - fontMetrics.top + SizeUtils.dp2px(TEXT_PADDING) * 2
        val contentHeight = SizeUtils.dp2px(14f) + itemHeight * canSeeItem
        val contentWidth = (contentHeight * 120f / 81f).toInt()

        contentView = binding.root
        width = contentWidth
        height = contentHeight
        isOutsideTouchable = true
    }

    /**
     * Configure RecyclerView with adapter for option display.
     */
    private fun setupRecyclerView() {
        val adapter = OptionAdapter()
        adapter.onItemClickListener = { position ->
            dismiss()
            onPickListener?.invoke(position, strArray[position])
        }
        
        binding.recyclerView.apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    /**
     * Display popup with intelligent positioning relative to anchor view.
     * 
     * @param anchor Reference view for popup positioning
     */
    fun show(anchor: View) {
        val locationArray = IntArray(2)
        anchor.getLocationInWindow(locationArray)

        val x = locationArray[0] + anchor.width - width + SizeUtils.dp2px(5f)

        val yPosition = if (context.resources.displayMetrics.heightPixels - locationArray[1] - anchor.height > height - SizeUtils.dp2px(5f)) {
            // Show below anchor if space available
            locationArray[1] + anchor.height - SizeUtils.dp2px(5f)
        } else {
            // Show above anchor if insufficient space below
            (locationArray[1] - height + SizeUtils.dp2px(5f)).coerceAtLeast(0)
        }
        
        showAtLocation(anchor, Gravity.NO_GRAVITY, x, yPosition)
    }

    /**
     * RecyclerView adapter for option items with ViewBinding patterns.
     */
    private inner class OptionAdapter : RecyclerView.Adapter<OptionAdapter.ViewHolder>() {
        
        /**
         * Item click event listener.
         */
        var onItemClickListener: ((position: Int) -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val textView = MyTextView(context).apply {
                textSize = TEXT_SIZE_SP
                setDrawableHeightPx(SizeUtils.sp2px(18f))
                setTextColor(0xffffffff.toInt())
                setPadding(
                    SizeUtils.dp2px(14f), 
                    SizeUtils.dp2px(TEXT_PADDING), 
                    SizeUtils.dp2px(14f), 
                    SizeUtils.dp2px(TEXT_PADDING)
                )
                compoundDrawablePadding = SizeUtils.dp2px(10f)
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            }
            return ViewHolder(textView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = strArray[position]
            if (resIdArray != null && position < resIdArray.size) {
                holder.textView.setOnlyDrawableStart(resIdArray[position])
            } else {
                holder.textView.setOnlyDrawableStart(0)
            }
        }

        override fun getItemCount(): Int = strArray.size

        /**
         * ViewHolder for option items with click handling.
         */
        inner class ViewHolder(val textView: MyTextView) : RecyclerView.ViewHolder(textView) {
            init {
                textView.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener?.invoke(position)
                    }
                }
            }
        }
    }
}