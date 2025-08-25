package com.topdon.lib.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.lib.ui.R
import com.topdon.lib.ui.databinding.DialogProgressBinding

/**
 * Progress dialog with ViewBinding implementation.
 * 
 * Provides professional progress indication interface for data export operations
 * and other long-running processes in thermal imaging applications.
 * 
 * Features include:
 * - Horizontal progress bar with customizable range
 * - Professional dialog styling with adaptive sizing
 * - Research-grade progress tracking for data operations
 * - Portrait/landscape adaptive dimensions
 * 
 * @param context Dialog display context
 * @author Topdon Thermal Imaging Team
 * @since 2024-01-01
 */
class ProgressDialog(context: Context) : Dialog(context, R.style.InfoDialog) {
    
    private lateinit var binding: DialogProgressBinding
    
    /**
     * Maximum progress value (default: 100).
     */
    var max: Int = 100
        set(value) {
            if (::binding.isInitialized) {
                binding.progressBar.max = value
            }
            field = value
        }

    /**
     * Current progress value (default: 0).
     */
    var progress: Int = 0
        set(value) {
            if (::binding.isInitialized) {
                binding.progressBar.progress = value
            }
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        
        binding = DialogProgressBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        
        setupDialogDimensions()
    }

    /**
     * Setup dialog window dimensions with orientation-aware sizing.
     */
    private fun setupDialogDimensions() {
        window?.let { window ->
            val layoutParams = window.attributes
            val screenWidth = ScreenUtil.getScreenWidth(context)
            val widthRatio = if (ScreenUtil.isPortrait(context)) 0.8 else 0.45
            layoutParams.width = (screenWidth * widthRatio).toInt()
            layoutParams.height = LayoutParams.WRAP_CONTENT
            window.attributes = layoutParams
        }
    }

    /**
     * Show dialog and initialize progress values.
     */
    override fun show() {
        super.show()
        binding.progressBar.apply {
            this.max = this@ProgressDialog.max
            this.progress = this@ProgressDialog.progress
        }
    }
