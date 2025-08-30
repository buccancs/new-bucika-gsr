package com.topdon.libapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Bitmap utility class for image processing operations
 * Provides comprehensive image manipulation functions for thermal imaging
 */
public class BitmapUtils {
    
    /**
     * Convert bitmap to byte array
     */
    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        if (bitmap == null) return null;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }
    
    /**
     * Convert byte array to bitmap
     */
    public static Bitmap byteArrayToBitmap(byte[] byteArray) {
        if (byteArray == null || byteArray.length == 0) return null;
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }
    
    /**
     * Convert bitmap to Base64 string
     */
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        byte[] byteArray = bitmapToByteArray(bitmap);
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
    
    /**
     * Convert Base64 string to bitmap
     */
    public static Bitmap base64ToBitmap(String base64String) {
        if (base64String == null || base64String.isEmpty()) return null;
        byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
        return byteArrayToBitmap(decodedBytes);
    }
    
    /**
     * Rotate bitmap by specified degrees
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, float degrees) {
        if (bitmap == null || degrees == 0) return bitmap;
        
        Matrix matrix = new Matrix();
        matrix.setRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
    
    /**
     * Scale bitmap to specified dimensions
     */
    public static Bitmap scaleBitmap(Bitmap bitmap, int targetWidth, int targetHeight) {
        if (bitmap == null) return null;
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
    }
    
    /**
     * Crop bitmap to specified rectangle
     */
    public static Bitmap cropBitmap(Bitmap bitmap, Rect cropRect) {
        if (bitmap == null || cropRect == null) return bitmap;
        
        int x = Math.max(0, cropRect.left);
        int y = Math.max(0, cropRect.top);
        int width = Math.min(cropRect.width(), bitmap.getWidth() - x);
        int height = Math.min(cropRect.height(), bitmap.getHeight() - y);
        
        if (width <= 0 || height <= 0) return bitmap;
        
        return Bitmap.createBitmap(bitmap, x, y, width, height);
    }
    
    /**
     * Save bitmap to file
     */
    public static boolean saveBitmapToFile(Bitmap bitmap, File file, Bitmap.CompressFormat format, int quality) {
        if (bitmap == null || file == null) return false;
        
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(format, quality, out);
            out.flush();
            out.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Load bitmap from file path
     */
    public static Bitmap loadBitmapFromFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) return null;
        return BitmapFactory.decodeFile(filePath);
    }
    
    /**
     * Get image orientation from EXIF data
     */
    public static int getImageOrientation(String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException e) {
            return ExifInterface.ORIENTATION_NORMAL;
        }
    }
    
    /**
     * Combine two bitmaps side by side
     */
    public static Bitmap combineBitmapsHorizontally(Bitmap left, Bitmap right) {
        if (left == null) return right;
        if (right == null) return left;
        
        int width = left.getWidth() + right.getWidth();
        int height = Math.max(left.getHeight(), right.getHeight());
        
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        
        canvas.drawBitmap(left, 0, 0, null);
        canvas.drawBitmap(right, left.getWidth(), 0, null);
        
        return result;
    }
    
    /**
     * Add watermark to bitmap
     */
    public static Bitmap addWatermark(Bitmap src, String watermarkText, int x, int y, int color, float textSize) {
        if (src == null) return null;
        
        Bitmap result = src.copy(src.getConfig(), true);
        Canvas canvas = new Canvas(result);
        
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setTextSize(textSize);
        
        canvas.drawText(watermarkText, x, y, paint);
        
        return result;
    }
    
    /**
     * Create a thumbnail bitmap
     */
    public static Bitmap createThumbnail(Bitmap source, int maxWidth, int maxHeight) {
        if (source == null) return null;
        
        int width = source.getWidth();
        int height = source.getHeight();
        
        if (width <= maxWidth && height <= maxHeight) {
            return source;
        }
        
        float scaleWidth = (float) maxWidth / width;
        float scaleHeight = (float) maxHeight / height;
        float scale = Math.min(scaleWidth, scaleHeight);
        
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);
        
        return scaleBitmap(source, newWidth, newHeight);
    }
    
    /**
     * Check if bitmap is valid
     */
    public static boolean isValidBitmap(Bitmap bitmap) {
        return bitmap != null && !bitmap.isRecycled() && bitmap.getWidth() > 0 && bitmap.getHeight() > 0;
    }
    
    /**
     * Draw center label with title, address, time and temperature
     */
    public static Bitmap drawCenterLable(Bitmap bitmap, String title, String address, String time, int temperature) {
        if (bitmap == null) return null;
        
        Bitmap result = bitmap.copy(bitmap.getConfig(), true);
        Canvas canvas = new Canvas(result);
        
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(android.graphics.Color.WHITE);
        paint.setTextSize(24f);
        paint.setTextAlign(Paint.Align.CENTER);
        
        int centerX = bitmap.getWidth() / 2;
        int y = bitmap.getHeight() - 100; // Start from bottom
        
        // Draw title
        if (title != null && !title.isEmpty()) {
            canvas.drawText(title, centerX, y, paint);
            y -= 30;
        }
        
        // Draw address
        if (address != null && !address.isEmpty()) {
            canvas.drawText(address, centerX, y, paint);
            y -= 30;
        }
        
        // Draw time
        if (time != null && !time.isEmpty()) {
            canvas.drawText(time, centerX, y, paint);
            y -= 30;
        }
        
        // Draw temperature if provided
        if (temperature != 0) {
            canvas.drawText(temperature + "°C", centerX, y, paint);
        }
        
        return result;
    }
}