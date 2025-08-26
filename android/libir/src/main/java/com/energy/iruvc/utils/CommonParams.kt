package com.energy.iruvc.utils

// Stub implementation of CommonParams with commonly used nested classes
object CommonParams {
    
    enum class DataFlowMode {
        IMAGE_ONLY,
        TEMP_ONLY,
        IMAGE_AND_TEMP_OUTPUT,
        Y16_MODE
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
    
    data class ImageRes(val width: Int, val height: Int)
    
    data class GainSwitchParam(val enabled: Boolean)
    
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
}