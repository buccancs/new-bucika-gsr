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
}