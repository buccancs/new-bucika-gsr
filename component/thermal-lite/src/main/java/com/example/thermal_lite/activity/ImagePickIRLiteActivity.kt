package com.example.thermal_lite.activity

import android.graphics.Bitmap
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.example.thermal_lite.fragment.IRMonitorLiteFragment
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.ktbase.BasePickImgActivity
import com.topdon.module.thermal.ir.R

/**
 * des:单光红外拍照
 * author: CaiSongL
 * date: 2024/8/24 18:10
 **/
@Route(path = RouterConfig.IR_IMG_PICK_LITE)
class ImagePickIRLiteActivity : BasePickImgActivity() {


    var irFragment : IRMonitorLiteFragment ?= null

    override fun initView() {
        irFragment = if (savedInstanceState == null) {
            IRMonitorLiteFragment.newInstance(true)
        } else {
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as IRMonitorLiteFragment
        }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container_view, irFragment!!)
                .commit()
        }

    }

    override suspend fun getPickBitmap(): Bitmap? {
        return irFragment?.getBitmap()
    }

    override fun initData() {

    }


}