package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogFirmwareUpBinding
import com.topdon.lib.core.utils.ScreenUtil

/**
 * Professional firmware upgrade dialog for BucikaGSR Android application.
 * 
 * This dialog provides a comprehensive interface for notifying users of available firmware updates,
 * displaying version information, file sizes, and upgrade content details with professional
 * styling and user interaction management suitable for clinical and research environments.
 * 
 * Features:
 * - Version information display with professional formatting
 * - File size indication for download preparation
 * - Detailed upgrade content with scrollable view
 * - Optional device restart notifications for firmware updates
 * - Configurable cancel button visibility for forced updates
 * - Professional dialog styling with custom backgrounds and animations
 * 
 * @param context The context for creating the dialog
 * @author LCG
 * @since 2024/3/4
 */
class FirmwareUpDialog(context: Context) : Dialog(context, R.style.InfoDialog), View.OnClickListener {

    /**
     * Dialog view binding for type-safe view access.
     */
    private lateinit var binding: DialogFirmwareUpBinding

    /**
     * Title text displaying version information such as "发现新版本 V3.50".
     * 
     * @return Current title text or null if not set
     * @throws IllegalStateException If dialog has not been created yet
     */
    var titleStr: CharSequence?
        get() = binding.tvTitle.text
        set(value) {
            binding.tvTitle.text = value
        }

    /**
     * File size text displaying download size such as "大小: 239.6MB".
     * 
     * @return Current file size text or null if not set
     * @throws IllegalStateException If dialog has not been created yet
     */
    var sizeStr: CharSequence?
        get() = binding.tvSize.text
        set(value) {
            binding.tvSize.text = value
        }

    /**
     * Upgrade content text containing detailed information about the update.
     * This content is typically obtained directly from API responses and displayed
     * in a scrollable format for comprehensive user review.
     * 
     * @return Current upgrade content text or null if not set
     * @throws IllegalStateException If dialog has not been created yet
     */
    var contentStr: CharSequence?
        get() = binding.tvContent.text
        set(value) {
            binding.tvContent.text = value
        }

    /**
     * Controls visibility of device restart notification.
     * Currently only firmware upgrades require restart notifications.
     * Default state is hidden (GONE).
     * 
     * @return true if restart tips are visible, false otherwise
     * @throws IllegalStateException If dialog has not been created yet
     */
    var isShowRestartTips: Boolean
        get() = binding.tvRestartTips.isVisible
        set(value) {
            binding.tvRestartTips.isVisible = value
        }

    /**
     * Controls visibility of the cancel button.
     * Can be hidden for forced updates that require user confirmation.
     * Default state is visible.
     * 
     * @return true if cancel button is visible, false otherwise
     * @throws IllegalStateException If dialog has not been created yet
     */
    var isShowCancel: Boolean
        get() = binding.tvCancel.isVisible
        set(value) {
            binding.tvCancel.isVisible = value
        }

    /**
     * Cancel button click event listener.
     * Invoked when user cancels the firmware update process.
     */
    var onCancelClickListener: (() -> Unit)? = null

    /**
     * Confirm button click event listener.
     * Invoked when user confirms to proceed with the firmware update.
     */
    var onConfirmClickListener: (() -> Unit)? = null

    /**
     * Initializes the dialog with professional styling and user interface setup.
     * 
     * Configures:
     * - Non-cancellable dialog behavior for firmware update integrity
     * - Professional dialog sizing based on screen dimensions
     * - View binding initialization with click listeners
     * - Window layout parameters for optimal display
     * 
     * @param savedInstanceState Bundle containing the dialog's previously saved state
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ViewBinding for type-safe view access
        binding = DialogFirmwareUpBinding.inflate(LayoutInflater.from(context))
        
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setContentView(binding.root)

        // Configure professional dialog sizing
        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * 0.72).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }

        // Set up click listeners for user interaction
        binding.tvCancel.setOnClickListener(this)
        binding.tvConfirm.setOnClickListener(this)
    }

    /**
     * Handles dialog button click events with professional user interaction management.
     * 
     * Processes:
     * - Cancel button: Dismisses dialog and triggers cancel callback
     * - Confirm button: Dismisses dialog and triggers confirmation callback
     * 
     * @param v The clicked view (cancel or confirm button)
     */
    override fun onClick(v: View?) {
        when (v) {
            binding.tvCancel -> {
                dismiss()
                onCancelClickListener?.invoke()
            }
            binding.tvConfirm -> {
                dismiss()
                onConfirmClickListener?.invoke()
            }
        }
    }
}