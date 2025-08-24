package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.topdon.lib.core.R
import com.topdon.lib.core.adapter.TargetColorAdapter
import com.topdon.lib.core.bean.ObserveBean
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.lib.core.databinding.DialogTipTargetColorBinding

/**
 * 观测-标靶颜色
 */
class TipTargetColorDialog : Dialog {

    constructor(context: Context) : super(context)

    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    class Builder {
        var dialog: TipTargetColorDialog? = null
        private var context: Context? = null
        private var closeEvent: ((targetColor: Int) -> Unit)? = null
        private var canceled = false
        private var targetColor = ObserveBean.TYPE_TARGET_COLOR_GREEN

        private lateinit var titleText: TextView
        private lateinit var imgClose: ImageView
        private lateinit var recyclerView: RecyclerView

        constructor(context: Context) {
            this.context = context
        }

        fun setCancelListener(event: ((targetColor: Int) -> Unit)? = null): Builder {
            this.closeEvent = event
            return this
        }

        fun setTargetColor(color: Int): Builder {
            this.targetColor = color
            return this
        }

        fun setCanceled(canceled: Boolean): Builder {
            this.canceled = canceled
            return this
        }

        fun dismiss() {
            this.dialog!!.dismiss()
        }

        fun create(): TipTargetColorDialog {
            if (dialog == null) {
                dialog = TipTargetColorDialog(context!!, R.style.InfoDialog)
            }
            val binding = DialogTipTargetColorBinding.inflate(
                LayoutInflater.from(context!!)
            )

            binding.tvIKnow.setOnClickListener {
                dismiss()
                closeEvent?.invoke(targetColor)
            }

            titleText = binding.tvTitle
            imgClose = binding.imgClose
            recyclerView = binding.recyclerView
            recyclerView.layoutManager = LinearLayoutManager(context!!, RecyclerView.HORIZONTAL, false)
            val targetColorAdapter = TargetColorAdapter(context!!,targetColor)
            targetColorAdapter.listener = listener@{ _, item ->
                targetColor = item
                targetColorAdapter.selectedCode(item)
            }
            recyclerView?.adapter = targetColorAdapter
            dialog!!.addContentView(
                binding.root,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            )
            val lp = dialog!!.window!!.attributes
            val wRatio =
                if (context!!.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    //竖屏
                    0.90
                } else {
                    //横屏
                    0.35
                }
            lp.width = (ScreenUtil.getScreenWidth(context!!) * wRatio).toInt() //设置宽度
            dialog!!.window!!.attributes = lp

            dialog!!.setCanceledOnTouchOutside(canceled)
            imgClose.setOnClickListener {
                dismiss()
//              closeEvent?.invoke(targetColor)
            }
            dialog!!.setContentView(binding.root)
            return dialog as TipTargetColorDialog
        }
    }
}