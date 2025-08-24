package com.topdon.lib.ui.adapter

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.ScreenUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.topdon.lib.ui.R
import com.topdon.lib.ui.bean.ColorBean
import com.topdon.lib.ui.listener.SingleClickListener
import com.topdon.lib.ui.databinding.UiItemMenuSecondViewBinding

@Deprecated("旧的双光菜单，已重构过了")
class MenuPANightAdapter(data: MutableList<ColorBean>, layoutId : Int, private val isDual: Boolean) : BaseQuickAdapter<ColorBean, BaseViewHolder>(layoutId,data) {

    var listener: ((index: Int) -> Unit)? = null

    override fun convert(holder: BaseViewHolder, item: ColorBean) {
        if (!isDual){
            val with = (ScreenUtils.getScreenWidth() / 2)
            holder.itemView.layoutParams = ViewGroup.LayoutParams(with, ViewGroup.LayoutParams.WRAP_CONTENT)
            val imageSize = (ScreenUtils.getScreenWidth() * 62 / 375f).toInt()
            val itemImg = holder.itemView.findViewById<android.widget.ImageView>(R.id.item_menu_tab_img)
            val layoutParams = itemImg.layoutParams
            layoutParams.width = imageSize
            layoutParams.height = imageSize
            itemImg.layoutParams = layoutParams
        }else{
            holder.itemView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val imageSize = (ScreenUtils.getScreenWidth() * 62 / 375f).toInt()
            val itemImg = holder.itemView.findViewById<android.widget.ImageView>(R.id.item_menu_tab_img)
            val layoutParams = itemImg.layoutParams
            layoutParams.width = imageSize
            layoutParams.height = imageSize
            itemImg.layoutParams = layoutParams
        }
        if (item.isSelect){
            holder.setImageResource(R.id.item_menu_tab_img,item.res)
        }else{
            holder.setImageResource(R.id.item_menu_tab_img,item.n_res)
        }
        holder.setText(R.id.item_menu_tab_text,item.name)
        holder.itemView.setOnClickListener(object :SingleClickListener(){
            override fun onSingleClick() {
                listener?.invoke(data.indexOf(item))
            }
        })
        if (item.isSelect) {
            holder.setTextColor(R.id.item_menu_tab_text,ContextCompat.getColor(context, R.color.white))
        } else {
            holder.setTextColor(R.id.item_menu_tab_text,ContextCompat.getColor(context, R.color.font_third_color))
        }
    }
}