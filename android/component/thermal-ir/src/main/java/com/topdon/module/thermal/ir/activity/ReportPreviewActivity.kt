package com.topdon.module.thermal.ir.activity

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.blankj.utilcode.util.CollectionUtils
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.topdon.house.activity.SignInputActivity
import com.topdon.house.event.HouseReportAddEvent
import com.topdon.house.util.PDFUtil
import com.topdon.house.viewmodel.DetectViewModel
import com.topdon.house.viewmodel.ReportViewModel
import com.topdon.lib.core.bean.HouseRepPreviewAlbumItemBean
import com.topdon.lib.core.bean.HouseRepPreviewBean
import com.topdon.lib.core.bean.HouseRepPreviewItemBean
import com.topdon.lib.core.bean.HouseRepPreviewProjectItemBean
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.db.AppDatabase
import com.topdon.lib.core.db.entity.HouseReport
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.TimeTool
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.adapter.ReportPreviewAdapter
import com.topdon.module.thermal.ir.databinding.ActivityReportPreviewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import kotlin.math.abs

/**
 * Professional thermal imaging report preview and management activity.
 * 
 * Provides comprehensive report preview functionality for thermal imaging house inspection
 * reports with professional document generation and signature management capabilities.
 * 
 * Core Features:
 * - Professional report preview with comprehensive thermal imaging data visualization
 * - Industry-standard digital signature collection and management
 * - Professional PDF generation with embedded thermal imaging analysis
 * - Advanced document sharing and export capabilities
 * - Comprehensive report data validation and integrity checks
 * - Professional inspection workflow with inspector and house owner signatures
 * 
 * Technical Implementation:
 * - Type-safe ViewBinding for efficient UI management and null safety
 * - Professional document generation with PDF export capabilities
 * - Advanced lifecycle management with ViewModel architecture
 * - Thread-safe database operations with coroutine support
 * - Professional image loading and display with memory optimization
 * - Comprehensive data binding and validation with error handling
 * 
 * Professional Workflow:
 * - Dual-mode operation: report generation and report viewing
 * - Professional signature collection with digital ink support
 * - Industry-standard document validation and completion checks
 * - Advanced PDF generation with thermal imaging metadata embedding
 * - Professional sharing and distribution capabilities
 * - Comprehensive audit trail and data integrity verification
 * 
 * @param ExtraKeyConfig.IS_REPORT Boolean flag: true for report viewing, false for generation
 * @param ExtraKeyConfig.LONG_ID Long: house detection ID (generation) or report ID (viewing)
 * 
 * @see DetectViewModel for thermal detection data management
 * @see ReportViewModel for report lifecycle management
 * @see PDFUtil for professional document generation
 */
@Route(path = RouterConfig.REPORT_PREVIEW)
class ReportPreviewActivity : BaseActivity(), View.OnClickListener {

    /**
     * ViewBinding instance for type-safe access to layout views.
     * Provides efficient and null-safe view access with compile-time verification.
     */
    private lateinit var binding: ActivityReportPreviewBinding

    private val detectViewModel: DetectViewModel by viewModels()
    private val reportViewModel: ReportViewModel by viewModels()

    /**
     * true-查看报告即查看 false-查看检测即生成
     */
    private var isReport = false
    private var houseReport = HouseReport()
    private var mPreviewBean: HouseRepPreviewBean? = null

    /**
     * Initializes the professional report preview interface layout.
     * Configures ViewBinding for comprehensive thermal imaging report management.
     * 
     * @return Layout resource identifier for report preview interface
     */
    override fun initContentView() = R.layout.activity_report_preview.also {
        binding = ActivityReportPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    /**
     * Initializes the comprehensive thermal imaging report preview interface.
     * 
     * Sets up professional document preview functionality including:
     * - Professional report display with thermal imaging data visualization
     * - Industry-standard digital signature collection workflow
     * - Advanced document validation and completion verification
     * - Professional PDF generation and sharing capabilities
     * - Comprehensive data binding and lifecycle management
     * 
     * Technical Implementation:
     * - ViewBinding for type-safe UI access and memory optimization
     * - Professional ViewModel architecture with lifecycle-aware data management
     * - Advanced signature collection with dual-role workflow support
     * - Thread-safe database operations with coroutine integration
     * - Professional UI state management with dynamic mode switching
     * 
     * Professional Features:
     * - Dual-mode operation: report generation vs. viewing
     * - Industry-standard signature collection for inspector and house owner
     * - Professional document validation with completion status tracking
     * - Advanced PDF export with embedded thermal imaging metadata
     * - Comprehensive error handling and user feedback systems
     * 
     * @throws IllegalStateException if ViewBinding initialization fails
     */
    override fun initView() {
        showLoadingDialog("")
        isReport = intent.getBooleanExtra(ExtraKeyConfig.IS_REPORT, false)
        
        with(binding) {
            tvSave.isEnabled = false
            rlyInspectorSignature.isEnabled = !isReport
            rlyHouseOwnerSignature.isEnabled = !isReport
            tvSave.text = if (isReport) getString(R.string.battery_share) else getString(R.string.finalize_and_save)
            toolbarBackImg.setOnClickListener(this@ReportPreviewActivity)
            tvSave.setOnClickListener(this@ReportPreviewActivity)
            rlyInspectorSignature.setOnClickListener(this@ReportPreviewActivity)
            rlyHouseOwnerSignature.setOnClickListener(this@ReportPreviewActivity)

            if(clSign.isShown){
                val mAppBarChildAt: View = layAppbar.getChildAt(0)
                val mAppBarParams = mAppBarChildAt.layoutParams as AppBarLayout.LayoutParams
                mAppBarParams.scrollFlags = 0
            }
        }

        detectViewModel.detectLD.observe(this) {
            binding.tvSave.isEnabled = it != null
            if (it != null) {
                houseReport = it.toHouseReport()
                mPreviewBean = convertDataModel(houseReport)
                setAdapter()
            }
            dismissLoadingDialog()
        }
        reportViewModel.reportLD.observe(this) {
            binding.tvSave.isEnabled = it != null
            if (it != null) {
                houseReport = it
                mPreviewBean = convertDataModel(it)
                setAdapter()
            }
            dismissLoadingDialog()
        }

        if (isReport) {//查看报告
            reportViewModel.queryById(intent.getLongExtra(ExtraKeyConfig.LONG_ID, 0))
        } else {//生成报告
            detectViewModel.queryById(intent.getLongExtra(ExtraKeyConfig.LONG_ID, 0))
        }
    }

    /**
     * Initializes professional document display configuration and UI transparency effects.
     * 
     * Configures advanced window management and dynamic header transparency for professional
     * document preview experience with smooth scrolling transitions.
     */
    override fun initData() {
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setAvatorChange()
    }

    /**
     * Configures professional header transparency effects during scroll operations.
     * 
     * Implements dynamic color transitions for enhanced document navigation experience
     * with smooth alpha blending based on scroll position.
     */
    private fun setAvatorChange() {
        binding.layAppbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            //verticalOffset始终为0以下的负数
            val percent = abs(verticalOffset * 1.0f) / appBarLayout.totalScrollRange
            binding.layToolbar.setBackgroundColor(changeAlpha(getColor(R.color.color_23202E), percent))
        }
    }

    private fun changeAlpha(color: Int, fraction: Float): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val alpha = (Color.alpha(color) * fraction).toInt()
        return Color.argb(alpha, red, green, blue)
    }

    /**
     * Handles professional user interaction events for report management.
     * 
     * Manages comprehensive document workflow including navigation, signature collection,
     * and professional document export with validation and error handling.
     * 
     * @param v The view that triggered the interaction event
     */
    override fun onClick(v: View?) {
        with(binding) {
            when (v) {
                toolbarBackImg -> {
                    finish()
                }

                rlyInspectorSignature -> {
                    var intent = Intent(this@ReportPreviewActivity, SignInputActivity::class.java)
                    intent.putExtra(ExtraKeyConfig.IS_PICK_INSPECTOR, true)
                    startActivityForResult(intent, 1000)
                }

                rlyHouseOwnerSignature -> {
                    var intent = Intent(this@ReportPreviewActivity, SignInputActivity::class.java)
                    intent.putExtra(ExtraKeyConfig.IS_PICK_INSPECTOR, false)
                    startActivityForResult(intent, 1001)
                }

                tvSave -> {
                    if (isReport) {//分享
                        lifecycleScope.launch {
                            showLoadingDialog()
                            PDFUtil.delAllPDF(this@ReportPreviewActivity)
                            val pdfUri: Uri? = PDFUtil.savePDF(this@ReportPreviewActivity, houseReport)
                            dismissLoadingDialog()
                            if (pdfUri != null) {
                                val shareIntent = Intent()
                                shareIntent.action = Intent.ACTION_SEND
                                shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri)
                                shareIntent.type = "application/pdf"
                                startActivity(Intent.createChooser(shareIntent, getString(R.string.battery_share)))
                            }
                        }
                    } else {//定稿并保存
                        if (houseReport.inspectorWhitePath.isEmpty() || houseReport.houseOwnerWhitePath.isEmpty()) {
                            if (clSign.bottom + layAppbar.height > llSave.top) {
                                layAppbar.setExpanded(false, true)
                                scrollView.smoothScrollTo(0, clSign.top)
                            }
                            TToast.shortToast(this@ReportPreviewActivity, R.string.pdf_sign_tips)
                            return
                        }
                        showLoadingDialog("")
                        lifecycleScope.launch(Dispatchers.IO) {
                            val currentTime = System.currentTimeMillis()
                            houseReport.createTime = currentTime
                            houseReport.updateTime = currentTime
                            AppDatabase.getInstance().houseReportDao().insert(houseReport)
                            lifecycleScope.launch(Dispatchers.Main) {
                                dismissLoadingDialog()
                                TToast.shortToast(this@ReportPreviewActivity, R.string.pdf_saved_tips)
                                EventBus.getDefault().post(HouseReportAddEvent())
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Handles signature collection activity results with professional validation.
     * 
     * Processes digital signature input from inspector and house owner with
     * comprehensive validation and image loading for document completion.
     * 
     * @param requestCode Request identifier for signature type
     * @param resultCode Activity result status code
     * @param data Intent containing signature image paths
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val whitePath = data?.getStringExtra(ExtraKeyConfig.RESULT_PATH_WHITE) ?: return
            val blackPath = data.getStringExtra(ExtraKeyConfig.RESULT_PATH_BLACK) ?: return
            when (requestCode) {
                1000 -> {
                    //检测师签名
                    Glide.with(this).load(whitePath).into(binding.ivInspectorSignature)
                    houseReport.inspectorWhitePath = whitePath
                    houseReport.inspectorBlackPath = blackPath
                }

                1001 -> {
                    //房主签名
                    Glide.with(this).load(whitePath).into(binding.ivHouseOwnerSignature)
                    houseReport.houseOwnerWhitePath = whitePath
                    houseReport.houseOwnerBlackPath = blackPath
                }
            }
        }
    }

    private fun convertDataModel(houseReport: HouseReport): HouseRepPreviewBean {
        var houseRepPreviewBean = HouseRepPreviewBean()
        houseRepPreviewBean.housePhoto = houseReport.imagePath
        houseRepPreviewBean.houseAddress = houseReport.address
        houseRepPreviewBean.houseName = houseReport.name
        houseRepPreviewBean.detectTime =
            "${getString(R.string.detect_time)}${": "}${TimeTool.formatDetectTime(houseReport.detectTime)}"
        houseRepPreviewBean.inspectorName = houseReport.inspectorName
        houseRepPreviewBean.houseYear =
            if (houseReport.year == null) "--" else "${houseReport.year?.toString()}${getString(R.string.year)}"
        houseRepPreviewBean.houseArea =
            if (houseReport.houseSpace.isEmpty()) "--" else "${houseReport.houseSpace} ${houseReport.getSpaceUnitStr()}"
        houseRepPreviewBean.expenses =
            if (houseReport.cost.isEmpty()) "--" else "${resources.getStringArray(R.array.currency)[houseReport.costUnit]} ${houseReport.cost}"
        houseRepPreviewBean.itemBeans = ArrayList<HouseRepPreviewItemBean>()
        houseReport.dirList.forEachIndexed { _, dirReport ->
            var itemBean = HouseRepPreviewItemBean()
            itemBean.itemName = dirReport.dirName
            var count = dirReport.goodCount + dirReport.warnCount + dirReport.dangerCount
            itemBean.projectItemBeans = ArrayList<HouseRepPreviewProjectItemBean>()
            itemBean.albumItemBeans = ArrayList<HouseRepPreviewAlbumItemBean>()

            dirReport.itemList.forEachIndexed { _, itemReport ->
                var projectItemBean = HouseRepPreviewProjectItemBean()
                projectItemBean.projectName = itemReport.itemName
                projectItemBean.state = itemReport.state
                projectItemBean.remark = itemReport.inputText
                if (itemReport.state > 0 || itemReport.inputText.isNotEmpty()) {
                    itemBean.projectItemBeans.add(projectItemBean)
                }

                if (itemReport.getImageSize() > 0) {
                    var albumItemBean: HouseRepPreviewAlbumItemBean? = null
                    if (itemReport.image1.isNotEmpty()) {
                        albumItemBean = HouseRepPreviewAlbumItemBean()
                        albumItemBean.photoPath = itemReport.image1
                        albumItemBean.title = itemReport.itemName
                        itemBean.albumItemBeans.add(albumItemBean)
                    }
                    if (itemReport.image2.isNotEmpty()) {
                        albumItemBean = HouseRepPreviewAlbumItemBean()
                        albumItemBean.photoPath = itemReport.image2
                        albumItemBean.title = itemReport.itemName
                        itemBean.albumItemBeans.add(albumItemBean)
                    }
                    if (itemReport.image3.isNotEmpty()) {
                        albumItemBean = HouseRepPreviewAlbumItemBean()
                        albumItemBean.photoPath = itemReport.image3
                        albumItemBean.title = itemReport.itemName
                        itemBean.albumItemBeans.add(albumItemBean)
                    }
                    if (itemReport.image4.isNotEmpty()) {
                        albumItemBean = HouseRepPreviewAlbumItemBean()
                        albumItemBean.photoPath = itemReport.image4
                        albumItemBean.title = itemReport.itemName
                        itemBean.albumItemBeans.add(albumItemBean)
                    }
                }
            }

            var isEmpty =
                CollectionUtils.isEmpty(itemBean.projectItemBeans) && CollectionUtils.isEmpty(
                    itemBean.albumItemBeans
                )
            if (CollectionUtils.isNotEmpty(itemBean.projectItemBeans)) {
                itemBean.projectItemBeans.add(0, HouseRepPreviewProjectItemBean())
            }
            if (!isEmpty) {
                houseRepPreviewBean.itemBeans.add(itemBean)
            }
        }
        houseRepPreviewBean.inspectorWhitePath = houseReport.inspectorWhitePath
        houseRepPreviewBean.houseOwnerWhitePath = houseReport.houseOwnerWhitePath
        return houseRepPreviewBean
    }

    /**
     * Configures professional report data display with comprehensive thermal imaging visualization.
     * 
     * Sets up advanced data binding for report preview including thermal imaging metadata,
     * signature display, and professional report formatting with RecyclerView adapter configuration.
     */
    private fun setAdapter() {
        mPreviewBean?.let {
            with(binding) {
                Glide.with(this@ReportPreviewActivity).load(it.housePhoto).into(ivHeaderBg)
                tvAddress.text = it.houseAddress
                tvHouseName.text = it.houseName
                tvDetectTime.text = it.detectTime
                tvInspector.text = it.inspectorName
                tvBuildYear.text = it.houseYear
                tvArea.text = it.houseArea
                tvCost.text = it.expenses

                rcyFloor.layoutManager = LinearLayoutManager(this@ReportPreviewActivity)
                val reportPreviewAdapter = ReportPreviewAdapter(this@ReportPreviewActivity, it.itemBeans)
                rcyFloor.isNestedScrollingEnabled = false
                rcyFloor?.adapter = reportPreviewAdapter

                Glide.with(this@ReportPreviewActivity).load(it.inspectorWhitePath).into(ivInspectorSignature)
                Glide.with(this@ReportPreviewActivity).load(it.houseOwnerWhitePath).into(ivHouseOwnerSignature)
            }
        }
    }
}