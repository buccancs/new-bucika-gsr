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

class OptionPickPopup(
    private val context: Context, 
    private val strArray: Array<String>, 
    private val resIdArray: Array<Int>? = null
) : PopupWindow() {

    companion object {
        
        private const val TEXT_SIZE_SP: Float = 14f
        
        private const val TEXT_PADDING: Float = 7f
    }

    private val binding: PopupOptionPickBinding = PopupOptionPickBinding.inflate(
        LayoutInflater.from(context)
    )

    var onPickListener: ((position: Int, str: String) -> Unit)? = null

    init {
        setupDimensions()
        setupRecyclerView()
    }

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

    fun show(anchor: View) {
        val locationArray = IntArray(2)
        anchor.getLocationInWindow(locationArray)

        val x = locationArray[0] + anchor.width - width + SizeUtils.dp2px(5f)

        val yPosition = if (context.resources.displayMetrics.heightPixels - locationArray[1] - anchor.height > height - SizeUtils.dp2px(5f)) {

            locationArray[1] + anchor.height - SizeUtils.dp2px(5f)
        } else {

            (locationArray[1] - height + SizeUtils.dp2px(5f)).coerceAtLeast(0)
        }
        
        showAtLocation(anchor, Gravity.NO_GRAVITY, x, yPosition)
    }

    private inner class OptionAdapter : RecyclerView.Adapter<OptionAdapter.ViewHolder>() {
        
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
