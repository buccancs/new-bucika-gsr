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

/**
 * Loading Dialog
 * 
 * A modern loading dialog component for the BucikaGSR application that provides
 * visual feedback during asynchronous operations. Features a customizable loading
 * animation with optional text messages.
 * 
 * Key Features:
 * - Modern LMS loading animation with configurable dots
 * - Optional tip text display with dynamic visibility
 * - Responsive sizing based on screen orientation
 * - Non-cancelable by default for critical operations
 * - Transparent background with rounded corners
 * - String resource and CharSequence text support
 * 
 * ViewBinding Integration:
 * - Modern type-safe view access using DialogLoadingBinding
 * - Eliminates synthetic view imports for better compile-time safety
 * - Follows Android development best practices
 * 
 * Usage Examples:
 * - Data loading operations
 * - File processing feedback
 * - Network request progress indication
 * - Background task status display
 * 
 * UI Specifications:
 * - Portrait mode: 30% of screen width
 * - Landscape mode: 15% of screen width
 * - Wrap content height with consistent padding
 * - Centered loading animation with optional text below
 * 
 * @author BucikaGSR Development Team
 * @since 1.0.0
 * @see Dialog for base dialog functionality
 * @see LmsLoadView for loading animation component
 */
class LoadingDialog(context: Context) : Dialog(context, R.style.TransparentDialog) {

    /**
     * ViewBinding instance for type-safe view access
     * Provides compile-time verified access to all views in dialog_loading.xml
     */
    private val binding: DialogLoadingBinding = DialogLoadingBinding.inflate(LayoutInflater.from(context))

    /**
     * Sets tip text using string resource ID
     * 
     * @param resId String resource identifier for the tip text
     */
    fun setTips(@StringRes resId: Int) {
        binding.tvTips.setText(resId)
        binding.tvTips.isVisible = true
    }

    /**
     * Sets tip text using CharSequence
     * 
     * Automatically manages visibility based on text content:
     * - Shows text view if content is non-empty
     * - Hides text view if content is null or empty
     * 
     * @param text The tip text to display, or null to hide
     */
    fun setTips(text: CharSequence?) {
        binding.tvTips.text = text
        binding.tvTips.isVisible = text?.isNotEmpty() == true
    }

    /**
     * Initializes the dialog with responsive sizing and configuration
     * 
     * Sets up:
     * - Non-cancelable behavior for critical operations
     * - Touch outside cancellation disabled
     * - Responsive sizing based on screen orientation
     * - ViewBinding root as content view
     */
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
