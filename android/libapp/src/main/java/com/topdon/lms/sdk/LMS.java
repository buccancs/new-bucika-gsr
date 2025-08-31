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
    
    public String token = "stub_token";
    
    public static LMS getInstance() {
        return instance;
    }
    
    public String getLoginName() {
        return "guest";
    }
    
    public boolean isLogin() {
        return true; // Stub - always logged in
    }
    
    public void syncUserInfo() {
        // Stub implementation for sync
    }
    
    public void getUserInfo(UserInfoCallback callback) {
        // Stub implementation - create mock user info
        com.topdon.lms.sdk.bean.CommonBean bean = new com.topdon.lms.sdk.bean.CommonBean();
        bean.data = "{\"id\":1,\"name\":\"Test User\"}";
        callback.invoke(bean);
    }
    
    public void bindDevice(String deviceId, CheckUpdateCallback callback) {
        // Stub implementation
        CallbackData response = new CallbackData();
        response.code = 2000;
        response.data = "{\"success\":true}";
        callback.invoke(response);
    }
    
    public void bindDevice(String sn, String randomNum, String param3, String param4, CheckUpdateCallback callback) {
        // Overloaded stub implementation for 5-parameter version
        CallbackData response = new CallbackData();
        response.code = 2000;
        response.data = "{\"success\":true}";
        callback.invoke(response);
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
    
    // Functional interface for getUserInfo callback
    public interface UserInfoCallback {
        void invoke(com.topdon.lms.sdk.bean.CommonBean bean);
    }
}