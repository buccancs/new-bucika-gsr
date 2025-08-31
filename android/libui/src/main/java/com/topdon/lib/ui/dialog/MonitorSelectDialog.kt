package com.topdon.lib.ui.dialog

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.lib.ui.R
import com.topdon.lib.ui.databinding.DialogMonitorSelectBinding

class MonitorSelectDialog(context: Context) : Dialog(context, R.style.InfoDialog) {

    class Builder(private val context: Context) {
        
        private var isFirstStep = true
        
        private var monitorType = 0

        private var positiveClickListener: ((select: Int) -> Unit)? = null

        fun setPositiveListener(listener: ((select: Int) -> Unit)?): Builder {
            this.positiveClickListener = listener
            return this
        }

        fun create(): MonitorSelectDialog {
            val dialog = MonitorSelectDialog(context)
            dialog.setCanceledOnTouchOutside(false)
            
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_monitor_select, null)
            dialog.setContentView(view)

            // Get view references
            val btnConfirmOrBack = view.findViewById<Button>(R.id.btn_confirm_or_back)
            val btnCancel = view.findViewById<Button>(R.id.btn_cancel)
            val clFirstStep = view.findViewById<ConstraintLayout>(R.id.cl_first_step)
            val clSecondStep = view.findViewById<ConstraintLayout>(R.id.cl_second_step)
            val tvTitle = view.findViewById<TextView>(R.id.tv_title)
            val btnSelectLocation = view.findViewById<Button>(R.id.btn_select_location)
            val tvPoint = view.findViewById<TextView>(R.id.tv_point)
            val tvLine = view.findViewById<TextView>(R.id.tv_line)
            val tvRect = view.findViewById<TextView>(R.id.tv_rect)

            val lp = dialog.window!!.attributes
            lp.width = (ScreenUtil.getScreenWidth(context) * if (ScreenUtil.isPortrait(context)) 0.85 else 0.35).toInt()
            dialog.window!!.attributes = lp

            btnConfirmOrBack.setOnClickListener {
                if (isFirstStep) {
                    if (monitorType == 0) {
                        return@setOnClickListener
                    }
                    isFirstStep = false
                    btnCancel.visibility = View.VISIBLE
                    clFirstStep.visibility = View.INVISIBLE
                    clSecondStep.visibility = View.VISIBLE
                    tvTitle.text = context.getString(R.string.select_monitor_type_step2)
                    btnConfirmOrBack.text = context.getString(R.string.select_monitor_return)
                } else {
                    isFirstStep = true
                    btnCancel.visibility = View.GONE
                    clFirstStep.visibility = View.VISIBLE
                    clSecondStep.visibility = View.GONE
                    tvTitle.text = context.getString(R.string.select_monitor_type_step1)
                    btnConfirmOrBack.text = context.getString(R.string.app_confirm)
                }
            }

            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            btnSelectLocation.setOnClickListener {
                dialog.dismiss()
                positiveClickListener?.invoke(monitorType)
            }

            tvPoint.setOnClickListener {
                updateUI(tvPoint, tvLine, tvRect, 1)
            }
            tvLine.setOnClickListener {
                updateUI(tvPoint, tvLine, tvRect, 2)
            }
            tvRect.setOnClickListener {
                updateUI(tvPoint, tvLine, tvRect, 3)
            }
            return dialog
        }

        private fun updateUI(tvPoint: TextView, tvLine: TextView, tvRect: TextView, index: Int) {
            tvPoint.isSelected = index == 1
            tvLine.isSelected = index == 2
            tvRect.isSelected = index == 3
            monitorType = index
        }
    }
}
