package com.topdon.lms.sdk;

/**
 * Stub implementation for LMS SDK
 * 
 * @author Copilot
 */
public class LMS {
    public static final int SUCCESS = 0;
    private static LMS instance = new LMS();
    
    public static LMS getInstance() {
        return instance;
    }
    
    public String getLoginName() {
        return "guest";
    }
}