package com.topdon.thermal.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.postDelayed
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.blankj.utilcode.util.AppUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.alibaba.android.arouter.launcher.ARouter
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseFragment
import com.topdon.lib.core.tools.DeviceTools
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.utils.CommUtils
import com.topdon.thermal.BuildConfig
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.utils.NetWorkUtils
import com.topdon.thermal.R
import com.topdon.thermal.activity.IRThermalNightActivity
import com.topdon.thermal.activity.IRThermalPlusActivity
import com.topdon.thermal.databinding.FragmentThermalIrBinding

class IRThermalFragment : BaseFragment(), View.OnClickListener {

    private var _binding: FragmentThermalIrBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentThermalIrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private var isTC007 = false

    override fun initContentView() = R.layout.fragment_thermal_ir

    override fun initView() {
        isTC007 = arguments?.getBoolean(ExtraKeyConfig.IS_TC007, false) ?: false
        binding.titleView.setTitleText(if (isTC007) "TC007" else getString(R.string.tc_has_line_device))

        binding.clOpenThermal.setOnClickListener(this)
        binding.tvMainEnter.setOnClickListener(this)
        binding.cl07ConnectTips.setOnClickListener(this)
        binding.tv07Connect.setOnClickListener(this)

        binding.tvMainEnter.isVisible = !isTC007
        binding.cl07ConnectTips.isVisible = isTC007
        binding.tv07Connect.isVisible = isTC007

        if (isTC007) {
            binding.animationView.setAnimation("TC007AnimationJSON.json")
            binding.clNotConnect.isVisible = !WebSocketProxy.getInstance().isTC007Connect()
            binding.clConnect.isVisible = WebSocketProxy.getInstance().isTC007Connect()
        } else {
            binding.animationView.setAnimation("TDAnimationJSON.json")
            checkConnect()
        }
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {

                if (WebSocketProxy.getInstance().isConnected()) {
                    NetWorkUtils.switchNetwork(true)
                }else{
                    NetWorkUtils.connectivityManager.bindProcessToNetwork(null)
                }
            }
        })
    }

    override fun initData() {

    }

    override fun onResume() {
        super.onResume()
        if (!isTC007) {
            checkConnect()
        }
    }

    override fun connected() {
        SharedManager.hasTcLine = true
        if (!isTC007) {
            binding.clConnect.isVisible = true
            binding.clNotConnect.isVisible = false
        }
    }

    override fun disConnected() {
        if (!isTC007) {
            binding.clConnect.isVisible = false
            binding.clNotConnect.isVisible = true
        }
    }

    override fun onSocketConnected(isTS004: Boolean) {
        if (isTC007 && !isTS004) {
            binding.clConnect.isVisible = true
            binding.clNotConnect.isVisible = false
        }
    }

    override fun onSocketDisConnected(isTS004: Boolean) {
        if (isTC007 && !isTS004) {
            binding.clConnect.isVisible = false
            binding.clNotConnect.isVisible = true
        }
    }

    private fun checkConnect() {
        if (DeviceTools.isConnect(isAutoRequest = false)) {
            connected()
        } else {
            disConnected()
            if (DeviceTools.findUsbDevice() != null) {
                showConnectTip()
            }
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.clOpenThermal -> {
                if (isTC007) {
                    ARouter.getInstance().build(RouterConfig.IR_THERMAL_07).navigation(requireContext())
                } else {
                    if (DeviceTools.isTC001PlusConnect()) {
                        startActivityForResult(Intent(requireContext(), IRThermalPlusActivity::class.java), 101)
                    }else if(DeviceTools.isTC001LiteConnect()){
                        ARouter.getInstance().build(RouterConfig.IR_TCLITE).navigation(activity,101)
                    } else if (DeviceTools.isHikConnect()) {
                        ARouter.getInstance().build(RouterConfig.IR_HIK_MAIN).navigation(activity)
                    } else {
                        startActivityForResult(Intent(requireContext(), IRThermalNightActivity::class.java), 101)
                    }
                }
            }
            binding.tvMainEnter -> {
                if (!DeviceTools.isConnect()) {

                    if (DeviceTools.findUsbDevice() == null) {
                        activity?.let {
                            TipDialog.Builder(it)
                                .setMessage(R.string.device_connect_tip)
                                .setPositiveListener(R.string.app_confirm)
                                .create().show()
                        }
                    } else {
                        XXPermissions.with(this)
                            .permission(listOf(
                                Permission.CAMERA
                            ))
                            .request(object : OnPermissionCallback {
                                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                                    if (allGranted) {
                                        showConnectTip()
                                    }
                                }

                                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                                    if (doNotAskAgain) {

                                        context?.let {
                                            TipDialog.Builder(it)
                                                .setTitleMessage(getString(R.string.app_tip))
                                                .setMessage(getString(R.string.app_camera_content))
                                                .setPositiveListener(R.string.app_open) {
                                                    AppUtils.launchAppDetailsSettings()
                                                }
                                                .setCancelListener(R.string.app_cancel) {
                                                }
                                                .setCanceled(true)
                                                .create().show()
                                        }
                                    }
                                }
                            })
                    }
                }
            }
            binding.cl07ConnectTips -> {
                ARouter.getInstance().build(RouterConfig.IR_CONNECT_TIPS)
                    .withBoolean(ExtraKeyConfig.IS_TC007, true)
                    .navigation(requireContext())
            }
            binding.tv07Connect -> {
                ARouter.getInstance()
                    .build(RouterConfig.IR_DEVICE_ADD)
                    .withBoolean("isTS004", false)
                    .navigation(requireContext())
            }
        }
    }

    private var tipConnectDialog: TipDialog? = null

    private var isCancelUpdateVersion = false

    private fun showConnectTip() {

        if (requireContext().applicationInfo.targetSdkVersion >= Build.VERSION_CODES.P &&
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
        ) {
            if (isCancelUpdateVersion) {
                return
            }
            if (tipConnectDialog != null && tipConnectDialog!!.isShowing) {
                return
            }
            tipConnectDialog = TipDialog.Builder(requireContext())
                .setMessage(getString(R.string.tip_target_sdk))
                .setPositiveListener(R.string.app_confirm) {
                    val url = "https://www.topdon.com/pages/pro-down?fuzzy=TS001"
                    val intent = Intent()
                    intent.action = "android.intent.action.VIEW"
                    intent.data = Uri.parse(url)
                    startActivity(intent)
                }.setCancelListener(R.string.app_cancel, {
                    isCancelUpdateVersion = true
                })
                .create()
            tipConnectDialog?.show()
        }
    }

    private fun checkStoragePermission() {
        val permissionList: List<String> = if (activity?.applicationInfo?.targetSdkVersion!! >= 34){
            listOf(
                Permission.READ_MEDIA_VIDEO,
                Permission.READ_MEDIA_IMAGES,
                Permission.WRITE_EXTERNAL_STORAGE
            )
        } else if (activity?.applicationInfo?.targetSdkVersion!! >= 33) {
            listOf(
                Permission.READ_MEDIA_VIDEO,
                Permission.READ_MEDIA_IMAGES,
                Permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            listOf(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE)
        }

        if (!XXPermissions.isGranted(requireContext(), permissionList)) {
            if (BaseApplication.instance.isDomestic()) {
                context?.let {
                    TipDialog.Builder(it)
                        .setMessage(getString(R.string.permission_request_storage_app, CommUtils.getAppName()))
                        .setCancelListener(R.string.app_cancel)
                        .setPositiveListener(R.string.app_confirm) {
                            initStoragePermission(permissionList)
                        }
                        .create().show()
                }
            } else {
                initStoragePermission(permissionList)
            }
        } else {
            initStoragePermission(permissionList)
        }
    }

    private fun initStoragePermission(permissionList: List<String>) {

    }

}
