package com.topdon.commons.util;

import android.content.Context;

// Commented out UMeng import for BucikaGSR compatibility
// import com.umeng.analytics.MobclickAgent;

/**
 * @Desc Analytics utility class (UMeng functionality stubbed for BucikaGSR)
 * @ClassName UMUtils
 * @Email 616862466@qq.com
 * @Author 子墨
 * @Date 2023/3/28 13:53
 */

public class UMUtils {

    // Stub implementations for UMeng analytics - can be replaced with other analytics solutions
    public static void onEvent(Context mContext, String var1, String var2) {
        // MobclickAgent.onEvent(mContext, var1, var2);
        // Placeholder for analytics event tracking
        android.util.Log.d("UMUtils", "Event: " + var1 + ", Value: " + var2);
    }

    public static void onEvent(Context mContext, String var1) {
        // MobclickAgent.onEvent(mContext, var1);
        // Placeholder for analytics event tracking
        android.util.Log.d("UMUtils", "Event: " + var1);
    }

}
