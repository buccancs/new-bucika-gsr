package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.*
// ARouter replaced with ModernRouter (internal) for BucikaGSR
// import com.alibaba.android.arouter.launcher.ARouter
import com.topdon.lib.core.R
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.tools.NumberTools
import com.topdon.lib.core.tools.UnitTools
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.lib.core.databinding.DialogTipEmissivityBinding

/**
 * 发射率的提示弹窗
 */
class TipEmissivityDialog : Dialog {

    constructor(context: Context) : super(context)

    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    var onDismissListener :((check : Boolean) -> Unit) ?= null

    class Builder {
        private var isTC007: Boolean = false
        private var text: String = ""
        private var radiation: Float = 0f
        private var distance: Float = 0f
        private var environment: Float = 0f
        var dialog: TipEmissivityDialog? = null
        private var context: Context? = null
        private var title: String? = null
        private var message: String? = null
        private var closeEvent: ((check: Boolean) -> Unit)? = null
        private var canceled = false
        private var hasCheck = false

        private lateinit var titleText: TextView
        private lateinit var messageText: TextView
        private lateinit var checkBox: CheckBox
        private lateinit var imgClose: ImageView

        constructor(context: Context) {
            this.context = context
        }

        fun setMessage(message: Int): Builder {
            this.message = context!!.getString(message)
            return this
        }

        fun setTitle(title: Int): Builder {
            this.title = context!!.getString(title)
            return this
        }

        fun setDataBean(environment: Float,distance : Float,radiation : Float,text : String,isTC007 : Boolean = false): Builder {
            this.environment = environment
            this.distance = distance
            this.radiation = radiation
            this.text = text
            this.isTC007 = isTC007
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

        fun create(): TipEmissivityDialog {
            if (dialog == null) {
                dialog = TipEmissivityDialog(context!!, R.style.InfoDialog)
            }

            val binding = DialogTipEmissivityBinding.inflate(
                LayoutInflater.from(context!!)
            )

            binding.tvEnvironmentTitle.text = context!!.getString(R.string.thermal_config_environment) + ":"
            binding.tvDistanceTitle.text = context!!.getString(R.string.thermal_config_distance) + ":"

            binding.dialogTipSuccessBtn.setOnClickListener {
                dialog?.onDismissListener?.invoke(hasCheck)
                dismiss()
            }
            binding.dialogTipCancelBtn.setOnClickListener {
                dialog?.onDismissListener?.invoke(hasCheck)
                // ARouter.getInstance().build(RouterConfig.IR_SETTING)
                //     .withBoolean(ExtraKeyConfig.IS_TC007, isTC007)
                //     .navigation(context)
                // Navigation replaced with ModernRouter for BucikaGSR - IR Settings navigation
                dismiss()
            }
            val tvEmissivity = binding.tvEmissivity
            val tvEmissivityMaterials = binding.tvEmissivityMaterials
            val tvEnvironmentValue = binding.tvEnvironmentValue
            val tvDistanceValue = binding.tvDistanceValue

            if (text.isNotEmpty()){
                tvEmissivityMaterials.text = text
                tvEmissivityMaterials.visibility = View.VISIBLE
            }else{
                tvEmissivityMaterials.visibility = View.GONE
            }
            tvEmissivity.text = "${context?.getString(R.string.thermal_config_radiation)}: ${
                NumberTools.to02(radiation)}"
            tvEnvironmentValue.text = UnitTools.showC(environment)
            tvDistanceValue.text = "${
                NumberTools.to02(distance)}m"
            titleText = binding.tvTitle
            messageText = binding.dialogTipMsgText
            checkBox = binding.dialogTipCheck
            imgClose = binding.imgClose
            dialog!!.addContentView(
                binding.root,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            )
            val lp = dialog!!.window!!.attributes
            val wRatio =
                if (context!!.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    //竖屏
                    0.75
                } else {
                    //横屏
                    0.35
                }
            lp.width = (ScreenUtil.getScreenWidth(context!!) * wRatio).toInt() //设置宽度
            dialog!!.window!!.attributes = lp

            dialog!!.setCanceledOnTouchOutside(canceled)
            checkBox.isChecked = false
            hasCheck = false
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                hasCheck = isChecked
            }
            imgClose.setOnClickListener {
                dismiss()
                closeEvent?.invoke(hasCheck)
            }
            //title
            if (title != null) {
                titleText.setText(title, TextView.BufferType.NORMAL)
            }
            //msg
//            if (message != null) {
//                messageText.visibility = View.VISIBLE
//                messageText.setText(message, TextView.BufferType.NORMAL)
//            } else {
//                messageText.visibility = View.GONE
//            }
            dialog!!.setContentView(binding.root)
            return dialog as TipEmissivityDialog
        }
    }
