package com.topdon.thermal.activity

import android.content.Intent
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ToastUtils
import com.elvishew.xlog.XLog
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.db.entity.ThermalEntity
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.FileTools
import com.topdon.lib.core.tools.ToastTools
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.libcom.ExcelUtil
import com.topdon.lms.sdk.BuildConfig
import com.topdon.thermal.R
import com.topdon.thermal.viewmodel.IRMonitorViewModel
import com.topdon.thermal.databinding.ActivityIrLogMpChartBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.collections.ArrayList

@Route(path = RouterConfig.IR_THERMAL_LOG_MP_CHART)
class IRLogMPChartActivity : BaseActivity() {

    private lateinit var binding: ActivityIrLogMpChartBinding

    private val viewModel: IRMonitorViewModel by viewModels()

    private var startTime = 0L

    private val permissionList by lazy {
        if (this.applicationInfo.targetSdkVersion >= 34){
            listOf(
                Permission.WRITE_EXTERNAL_STORAGE,
            )
        } else if (this.applicationInfo.targetSdkVersion == 33) {
            mutableListOf(
                Permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            mutableListOf(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    override fun initContentView() = R.layout.activity_ir_log_mp_chart

    override fun initView() {

        binding = ActivityIrLogMpChartBinding.inflate(layoutInflater)
        
        startTime = intent.getLongExtra(ExtraKeyConfig.TIME_MILLIS, 0)
        viewModel.detailListLD.observe(this) {
            dismissLoadingDialog()

            val isPoint = it?.isNotEmpty() == true && it.first().type == "point"
            binding.monitorCurrentVol.text = getString(if (isPoint) R.string.chart_temperature else R.string.chart_temperature_high)
            binding.monitorRealVol.visibility = if (isPoint) View.GONE else View.VISIBLE
            binding.monitorRealImg.visibility = if (isPoint) View.GONE else View.VISIBLE

            try {
                binding.logChartTimeChart.initEntry(it as ArrayList<ThermalEntity>)
            } catch (e: Exception) {
                XLog.e("刷新图表异常:${e.message}")
            }
        }

        binding.btnEx?.setOnClickListener {
            TipDialog.Builder(this)
                .setMessage(R.string.tip_album_temp_exportfile)
                .setPositiveListener(R.string.app_confirm) {
                    val tempData = viewModel.detailListLD.value
                    if (tempData?.isEmpty() == true) {
                        ToastTools.showShort(R.string.http_code998)
                    } else {
                        XXPermissions.with(this)
                            .permission(
                                permissionList
                            )
                            .request(object : OnPermissionCallback {
                                override fun onGranted(
                                    permissions: MutableList<String>,
                                    allGranted: Boolean
                                ) {
                                    if (allGranted) {
                                        lifecycleScope.launch {
                                            showLoadingDialog()
                                            var filePath: String? = null
                                            withContext(Dispatchers.IO) {
                                                tempData?.get(0)?.let {
                                                    filePath = ExcelUtil.exportExcel(tempData as java.util.ArrayList<ThermalEntity>?, "point" == it.type)
                                                }
                                            }
                                            dismissLoadingDialog()
                                            if (filePath.isNullOrEmpty()) {
                                                ToastTools.showShort(R.string.liveData_save_error)
                                            } else {
                                                val uri = FileTools.getUri(File(filePath))
                                                val shareIntent = Intent()
                                                shareIntent.action = Intent.ACTION_SEND
                                                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                                                shareIntent.type = "application/xlsx"
                                                startActivity(Intent.createChooser(shareIntent, getString(R.string.battery_share)))
                                            }
                                        }
                                    } else {
                                        ToastTools.showShort(R.string.scan_ble_tip_authorize)
                                    }
                                }

                                override fun onDenied(
                                    permissions: MutableList<String>,
                                    doNotAskAgain: Boolean
                                ) {
                                    if (doNotAskAgain) {

                                        if (BaseApplication.instance.isDomestic()){
                                            ToastUtils.showShort(getString(R.string.app_storage_content))
                                            return
                                        }
                                        TipDialog.Builder(this@IRLogMPChartActivity)
                                            .setTitleMessage(getString(R.string.app_tip))
                                            .setMessage(getString(R.string.app_storage_content))
                                            .setPositiveListener(R.string.app_open) {
                                                AppUtils.launchAppDetailsSettings()
                                            }
                                            .setCancelListener(R.string.app_cancel) {
                                            }
                                            .setCanceled(true)
                                            .create().show()
                                    }
                                }

                            })
                    }
                }.setCancelListener(R.string.app_cancel){
                }
                .setCanceled(true)
                .create().show()
        }
        binding.tvSavePath?.text = getString(R.string.temp_export_path) + ": " + FileConfig.excelDir
        viewModel.queryDetail(startTime)

    }

    override fun initData() {

    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
