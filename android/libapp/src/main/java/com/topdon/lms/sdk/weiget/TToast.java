package com.topdon.lms.sdk.weiget;

import android.content.Context;
import android.util.Log;

/**
 * Stub implementation for TToast
 * 
 * @author Copilot
 */
public class TToast {
    
    public static void shortToast(Context context, String message) {
        // Stub implementation - log instead of showing toast
        Log.d("TToast", "Short toast: " + message);
    }
    
    public static void longToast(Context context, String message) {
        // Stub implementation - log instead of showing toast
        Log.d("TToast", "Long toast: " + message);
    }
}