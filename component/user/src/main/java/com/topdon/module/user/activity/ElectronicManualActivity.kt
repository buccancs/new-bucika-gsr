package com.topdon.module.user.activity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.utils.Constants
import com.topdon.module.user.R
import kotlinx.android.synthetic.main.activity_electronic_manual.*
import kotlinx.android.synthetic.main.item_electronic_manual.view.item_lay
import kotlinx.android.synthetic.main.item_electronic_manual.view.item_text

/**
 * 电子说明书 或 FAQ 设备类型选择页面
 */
@Route(path = RouterConfig.ELECTRONIC_MANUAL)
class ElectronicManualActivity : BaseActivity() {

    override fun initContentView() = R.layout.activity_electronic_manual

    override fun initView() {
        val productType = intent.getIntExtra(Constants.SETTING_TYPE, 0) //0-电子说明书 1-FAQ

        title_view.setTitleText(if (productType == Constants.SETTING_BOOK) R.string.electronic_manual else R.string.app_question)

        val adapter = MyAdapter(productType == 1)
        adapter.onPickListener = { isTS001 ->
            if (isTS001) {
                if (productType == Constants.SETTING_BOOK) {
                    //电子说明书-TS001
                } else {
                    //FAQ-TS001
                    ARouter.getInstance().build(RouterConfig.QUESTION).withBoolean("isTS001", true).navigation(this)
                }
            } else {
                if (productType == Constants.SETTING_BOOK) {
                    //电子说明书-TS004
                    ARouter.getInstance().build(RouterConfig.PDF).withBoolean("isTS001", false).navigation(this)
                } else {
                    //FAQ-TS004
                    ARouter.getInstance().build(RouterConfig.QUESTION).withBoolean("isTS001", false).navigation(this)
                }
            }
        }

        electronic_manual_recycler.layoutManager = LinearLayoutManager(this)
        electronic_manual_recycler.adapter = adapter
    }

    override fun initData() {

    }




    private class MyAdapter(private val isFAQ: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var onPickListener: ((isTS001: Boolean) -> Unit)? = null

        private val optionList: ArrayList<String> = ArrayList(2)

        init {
            // 由于 TC001 的说明书为旧版本 样式， 2024-4-9 产品决定先隐藏，只放 TS004 的说明书
            if (isFAQ) {
                optionList.add("TS001")
            }
            optionList.add("TS004")
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_electronic_manual, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ItemViewHolder) {
                holder.rootView.item_text.text = optionList[position]
                holder.rootView.item_lay.setOnClickListener {
                    onPickListener?.invoke(isFAQ && position == 0)
                }
            }
        }

        override fun getItemCount(): Int = optionList.size

        private class ItemViewHolder(val rootView: View) : RecyclerView.ViewHolder(rootView)
    }

}