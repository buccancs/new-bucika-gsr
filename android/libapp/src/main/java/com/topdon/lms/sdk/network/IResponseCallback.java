package com.topdon.lms.sdk.network;

/**
 * Stub implementation for IResponseCallback
 * 
 * @author Copilot
 */
public interface IResponseCallback {
    void onResponse(String response);
    void onFail(Exception exception);
    
    // Additional onFail signature for compatibility
    default void onFail(String failMsg, String errorCode) {
        onFail(new Exception(failMsg + " (code: " + errorCode + ")"));
    }
    
    // Additional onFail signature for newer versions
    default void onFail(int errorCode, String errorMessage) {
        onFail(new Exception(errorMessage + " (code: " + errorCode + ")"));
    }
    
    // Generic onSuccess for different response types
    default void onSuccess(Object result) {
        if (result != null) {
            onResponse(result.toString());
        } else {
            onResponse("{}");
        }
    }
}