package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogNotTipsSelectBinding
import com.topdon.lib.core.utils.ScreenUtil

class NotTipsSelectDialog(context: Context) : Dialog(context, R.style.InfoDialog) {

    private lateinit var binding: DialogNotTipsSelectBinding

    @StringRes
    private var tipsResId: Int = 0
    private var onConfirmListener: ((isSelect: Boolean) -> Unit)? = null

    fun setTipsResId(@StringRes tipsResId: Int): NotTipsSelectDialog {
        this.tipsResId = tipsResId
        return this
    }

    fun setOnConfirmListener(l: ((isSelect: Boolean) -> Unit)?): NotTipsSelectDialog {
        onConfirmListener = l
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = DialogNotTipsSelectBinding.inflate(layoutInflater)
        
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setContentView(binding.root)

        setupMessage()
        
        setupInteractions()
        
        setupWindowSizing()
    }

    private fun setupMessage() {
        if (tipsResId != 0) {
            binding.tvMessage.setText(tipsResId)
        }
    }

    private fun setupInteractions() {

        binding.tvSelect.setOnClickListener { view ->
            view.isSelected = !view.isSelected
        }
        
        binding.tvIKnow.setOnClickListener {
            onConfirmListener?.invoke(binding.tvSelect.isSelected)
            dismiss()
        }
    }

    private fun setupWindowSizing() {
        window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * 0.73f).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window.attributes = layoutParams
        }
    }
}
