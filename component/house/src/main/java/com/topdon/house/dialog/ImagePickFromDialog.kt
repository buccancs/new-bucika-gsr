package com.topdon.house.dialog

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.topdon.house.R
import com.topdon.lib.core.utils.ScreenUtil
import kotlinx.android.synthetic.main.dialog_image_pick_from.view.*

/**
 * 房屋检测 - 房屋图片拾取来源选择弹框.
 *
 * Created by LCG on 2024/1/23.
 */
class ImagePickFromDialog(private val context: Context) : Dialog(context, R.style.InfoDialog), View.OnClickListener {

    /**
     * 拾取事件监听.
     * 0-相册 1-可见光相机 2-热成像相机
     */
    private var onSelectListener: ((type: Int) -> Unit)? = null

    private lateinit var contentView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        contentView = LayoutInflater.from(context).inflate(R.layout.dialog_image_pick_from, null)
        contentView.tv_gallery.setOnClickListener(this)
        contentView.tv_light_camera.setOnClickListener(this)
        setContentView(contentView)

        window?.let {
            val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * if (isPortrait) 0.76f else 0.48f).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }
    }

    /**
     * 设置拾取事件监听.
     * 0-相册 1-可见光相机 2-热成像相机
     */
    fun setSelectListener(l: ((type: Int) -> Unit)): ImagePickFromDialog {
        this.onSelectListener = l
        return this
    }

    override fun onClick(v: View?) {
        when (v) {
            contentView.tv_gallery -> {//从相册获取
                dismiss()
                onSelectListener?.invoke(0)
            }
            contentView.tv_light_camera -> {//相机拍照
                dismiss()
                onSelectListener?.invoke(1)
            }
        }
    }
}