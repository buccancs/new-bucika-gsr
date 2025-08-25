package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.TextView
import androidx.annotation.StringRes
import com.topdon.lib.core.R
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.lib.core.databinding.DialogTipShutterBinding


/**
 * 自动快门提示弹窗
 * @author: CaiSongL
 * @date: 2023/4/13 10:57
 */
class TipShutterDialog : Dialog {


    constructor(context: Context) : super(context)

    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    class Builder(private val context: Context) {
        var dialog: TipShutterDialog? = null
        private var titleRes: Int? = null
        private var message: CharSequence? = null
        private var closeEvent: ((check: Boolean) -> Unit)? = null
        private var canceled = false

        fun setTitle(@StringRes resId: Int): Builder {
            this.titleRes = resId
            return this
        }

        fun setMessage(message: CharSequence): Builder {
            this.message = message
            return this
        }

        fun setMessage(@StringRes message: Int): Builder {
            this.message = context.getString(message)
            return this
        }


        fun setCancelListener(event: ((check: Boolean) -> Unit)? = null): Builder {
            this.closeEvent = event
            return this
        }

        fun setCanceled(canceled: Boolean): Builder {
            this.canceled = canceled
            return this
        }

        fun dismiss() {
            this.dialog!!.dismiss()
        }


        fun create(): TipShutterDialog {
            if (dialog == null) {
                dialog = TipShutterDialog(context, R.style.InfoDialog)
            }
            val binding = DialogTipShutterBinding.inflate(
                LayoutInflater.from(context)
            )
            dialog!!.addContentView(binding.root, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            dialog!!.setCanceledOnTouchOutside(canceled)

            val lp = dialog!!.window!!.attributes
            lp.width = (ScreenUtil.getScreenWidth(context) * if (ScreenUtil.isPortrait(context)) 0.85 else 0.35).toInt() //设置宽度
            dialog!!.window!!.attributes = lp

            binding.tvIKnow.setOnClickListener {
                dismiss()
                closeEvent?.invoke(binding.dialogTipCheck.isChecked)
            }
            binding.imgClose.setOnClickListener {
                dismiss()
                closeEvent?.invoke(binding.dialogTipCheck.isChecked)
            }
            if (titleRes != null) {
                binding.tvTitle.setText(titleRes!!)
            }
            if (message != null) {
                binding.dialogTipMsgText.visibility = View.VISIBLE
                binding.dialogTipMsgText.setText(message, TextView.BufferType.NORMAL)
            } else {
                binding.dialogTipMsgText.visibility = View.GONE
            }
            dialog!!.setContentView(binding.root)
            return dialog as TipShutterDialog
        }
    }

