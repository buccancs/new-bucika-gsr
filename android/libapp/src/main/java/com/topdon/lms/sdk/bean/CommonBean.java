package com.topdon.lms.sdk.bean;

/**
 * Stub implementation for CommonBean
 * 
 * @author Copilot
 */
public class CommonBean {
    public String data;
    public int code;
    public String message;
    
    public CommonBean() {
        this.code = 200;
        this.message = "success";
        this.data = "{}";
    }
}