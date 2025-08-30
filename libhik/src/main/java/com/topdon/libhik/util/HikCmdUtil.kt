package com.topdon.libhik.util

import android.content.Context
import com.elvishew.xlog.XLog
import com.hcusbsdk.Interface.FStreamCallBack
import com.hcusbsdk.Interface.JavaInterface
import com.hcusbsdk.Interface.USB_COMMAND_STATE
import com.hcusbsdk.Interface.USB_DEVICE_INFO
import com.hcusbsdk.Interface.USB_DEVICE_REG_RES
import com.hcusbsdk.Interface.USB_IMAGE_CONTRAST
import com.hcusbsdk.Interface.USB_IMAGE_ENHANCEMENT
import com.hcusbsdk.Interface.USB_IMAGE_VIDEO_ADJUST
import com.hcusbsdk.Interface.USB_STREAM_CALLBACK_PARAM
import com.hcusbsdk.Interface.USB_SYSTEM_SERIAL_DATA_TRANSMISSION
import com.hcusbsdk.Interface.USB_THERMAL_STREAM_PARAM
import com.hcusbsdk.Interface.USB_THERMOMETRY_BASIC_PARAM
import com.hcusbsdk.Interface.USB_USER_LOGIN_INFO
import com.hcusbsdk.Interface.USB_VIDEO_PARAM
import kotlinx.coroutines.delay
import java.io.File

/**
 * 收发机芯指令的工具类.
 *
 * 海康 SDK 的 API 写得又臭又长，全都放一个类里的话一大坨的东西找不到重点，
 * 用这个类把这些又臭又长的代码装起来。
 *
 * Created by LCG on 2024/11/19.
 */
object HikCmdUtil {
    /**
     * 仅用于日志输出
     */
    private const val TAG = "HikCmd"


    /**
     * 执行初始化.
     */
    fun init(): Boolean = if (JavaInterface.getInstance().USB_Init()) {
        XLog.i("$TAG init() 初始化成功")
        true
    } else {
        XLog.e("$TAG init() 初始化失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
        false
    }

    /**
     * 设置日志文件存放的目录，注意，目录.
     */
    fun setLogPath(logFile: File?) {
        if (logFile != null) {
            JavaInterface.getInstance().USB_SetLogToFile(JavaInterface.INFO_LEVEL, logFile.absolutePath, 1)
        }
    }

    /**
     * 登录并返回 userId.
     * @return 若登录失败则返回 [JavaInterface.USB_INVALID_USER_ID]
     */
    fun login(context: Context): Int {
        //获取设备数量
        val deviceCount: Int = JavaInterface.getInstance().USB_GetDeviceCount(context)
        if (deviceCount < 0) {
            XLog.e("$TAG login() 获取设备数量失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
            return JavaInterface.USB_INVALID_USER_ID
        }
        if (deviceCount == 0) {
            XLog.e("$TAG login() 目标设备未连接！")
            return JavaInterface.USB_INVALID_USER_ID
        }

        //获取设备信息
        val deviceInfoArray: Array<USB_DEVICE_INFO> = Array(JavaInterface.MAX_DEVICE_NUM) {
            USB_DEVICE_INFO()
        }
        if (!JavaInterface.getInstance().USB_EnumDevices(deviceCount, deviceInfoArray)) {
            XLog.e("$TAG login() 获取设备信息失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
            return JavaInterface.USB_INVALID_USER_ID
        }

        val loginInfo = USB_USER_LOGIN_INFO()
        loginInfo.dwTimeout = 5000 //超时时间
        loginInfo.dwDevIndex = deviceInfoArray[0].dwIndex
        loginInfo.dwVID = deviceInfoArray[0].dwVID
        loginInfo.dwPID = deviceInfoArray[0].dwPID
        loginInfo.dwFd = deviceInfoArray[0].dwFd
        val userId: Int = JavaInterface.getInstance().USB_Login(loginInfo, USB_DEVICE_REG_RES())
        if (userId == JavaInterface.USB_INVALID_USER_ID) {
            XLog.e("$TAG login() 登录失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
        } else {
            XLog.i("$TAG login() 登录成功")
        }
        return userId
    }

    /**
     * 重置一些配置：开启细节增强、重置伪彩为白热、码流不叠加温度图像、横屏、不镜像.
     */
    suspend fun initConfig(userId: Int) {
        //开启细节增强、重置伪彩为白热
        val imageParam = USB_IMAGE_ENHANCEMENT()
        if (JavaInterface.getInstance().USB_GetImageEnhancement(userId, imageParam)) {
            if (imageParam.byLSEDetailEnabled != 1.toByte() || imageParam.byPaletteMode != 1.toByte()) {
                XLog.i("$TAG initConfig() 开启细节增强、重置伪彩(白热)")
                //1-白热 2-黑热 10-融合1 11-彩虹 12-融合2 13-铁红1 14-铁红2 15-深褐色
                //16-色彩1 17-色彩2 18-冰火 19-雨 20-红热 21-绿热 22-深蓝
                imageParam.byPaletteMode = 1
                imageParam.byLSEDetailEnabled = 1 //0-关闭 1-开启
                if (JavaInterface.getInstance().USB_SetImageEnhancement(userId, imageParam)) {
                    checkCommandState(userId) {
                        XLog.v("$TAG initConfig() 设置图像增强参数 $it")
                    }
                } else {
                    XLog.e("$TAG initConfig() 设置图像增强参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
                }
            }
        } else {
            XLog.e("$TAG initConfig() 获取图像增强参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
        }

        //码流不叠加温度图像
        val basicTempParam = USB_THERMOMETRY_BASIC_PARAM()
        if (JavaInterface.getInstance().USB_GetThermometryBasicParam(userId, basicTempParam)) {
            if (basicTempParam.byThermometryStreamOverlay != 1.toByte()) {
                XLog.i("$TAG initConfig() 设置码流不叠加温度图像")
                basicTempParam.byThermometryStreamOverlay = 1 //2-叠加 1-不叠加
                if (JavaInterface.getInstance().USB_SetThermometryBasicParam(userId, basicTempParam)) {
                    checkCommandState(userId) {
                        XLog.v("$TAG initConfig() 设置测温基本参数 $it")
                    }
                } else {
                    XLog.e("$TAG initConfig() 设置测温基本参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
                }
            }
        } else {
            XLog.e("$TAG initConfig() 获取测温基本参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
        }
    }


    /**
     * 手动快门
     */
    fun shutter(userId: Int) {
        JavaInterface.getInstance().USB_SetImageManualCorrect(userId)
    }

    /**
     * 开启关闭自动快门
     */
    suspend fun setAutoShutter(userId: Int, isAutoShutter: Boolean) {
        XLog.i("$TAG setAutoShutter() ${if (isAutoShutter) "开启" else "关闭"} 自动快门")
        val param = USB_SYSTEM_SERIAL_DATA_TRANSMISSION()
        param.byMode = 2 //0-保留 1-读 2-写
        param.wDeviceCMDFlag = 0
        param.dwDeviceCMD = 0x2001 //命令 code
        param.dwValue = if (isAutoShutter) 1 else 0
        if (JavaInterface.getInstance().USB_SetSystemSerialData(userId, param)) {
            checkCommandState(userId) {
                XLog.v("$TAG setAutoShutter() 发送自定协议参数 $it")
            }
        } else {
            XLog.e("$TAG setAutoShutter() 发送自定协议参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
        }
    }

    /**
     * 开始锅盖校正
     */
    suspend fun startCorrect(userId: Int) {
        XLog.i("$TAG startCorrect() 开始锅盖校正")
        val param = USB_SYSTEM_SERIAL_DATA_TRANSMISSION()
        param.byMode = 2 //0-保留 1-读 2-写
        param.wDeviceCMDFlag = 0
        param.dwDeviceCMD = 0xf026 //命令 code
        param.dwValue = 2
        if (JavaInterface.getInstance().USB_SetSystemSerialData(userId, param)) {
            checkCommandState(userId) {
                XLog.v("$TAG startCorrect() 发送自定协议参数 $it")
            }
        } else {
            XLog.e("$TAG startCorrect() 发送自定协议参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
        }
    }



    /**
     * 设置对比度.
     * @param contrast 取值范围 `[0,100]`
     */
    suspend fun setContrast(userId: Int, contrast: Int) {
        XLog.i("$TAG setContrast($contrast) 设置对比度")
        val param = USB_IMAGE_CONTRAST()
        param.dwContrast = contrast
        if (JavaInterface.getInstance().USB_SetImageContrast(userId, param)) {
            checkCommandState(userId) {
                XLog.v("$TAG setContrast() 设置对比度参数 $it")
            }
        } else {
            XLog.e("$TAG setContrast() 设置对比度参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
        }
    }



    /* ******************************  图像增强参数 ImageEnhancement  ****************************** */
    /**
     * 设置细节增强(超分)等级
     */
    suspend fun setEnhanceLevel(userId: Int, level: Int) {
        val param = USB_IMAGE_ENHANCEMENT()
        if (JavaInterface.getInstance().USB_GetImageEnhancement(userId, param)) {
            if (param.dwLSEDetailLevel == level) {
                return
            }
            XLog.i("$TAG setEnhanceLevel() 变更细节增强等级 ${param.dwLSEDetailLevel}->$level")
            param.dwLSEDetailLevel = level
            if (JavaInterface.getInstance().USB_SetImageEnhancement(userId, param)) {
                checkCommandState(userId) {
                    XLog.v("$TAG setEnhanceLevel() 设置图像增强参数 $it")
                }
            } else {
                XLog.e("$TAG setEnhanceLevel() 设置图像增强参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
            }
        } else {
            XLog.e("$TAG setEnhanceLevel() 获取图像增强参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
        }
    }



    /* ******************************  视频调整参数 ImageVideoAdjust  ****************************** */
    /**
     * 设置镜像.
     * @param isMirror true-左右镜像 false-不镜像
     */
    suspend fun setMirror(userId: Int, rotateAngle: Int, isMirror: Boolean) {
        val videoAdjust = USB_IMAGE_VIDEO_ADJUST()
        if (JavaInterface.getInstance().USB_GetImageVideoAdjust(userId, videoAdjust)) {
            val mirrorCode: Byte = if (isMirror) (if (rotateAngle == 0 || rotateAngle == 180) 2 else 3) else 0
            if (videoAdjust.byImageFlipStyle == mirrorCode) {
                return
            }
            XLog.i("$TAG setMirror() 变更镜像 ${videoAdjust.byImageFlipStyle}->$mirrorCode")
            videoAdjust.byImageFlipStyle = mirrorCode
            if (JavaInterface.getInstance().USB_SetImageVideoAdjust(userId, videoAdjust)) {
                checkCommandState(userId) {
                    XLog.v("$TAG setMirror() 设置视频调整参数 $it")
                }
            } else {
                XLog.e("$TAG setMirror() 设置视频调整参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
            }
        } else {
            XLog.e("$TAG setMirror() 获取视频调整参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
        }
    }



    /* ******************************  码流回调  ****************************** */
    /**
     * 添加码流回调.
     * @return 码流回调 Id，若失败则为 [JavaInterface.USB_INVALID_CHANNEL]
     */
    suspend fun startStream(userId: Int, callback: FStreamCallBack): Int {
        //设置横屏、不镜像
        val videoAdjust = USB_IMAGE_VIDEO_ADJUST()
        if (JavaInterface.getInstance().USB_GetImageVideoAdjust(userId, videoAdjust)) {
            val isRotateChange = videoAdjust.byCorridor != 0.toByte()
            val isMirrorChange = videoAdjust.byImageFlipStyle != 0.toByte()
            if (isRotateChange || isMirrorChange) {
                XLog.i("$TAG startStream() 设置视频调整参数：横屏、不镜像")
                videoAdjust.byCorridor = 0 //0-256x192 1-192x256
                videoAdjust.byImageFlipStyle = 0 //镜像 0-关闭 1-中心 2-左右 3-上下
                if (JavaInterface.getInstance().USB_SetImageVideoAdjust(userId, videoAdjust)) {
                    checkCommandState(userId) {
                        XLog.v("$TAG startStream() 设置视频调整参数 $it")
                    }
                } else {
                    XLog.e("$TAG startStream() 设置视频调整参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
                }
            }
        } else {
            XLog.e("$TAG startStream() 获取视频调整参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
        }

        //设置视频参数
        //横屏 256x392 竖屏 192x520 即高度需要为(192+4)*2 或 (256+4)*2
        //旋转自己上层实现，原始出图固定 256x192 就行了
        XLog.i("$TAG startStream() 设置视频参数：256x392")
        val videoParam = USB_VIDEO_PARAM()
        videoParam.dwVideoFormat = JavaInterface.USB_STREAM_YUY2
        videoParam.dwWidth = 256
        videoParam.dwHeight = 392
        videoParam.dwFramerate = 25 //25帧
        if (JavaInterface.getInstance().USB_SetVideoParam(userId, videoParam)) {
            checkCommandState(userId) {
                XLog.v("$TAG startStream() 设置视频参数 $it")
            }
        } else {
            XLog.w("$TAG startStream() 设置视频参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
            return JavaInterface.USB_INVALID_CHANNEL
        }

        //开启码流回调
        XLog.i("$TAG startStream() 开启码流回调")
        val callbackParam = USB_STREAM_CALLBACK_PARAM()
        callbackParam.dwStreamType = JavaInterface.USB_STREAM_YUY2
        callbackParam.fnStreamCallBack = callback
        val callbackId: Int = JavaInterface.getInstance().USB_StartStreamCallback(userId, callbackParam)
        if (callbackId == JavaInterface.USB_INVALID_CHANNEL) {
            XLog.w("$TAG startStream() 添加码流回调失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
            return JavaInterface.USB_INVALID_CHANNEL
        } else {
            XLog.v("$TAG startStream() 开启码流回调成功")
        }

        //设置码流类型
        XLog.i("$TAG startStream() 设置码流类型：8")
        val streamParam = USB_THERMAL_STREAM_PARAM()
        streamParam.byVideoCodingType = 8
        if (JavaInterface.getInstance().USB_SetThermalStreamParam(userId, streamParam)) {
            checkCommandState(userId) {
                XLog.v("$TAG startStream() 设置码流类型 $it")
            }
        } else {
            XLog.w("$TAG startStream() 设置码流类型失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
            return JavaInterface.USB_INVALID_CHANNEL
        }

        return callbackId
    }
    /**
     * 移除码流回调.
     */
    fun removeStreamCallback(userId: Int, callbackId: Int) {
        if (callbackId != JavaInterface.USB_INVALID_CHANNEL) {
            if (!JavaInterface.getInstance().USB_StopChannel(userId, callbackId)) {
                XLog.w("$TAG removeStreamCallback() 移除码流回调失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
            }
        }
    }



    /* ******************************  测温基本参数 ThermometryBasicParam  ****************************** */
    /**
     * 设置发射率
     * @param emissivity 取值范围 `[1, 100]`
     */
    suspend fun setEmissivity(userId: Int, emissivity: Int) {
        val param = USB_THERMOMETRY_BASIC_PARAM()
        if (JavaInterface.getInstance().USB_GetThermometryBasicParam(userId, param)) {
            if (param.dwEmissivity == emissivity) {
                return
            }
            XLog.i("$TAG setEmissivity() 变更发射率 ${param.dwEmissivity}->$emissivity")
            param.dwEmissivity = emissivity
            if (JavaInterface.getInstance().USB_SetThermometryBasicParam(userId, param)) {
                checkCommandState(userId) {
                    XLog.v("$TAG setEmissivity() 设置测温基本参数 $it")
                }
            } else {
                XLog.e("$TAG setEmissivity() 设置测温基本参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
            }
        } else {
            XLog.e("$TAG setEmissivity() 获取测温基本参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
        }
    }

    /**
     * 设置测温距离，单位 cm
     * @param distance 取值范围 `[30, 200]`
     */
    suspend fun setDistance(userId: Int, distance: Int) {
        val param = USB_THERMOMETRY_BASIC_PARAM()
        if (JavaInterface.getInstance().USB_GetThermometryBasicParam(userId, param)) {
            if (param.dwDistance == distance) {
                return
            }
            XLog.i("$TAG setDistance() 变更测温距离 ${param.dwDistance}cm->${distance}cm")
            param.dwDistance = distance
            if (JavaInterface.getInstance().USB_SetThermometryBasicParam(userId, param)) {
                checkCommandState(userId) {
                    XLog.v("$TAG setDistance() 设置测温基本参数 $it")
                }
            } else {
                XLog.e("$TAG setDistance() 设置测温基本参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
            }
        } else {
            XLog.e("$TAG setDistance() 获取测温基本参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
        }
    }

    /**
     * 设置测温 自动(-1)/常温(1)/高温(0) 档位.
     */
    suspend fun setTemperatureMode(userId: Int, mode: Int) {
        val param = USB_THERMOMETRY_BASIC_PARAM()
        if (JavaInterface.getInstance().USB_GetThermometryBasicParam(userId, param)) {
            //优先级：自动 > 测温范围
            val oldAuto: Byte = param.byTemperatureRangeAutoChangedEnabled
            val oldRange: Byte = param.byTemperatureRange
            val newAuto: Byte = if (mode == -1) 1 else 0 //0-关闭 1-开启
            val newRange: Byte = if (mode == 1) 2 else 3 //2(-20~150常温档) 3(100~550高温档)
            if (oldAuto == newAuto && (oldRange == newRange || newRange == 1.toByte())) {
                return
            }
            XLog.i("$TAG setTemperatureMode() 变更测温档位 ${getTempModeStr(oldAuto, oldRange)}->${getTempModeStr(newAuto, newRange)}")
            param.byTemperatureRangeAutoChangedEnabled = newAuto
            param.byTemperatureRange = newRange
            if (JavaInterface.getInstance().USB_SetThermometryBasicParam(userId, param)) {
                checkCommandState(userId) {
                    XLog.v("$TAG setTemperatureMode() 设置测温基本参数 $it")
                }
            } else {
                XLog.e("$TAG setTemperatureMode() 设置测温基本参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
            }
        } else {
            XLog.e("$TAG setTemperatureMode() 获取测温基本参数失败！错误码：${JavaInterface.getInstance().USB_GetLastError()}")
        }
    }

    private fun getTempModeStr(autoEnable: Byte, range: Byte): String {
        if (autoEnable == 1.toByte()) {
            return "自动"
        }
        return if (range == 2.toByte()) "常温档" else "高温档"
    }







    private fun getCommandState(userId: Int): Int {
        val state = USB_COMMAND_STATE()
        JavaInterface.getInstance().USB_GetCommandState(userId, state)
        return state.byState.toInt() and 0xff
    }

    /**
     * 在执行设置命令后，检查设备配置状态直到设备端完成请求
     */
    private suspend inline fun checkCommandState(userId: Int, callback: (state: String) -> Unit) {
        var commandState = 1 //1为 设备端尚未完成先前的请求
        while (commandState == 1) {
            commandState = getCommandState(userId)
            callback.invoke("Command State: ${getCommandState(userId).toComStateStr()}")
            if (commandState == 1) {
                //恢复默认、设置专家测温校正参数海康文档建议等待 1000 毫秒，其他请求建议不低于 10 毫秒
                delay(100)
            }
        }
    }

    private fun Int.toComStateStr(): String = when (this) {
        0x00 -> "0x00: 正常"
        0x01 -> "0x01: 设备端尚未完成先前的请求"
        0x02 -> "0x02: 设备端不允许特定请求的状态"
        0x03 -> "0x03: 设备端的实际电源模式不足以完成请求"
        0x04 -> "0x04: SET_CUR 请求设置的参数超出限定范围"
        0x05 -> "0x05: 不支持的 Unint ID"
        0x06 -> "0x06: 不支持的 CS ID"
        0x07 -> "0x07: 不支持的请求命令类型"
        0x08 -> "0x08: SET_CUR 请求设置的参数在限定范围，但设置值无效"
        0x09 -> "0x09: 不支持的子功能"
        0x0a -> "0x0a: 设备端功能执行异常"
        0x0b -> "0x0b: 设备端内部协议流程异常"
        0x0c -> "0x0c: 大数据传输流程异常"
        0xff -> "0xff: 未知错误"
        else -> "0x${this.toString(16)}: 不在文档内的状态码"
    }
}