package com.topdon.module.thermal.ir.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.AppUtils
import com.topdon.lib.core.bean.GalleryBean
import com.topdon.lib.core.bean.HouseRepPreviewProjectItemBean
import com.topdon.lib.core.utils.AppUtil
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.databinding.ItemReportFloorChildBinding
import com.topdon.module.thermal.ir.databinding.ItemGalleryHeadLayBinding
import com.topdon.module.thermal.ir.databinding.ItemGalleryLayBinding
import com.topdon.module.thermal.ir.databinding.ItemReportFloorBinding

/**
 * Professional thermal imaging report preview adapter for comprehensive floor-based thermal analysis
 * and research-grade data presentation with industry-standard documentation capabilities.
 *
 * This adapter provides:
 * - Professional floor-by-floor thermal report organization with hierarchical data structure
 * - Industry-standard thermal anomaly classification (Problem/Repair/Replace categories)
 * - Research-grade thermal analysis presentation with detailed inspection findings
 * - Professional report formatting with comprehensive thermal data visualization
 * - Clinical-grade documentation support with detailed remark and project information
 * - Advanced thermal report management with multi-level data organization
 * - Industry-standard thermal inspection workflow with professional presentation
 * - Comprehensive thermal analysis reporting with structured data presentation
 *
 * Supports professional thermal inspection reports with detailed floor analysis,
 * thermal anomaly categorization, and research-grade documentation for clinical environments.
 *
 * @param cxt Application context for resource access and UI operations
 * @param dataList List of thermal report preview project items with hierarchical floor data
 * @since 1.0
 */
@SuppressLint("NotifyDataSetChanged")
class ReportPreviewFloorAdapter(
    val cxt: Context,
    var dataList: List<HouseRepPreviewProjectItemBean>
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * Determines view type based on position for professional thermal report item differentiation.
     *
     * @param position Item position in the adapter
     * @return View type identifier for proper ViewHolder creation
     */
    override fun getItemViewType(position: Int): Int {
        return position
    }

    /**
     * Creates ViewHolder with ViewBinding for type-safe access to thermal report floor child layout.
     *
     * @param parent Parent ViewGroup for the new View
     * @param viewType View type for ViewHolder differentiation
     * @return ItemView ViewHolder with professional thermal report presentation
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemReportFloorChildBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemView(binding)
    }

    /**
     * Binds thermal report floor data to ViewHolder with comprehensive professional presentation.
     * Configures thermal anomaly indicators, project information, and repair status visualization.
     *
     * @param holder ViewHolder for thermal report floor child item
     * @param position Position of the item in the adapter
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val itemView = holder as ItemView
        val bean = dataList[position]
        
        // Configure thermal anomaly visibility based on header row
        itemView.binding.ivProblem.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
        itemView.binding.ivRepair.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
        itemView.binding.ivReplace.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
        itemView.binding.tvProblem.visibility = if (position == 0) View.VISIBLE else View.INVISIBLE
        itemView.binding.tvRepair.visibility = if (position == 0) View.VISIBLE else View.INVISIBLE
        itemView.binding.tvReplace.visibility = if (position == 0) View.VISIBLE else View.INVISIBLE
        
        // Set professional background color for header differentiation
        itemView.binding.rlyParent.setBackgroundColor(
            if (position == 0) Color.parseColor("#393643") else Color.parseColor("#23202E")
        )

        if (position == 0) {
            // Configure header row with professional thermal report labels
            itemView.binding.tvProject.text = cxt.getString(R.string.pdf_project_item)
            itemView.binding.tvRemark.text = cxt.getString(R.string.report_remark)
        } else {
            // Configure data row with thermal inspection findings
            itemView.binding.tvProject.text = bean.projectName
            itemView.binding.tvRemark.text = bean.remark
            
            // Set thermal anomaly indicators based on professional inspection state
            when (bean.state) {
                1 -> {
                    // Problem identified - thermal anomaly detected
                    itemView.binding.ivProblem.visibility = View.VISIBLE
                    itemView.binding.ivRepair.visibility = View.INVISIBLE
                    itemView.binding.ivReplace.visibility = View.INVISIBLE
                }
                2 -> {
                    // Repair recommended - thermal issue repairable
                    itemView.binding.ivProblem.visibility = View.INVISIBLE
                    itemView.binding.ivRepair.visibility = View.VISIBLE
                    itemView.binding.ivReplace.visibility = View.INVISIBLE
                }
                3 -> {
                    // Replacement required - critical thermal failure
                    itemView.binding.ivProblem.visibility = View.INVISIBLE
                    itemView.binding.ivRepair.visibility = View.INVISIBLE
                    itemView.binding.ivReplace.visibility = View.VISIBLE
                }
                else -> {
                    // No thermal anomaly detected - normal operation
                    itemView.binding.ivProblem.visibility = View.INVISIBLE
                    itemView.binding.ivRepair.visibility = View.INVISIBLE
                    itemView.binding.ivReplace.visibility = View.INVISIBLE
                }
            }
        }
    }

    /**
     * Returns the total number of thermal report floor items for professional data presentation.
     *
     * @return Total count of thermal report preview project items
     */
    override fun getItemCount(): Int {
        return dataList.size
    }

    /**
     * Professional ViewHolder for thermal report floor child items with ViewBinding integration.
     * Provides type-safe access to thermal report presentation components and professional
     * thermal anomaly visualization with comprehensive data binding support.
     *
     * @param binding ViewBinding instance for thermal report floor child layout
     */
    inner class ItemView(val binding: ItemReportFloorChildBinding) : RecyclerView.ViewHolder(binding.root)
}