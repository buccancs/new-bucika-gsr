package com.topdon.commons.util

import android.content.Context
import android.util.Log

// Commented out UMeng import for BucikaGSR compatibility
// import com.umeng.analytics.MobclickAgent

/**
 * @Desc Analytics utility class (UMeng functionality stubbed for BucikaGSR)
 * @ClassName UMUtils
 * @Email 616862466@qq.com
 * @Author 子墨
 * @Date 2023/3/28 13:53
 */
object UMUtils {

    // Stub implementations for UMeng analytics - can be replaced with other analytics solutions
    fun onEvent(context: Context, event: String, value: String) {
        // MobclickAgent.onEvent(context, event, value)
        // Placeholder for analytics event tracking
        Log.d("UMUtils", "Event: $event, Value: $value")
    }

    fun onEvent(context: Context, event: String) {
        // MobclickAgent.onEvent(context, event)
        // Placeholder for analytics event tracking
        Log.d("UMUtils", "Event: $event")
    }
}