package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogMsgBinding
import com.topdon.lib.core.utils.ScreenUtil

/**
 * Professional message dialog with modern ViewBinding implementation
 * 
 * Provides comprehensive message display functionality for thermal IR applications
 * with customizable icons, messages, and professional user interaction patterns.
 * 
 * Features:
 * - Modern ViewBinding architecture for type-safe view access
 * - Professional message display with customizable icons
 * - Responsive sizing for portrait/landscape orientations
 * - Builder pattern for fluent configuration
 * - Professional close interaction handling
 * - Research-grade user experience patterns
 * 
 * @author fylder
 * @since 2018/6/15 (modernized with ViewBinding)
 * 
 * @see DialogMsgBinding For ViewBinding implementation
 */
class MsgDialog : Dialog {

    constructor(context: Context) : super(context)

    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    /**
     * Professional builder for fluent MsgDialog configuration
     * 
     * Provides comprehensive configuration options with type-safe ViewBinding access
     * and responsive layout handling for different screen orientations.
     */
    class Builder {
        var dialog: MsgDialog? = null

        private var context: Context? = null

        private var imgRes: Int = 0
        private var message: String? = null
        private var positiveClickListener: OnClickListener? = null

        /** Professional ViewBinding for type-safe view access */
        private lateinit var binding: DialogMsgBinding

        constructor(context: Context) {
            this.context = context
        }

        /**
         * Set dialog icon from drawable resource
         * 
         * @param res Drawable resource ID for dialog icon
         * @return Builder instance for method chaining
         */
        fun setImg(@DrawableRes res: Int): Builder {
            this.imgRes = res
            return this
        }

        /**
         * Set dialog message from string
         * 
         * @param message Message text to display
         * @return Builder instance for method chaining
         */
        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }

        /**
         * Set dialog message from string resource
         * 
         * @param message String resource ID for message text
         * @return Builder instance for method chaining
         */
        fun setMessage(@StringRes message: Int): Builder {
            this.message = context!!.getString(message)
            return this
        }

        /**
         * Set close button click listener
         * 
         * @param listener Click listener for close button
         * @return Builder instance for method chaining
         */
        fun setCloseListener(listener: OnClickListener): Builder {
            this.positiveClickListener = listener
            return this
        }

        /**
         * Dismiss the dialog professionally
         */
        fun dismiss() {
            this.dialog!!.dismiss()
        }

        /**
         * Create professional MsgDialog with ViewBinding and comprehensive configuration
         * 
         * Initializes:
         * - ViewBinding for type-safe view access
         * - Professional dialog properties
         * - Responsive window sizing based on orientation
         * - Icon and message configuration
         * - Close button interaction handling
         * 
         * @return Configured MsgDialog instance
         */
        fun create(): MsgDialog {
            if (dialog == null) {
                dialog = MsgDialog(context!!, R.style.InfoDialog)
            }
            
            // Initialize ViewBinding
            binding = DialogMsgBinding.inflate(LayoutInflater.from(context!!))
            
            dialog!!.addContentView(
                binding.root, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            )
            
            // Configure responsive window sizing
            val lp = dialog!!.window!!.attributes
            val wRatio = if (context!!.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                0.9 // Portrait mode - 90% width
            } else {
                0.3 // Landscape mode - 30% width
            }
            lp.width = (ScreenUtil.getScreenWidth(context!!) * wRatio).toInt()
            dialog!!.window!!.attributes = lp

            // Setup dialog properties
            dialog!!.setCanceledOnTouchOutside(false)
            
            // Setup close button interaction
            setupCloseButton()
            
            // Configure icon display
            configureIcon()
            
            // Configure message display
            configureMessage()

            dialog!!.setContentView(binding.root)
            return dialog as MsgDialog
        }

        /**
         * Setup professional close button interaction
         */
        private fun setupCloseButton() {
            binding.dialogMsgClose.setOnClickListener {
                dismiss()
                positiveClickListener?.onClick(dialog!!)
            }
        }

        /**
         * Configure professional icon display
         */
        private fun configureIcon() {
            if (imgRes != 0) {
                binding.dialogMsgImg.visibility = View.VISIBLE
                binding.dialogMsgImg.setImageResource(imgRes)
            } else {
                binding.dialogMsgImg.visibility = View.GONE
            }
        }

        /**
         * Configure professional message display
         */
        private fun configureMessage() {
            if (message != null) {
                binding.dialogMsgText.visibility = View.VISIBLE
                binding.dialogMsgText.setText(message, TextView.BufferType.NORMAL)
            } else {
                binding.dialogMsgText.visibility = View.GONE
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