package com.example.thermal_lite;

/**
 * Created by fengjibo on 2023/5/8.
 */
public class IrConst {

    //默认的出图数据，可在“USB基础信息”中修改
    public static final int DEFAULT_STREAM_WIDTH = 256;
    public static final int DEFAULT_STREAM_HEIGHT = 386;
    public static final int DEFAULT_STREAM_FPS = 25;
    public static final float DEFAULT_STREAM_BANDWIDTH = 1.0f;
    public static final boolean DEFAULT_DOUBLE_IMAGE = true;

    public static final String KEY_DEFAULT_STREAM_WIDTH = "KEY_DEFAULT_STREAM_WIDTH";
    public static final String KEY_DEFAULT_STREAM_HEIGHT = "KEY_DEFAULT_STREAM_HEIGHT";
    public static final String KEY_DEFAULT_STREAM_FPS = "KEY_DEFAULT_STREAM_FPS";
    public static final String KEY_DEFAULT_STREAM_BANDWIDTH = "KEY_DEFAULT_STREAM_BANDWIDTH";
    public static final String KEY_DEFAULT_DOUBLE_IMAGE = "KEY_DEFAULT_DOUBLE_IMAGE";
    //统一修改当前加载的距离修正表
    public static final String TAU_HIGH_GAIN_ASSET_PATH = "lite/highF.bin";
    public static final String TAU_LOW_GAIN_ASSET_PATH = "lite/lowF.bin";

}
