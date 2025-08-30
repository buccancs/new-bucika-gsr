package com.example.connectlisten;

/**
 * JNI test utility for native library testing
 */
public class JNITest {
    
    static {
        try {
            System.loadLibrary("jniavutil");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Test native method
     */
    public native int testConnection();
    
    /**
     * Initialize native library
     */
    public native boolean initNative();
    
    /**
     * Release native resources
     */
    public native void releaseNative();
    
    /**
     * Test method to verify library loading
     */
    public static boolean isNativeLibraryLoaded() {
        try {
            JNITest test = new JNITest();
            test.testConnection();
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }
}