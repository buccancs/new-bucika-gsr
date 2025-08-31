package com.topdon.lms.sdk;

import android.content.Context;

/**
 * Stub implementation for LMS SDK
 * 
 * @author Copilot
 */
public class LMS {
    public static final int SUCCESS = 0;
    private static LMS instance = new LMS();
    
    // Context stub
    public static Context mContext;
    
    public static LMS getInstance() {
        return instance;
    }
    
    public String getLoginName() {
        return "guest";
    }
    
    public void checkAppUpdate(CheckUpdateCallback callback) {
        // Stub implementation - create a response object
        CallbackData response = new CallbackData();
        response.code = 2000;
        response.data = "{\"version\":\"1.0.0\"}";
        callback.invoke(response);
    }
    
    public void getStatement(String type, com.topdon.lms.sdk.network.IResponseCallback callback) {
        // Stub implementation
        callback.onResponse("{\"code\":\"2000\",\"data\":{\"url\":\"https://example.com\"}}");
    }
    
    // Inner class for callback data
    public static class CallbackData {
        public int code;
        public String data;
    }
    
    // Functional interface for checkAppUpdate callback
    public interface CheckUpdateCallback {
        void invoke(CallbackData data);
    }
}