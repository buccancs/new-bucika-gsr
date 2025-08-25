package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogLongTextBinding
import com.topdon.lib.core.utils.ScreenUtil

/**
 * Long Text Dialog
 * 
 * A specialized dialog component for displaying lengthy text content in the BucikaGSR application.
 * Provides a scrollable interface for detailed information, help text, or documentation display.
 * 
 * Key Features:
 * - Scrollable content area with fixed aspect ratio
 * - Customizable title and content text
 * - Single "I Know" dismissal button
 * - Responsive sizing at 74% of screen width
 * - Touch outside and back button cancellation support
 * - Professional styling with rounded corners and themed colors
 * 
 * ViewBinding Integration:
 * - Modern type-safe view access using DialogLongTextBinding
 * - Eliminates synthetic view imports for better compile-time safety
 * - Follows Android development best practices
 * 
 * Usage Scenarios:
 * - Device operation instructions
 * - Detailed error messages or troubleshooting information
 * - Feature explanations and help content
 * - Terms and conditions display
 * - Technical documentation viewing
 * 
 * UI Design:
 * - Fixed width: 74% of screen width for optimal readability
 * - Scrollable content area with 247:166 aspect ratio
 * - Consistent padding and margins for visual balance
 * - Themed background with rounded corners
 * - Single action button for user acknowledgment
 * 
 * @param context Android context for dialog creation
 * @param title Optional title text to display at the top
 * @param content The main content text to display in scrollable area
 * 
 * @author BucikaGSR Development Team
 * @since 1.0.0
 * @see Dialog for base dialog functionality
 */
class LongTextDialog(
    context: Context, 
    private val title: String?, 
    private val content: String?
) : Dialog(context, R.style.InfoDialog) {

    /**
     * ViewBinding instance for type-safe view access
     * Provides compile-time verified access to all views in dialog_long_text.xml
     */
    private lateinit var binding: DialogLongTextBinding

    /**
     * Initializes the dialog with content and responsive sizing
     * 
     * Sets up:
     * - ViewBinding initialization and content view assignment
     * - Title and content text population
     * - Cancellation behavior (touch outside and back button enabled)
     * - Dismiss button click handling
     * - Responsive dialog sizing at 74% screen width
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        binding = DialogLongTextBinding.inflate(LayoutInflater.from(context))
        binding.tvTitle.text = title
        binding.tvText.text = content
        setContentView(binding.root)
        
        binding.tvIKnow.setOnClickListener {
            dismiss()
        }

        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * 0.74f).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }
    }
