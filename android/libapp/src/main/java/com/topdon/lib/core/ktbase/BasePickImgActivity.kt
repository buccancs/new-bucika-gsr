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

abstract class BasePickImgActivity : BaseActivity(), View.OnClickListener {

    private lateinit var binding: ActivityImagePickIrPlushBinding
    
    val RESULT_IMAGE_PATH = "RESULT_IMAGE_PATH"
    
    private var hasTakePhoto = false

    override fun initContentView(): Int {
        return R.layout.activity_image_pick_ir_plush
    }

    override fun initView() {
    }

    override fun initData() {
    }

    fun getViewBinding(): ViewBinding {
        binding = ActivityImagePickIrPlushBinding.inflate(layoutInflater)
        return binding
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupDefaultState()
        setupClickListeners()
        setupTitleBar()
        resize()
    }

    private fun setupDefaultState() {
        binding.ivEditCircle.isSelected = true
        binding.imageEditView.type = ImageEditView.Type.CIRCLE
        binding.viewColor.setBackgroundColor(binding.imageEditView.color)
    }

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

            e.printStackTrace()
        }
    }

    private fun resize() {
        val widthPixels = resources.displayMetrics.widthPixels
        val heightPixels = resources.displayMetrics.heightPixels
        
        binding.titleView.measure(
            MeasureSpec.makeMeasureSpec(widthPixels, MeasureSpec.EXACTLY), 
            MeasureSpec.makeMeasureSpec(heightPixels, MeasureSpec.AT_MOST)
        )

        val ivPickHeight = SizeUtils.dp2px(60f + 20 + 20)
        val menuHeight = (widthPixels * 75f / 384).toInt()
        val bottomHeight = ivPickHeight.coerceAtLeast(menuHeight)
        val canUseHeight = heightPixels - binding.titleView.measuredHeight - bottomHeight
        val wantHeight = (widthPixels * 256f / 192).toInt()
        
        if (wantHeight <= canUseHeight) {
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

    open suspend fun getPickBitmap(): Bitmap? {
        return null
    }

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

    private fun selectDrawingTool(type: ImageEditView.Type) {
        with(binding) {

            ivEditCircle.isSelected = false
            ivEditRect.isSelected = false
            ivEditArrow.isSelected = false
            
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

    override fun onBackPressed() {
        if (hasTakePhoto) {
            switchPhotoState(false)
        } else {
            super.onBackPressed()
        }
    }

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

    private fun showExitTipsDialog(listener: (() -> Unit)) {
        TipDialog.Builder(this)
            .setMessage(R.string.diy_tip_save)
            .setPositiveListener(R.string.app_exit) {
                listener.invoke()
            }
            .setCancelListener(R.string.app_cancel)
            .create().show()
    }

    override fun disConnected() {
        super.disConnected()
        finish()
    }
}
