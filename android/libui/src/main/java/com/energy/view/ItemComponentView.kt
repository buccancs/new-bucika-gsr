package com.energy.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.topdon.lib.ui.R

/**
 * Created by fengjibo on 2023/5/29.
 */
class ItemComponentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var onItemClickListener: OnItemClickListener? = null
    private val playButtonView: Button
    private val titleTextView: TextView
    private val switchCompat: SwitchCompat
    private val valueTextView: TextView

    init {
        initView(attrs)
        
        playButtonView = findViewById(R.id.play_button)
        titleTextView = findViewById(R.id.tv_title)
        switchCompat = findViewById(R.id.switch_compat)
        valueTextView = findViewById(R.id.text_value)
    }

    private fun initView(attrs: AttributeSet?) {
        inflate(context, R.layout.layout_item_component, this)

        attrs?.let {
            val attributes = context.obtainStyledAttributes(it, R.styleable.item_component)

            val titleText = attributes.getText(R.styleable.item_component_item_title)
            titleTextView.text = titleText

            val switchEnable = attributes.getBoolean(R.styleable.item_component_item_switch_enable, false)
            switchCompat.visibility = if (switchEnable) View.VISIBLE else View.GONE

            val buttonEnable = attributes.getBoolean(R.styleable.item_component_item_button_enable, false)
            playButtonView.visibility = if (buttonEnable) View.VISIBLE else View.GONE

            val buttonText = attributes.getText(R.styleable.item_component_item_button_text)
            playButtonView.text = buttonText

            val valueEnable = attributes.getBoolean(R.styleable.item_component_item_value_enable, false)
            valueTextView.visibility = if (valueEnable) View.VISIBLE else View.GONE

            val valueText = attributes.getText(R.styleable.item_component_item_value_text)
            valueTextView.text = valueText

            attributes.recycle()
        }

        setOnClickListener {
            onItemClickListener?.onItemClick()
        }
    }

    fun interface OnItemClickListener {
        fun onItemClick()
    }

    fun interface OnSwitchCompatCheckListener {
        fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean, id: Int)
    }

    fun interface OnButtonClickListener {
        fun onClick(view: View, id: Int)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        this.onItemClickListener = onItemClickListener
    }

    fun setTitle(text: String) {
        titleTextView.text = text
    }

    fun getTitle(): String = titleTextView.text.toString()

    fun setPlayButtonText(text: String) {
        playButtonView.text = text
    }

    fun setValueText(text: String) {
        valueTextView.text = text
    }

    fun setSwitchCompatEnable(switchCompatEnable: Boolean) {
        switchCompat.visibility = if (switchCompatEnable) View.VISIBLE else View.GONE
    }

    fun setPlayButtonEnable(playButtonEnable: Boolean) {
        playButtonView.visibility = if (playButtonEnable) View.VISIBLE else View.GONE
    }

    fun setValueTextViewEnable(valueTextViewEnable: Boolean) {
        valueTextView.visibility = if (valueTextViewEnable) View.VISIBLE else View.GONE
    }

    fun setSwitchCompatChecked(switchCompatChecked: Boolean) {
        switchCompat.isChecked = switchCompatChecked
    }

    fun setSwitchCompatCheckListener(onSwitchCompatCheckListener: OnSwitchCompatCheckListener?) {
        switchCompat.setOnCheckedChangeListener { buttonView, isChecked ->
            onSwitchCompatCheckListener?.onCheckedChanged(buttonView, isChecked, id)
        }
    }

    fun setButtonClickListener(onButtonClickListener: OnButtonClickListener?) {
        playButtonView.setOnClickListener { view ->
            onButtonClickListener?.onClick(view, id)
        }
    }
}