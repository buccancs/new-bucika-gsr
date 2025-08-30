package com.infisense.usbir.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * Bitmap utility class for common bitmap operations
 */
public class BitmapUtils {
    
    /**
     * Scale bitmap to specified width and height
     * @param bitmap Original bitmap
     * @param width Target width
     * @param height Target height
     * @return Scaled bitmap
     */
    public static Bitmap scaleWithWH(Bitmap bitmap, float width, float height) {
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        
        if (originalWidth == 0 || originalHeight == 0) {
            return bitmap;
        }
        
        float scaleX = width / originalWidth;
        float scaleY = height / originalHeight;
        
        Matrix matrix = new Matrix();
        matrix.postScale(scaleX, scaleY);
        
        try {
            return Bitmap.createBitmap(bitmap, 0, 0, originalWidth, originalHeight, matrix, true);
        } catch (OutOfMemoryError e) {
            // Return original bitmap if out of memory
            return bitmap;
        }
    }
    
    /**
     * Draw center label on bitmap with title, address and timestamp
     * @param bitmap Original bitmap
     * @param title Title text
     * @param address Address text
     * @param timestamp Timestamp text
     * @param flags Drawing flags
     * @return Bitmap with labels drawn
     */
    public static Bitmap drawCenterLable(Bitmap bitmap, String title, String address, String timestamp, int flags) {
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        // For now, return the original bitmap as-is
        // This would need proper Canvas implementation for text drawing
        return bitmap;
    }
}