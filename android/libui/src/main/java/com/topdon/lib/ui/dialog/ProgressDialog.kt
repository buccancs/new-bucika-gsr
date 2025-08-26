package com.topdon.lib.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.lib.ui.R
import com.topdon.lib.ui.databinding.DialogProgressBinding

class ProgressDialog(context: Context) : Dialog(context, R.style.InfoDialog) {
    
    private lateinit var binding: DialogProgressBinding
    
    var max: Int = 100
        set(value) {
            if (::binding.isInitialized) {
                binding.progressBar.max = value
            }
            field = value
        }

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

    override fun show() {
        super.show()
        binding.progressBar.apply {
            this.max = this@ProgressDialog.max
            this.progress = this@ProgressDialog.progress
        }
    }
