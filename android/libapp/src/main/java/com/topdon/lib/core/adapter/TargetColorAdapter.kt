package com.topdon.lib.core.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * Minimal stub for TargetColorAdapter
 * Maintains interface compatibility with IRCamera
 */
class TargetColorAdapter(private val context: Context) : RecyclerView.Adapter<TargetColorAdapter.ViewHolder>() {
    
    var onItemClickListener: ((position: Int) -> Unit)? = null
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(View(context))
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Stub implementation
    }
    
    override fun getItemCount(): Int = 0
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}