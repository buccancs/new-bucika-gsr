package com.topdon.module.user.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.topdon.module.user.R
import kotlinx.android.synthetic.main.item_language.view.*

class LanguageAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var listener: ItemOnClickListener? = null

    private var selectIndex = 0
    private var languages: Array<out CharSequence> = context.resources.getTextArray(R.array.setting_language_list)

    fun setSelect(index: Int) {
        selectIndex = index
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder(LayoutInflater.from(context).inflate(R.layout.item_language, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            if (position == selectIndex) {
                holder.img.visibility = View.VISIBLE
            } else {
                holder.img.visibility = View.INVISIBLE
            }
            holder.name.text = languages[position]
            holder.lay.setOnClickListener {
                listener?.onClick(position)
            }
        }
    }

    override fun getItemCount(): Int {
        return languages.size
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var lay: View = itemView.item_language_lay
        var name: TextView = itemView.item_language_text
        var img: ImageView = itemView.item_language_img
    }

    interface ItemOnClickListener {
        fun onClick(position: Int)
    }
}