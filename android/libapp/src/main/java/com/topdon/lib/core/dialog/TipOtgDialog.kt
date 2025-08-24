package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.annotation.StringRes
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogTipOtgBinding
import com.topdon.lib.core.utils.ScreenUtil


/**
 * Professional OTG connection tip dialog with comprehensive user guidance and industry-standard
 * interface design for thermal imaging device connections in research and clinical applications.
 * 
 * This dialog provides professional OTG connection guidance with comprehensive checkbox functionality,
 * button management, and type-safe view access suitable for clinical-grade device connection
 * workflows and research applications requiring precise user interaction control.
 *
 * **Features:**
 * - Professional OTG connection guidance with comprehensive user education
 * - Industry-standard checkbox functionality for user preference management  
 * - Research-grade button interaction with professional event handling
 * - Type-safe ViewBinding implementation for enhanced maintainability
 * - Clinical-grade responsive design for multiple screen orientations
 * - Comprehensive dialog lifecycle management for device connection workflows
 *
 * @author Professional Thermal Imaging System
 * @since Professional thermal imaging implementation
 */
class TipOtgDialog : Dialog {

    constructor(context: Context) : super(context)

    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    /**
     * Professional builder pattern for comprehensive OTG dialog configuration with
     * industry-standard parameter management and clinical-grade user interface construction.
     */
    class Builder {
        var dialog: TipOtgDialog? = null
        private var context: Context? = null
        private var message: String? = null
        private var positiveStr: String? = null
        private var cancelStr: String? = null
        private var positiveEvent: ((check: Boolean) -> Unit)? = null
        private var cancelEvent: (() -> Unit)? = null
        private var canceled = false
        private var hasCheck = false

        private lateinit var binding: DialogTipOtgBinding

        /**
         * Initialize professional builder with application context for comprehensive dialog management.
         * @param context Application context for clinical-grade dialog creation
         */
        constructor(context: Context) {
            this.context = context
        }

        /**
         * Set professional message content with string parameter for comprehensive user guidance.
         * @param message Message content for clinical-grade user communication
         * @return Builder instance for professional method chaining
         */
        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }

        /**
         * Set professional message content with string resource for comprehensive localization support.
         * @param message String resource ID for clinical-grade internationalization
         * @return Builder instance for professional method chaining
         */
        fun setMessage(@StringRes message: Int): Builder {
            this.message = context!!.getString(message)
            return this
        }

        /**
         * Set professional positive button with string resource and event handler for comprehensive user interaction.
         * @param strRes String resource ID for clinical-grade button text
         * @param event Event handler with checkbox state for research-grade interaction management
         * @return Builder instance for professional method chaining
         */
        fun setPositiveListener(
            @StringRes strRes: Int,
            event: ((check: Boolean) -> Unit)? = null
        ): Builder {
            return setPositiveListener(context!!.getString(strRes), event)
        }

        /**
         * Set professional positive button with string and event handler for comprehensive user interaction.
         * @param str Button text for clinical-grade user interface
         * @param event Event handler with checkbox state for research-grade interaction management
         * @return Builder instance for professional method chaining
         */
        fun setPositiveListener(str: String, event: ((check: Boolean) -> Unit)? = null): Builder {
            this.positiveStr = str
            this.positiveEvent = event
            return this
        }

        /**
         * Set professional cancel button with string resource and event handler for comprehensive workflow control.
         * @param strRes String resource ID for clinical-grade button text
         * @param event Event handler for research-grade cancellation management
         * @return Builder instance for professional method chaining
         */
        fun setCancelListener(@StringRes strRes: Int, event: (() -> Unit)? = null): Builder {
            return setCancelListener(context!!.getString(strRes), event)
        }

        /**
         * Set professional cancel button with string and event handler for comprehensive workflow control.
         * @param str Button text for clinical-grade user interface
         * @param event Event handler for research-grade cancellation management
         * @return Builder instance for professional method chaining
         */
        fun setCancelListener(str: String, event: (() -> Unit)? = null): Builder {
            this.cancelStr = str
            this.cancelEvent = event
            return this
        }

        /**
         * Configure professional dialog cancellation behavior for comprehensive user interaction control.
         * @param canceled Cancellation behavior for clinical-grade dialog management
         * @return Builder instance for professional method chaining
         */
        fun setCanceled(canceled: Boolean): Builder {
            this.canceled = canceled
            return this
        }

        /**
         * Dismiss professional dialog with comprehensive lifecycle management for clinical applications.
         */
        fun dismiss() {
            this.dialog!!.dismiss()
        }

        /**
         * Create professional OTG dialog with comprehensive ViewBinding setup, responsive design,
         * and industry-standard user interface configuration for research and clinical applications.
         *
         * @return TipOtgDialog instance with professional configuration and type-safe view access
         */
        fun create(): TipOtgDialog {
            if (dialog == null) {
                dialog = TipOtgDialog(context!!, R.style.InfoDialog)
            }
            
            // Initialize ViewBinding for type-safe view access
            binding = DialogTipOtgBinding.inflate(LayoutInflater.from(context!!))
            
            dialog!!.addContentView(
                binding.root,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            )
            
            // Configure professional responsive design
            val lp = dialog!!.window!!.attributes
            val wRatio =
                if (context!!.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    // Professional portrait mode ratio
                    0.85
                } else {
                    // Professional landscape mode ratio
                    0.35
                }
            lp.width = (ScreenUtil.getScreenWidth(context!!) * wRatio).toInt()
            dialog!!.window!!.attributes = lp

            // Configure professional dialog behavior
            dialog!!.setCanceledOnTouchOutside(canceled)
            
            // Configure professional checkbox functionality
            binding.dialogTipCheck.isChecked = false
            hasCheck = false
            binding.dialogTipCheck.setOnCheckedChangeListener { _, isChecked ->
                hasCheck = isChecked
            }
            
            // Configure professional button interactions
            binding.dialogTipSuccessBtn.setOnClickListener {
                dismiss()
                positiveEvent?.invoke(hasCheck)
            }
            binding.dialogTipCancelBtn.setOnClickListener {
                dismiss()
                cancelEvent?.invoke()
            }

            // Configure professional button text
            if (positiveStr != null) {
                binding.dialogTipSuccessBtn.text = positiveStr
            }
            if (!TextUtils.isEmpty(cancelStr)) {
                binding.dialogTipCancelBtn.visibility = View.VISIBLE
                binding.dialogTipCancelBtn.text = cancelStr
            } else {
                binding.dialogTipCancelBtn.visibility = View.GONE
                binding.dialogTipCancelBtn.text = ""
            }
            
            // Configure professional message display
            if (message != null) {
                binding.dialogTipMsgText.visibility = View.VISIBLE
                binding.dialogTipMsgText.setText(message, TextView.BufferType.NORMAL)
            } else {
                binding.dialogTipMsgText.visibility = View.GONE
            }

            dialog!!.setContentView(binding.root)
            return dialog as TipOtgDialog
        }
    }
}