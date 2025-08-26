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

class TipProgressDialog : Dialog {

    constructor(context: Context) : super(context)

    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    class Builder {
        var dialog: TipProgressDialog? = null

        private var context: Context? = null
        private var message: String? = null
        private var canceleable = true

        private lateinit var binding: DialogTipProgressBinding

        constructor(context: Context) {
            this.context = context
        }

        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }

        fun setMessage(@StringRes message: Int): Builder {
            this.message = context!!.getString(message)
            return this
        }

        fun setCanceleable(cancal: Boolean): Builder {
            this.canceleable = cancal
            return this
        }

        fun dismiss() {
            this.dialog!!.dismiss()
        }

        fun create(): TipProgressDialog {
            if (dialog == null) {
                dialog = TipProgressDialog(context!!, R.style.InfoDialog)
            }
            
            binding = DialogTipProgressBinding.inflate(LayoutInflater.from(context!!))

            dialog!!.addContentView(
                binding.root,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            )
            
            val lp = dialog!!.window!!.attributes
            val wRatio = if (context!!.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                0.52
            } else {
                0.35
            }
            lp.width = (ScreenUtil.getScreenWidth(context!!) * wRatio).toInt()
            dialog!!.window!!.attributes = lp

            dialog!!.setCanceledOnTouchOutside(canceleable)
            
            configureMessage()

            dialog!!.setContentView(binding.root)
            return dialog as TipProgressDialog
        }

        private fun configureMessage() {
            if (message != null) {
                binding.dialogTipLoadMsg.visibility = View.VISIBLE
                binding.dialogTipLoadMsg.setText(message, TextView.BufferType.NORMAL)
            } else {
                binding.dialogTipLoadMsg.visibility = View.GONE
            }
        }
    }

    interface OnClickListener {
        fun onClick(dialog: DialogInterface)
    }
}
