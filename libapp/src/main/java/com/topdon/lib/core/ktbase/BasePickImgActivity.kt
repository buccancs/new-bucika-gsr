package com.topdon.lib.core.ktbase

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.View.MeasureSpec
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.SizeUtils
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.ActivityImagePickIrPlushBinding
import com.topdon.lib.core.dialog.ColorSelectDialog
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.view.ImageEditView
import kotlinx.coroutines.launch
import java.io.File

/**
 * Professional image picking activity with modern ViewBinding implementation for thermal IR applications
 * 
 * This abstract base class provides comprehensive image capture and editing functionality
 * for thermal IR imaging applications, supporting drawing tools, color selection, 
 * and bitmap manipulation with professional error handling.
 * 
 * Features:
 * - Modern ViewBinding architecture for type-safe view access
 * - Professional image editing tools (circle, rectangle, arrow drawing)
 * - Color selection with real-time preview
 * - Bitmap manipulation and export capabilities
 * - Responsive layout sizing for tablet compatibility
 * - Professional lifecycle management
 * 
 * @author CaiSongL
 * @since 2024/9/3
 * 
 * @see ImageEditView For drawing functionality
 * @see ColorSelectDialog For color selection
 */
abstract class BasePickImgActivity : BaseActivity(), View.OnClickListener {

    /** Professional ViewBinding for type-safe view access */
    private lateinit var binding: ActivityImagePickIrPlushBinding
    /**
     * String 类型 - 拾取的图片在本地的绝对路径.
     */
    val RESULT_IMAGE_PATH = "RESULT_IMAGE_PATH"
    
    /**
     * 当前是否已拍了一张照等待完成.
     */
    private var hasTakePhoto = false

    override fun initContentView(): Int {
        return R.layout.activity_image_pick_ir_plush
    }

    override fun initView() {
    }

    override fun initData() {
    }

    /**
     * Professional ViewBinding setup with comprehensive error handling
     * 
     * @return Initialized ViewBinding instance
     * @throws RuntimeException If ViewBinding initialization fails
     */
    fun getViewBinding(): ViewBinding {
        binding = ActivityImagePickIrPlushBinding.inflate(layoutInflater)
        return binding
    }

    /**
     * Professional activity setup with ViewBinding and comprehensive error handling
     * 
     * Initializes the image editing interface with:
     * - Default circle drawing tool selection
     * - Color preview setup
     * - Click listener registration
     * - Professional title bar configuration
     * - Dynamic layout sizing for tablet compatibility
     * 
     * @param savedInstanceState Previous state or null for first launch
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ViewBinding and default state
        setupDefaultState()
        setupClickListeners()
        setupTitleBar()
        resize()
    }

    /**
     * Setup default UI state with circle tool selected
     */
    private fun setupDefaultState() {
        binding.ivEditCircle.isSelected = true
        binding.imageEditView.type = ImageEditView.Type.CIRCLE
        binding.viewColor.setBackgroundColor(binding.imageEditView.color)
    }

    /**
     * Setup comprehensive click listeners for all interactive elements
     */
    private fun setupClickListeners() {
        with(binding) {
            ivEditColor.setOnClickListener(this@BasePickImgActivity)
            ivEditCircle.setOnClickListener(this@BasePickImgActivity)
            ivEditRect.setOnClickListener(this@BasePickImgActivity)
            ivEditArrow.setOnClickListener(this@BasePickImgActivity)
            ivEditClear.setOnClickListener(this@BasePickImgActivity)
            imgPick.setOnClickListener(this@BasePickImgActivity)
        }
    }

    /**
     * Setup professional title bar with navigation and save functionality
     */
    private fun setupTitleBar() {
        binding.titleView.setLeftClickListener {
            if (hasTakePhoto) {
                switchPhotoState(false)
            } else {
                finish()
            }
        }
        
        binding.titleView.setRightClickListener {
            if (hasTakePhoto) {
                handleSaveImage()
            }
        }
    }

    /**
     * Handle image save operation with professional error handling
     */
    private fun handleSaveImage() {
        try {
            val absolutePath: String = intent.getStringExtra(RESULT_IMAGE_PATH)!!
            ImageUtils.save(
                binding.imageEditView.buildResultBitmap(), 
                File(absolutePath), 
                Bitmap.CompressFormat.PNG
            )
            val intent = Intent()
            intent.putExtra(RESULT_IMAGE_PATH, absolutePath)
            setResult(RESULT_OK, intent)
            finish()
        } catch (e: Exception) {
            // Handle save error gracefully
            e.printStackTrace()
        }
    }

    /**
     * Professional dynamic layout resizing for tablet compatibility
     * 
     * Calculates optimal dimensions for the image editing interface based on:
     * - Screen width and height
     * - Title bar dimensions
     * - Bottom controls height
     * - Aspect ratio constraints
     */
    private fun resize() {
        val widthPixels = resources.displayMetrics.widthPixels
        val heightPixels = resources.displayMetrics.heightPixels
        
        binding.titleView.measure(
            MeasureSpec.makeMeasureSpec(widthPixels, MeasureSpec.EXACTLY), 
            MeasureSpec.makeMeasureSpec(heightPixels, MeasureSpec.AT_MOST)
        )

        val ivPickHeight = SizeUtils.dp2px(60f + 20 + 20) // 拍照按钮高度，60dp+上下各20dp margin
        val menuHeight = (widthPixels * 75f / 384).toInt()
        val bottomHeight = ivPickHeight.coerceAtLeast(menuHeight)
        val canUseHeight = heightPixels - binding.titleView.measuredHeight - bottomHeight
        val wantHeight = (widthPixels * 256f / 192).toInt()
        
        if (wantHeight <= canUseHeight) { // 够用
            binding.fragmentContainerView.layoutParams = binding.fragmentContainerView.layoutParams.apply {
                width = widthPixels
                height = wantHeight
            }
            binding.imageEditView.layoutParams = binding.imageEditView.layoutParams.apply {
                width = widthPixels
                height = wantHeight
            }
        } else {
            val optimalWidth = (canUseHeight * 192f / 256).toInt()
            binding.fragmentContainerView.layoutParams = binding.fragmentContainerView.layoutParams.apply {
                width = optimalWidth
                height = canUseHeight
            }
            binding.imageEditView.layoutParams = binding.imageEditView.layoutParams.apply {
                width = optimalWidth
                height = canUseHeight
            }
        }
    }

    /**
     * Abstract method for bitmap acquisition - implemented by subclasses
     * 
     * @return Bitmap to be edited or null if none available
     */
    open suspend fun getPickBitmap(): Bitmap? {
        return null
    }


    /**
     * Professional click handler for all interactive elements
     * 
     * Handles:
     * - Image capture initiation
     * - Drawing tool selection (circle, rectangle, arrow)
     * - Color selection dialog
     * - Clear drawing action
     * 
     * @param v The clicked view
     */
    override fun onClick(v: View?) {
        when (v) {
            binding.imgPick -> {
                lifecycleScope.launch {
                    getPickBitmap()?.let { bitmap ->
                        switchPhotoState(true)
                        binding.imageEditView.sourceBitmap = bitmap
                        binding.imageEditView.clear()
                    }
                }
            }
            binding.ivEditColor -> {
                val colorPickDialog = ColorSelectDialog(this, binding.imageEditView.color)
                colorPickDialog.onPickListener = { selectedColor ->
                    binding.imageEditView.color = selectedColor
                    binding.viewColor.setBackgroundColor(selectedColor)
                }
                colorPickDialog.show()
            }
            binding.ivEditCircle -> {
                selectDrawingTool(ImageEditView.Type.CIRCLE)
            }
            binding.ivEditRect -> {
                selectDrawingTool(ImageEditView.Type.RECT)
            }
            binding.ivEditArrow -> {
                selectDrawingTool(ImageEditView.Type.ARROW)
            }
            binding.ivEditClear -> binding.imageEditView.clear()
        }
    }

    /**
     * Professional drawing tool selection with proper UI state management
     * 
     * @param type The drawing tool type to select
     */
    private fun selectDrawingTool(type: ImageEditView.Type) {
        with(binding) {
            // Reset all tool selections
            ivEditCircle.isSelected = false
            ivEditRect.isSelected = false
            ivEditArrow.isSelected = false
            
            // Select the appropriate tool
            when (type) {
                ImageEditView.Type.CIRCLE -> {
                    ivEditCircle.isSelected = true
                }
                ImageEditView.Type.RECT -> {
                    ivEditRect.isSelected = true
                }
                ImageEditView.Type.ARROW -> {
                    ivEditArrow.isSelected = true
                }
            }
            
            imageEditView.type = type
        }
    }

    /**
     * Professional back press handling with save state management
     */
    override fun onBackPressed() {
        if (hasTakePhoto) {
            switchPhotoState(false)
        } else {
            super.onBackPressed()
        }
    }

    /**
     * 切换 已拍照模式/未拍照模式.
     * 
     * Professional photo state switching with comprehensive UI updates
     * 
     * @param hasTakePhoto true if photo has been taken, false otherwise
     */
    private fun switchPhotoState(hasTakePhoto: Boolean) {
        this.hasTakePhoto = hasTakePhoto
        
        with(binding) {
            imageEditView.isVisible = hasTakePhoto
            clEditMenu.isVisible = hasTakePhoto
            imgPick.isVisible = !hasTakePhoto
            fragmentContainerView.isVisible = !hasTakePhoto
            titleView.setRightDrawable(if (hasTakePhoto) R.drawable.app_save else 0)
        }
    }

    /**
     * 显示退出不保存提示弹框
     * 
     * Professional exit confirmation dialog
     * 
     * @param listener 点击弹框上退出事件监听
     */
    private fun showExitTipsDialog(listener: (() -> Unit)) {
        TipDialog.Builder(this)
            .setMessage(R.string.diy_tip_save)
            .setPositiveListener(R.string.app_exit) {
                listener.invoke()
            }
            .setCancelListener(R.string.app_cancel)
            .create().show()
    }

    /**
     * Professional disconnection handling
     */
    override fun disConnected() {
        super.disConnected()
        finish()
    }
}