package com.energy.iruvc.utils

// Comprehensive CommonParams with all thermal imaging enums and classes
object CommonParams {
    
    enum class DataFlowMode {
        IMAGE_ONLY,
        TEMP_ONLY,
        IMAGE_AND_TEMP_OUTPUT,
        Y16_MODE,
        IMAGE_OUTPUT,
        TEMP_OUTPUT,
        IR_OUTPUT,
        KBC_OUTPUT,
        HBC_DPC_OUTPUT,
        VBC_OUTPUT,
        TNR_OUTPUT,
        SNR_OUTPUT,
        AGC_OUTPUT,
        DDE_OUTPUT,
        GAMMA_OUTPUT,
        MIRROR_OUTPUT
    }
    
    enum class PreviewPathChannel {
        PREVIEW_PATH0,
        PREVIEW_PATH1
    }
    
    enum class StartPreviewSource {
        SOURCE_SENSOR,
        SOURCE_FILE
    }
    
    enum class StartPreviewMode {
        VOC_DVP_MODE,
        MIPI_MODE
    }
    
    enum class PseudoColorUsbDualType {
        WHITE_HOT,
        BLACK_HOT,
        IRON,
        COOL,
        RAINBOW,
        LAVA,
        GLOBOW,
        GRADED_FIRE,
        HOTTEST,
        SEPIA,
        WHITE_HOT_MODE,
        BLACK_HOT_MODE,
        IRONBOW_MODE,
        RAINBOW_MODE,
        AURORA_MODE,
        MEDICAL_MODE,
        RED_HOT_MODE,
        JUNGLE_MODE,
        NIGHT_MODE
    }
    
    enum class PseudoColorType {
        WHITE_HOT,
        BLACK_HOT,
        IRON,
        COOL,
        RAINBOW,
        LAVA,
        GLOBOW,
        GRADED_FIRE,
        HOTTEST,
        PSEUDO_1,
        PSEUDO_2,
        PSEUDO_3,
        PSEUDO_4,
        PSEUDO_5,
        PSEUDO_6,
        PSEUDO_7,
        PSEUDO_8,
        PSEUDO_9,
        PSEUDO_10,
        PSEUDO_11,
        GRAYSCALE,
        RED_HOT,
        BLUE_HOT,
        GREEN_HOT
    }
    
    enum class IRPROCSRCFMTType {
        IRPROC_SRC_FMT_ARGB8888,
        IRPROC_SRC_FMT_RGB888,
        IRPROC_SRC_FMT_GRAY8,
        IRPROC_SRC_FMT_YUV420,
        IRPROC_SRC_FMT_YUV422,
        IRPROC_SRC_FMT_Y14,
        Y16,
        YUV,
        RGB
    }
    
    enum class GainStatus {
        LOW,
        HIGH,
        AUTO,
        HIGH_GAIN,
        LOW_GAIN
    }
    
    enum class GainMode {
        MANUAL,
        AUTO,
        SCENE_ADAPTIVE,
        HIGH,
        LOW,
        GAIN_MODE_HIGH_LOW
    }
    
    enum class FRAMEFORMATType {
        Y16,
        Y8,
        RGB,
        ARGB,
        YUV420,
        YUV422,
        YUYV,
        NV21,
        FRAME_FORMAT_YUYV,
        FRAME_FORMAT_Y16,
        FRAME_FORMAT_NV21,
        FRAME_FORMAT_MJPEG
    }
    
    enum class PreviewCameraStyle {
        NORMAL,
        DUAL,
        THERMAL,
        EXTERNAL_CAMERA,
        ALL_IN_ONE
    }
    
    enum class DeviceStyle {
        SINGLE,
        DUAL,
        THERMAL,
        ALL_IN_ONE
    }
    
    enum class Y16ModePreviewSrcType {
        RAW,
        PROCESSED,
        Y16_MODE_TEMPERATURE,
        Y16_MODE_IR,
        Y16_MODE_KBC,
        Y16_MODE_HBC_DPC,
        Y16_MODE_VBC,
        Y16_MODE_TNR,
        Y16_MODE_SNR,
        Y16_MODE_AGC,
        Y16_MODE_DDE,
        Y16_MODE_GAMMA,
        Y16_MODE_MIRROR
    }
    
    data class ImageRes(val width: Int, val height: Int)
    data class GainSwitchParam(val enabled: Boolean)
    data class AutoGainSwitchInfo_t(val enabled: Boolean = false, val threshold: Float = 0.5f)
    data class GainSwitchParam_t(val mode: GainMode = GainMode.AUTO, val sensitivity: Float = 1.0f)
    data class ImageRes_t(val width: Int, val height: Int)
    
    object PropTPDParamsValue {
        enum class GAINSELStatus(val value: Int) {
            LOW_GAIN(0),
            HIGH_GAIN(1)
        }
    }
    
    object PropImageParams {
        object IMAGE_PROP_SEL_MIRROR_FLIP
        object IMAGE_PROP_LEVEL_CONTRAST
        object IMAGE_PROP_LEVEL_DDE
    }
    
    object PropImageParamsValue {
        data class NumberType(val value: String)
        
        enum class MirrorFlipType {
            NO_MIRROR_FLIP,
            ONLY_MIRROR,
            ONLY_FLIP,
            MIRROR_AND_FLIP
        }
        
        enum class DDEType {
            DDE_0, DDE_1, DDE_2, DDE_3, DDE_4
        }
    }
    
    object PropAutoShutterParameter {
        object SHUTTER_PROP_SWITCH
    }
    
    object PropAutoShutterParameterValue {
        enum class StatusSwith {
            ON, OFF
        }
    }
    
    object IMAGE_OUTPUT
    object TNR_OUTPUT
    object P2
    
    // IRPROC constants
    const val IRPROC_SRC_FMT_ARGB8888 = 4
    
    // Support size list functionality
    fun getSupportSizeList(format: Any?): List<Any> = listOf("256x192", "640x480", "1024x768")
    val supportSizeList: List<String> = listOf("256x192", "640x480", "1024x768")
}