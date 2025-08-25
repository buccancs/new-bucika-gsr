package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogLoadingBinding
import com.topdon.lib.core.utils.ScreenUtil

class LoadingDialog(context: Context) : Dialog(context, R.style.TransparentDialog) {

    private val binding: DialogLoadingBinding = DialogLoadingBinding.inflate(LayoutInflater.from(context))

    fun setTips(@StringRes resId: Int) {
        binding.tvTips.setText(resId)
        binding.tvTips.isVisible = true
    }

    fun setTips(text: CharSequence?) {
        binding.tvTips.text = text
        binding.tvTips.isVisible = text?.isNotEmpty() == true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setContentView(binding.root)

        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * 
                if (ScreenUtil.isPortrait(context)) 0.3 else 0.15).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }
    }
