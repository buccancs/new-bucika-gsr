package com.topdon.thermal.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.SizeUtils
import com.topdon.thermal.R
import com.topdon.thermal.databinding.ItemIrConfigEmissivityBinding
import com.topdon.thermal.utils.IRConfigData

class ConfigEmAdapter(val context: Context) : RecyclerView.Adapter<ConfigEmAdapter.ViewHolder>() {
    private val dataList: ArrayList<IRConfigData> = IRConfigData.irConfigData(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemIrConfigEmissivityBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.tvEmName.text = dataList[position].name
        holder.binding.tvEmNum.text = dataList[position].value
        holder.binding.tvEmName.background = EmBgDrawable(false, position == dataList.size - 1)
        holder.binding.tvEmNum.background = EmBgDrawable(true, position == dataList.size - 1)
    }

    override fun getItemCount(): Int = dataList.size

    class ViewHolder(val binding: ItemIrConfigEmissivityBinding) : RecyclerView.ViewHolder(binding.root)

    private class EmBgDrawable(val drawRight: Boolean, val drawBottom: Boolean) : Drawable() {
        private val paint = Paint()

        init {
            paint.color = 0xff5b5961.toInt()
            paint.strokeWidth = SizeUtils.dp2px(1f).coerceAtLeast(1).toFloat()
        }

        override fun draw(canvas: Canvas) {
            canvas.drawLine(0f, 0f, 0f, bounds.bottom.toFloat(), paint)
            canvas.drawLine(0f, 0f, bounds.right.toFloat(), 0f, paint)
            if (drawRight) {
                canvas.drawLine(bounds.right.toFloat(), 0f, bounds.right.toFloat(), bounds.bottom.toFloat(), paint)
            }
            if (drawBottom) {
                canvas.drawLine(0f, bounds.bottom.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), paint)
            }
        }

        override fun setAlpha(alpha: Int) {
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
        }

        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
