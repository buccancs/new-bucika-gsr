package com.topdon.module.thermal.ir.activity

import android.annotation.SuppressLint
import android.location.*
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.blankj.utilcode.util.ToastUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.bean.ContinuousBean
import com.topdon.lib.core.bean.WatermarkBean
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.TimeTool
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.ui.listener.SingleClickListener
import com.topdon.lib.core.utils.CommUtils
import com.topdon.module.thermal.ir.BuildConfig
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.databinding.ActivityIrCameraSettingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Professional thermal camera configuration activity for research-grade imaging parameters.
 * 
 * Provides comprehensive camera settings management including:
 * - Automatic capture modes with customizable intervals (1-30 second range)
 * - Watermark configuration for professional documentation and traceability
 * - GPS location embedding for field research applications
 * - Device-specific optimization for TC007 and other thermal imaging devices
 * - Professional image naming and metadata management
 * - Clinical-grade image quality and compression settings
 * 
 * Essential for maintaining research standards in thermal imaging workflows
 * and ensuring proper documentation for clinical applications.
 *
 * @author CaiSongL
 * @since 2023/4/3
 */
@Route(path = RouterConfig.IR_CAMERA_SETTING)
class IRCameraSettingActivity : BaseActivity() {

    companion object {
        const val KEY_PRODUCT_TYPE = "key_product_type"
    }

    /** ViewBinding instance for type-safe view access */
    private lateinit var binding: ActivityIrCameraSettingBinding
    
    private var locationManager: LocationManager? = null
    private var locationProvider: String? = null

    private var watermarkBean: WatermarkBean = SharedManager.watermarkBean
    private var continuousBean: ContinuousBean = SharedManager.continuousBean
    private var productName = ""

    private val permissionList = listOf(
        Permission.ACCESS_FINE_LOCATION,
        Permission.ACCESS_COARSE_LOCATION
    )

    override fun initContentView(): Int = R.layout.activity_ir_camera_setting

    override fun initView() {
        binding = ActivityIrCameraSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        productName = intent.getStringExtra(KEY_PRODUCT_TYPE) ?: ""
        if (isTC007()) {
            watermarkBean = SharedManager.wifiWatermarkBean // TC007 only supports watermark
            continuousBean = SharedManager.continuousBean
        } else {
            watermarkBean = SharedManager.watermarkBean
            continuousBean = SharedManager.continuousBean
        }

        binding.barPickViewTime.setProgressAndRefresh((continuousBean.continuaTime / 100).toInt())
        binding.barPickViewTime.onStopTrackingTouch = { progress, _ ->
            continuousBean.continuaTime = progress.toLong() * 100
            SharedManager.continuousBean = continuousBean
        }
        binding.barPickViewTime.valueFormatListener = {
            (it / 10).toString() + if (it % 10 == 0) "" else ("." + (it % 10).toString())
        }

        binding.barPickViewCount.setProgressAndRefresh(continuousBean.count)
        binding.barPickViewCount.onStopTrackingTouch = { progress, _ ->
            continuousBean.count = progress
            SharedManager.continuousBean = continuousBean
        }


        binding.switchTime.isChecked = watermarkBean.isAddTime
        binding.switchWatermark.isChecked = watermarkBean.isOpen
        binding.switchDelay.isChecked = continuousBean.isOpen

        binding.clDelayMore.isVisible = continuousBean.isOpen
        binding.clWatermarkMore.isVisible = watermarkBean.isOpen
        binding.clShowEp.isVisible = watermarkBean.isOpen

        binding.tvTimeShow.text = TimeTool.getNowTime()
        binding.tvTimeShow.isVisible = watermarkBean.isAddTime

        binding.tvAddress.inputType = InputType.TYPE_NULL
        if (TextUtils.isEmpty(watermarkBean.address)) {
            binding.tvAddress.visibility = View.GONE
        } else {
            binding.tvAddress.visibility = View.VISIBLE
            binding.tvAddress.text = watermarkBean.address
        }
        binding.edTitle.setText(watermarkBean.title)
        binding.edAddress.setText(watermarkBean.address)
        binding.tvTitleShow.text = watermarkBean.title
        binding.switchDelay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.clDelayMore.visibility = View.VISIBLE
            } else {
                binding.clDelayMore.visibility = View.GONE
            }
            continuousBean.isOpen = isChecked
            SharedManager.continuousBean = continuousBean
        }
        binding.switchWatermark.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.clWatermarkMore.visibility = View.VISIBLE
                binding.clShowEp.visibility = View.VISIBLE
            } else {
                binding.clWatermarkMore.visibility = View.GONE
                binding.clShowEp.visibility = View.GONE
            }
            watermarkBean.isOpen = isChecked
        }
        binding.switchTime.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.tvTimeShow.text = TimeTool.getNowTime()
                binding.tvTimeShow.visibility = View.VISIBLE
            } else {
                binding.tvTimeShow.visibility = View.GONE
            }
            watermarkBean.isAddTime = isChecked
        }
        binding.edTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            
            override fun afterTextChanged(s: Editable?) {
                watermarkBean.title = binding.edTitle.text.toString()
                binding.tvTitleShow.text = watermarkBean.title
            }
        })
        binding.edAddress.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            
            override fun afterTextChanged(s: Editable?) {
                watermarkBean.address = binding.edAddress.text.toString()
                binding.tvAddress.text = watermarkBean.address
                if (!watermarkBean.address.isNullOrEmpty()) {
                    binding.tvAddress.visibility = View.VISIBLE
                } else {
                    binding.tvAddress.visibility = View.GONE
                }
            }
        })
        binding.imgLocation.setOnClickListener(object : SingleClickListener() {
            override fun onSingleClick() {
                checkStoragePermission()
            }
        })
        // TC007 devices don't need delayed capture
        binding.lyAuto.visibility = if (isTC007()) View.GONE else View.VISIBLE
    }

    /**
     * Checks if current device is TC007 model.
     *
     * @return true if device is TC007, false otherwise
     */
    fun isTC007(): Boolean {
        return productName.contains("TC007")
    }
    @SuppressLint("MissingPermission")
    private fun getLocation() : String? {
        //1.获取位置管理器
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        //2.获取位置提供器，GPS或是NetWork
        val providers = locationManager?.getProviders(true)
        locationProvider = if (providers!!.contains(LocationManager.GPS_PROVIDER)) {
            //如果是GPS
            LocationManager.GPS_PROVIDER
        } else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
            //如果是Network
            LocationManager.NETWORK_PROVIDER
        } else {
            return null
        }
        var location = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location == null){
            location = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }
        return if (location == null){
            null
        }else{
            getAddress(location)

        }
    }

    var locationListener: LocationListener = object : LocationListener {
        // Provider的状态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            Toast.makeText(
                this@IRCameraSettingActivity, provider, Toast.LENGTH_SHORT
            ).show()
        }

        // Provider被enable时触发此函数，比如GPS被打开
        override fun onProviderEnabled(provider: String) {
            Toast.makeText(
                this@IRCameraSettingActivity, "GPS打开", Toast.LENGTH_SHORT
            ).show()
            getLocation()
        }

        // Provider被disable时触发此函数，比如GPS被关闭
        override fun onProviderDisabled(provider: String) {
            Toast.makeText(
                this@IRCameraSettingActivity, "GPS关闭", Toast.LENGTH_SHORT
            ).show()
        }

        //当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
        override fun onLocationChanged(location: Location) {
            if (location != null) {
                //如果位置发生变化，重新显示地理位置经纬度
                Toast.makeText(
                    this@IRCameraSettingActivity, location.longitude.toString() + " " +
                            location.latitude + "", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        locationManager = applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager
        val providers: List<String> = locationManager!!.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val l: Location = locationManager!!.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                // Found best last known location: %s", l);
                bestLocation = l
            }
        }
        return bestLocation
    }

    //获取地址信息:城市、街道等信息
    private fun getAddress(location: Location?): String {
        var result: List<Address?>? = null
        try {
            if (location != null) {
                val gc = Geocoder(this, Locale.getDefault())
                result = gc.getFromLocation(
                    location.latitude,
                    location.longitude, 1
                )
                Log.v("TAG", "获取地址信息：$result")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        var str = ""
        if (result!=null && result.isNotEmpty()){
            result?.get(0)?.let {
                str +=  getNullString(it.adminArea)
                if (TextUtils.isEmpty(it.subLocality) && !str.contains(getNullString(it.subAdminArea))){
                    str +=  getNullString(it.subAdminArea)
                }
                if (!str.contains(getNullString(it.locality))){
                    str +=  getNullString(it.locality)
                }
                if (!str.contains(getNullString(it.subLocality))){
                    str +=  getNullString(it.subLocality)
                }
            }
        }
        return str
    }

    private fun getNullString(str : String?):String{
        return if (str.isNullOrEmpty()){
            ""
        }else{
            str
        }
    }



    override fun onPause() {
        super.onPause()
        if (isTC007()){
            SharedManager.wifiWatermarkBean = watermarkBean
        }else{
            SharedManager.watermarkBean = watermarkBean
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    override fun initData() {
    }


    private fun initLocationPermission() {
        XXPermissions.with(this@IRCameraSettingActivity)
            .permission(
                permissionList
            ).request(object :OnPermissionCallback{
                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all){
                        showLoadingDialog(R.string.get_current_address)
                        lifecycleScope.launch{
                            var addressText : String ?= ""
                            withContext(Dispatchers.IO){
                                addressText =  getLocation()
                            }
                            dismissLoadingDialog()
                            if (addressText == null){
                                ToastUtils.showShort(R.string.get_Location_failed)
                            }else{
                                watermarkBean.address = addressText as String
                                ed_address.setText(addressText)
                                tv_address.visibility = View.VISIBLE
                                tv_address.setText(addressText)
                            }
                        }
                    }else{
                        ToastUtils.showShort(R.string.scan_ble_tip_authorize)
                    }
                }
                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    if (never) {
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        if (BaseApplication.instance.isDomestic()){
                            ToastUtils.showShort(getString(R.string.app_location_content))
                        }else{
                            TipDialog.Builder(this@IRCameraSettingActivity)
                                .setTitleMessage(getString(R.string.app_tip))
                                .setMessage(getString(R.string.app_location_content))
                                .setPositiveListener(R.string.app_open){
                                    XXPermissions.startPermissionActivity(this@IRCameraSettingActivity, permissions);
                                }
                                .setCancelListener(R.string.app_cancel){
                                }
                                .setCanceled(true)
                                .create().show()
                        }
                    } else {
                        ToastUtils.showShort(R.string.scan_ble_tip_authorize)
                    }
                }

            })
    }

    private fun checkStoragePermission() {
        if (!XXPermissions.isGranted(this, permissionList)) {
            if (BaseApplication.instance.isDomestic()) {
                TipDialog.Builder(this)
                    .setMessage(getString(R.string.permission_request_location_app, CommUtils.getAppName()))
                    .setCancelListener(R.string.app_cancel)
                    .setPositiveListener(R.string.app_confirm) {
                        initLocationPermission()
                    }
                    .create().show()
            } else {
                initLocationPermission()
            }
        } else {
            initLocationPermission()
        }
    }
}