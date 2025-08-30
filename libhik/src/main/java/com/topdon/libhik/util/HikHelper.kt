package com.topdon.libhik.util

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.annotation.IntRange
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.elvishew.xlog.XLog
import com.hcusbsdk.Interface.FStreamCallBack
import com.hcusbsdk.Interface.JavaInterface
import com.hcusbsdk.Interface.USB_FRAME_INFO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Arrays

/**
 * 帮助实现机芯初始化、添加回调等的的工具类.
 *
 * 设置以下回调以获取数据：
 * - [setFrameListener] 设置 YUV 数据变更回调
 * - [setTempListener] 设置温度数据变更回调
 *
 * 设置码流回调设置完毕回调：
 * - [onReadyListener]
 *
 * 设置超时回调给海康擦屁股：
 * - [onTimeoutListener]
 *
 * 核心方法为：
 * - [init] 初始化
 * - [startStream] 开启码流回调
 * - [stopStream] 停止码流回调
 * - [release] 回收相关资源
 *
 * 或者如果是 [ComponentActivity] 的话，
 * 直接调用 [bind] 即可，省下调上述4个方法
 *
 * 剩下的就是诸如 对比度、旋转、镜像 之类的属性了
 *
 * Created by LCG on 2024/11/19.
 */
object HikHelper {
    /**
     * 当前设备用户 Id.
     */
    private var userId: Int = JavaInterface.USB_INVALID_USER_ID
    /**
     * 码流回调 Id，用于停止回调.
     */
    private var callbackId: Int = JavaInterface.USB_INVALID_CHANNEL

    /**
     * 定时检查有没有回调过来的 Job
     */
    private var timeoutJob: Job? = null
    /**
     * 码流数据回调.
     */
    private val streamCallBack = MyFStreamCallBack()


    /**
     * 根据指定 Activity 的生命周期执行相应初始化、开启码流、停止码流、回收资源操作.
     */
    fun bind(activity: ComponentActivity) {
        init(activity)
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                owner.lifecycleScope.launch {
                    startStream()
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                stopStream()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                release()
            }
        })
    }

    /**
     * 根据指定 Fragment 的生命周期执行相应初始化、开启码流、停止码流、回收资源操作.
     */
    fun bind(fragment: Fragment) {
        init(fragment.requireContext())
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                owner.lifecycleScope.launch {
                    startStream()
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                stopStream()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                release()
            }
        })
    }

    /**
     * 是否已成功初始化，用于判断是否需要调用 USB_Cleanup
     */
    private var hasInit = false
    /**
     * 初始化，确保设备已连接且有权限.
     * @return true-已成功初始化 false-初始化失败
     */
    fun init(context: Context): Boolean {
        if (userId != JavaInterface.USB_INVALID_USER_ID) {//已成功初始化并登录
            return true
        }

        //初始化
        if (!hasInit) {
            if (!HikCmdUtil.init()) {//初始化失败
                return false
            }
            hasInit = true
        }

        //设置日志路径
        HikCmdUtil.setLogPath(context.getExternalFilesDir("hkLog"))

        //登录
        userId = HikCmdUtil.login(context)
        return userId != JavaInterface.USB_INVALID_USER_ID
    }

    /**
     * 是否需要检查超时
     */
    @Volatile
    private var wantCheckTimeout = false
    /**
     * 开启码流回调.
     */
    suspend fun startStream() {
        if (callbackId == JavaInterface.USB_INVALID_CHANNEL) {
            callbackId = HikCmdUtil.startStream(userId, streamCallBack)
            wantCheckTimeout = true
            timeoutJob = CoroutineScope(Dispatchers.Main).launch {
                while (wantCheckTimeout) {
                    delay(5000) //5秒检查一次
                    if (System.currentTimeMillis() - streamCallBack.beforeFrameTime > 5000) {
                        wantCheckTimeout = false
                        onTimeoutListener?.invoke()
                    }
                }
            }
            onReadyListener?.invoke()
        }
    }

    /**
     * 停止码流回调，注意，YUV 及 温度数据回调也一并置 null.
     */
    fun stopStream() {
        if (callbackId != JavaInterface.USB_INVALID_CHANNEL) {
            timeoutJob?.cancel()
            HikCmdUtil.removeStreamCallback(userId, callbackId)
            callbackId = JavaInterface.USB_INVALID_CHANNEL
            wantCheckTimeout = false
        }
    }

    /**
     * 清理及回收相关资源.
     */
    fun release() {
        if (userId != JavaInterface.USB_INVALID_USER_ID) {//已登录要退出登录
            JavaInterface.getInstance().USB_Logout(userId)
            userId = JavaInterface.USB_INVALID_USER_ID
        }
        if (hasInit) {
            JavaInterface.getInstance().USB_Cleanup()
            hasInit = false
        }
        streamCallBack.onTempListener = null
        streamCallBack.onFrameListener = null
        onTimeoutListener = null
    }

    /**
     * 设置整帧数据变更回调，注意，回调不在主线程！
     */
    fun setFrameListener(listener : ((ByteArray, ByteArray) -> Unit)) {
        streamCallBack.onFrameListener = listener
    }
    /**
     * 设置温度数据变更回调，注意，回调不在主线程！
     */
    fun setTempListener(listener : ((ByteArray) -> Unit)) {
        streamCallBack.onTempListener = listener
    }

    /**
     * 给海康擦屁股，一段时间内没有回调的事件监听.
     */
    var onTimeoutListener: (() -> Unit)? = null
    /**
     * 机芯命令不支持并发执行，该回调为开启码流回调结束，在此执行配置设置.
     */
    var onReadyListener: (() -> Unit)? = null

    /**
     * 复制一份整帧数据（YUV+温度）并返回
     */
    fun copyFrameData(): ByteArray = streamCallBack.copyFrameArray()


    private class MyFStreamCallBack : FStreamCallBack {
        /**
         * YUV 数组，YUY2 格式
         */
        private val yuvArray = ByteArray(256 * 192 * 2)
        /**
         * 温度数组
         */
        private val tempArray = ByteArray(256 * 192 * 2)
        /**
         * 温度数据变更回调.
         */
        @Volatile
        var onTempListener: ((ByteArray) -> Unit)? = null
        /**
         * 一帧数据（YUY2+温度）变更回调.
         */
        @Volatile
        var onFrameListener: ((ByteArray, ByteArray) -> Unit)? = null

        /**
         * 上一帧回调的时间戳
         */
        @Volatile
        var beforeFrameTime: Long = 0

        /**
         * 复制一份整帧数据（YUV+温度）并返回
         */
        fun copyFrameArray(): ByteArray = synchronized(this) {
            val frameArray = ByteArray(256 * 192 * 4)
            System.arraycopy(yuvArray, 0, frameArray, 0, yuvArray.size)
            System.arraycopy(tempArray, 0, frameArray, yuvArray.size, tempArray.size)
            frameArray
        }

        override fun invoke(handle: Int, frameInfo: USB_FRAME_INFO) {
            beforeFrameTime = System.currentTimeMillis()
            if (frameInfo.dwBufSize != 4640 + 256 * 192 * 4) {
                XLog.w("数据长度不对！${frameInfo.dwBufSize}")
                return
            }
            val dataArray: ByteArray = Arrays.copyOf(frameInfo.pBuf, frameInfo.dwBufSize)


            /*val frameHead = FrameHead(dataArray)
            XLog.d(frameHead.toString())
            for (i in frameHead.tempRuleList.indices) {
                XLog.v(frameHead.tempRuleList[i].toString())
                XLog.v(dataArray.buildPrintStr(216 + i * 208, 88))
            }*/

            synchronized(this) {
                System.arraycopy(dataArray, dataArray.size - yuvArray.size, yuvArray, 0, yuvArray.size)
                for (i in tempArray.indices step 2) {
                    val newValue = ((dataArray[4640 + i].toInt() and 0xff) or (dataArray[4640 + i + 1].toInt() and 0xff shl 8)) + 14272
                    tempArray[i] = (newValue and 0xff).toByte()
                    tempArray[i + 1] = (newValue shr 8 and 0xff).toByte()
                }
            }
            onTempListener?.invoke(tempArray)
            onFrameListener?.invoke(yuvArray, tempArray)
        }
    }

    /**
     * 重置一些配置：开启细节增强、重置伪彩为白热、码流不叠加温度图像、横屏、不镜像.
     */
    suspend fun initConfig() = withContext(Dispatchers.IO) {
        HikCmdUtil.initConfig(userId)
    }

    /**
     * 手动快门，实际调用发现执行手动快门的耗时不短，放子线程避免阻塞主线程
     */
    suspend fun shutter() = withContext(Dispatchers.IO) {
        HikCmdUtil.shutter(userId)
    }

    /**
     * 开启关闭自动快门
     */
    suspend fun setAutoShutter(isAutoShutter: Boolean) = withContext(Dispatchers.IO) {
        HikCmdUtil.setAutoShutter(userId, isAutoShutter)
    }

    /**
     * 设置对比度，取值 `[0,100]`
     */
    suspend fun setContrast(value: Int) = withContext(Dispatchers.IO) {
        HikCmdUtil.setContrast(userId, value)
    }

    /**
     * 细节增强等级，取值 `[0,100]`
     */
    suspend fun setEnhanceLevel(value: Int) = withContext(Dispatchers.IO) {
        HikCmdUtil.setEnhanceLevel(userId, value)
    }

    /**
     * 设置镜像
     */
    suspend fun setMirror(rotateAngle: Int, isMirror: Boolean) = withContext(Dispatchers.IO) {
        HikCmdUtil.setMirror(userId, rotateAngle, isMirror)
    }

    /**
     * 设置发射率
     * @param emissivity 取值范围 `[1, 100]`
     */
    suspend fun setEmissivity(emissivity: Int) = withContext(Dispatchers.IO) {
        HikCmdUtil.setEmissivity(userId, emissivity)
    }

    /**
     * 设置测温距离，单位 cm
     * @param distance 取值范围 `[30, 200]`
     */
    suspend fun setDistance(distance: Int) = withContext(Dispatchers.IO) {
        HikCmdUtil.setDistance(userId, distance)
    }

    /**
     * 设置测温 自动(-1)/常温(1)/高温(0) 档位.
     */
    suspend fun setTemperatureMode(@IntRange(-1, 1) mode: Int) = withContext(Dispatchers.IO) {
        HikCmdUtil.setTemperatureMode(userId, mode)
    }

    /**
     * 开始锅盖校正
     */
    suspend fun startCorrect() = withContext(Dispatchers.IO) {
        HikCmdUtil.startCorrect(userId)
    }
}