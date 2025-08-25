package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
import com.blankj.utilcode.util.SizeUtils
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogColorSelectBinding
import com.topdon.lib.core.utils.ScreenUtil

/**
 * Color picker dialog with ViewBinding implementation.
 * 
 * Provides professional color selection interface for thermal imaging applications
 * with comprehensive color manipulation and selection validation.
 * 
 * Features include:
 * - ColorSelectView integration for precise color picking
 * - Real-time color preview and validation
 * - Professional dialog styling with modern layouts
 * - Research-grade color management for data visualization
 * 
 * @param context Dialog display context
 * @param color Initial color value for selection
 * @author Topdon Thermal Imaging Team
 * @since 2024-02-02
 */
class ColorSelectDialog(context: Context, @ColorInt private var color: Int) : Dialog(context, R.style.InfoDialog) {

    private lateinit var binding: DialogColorSelectBinding

    /**
     * Color selection event listener.
     * 
     * @param color Selected color value as integer
     */
    var onPickListener: ((color: Int) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        binding = DialogColorSelectBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        
        setupColorPicker()
        setupSaveButton()
        setupDialogDimensions()
    }

    /**
     * Configure color picker view with initial color and listener.
     */
    private fun setupColorPicker() {
        binding.colorSelectView.apply {
            selectColor(color)
            onSelectListener = { selectedColor ->
                color = selectedColor
            }
        }
    }

    /**
     * Configure save button with color selection confirmation.
     */
    private fun setupSaveButton() {
        binding.tvSave.setOnClickListener {
            dismiss()
            onPickListener?.invoke(color)
        }
    }

    /**
     * Setup dialog window dimensions for optimal display.
     */
    private fun setupDialogDimensions() {
        window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.width = ScreenUtil.getScreenWidth(context) - SizeUtils.dp2px(36f)
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window.attributes = layoutParams
        }
    }
