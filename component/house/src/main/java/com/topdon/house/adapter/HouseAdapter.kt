package com.topdon.house.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.topdon.house.R
import com.topdon.lib.core.db.entity.HouseBase
import com.topdon.lib.core.tools.GlideLoader
import com.topdon.lib.core.tools.TimeTool
import kotlinx.android.synthetic.main.item_house_list.view.*

/**
 * 检测 及 报告 列表所用 Adapter.
 *
 * Created by LCG on 2024/8/28.
 */
@SuppressLint("NotifyDataSetChanged")
internal class HouseAdapter(val context: Context, val isDetect: Boolean) : RecyclerView.Adapter<HouseAdapter.ViewHolder>() {
    var dataList: ArrayList<HouseBase> = ArrayList()

    /**
     * 当前是否处于编辑模式.
     */
    var isEditMode: Boolean = false
        set(value) {
            field = value
            selectIndexList.clear()
            onSelectChangeListener?.invoke(0)
            notifyItemRangeChanged(0, itemCount)
        }
    /**
     * 仅当处于编辑模式时，当前选中的 item index 列表.
     */
    var selectIndexList: ArrayList<Int> = ArrayList()

    /**
     * 更多被点击事件监听.
     */
    var onMoreClickListener: ((position: Int, v: View) -> Unit)? = null
    /**
     * 仅报告列表时，分享被点击事件监听.
     */
    var onShareClickListener: ((position: Int) -> Unit)? = null
    /**
     * item 点击事件监听.
     */
    var onItemClickListener: ((position: Int) -> Unit)? = null
    /**
     * 一个 item 选中或取消选中事件监听.
     */
    var onSelectChangeListener: ((selectSize: Int) -> Unit)? = null

    /**
     * 使用指定的检测数据刷新整个列表.
     */
    fun refresh(newList: List<HouseBase>) {
        dataList.clear()
        dataList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_house_list, parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val houseBase: HouseBase = dataList[position]

        holder.itemView.iv_menu_more.isVisible = !isEditMode
        holder.itemView.iv_select.isVisible = isEditMode
        holder.itemView.iv_select.isSelected = selectIndexList.contains(position)

        holder.itemView.tv_address.text = houseBase.address
        holder.itemView.tv_name.text = houseBase.name
        holder.itemView.tv_detect_time.text = TimeTool.formatDetectTime(houseBase.detectTime)

        holder.itemView.view_space_year.isVisible = houseBase.year != null || houseBase.houseSpace.isNotEmpty()

        holder.itemView.tv_house_year.isVisible = houseBase.year != null
        holder.itemView.tv_house_year.text = houseBase.year.toString()

        holder.itemView.tv_house_space.isVisible = houseBase.houseSpace.isNotEmpty()
        holder.itemView.tv_house_space.text = "${houseBase.houseSpace}${houseBase.getSpaceUnitStr()}"

        holder.itemView.tv_cost.isVisible = houseBase.cost.isNotEmpty()
        holder.itemView.tv_cost_unit.isVisible = houseBase.cost.isNotEmpty()
        holder.itemView.tv_cost.text = houseBase.cost
        holder.itemView.tv_cost_unit.text = houseBase.getCostUnitStr()

        holder.itemView.tv_detect_share.setText(if (isDetect) R.string.app_detection else R.string.battery_share)

        GlideLoader.load(holder.itemView.iv_house_image, houseBase.imagePath)
    }

    override fun getItemCount(): Int = dataList.size

    inner class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
        init {
            rootView.iv_menu_more.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onMoreClickListener?.invoke(position, it)
                }
            }
            rootView.setOnClickListener {
                if (isEditMode) {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        if (selectIndexList.contains(position)) {//选中->未选中
                            selectIndexList.remove(position)
                            rootView.iv_select.isSelected = false
                        } else {//未选中->选中
                            selectIndexList.add(position)
                            rootView.iv_select.isSelected = true
                        }
                        onSelectChangeListener?.invoke(selectIndexList.size)
                    }
                } else {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener?.invoke(position)
                    }
                }
            }
            if (!isDetect) {
                rootView.tv_detect_share.setOnClickListener {
                    if (!isEditMode) {//编辑模式不响应分享事件
                        val position = bindingAdapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            onShareClickListener?.invoke(position)
                        }
                    }
                }
            }
        }
    }
}