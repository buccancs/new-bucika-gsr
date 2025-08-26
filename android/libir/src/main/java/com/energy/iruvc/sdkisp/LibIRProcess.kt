package com.energy.iruvc.sdkisp

import com.energy.iruvc.utils.CommonParams

/**
 * Stub implementation for LibIRProcess - native IR processing library
 */
object LibIRProcess {
    
    @JvmStatic
    fun convertYuyvMapToARGBPseudocolor(
        srcImage: ByteArray, 
        size: Long, 
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
        size: Long, 
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
        size: Long, 
        pseudoColorType: Any, 
        destImage: ByteArray
    ): Int {
        // Generic stub implementation
        if (srcImage.size >= destImage.size) {
            System.arraycopy(srcImage, 0, destImage, 0, destImage.size)
        }
        return 0
    }
}