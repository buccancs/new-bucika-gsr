package com.topdon.module.thermal.ir.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.topdon.house.activity.ImagesDetailActivity
import com.topdon.lib.core.bean.GalleryBean
import com.topdon.lib.core.bean.HouseRepPreviewAlbumItemBean
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.ui.widget.RoundImageView
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.report.bean.ReportData
import com.topdon.module.thermal.ir.databinding.ItemReportAlbumChildBinding

/**
 * Professional thermal imaging report album adapter for comprehensive thermal image gallery
 * management and research-grade documentation presentation with advanced image handling.
 *
 * This adapter provides:
 * - Professional thermal image gallery presentation with rounded image views
 * - Industry-standard thermal image loading with Glide integration for optimal performance
 * - Research-grade thermal documentation with comprehensive image metadata display
 * - Professional thermal image navigation with click handler management
 * - Clinical-grade thermal image organization with structured album presentation
 * - Advanced thermal report gallery integration with detail view navigation
 * - Industry-standard thermal documentation workflow with professional image presentation
 * - Comprehensive thermal analysis reporting with visual evidence management
 *
 * Supports professional thermal inspection documentation with detailed image galleries,
 * thermal analysis visualization, and research-grade documentation for clinical environments.
 *
 * @param cxt Application context for resource access and Glide image loading operations
 * @param dataList List of thermal report preview album items with image metadata
 * @since 1.0
 */
@SuppressLint("NotifyDataSetChanged")
class ReportPreviewAlbumAdapter(
    private val cxt: Context,
    private var dataList: List<HouseRepPreviewAlbumItemBean>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /** Jump listener for professional thermal image detail navigation and comprehensive image viewing */
    var jumpListener: ((item: HouseRepPreviewAlbumItemBean, position: Int) -> Unit)? = null
    
    /**
     * Determines view type based on position for professional thermal album differentiation.
     *
     * @param position Item position in the adapter
     * @return View type identifier for proper ViewHolder creation
     */
    override fun getItemViewType(position: Int): Int {
        return position
    }

    /**
     * Creates ViewHolder with ViewBinding for type-safe access to thermal album child layout.
     *
     * @param parent Parent ViewGroup for the new View
     * @param viewType View type for ViewHolder differentiation
     * @return ItemView ViewHolder with professional thermal album presentation
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemReportAlbumChildBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemView(binding)
    }

    /**
     * Binds thermal album image data to ViewHolder with professional Glide image loading.
     * Configures thermal image presentation and navigation with click handler management.
     *
     * @param holder ViewHolder for thermal album child item
     * @param position Position of the item in the adapter
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val itemView = holder as ItemView
        val bean = dataList[position]
        
        // Load thermal image with professional Glide optimization
        Glide.with(cxt).load(bean.photoPath).into(itemView.binding.rivPhoto)
        
        // Configure thermal image title with professional presentation
        itemView.binding.tvName.text = bean.title
        
        // Setup professional thermal image detail navigation
        itemView.binding.rivPhoto.setOnClickListener {
            jumpListener?.invoke(bean, position)
        }
    }

    /**
     * Returns the total number of thermal album items for professional gallery presentation.
     *
     * @return Total count of thermal album preview items
     */
    override fun getItemCount(): Int {
        return dataList.size
    }

    /**
     * Professional ViewHolder for thermal album child items with ViewBinding integration.
     * Provides type-safe access to thermal image gallery components and professional
     * thermal documentation presentation with comprehensive image handling support.
     *
     * @param binding ViewBinding instance for thermal album child layout
     */
    inner class ItemView(val binding: ItemReportAlbumChildBinding) : RecyclerView.ViewHolder(binding.root)
}