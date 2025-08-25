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

class TargetItemAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    var listener: ((index: Int, code: Int) -> Unit)? = null
    
    private var selected = -1

    fun selected(index: Int) {
        selected = index
        notifyDataSetChanged()
    }

    fun getSelected(): Int = selected

    private val targetShapes = arrayListOf(
        ColorBean(R.drawable.ic_menu_thermal6002, "", ObserveBean.TYPE_TARGET_HORIZONTAL),
        ColorBean(R.drawable.ic_menu_thermal6001, "", ObserveBean.TYPE_TARGET_VERTICAL),
        ColorBean(R.drawable.ic_menu_thermal6003, "", ObserveBean.TYPE_TARGET_CIRCLE),
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItmeTargetModeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemView(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemView) {
            val targetShape = targetShapes[position]
            
            with(holder.binding) {

                itemMenuTabImg.setImageResource(targetShape.res)
                itemMenuTabImg.isSelected = targetShape.code == selected
                
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
                
                itemMenuTabLay.setOnClickListener {
                    listener?.invoke(position, targetShape.code)
                    selected(targetShape.code)
                }
            }
        }
    }

    override fun getItemCount(): Int = targetShapes.size

    inner class ItemView(val binding: ItmeTargetModeBinding) : RecyclerView.ViewHolder(binding.root)
