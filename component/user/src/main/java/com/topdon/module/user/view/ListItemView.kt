package com.topdon.module.user.view

import android.content.Context
import android.content.res.TypedArray
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.topdon.module.user.R

class ListItemView : LinearLayout {
    private lateinit var mIvLeftIcon: ImageView
    private lateinit var mIvLeftContent: TextView
    private lateinit var mIvRightContent: TextView
    private lateinit var mLineView: View
    private var lineShow: Boolean = false
    private var leftIconRes: Int = 0
    private var leftContent: String = ""
    private var rightContent: String = ""

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val ta: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.ListItemView)
        for (i in 0 until ta.indexCount) {
            when (ta.getIndex(i)) {
                R.styleable.ListItemView_list_item_left_icon ->leftIconRes =
                    ta.getResourceId(R.styleable.ListItemView_list_item_left_icon, 0)
                R.styleable.ListItemView_list_item_left_text -> leftContent =
                    ta.getString(R.styleable.ListItemView_list_item_left_text).toString()
                R.styleable.ListItemView_list_item_right_text -> rightContent =
                    ta.getString(R.styleable.ListItemView_list_item_right_text).toString()
                R.styleable.ListItemView_list_item_line-> lineShow =
                    ta.getBoolean(R.styleable.ListItemView_list_item_line, false)
            }
        }
        ta.recycle()
        initView()
    }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private fun initView() {
        inflate(context, R.layout.ui_list_item_view, this)
        mIvLeftIcon = findViewById(R.id.iv_left_icon)
        mIvLeftContent = findViewById(R.id.iv_left_content)
        mIvRightContent = findViewById(R.id.iv_right_content)
        mLineView = findViewById(R.id.view_line)
        mIvLeftIcon.setImageResource(leftIconRes)
        mIvLeftContent.text = leftContent
        mIvRightContent.text = rightContent
        mLineView.visibility = if(lineShow) View.VISIBLE else View.GONE
    }

    fun setLeftText(text: CharSequence?) {
        if (mIvLeftContent == null || TextUtils.isEmpty(text)) return
        mIvLeftContent.text = text
        mIvLeftContent.movementMethod = LinkMovementMethod.getInstance()
    }

    fun getLeftText(): String{
        if (mIvLeftContent == null) return ""
        return mIvLeftContent.text.toString()
    }

    fun setRightText(text: CharSequence?) {
        if (mIvLeftContent == null || TextUtils.isEmpty(text)) return
        mIvRightContent.text = text
        mIvRightContent.movementMethod = LinkMovementMethod.getInstance()
    }

    fun getRightText(): String{
        if (mIvRightContent == null) return ""
        return mIvRightContent.text.toString()
    }
}