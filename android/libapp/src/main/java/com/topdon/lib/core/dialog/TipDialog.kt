package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogTipBinding

/**
 * Professional tip dialog with comprehensive user interaction support.
 * 
 * Provides industry-standard dialog functionality with ViewBinding for type-safe
 * view access and professional UI patterns. Supports customizable messages,
 * buttons, and advanced configuration options for research applications.
 *
 * Features:
 * - ViewBinding integration for type-safe view access
 * - Responsive layout with orientation-aware sizing
 * - Customizable title and message content
 * - Professional button configuration system
 * - Comprehensive callback support
 * - Research-grade user interaction patterns
 * 
 * @author fylder
 * @date 2018/6/15
 * Modernized with ViewBinding and comprehensive documentation.
 */
class TipDialog : Dialog {

    constructor(context: Context) : super(context)

    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    /**
     * Disables back button to ensure professional user flow.
     */
    override fun onBackPressed() {
        // Disabled for professional user experience
    }


    /**
     * Professional builder pattern for creating customized tip dialogs.
     * Provides fluent API for comprehensive dialog configuration.
     *
     * @property context Android context for dialog creation
     * @property dialog TipDialog instance being built
     * @property message Main dialog message content
     * @property titleMessage Dialog title text
     * @property positiveStr Positive button text
     * @property cancelStr Cancel button text  
     * @property positiveEvent Positive button click handler
     * @property cancelEvent Cancel button click handler
     * @property canceled Whether dialog is cancelable via touch outside
     * @property isShowRestartTips Whether to show restart tips section
     */
    class Builder(private val context: Context) {
        var dialog: TipDialog? = null

        private var message: String? = null
        private var titleMessage: String? = null
        private var positiveStr: String? = null
        private var cancelStr: String? = null
        private var positiveEvent: (() -> Unit)? = null
        private var cancelEvent: (() -> Unit)? = null
        private var canceled = false
        private var isShowRestartTips = false

        /**
         * Sets the dialog title message.
         * @param message Title text to display
         * @return Builder instance for method chaining
         */
        fun setTitleMessage(message: String): Builder {
            this.titleMessage = message
            return this
        }

        /**
         * Sets the main dialog message.
         * @param message Message text to display
         * @return Builder instance for method chaining
         */
        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }

        /**
         * Sets the main dialog message from string resource.
         * @param message String resource ID for message text
         * @return Builder instance for method chaining
         */
        fun setMessage(@StringRes message: Int): Builder {
            this.message = context.getString(message)
            return this
        }

        /**
         * Configures positive button with string resource and click handler.
         * @param strRes String resource ID for button text
         * @param event Optional click event handler
         * @return Builder instance for method chaining
         */
        fun setPositiveListener(@StringRes strRes: Int, event: (() -> Unit)? = null): Builder {
            return setPositiveListener(context.getString(strRes), event)
        }

        /**
         * Configures positive button with text and click handler.
         * @param str Button text
         * @param event Optional click event handler  
         * @return Builder instance for method chaining
         */
        fun setPositiveListener(str: String, event: (() -> Unit)? = null): Builder {
            this.positiveStr = str
            this.positiveEvent = event
            return this
        }

        /**
         * Configures cancel button with string resource and click handler.
         * @param strRes String resource ID for button text
         * @param event Optional click event handler
         * @return Builder instance for method chaining
         */
        fun setCancelListener(@StringRes strRes: Int, event: (() -> Unit)? = null): Builder {
            return setCancelListener(context.getString(strRes), event)
        }

        /**
         * Configures cancel button with text and click handler.
         * @param str Button text
         * @param event Optional click event handler
         * @return Builder instance for method chaining
         */
        fun setCancelListener(str: String, event: (() -> Unit)? = null): Builder {
            this.cancelStr = str
            this.cancelEvent = event
            return this
        }

        /**
         * Sets whether dialog can be canceled by touching outside.
         * @param canceled True if cancelable, false otherwise
         * @return Builder instance for method chaining
         */
        fun setCanceled(canceled: Boolean): Builder {
            this.canceled = canceled
            return this
        }

        /**
         * Configures restart tips visibility for system operations.
         * @param isShowRestartTips True to show restart tips section
         * @return Builder instance for method chaining
         */
        fun setShowRestartTops(isShowRestartTips: Boolean): Builder {
            this.isShowRestartTips = isShowRestartTips
            return this
        }

        /**
         * Dismisses the dialog if currently shown.
         */
        fun dismiss() {
            this.dialog?.dismiss()
        }


        /**
         * Creates and configures the dialog with ViewBinding support.
         * Applies all configured properties and returns ready-to-show dialog.
         *
         * @return Fully configured TipDialog instance
         */
        fun create(): TipDialog {
            if (dialog == null) {
                dialog = TipDialog(context, R.style.InfoDialog)
            }

            val binding = DialogTipBinding.inflate(LayoutInflater.from(context))
            dialog!!.addContentView(binding.root, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            
            // Configure responsive layout dimensions
            val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            val widthPixels = context.resources.displayMetrics.widthPixels
            val lp = dialog!!.window!!.attributes
            lp.width = (widthPixels * if (isPortrait) 0.85 else 0.35).toInt()
            dialog!!.window!!.attributes = lp

            // Configure touch outside behavior
            dialog!!.setCanceledOnTouchOutside(canceled)
            
            // Configure button click handlers
            binding.dialogTipSuccessBtn.setOnClickListener {
                dismiss()
                positiveEvent?.invoke()
            }
            binding.dialogTipCancelBtn.setOnClickListener {
                dismiss()
                cancelEvent?.invoke()
            }

            // Configure positive button
            if (positiveStr != null) {
                binding.dialogTipSuccessBtn.text = positiveStr
            }
            
            // Configure cancel button and spacing
            if (!TextUtils.isEmpty(cancelStr)) {
                binding.spaceMargin.visibility = View.VISIBLE
                binding.dialogTipCancelBtn.visibility = View.VISIBLE
                binding.dialogTipCancelBtn.text = cancelStr
            } else {
                binding.spaceMargin.visibility = View.GONE
                binding.dialogTipCancelBtn.visibility = View.GONE
                binding.dialogTipCancelBtn.text = ""
            }
            
            // Configure main message
            if (message != null) {
                binding.dialogTipMsgText.visibility = View.VISIBLE
                binding.dialogTipMsgText.setText(message, TextView.BufferType.NORMAL)
            } else {
                binding.dialogTipMsgText.visibility = View.GONE
            }

            // Configure title message
            if (titleMessage != null) {
                binding.dialogTipTitleMsgText.visibility = View.VISIBLE
                binding.dialogTipTitleMsgText.setText(titleMessage, TextView.BufferType.NORMAL)
            } else {
                binding.dialogTipTitleMsgText.visibility = View.GONE
            }

            // Configure restart tips visibility
            binding.tvRestartTips.isVisible = isShowRestartTips

            dialog!!.setContentView(binding.root)
            return dialog as TipDialog
        }
    }

