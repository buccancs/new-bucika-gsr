package com.topdon.lms.sdk

/**
 * Stub implementation of LMS SDK for compilation
 * This provides minimal implementation to allow compilation when LMS AAR is in app module
 */
class LMS private constructor() {
    companion object {
        @Volatile
        private var instance: LMS? = null
        
        var mContext: android.content.Context? = null
        
        @JvmStatic
        fun getInstance(): LMS {
            return instance ?: synchronized(this) {
                instance ?: LMS().also { instance = it }
            }
        }
    }
    
    var loginName: String = ""
    
    // Minimal stub methods for compilation
    fun init(context: android.content.Context) {
        LMS.mContext = context
    }
    
    fun isLogin(): Boolean = false
    
    fun login(username: String, password: String, callback: Any?) {}
    
    fun checkAppUpdate(callback: (CallbackData) -> Unit) {
        // Stub implementation - calls back with empty data
        callback.invoke(CallbackData())
    }
    
    fun getStatement(request: Any?, callback: Any?) {}
    
    fun syncUserInfo() {}
    
    fun getUserInfo(): CommonBean = CommonBean()
    
    // Mock callback data
    data class CallbackData(
        val code: Int = 2000,
        val data: String = "{}"
    )
}