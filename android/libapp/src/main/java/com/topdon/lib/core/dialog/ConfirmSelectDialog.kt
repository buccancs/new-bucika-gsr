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

/**
 * Professional confirmation dialog for TS004 remote gallery deletion with modern ViewBinding implementation
 * 
 * Provides comprehensive selection confirmation functionality for thermal IR gallery management
 * with optional icon display, customizable messaging, and professional user interaction patterns.
 * 
 * Features:
 * - Modern ViewBinding architecture for type-safe view access
 * - Professional confirmation workflow with selection tracking
 * - Customizable title, message, and button text
 * - Optional icon and message display
 * - Research-grade user interaction patterns
 * - Responsive sizing for different screen sizes
 * 
 * @author LCG  
 * @since 2024/2/29
 * 
 * @see DialogConfirmSelectBinding For ViewBinding implementation
 */
class ConfirmSelectDialog(context: Context) : Dialog(context, R.style.InfoDialog), View.OnClickListener {

    /** Professional ViewBinding for type-safe view access */
    private lateinit var binding: DialogConfirmSelectBinding
    
    /** Confirmation callback with selection state */
    var onConfirmClickListener: ((isSelect: Boolean) -> Unit)? = null

    /**
     * Professional icon display control
     * 
     * @param isShowIcon true to show information icon, false to hide
     */
    fun setShowIcon(isShowIcon: Boolean) {
        binding.ivIcon.isVisible = isShowIcon
    }

    /**
     * Set title text from string resource
     * 
     * @param titleRes String resource ID for title text
     */
    fun setTitleRes(@StringRes titleRes: Int) {
        binding.tvTitle.setText(titleRes)
    }

    /**
     * Set title text from string
     * 
     * @param titleStr Title text to display
     */
    fun setTitleStr(titleStr: String) {
        binding.tvTitle.text = titleStr
    }

    /**
     * Professional message display control with selection functionality
     * 
     * @param isShowMessage true to show message and selection checkbox, false to hide
     */
    fun setShowMessage(isShowMessage: Boolean) {
        binding.rlMessage.isVisible = isShowMessage
    }

    /**
     * Set message text from string resource
     * 
     * @param messageRes String resource ID for message text
     */
    fun setMessageRes(@StringRes messageRes: Int) {
        binding.tvMessage.setText(messageRes)
    }

    /**
     * Professional cancel button display control
     * 
     * @param isShowCancel true to show cancel button, false to hide (defaults to visible)
     */
    fun setShowCancel(isShowCancel: Boolean) {
        binding.tvCancel.isVisible = isShowCancel
    }
    
    /**
     * Set cancel button text from string resource
     * 
     * @param cancelRes String resource ID for cancel button text (defaults to "取消")
     */
    fun setCancelText(@StringRes cancelRes: Int) {
        binding.tvCancel.setText(cancelRes)
    }

    /**
     * Set confirm button text from string resource
     * 
     * @param confirmRes String resource ID for confirm button text (defaults to "删除")
     */
    fun setConfirmText(@StringRes confirmRes: Int) {
        binding.tvConfirm.setText(confirmRes)
    }

    /**
     * Professional dialog creation with ViewBinding setup and comprehensive configuration
     * 
     * Initializes:
     * - ViewBinding for type-safe view access
     * - Professional dialog properties (cancelable, touch outside cancellation)
     * - Responsive window sizing (72% screen width)
     * - Click listener registration for all interactive elements
     * 
     * @param savedInstanceState Previous state or null for first creation
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ViewBinding
        binding = DialogConfirmSelectBinding.inflate(LayoutInflater.from(context))
        
        // Setup dialog properties
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        setContentView(binding.root)

        // Configure responsive window sizing
        window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * 0.72).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window.attributes = layoutParams
        }

        // Setup click listeners for all interactive elements
        setupClickListeners()
    }

    /**
     * Setup comprehensive click listeners for professional user interaction
     */
    private fun setupClickListeners() {
        with(binding) {
            rlMessage.setOnClickListener(this@ConfirmSelectDialog)
            tvCancel.setOnClickListener(this@ConfirmSelectDialog)
            tvConfirm.setOnClickListener(this@ConfirmSelectDialog)
        }
    }

    /**
     * Professional click handler with modern ViewBinding implementation
     * 
     * Handles:
     * - Selection state toggle for message area
     * - Cancel button dismissal
     * - Confirm button with callback execution
     * 
     * @param v The clicked view
     */
    override fun onClick(v: View?) {
        when (v) {
            binding.rlMessage -> {
                // Toggle selection state with visual feedback
                binding.ivSelect.isSelected = !binding.ivSelect.isSelected
            }
            binding.tvCancel -> {
                // Professional cancellation
                dismiss()
            }
            binding.tvConfirm -> {
                // Confirm with selection state callback
                dismiss()
                onConfirmClickListener?.invoke(binding.ivSelect.isSelected)
            }
        }
    }
}