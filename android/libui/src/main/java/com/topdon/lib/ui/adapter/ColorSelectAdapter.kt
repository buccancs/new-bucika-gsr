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

class ColorSelectAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var listener: ((code: Int, color: Int) -> Unit)? = null
    
    private var type = 0
    
    private var selected = -1

    fun selected(index: Int) {
        selected = index
        notifyDataSetChanged()
    }

    private val colorBean = arrayListOf(
        ColorSelectBean(R.color.color_select1, "#FF000000", 1),
        ColorSelectBean(R.color.color_select2, "#FFFFFFFF", 2),
        ColorSelectBean(R.color.color_select3, "#FF2B79D8", 3),
        ColorSelectBean(R.color.color_select4, "#FFFF0000", 4),
        ColorSelectBean(R.color.color_select5, "#FF0FA752", 5),
        ColorSelectBean(R.color.color_select6, "#FF808080", 6),
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = UiItemColorSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemView(binding)
    }

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

    override fun getItemCount(): Int {
        return colorBean.size
    }

    inner class ItemView(val binding: UiItemColorSelectBinding) : RecyclerView.ViewHolder(binding.root)
