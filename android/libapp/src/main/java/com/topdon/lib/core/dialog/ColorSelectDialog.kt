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

class ColorSelectDialog(context: Context, @ColorInt private var color: Int) : Dialog(context, R.style.InfoDialog) {

    private lateinit var binding: DialogColorSelectBinding

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

    private fun setupColorPicker() {
        binding.colorSelectView.apply {
            selectColor(color)
            onSelectListener = { selectedColor ->
                color = selectedColor
            }
        }
    }

    private fun setupSaveButton() {
        binding.tvSave.setOnClickListener {
            dismiss()
            onPickListener?.invoke(color)
        }
    }

    private fun setupDialogDimensions() {
        window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.width = ScreenUtil.getScreenWidth(context) - SizeUtils.dp2px(36f)
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window.attributes = layoutParams
        }
    }
}
