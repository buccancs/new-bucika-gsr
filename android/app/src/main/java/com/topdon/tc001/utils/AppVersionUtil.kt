package com.topdon.tc001.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ZipUtils
import com.elvishew.xlog.XLog
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.HttpConfig
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.utils.AppUtil
import com.topdon.lms.sdk.LMS
import com.topdon.lms.sdk.activity.LmsUpdateDialog
import com.topdon.lms.sdk.bean.AppInfoBean
import com.topdon.lms.sdk.utils.NetworkUtil
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.lms.sdk.xutils.common.Callback
import com.topdon.lms.sdk.xutils.common.task.PriorityExecutor
import com.topdon.lms.sdk.xutils.http.RequestParams
import com.topdon.tc001.R
import com.topdon.tc001.tools.VersionTools
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class AppVersionUtil(
    private val context: Context,
    private val dotIsShowListener: DotIsShowListener?
) {
    private var completeReceiver: DownloadCompleteReceiver? = null
    private var downloadManager: DownloadManager? = null
    private var fileName = ""
    private var downloadId = 0L

    interface DotIsShowListener {
        fun isShow(show: Boolean)
        fun version(version: String)
    }

    fun checkVersion(isShowDialog: Boolean) {
        if (downloadManager == null) {
            downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        }

        if (!NetworkUtil.isConnected(context)) {
            TToast.shortToast(context, com.topdon.lms.sdk.R.string.lms_setting_http_error)
            return
        }

        LMS.getInstance().checkAppUpdate { commonBean ->
            if (commonBean.code == LMS.SUCCESS) {
                val appInfoBean = LMS.getInstance().updateAppInfoBean
                XLog.w("bcf", "app更新信息: ${GsonUtils.toJson(appInfoBean)}")
                
                appInfoBean?.let { bean ->
                    if (bean.versionCode > getDealVersionCode()) {
                        if (isShowDialog) {
                            var information = ""
                            bean.softConfigOtherTypeVOList?.forEach { updateDescription ->
                                if (updateDescription.descType == 3) {
                                    information = updateDescription.textDescription
                                }
                            }
                            showUpdateDialog(context, bean.downloadPackageUrl, information, bean.forcedUpgradeFlag.toInt())
                        }
                        
                        dotIsShowListener?.apply {
                            isShow(true)
                            version(bean.versionNo)
                        }
                        HttpConfig.hasNewVersion = true
                    } else {
                        HttpConfig.hasNewVersion = false
                    }
                } ?: run {
                    HttpConfig.hasNewVersion = false
                }
            } else {
                HttpConfig.hasNewVersion = false
            }
        }
    }

    private fun getDealVersionCode(): Float {
        return AppUtil.getVersionCode(context) / 10f
    }

    private fun showNewVersionDialog(bean: AppInfoBean) {
        var information = ""
        bean.softConfigOtherTypeVOList?.forEach { updateDescription ->
            if (updateDescription.descType == 3) {
                information = updateDescription.textDescription
            }
        }

        val dialogBuilder = TipDialog.Builder(context)
            .setMessage(information)
            .setTitleMessage(context.getString(R.string.updata_new_version_update))

        if (bean.forcedUpgradeFlag.toInt() == 1) {

            dialogBuilder.setPositiveListener(R.string.app_confirm) {
                handleDownloadClick(bean.downloadPackageUrl)
            }.create().show()
        } else {
            dialogBuilder.setPositiveListener(R.string.app_confirm) {
                handleDownloadClick(bean.downloadPackageUrl)
            }.setCancelListener(R.string.app_cancel) {
                SharedManager.setVersionCheckDate(System.currentTimeMillis())
            }.create().show()
        }
    }

    private fun handleDownloadClick(url: String) {
        if (downloadId > 0L) {
            TToast.shortToast(context, context.getString(R.string.installation_package_downloading))
        } else {
            TToast.shortToast(context, context.getString(R.string.installation_package_downloading_tips))
            startDownload(url)
        }
    }

    private fun startDownload(url: String) {
        completeReceiver = DownloadCompleteReceiver()
        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        
        if (Build.VERSION.SDK_INT < 33) {
            context.registerReceiver(completeReceiver, intentFilter)
        } else {
            context.registerReceiver(completeReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        }

        val uri = Uri.parse(url)
        val downloadRequest = DownloadManager.Request(uri).apply {
            setTitle(context.getString(R.string.tips_download_information))
            setDescription(context.getString(R.string.installation_package_download_progress))
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            
            fileName = "topinfrared${System.currentTimeMillis()}.zip"
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        }

        downloadManager?.let { manager ->
            downloadId = manager.enqueue(downloadRequest)
            VersionTools.setMDownloadId(downloadId)
        }
    }

    private inner class DownloadCompleteReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                installApk()
            }
        }
    }

    fun installApk() {
        downloadId = 0L
        VersionTools.setMDownloadId(0L)
        completeReceiver?.let { context.unregisterReceiver(it) }
        
        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath, fileName)
            val localFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath)
            val files = ZipUtils.unzipFile(file, localFile)
            
            if (files?.isNotEmpty() == true) {
                AppUtil.installApp(context, files[0])
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun showUpdateDialog(context: Context, url: String, content: String, forcedUpgradeFlag: Int) {
        LmsUpdateDialog.Build
            .setContentStr(content)
            .setUpgradeFlag(forcedUpgradeFlag)
            .setSureEvent {
                download(url)
                null
            }
            .setCancelEvent { null }
            .build(context)
    }

    fun download(url: String) {
        val params = RequestParams()
        
        try {

            val splitUrl = url.split("?")
            val urlParams = splitUrl[1].split("&")
            val params1 = urlParams[0].split("=")
            val params2 = urlParams[1].split("=")
            val params3 = urlParams[2].split("=")
            
            params.apply {
                addBodyParameter(params1[0], params1[1])
                addBodyParameter(params2[0], params2[1])
                addBodyParameter(params3[0], params3[1])
                uri = splitUrl[0]
            }
        } catch (e: Exception) {
            XLog.e("bcf", "升级接口解析异常")
            params.uri = url
        }

        fileName = "topinfrared${System.currentTimeMillis()}.zip"
        val path = "${context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath}${File.separator}$fileName"
        XLog.e("bcf", "download path: $path")
        
        params.apply {
            setSaveFilePath(path)
            setCacheDirName(fileName)
            setAutoResume(true)
            setExecutor(PriorityExecutor(3, true))
        }

        com.topdon.lms.sdk.xutils.x.http().get(params, object : Callback.ProgressCallback<File> {
            override fun onWaiting() {
                XLog.e("bcf", "onWaiting")
            }

            override fun onStarted() {
                XLog.e("bcf", "onStarted")
            }

            override fun onLoading(total: Long, current: Long, isDownloading: Boolean) {
                XLog.w("bcf", "onLoading: $current/$total")
                val progress = (current * 100 / total).toInt()
                LmsUpdateDialog.Build.setProgressNum(progress / 100f)
            }

            override fun onSuccess(result: File) {
                XLog.e("bcf", "onSuccess, start install apk")
                LmsUpdateDialog.Build.dismiss()
                installApkNew()
            }

            override fun onError(ex: Throwable, isOnCallback: Boolean) {
                ex.printStackTrace()
                XLog.e("bcf", "onError ${ex.message}")
            }

            override fun onCancelled(cex: Callback.CancelledException) {
                cex.printStackTrace()
                XLog.e("bcf", "onCancelled ${cex.message}")
            }

            override fun onFinished() {
                XLog.e("bcf", "onFinished")
            }
        })
    }

    fun installApkNew() {
        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath, fileName)
            val localFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath)
            val files = ZipUtils.unzipFile(file, localFile)
            
            if (files?.isNotEmpty() == true) {
                AppUtil.installApp(context, files[0])
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}