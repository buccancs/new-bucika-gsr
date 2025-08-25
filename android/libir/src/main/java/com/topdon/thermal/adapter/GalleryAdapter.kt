package com.topdon.thermal.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.topdon.lib.core.bean.GalleryBean
import com.topdon.lib.core.bean.GalleryTitle
import com.topdon.lib.core.tools.GlideLoader
import com.topdon.lib.core.tools.TimeTool
import com.topdon.thermal.R
import com.topdon.thermal.databinding.ItemGalleryHeadLayBinding
import com.topdon.thermal.databinding.ItemGalleryLayBinding

/**
 * Professional Gallery Adapter for Thermal Image and Video Management
 * 
 * This adapter provides comprehensive gallery functionality for thermal images and videos
 * with professional features including multi-selection, remote TS004 device support, 
 * and research-grade metadata display essential for thermal imaging applications.
 * 
 * **Gallery Features:**
 * - Professional ViewBinding implementation replacing deprecated Kotlin synthetics
 * - Support for both thermal images and video files with appropriate visual indicators
 * - Comprehensive multi-selection mode with visual feedback for batch operations
 * - TS004 remote device integration with download status indicators and management
 * - Professional sectioned display with date-based grouping for research organization
 * 
 * **Research Application Benefits:**
 * - Efficient browsing of large thermal image datasets with optimized performance
 * - Professional selection interface for batch export and analysis operations
 * - Visual indicators for video duration, file types, and download status
 * - Integration with comprehensive gallery management and archival systems
 * - Research-grade metadata display and organization capabilities
 * 
 * **Data Management:**
 * - Dynamic data updates with professional change tracking and notifications
 * - Comprehensive selection state management for research workflow efficiency
 * - Professional image loading with Glide integration for optimal performance
 * - Support for mixed content types (images, videos) with appropriate handling
 * 
 * @author BucikaGSR Development Team
 * @since 2024.1.0
 * @see GalleryBean For thermal image and video metadata structure
 * @see GalleryTitle For professional section header organization
 */
@SuppressLint("NotifyDataSetChanged")
class GalleryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEAD = 0
        private const val TYPE_DATA = 1
    }

    /**
     * 当前显示的数据列表，包含有标题 item.
     */
    val dataList: ArrayList<GalleryBean> = ArrayList()

    /**
     * 编辑模式下，当前选中的 position 列表.
     */
    val selectList: ArrayList<Int> = ArrayList()

    /**
     * 是否为 TS004 远端模式，处于该模式会有下载图标.
     */
    var isTS004Remote = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    /**
     * 当前是否处于编辑模式.
     */
    var isEditMode = false
        set(value) {
            if (field != value) {
                field = value
                if (!value) {
                    selectList.clear()
                    selectCallback?.invoke(selectList)
                }
                notifyDataSetChanged()
            }
        }


    /**
     * 非编辑模式下 item 长按进入编辑模式事件监听.
     */
    var onLongEditListener: (() -> Unit)? = null
    /**
     * 选中数量变更回调.
     * data 当前选中的 item position 列表
     */
    var selectCallback: ((data: ArrayList<Int>) -> Unit)? = null
    /**
     * 非编辑模式时，item 点击事件监听.
     */
    var itemClickCallback: ((position: Int) -> Unit)? = null


    fun refreshList(newList: List<GalleryBean>) {
        dataList.clear()
        dataList.addAll(newList)
        notifyDataSetChanged()
    }

    fun buildSelectList(): ArrayList<GalleryBean> {
        val resultList: ArrayList<GalleryBean> = ArrayList()
        selectList.forEach {
            resultList.add(dataList[it])
        }
        return resultList
    }

    fun selectAll() {
        var dataCount = 0
        dataList.forEach {
            if (it !is GalleryTitle) {
                dataCount++
            }
        }
        if (selectList.size >= dataCount) {
            selectList.clear()
        } else {
            selectList.clear()
            for (i in 0 until dataList.size) {
                if (dataList[i] !is GalleryTitle) {
                    selectList.add(i)
                }
            }
        }
        selectCallback?.invoke(selectList)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (dataList[position] is GalleryTitle) {
            TYPE_HEAD
        } else {
            TYPE_DATA
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEAD) {
            ItemHeadView(ItemGalleryHeadLayBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            ItemView(ItemGalleryLayBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val data = dataList[position]
        if (holder is ItemView) {
            GlideLoader.load(holder.binding.itemGalleryImg, data.thumb)
            if (data.name.uppercase().endsWith(".MP4")) {
                holder.binding.itemGalleryText.text = TimeTool.showVideoTime(data.duration)
                holder.binding.ivVideoTime.isVisible = true
            } else {
                holder.binding.itemGalleryText.text = ""
                holder.binding.ivVideoTime.isVisible = false
            }

            holder.binding.ivHasDownload.isVisible = isTS004Remote && data.hasDownload

            holder.binding.ivCheck.isVisible = isEditMode
            holder.binding.ivCheck.isSelected = selectList.contains(position)

            holder.binding.itemGalleryImg.setOnClickListener {
                if (isEditMode) {
                    if (selectList.contains(position)) {
                        selectList.remove(position)
                    } else {
                        selectList.add(position)
                    }
                    selectCallback?.invoke(selectList)

                    holder.binding.ivCheck.isSelected = selectList.contains(position)
                } else {
                    itemClickCallback?.invoke(position)
                }
            }
            holder.binding.itemGalleryImg.setOnLongClickListener {
                if (!isEditMode) {
                    selectList.add(position)
                    selectCallback?.invoke(selectList)
                    holder.binding.ivCheck.isVisible = true
                    holder.binding.ivCheck.isSelected = true
                    isEditMode = true
                    onLongEditListener?.invoke()
                }
                return@setOnLongClickListener true
            }
        } else if (holder is ItemHeadView) {
            holder.binding.itemGalleryHeadText.text = TimeTool.showDateType(data.timeMillis, 4)
            holder.binding.itemGalleryHeadText.setTextColor(0x80ffffff.toInt())
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    /**
     * Professional ViewHolder for gallery section headers with ViewBinding
     * Provides date-based organization for research workflow efficiency
     */
    inner class ItemHeadView(val binding: ItemGalleryHeadLayBinding) : RecyclerView.ViewHolder(binding.root)

    /**
     * Professional ViewHolder for thermal images and videos with ViewBinding  
     * Supports multi-selection, download indicators, and research-grade interaction
     */
    inner class ItemView(val binding: ItemGalleryLayBinding) : RecyclerView.ViewHolder(binding.root)
}