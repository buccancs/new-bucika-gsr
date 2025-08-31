package com.topdon.lms.sdk.utils;

import android.content.Context;

/**
 * Stub implementation for SPUtils
 * 
 * @author Copilot
 */
public class SPUtils {
    private static SPUtils instance;
    
    public static SPUtils getInstance(Context context) {
        if (instance == null) {
            instance = new SPUtils();
        }
        return instance;
    }
    
    public Object get(String key, Object defaultValue) {
        // Stub implementation - return default value
        return defaultValue;
    }
}