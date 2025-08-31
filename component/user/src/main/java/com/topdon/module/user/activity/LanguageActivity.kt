package com.topdon.module.user.activity

import android.content.Intent
import androidx.recyclerview.widget.LinearLayoutManager
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.AppLanguageUtils
import com.topdon.lib.core.tools.ConstantLanguages
import com.topdon.module.user.R
import com.topdon.module.user.adapter.LanguageAdapter
import kotlinx.android.synthetic.main.activity_language.*

class LanguageActivity : BaseActivity() {

    private val adapter by lazy { LanguageAdapter(this) }

    private var selectIndex = 0

    override fun initContentView() = R.layout.activity_language

    override fun initView() {
        title_view.setRightClickListener {//保存
            val localeStr: String = when (selectIndex) {
                0 -> ConstantLanguages.ENGLISH
                1 -> ConstantLanguages.RU
                2 -> ConstantLanguages.JA
                3 -> ConstantLanguages.GERMAN
                4 -> ConstantLanguages.FR
                5 -> ConstantLanguages.PT
                6 -> ConstantLanguages.ES
                7 -> ConstantLanguages.IT
                8 -> ConstantLanguages.PL
                9 -> ConstantLanguages.CS
                10 -> ConstantLanguages.UK
                11 -> ConstantLanguages.NL
                12 -> ConstantLanguages.ZH_CN
                13 -> ConstantLanguages.ZH_TW
                else -> AppLanguageUtils.getSystemLanguage()
            }
            setResult(RESULT_OK, Intent().also { it.putExtra("localeStr", localeStr) })
            finish()
        }

        language_recycler.layoutManager = LinearLayoutManager(this)
        language_recycler.adapter = adapter
        adapter.listener = object : LanguageAdapter.ItemOnClickListener {
            override fun onClick(position: Int) {
                adapter.setSelect(position)
                selectIndex = position
            }
        }
    }

    override fun initData() {

    }

    override fun onResume() {
        super.onResume()
        showLanguage()
    }

    private fun showLanguage() {
        val selectIndex = when (SharedManager.getLanguage(this)) {
            ConstantLanguages.ENGLISH -> 0
            ConstantLanguages.RU -> 1
            ConstantLanguages.JA -> 2
            ConstantLanguages.GERMAN -> 3
            ConstantLanguages.FR -> 4
            ConstantLanguages.PT -> 5
            ConstantLanguages.ES -> 6
            ConstantLanguages.IT -> 7
            ConstantLanguages.PL -> 8
            ConstantLanguages.CS -> 9
            ConstantLanguages.UK -> 10
            ConstantLanguages.NL -> 11
            ConstantLanguages.ZH_CN -> 12
            ConstantLanguages.ZH_TW -> 13
            else -> 0
        }
        adapter.setSelect(selectIndex)
        this.selectIndex = selectIndex
    }

}

