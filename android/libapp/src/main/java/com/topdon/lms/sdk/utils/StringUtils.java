package com.topdon.lms.sdk.utils;

import android.content.Context;

/**
 * Stub implementation for StringUtils
 * 
 * @author Copilot
 */
public class StringUtils {
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    public static String getResString(Context context, int resId) {
        try {
            return context.getString(resId);
        } catch (Exception e) {
            return "";
        }
    }
    
    public static String getResString(int resId) {
        // Stub implementation - return default string
        return "Resource string " + resId;
    }
}