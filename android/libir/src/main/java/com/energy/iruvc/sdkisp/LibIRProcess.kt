package com.energy.iruvc.sdkisp

import com.energy.iruvc.utils.CommonParams

/**
 * Stub implementation for LibIRProcess - native IR processing library
 */
object LibIRProcess {
    
    @JvmStatic
    fun convertYuyvMapToARGBPseudocolor(
        srcImage: ByteArray, 
        size: Int, 
        pseudoColorType: CommonParams.PseudoColorType, 
        destImage: ByteArray
    ): Int {
        // Stub implementation - copy source to destination
        if (srcImage.size >= destImage.size) {
            System.arraycopy(srcImage, 0, destImage, 0, destImage.size)
        }
        return 0
    }
    
    @JvmStatic  
    fun convertYuyvMapToARGBPseudocolor(
        srcImage: ByteArray, 
        size: Int, 
        pseudoColorType: CommonParams.PseudoColorUsbDualType, 
        destImage: ByteArray
    ): Int {
        // Stub implementation - copy source to destination
        if (srcImage.size >= destImage.size) {
            System.arraycopy(srcImage, 0, destImage, 0, destImage.size)
        }
        return 0
    }
    
    @JvmStatic
    fun convertYuyvMapToARGBPseudocolor(
        srcImage: ByteArray, 
        size: Int, 
        pseudoColorType: Any, 
        destImage: ByteArray
    ): Int {
        // Generic stub implementation
        if (srcImage.size >= destImage.size) {
            System.arraycopy(srcImage, 0, destImage, 0, destImage.size)
        }
        return 0
    }
    
    // Type definitions for gain switching and image resolution
    data class AutoGainSwitchInfo_t(
        var enabled: Boolean = false, 
        var threshold: Float = 0.5f,
        var switched_flag: Boolean = false,
        var cur_switched_cnt: Int = 0,
        var cur_detected_low_cnt: Int = 0,
        var cur_detected_high_cnt: Int = 0,
        var switch_frame_cnt: Int = 0,
        var waiting_frame_cnt: Int = 0
    )
    
    data class GainSwitchParam_t(
        var mode: Any = "AUTO", 
        var sensitivity: Float = 1.0f,
        var above_pixel_prop: Float = 0.0f,
        var above_temp_data: Int = 0,
        var below_pixel_prop: Float = 0.0f,
        var below_temp_data: Int = 0
    )
    data class ImageRes_t(var width: Int = 0, var height: Int = 0)
    
    @JvmStatic
    fun rotateRight90(srcImage: ByteArray, imageRes: ImageRes_t, format: Any, destImage: ByteArray): Int {
        // Stub implementation - simple copy
        System.arraycopy(srcImage, 0, destImage, 0, minOf(srcImage.size, destImage.size))
        return 0
    }
    
    @JvmStatic
    fun rotateLeft90(srcImage: ByteArray, imageRes: ImageRes_t, format: Any, destImage: ByteArray): Int {
        // Stub implementation - simple copy
        System.arraycopy(srcImage, 0, destImage, 0, minOf(srcImage.size, destImage.size))
        return 0
    }
    
    @JvmStatic  
    fun rotate180(srcImage: ByteArray, imageRes: ImageRes_t, format: Any, destImage: ByteArray): Int {
        // Stub implementation - simple copy
        System.arraycopy(srcImage, 0, destImage, 0, minOf(srcImage.size, destImage.size))
        return 0
    }
}