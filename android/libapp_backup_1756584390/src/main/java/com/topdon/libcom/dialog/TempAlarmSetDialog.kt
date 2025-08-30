package com.topdon.libcom.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.CompoundButton
import com.topdon.lib.core.R
import com.topdon.lib.core.bean.AlarmBean

class TempAlarmSetDialog(
    context: Context,
    private val isEdit: Boolean,
) : Dialog(context, R.style.app_compat_dialog), CompoundButton.OnCheckedChangeListener {

    var alarmBean = AlarmBean()
    var onSaveListener: ((bean: AlarmBean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        // Simplified implementation
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        // Simplified implementation
    }

    private fun save() {
        onSaveListener?.invoke(alarmBean)
        dismiss()
    }
}