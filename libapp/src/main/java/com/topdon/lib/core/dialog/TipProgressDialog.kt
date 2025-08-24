package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.TextView
import androidx.annotation.StringRes
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogTipProgressBinding
import com.topdon.lib.core.utils.ScreenUtil

/**
 * Professional progress dialog with modern ViewBinding implementation
 * 
 * Provides comprehensive loading indication functionality for thermal IR applications
 * with customizable messages, responsive sizing, and professional user experience patterns.
 * 
 * Features:
 * - Modern ViewBinding architecture for type-safe view access
 * - Professional progress indication with spinner animation
 * - Customizable loading messages from string resources
 * - Responsive sizing for portrait/landscape orientations
 * - Builder pattern for fluent configuration
 * - Professional cancellation handling
 * - Research-grade user experience patterns
 * 
 * @author fylder
 * @since 2018/6/15 (modernized with ViewBinding)
 * 
 * @see DialogTipProgressBinding For ViewBinding implementation
 */
class TipProgressDialog : Dialog {

    constructor(context: Context) : super(context)

    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    /**
     * Professional builder for fluent TipProgressDialog configuration
     * 
     * Provides comprehensive configuration options with type-safe ViewBinding access
     * and responsive layout handling for different screen orientations.
     */
    class Builder {
        var dialog: TipProgressDialog? = null

        private var context: Context? = null
        private var message: String? = null
        private var canceleable = true

        /** Professional ViewBinding for type-safe view access */
        private lateinit var binding: DialogTipProgressBinding

        constructor(context: Context) {
            this.context = context
        }

        /**
         * Set loading message from string
         * 
         * @param message Loading message text to display
         * @return Builder instance for method chaining
         */
        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }

        /**
         * Set loading message from string resource
         * 
         * @param message String resource ID for loading message text
         * @return Builder instance for method chaining
         */
        fun setMessage(@StringRes message: Int): Builder {
            this.message = context!!.getString(message)
            return this
        }

        /**
         * Set dialog cancellation behavior
         * 
         * @param cancal true if dialog can be cancelled by touch outside, false otherwise
         * @return Builder instance for method chaining
         */
        fun setCanceleable(cancal: Boolean): Builder {
            this.canceleable = cancal
            return this
        }

        /**
         * Dismiss the dialog professionally
         */
        fun dismiss() {
            this.dialog!!.dismiss()
        }

        /**
         * Create professional TipProgressDialog with ViewBinding and comprehensive configuration
         * 
         * Initializes:
         * - ViewBinding for type-safe view access
         * - Professional dialog properties
         * - Responsive window sizing based on orientation
         * - Progress bar animation
         * - Message configuration
         * - Cancellation handling
         * 
         * @return Configured TipProgressDialog instance
         */
        fun create(): TipProgressDialog {
            if (dialog == null) {
                dialog = TipProgressDialog(context!!, R.style.InfoDialog)
            }
            
            // Initialize ViewBinding
            binding = DialogTipProgressBinding.inflate(LayoutInflater.from(context!!))

            dialog!!.addContentView(
                binding.root,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            )
            
            // Configure responsive window sizing
            val lp = dialog!!.window!!.attributes
            val wRatio = if (context!!.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                0.52 // Portrait mode - 52% width
            } else {
                0.35 // Landscape mode - 35% width
            }
            lp.width = (ScreenUtil.getScreenWidth(context!!) * wRatio).toInt()
            dialog!!.window!!.attributes = lp

            // Setup professional dialog properties
            dialog!!.setCanceledOnTouchOutside(canceleable)
            
            // Configure message display
            configureMessage()

            dialog!!.setContentView(binding.root)
            return dialog as TipProgressDialog
        }

        /**
         * Configure professional message display
         */
        private fun configureMessage() {
            if (message != null) {
                binding.dialogTipLoadMsg.visibility = View.VISIBLE
                binding.dialogTipLoadMsg.setText(message, TextView.BufferType.NORMAL)
            } else {
                binding.dialogTipLoadMsg.visibility = View.GONE
            }
        }
    }

    /**
     * Professional click listener interface for dialog interactions
     */
    interface OnClickListener {
        fun onClick(dialog: DialogInterface)
    }
}