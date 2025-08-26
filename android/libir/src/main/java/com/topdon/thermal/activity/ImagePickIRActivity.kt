package com.topdon.thermal.activity

import android.graphics.Bitmap
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BasePickImgActivity
import com.topdon.thermal.R
import com.topdon.thermal.fragment.IRMonitorThermalFragment

@Route(path = RouterConfig.IR_IMG_PICK)
class ImagePickIRActivity : BasePickImgActivity() {

    var irFragment : IRMonitorThermalFragment ?= null

    override fun initView() {
        irFragment = if (savedInstanceState == null) {
            IRMonitorThermalFragment.newInstance(true)
        } else {
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as IRMonitorThermalFragment
        }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container_view, irFragment!!)
                .commit()
        }

    }

    override suspend fun getPickBitmap(): Bitmap? {
        return irFragment?.getBitmap() ?: null
    }

    override fun initData() {

    }
}
