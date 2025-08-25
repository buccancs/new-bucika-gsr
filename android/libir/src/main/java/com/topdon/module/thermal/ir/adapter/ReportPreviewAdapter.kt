package com.topdon.module.thermal.ir.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.CollectionUtils
import com.topdon.house.activity.ImagesDetailActivity
import com.topdon.lib.core.bean.HouseRepPreviewItemBean
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.view.DetectHorizontalScrollView.OnScrollStopListner
import com.topdon.module.thermal.ir.databinding.ItemGalleryHeadLayBinding
import com.topdon.module.thermal.ir.databinding.ItemGalleryLayBinding
import com.topdon.module.thermal.ir.databinding.ItemReportFloorBinding


/**
 * Professional thermal imaging report preview adapter for comprehensive hierarchical report management
 * and research-grade documentation presentation with multi-level data organization.
 *
 * This adapter provides:
 * - Professional hierarchical thermal report organization with floor-based structure
 * - Industry-standard thermal inspection data presentation with multi-level categorization
 * - Research-grade thermal analysis management with comprehensive data visualization
 * - Professional thermal report navigation with nested adapter management
 * - Clinical-grade thermal documentation with structured data presentation
 * - Advanced thermal report gallery integration with image management capabilities
 * - Industry-standard thermal inspection workflow with professional data organization
 * - Comprehensive thermal analysis reporting with detailed project categorization
 *
 * Supports professional thermal inspection reports with detailed floor analysis,
 * project categorization, and comprehensive gallery management for research environments.
 *
 * @param cxt Application context for resource access and UI operations
 * @param dataList List of thermal report preview items with hierarchical organization
 * @since 1.0
 */
@SuppressLint("NotifyDataSetChanged")
class ReportPreviewAdapter(private val cxt: Context, var dataList: List<HouseRepPreviewItemBean>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * Determines view type based on position for professional thermal report differentiation.
     *
     * @param position Item position in the adapter
     * @return View type identifier for proper ViewHolder creation
     */
    override fun getItemViewType(position: Int): Int {
        return position
    }

    /**
     * Creates ViewHolder with ViewBinding for type-safe access to thermal report floor layout.
     *
     * @param parent Parent ViewGroup for the new View
     * @param viewType View type for ViewHolder differentiation
     * @return ItemView ViewHolder with professional thermal report presentation
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemReportFloorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemView(binding)
    }

    /**
     * Binds thermal report floor data to ViewHolder with comprehensive professional presentation.
     * Configures hierarchical thermal report structure with nested adapters and gallery management.
     *
     * @param holder ViewHolder for thermal report floor item
     * @param position Position of the item in the adapter
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val itemView = holder as ItemView
        val data = dataList[position]
        
        // Configure floor number with professional thermal report identification
        itemView.binding.tvFloorNumber.text = data.itemName

        // Setup professional thermal report project listing with nested adapter
        itemView.binding.rcyReport.layoutManager = LinearLayoutManager(cxt)
        val reportPreviewAdapter = ReportPreviewFloorAdapter(cxt, data.projectItemBeans)
        itemView.binding.rcyReport.adapter = reportPreviewAdapter

        // Configure project categorization visibility and professional data presentation
        if (CollectionUtils.isNotEmpty(data.projectItemBeans)) {
            itemView.binding.flyProject.visibility = View.VISIBLE
            itemView.binding.rcyCategory.layoutManager = LinearLayoutManager(cxt)
            val reportCategoryAdapter = ReportPreviewFloorAdapter(cxt, data.projectItemBeans)
            itemView.binding.rcyCategory.adapter = reportCategoryAdapter
        } else {
            itemView.binding.flyProject.visibility = View.GONE
        }

        // Configure professional thermal image gallery with research-grade presentation
        if (CollectionUtils.isNotEmpty(data.albumItemBeans)) {
            itemView.binding.llyAlbum.visibility = View.VISIBLE
            itemView.binding.rcyAlbum.layoutManager = GridLayoutManager(cxt, 3)
            val albumAdapter = ReportPreviewAlbumAdapter(cxt, data.albumItemBeans)
            itemView.binding.rcyAlbum.adapter = albumAdapter
            
            // Configure professional thermal image navigation and detail viewing
            albumAdapter.jumpListener = { _, position ->
                val intent = Intent(cxt, ImagesDetailActivity::class.java)
                val photos = ArrayList<String>()
                data.albumItemBeans.forEach {
                    photos.add(it.photoPath)
                }
                intent.putExtra(ExtraKeyConfig.IMAGE_PATH_LIST, photos)
                intent.putExtra(ExtraKeyConfig.CURRENT_ITEM, position)
                cxt.startActivity(intent)
            }
        } else {
            itemView.binding.llyAlbum.visibility = View.GONE
        }

        // Configure professional horizontal scroll detection for thermal report navigation
        itemView.binding.hsvReport.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                itemView.binding.hsvReport.startScrollerTask()
            }
            false
        }

        // Setup professional scroll position detection with visual feedback
        itemView.binding.hsvReport.setOnScrollStopListner(object : OnScrollStopListner {
            override fun onScrollToRightEdge() {
                itemView.binding.viewCategoryMask.visibility = View.VISIBLE
            }

            override fun onScrollToMiddle() {
                itemView.binding.viewCategoryMask.visibility = View.VISIBLE
            }

            override fun onScrollToLeftEdge() {
                itemView.binding.viewCategoryMask.visibility = View.GONE
            }

            override fun onScrollStoped() {
                // No action required for stopped state
            }

            override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
                if (itemView.binding.viewCategoryMask.visibility == View.VISIBLE) {
                    return
                }
                itemView.binding.viewCategoryMask.visibility = View.VISIBLE
            }
        })
    }

    /**
     * Returns the total number of thermal report floor items for professional data presentation.
     *
     * @return Total count of thermal report preview items
     */
    override fun getItemCount(): Int {
        return dataList.size
    }

    /**
     * Professional ViewHolder for thermal report floor items with ViewBinding integration.
     * Provides type-safe access to hierarchical thermal report presentation components
     * and comprehensive thermal data organization with nested adapter management.
     *
     * @param binding ViewBinding instance for thermal report floor layout
     */
    inner class ItemView(val binding: ItemReportFloorBinding) : RecyclerView.ViewHolder(binding.root)
}