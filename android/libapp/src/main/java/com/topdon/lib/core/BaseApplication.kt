package com.topdon.lib.core

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Process
import android.text.TextUtils
import android.util.Log
import android.webkit.WebView
import androidx.annotation.RequiresApi

import com.blankj.utilcode.util.LanguageUtils
import com.elvishew.xlog.XLog
import com.topdon.lib.core.bean.event.SocketMsgEvent
import com.topdon.lib.core.broadcast.DeviceBroadcastReceiver
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.DeviceConfig
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.db.AppDatabase
import com.topdon.lib.core.repository.FileBean
import com.topdon.lib.core.repository.TS004Repository
import com.topdon.lib.core.socket.SocketCmdUtil
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.tools.LanguageTool
import com.topdon.lib.core.utils.NetWorkUtils
import com.topdon.lib.core.utils.WifiUtil
import com.topdon.lib.core.utils.WsCmdConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.io.File
import java.util.Locale

// Temporary replacement for AppLanguageUtils
object AppLanguageUtils {
    @JvmStatic
    fun getChineseSystemLanguage(): String {
        return "zh-CN" // Default Chinese
    }
    
    @JvmStatic
    fun getSystemLanguage(): String {
        return "en-US" // Default English
    }
    
    @JvmStatic
    fun getLocaleByLanguage(language: String?): Locale {
        return when (language) {
            "zh-CN" -> Locale.SIMPLIFIED_CHINESE
            "zh-TW" -> Locale.TRADITIONAL_CHINESE
            else -> Locale.ENGLISH
        }
    }
    
    @JvmStatic
    fun attachBaseContext(context: Context?, language: String?): Context? {
        // Simple implementation - just return the original context
        return context
    }
}

abstract class BaseApplication : Application() {

    companion object {
        lateinit var instance: BaseApplication
        val usbObserver by lazy { DeviceBroadcastReceiver() }
    }
    var tau_data_H: ByteArray? = null
    var tau_data_L: ByteArray? = null

    var activitys = arrayListOf<Activity>()
    var hasOtgShow = false

    abstract fun getSoftWareCode(): String

    abstract fun isDomestic(): Boolean

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            webviewSetPath(this)
        }
        initARouter()
        onLanguageChange()

        WebSocketProxy.getInstance().onMessageListener = {
            parserSocketMessage(it)
        }

    }

    open fun initWebSocket(){
        connectWebSocket()

        // Use modern NetworkCallback for API 24+ instead of deprecated broadcast receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            
            connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Log.i("WebSocket", "Network available - connecting WebSocket")
                    connectWebSocket()
                }
                
                override fun onLost(network: Network) {
                    super.onLost(network)
                    Log.i("WebSocket", "Network lost")
                }
            })
        } else {
            // Fallback for older APIs - using deprecated broadcast receiver
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < 33) {
                registerReceiver(NetworkChangedReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
            } else {
                registerReceiver(NetworkChangedReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION), Context.RECEIVER_NOT_EXPORTED)
            }
        }
    }

    private fun connectWebSocket() {
        val ssid = WifiUtil.getCurrentWifiSSID(this) ?: return
        Log.i("WebSocket", "当前连接 Wifi SSID: $ssid")
        if (ssid.startsWith(DeviceConfig.TS004_NAME_START)) {
            SharedManager.hasTS004 = true
            WebSocketProxy.getInstance().startWebSocket(ssid)
        } else if (ssid.startsWith(DeviceConfig.TC007_NAME_START)) {
            SharedManager.hasTC007 = true
            WebSocketProxy.getInstance().startWebSocket(ssid)
        }else{
            NetWorkUtils.switchNetwork(true)
        }
    }

    fun disconnectWebSocket() {
        Log.i("WebSocket", "disconnectWebSocket()")
        WebSocketProxy.getInstance().stopWebSocket()
    }

    private fun parserSocketMessage(msgJson: String) {
        if (TextUtils.isEmpty(msgJson)) return
        EventBus.getDefault().post(SocketMsgEvent(msgJson))

        if (SharedManager.is04AutoSync) {
            when (SocketCmdUtil.getCmdResponse(msgJson)) {
                WsCmdConstants.AR_COMMAND_SNAPSHOT -> {
                    autoSaveNewest(false)
                }

                WsCmdConstants.AR_COMMAND_VRECORD -> {
                    try {
                        val data: JSONObject = JSONObject(msgJson).getJSONObject("data")
                        val enable: Boolean = data.getBoolean("enable")
                        if (!enable) {
                            autoSaveNewest(true)
                        }
                    } catch (_: Exception) {

                    }
                }
            }
        }
    }

    private fun autoSaveNewest(isVideo: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val fileList: List<FileBean>? = TS004Repository.getNewestFile(if (isVideo) 1 else 0)
            if (!fileList.isNullOrEmpty()) {
                val fileBean: FileBean = fileList[0]
                val url = "http://192.168.40.1:8080/DCIM/${fileBean.name}"
                val file = File(FileConfig.ts004GalleryDir, fileBean.name)
                TS004Repository.download(url, file)
                MediaScannerConnection.scanFile(this@BaseApplication, arrayOf(FileConfig.ts004GalleryDir), null, null)
            }
        }
    }

    private inner class NetworkChangedReceiver : BroadcastReceiver() {
        @Suppress("DEPRECATION") // Maintained for backward compatibility with older Android versions
        override fun onReceive(context: Context?, intent: Intent?) {
            if (ConnectivityManager.CONNECTIVITY_ACTION == intent?.action) {
                val manager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                
                // Use modern network checking for API 23+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val activeNetwork = manager.activeNetwork
                    val networkCapabilities = manager.getNetworkCapabilities(activeNetwork)
                    val isWifi = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                    val isConnected = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                    
                    if (isConnected && isWifi) {
                        connectWebSocket()
                    }
                    Log.i("WebSocket", "Network changed - WiFi: $isWifi, Connected: $isConnected")
                } else {
                    // Fallback for older APIs
                    val activeNetwork = manager.activeNetworkInfo ?: return
                    if (activeNetwork.isConnected && activeNetwork.type == ConnectivityManager.TYPE_WIFI) {
                        connectWebSocket()
                    }
                    Log.i("WebSocket", "Network changed - Type: ${activeNetwork.type}, Connected: ${activeNetwork.isConnected}")
                }
            }
        }
    }

    @RequiresApi(api = 28)
    open fun webviewSetPath(context: Context?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName(context)
            if (!applicationContext.packageName.equals(processName)) {
                WebView.setDataDirectorySuffix(processName!!)
            }
        }
    }

    open fun getProcessName(context: Context?): String? {
        if (context == null) return null
        val manager: ActivityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (processInfo in manager.runningAppProcesses) {
            if (processInfo.pid == Process.myPid()) {
                return processInfo.processName
            }
        }
        return null
    }

    private fun initARouter() {
        try {
            if (BuildConfig.DEBUG) {
                Log.e("TopInfrared_LOG", "router init debug - using ModernRouter (internal)")
            }

        } catch (e: Exception) {

            if (SharedManager.getHasShowClause()) {
                Log.e("TopInfrared_LOG", "router init error: ${e.message}")
            }

        }
    }

    fun clearDb() {
        GlobalScope.launch(Dispatchers.Default) {
            try {
                AppDatabase.getInstance().thermalDao().deleteZero(SharedManager.getUserId())
            } catch (e: Exception) {
                XLog.e("delete db error: ${e.message}")
            }
        }
    }

    open fun onLanguageChange() {
        val selectLan = SharedManager.getLanguage(baseContext)
        if (TextUtils.isEmpty(selectLan)) {
            if (isDomestic()) {

                val autoSelect = AppLanguageUtils.getChineseSystemLanguage()
                val locale = AppLanguageUtils.getLocaleByLanguage(autoSelect)
                LanguageUtils.applyLanguage(locale)
                SharedManager.setLanguage(baseContext, autoSelect)
            } else {

                val autoSelect = AppLanguageUtils.getSystemLanguage()
                val locale = AppLanguageUtils.getLocaleByLanguage(autoSelect)
                LanguageUtils.applyLanguage(locale)
                SharedManager.setLanguage(baseContext, autoSelect)
            }
        } else {
            val locale = AppLanguageUtils.getLocaleByLanguage(SharedManager.getLanguage(this))
            LanguageUtils.applyLanguage(locale)
        }
        WebView(this).destroy()
    }

    open fun getAppLanguage(context: Context): String? {
        return SharedManager.getLanguage(context)
    }

    fun exitAll() {
        hasOtgShow = false
        activitys.forEach {
            it.finish()
        }
    }

}
