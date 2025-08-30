package com.example.open3d;

/**
 * JNI wrapper for Open3D native methods
 * Provides access to native image processing functions in libopen3d.so
 */
public class JNITool {
    
    /**
     * Singleton instance for INSTANCE pattern used in ImageThreadTC
     */
    public static final JNITool INSTANCE = new JNITool();
    
    static {
        try {
            System.loadLibrary("open3d");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Draw edge from temperature region bitmap ARGB PSD
     */
    public static native byte[] draw_edge_from_temp_reigon_bitmap_argb_psd(
            byte[] imageDst,
            byte[] temperatureSrc,
            int height,
            int width,
            float highTemp,
            float lowTemp,
            int highColor,
            int lowColor,
            int lineWidth
    );
    
    /**
     * Max temperature tracking
     */
    public native byte[] maxTempL(
            byte[] imageDst,
            byte[] temperatureSrc,
            int imageHeight,
            int imageWidth,
            int color
    );
    
    /**
     * Low temperature tracking
     */
    public native byte[] lowTemTrack(
            byte[] imageDst,
            byte[] temperatureSrc,
            int imageHeight,
            int imageWidth,
            int color
    );
    
    /**
     * Difference to first frame by temperature width/height
     */
    public native byte[] diff2firstFrameByTempWH(
            int imageHeight,
            int imageWidth,
            byte[] firstFrameTemp,
            byte[] temperatureSrc,
            byte[] imageDst
    );
}