package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogNotTipsSelectBinding
import com.topdon.lib.core.utils.ScreenUtil

/**
 * Professional "don't show again" confirmation dialog with modern ViewBinding implementation
 * 
 * Similar to TipDialog but includes a "don't remind again" selection checkbox functionality
 * for enhanced user experience in thermal IR applications.
 * 
 * Features:
 * - Modern ViewBinding architecture for type-safe view access
 * - Professional "don't show again" selection tracking
 * - Customizable tip messages from string resources
 * - Responsive sizing (73% screen width)
 * - Professional confirmation workflow
 * - Research-grade user interaction patterns
 * 
 * @author LCG
 * @since 2024/10/26
 * 
 * @see DialogNotTipsSelectBinding For ViewBinding implementation
 */
class NotTipsSelectDialog(context: Context) : Dialog(context, R.style.InfoDialog) {

    /** Professional ViewBinding for type-safe view access */
    private lateinit var binding: DialogNotTipsSelectBinding

    @StringRes
    private var tipsResId: Int = 0
    private var onConfirmListener: ((isSelect: Boolean) -> Unit)? = null

    /**
     * Set tip message from string resource
     * 
     * @param tipsResId String resource ID for tip message
     * @return This dialog instance for method chaining
     */
    fun setTipsResId(@StringRes tipsResId: Int): NotTipsSelectDialog {
        this.tipsResId = tipsResId
        return this
    }

    /**
     * Set confirmation listener for "I know" button clicks
     * 
     * @param l Listener receiving selection state (true if "don't show again" is selected)
     * @return This dialog instance for method chaining
     */
    fun setOnConfirmListener(l: ((isSelect: Boolean) -> Unit)?): NotTipsSelectDialog {
        onConfirmListener = l
        return this
    }

    /**
     * Professional dialog creation with ViewBinding setup and comprehensive configuration
     * 
     * Initializes:
     * - ViewBinding for type-safe view access
     * - Professional dialog properties (non-cancelable for important tips)
     * - Responsive window sizing (73% screen width)
     * - Message configuration from string resources
     * - Selection toggle functionality
     * - Confirmation button interaction
     * 
     * @param savedInstanceState Previous state or null for first creation
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ViewBinding
        binding = DialogNotTipsSelectBinding.inflate(layoutInflater)
        
        // Setup professional dialog properties
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setContentView(binding.root)

        // Configure message display
        setupMessage()
        
        // Setup interactive elements
        setupInteractions()
        
        // Configure responsive window sizing
        setupWindowSizing()
    }

    /**
     * Setup message display from string resource
     */
    private fun setupMessage() {
        if (tipsResId != 0) {
            binding.tvMessage.setText(tipsResId)
        }
    }

    /**
     * Setup professional interactive elements
     */
    private fun setupInteractions() {
        // Selection toggle functionality
        binding.tvSelect.setOnClickListener { view ->
            view.isSelected = !view.isSelected
        }
        
        // Confirmation button with selection state callback
        binding.tvIKnow.setOnClickListener {
            onConfirmListener?.invoke(binding.tvSelect.isSelected)
            dismiss()
        }
    }

    /**
     * Configure responsive window sizing
     */
    private fun setupWindowSizing() {
        window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * 0.73f).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window.attributes = layoutParams
        }
    }
}