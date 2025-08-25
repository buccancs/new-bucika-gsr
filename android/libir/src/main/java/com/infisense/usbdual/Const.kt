package com.infisense.usbdual

import android.os.Environment
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.DeviceType
import com.energy.iruvc.utils.DualCameraParams
import java.io.File

object Const {

    const val TYPE_IR = 0
    const val TYPE_IR_DUAL = 1

    const val RESTART_USB = 1000
    const val HANDLE_CONNECT = 10001
    const val HANDLE_REGISTER = 10002
    const val SHOW_LOADING = 1003
    const val HIDE_LOADING = 1004
    const val SHOW_RESTART_MESSAGE = 1005
    const val HIDE_LOADING_FINISH = 1006

    var isReadFlashData = false

    var isDeviceConnected = false

    const val TAU_HIGH_GAIN_ASSET_PATH = "tau/V262_mini256带防尘片_H.bin"
    const val TAU_HIGH_LOW_ASSET_PATH = "tau/V262_mini256带防尘片_L.bin"

    var USE_DEVICE_TYPE = DeviceType.WN_256

    const val PID = 0x5840
    const val SENSOR_WIDTH = 256
    const val SENSOR_HEIGHT = 384

    var CAMERA_WIDTH = 640
    var CAMERA_HEIGHT = 480
    const val CAMERA_LOW_FPS = 15
    const val CAMERA_HIGH_FPS = 30

    var IR_WIDTH = 192
    var IR_HEIGHT = 256
    var VL_WIDTH = 480
    var VL_HEIGHT = 640

    val IR_ROTATE = DualCameraParams.TypeLoadParameters.ROTATE_0

    val IR_MIRROR_FLIP_TYPE = CommonParams.PropImageParamsValue.MirrorFlipType.NO_MIRROR_FLIP

    var DUAL_WIDTH = 480
    var DUAL_HEIGHT = 640

    const val INFISENSE_DIR = "infiray"
    val INFISENSE_SAVE_DIR = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}${File.separator}$INFISENSE_DIR"

    const val SP_KEY_ALIGN_INIT_DATA = "alignInitData"
    const val SP_KEY_ALIGN_ANGLE = "alignAngle"

    const val SP_KEY_PATTERN_WIDTH = 10
    const val SP_KEY_PATTERN_HEIGHT = 7
    const val SP_KEY_PATTERN_SPACE = 0.024
    const val SP_KEY_CALIB_CNT = 13
    const val SP_KEY_VL_DISTORTED = true
    const val SP_KEY_CAM_DIST = 0.013
    const val SP_KEY_IS_HORIZONTAL = true
    const val SP_KEY_RATIO_THRESH = 0.9
    const val SP_KEY_ROT_THRESH = 1.5
