package com.topdon.lms.sdk.network;

/**
 * Stub implementation for IResponseCallback
 * 
 * @author Copilot
 */
public interface IResponseCallback {
    void onSuccess(Object result);
    void onError(String error);
}