package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogConfirmSelectBinding
import com.topdon.lib.core.utils.ScreenUtil

class ConfirmSelectDialog(context: Context) : Dialog(context, R.style.InfoDialog), View.OnClickListener {

    private lateinit var binding: DialogConfirmSelectBinding
    
    var onConfirmClickListener: ((isSelect: Boolean) -> Unit)? = null

    fun setShowIcon(isShowIcon: Boolean) {
        binding.ivIcon.isVisible = isShowIcon
    }

    fun setTitleRes(@StringRes titleRes: Int) {
        binding.tvTitle.setText(titleRes)
    }

    fun setTitleStr(titleStr: String) {
        binding.tvTitle.text = titleStr
    }

    fun setShowMessage(isShowMessage: Boolean) {
        binding.rlMessage.isVisible = isShowMessage
    }

    fun setMessageRes(@StringRes messageRes: Int) {
        binding.tvMessage.setText(messageRes)
    }

    fun setShowCancel(isShowCancel: Boolean) {
        binding.tvCancel.isVisible = isShowCancel
    }
    
    fun setCancelText(@StringRes cancelRes: Int) {
        binding.tvCancel.setText(cancelRes)
    }

    fun setConfirmText(@StringRes confirmRes: Int) {
        binding.tvConfirm.setText(confirmRes)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = DialogConfirmSelectBinding.inflate(LayoutInflater.from(context))
        
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        setContentView(binding.root)

        window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * 0.72).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window.attributes = layoutParams
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        with(binding) {
            rlMessage.setOnClickListener(this@ConfirmSelectDialog)
            tvCancel.setOnClickListener(this@ConfirmSelectDialog)
            tvConfirm.setOnClickListener(this@ConfirmSelectDialog)
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.rlMessage -> {

                binding.ivSelect.isSelected = !binding.ivSelect.isSelected
            }
            binding.tvCancel -> {

                dismiss()
            }
            binding.tvConfirm -> {

                dismiss()
                onConfirmClickListener?.invoke(binding.ivSelect.isSelected)
            }
        }
    }
