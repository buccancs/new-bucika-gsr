package com.topdon.commons.util;

import android.content.Context;
import android.util.Log;

/**
 * @Desc 友盟埋点工具类 - Stub implementation
 * @ClassName UMUtils
 * @Email 616862466@qq.com
 * @Author 子墨
 * @Date 2023/3/28 13:53
 */

public class UMUtils {

    private static final String TAG = "UMUtils";

    public static void onEvent(Context mContext, String var1, String var2) {
        // Stub implementation - logging instead of actual analytics
        Log.d(TAG, "Analytics event: " + var1 + " with value: " + var2);
    }

    public static void onEvent(Context mContext, String var1) {
        // Stub implementation - logging instead of actual analytics
        Log.d(TAG, "Analytics event: " + var1);
    }

}
