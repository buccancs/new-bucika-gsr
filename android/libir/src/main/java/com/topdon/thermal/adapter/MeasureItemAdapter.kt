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

class MeasureItemAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    var listener: ((index: Int, code: Int) -> Unit)? = null
    
    private var selected = -1

    fun selected(index: Int) {
        selected = index
        notifyDataSetChanged()
    }

    private val measurementTargets = arrayListOf(
        ColorBean(R.drawable.ic_menu_thermal7001, "1.8m", ObserveBean.TYPE_MEASURE_PERSON),
        ColorBean(R.drawable.ic_menu_thermal7002, "1.0m", ObserveBean.TYPE_MEASURE_SHEEP),
        ColorBean(R.drawable.ic_menu_thermal7003, "0.5m", ObserveBean.TYPE_MEASURE_DOG),
        ColorBean(R.drawable.ic_menu_thermal7004, "0.2m", ObserveBean.TYPE_MEASURE_BIRD),
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItmeTargetModeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemView(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemView) {
            val target = measurementTargets[position]
            
            with(holder.binding) {

                itemMenuTabImg.setImageResource(target.res)
                itemMenuTabText.apply {
                    visibility = View.VISIBLE
                    text = target.name
                    isSelected = target.code == selected
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                }
                
                itemMenuTabImg.isSelected = target.code == selected
                
                itemMenuTabLay.setOnClickListener {
                    listener?.invoke(position, target.code)
                    selected(target.code)
                }
            }
        }
    }

    override fun getItemCount(): Int = measurementTargets.size

    inner class ItemView(val binding: ItmeTargetModeBinding) : RecyclerView.ViewHolder(binding.root)
